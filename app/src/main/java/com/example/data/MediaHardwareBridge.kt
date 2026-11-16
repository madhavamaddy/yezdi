package com.example.data

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

interface HardwareAudioBridge {
    fun dispatchPlayPause()
    fun dispatchNext()
    fun dispatchPrevious()
    fun adjustMusicVolume(stepDelta: Int)
    fun currentVolumePercent(): Int
}

class AndroidHardwareAudioBridge(context: Context) : HardwareAudioBridge {
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    override fun dispatchPlayPause() {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    override fun dispatchNext() {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    override fun dispatchPrevious() {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    override fun adjustMusicVolume(stepDelta: Int) {
        val am = audioManager ?: return
        val direction = when {
            stepDelta > 0 -> AudioManager.ADJUST_RAISE
            stepDelta < 0 -> AudioManager.ADJUST_LOWER
            else -> return
        }
        runCatching {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        }
    }

    override fun currentVolumePercent(): Int {
        val am = audioManager ?: return 70
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((current.toFloat() / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val am = audioManager ?: return
        val eventTime = SystemClock.uptimeMillis()
        runCatching {
            am.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
            am.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }
}
