package com.koi.thepiece

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Top-level DataStore instance for audio preferences.
 *
 * This creates a typed Preferences DataStore that can be accessed via `context.dataStore`.
 * The stored file name is `"audio_prefs"`.
 */
private val Context.dataStore by preferencesDataStore(name = "audio_prefs")
/**
 * Centralized keys used for reading/writing audio settings from DataStore.
 *
 * Keys stored:
 * - MASTER: master volume float in [0, 1]
 * - BGM: bgm volume float in [0, 1]
 * - SFX: sfx volume float in [0, 1]
 * - MUTED: mute toggle boolean
 */
object AudioPrefsKeys {
    val MASTER = floatPreferencesKey("master_volume")
    val BGM = floatPreferencesKey("bgm_volume")
    val SFX = floatPreferencesKey("sfx_volume")
    val MUTED = booleanPreferencesKey("muted")
}
/**
 * Audio preferences wrapper that exposes DataStore values as reactive [Flow] streams
 * and provides a single function to persist updated settings.
 *
 * This class is intended to be used by your audio system / ViewModel layer so that:
 * - UI can observe current persisted settings (flows)
 * - audio settings can be saved consistently (saveVolumes)
 *
 * @param context Context used to access the Preferences DataStore.
 */
class AudioPrefs(private val context: Context) {
    /**
     * Flow emitting the saved master volume.
     *
     * Default: `1.0f` when no value is stored yet.
     */
    val masterFlow: Flow<Float> = context.dataStore.data.map { it[AudioPrefsKeys.MASTER] ?: 1.0f }
    /**
     * Flow emitting the saved BGM volume.
     *
     * Default: `1.0f` when no value is stored yet.
     */
    val bgmFlow: Flow<Float> = context.dataStore.data.map { it[AudioPrefsKeys.BGM] ?: 1.0f }
    /**
     * Flow emitting the saved SFX volume.
     *
     * Default: `1.0f` when no value is stored yet.
     */
    val sfxFlow: Flow<Float> = context.dataStore.data.map { it[AudioPrefsKeys.SFX] ?: 1.0f }
    /**
     * Flow emitting the saved mute state.
     *
     * Default: `false` when no value is stored yet.
     */
    val mutedFlow: Flow<Boolean> = context.dataStore.data.map { it[AudioPrefsKeys.MUTED] ?: false }
    /**
     * Persists audio settings (volumes + mute state) into DataStore.
     *
     * This writes all four values in one edit transaction so they remain consistent.
     *
     * @param master Master volume value to store (recommended range [0, 1]).
     * @param bgm Background music volume value to store (recommended range [0, 1]).
     * @param sfx Sound effects volume value to store (recommended range [0, 1]).
     * @param muted Whether audio should be considered muted.
     */
    suspend fun saveVolumes(master: Float, bgm: Float, sfx: Float, muted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AudioPrefsKeys.MASTER] = master
            prefs[AudioPrefsKeys.BGM] = bgm
            prefs[AudioPrefsKeys.SFX] = sfx
            prefs[AudioPrefsKeys.MUTED] = muted
        }
    }
}
