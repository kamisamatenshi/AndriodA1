package com.koi.thepiece.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.koi.thepiece.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ✅ Top-level DataStore (recommended)
private val Context.dataStore by preferencesDataStore(name = "audio_prefs")

/**
 * Fully featured AudioManager for:
 * - BGM playback (res/raw or assets)
 * - SFX playback (overlapping allowed)
 * - Master / BGM / SFX volume control
 * - Mute toggle
 * - Persistent settings via DataStore
 *
 * Volume model:
 * Final BGM volume = masterVolume * bgmVolume
 * Final SFX volume = masterVolume * sfxVolume
 */
class AudioManager(private val context: Context) {

    // ----------------------------
    // DataStore keys
    // ----------------------------

    private object Keys {
        // Current values (what is actively applied)
        val MASTER = floatPreferencesKey("master_volume")
        val BGM = floatPreferencesKey("bgm_volume")
        val SFX = floatPreferencesKey("sfx_volume")
        val MUTED = booleanPreferencesKey("muted")

        // ✅ Values to restore when unmuting (persist across restarts)
        val PREV_MASTER = floatPreferencesKey("prev_master_volume")
        val PREV_BGM = floatPreferencesKey("prev_bgm_volume")
        val PREV_SFX = floatPreferencesKey("prev_sfx_volume")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ----------------------------
    // Media players
    // ----------------------------

    private var bgmPlayer: MediaPlayer? = null
    private val activeSfxPlayers = mutableSetOf<MediaPlayer>()

    // ----------------------------
    // State (backing fields)
    // ----------------------------

    private var masterVolume: Float = 1.0f
    private var bgmVolume: Float = 1.0f
    private var sfxVolume: Float = 1.0f
    private var muted: Boolean = false

    // Store previous values for unmute restore
    private var prevMasterVolume: Float = 1.0f
    private var prevBgmVolume: Float = 1.0f
    private var prevSfxVolume: Float = 1.0f

    // default click sfx
    var defaultClickSfxResId: Int = R.raw.sfx_dendenmushi

    // ----------------------------
    // ✅ StateFlows for UI
    // ----------------------------

    private val _masterState = MutableStateFlow(masterVolume)
    val masterState: StateFlow<Float> = _masterState.asStateFlow()

    private val _bgmState = MutableStateFlow(bgmVolume)
    val bgmState: StateFlow<Float> = _bgmState.asStateFlow()

    private val _sfxState = MutableStateFlow(sfxVolume)
    val sfxState: StateFlow<Float> = _sfxState.asStateFlow()

    private val _mutedState = MutableStateFlow(muted)
    val mutedState: StateFlow<Boolean> = _mutedState.asStateFlow()

    init {
        // Load saved prefs on creation
        scope.launch {
            val prefs = context.dataStore.data.first()

            val savedMuted = prefs[Keys.MUTED] ?: false

            // Read previous restore values first (important for “mute persists across restarts”)
            prevMasterVolume = (prefs[Keys.PREV_MASTER] ?: 1.0f).coerceIn(0f, 1f)
            prevBgmVolume = (prefs[Keys.PREV_BGM] ?: 1.0f).coerceIn(0f, 1f)
            prevSfxVolume = (prefs[Keys.PREV_SFX] ?: 1.0f).coerceIn(0f, 1f)

            muted = savedMuted

            if (muted) {
                // When muted, apply 0 volume, but keep restore volumes from prev*
                masterVolume = 0f
                bgmVolume = 0f
                sfxVolume = 0f
            } else {
                // When not muted, load active volumes
                masterVolume = (prefs[Keys.MASTER] ?: 1.0f).coerceIn(0f, 1f)
                bgmVolume = (prefs[Keys.BGM] ?: 1.0f).coerceIn(0f, 1f)
                sfxVolume = (prefs[Keys.SFX] ?: 1.0f).coerceIn(0f, 1f)

                // Also keep prev* in sync with current (nice UX)
                prevMasterVolume = masterVolume
                prevBgmVolume = bgmVolume
                prevSfxVolume = sfxVolume
            }

            // Push to UI state
            pushState()

            // Apply to any active players
            applyBgmVolume()
            applyAllSfxVolume()
        }
    }

    // ----------------------------
    // Getters (keep compatibility)
    // ----------------------------

    fun getMasterVolume(): Float = masterVolume
    fun getBgmVolume(): Float = bgmVolume
    fun getSfxVolume(): Float = sfxVolume
    fun isMuted(): Boolean = muted

    // ----------------------------
    // Persistence helpers
    // ----------------------------

