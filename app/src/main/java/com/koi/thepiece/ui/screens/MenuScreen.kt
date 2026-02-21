package com.koi.thepiece.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.R
import com.koi.thepiece.ui.components.SfxButton
import com.koi.thepiece.ui.components.SfxFAB
import kotlinx.coroutines.delay

@Composable
fun MenuScreen(
    audioManager: AudioManager,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onGoDeckList: () -> Unit,
    onGoCatalog: () -> Unit,
    onGoScanner: () -> Unit,
    onGoSettings: () -> Unit,
    onBack: ()-> Unit
) {
    val ctx = LocalContext.current

    var isMuted by remember { mutableStateOf(audioManager.isMuted()) }

    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val camPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCamPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) {
            camPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // -----------------------------
    // Staggered fade-in steps
    // -----------------------------
    var showImage by remember { mutableStateOf(false) }
    var showCamText by remember { mutableStateOf(false) }
    var showBtn1 by remember { mutableStateOf(false) }
    var showBtn2 by remember { mutableStateOf(false) }
    var showBtn3 by remember { mutableStateOf(false) }
    var showBtn4 by remember { mutableStateOf(false) }
    var showDisclaimer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(120); showImage = true
        delay(120); showCamText = true
        delay(120); showBtn1 = true
        delay(120); showBtn2 = true
        delay(120); showBtn3 = true
        delay(120); showBtn4 = true
        delay(150); showDisclaimer = true
    }

    fun fadeSpec() = fadeIn(animationSpec = tween(durationMillis = 250))

    Box(modifier = Modifier.fillMaxSize()) {

        // CENTER CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Image first
            AnimatedVisibility(visible = showImage, enter = fadeSpec()) {
                Image(
                    painter = painterResource(id = R.drawable.menucompass),
                    contentDescription = "Menu Logo",
                    modifier = Modifier
                        .height(280.dp)
                        .fillMaxWidth()
                )
            }

            // Camera text BELOW image, ABOVE first button
            AnimatedVisibility(visible = showCamText, enter = fadeSpec()) {
                Text(
                    text = if (hasCamPermission) "Camera: Granted" else "Camera: Not granted",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(visible = showBtn1, enter = fadeSpec()) {
                SfxButton(
                    audio = audioManager,
                    onClick = onGoDeckList,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Deck List")
                }
            }

            AnimatedVisibility(visible = showBtn2, enter = fadeSpec()) {
                SfxButton(
                    audio = audioManager,
                    onClick = onGoCatalog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Catalog")
                }
            }

            AnimatedVisibility(visible = showBtn3, enter = fadeSpec()) {
                SfxButton(
                    audio = audioManager,
                    onClick = {
                        if (!hasCamPermission) {
                            camPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            onGoScanner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan One Piece Card (OCR)")
                }
            }

            AnimatedVisibility(visible = showBtn4, enter = fadeSpec()) {
                SfxButton(
                    audio = audioManager,
                    onClick = onGoSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Settings")
                }
            }

            // Disclaimer directly BELOW the settings button
            AnimatedVisibility(visible = showDisclaimer, enter = fadeSpec()) {
                Text(
                    text = "This application is developed solely for educational purposes. All art assets, characters, audio, and related intellectual property belong to Bandai Namco Entertainment Inc. This project is not affiliated with, endorsed by, or sponsored by Bandai Namco Entertainment Inc. No copyright infringement is intended.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Mute Button (TOP RIGHT)
        SfxFAB(
            audio = audioManager,
            onClick = {
                audioManager.toggleMute()
                isMuted = audioManager.isMuted()
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute"
            )
        }

        // Light / Dark Toggle (BOTTOM RIGHT)
        SfxFAB(
            audio = audioManager,
            onClick = onToggleTheme,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription =
                    if (darkTheme) "Switch to Light Mode" else "Switch to Dark Mode"
            )
        }
    }
}