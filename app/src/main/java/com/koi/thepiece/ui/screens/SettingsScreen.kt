package com.koi.thepiece.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.components.SfxButton
import kotlin.math.roundToInt
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Settings screen UI for audio configuration.
 *
 * This screen provides:
 * - A dropdown selector for choosing the background music (BGM) track.
 * - Sliders for Master / BGM / SFX volumes (all in range [0, 1]).
 * - A "Back" button that plays a click SFX before navigating back.
 *
 * All values displayed here are sourced from [AudioManager] StateFlows, meaning:
 * - UI reflects persisted settings (as restored by the AudioManager).
 * - Changing sliders writes new values via AudioManager setters.
 *
 * @param audio Instance of [AudioManager] that exposes reactive audio state and setters.
 * @param onBack Callback triggered when user taps the back button.
 */
@Composable
fun SettingsScreen(
    audio: AudioManager,
    onBack: () -> Unit
) {
    // These always reflect the persisted values
    val master by audio.masterState.collectAsState()
    val bgm by audio.bgmState.collectAsState()
    val sfx by audio.sfxState.collectAsState()

    // =====================
    // Observe selected BGM
    // =====================
    /**
     * Current selected BGM id (e.g. "default", "OP9").
     * This is persisted by AudioManager and also used to control playback.
     */
    val selectedBgmId by audio.selectedBgmIdState.collectAsState()

    // Get available tracks
    /**
     * Available BGM tracks exposed by AudioManager.
     *
     * `remember { ... }` is used to avoid repeatedly constructing the list on recomposition.
     * Assumption: track list is static for the lifetime of this screen.
     */
    val tracks = remember { audio.getBgmTracks() }

    // Get title of selected track
    /**
     * The title shown in the dropdown text field.
     * Falls back to first track title, then "Unknown" if no tracks exist.
     */
    val selectedTitle = tracks.firstOrNull { it.id == selectedBgmId }?.title
        ?: tracks.firstOrNull()?.title
        ?: "Unknown"

    // Root container for stacking main content and floating back button.
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.titleLarge)

            // =====================
            // BGM dropdown selector
            // =====================
                /**
             * Dropdown selector for background music.
             * When a track is selected, AudioManager persists the id and switches playback.
             */
            BgmPicker(
                title = "Background Music",
                selectedTitle = selectedTitle,
                tracks = tracks,
                onSelect = { id ->
                    audio.setSelectedBgm(id, loop = true)
                }
            )
            /**
             * Master volume slider.
             * This affects both BGM and SFX because effective volume is computed in AudioManager
             * using masterVolume * channelVolume.
             */
            VolumeSlider(
                title = "Master Volume",
                value = master,
                onValueChange = { audio.setMasterVolume(it) }
            )
            /**
             * Background music volume slider.
             * Affects only BGM channel output.
             */
            VolumeSlider(
                title = "BGM Volume",
                value = bgm,
                onValueChange = { audio.setBgmVolume(it) }
            )
            /**
             * Sound effect volume slider.
             * Affects currently playing and future SFX playback.
             */
            VolumeSlider(
                title = "SFX Volume",
                value = sfx,
                onValueChange = { audio.setSfxVolume(it) }
            )

            Divider()
        }

        SfxButton(
            audio = audio,
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}

// =====================
// BGM dropdown UI component
// =====================

/**
 * Dropdown UI component for selecting a background music track.
 *
 * Uses Material3 "ExposedDropdownMenuBox" pattern:
 * - An [OutlinedTextField] acts as a non-editable anchor.
 * - A dropdown menu lists all track titles.
 *
 * @param title Label shown above the dropdown field.
 * @param selectedTitle Currently displayed track title.
 * @param tracks List of selectable tracks (id + title + resId).
 * @param onSelect Callback returning the selected track id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BgmPicker(
    title: String,
    selectedTitle: String,
    tracks: List<AudioManager.BgmTrack>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedTitle,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text("Select track") }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                tracks.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t.title) },
                        onClick = {
                            expanded = false
                            onSelect(t.id)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Reusable volume slider row used by the settings screen.
 *
 * UI layout:
 * - First row: title on the left + percentage text on the right.
 * - Second row: a [Slider] with [valueRange] fixed to 0..1.
 *
 * @param title Label shown on the left (e.g. "Master Volume").
 * @param value Current slider value in [0, 1].
 * @param onValueChange Callback invoked continuously as user drags the slider.
 */
@Composable
private fun VolumeSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Text("${(value * 100).roundToInt()}%")
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}
