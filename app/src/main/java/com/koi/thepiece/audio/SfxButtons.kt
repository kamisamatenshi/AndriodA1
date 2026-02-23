package com.koi.thepiece.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.koi.thepiece.audio.AudioManager


/**
 * A Material3 [Button] wrapper that automatically plays a sound effect (SFX)
 * before executing the provided [onClick] action.
 *
 * Behavior:
 * - If [sfxResId] is provided, that sound effect will be played.
 * - Otherwise, the [AudioManager.defaultClickSfxResId] will be used.
 * - After playing the SFX, the supplied [onClick] lambda is executed.
 *
 * This ensures consistent audio feedback across interactive UI elements.
 *
 * @param audio Instance of [AudioManager] used to trigger sound playback.
 * @param onClick Lambda executed after the SFX is played.
 * @param modifier Modifier applied to the Button.
 * @param enabled Whether the button is enabled.
 * @param sfxResId Optional raw resource ID for a custom SFX.
 * @param colors Button color configuration (default: Material3 defaults).
 * @param content Composable content inside the Button (RowScope).
 */
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

/**
 * A Material3 [TextButton] wrapper that plays a sound effect (SFX)
 * before executing the provided [onClick] action.
 *
 * Behavior:
 * - Plays [sfxResId] if provided.
 * - Otherwise falls back to [AudioManager.defaultClickSfxResId].
 * - Executes [onClick] after playing the sound.
 *
 * Useful for text-based interactive elements requiring audio feedback.
 *
 * @param audio Instance of [AudioManager] used for sound playback.
 * @param onClick Lambda executed after SFX is played.
 * @param modifier Modifier applied to the TextButton.
 * @param enabled Whether the button is enabled.
 * @param sfxResId Optional raw resource ID for a custom SFX.
 * @param content Composable content inside the TextButton (RowScope).
 */
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

/**
 * A Material3 [IconButton] wrapper that plays a sound effect (SFX)
 * before executing the provided [onClick] action.
 *
 * Behavior:
 * - Plays the specified [sfxResId] if provided.
 * - Otherwise falls back to [AudioManager.defaultClickSfxResId].
 * - Executes [onClick] after audio playback.
 *
 * Designed for icon-only clickable UI elements with audio feedback.
 *
 * @param audio Instance of [AudioManager] responsible for playing SFX.
 * @param onClick Lambda executed after SFX playback.
 * @param modifier Modifier applied to the IconButton.
 * @param enabled Whether the button is enabled.
 * @param sfxResId Optional raw resource ID for a custom SFX.
 * @param content Composable icon content.
 */
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

/**
 * A Material3 [FloatingActionButton] wrapper that plays a sound effect (SFX)
 * before executing the provided [onClick] action.
 *
 * Behavior:
 * - Plays [sfxResId] if provided.
 * - Otherwise calls [AudioManager.playClick] (default click sound).
 * - Executes [onClick] after sound playback.
 *
 * Intended for primary action buttons requiring consistent audio feedback.
 *
 * @param audio Instance of [AudioManager] used for sound playback.
 * @param onClick Lambda executed after SFX playback.
 * @param modifier Modifier applied to the FloatingActionButton.
 * @param sfxResId Optional raw resource ID for a custom SFX.
 * @param content Composable content inside the FAB.
 */
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
