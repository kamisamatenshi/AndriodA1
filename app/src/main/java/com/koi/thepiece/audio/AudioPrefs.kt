package com.koi.thepiece

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "audio_prefs")

object AudioPrefsKeys {
    val MASTER = floatPreferencesKey("master_volume")
    val BGM = floatPreferencesKey("bgm_volume")
    val SFX = floatPreferencesKey("sfx_volume")
    val MUTED = booleanPreferencesKey("muted")
}

class AudioPrefs(private val context: Context) {

    val masterFlow: Flow<Float> = context.dataStore.data.map { it[AudioPrefsKeys.MASTER] ?: 1.0f }
    val bgmFlow: Flow<Float> = context.dataStore.data.map { it[AudioPrefsKeys.BGM] ?: 1.0f }
    val sfxFlow: Flow<Float> = context.dataStore.data.map { it[AudioPrefsKeys.SFX] ?: 1.0f }
    val mutedFlow: Flow<Boolean> = context.dataStore.data.map { it[AudioPrefsKeys.MUTED] ?: false }

    suspend fun saveVolumes(master: Float, bgm: Float, sfx: Float, muted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AudioPrefsKeys.MASTER] = master
            prefs[AudioPrefsKeys.BGM] = bgm
            prefs[AudioPrefsKeys.SFX] = sfx
            prefs[AudioPrefsKeys.MUTED] = muted
        }
    }
}
