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
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Top-level DataStore instance for audio preferences.
 *
 * This is defined as an extension property on [Context] so it can be accessed anywhere
 * with `context.dataStore`.
 *
 * Storage name: `"audio_prefs"`
 */
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
    // BGM Select
    // ----------------------------

    /**
     * Represents an available BGM track selectable by the user.
     *
     * @property id Unique identifier stored in DataStore (e.g. `"default"`, `"OP9"`).
     * @property title Human-readable title displayed in UI (e.g. dropdown label).
     * @property resId Raw resource id under `res/raw/`.
     */
    data class BgmTrack(
        val id: String,     // unique id stored in DataStore
        val title: String,  // name shown in Settings dropdown
        val resId: Int      // raw resource id
    )

    // ----------------------------
    // DataStore keys
    // ----------------------------

    /**
     * Centralized preference keys used by DataStore.
     *
     * Keys are split into:
     * - Active values: current effective volumes and mute flag.
     * - Previous values: restore volumes used when unmuting (also persisted across restarts).
     * - Selected track: chosen BGM track id.
     */
    private object Keys {
        // Current values (what is actively applied)
        val MASTER = floatPreferencesKey("master_volume")
        val BGM = floatPreferencesKey("bgm_volume")
        val SFX = floatPreferencesKey("sfx_volume")
        val MUTED = booleanPreferencesKey("muted")

        val PREV_MASTER = floatPreferencesKey("prev_master_volume")
        val PREV_BGM = floatPreferencesKey("prev_bgm_volume")
        val PREV_SFX = floatPreferencesKey("prev_sfx_volume")

        // Datastore Key for selected track
        val SELECTED_BGM_ID = stringPreferencesKey("selected_bgm_id")
    }
    /**
     * Background scope for DataStore reads/writes and non-UI work.
     * Uses IO dispatcher to avoid blocking the main thread.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ----------------------------
    // Media players
    // ----------------------------
    /**
     * Dedicated MediaPlayer instance for background music.
     * Only one BGM track is active at a time.
     */
    private var bgmPlayer: MediaPlayer? = null
    /**
     * Tracks which BGM raw resource is currently loaded, so we can avoid re-creating the player
     * if the same track is requested again.
     */
    private var bgmResId: Int? = null
    /**
     * Set of currently playing SFX MediaPlayers to support overlap.
     * Each SFX MediaPlayer is removed on completion/error.
     */
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
    // StateFlows for UI
    // ----------------------------

    private val _masterState = MutableStateFlow(masterVolume)
    val masterState: StateFlow<Float> = _masterState.asStateFlow()

    private val _bgmState = MutableStateFlow(bgmVolume)
    val bgmState: StateFlow<Float> = _bgmState.asStateFlow()

    private val _sfxState = MutableStateFlow(sfxVolume)
    val sfxState: StateFlow<Float> = _sfxState.asStateFlow()

    private val _mutedState = MutableStateFlow(muted)
    val mutedState: StateFlow<Boolean> = _mutedState.asStateFlow()

    private val _selectedBgmIdState = MutableStateFlow("default")
    val selectedBgmIdState: StateFlow<String> = _selectedBgmIdState.asStateFlow()

    // ---------------------------
    // Available BGM tracks
    // Add more tracks here if you put new mp3 in res/raw/
    // ---------------------------
    /**
     * Tracks offered to the UI for selection.
     * Add any new `res/raw/.mp3` (or other supported formats) here.
     */
    private val availableBgmTracks: List<BgmTrack> = listOf(
        BgmTrack(id = "default", title = "Default", resId = R.raw.bgm),
        // Add more when you have them in res/raw:
        BgmTrack(id = "OP9", title = "OP9", resId = R.raw.bgm_2),
        BgmTrack(id = "OP17", title = "OP17", resId = R.raw.bgm_3),
        BgmTrack(id = "OP20", title = "OP20", resId = R.raw.bgm_4),
    )


    /**
     * On construction, load persisted preferences:
     * - Restore mute state.
     * - Restore “previous volumes” first (needed to preserve mute across restarts).
     * - Restore active volumes if not muted.
     * - Restore selected BGM track id.
     *
     * Finally:
     * - Push state into StateFlows for UI.
     * - Apply volumes to any active players.
     */
    init {
        // Load saved prefs on creation
        scope.launch {
            val prefs = context.dataStore.data.first()

            val savedMuted = prefs[Keys.MUTED] ?: false

            // Read previous restore values first (important for “mute persists across restarts”)
            prevMasterVolume = (prefs[Keys.PREV_MASTER] ?: 1.0f).coerceIn(0f, 1f)
            prevBgmVolume = (prefs[Keys.PREV_BGM] ?: 1.0f).coerceIn(0f, 1f)
            prevSfxVolume = (prefs[Keys.PREV_SFX] ?: 1.0f).coerceIn(0f, 1f)

            val savedBgmId = prefs[Keys.SELECTED_BGM_ID] ?: "default"
            _selectedBgmIdState.value = savedBgmId

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

    // Expose track list to UI
    fun getBgmTracks(): List<BgmTrack> = availableBgmTracks
    fun isMuted(): Boolean = muted

    // ----------------------------
    // Persistence helpers
    // ----------------------------

    /**
     * Persists the current "active" values:
     * - masterVolume, bgmVolume, sfxVolume
     * - muted flag
     *
     * This represents what the app is currently applying.
    */
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

    /**
     * Persists the current "restore" values used for unmuting:
     * - prevMasterVolume, prevBgmVolume, prevSfxVolume
     *
     * This enables mute to persist across restarts while still restoring prior volumes.
     */
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

    /**
     * Pushes internal state into StateFlows so Compose/UI can reactively update.
     */
    private fun pushState() {
        _masterState.value = masterVolume
        _bgmState.value = bgmVolume
        _sfxState.value = sfxVolume
        _mutedState.value = muted
    }

    // ----------------------------
    // Volume setters
    // ----------------------------
    /**
     * Sets master volume.
     *
     * Behavior:
     * - Clamps input to [0, 1].
     * - If muted: updates only restore value (prevMasterVolume) so unmute restores correctly.
     * - If not muted: updates active value, applies to BGM/SFX, and persists.
     *
     * @param v Desired volume in [0, 1] (values outside will be clamped).
     */
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
    /**
     * Sets BGM volume.
     *
     * Behavior:
     * - Clamps input to [0, 1].
     * - If muted: updates only restore value (prevBgmVolume).
     * - If not muted: updates active value, applies to BGM, and persists.
     *
     * @param v Desired volume in [0, 1] (values outside will be clamped).
     */
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

    /**
     * Sets SFX volume.
     *
     * Behavior:
     * - Clamps input to [0, 1].
     * - If muted: updates only restore value (prevSfxVolume).
     * - If not muted: updates active value, applies to all active SFX players, and persists.
     *
     * @param v Desired volume in [0, 1] (values outside will be clamped).
     */
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

    /**
     * Sets the mute state.
     *
     * Mute ON:
     * - Saves current active volumes into prev* (restore values).
     * - Forces active volumes to 0.
     *
     * Mute OFF:
     * - Restores active volumes from prev*.
     *
     * Always:
     * - Applies volumes to BGM/SFX.
     * - Pushes state to UI.
     * - Persists active values (including muted flag).
     *
     * @param mute True to mute, false to unmute.
     */
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

    /**
     * Toggles mute state:
     * - If currently muted -> unmute.
     * - If currently unmuted -> mute.
     */
    fun toggleMute() {
        setMuted(!muted)
    }

    /**
     * Plays the default click sound effect configured by [defaultClickSfxResId].
     */
    fun playClick() {
        playSfx(defaultClickSfxResId)
    }

    // ----------------------------
    // BGM playback (res/raw)
    // ----------------------------

    /**
     * Plays a background music track from a raw resource.
     *
     * Optimization:
     * - If the same [resId] is already loaded, it will only update looping and start playback
     *   if currently paused, rather than recreating the player.
     *
     * @param resId Raw resource id (e.g. `R.raw.bgm`).
     * @param loop Whether the track should loop continuously.
     */
    fun playBgm(resId: Int, loop: Boolean = true) {
        // if same track already exists, just ensure it's playing
        if (bgmPlayer != null && bgmResId == resId) {
            bgmPlayer?.isLooping = loop
            if (bgmPlayer?.isPlaying == false) bgmPlayer?.start()
            return
        }

        // new track
        stopBgmFully()
        bgmResId = resId
        bgmPlayer = MediaPlayer.create(context, resId).apply {
            isLooping = loop
            start()
        }
    }

    // ----------------------------
    // BGM playback from selected list
    // ----------------------------
    /**
     * Sets the selected BGM track by id, persists it, and plays it immediately.
     *
     * If the id is not found, falls back to the first entry in [availableBgmTracks].
     *
     * @param id Track identifier stored in DataStore (e.g. `"default"`, `"OP9"`).
     * @param loop Whether the track should loop continuously.
     */
    fun setSelectedBgm(id: String, loop: Boolean = true) {
        // Find selected track
        val track = availableBgmTracks.firstOrNull { it.id == id }
            ?: availableBgmTracks.first() // fallback

        // Update state
        _selectedBgmIdState.value = track.id

        // Save to DataStore
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[Keys.SELECTED_BGM_ID] = track.id
            }
        }

        // Switch BGM immediately
        playBgm(track.resId, loop = loop)
        applyBgmVolume()
    }

    // =====================
    // ADDED: Play saved BGM on app start
    // =====================
    /**
     * Plays the saved BGM selection from DataStore.
     *
     * Intended for app start:
     * - Reads the saved id.
     * - Falls back to default if not found.
     * - Plays and applies volume.
     *
     * @param loop Whether the track should loop continuously.
     */
    suspend fun playSelectedBgm(loop: Boolean = true) {
        val id = readSavedBgmId()
        val track = availableBgmTracks.firstOrNull { it.id == id }
            ?: availableBgmTracks.first()

        _selectedBgmIdState.value = track.id // keep UI in sync
        playBgm(track.resId, loop = loop)
        applyBgmVolume()
    }

    
    /**
     * Reads the selected BGM id from DataStore as the source of truth.
     *
     * @return Saved track id, or `"default"` if none is stored.
     */
    private suspend fun readSavedBgmId(): String {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.SELECTED_BGM_ID] ?: "default"
    }

    // ----------------------------
    // BGM playback (assets)
    // ----------------------------

    /**
     * Plays background music from an asset file.
     *
     * This:
     * - Stops any existing BGM player.
     * - Creates a new MediaPlayer.
     * - Configures music audio attributes (where supported).
     * - Prepares asynchronously, then starts playback.
     *
     * @param assetPath Path within the `assets/` directory (e.g. `"music/bgm.mp3"`).
     * @param loop Whether the track should loop continuously.
     */
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
    /**
     * Pauses BGM if it is currently playing.
     */
    fun pauseBgm() {
        bgmPlayer?.let { if (it.isPlaying) it.pause() }
    }
    /**
     * Fully stops and releases the BGM player and clears the tracked resource id.
     *
     * Use this when you want a hard reset (e.g. switching tracks).
     */
    fun stopBgmFully() {
        bgmPlayer?.release()
        bgmPlayer = null
        bgmResId = null
    }
   /**
     * Resumes BGM if it exists and is currently not playing.
     */
    fun resumeBgm() {
        bgmPlayer?.let { if (!it.isPlaying) it.start() }
    }
    /**
     * Stops and releases the BGM player safely.
     *
     * Unlike [stopBgmFully], this does not clear [bgmResId] (but it does clear [bgmPlayer]).
     */
    fun stopBgm() {
        bgmPlayer?.let { safeStopAndRelease(it) }
        bgmPlayer = null
    }

    // ----------------------------
    // SFX playback
    // ----------------------------
    /**
     * Plays a short sound effect from a raw resource.
     *
     * Characteristics:
     * - Supports overlapping playback by allocating a new MediaPlayer per play.
     * - Applies SFX audio attributes (where supported).
     * - Auto-cleans up on completion or error.
     *
     * @param resId Raw resource id of the SFX clip (e.g. `R.raw.sfx_click`).
     */
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

    /**
     * Stops and releases all currently playing SFX players.
     */
    fun stopAllSfx() {
        val copy = activeSfxPlayers.toList()
        activeSfxPlayers.clear()
        copy.forEach { safeStopAndRelease(it) }
    }

    // ----------------------------
    // Volume application
    // ----------------------------
    /**
     * Applies effective BGM volume to the current [bgmPlayer], if any.
     *
     * Effective volume is computed by: `masterVolume * bgmVolume`,
     * then clamped to [0, 1].
     */
    private fun applyBgmVolume() {
        val finalVolume = (masterVolume * bgmVolume).coerceIn(0f, 1f)
        bgmPlayer?.setVolume(finalVolume, finalVolume)
    }
    /**
     * Applies effective SFX volume to all currently active SFX players.
     */
    private fun applyAllSfxVolume() {
        activeSfxPlayers.forEach { applySfxVolume(it) }
    }
    /**
     * Applies effective SFX volume to a specific MediaPlayer instance.
     *
     * Effective volume is computed by: `masterVolume * sfxVolume`,
     * then clamped to [0, 1].
     *
     * @param mp The SFX MediaPlayer to apply volume to.
     */
    private fun applySfxVolume(mp: MediaPlayer) {
        val finalVolume = (masterVolume * sfxVolume).coerceIn(0f, 1f)
        mp.setVolume(finalVolume, finalVolume)
    }

    // ----------------------------
    // Audio attributes
    // ----------------------------
    /**
     * Configures audio attributes for music playback where supported (API 21+).
     *
     * Uses:
     * - USAGE_GAME for in-game audio routing
     * - CONTENT_TYPE_MUSIC for background music
     *
     * @param mp The MediaPlayer to configure.
     */
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
    /**
     * Configures audio attributes for sound effect playback where supported (API 21+).
     *
     * Uses:
     * - USAGE_GAME for in-game audio routing
     * - CONTENT_TYPE_SONIFICATION for short UI/game sounds
     *
     * @param mp The MediaPlayer to configure.
     */
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
    /**
     * Releases all audio resources.
     *
     * Call this from the owning component's lifecycle (e.g. Activity/Service onDestroy).
     * Note: settings persistence is handled by setters, so there's no need to persist here.
     */
    fun onDestroy() {
        stopBgm()
        stopAllSfx()
        // no need to persist here; setters already persist
    }
    /**
     * Safely stops a MediaPlayer (if playing) and then releases it.
     *
     * @param mp MediaPlayer to stop and release.
     */
    private fun safeStopAndRelease(mp: MediaPlayer) {
        try {
            if (mp.isPlaying) mp.stop()
        } catch (_: Exception) {}
        safeRelease(mp)
    }
    
    /**
     * Safely resets and releases a MediaPlayer.
     *
     * This is defensive to avoid crashes if the player is in an unexpected state.
     *
     * @param mp MediaPlayer to release.
     */
    private fun safeRelease(mp: MediaPlayer) {
        try { mp.reset() } catch (_: Exception) {}
        try { mp.release() } catch (_: Exception) {}
    }
}
