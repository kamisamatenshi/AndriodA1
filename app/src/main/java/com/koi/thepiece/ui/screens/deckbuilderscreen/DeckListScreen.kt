package com.koi.thepiece.ui.screens.deckbuilderscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.koi.thepiece.audio.AudioManager
import com.koi.thepiece.ui.components.SfxButton
import kotlinx.coroutines.delay

@Composable
fun DeckListScreen (
    onBack: () -> Unit,
    audio: AudioManager,
    onGoCreateNewDeck: () -> Unit
) {
    var showBtn1 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(120); showBtn1 = true
    }

    fun fadeSpec() = fadeIn(animationSpec = tween(durationMillis = 250))

    Text("Deck List")
    AnimatedVisibility(visible = showBtn1, enter = fadeSpec()) {
        SfxButton(
            onClick =  onGoCreateNewDeck,
            audio = audio,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create New Deck")
        }
    }
}