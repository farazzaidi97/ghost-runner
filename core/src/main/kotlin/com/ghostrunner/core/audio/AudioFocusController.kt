package com.ghostrunner.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.ghostrunner.core.domain.PaceState

class AudioFocusController(context: Context) {

    private val audioManager: AudioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var current: AudioFocusRequest? = null

    val hasFocus: Boolean get() = current != null

    fun applyForState(state: PaceState) {
        when (state) {
            is PaceState.Behind -> requestDucking()
            is PaceState.OnPace, is PaceState.Ahead -> release()
        }
    }

    fun requestDucking(): Boolean {
        if (current != null) return true
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { /* no-op; we duck others, we don't react */ }
            .build()
        val result = audioManager.requestAudioFocus(request)
        return if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            current = request
            true
        } else {
            false
        }
    }

    fun release() {
        current?.let { audioManager.abandonAudioFocusRequest(it) }
        current = null
    }
}
