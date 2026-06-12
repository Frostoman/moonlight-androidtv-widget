package com.androidtv.gameswidget.ui

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.androidtv.gameswidget.R

data class UiAction(val id: Int, val label: String) {
    companion object {
        const val ADD_PC = 1
        const val RESYNC = 2
        const val UNPAIR = 3
        const val RENAME = 4
        const val SELECT = 5
    }
}

/** Renders an action (add PC, resync, unpair) as a simple card. */
class ActionCardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val card = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(300, 180)
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val action = item as UiAction
        val card = viewHolder.view as ImageCardView
        card.titleText = action.label
        card.mainImageView.setImageDrawable(
            ContextCompat.getDrawable(card.context, R.drawable.default_box_art),
        )
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
}
