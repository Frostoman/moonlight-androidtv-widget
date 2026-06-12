package com.androidtv.gameswidget.ui

import android.graphics.BitmapFactory
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.androidtv.gameswidget.R
import com.androidtv.gameswidget.data.HostStore
import com.androidtv.gameswidget.net.NvApp

/** Renders a game as a poster card with its box art (or a placeholder). */
class GameCardPresenter(private val store: HostStore) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val card = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val game = item as NvApp
        val card = viewHolder.view as ImageCardView
        card.titleText = game.appName
        card.contentText = if (game.hdrSupported) "HDR" else ""

        val file = store.boxArtFile(game.appId)
        val bmp = if (file.exists() && file.length() > 0) {
            runCatching {
                BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = 1 })
            }.getOrNull()
        } else null

        if (bmp != null) {
            card.mainImageView.setImageBitmap(bmp)
        } else {
            card.mainImageView.setImageDrawable(
                ContextCompat.getDrawable(card.context, R.drawable.default_box_art),
            )
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        (viewHolder.view as ImageCardView).mainImage = null
    }

    private companion object {
        const val CARD_WIDTH = 213
        const val CARD_HEIGHT = 320
    }
}