    private fun persistActive() {
        // Persist active volumes + muted flag
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[Keys.MASTER] = masterVolume
                prefs[Keys.BGM] = bgmVolume
                prefs[Keys.SFX] = sfxVolume
                prefs[Keys.MUTED] = muted
            }
        }
    }

    private fun persistPrev() {
        // Persist restore volumes (used when unmuting, even after restart)
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[Keys.PREV_MASTER] = prevMasterVolume
                prefs[Keys.PREV_BGM] = prevBgmVolume
                prefs[Keys.PREV_SFX] = prevSfxVolume
            }
        }
    }

    private fun pushState() {
        _masterState.value = masterVolume
        _bgmState.value = bgmVolume
        _sfxState.value = sfxVolume
        _mutedState.value = muted
    }

    // ----------------------------
    // Volume setters
    // ----------------------------

    fun setMasterVolume(v: Float) {
        val nv = v.coerceIn(0f, 1f)

        if (muted) {
            // Update restore volume, but keep active 0
            prevMasterVolume = nv
            persistPrev()
            return
        }

        masterVolume = nv
        prevMasterVolume = masterVolume
        applyBgmVolume()
        applyAllSfxVolume()
        pushState()
        persistActive()
        persistPrev()
    }

    fun setBgmVolume(v: Float) {
        val nv = v.coerceIn(0f, 1f)

        if (muted) {
            prevBgmVolume = nv
            persistPrev()
            return
        }

        bgmVolume = nv
        prevBgmVolume = bgmVolume
        applyBgmVolume()
        pushState()
        persistActive()
        persistPrev()
    }

    fun setSfxVolume(v: Float) {
        val nv = v.coerceIn(0f, 1f)

        if (muted) {
            prevSfxVolume = nv
            persistPrev()
            return
        }

        sfxVolume = nv
        prevSfxVolume = sfxVolume
        applyAllSfxVolume()
        pushState()
        persistActive()
        persistPrev()
    }

    fun setMuted(mute: Boolean) {
        if (muted == mute) return
        muted = mute

        if (mute) {
            // Save restore values (current active ones)
            prevMasterVolume = masterVolume
            prevBgmVolume = bgmVolume
            prevSfxVolume = sfxVolume
            persistPrev()

            // Apply 0 active
            masterVolume = 0f
            bgmVolume = 0f
            sfxVolume = 0f
        } else {
            // Restore active from prev* (even after restart)
            masterVolume = prevMasterVolume
            bgmVolume = prevBgmVolume
            sfxVolume = prevSfxVolume
        }

        applyBgmVolume()
        applyAllSfxVolume()
        pushState()
        persistActive()
    }

    fun toggleMute() {
        setMuted(!muted)
    }

    fun playClick() {
        playSfx(defaultClickSfxResId)
    }

    // ----------------------------
    // BGM playback (res/raw)
    // ----------------------------

    fun playBgm(resId: Int, loop: Boolean = true) {
        stopBgm()

        val mp = MediaPlayer.create(context, resId) ?: return
        configureMusicAttributes(mp)

        mp.isLooping = loop
        mp.setOnErrorListener { player, _, _ ->
            safeRelease(player)
            bgmPlayer = null
            true
        }

        bgmPlayer = mp
        applyBgmVolume()
        mp.start()
    }

    // ----------------------------
    // BGM playback (assets)
    // ----------------------------

    fun playBgmFromAssets(assetPath: String, loop: Boolean = true) {
        stopBgm()

        val afd = context.assets.openFd(assetPath)
        val mp = MediaPlayer()
        configureMusicAttributes(mp)

        mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()

        mp.isLooping = loop

        mp.setOnPreparedListener {
            applyBgmVolume()
            it.start()
        }

        mp.setOnErrorListener { player, _, _ ->
            safeRelease(player)
            bgmPlayer = null
            true
        }

        mp.prepareAsync()
        bgmPlayer = mp
    }

    fun pauseBgm() {
        bgmPlayer?.pause()
    }

    fun resumeBgm() {
        bgmPlayer?.start()
    }

    fun stopBgm() {
        bgmPlayer?.let { safeStopAndRelease(it) }
        bgmPlayer = null
    }

    // ----------------------------
    // SFX playback
    // ----------------------------

    fun playSfx(resId: Int) {
        val mp = MediaPlayer.create(context, resId) ?: return

        configureSfxAttributes(mp)
        applySfxVolume(mp)

        activeSfxPlayers.add(mp)

        mp.setOnCompletionListener {
            activeSfxPlayers.remove(it)
            safeRelease(it)
        }

        mp.setOnErrorListener { player, _, _ ->
            activeSfxPlayers.remove(player)
            safeRelease(player)
            true
        }

        mp.start()
    }

    fun stopAllSfx() {
        val copy = activeSfxPlayers.toList()
        activeSfxPlayers.clear()
        copy.forEach { safeStopAndRelease(it) }
    }

    // ----------------------------
    // Volume application
    // ----------------------------

    private fun applyBgmVolume() {
        val finalVolume = (masterVolume * bgmVolume).coerceIn(0f, 1f)
        bgmPlayer?.setVolume(finalVolume, finalVolume)
    }

    private fun applyAllSfxVolume() {
        activeSfxPlayers.forEach { applySfxVolume(it) }
    }

    private fun applySfxVolume(mp: MediaPlayer) {
        val finalVolume = (masterVolume * sfxVolume).coerceIn(0f, 1f)
        mp.setVolume(finalVolume, finalVolume)
    }

    // ----------------------------
    // Audio attributes
    // ----------------------------

    private fun configureMusicAttributes(mp: MediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        }
    }

    private fun configureSfxAttributes(mp: MediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
    }

    // ----------------------------
    // Cleanup
    // ----------------------------

    fun onDestroy() {
        stopBgm()
        stopAllSfx()
        // no need to persist here; setters already persist
    }

    private fun safeStopAndRelease(mp: MediaPlayer) {
        try {
            if (mp.isPlaying) mp.stop()
        } catch (_: Exception) {}
        safeRelease(mp)
    }

    private fun safeRelease(mp: MediaPlayer) {
        try { mp.reset() } catch (_: Exception) {}
        try { mp.release() } catch (_: Exception) {}
    }
}
