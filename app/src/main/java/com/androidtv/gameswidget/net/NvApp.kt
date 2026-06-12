package com.androidtv.gameswidget.net

/** A streamable app/game as reported by the host's /applist endpoint. */
data class NvApp(
    val appId: Int,
    val appName: String,
    val hdrSupported: Boolean,
)
