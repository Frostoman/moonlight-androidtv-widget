package com.androidtv.gameswidget.net

/** Subset of /serverinfo fields we care about. */
data class ComputerDetails(
    val name: String,
    val uuid: String,
    val httpsPort: Int,
    val appVersion: String,
    val paired: Boolean,
) {
    /** First component of the "x.y.z.w" appversion; >= 7 means SHA-256 pairing. */
    val majorVersion: Int get() = appVersion.substringBefore('.').toIntOrNull() ?: 0
}
