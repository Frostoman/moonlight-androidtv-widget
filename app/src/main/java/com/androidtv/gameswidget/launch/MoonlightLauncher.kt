package com.androidtv.gameswidget.launch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Launches a game by handing off to Moonlight's exported ShortcutTrampoline —
 * the same public entry point Moonlight's own home-screen shortcuts use.
 *
 * Moonlight resolves the PC from its OWN database by [pcUuid], so the PC must
 * already be added & paired inside Moonlight for the stream to start. This app's
 * separate pairing is only used to read the game list.
 */
object MoonlightLauncher {

    const val MOONLIGHT_PACKAGE = "com.limelight"
    private const val TRAMPOLINE = "com.limelight.ShortcutTrampoline"

    // ShortcutTrampoline / AppView / Game extra keys (verbatim from moonlight-android).
    private const val EXTRA_PC_UUID = "UUID"
    private const val EXTRA_PC_NAME = "Name"
    private const val EXTRA_APP_ID = "AppId"
    private const val EXTRA_APP_NAME = "AppName"

    fun isMoonlightInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(MOONLIGHT_PACKAGE, 0); true
    }.getOrDefault(false)

    /** Build the launch intent for a specific game on a specific PC. */
    fun launchIntent(pcUuid: String, pcName: String?, appId: Int, appName: String?): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(MOONLIGHT_PACKAGE, TRAMPOLINE)
            putExtra(EXTRA_PC_UUID, pcUuid)
            pcName?.let { putExtra(EXTRA_PC_NAME, it) }
            // ShortcutTrampoline reads AppId as a String, then re-parses it as int.
            putExtra(EXTRA_APP_ID, appId.toString())
            appName?.let { putExtra(EXTRA_APP_NAME, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    fun launch(context: Context, pcUuid: String, pcName: String?, appId: Int, appName: String?): Boolean {
        if (!isMoonlightInstalled(context)) return false
        return try {
            context.startActivity(launchIntent(pcUuid, pcName, appId, appName))
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Open Moonlight's main screen (fallback when we can't deep-link). */
    fun launchMoonlight(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(MOONLIGHT_PACKAGE) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
