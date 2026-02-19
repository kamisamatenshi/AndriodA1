package com.koi.thepiece.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.koi.thepiece.audio.AudioManager

@Composable
fun SfxButton(
    audio: AudioManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sfxResId: Int? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = {
            if (sfxResId != null) audio.playSfx(sfxResId) else audio.playSfx(audio.defaultClickSfxResId)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}

@Composable
fun SfxTextButton(
    audio: AudioManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sfxResId: Int? = null,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = {
            if (sfxResId != null) audio.playSfx(sfxResId) else audio.playSfx(audio.defaultClickSfxResId)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
fun SfxIconButton(
    audio: AudioManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sfxResId: Int? = null,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = {
            if (sfxResId != null) audio.playSfx(sfxResId) else audio.playSfx(audio.defaultClickSfxResId)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
fun SfxFAB(
    audio: AudioManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sfxResId: Int? = null,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = {
            if (sfxResId != null)
                audio.playSfx(sfxResId)
            else
                audio.playClick()

            onClick()
        },
        modifier = modifier
    ) {
        content()
    }
}
