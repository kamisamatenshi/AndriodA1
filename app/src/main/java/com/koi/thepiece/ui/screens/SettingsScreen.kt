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

@Composable
fun SettingsScreen(
    audio: AudioManager,
    onBack: () -> Unit
) {
    // These always reflect the persisted values
    val master by audio.masterState.collectAsState()
    val bgm by audio.bgmState.collectAsState()
    val sfx by audio.sfxState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.titleLarge)

            VolumeSlider(
                title = "Master Volume",
                value = master,
                onValueChange = { audio.setMasterVolume(it) }
            )

            VolumeSlider(
                title = "BGM Volume",
                value = bgm,
                onValueChange = { audio.setBgmVolume(it) }
            )

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
