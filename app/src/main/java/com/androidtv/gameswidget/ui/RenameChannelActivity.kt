package com.androidtv.gameswidget.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidtv.gameswidget.R
import com.androidtv.gameswidget.databinding.ActivityRenameBinding
import com.androidtv.gameswidget.tv.TvChannelPublisher

/**
 * Lets the user set the text shown after the launcher's "Ігри:" prefix on the
 * home-screen channel. Stored in the same prefs the channel publisher reads.
 * Empty = no suffix (the launcher still shows the fixed app-label prefix).
 */
class RenameChannelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRenameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRenameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(TvChannelPublisher.PREFS, Context.MODE_PRIVATE)
        binding.nameInput.setText(prefs.getString(TvChannelPublisher.KEY_CHANNEL_NAME, ""))

        binding.saveButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            prefs.edit().putString(TvChannelPublisher.KEY_CHANNEL_NAME, name).apply()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                TvChannelPublisher(this).applyChannelName()
            }
            Toast.makeText(this, getString(R.string.rename_saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
