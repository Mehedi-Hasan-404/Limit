package com.livetvpro.app.ui.player.settings

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.livetvpro.app.R
import com.livetvpro.app.utils.DeviceUtils
import timber.log.Timber

/**
 * Player settings shown as a proper Material3 alert dialog.
 *
 * Dialog chrome (rounded card, scrim, elevation, Cancel/Apply buttons) is
 * handled by [MaterialAlertDialogBuilder] + ThemeOverlay.LiveTVPro.Dialog.
 *
 * Body is inflated from R.layout.dialog_player_settings which contains:
 *   • TabLayout    — Video / Audio / Text / Speed
 *   • RecyclerView — animated M3 radio / checkbox track options
 *   • Checkbox row — Auto-play · Mute · HW Decode
 *     (exact SenseiGram style: small 12sp, bold, horizontal, buttonTint = red)
 */
class PlayerSettingsDialog(
    context: Context,
    private val player: ExoPlayer,
    private val onSettingsChanged: ((autoPlay: Boolean, muteOnStart: Boolean, hwDecode: Boolean) -> Unit)? = null
) {

    // ── State ─────────────────────────────────────────────────────────────────

    private var selectedAudio: TrackUiModel.Audio? = null
    private var selectedText: TrackUiModel.Text?  = null
    private var selectedSpeed: Float = 1.0f
    private var selectedVideoQualities = mutableSetOf<TrackUiModel.Video>()

    private var isVideoNone = false; private var isAudioNone = false; private var isTextNone = false
    private var isVideoAuto = true;  private var isAudioAuto = true;  private var isTextAuto = true

    private var videoTracks = listOf<TrackUiModel.Video>()
    private var audioTracks = listOf<TrackUiModel.Audio>()
    private var textTracks  = listOf<TrackUiModel.Text>()

    private var currentAdapter: TrackAdapter<*>? = null
    private var tracksListener: Player.Listener? = null

    private val tabs = mutableListOf<TabEntry>()
    private data class TabEntry(val label: String, val show: () -> Unit)

    // Views — bound after inflation
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var checkAutoPlay: CheckBox
    private lateinit var checkMuteOnStart: CheckBox
    private lateinit var checkHardwareDecode: CheckBox

    // ── Public API ────────────────────────────────────────────────────────────

    fun show(
        context: Context,
        autoPlay: Boolean = false,
        muteOnStart: Boolean = false,
        hardwareDecode: Boolean = true
    ) {
        val body = LayoutInflater.from(context)
            .inflate(R.layout.dialog_player_settings, null, false)

        recyclerView        = body.findViewById(R.id.recyclerView)
        tabLayout           = body.findViewById(R.id.tabLayout)
        checkAutoPlay       = body.findViewById(R.id.checkAutoPlay)
        checkMuteOnStart    = body.findViewById(R.id.checkMuteOnStart)
        checkHardwareDecode = body.findViewById(R.id.checkHardwareDecode)

        checkAutoPlay.isChecked       = autoPlay
        checkMuteOnStart.isChecked    = muteOnStart
        checkHardwareDecode.isChecked = hardwareDecode

        recyclerView.layoutManager = LinearLayoutManager(context)

        val tabListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tabs.getOrNull(tab?.position ?: return)?.show?.invoke()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        }
        tabLayout.addOnTabSelectedListener(tabListener)

        loadTracks()

        if (videoTracks.isEmpty() && audioTracks.isEmpty() && textTracks.isEmpty()) {
            val listener = object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    if (tracks.groups.isNotEmpty()) {
                        player.removeListener(this); tracksListener = null
                        recyclerView.post { loadTracks(); rebuildTabs() }
                    }
                }
            }
            tracksListener = listener
            player.addListener(listener)
        }

        rebuildTabs()
        if (DeviceUtils.isTvDevice) setupTvNavigation()

        MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_LiveTVPro_Dialog)
            .setTitle("Player Settings")
            .setView(body)
            .setNegativeButton("Cancel") { dialog, _ ->
                cleanup(); dialog.dismiss()
            }
            .setPositiveButton("Apply") { dialog, _ ->
                applySelections()
                onSettingsChanged?.invoke(
                    checkAutoPlay.isChecked,
                    checkMuteOnStart.isChecked,
                    checkHardwareDecode.isChecked
                )
                cleanup(); dialog.dismiss()
            }
            .setOnDismissListener { cleanup() }
            .show()
    }

    private fun cleanup() {
        tracksListener?.let { player.removeListener(it) }
        tracksListener = null
    }

    // ── Track loading ─────────────────────────────────────────────────────────

    private fun loadTracks() {
        try {
            val params = player.trackSelectionParameters

            val disabledVideo = params.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
            isVideoNone = disabledVideo
            isVideoAuto = !disabledVideo && !player.currentTracks.groups.any {
                it.type == C.TRACK_TYPE_VIDEO && params.overrides.containsKey(it.mediaTrackGroup)
            }

            val disabledAudio = params.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)
            isAudioNone = disabledAudio
            isAudioAuto = !disabledAudio && !player.currentTracks.groups.any {
                it.type == C.TRACK_TYPE_AUDIO && params.overrides.containsKey(it.mediaTrackGroup)
            }

            val disabledText = params.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
            isTextNone = disabledText
            isTextAuto = !disabledText && !player.currentTracks.groups.any {
                it.type == C.TRACK_TYPE_TEXT && params.overrides.containsKey(it.mediaTrackGroup)
            }

            videoTracks = PlayerTrackMapper.videoTracks(player)
            audioTracks = PlayerTrackMapper.audioTracks(player)
            textTracks  = PlayerTrackMapper.textTracks(player)

            selectedVideoQualities.clear()
            if (!isVideoAuto && !isVideoNone) selectedVideoQualities.addAll(videoTracks.filter { it.isSelected })
            selectedAudio = if (!isAudioAuto && !isAudioNone) audioTracks.firstOrNull { it.isSelected } else null
            selectedText  = if (!isTextAuto  && !isTextNone)  textTracks.firstOrNull  { it.isSelected } else null
            selectedSpeed = player.playbackParameters.speed
        } catch (e: Exception) { Timber.e(e, "Error loading tracks") }
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private fun rebuildTabs() {
        tabs.clear(); tabLayout.removeAllTabs()
        if (videoTracks.isNotEmpty()) tabs.add(TabEntry("Video") { showVideoTracks() })
        if (audioTracks.isNotEmpty()) tabs.add(TabEntry("Audio") { showAudioTracks() })
        if (textTracks.isNotEmpty())  tabs.add(TabEntry("Text")  { showTextTracks()  })
        tabs.add(TabEntry("Speed") { showSpeedOptions() })
        tabs.forEach { tabLayout.addTab(tabLayout.newTab().setText(it.label)) }
        tabs.firstOrNull()?.show?.invoke()
        if (DeviceUtils.isTvDevice) tabLayout.post { tabLayout.requestFocus() }
    }

    private fun showVideoTracks() {
        val adapter = TrackAdapter<TrackUiModel.Video> { selected ->
            when (selected.groupIndex) {
                -1 -> { selectedVideoQualities.clear(); isVideoAuto = true;  isVideoNone = false }
                -2 -> { selectedVideoQualities.clear(); isVideoAuto = false; isVideoNone = true  }
                else -> {
                    isVideoAuto = false; isVideoNone = false
                    val key = selectedVideoQualities.find { it.groupIndex == selected.groupIndex && it.trackIndex == selected.trackIndex }
                    if (key != null) selectedVideoQualities.remove(key) else selectedVideoQualities.add(selected)
                    if (selectedVideoQualities.isEmpty()) isVideoAuto = true
                }
            }
            (currentAdapter as? TrackAdapter<TrackUiModel.Video>)?.submit(buildVideoList())
        }
        adapter.submit(buildVideoList()); recyclerView.adapter = adapter; currentAdapter = adapter
    }

    private fun buildVideoList(): List<TrackUiModel.Video> {
        val list = mutableListOf(
            TrackUiModel.Video(-1, -1, 0, 0, 0, isSelected = isVideoAuto, isRadio = true),
            TrackUiModel.Video(-2, -2, 0, 0, 0, isSelected = isVideoNone, isRadio = true)
        )
        val useRadio = videoTracks.size == 1
        list.addAll(videoTracks.map { t ->
            val checked = selectedVideoQualities.any { it.groupIndex == t.groupIndex && it.trackIndex == t.trackIndex }
            t.copy(isSelected = !isVideoAuto && !isVideoNone && checked, isRadio = useRadio)
        })
        return list
    }

    private fun showAudioTracks() {
        val adapter = TrackAdapter<TrackUiModel.Audio> { selected ->
            when (selected.groupIndex) {
                -1 -> { selectedAudio = null; isAudioNone = false; isAudioAuto = true  }
                -2 -> { selectedAudio = null; isAudioNone = true;  isAudioAuto = false }
                else -> { selectedAudio = selected; isAudioNone = false; isAudioAuto = false }
            }
            (currentAdapter as? TrackAdapter<TrackUiModel.Audio>)?.updateSelection(selected)
        }
        adapter.submit(buildAudioList()); recyclerView.adapter = adapter; currentAdapter = adapter
    }

    private fun buildAudioList() = mutableListOf(
        TrackUiModel.Audio(-1, -1, "Auto", 0, 0, isSelected = isAudioAuto),
        TrackUiModel.Audio(-2, -2, "None", 0, 0, isSelected = isAudioNone)
    ).also { list -> list.addAll(audioTracks.map { t -> t.copy(isSelected = !isAudioAuto && !isAudioNone && t.isSelected) }) }

    private fun showTextTracks() {
        val adapter = TrackAdapter<TrackUiModel.Text> { selected ->
            when (selected.groupIndex) {
                -1 -> { selectedText = null; isTextNone = false; isTextAuto = true  }
                -2 -> { selectedText = null; isTextNone = true;  isTextAuto = false }
                else -> { selectedText = selected; isTextNone = false; isTextAuto = false }
            }
            (currentAdapter as? TrackAdapter<TrackUiModel.Text>)?.updateSelection(selected)
        }
        adapter.submit(buildTextList()); recyclerView.adapter = adapter; currentAdapter = adapter
    }

    private fun buildTextList() = mutableListOf(
        TrackUiModel.Text(-1, -1, "Auto", isSelected = isTextAuto),
        TrackUiModel.Text(-2, -2, "None", isSelected = isTextNone)
    ).also { list -> list.addAll(textTracks.map { t -> t.copy(isSelected = !isTextAuto && !isTextNone && t.isSelected) }) }

    private fun showSpeedOptions() {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        val adapter = TrackAdapter<TrackUiModel.Speed> { selected ->
            selectedSpeed = selected.speed
            (currentAdapter as? TrackAdapter<TrackUiModel.Speed>)?.updateSelection(selected)
        }
        adapter.submit(speeds.map { TrackUiModel.Speed(it, isSelected = it == selectedSpeed) })
        recyclerView.adapter = adapter; currentAdapter = adapter
    }

    // ── Apply ─────────────────────────────────────────────────────────────────

    private fun applySelections() {
        try {
            TrackSelectionApplier.applyMultipleVideo(
                player       = player,
                videoTracks  = if (isVideoAuto || isVideoNone) emptyList() else selectedVideoQualities.toList(),
                audio        = selectedAudio,
                text         = selectedText,
                disableVideo = isVideoNone,
                disableAudio = isAudioNone,
                disableText  = isTextNone
            )
            player.setPlaybackSpeed(selectedSpeed)
        } catch (e: Exception) { Timber.e(e, "Error applying selections") }
    }

    // ── TV D-pad ──────────────────────────────────────────────────────────────

    private fun setupTvNavigation() {
        recyclerView.isFocusable = true
        recyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        recyclerView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                tabLayout.getTabAt(tabLayout.selectedTabPosition)?.view?.requestFocus(); true
            } else false
        }

        tabLayout.setOnKeyListener { _, keyCode, event ->
            when {
                event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                    recyclerView.requestFocus(); true
                }
                event.action == KeyEvent.ACTION_UP &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -> {
                    val count = tabLayout.tabCount; if (count == 0) return@setOnKeyListener false
                    val current = tabLayout.selectedTabPosition
                    val next = if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) (current + 1).coerceAtMost(count - 1)
                               else (current - 1).coerceAtLeast(0)
                    tabLayout.selectTab(tabLayout.getTabAt(next)); true
                }
                else -> false
            }
        }
    }
}
