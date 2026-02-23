package com.koi.thepiece.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.koi.thepiece.AppGraph
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.R
import com.koi.thepiece.ui.components.SfxButton
import com.koi.thepiece.ui.components.SfxFAB
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main menu screen providing navigation entry points to the application's core features.
 *
 * Responsibilities:
 * - Displays the menu hub for feature navigation:
 *   - Deck list
 *   - Catalog
 *   - OCR card scanner (requires camera permission)
 *   - Settings
 * - Requests and displays camera permission status.
 * - Provides global toggles:
 *   - Mute/unmute SFX and BGM (via AudioManager)
 *   - Light/Dark theme (callback controlled by parent)
 * - Provides logout capability (dialog confirmation + token deletion).
 *
 * UX behaviors:
 * - Staggered fade-in animation sequence for menu elements.
 * - Back button triggers logout confirmation only after interactions are enabled.
 * - Back presses are throttled to prevent repeated dialog spam.
 *
 * Security behavior:
 * - Logout clears the locally stored session token (TokenStore) and triggers navigation
 *   back to the login flow via onLogoutConfirmed().
 */
@Composable
fun MenuScreen(
    audioManager: AudioManager,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onGoDeckList: () -> Unit,
    onGoCatalog: () -> Unit,
    onGoScanner: () -> Unit,
    onGoSettings: () -> Unit,
    onLogoutConfirmed :()->Unit
) {
    val ctx = LocalContext.current

    // Logout dialog / interaction state
    var showLogoutDialog by rememberSaveable  { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var logoutBusy by rememberSaveable { mutableStateOf(false) }
    var interactionsReady by remember { mutableStateOf(false) }
    var logoutEnabled by remember { mutableStateOf(false) }

    // Back press throttling to prevent repeated toggles
    var backLocked by remember { mutableStateOf(false) }

    /**
     * Ensures UI has rendered at least one frame before allowing sensitive interactions.
     * This prevents accidental immediate logout prompt when landing on the menu.
     */
    LaunchedEffect(Unit) {
        kotlinx.coroutines.android.awaitFrame()
        interactionsReady = true
    }

    /**
     * Back press lock reset.
     * This provides simple debouncing of the BackHandler.
     */
    LaunchedEffect(backLocked) {
        if (backLocked) {
            delay(350)
            backLocked = false
        }
    }

    /**
     * Back button triggers logout confirmation when enabled.
     * - Disabled while logout dialog is visible.
     * - Throttled using backLocked.
     */
    BackHandler(enabled = logoutEnabled && !showLogoutDialog) {
        if (backLocked) return@BackHandler
        backLocked = true
        showLogoutDialog = true
    }

    /**
     * Logout confirmation dialog.
     * On confirm:
     * - Clears the stored auth token from TokenStore.
     * - Calls onLogoutConfirmed() for navigation reset (handled by parent/back stack).
     */
    if (showLogoutDialog&&logoutEnabled) {
        AlertDialog(
            onDismissRequest = { if (!logoutBusy) showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Do you want to logout?") },
            confirmButton = {
                TextButton(
                    enabled = !logoutBusy,
                    onClick = {
                        if (logoutBusy) return@TextButton
                        logoutBusy = true
                        showLogoutDialog = false
                        scope.launch {
                            AppGraph.provideTokenStore(ctx).clearToken()
                            onLogoutConfirmed()
                        }
                    }
                ) { Text("Logout") }
            },
            dismissButton = {
                TextButton(
                    enabled = !logoutBusy,
                    onClick = { showLogoutDialog = false }
                ) { Text("Cancel") }
            }
        )
    }

    // Mute state is mirrored locally for icon rendering
    var isMuted by remember { mutableStateOf(audioManager.isMuted()) }

    /**
     * Camera permission state.
     * Used for:
     * - Displaying status text
     * - Blocking navigation to scanner until permission is granted
     */
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    /**
     * Permission launcher for camera access.
     * Updates hasCamPermission once user responds to the permission prompt.
     */
    val camPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCamPermission = granted
    }

    /**
     * Initial camera permission request.
     * If permission is not granted yet, request it once on screen entry.
     */
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

    /**
     * Staged animation sequence to reduce perceived UI clutter.
     * logoutEnabled is only enabled at the end to avoid early dialog triggers.
     */
    LaunchedEffect(Unit) {
        delay(120); showImage = true
        delay(120); showCamText = true
        delay(120); showBtn1 = true
        delay(120); showBtn2 = true
        delay(120); showBtn3 = true
        delay(120); showBtn4 = true
        delay(150); showDisclaimer = true

        logoutEnabled = true
    }

    /** Standard fade-in spec reused for menu elements. */
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
        // Top-left Logout
        AnimatedVisibility(visible = logoutEnabled, enter = fadeIn(tween(250))) {

            SfxFAB(
                audio = audioManager,
                onClick = {
                    if (!interactionsReady || !logoutEnabled) return@SfxFAB
                    showLogoutDialog = true
                },   // reuse your dialog
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout"
                )
            }
        }
        // Mute Button (Bottom RIGHT)
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