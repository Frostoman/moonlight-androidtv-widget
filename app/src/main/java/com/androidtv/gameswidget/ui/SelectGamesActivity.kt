package com.androidtv.gameswidget.ui

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.androidtv.gameswidget.App
import com.androidtv.gameswidget.R
import com.androidtv.gameswidget.tv.TvChannelPublisher

/** Hosts the game-selection checklist. */
class SelectGamesActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, SelectGamesFragment(), android.R.id.content)
        }
    }
}

/**
 * One checkbox per game. Checked = visible in the home-screen widget.
 * Toggles are persisted immediately; the channel is republished when leaving
 * the screen (from cached data, no network).
 */
class SelectGamesFragment : GuidedStepSupportFragment() {

    private val store by lazy { App.from(requireContext()).hostStore }
    private val games by lazy {
        store.loadGames().sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            getString(R.string.select_games_title),
            getString(R.string.select_games_desc),
            null,
            null,
        )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        for (game in games) {
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(game.appId.toLong())
                    .title(game.appName)
                    .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                    .checked(store.isGameVisible(game.appId))
                    .build(),
            )
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        // For a checkbox check-set the framework has already flipped isChecked.
        store.setGameVisible(action.id.toInt(), action.isChecked)
    }

    override fun onStop() {
        super.onStop()
        // Republish the channel with the new selection using cached games (no network).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ctx = requireContext().applicationContext
            val s = store
            Thread {
                runCatching { TvChannelPublisher(ctx).publish(s, s.loadGames()) }
            }.start()
        }
    }
}
