package com.livetvpro.app.ui.player.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.livetvpro.app.R

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
            val isSelected  = item == selectedItem

            @Suppress("UNCHECKED_CAST")
            val updatedItem = when (item) {
                is TrackUiModel.Video -> (item as TrackUiModel.Video).copy(isSelected = isSelected)
                is TrackUiModel.Audio -> (item as TrackUiModel.Audio).copy(isSelected = isSelected)
                is TrackUiModel.Text  -> (item as TrackUiModel.Text).copy(isSelected = isSelected)
                is TrackUiModel.Speed -> (item as TrackUiModel.Speed).copy(isSelected = isSelected)
                else -> item
            } as T

            items[index] = updatedItem
            if (wasSelected != isSelected) notifyItemChanged(index)
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        private val tvPrimary:   TextView    = view.findViewById(R.id.tvPrimary)
        private val tvSecondary: TextView    = view.findViewById(R.id.tvSecondary)
        private val radioToggle: RadioButton = view.findViewById(R.id.radioToggle)
        private val checkToggle: CheckBox    = view.findViewById(R.id.checkToggle)

        private var currentItem: T? = null

        init {
            view.setOnClickListener {
                val item = currentItem ?: return@setOnClickListener
                onSelect(item)
            }
        }

        fun bind(item: T) {
            currentItem = item
            tvSecondary.visibility = View.VISIBLE

            // Show radio or checkbox depending on isRadio flag
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
                        item.groupIndex == -2 -> {
                            tvPrimary.text = "None"
                            tvSecondary.visibility = View.GONE
                        }
                        item.groupIndex == -1 -> {
                            tvPrimary.text = "Auto"
                            tvSecondary.visibility = View.GONE
                        }
                        else -> {
                            val quality = if (item.width > 0 && item.height > 0)
                                "${item.width} × ${item.height}" else "Unknown"
                            val bitrate = if (item.bitrate > 0)
                                "${"%.2f".format(item.bitrate / 1_000_000f)} Mbps" else ""
                            tvPrimary.text = if (bitrate.isNotEmpty()) "$quality • $bitrate" else quality
                            tvSecondary.visibility = View.GONE
                        }
                    }
                }
                is TrackUiModel.Audio -> {
                    when {
                        item.groupIndex == -2 -> {
                            tvPrimary.text   = "None"
                            tvSecondary.text = "No audio"
                        }
                        item.groupIndex == -1 -> {
                            tvPrimary.text   = "Auto"
                            tvSecondary.text = "Automatic audio"
                        }
                        else -> {
                            val channels = when (item.channels) {
                                1 -> " • Mono"; 2 -> " • Stereo"
                                6 -> " • Surround 5.1"; 8 -> " • Surround 7.1"
                                else -> if (item.channels > 0) " • ${item.channels}ch" else ""
                            }
                            tvPrimary.text   = "${item.language}$channels"
                            tvSecondary.text = if (item.bitrate > 0) "${item.bitrate / 1000} kbps" else ""
                        }
                    }
                }
                is TrackUiModel.Text -> {
                    when {
                        item.groupIndex == -2 -> {
                            tvPrimary.text   = "None"
                            tvSecondary.text = "No subtitles"
                        }
                        item.groupIndex == -1 -> {
                            tvPrimary.text   = "Auto"
                            tvSecondary.text = "Automatic subtitles"
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
    override fun getItemCount() = items.size
}
