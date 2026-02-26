package com.livetvpro.app.ui.player.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton
import com.livetvpro.app.R

/**
 * Adapter for the player-settings track list.
 *
 * Each row is a full-width clickable LinearLayout (background=selectableItemBackground).
 * The widget on the right (RadioButton or CheckBox) is clickable=false / focusable=false —
 * the row itself is the sole touch target, matching the workflow pattern exactly.
 *
 * Both widgets use custom vector drawables that hardcode #EF4444, bypassing
 * Material3's internal color engine which ignores buttonTint/colorPrimary:
 *   RadioButton → android:button="@drawable/radio_button_selector"
 *   CheckBox    → android:button="@drawable/checkbox_selector"
 *                  (checkbox_checked.xml tint=#EF4444, checkbox_unchecked.xml tint=#FFFFFF)
 */
class TrackAdapter<T : TrackUiModel>(
    private val onSelect: (T) -> Unit
) : RecyclerView.Adapter<TrackAdapter<T>.VH>() {

    private val items = mutableListOf<T>()

    fun submit(list: List<T>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun updateSelection(selectedItem: T) {
        items.forEachIndexed { index, item ->
            val wasSelected = item.isSelected
            val isNowSelected = item == selectedItem

            @Suppress("UNCHECKED_CAST")
            val updated = when (item) {
                is TrackUiModel.Video -> item.copy(isSelected = isNowSelected)
                is TrackUiModel.Audio -> item.copy(isSelected = isNowSelected)
                is TrackUiModel.Text  -> item.copy(isSelected = isNowSelected)
                is TrackUiModel.Speed -> item.copy(isSelected = isNowSelected)
                else                  -> item
            } as T

            items[index] = updated
            if (wasSelected != isNowSelected) notifyItemChanged(index)
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        private val tvPrimary:   TextView            = view.findViewById(R.id.tvPrimary)
        private val tvSecondary: TextView            = view.findViewById(R.id.tvSecondary)
        private val radioToggle: MaterialRadioButton = view.findViewById(R.id.radioToggle)
        private val checkToggle: MaterialCheckBox    = view.findViewById(R.id.checkToggle)

        private var currentItem: T? = null

        init {
            itemView.setOnClickListener {
                val item = currentItem ?: return@setOnClickListener
                onSelect(item)
            }
        }

        fun bind(item: T) {
            currentItem = item
            tvSecondary.visibility = View.VISIBLE

            // Show the correct widget, set checked state
            if (item.isRadio) {
                radioToggle.visibility = View.VISIBLE
                checkToggle.visibility = View.GONE
                radioToggle.isChecked  = item.isSelected
            } else {
                radioToggle.visibility = View.GONE
                checkToggle.visibility = View.VISIBLE
                checkToggle.isChecked  = item.isSelected
            }

            when (item) {
                is TrackUiModel.Video -> {
                    when {
                        item.groupIndex == -1 -> {
                            tvPrimary.text         = "Auto"
                            tvSecondary.visibility = View.GONE
                        }
                        item.groupIndex == -2 -> {
                            tvPrimary.text         = "None"
                            tvSecondary.visibility = View.GONE
                        }
                        else -> {
                            val res     = if (item.width > 0 && item.height > 0) "${item.width} × ${item.height}" else "Unknown"
                            val bitrate = if (item.bitrate > 0) "${"%.2f".format(item.bitrate / 1_000_000f)} Mbps" else ""
                            tvPrimary.text         = if (bitrate.isNotEmpty()) "$res • $bitrate" else res
                            tvSecondary.visibility = View.GONE
                        }
                    }
                }
                is TrackUiModel.Audio -> {
                    when {
                        item.groupIndex == -1 -> {
                            tvPrimary.text   = "Auto"
                            tvSecondary.text = "Automatic audio"
                        }
                        item.groupIndex == -2 -> {
                            tvPrimary.text   = "None"
                            tvSecondary.text = "No audio"
                        }
                        else -> {
                            val ch = when (item.channels) {
                                1 -> " • Mono"; 2 -> " • Stereo"
                                6 -> " • Surround 5.1"; 8 -> " • Surround 7.1"
                                else -> if (item.channels > 0) " • ${item.channels}ch" else ""
                            }
                            tvPrimary.text   = "${item.language}$ch"
                            tvSecondary.text = if (item.bitrate > 0) "${item.bitrate / 1000} kbps" else ""
                        }
                    }
                }
                is TrackUiModel.Text -> {
                    when {
                        item.groupIndex == -1 -> {
                            tvPrimary.text   = "Auto"
                            tvSecondary.text = "Automatic subtitles"
                        }
                        item.groupIndex == -2 -> {
                            tvPrimary.text   = "None"
                            tvSecondary.text = "No subtitles"
                        }
                        else -> {
                            tvPrimary.text   = item.language
                            tvSecondary.text = "Subtitles"
                        }
                    }
                }
                is TrackUiModel.Speed -> {
                    tvPrimary.text   = "${item.speed}x"
                    tvSecondary.text = "Playback speed"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track_option, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size
}
