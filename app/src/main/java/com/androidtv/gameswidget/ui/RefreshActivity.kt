package com.androidtv.gameswidget.ui

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.androidtv.gameswidget.R
import com.androidtv.gameswidget.sync.SyncWorker

/**
 * Invisible trampoline for the home-screen "refresh" tile. Clicking the tile kicks
 * off a single background sync of the game list + channel and returns immediately to
 * the home screen — no need to open the app. Translucent: no visible UI.
 */
class RefreshActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncWorker.runOnce(applicationContext)
        Toast.makeText(applicationContext, getString(R.string.refresh_started), Toast.LENGTH_SHORT).show()
        finish()
    }
}
