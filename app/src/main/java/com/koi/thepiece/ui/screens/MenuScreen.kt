package com.koi.thepiece.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
@Composable
fun MenuScreen(
    onGoCatalog: () -> Unit,
    onGoScanner: () -> Unit,
    onGoSettings: () -> Unit,
) {
    val ctx = LocalContext.current

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

    // ask once when entering menu (only if not granted)
    LaunchedEffect(Unit) {
        if (!hasCamPermission) {
            camPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        //show permission status
        Text(if (hasCamPermission) "Camera: Granted " else "Camera: Not granted ")

        Button(onClick = onGoCatalog, modifier = Modifier.fillMaxWidth()) {
            Text("Go to Catalog")
        }

        Button(
            onClick = {
                // If permission not granted, request again instead of going scanner
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
}
