package com.androidtv.gameswidget.data

import android.content.Context
import com.androidtv.gameswidget.net.NvApp
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Persists the single paired host + its game list. SharedPreferences-backed;
 * box-art PNGs live as files under filesDir/boxart/.
 */
class HostStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("host", Context.MODE_PRIVATE)
    private val boxArtDir = File(context.filesDir, "boxart").apply { mkdirs() }

    val isConfigured: Boolean get() = prefs.contains(KEY_HOST)

    var host: String?
        get() = prefs.getString(KEY_HOST, null)
        set(v) = prefs.edit().putString(KEY_HOST, v).apply()

    var httpPort: Int
        get() = prefs.getInt(KEY_HTTP_PORT, 47989)
        set(v) = prefs.edit().putInt(KEY_HTTP_PORT, v).apply()

    var httpsPort: Int
        get() = prefs.getInt(KEY_HTTPS_PORT, 47984)
        set(v) = prefs.edit().putInt(KEY_HTTPS_PORT, v).apply()

    var pcName: String?
        get() = prefs.getString(KEY_PC_NAME, null)
        set(v) = prefs.edit().putString(KEY_PC_NAME, v).apply()

    /** PC UUID from serverinfo — also the key Moonlight uses to resolve the PC. */
    var pcUuid: String?
        get() = prefs.getString(KEY_PC_UUID, null)
        set(v) = prefs.edit().putString(KEY_PC_UUID, v).apply()

    /** Pinned server certificate (PEM), reused to re-establish trust each session. */
    var serverCert: X509Certificate?
        get() = prefs.getString(KEY_SERVER_CERT, null)?.let { pem ->
            CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(pem.toByteArray())) as X509Certificate
        }
        set(v) {
            val pem = v?.let {
                "-----BEGIN CERTIFICATE-----\n" +
                    android.util.Base64.encodeToString(it.encoded, android.util.Base64.DEFAULT) +
                    "-----END CERTIFICATE-----\n"
            }
            prefs.edit().putString(KEY_SERVER_CERT, pem).apply()
        }

    fun saveGames(games: List<NvApp>) {
        val arr = JSONArray()
        games.forEach {
            arr.put(JSONObject().put("id", it.appId).put("name", it.appName).put("hdr", it.hdrSupported))
        }
        prefs.edit().putString(KEY_GAMES, arr.toString()).apply()
    }

    fun loadGames(): List<NvApp> {
        val raw = prefs.getString(KEY_GAMES, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            NvApp(o.getInt("id"), o.getString("name"), o.optBoolean("hdr", false))
        }
    }

    fun boxArtFile(appId: Int): File = File(boxArtDir, "$appId.png")

    /** Poster file for the home-screen "refresh" tile (lives alongside box art so the
     *  FileProvider can serve it). The leading underscore keeps it clear of appId files. */
    fun refreshTileFile(): File = File(boxArtDir, "_refresh.png")

    // ---- Per-game visibility (which games appear in the home-screen widget) ----
    // We store the HIDDEN ids; an empty set means everything is visible, so games
    // discovered in future syncs are shown by default.

    private fun hiddenIds(): MutableSet<Int> {
        val raw = prefs.getString(KEY_HIDDEN, null) ?: return mutableSetOf()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getInt(it) }.toMutableSet()
        }.getOrDefault(mutableSetOf())
    }

    fun isGameVisible(appId: Int): Boolean = appId !in hiddenIds()

    fun setGameVisible(appId: Int, visible: Boolean) {
        val set = hiddenIds()
        if (visible) set.remove(appId) else set.add(appId)
        val arr = JSONArray()
        set.forEach { arr.put(it) }
        prefs.edit().putString(KEY_HIDDEN, arr.toString()).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
        boxArtDir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val KEY_HOST = "host"
        const val KEY_HTTP_PORT = "http_port"
        const val KEY_HTTPS_PORT = "https_port"
        const val KEY_PC_NAME = "pc_name"
        const val KEY_PC_UUID = "pc_uuid"
        const val KEY_SERVER_CERT = "server_cert"
        const val KEY_GAMES = "games"
        const val KEY_HIDDEN = "hidden_games"
    }
}
