package com.koi.thepiece.ui.screens.deckbuilderscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.koi.thepiece.audio.AudioManager
import coil.ImageLoader
import kotlinx.coroutines.delay

@Composable
fun DeckDetailScreen(
    onBack: () -> Unit,
    audio: AudioManager,
    imageLoader: ImageLoader,
    deckId: Int
) {
}