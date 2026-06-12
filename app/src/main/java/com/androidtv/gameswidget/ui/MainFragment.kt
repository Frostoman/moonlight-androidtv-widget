package com.androidtv.gameswidget.ui

import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.lifecycle.lifecycleScope
import com.androidtv.gameswidget.App
import com.androidtv.gameswidget.R
import com.androidtv.gameswidget.launch.MoonlightLauncher
import com.androidtv.gameswidget.net.NvApp
import com.androidtv.gameswidget.sync.SyncManager
import kotlinx.coroutines.launch

class MainFragment : BrowseSupportFragment() {

    private lateinit var rowsAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.browse_title)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter = rowsAdapter
        setupListeners()
        rebuildRows()
    }

    override fun onResume() {
        super.onResume()
        rebuildRows()
    }

    private fun rebuildRows() {
        val app = App.from(requireContext())
        val store = app.hostStore
        rowsAdapter.clear()

        if (store.isConfigured) {
            val games = store.loadGames()
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            val gameAdapter = ArrayObjectAdapter(GameCardPresenter(store))
            games.forEach { gameAdapter.add(it) }
            val header = store.pcName ?: getString(R.string.row_games)
            rowsAdapter.add(ListRow(HeaderItem(0, header), gameAdapter))
        }

        val actions = ArrayObjectAdapter(ActionCardPresenter()).apply {
            if (store.isConfigured) {
                add(UiAction(UiAction.RESYNC, getString(R.string.action_resync)))
                add(UiAction(UiAction.SELECT, getString(R.string.action_select_games)))
                add(UiAction(UiAction.RENAME, getString(R.string.action_rename)))
                add(UiAction(UiAction.UNPAIR, getString(R.string.action_unpair)))
            }
            add(UiAction(UiAction.ADD_PC, getString(R.string.action_add_pc)))
        }
        rowsAdapter.add(ListRow(HeaderItem(1, getString(R.string.row_settings)), actions))
    }

    private fun setupListeners() {
        setOnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is NvApp -> launchGame(item)
                is UiAction -> handleAction(item)
            }
        }
    }

    private fun launchGame(game: NvApp) {
        val store = App.from(requireContext()).hostStore
        val uuid = store.pcUuid
        if (uuid == null) {
            toast(getString(R.string.no_pc)); return
        }
        if (!MoonlightLauncher.isMoonlightInstalled(requireContext())) {
            toast(getString(R.string.moonlight_missing)); return
        }
        val ok = MoonlightLauncher.launch(requireContext(), uuid, store.pcName, game.appId, game.appName)
        if (!ok) toast(getString(R.string.moonlight_missing))
    }

    private fun handleAction(action: UiAction) {
        when (action.id) {
            UiAction.ADD_PC -> startActivity(android.content.Intent(requireContext(), PairActivity::class.java))
            UiAction.UNPAIR -> {
                App.from(requireContext()).hostStore.clear()
                rebuildRows()
                toast(getString(R.string.toast_unpaired))
            }
            UiAction.RESYNC -> resync()
            UiAction.RENAME -> startActivity(android.content.Intent(requireContext(), RenameChannelActivity::class.java))
            UiAction.SELECT -> startActivity(android.content.Intent(requireContext(), SelectGamesActivity::class.java))
        }
    }

    private fun resync() {
        toast(getString(R.string.action_resync))
        lifecycleScope.launch {
            when (val r = SyncManager(requireContext().applicationContext).sync()) {
                is SyncManager.Result.Success -> { rebuildRows(); toast(getString(R.string.toast_synced, r.games.size)) }
                is SyncManager.Result.Error -> toast(r.message)
                SyncManager.Result.NotConfigured -> toast(getString(R.string.no_pc))
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
