package com.livetvpro.app.ui.player.settings

/**
 * UI model for each row in the player-settings RecyclerView.
 *
 * isRadio = true  → row shows a MaterialRadioButton  (single-select: Auto / None)
 * isRadio = false → row shows a MaterialCheckBox     (multi-select: specific tracks)
 *
 * The widget itself is clickable=false / focusable=false — the entire row LinearLayout
 * handles the click, exactly as in the workflow's dialog_quality.xml + MainActivity.kt.
 */
sealed class TrackUiModel {
    abstract val isSelected: Boolean
    abstract val isRadio: Boolean

    data class Video(
        val groupIndex: Int,
        val trackIndex: Int,
        val width: Int,
        val height: Int,
        val bitrate: Int,
        override val isSelected: Boolean,
        override val isRadio: Boolean = true      // Auto/None rows are radio; specific tracks are checkbox
    ) : TrackUiModel()

    data class Audio(
        val groupIndex: Int,
        val trackIndex: Int,
        val language: String,
        val channels: Int,
        val bitrate: Int,
        override val isSelected: Boolean,
        override val isRadio: Boolean = true
    ) : TrackUiModel()

    data class Text(
        val groupIndex: Int?,
        val trackIndex: Int?,
        val language: String,
        override val isSelected: Boolean,
        override val isRadio: Boolean = true
    ) : TrackUiModel()

    data class Speed(
        val speed: Float,
        override val isSelected: Boolean,
        override val isRadio: Boolean = true
    ) : TrackUiModel()
}
