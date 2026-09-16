package com.ghostrunner.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.ghostrunner.core.domain.PaceState

class SpatialAudioRenderer(
    private val sampleRateHz: Int = FootstepSynthesizer.DEFAULT_SAMPLE_RATE,
) {

    private val synthesizer = FootstepSynthesizer(sampleRateHz)
    private val panCalculator = ChannelPanCalculator()
    private val basePcm: ShortArray = synthesizer.synthesize()

    private var audioTrack: AudioTrack? = null
    private var channelCount: Int = 0

    val isStereoFallback: Boolean get() = channelCount == STEREO_CHANNELS
    val isStarted: Boolean get() = audioTrack != null

    fun start() {
        if (audioTrack != null) return
        val (track, channels) = build71() ?: buildStereo()
        channelCount = channels
        audioTrack = track.apply { play() }
    }

    fun enqueueFootstep(state: PaceState) {
        val track = audioTrack ?: return
        val gains = if (channelCount == SURROUND_71_CHANNELS) {
            panCalculator.gainsFor71(state)
        } else {
            panCalculator.gainsForStereo(state)
        }
        val interleaved = ShortArray(basePcm.size * channelCount)
        val maxFloat = Short.MAX_VALUE.toFloat()
        val minFloat = Short.MIN_VALUE.toFloat()
        for (frame in basePcm.indices) {
            val sample = basePcm[frame].toFloat()
            val base = frame * channelCount
            for (c in 0 until channelCount) {
                val v = (sample * gains[c]).coerceIn(minFloat, maxFloat)
                interleaved[base + c] = v.toInt().toShort()
            }
        }
        track.write(interleaved, 0, interleaved.size)
    }

    fun stop() {
        audioTrack?.runCatching { stop() }
        audioTrack?.release()
        audioTrack = null
        channelCount = 0
    }

    private fun build71(): Pair<AudioTrack, Int>? {
        val mask = AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
        val minBuffer = AudioTrack.getMinBufferSize(sampleRateHz, mask, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer == AudioTrack.ERROR_BAD_VALUE || minBuffer == AudioTrack.ERROR) return null
        return runCatching {
            buildTrack(mask, minBuffer.coerceAtLeast(BUFFER_FLOOR_BYTES)) to SURROUND_71_CHANNELS
        }.getOrNull()
    }

    private fun buildStereo(): Pair<AudioTrack, Int> {
        val mask = AudioFormat.CHANNEL_OUT_STEREO
        val minBuffer = AudioTrack.getMinBufferSize(sampleRateHz, mask, AudioFormat.ENCODING_PCM_16BIT)
            .coerceAtLeast(BUFFER_FLOOR_BYTES)
        return buildTrack(mask, minBuffer) to STEREO_CHANNELS
    }

    private fun buildTrack(channelMask: Int, bufferSizeBytes: Int): AudioTrack {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRateHz)
            .setChannelMask(channelMask)
            .build()
        return AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    companion object {
        private const val SURROUND_71_CHANNELS: Int = 8
        private const val STEREO_CHANNELS: Int = 2
        private const val BUFFER_FLOOR_BYTES: Int = 4096
    }
}
