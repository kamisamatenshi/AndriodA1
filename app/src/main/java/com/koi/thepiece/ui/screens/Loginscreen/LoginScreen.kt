package com.koi.thepiece.ui.screens.Loginscreen

import android.content.Context
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
import androidx.compose.foundation.layout.Arrangement
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

enum class AuthPanel { BUTTONS, LOGIN, REGISTER }

@Composable
fun LoginScreen(
    audioManager: AudioManager,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onGoToMainmenu: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(context))

    var isMuted by remember { mutableStateOf(audioManager.isMuted()) }
    var showImage by remember { mutableStateOf(false) }
    var showDisclaimer by remember { mutableStateOf(false) }

    fun fadeSpec() = fadeIn(animationSpec = tween(durationMillis = 250))

    // ✅ separate effects: one for navigation, one for intro animation
    LaunchedEffect(Unit) {
        viewModel.goNext.collect { onGoToMainmenu() }
    }
    LaunchedEffect(Unit) {
        delay(120); showImage = true
        delay(150); showDisclaimer = true
    }

    val state = viewModel.uiState.value
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
            var email by rememberSaveable { mutableStateOf("") }
            var password by rememberSaveable { mutableStateOf("") }

            Spacer(Modifier.weight(1f))

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