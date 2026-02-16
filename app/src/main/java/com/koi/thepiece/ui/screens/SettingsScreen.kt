package com.koi.thepiece.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.koi.thepiece.AudioManager
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    audio: AudioManager,
    onBack: () -> Unit
) {
    var master by remember { mutableStateOf(audio.getMasterVolume()) }
    var bgm by remember { mutableStateOf(audio.getBgmVolume()) }
    var sfx by remember { mutableStateOf(audio.getSfxVolume()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }

        VolumeSlider(
            title = "Master Volume",
            value = master,
            onValueChange = {
                master = it
                audio.setMasterVolume(it)
            }
        )

        VolumeSlider(
            title = "BGM Volume",
            value = bgm,
            onValueChange = {
                bgm = it
                audio.setBgmVolume(it)
            }
        )

        VolumeSlider(
            title = "SFX Volume",
            value = sfx,
            onValueChange = {
                sfx = it
                audio.setSfxVolume(it)
            }
        )

        Divider()

    }
}

@Composable
private fun VolumeSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${(value * 100).roundToInt()}%")
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}
