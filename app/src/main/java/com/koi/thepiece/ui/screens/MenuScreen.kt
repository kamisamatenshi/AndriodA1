package com.koi.thepiece.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.koi.thepiece.AudioManager

@Composable
fun MenuScreen(
    audioManager: AudioManager,
    onGoCatalog: () -> Unit,
    onGoScanner: () -> Unit,
    onGoSettings: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (hasCamPermission) "Camera: Granted" else "Camera: Not granted ⚠️")


            Button(onClick = onGoCatalog, modifier = Modifier.fillMaxWidth()) {
                Text("Go to Catalog")
            }

            Button(
                onClick = {
                    if (!hasCamPermission) {
                        camPermissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        onGoScanner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go to Scanner")
            }

            Button(onClick = onGoSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Go to Setting")
            }
        }

        Button(
            onClick = {
                audioManager.toggleMute()
                isMuted = audioManager.isMuted()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute"
            )
        }
    }
}
