package com.koi.thepiece.ui.screens.Loginscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.koi.thepiece.R
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.components.SfxFAB
import com.koi.thepiece.ui.screens.catalogscreen.LoginViewModelFactory
import kotlinx.coroutines.delay
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext

/**
 * Identifies which authentication panel is currently shown within the login screen.
 * - BUTTONS: Shows initial Login / Register buttons and any session status messages.
 * - LOGIN: Shows the login form panel.
 * - REGISTER: Shows the register form panel.
 */
enum class AuthPanel { BUTTONS, LOGIN, REGISTER }

/**
 * Login and registration entry screen.
 *
 * Responsibilities:
 * - Displays an animated landing view with an application logo and legal disclaimer.
 * - Provides authentication UI:
 *   - Session check / error messaging
 *   - Login form
 *   - Registration form
 * - Coordinates navigation to the main menu after successful authentication.
 * - Exposes quick-access toggles for:
 *   - SFX mute/unmute
 *   - Light/Dark theme mode
 *
 * State management:
 * - UI state is driven by LoginViewModel.uiState.
 * - Local UI effects (intro animation and mute state) are held as Compose states.
 *
 * Navigation:
 * - Collects LoginViewModel.goNext; when emitted, triggers onGoToMainmenu().
 */
@Composable
fun LoginScreen(
    audioManager: AudioManager,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onGoToMainmenu: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(context))

    // UI-only local state
    var isMuted by remember { mutableStateOf(audioManager.isMuted()) }
    var showImage by remember { mutableStateOf(false) }
    var showDisclaimer by remember { mutableStateOf(false) }

    /** Standard fade-in spec reused for intro elements. */
    fun fadeSpec() = fadeIn(animationSpec = tween(durationMillis = 250))

    /**
     * Navigation effect:
     * When ViewModel emits a navigation signal, transition to main menu.
     */
    LaunchedEffect(Unit) {
        viewModel.goNext.collect { onGoToMainmenu() }
    }

    /**
     * Intro animation effect:
     * Staggers the appearance of the main logo and disclaimer for a cleaner landing experience.
     */
    LaunchedEffect(Unit) {
        delay(120); showImage = true
        delay(150); showDisclaimer = true
    }

    /** Current ViewModel-driven UI state. */
    val state = viewModel.uiState.value

    /**
     * Determines which panel to display based on the ViewModel state.
     * - ShowForm(Login) -> LOGIN
     * - ShowForm(Register) -> REGISTER
     * - Anything else -> BUTTONS
     */
    val panel = when (state) {
        is LoginUiState.ShowForm -> when (state.mode) {
            AuthMode.LOGIN -> AuthPanel.LOGIN
            AuthMode.REGISTER -> AuthPanel.REGISTER
        }
        else -> AuthPanel.BUTTONS
    }
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /**
             * User input state.
             * rememberSaveable preserves form values across configuration changes.
             */
            var email by rememberSaveable { mutableStateOf("") }
            var password by rememberSaveable { mutableStateOf("") }

            Spacer(Modifier.weight(1f))

            // Animated logo / hero image
            AnimatedVisibility(visible = showImage, enter = fadeSpec()) {
                Image(
                    painter = painterResource(id = R.drawable.menucompass),
                    contentDescription = "Menu Logo",
                    modifier = Modifier
                        .height(280.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(Modifier.height(18.dp))

            /**
             * Animated panel switcher:
             * - BUTTONS <-> LOGIN/REGISTER transitions use slide + fade
             * - Slide direction depends on whether transition is forward or back
             */
            AnimatedContent(
                targetState = panel,
                transitionSpec = {
                    // slide direction: buttons -> login/register slides in from right,
                    // back -> slides out to right
                    val forward = (initialState == AuthPanel.BUTTONS && targetState != AuthPanel.BUTTONS)
                    slideFade(forward)
                },
                label = "authPanelAnim"
            ) { p ->

                when (p) {
                    AuthPanel.BUTTONS -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            when (state) {
                                is LoginUiState.CheckingSession -> {
                                    Text("Checking session...", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                }
                                is LoginUiState.Error -> {
                                    Text(state.message, color = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.height(8.dp))
                                }
                                is LoginUiState.NeedAuth -> {
                                    Text(
                                        text = state.reason ?: "Please login to continue.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                                else -> Unit
                            }

                            Button(
                                onClick = { viewModel.onLoginPressed() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Login") }

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.onRegisterPressed() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Register") }
                        }
                    }

                    /**
                     * Login panel.
                     * Allows the user to submit email and password for authentication.
                     */
                    AuthPanel.LOGIN -> {
                        Column {
                            if (state is LoginUiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            }
                            Text("Login", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.submitLogin(email, password)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Continue") }

                            TextButton(
                                onClick = { viewModel.onBackToButtons() },
                                modifier = Modifier.align(Alignment.Start)
                            ) { Text("Back") }
                        }
                    }

                    /**
                     * Registration panel.
                     * Allows the user to create an account using email and password.
                     */
                    AuthPanel.REGISTER -> {
                        Column {
                            if (state is LoginUiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            }
                            Text("Register", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.submitRegister(email, password)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Create account") }

                            TextButton(
                                onClick = { viewModel.onBackToButtons() },
                                modifier = Modifier.align(Alignment.Start)
                            ) { Text("Back") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            /**
             * Legal disclaimer footer.
             * Clarifies educational-only intent and IP ownership.
             */
            AnimatedVisibility(visible = showDisclaimer, enter = fadeSpec()) {
                Text(
                    text = "This application is developed solely for educational purposes. All art assets, characters, audio, and related intellectual property belong to Bandai Namco Entertainment Inc. This project is not affiliated with, endorsed by, or sponsored by Bandai Namco Entertainment Inc. No copyright infringement is intended.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.weight(1f))
        }

        // Bottom left mute
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

        // Bottom right theme
        SfxFAB(
            audio = audioManager,
            onClick = onToggleTheme,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = if (darkTheme) "Switch to Light Mode" else "Switch to Dark Mode"
            )
        }
    }
}

/**
 * Slide + fade animation used for panel transitions in AnimatedContent.
 *
 * @param forward True when navigating from BUTTONS to LOGIN/REGISTER, false when returning.
 */
private fun slideFade(forward: Boolean): ContentTransform {
    val dur = 220
    return if (forward) {
        (slideInHorizontally(tween(dur)) { it / 6 } + fadeIn(tween(dur)))
            .togetherWith(slideOutHorizontally(tween(dur)) { -it / 10 } + fadeOut(tween(dur)))
    } else {
        (slideInHorizontally(tween(dur)) { -it / 10 } + fadeIn(tween(dur)))
            .togetherWith(slideOutHorizontally(tween(dur)) { it / 6 } + fadeOut(tween(dur)))
    }
}