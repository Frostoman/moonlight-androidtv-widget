package com.androidtv.gameswidget.tv

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.androidtv.gameswidget.R
import com.androidtv.gameswidget.data.HostStore
import com.androidtv.gameswidget.launch.MoonlightLauncher
import com.androidtv.gameswidget.net.NvApp
import com.androidtv.gameswidget.ui.MainActivity

/**
 * Publishes the game list as a preview channel + program cards on the Android TV
 * home screen. Requires Android O (API 26) and a launcher that supports channels.
 *
 * NOTE: androidx.tvprovider 1.0.0's updatePreviewChannel()/updatePreviewProgram()
 * crash with an NPE when the stored row has a null description (they read it back
 * via fromCursor() -> setDescription(null)). We therefore avoid those entirely:
 * the channel name is updated directly through ContentResolver, and programs are
 * wiped + republished each sync (also prevents duplicate cards).
 */
class TvChannelPublisher(private val context: Context) {

    private val prefs = context.getSharedPreferences("channel", Context.MODE_PRIVATE)
    private val helper = PreviewChannelHelper(context)

    @RequiresApi(Build.VERSION_CODES.O)
    fun publish(store: HostStore, games: List<NvApp>) {
        val channelId = ensureChannel()
        val pcUuid = store.pcUuid ?: return
        val pcName = store.pcName

        // Only games the user kept checked appear in the home-screen widget.
        val visible = games.filter { store.isGameVisible(it.appId) }

        // Clear existing programs before republishing.
        // 1) Best-effort bulk delete (cleans orphans, but some launchers forbid a
        //    selection clause on the preview_program URI -> ignore that failure).
        runCatching {
            context.contentResolver.delete(
                TvContractCompat.PreviewPrograms.CONTENT_URI,
                "${TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID} = ?",
                arrayOf(channelId.toString()),
            )
        }.onFailure { android.util.Log.w(TAG, "bulk program delete not permitted; using tracked ids", it) }
        // 2) Reliable per-id delete of programs we published last time (always allowed).
        val oldIds = readProgramIds()
        for (id in oldIds) runCatching { helper.deletePreviewProgram(id) }
        android.util.Log.i(TAG, "publish: removed ${oldIds.size} tracked programs, adding ${visible.size}/${games.size}")

        val newIds = mutableListOf<Long>()
        for (game in visible) {
            val intent = MoonlightLauncher.launchIntent(pcUuid, pcName, game.appId, game.appName)
            val posterUri = posterUriFor(store, game.appId)

            val builder = PreviewProgram.Builder()
                .setChannelId(channelId)
                .setType(TvContractCompat.PreviewPrograms.TYPE_GAME)
                .setTitle(game.appName)
                .setDescription(if (game.hdrSupported) "HDR" else "") // non-null avoids fromCursor NPE
                .setIntent(intent)
                .setInternalProviderId(game.appId.toString())
                .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_MOVIE_POSTER)

            if (posterUri != null) {
                builder.setPosterArtUri(posterUri)
                grantToLaunchers(posterUri)
            }

            runCatching { helper.publishPreviewProgram(builder.build()) }
                .onSuccess { newIds.add(it) }
                .onFailure { android.util.Log.e(TAG, "publish program '${game.appName}' failed", it) }
        }
        writeProgramIds(newIds)
        android.util.Log.i(TAG, "publish: published ${newIds.size} programs")
    }

    private fun readProgramIds(): List<Long> = runCatching {
        val arr = org.json.JSONArray(prefs.getString(KEY_PROGRAMS, "[]"))
        (0 until arr.length()).map { arr.getLong(it) }
    }.getOrDefault(emptyList())

    private fun writeProgramIds(ids: List<Long>) {
        val arr = org.json.JSONArray()
        ids.forEach { arr.put(it) }
        prefs.edit().putString(KEY_PROGRAMS, arr.toString()).apply()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureChannel(): Long {
        // Custom suffix shown after the launcher's "<app label>: " prefix.
        // Blank -> null display name, so the launcher omits the trailing ": ".
        val displayName: String? = channelName().ifBlank { null }
        val existing = prefs.getLong(KEY_CHANNEL_ID, -1L)

        // Set display_name directly (avoids the buggy updatePreviewChannel()).
        fun writeName(id: Long): Int {
            val values = ContentValues().apply {
                if (displayName == null) putNull(TvContractCompat.Channels.COLUMN_DISPLAY_NAME)
                else put(TvContractCompat.Channels.COLUMN_DISPLAY_NAME, displayName)
            }
            return runCatching {
                context.contentResolver.update(TvContractCompat.buildChannelUri(id), values, null, null)
            }.onFailure { android.util.Log.e(TAG, "channel rename failed", it) }.getOrDefault(0)
        }

        if (existing >= 0) {
            val rows = writeName(existing)
            android.util.Log.i(TAG, "ensureChannel: renamed channel id=$existing rows=$rows name=$displayName")
            if (rows > 0) return existing
            android.util.Log.w(TAG, "ensureChannel: stored channel missing, recreating")
        }

        // PreviewChannel.Builder requires a non-null display name; use a placeholder,
        // then immediately overwrite it (with null when blank).
        val channel = PreviewChannel.Builder()
            .setDisplayName(displayName ?: context.getString(R.string.app_name))
            .setDescription("") // non-null avoids fromCursor NPE on later reads
            .setAppLinkIntent(Intent(context, MainActivity::class.java))
            .setLogo(android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888))
            .build()

        val id = helper.publishDefaultChannel(channel)
        prefs.edit().putLong(KEY_CHANNEL_ID, id).apply()
        writeName(id)
        android.util.Log.i(TAG, "ensureChannel: published new channel id=$id name=$displayName")
        runCatching { TvContractCompat.requestChannelBrowsable(context, id) }
            .onFailure { android.util.Log.w(TAG, "requestChannelBrowsable not handled", it) }
        return id
    }

    /** User-defined channel name suffix (persisted); falls back to the string resource. */
    private fun channelName(): String =
        prefs.getString(KEY_CHANNEL_NAME, null) ?: context.getString(R.string.channel_name)

    /** Apply just the channel display name (fast, no network). No-op if not yet created. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun applyChannelName() {
        val id = prefs.getLong(KEY_CHANNEL_ID, -1L)
        if (id < 0) return
        val displayName: String? = channelName().ifBlank { null }
        val values = ContentValues().apply {
            if (displayName == null) putNull(TvContractCompat.Channels.COLUMN_DISPLAY_NAME)
            else put(TvContractCompat.Channels.COLUMN_DISPLAY_NAME, displayName)
        }
        runCatching {
            context.contentResolver.update(TvContractCompat.buildChannelUri(id), values, null, null)
        }.onFailure { android.util.Log.e(TAG, "applyChannelName failed", it) }
    }

    private fun posterUriFor(store: HostStore, appId: Int): android.net.Uri? {
        val file = store.boxArtFile(appId)
        if (!file.exists() || file.length() == 0L) return null
        return FileProvider.getUriForFile(context, "${context.packageName}.boxart", file)
    }

    // Best-effort: let common TV launchers read our box-art content URIs.
    private fun grantToLaunchers(uri: android.net.Uri) {
        val launchers = listOf(
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcherx",
            "com.android.tv",
        )
        for (pkg in launchers) {
            runCatching {
                context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    companion object {
        private const val TAG = "MoonlightChannel"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_PROGRAMS = "programs"
        const val KEY_CHANNEL_NAME = "channel_name"
        const val PREFS = "channel"

        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}
