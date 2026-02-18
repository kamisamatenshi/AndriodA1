package com.koi.thepiece

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build

/**
 * Fully featured AudioManager for:
 * - BGM playback (res/raw or assets)
 * - SFX playback (overlapping allowed)
 * - Master / BGM / SFX volume control
 * - Mute toggle
 *
 * Volume model:
 * Final BGM volume = masterVolume * bgmVolume
 * Final SFX volume = masterVolume * sfxVolume
 */
class AudioManager(private val context: Context) {

    // ----------------------------
    // Media players
    // ----------------------------

    private var bgmPlayer: MediaPlayer? = null
    private val activeSfxPlayers = mutableSetOf<MediaPlayer>()

    // ----------------------------
    // Volume state (0.0 – 1.0)
    // ----------------------------

    private var masterVolume: Float = 1.0f
    private var bgmVolume: Float = 1.0f
    private var sfxVolume: Float = 1.0f

    private var muted: Boolean = false

    // Store previous values for unmute restore
    private var prevMasterVolume: Float = 1.0f
    private var prevBgmVolume: Float = 1.0f
    private var prevSfxVolume: Float = 1.0f

    // ----------------------------
    // Getters
    // ----------------------------

    fun getMasterVolume(): Float = masterVolume
    fun getBgmVolume(): Float = bgmVolume
    fun getSfxVolume(): Float = sfxVolume
    fun isMuted(): Boolean = muted

    // ----------------------------
    // Volume setters
    // ----------------------------

    fun setMasterVolume(v: Float) {
        masterVolume = v.coerceIn(0f, 1f)
        applyBgmVolume()
        applyAllSfxVolume()
    }

    fun setBgmVolume(v: Float) {
        bgmVolume = v.coerceIn(0f, 1f)
        applyBgmVolume()
    }

    fun setSfxVolume(v: Float) {
        sfxVolume = v.coerceIn(0f, 1f)
        applyAllSfxVolume()
    }

    fun setMuted(mute: Boolean) {
        if (muted == mute) return

        muted = mute

        if (mute) {
            prevMasterVolume = masterVolume
            prevBgmVolume = bgmVolume
            prevSfxVolume = sfxVolume

            masterVolume = 0f
            bgmVolume = 0f
            sfxVolume = 0f
        } else {
            masterVolume = prevMasterVolume
            bgmVolume = prevBgmVolume
            sfxVolume = prevSfxVolume
        }

        applyBgmVolume()
        applyAllSfxVolume()
    }

    fun toggleMute() {
        setMuted(!muted)
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
        bgmPlayer?.let {
            safeStopAndRelease(it)
        }
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

        copy.forEach {
            safeStopAndRelease(it)
        }
    }

    // ----------------------------
    // Volume application
    // ----------------------------

    private fun applyBgmVolume() {
        val finalVolume = (masterVolume * bgmVolume).coerceIn(0f, 1f)
        bgmPlayer?.setVolume(finalVolume, finalVolume)
    }

    private fun applyAllSfxVolume() {
        activeSfxPlayers.forEach {
            applySfxVolume(it)
        }
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
