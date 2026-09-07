package com.example.ai.live.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Clean abstraction for capturing live audio input (Microphone).
 */
interface AudioInput {
    val isRecording: Boolean
    fun startRecording(onChunkCaptured: (ByteArray) -> Unit): Boolean
    fun stopRecording()
    fun release()
}

/**
 * Clean abstraction for playing back raw PCM audio (Speaker).
 */
interface AudioOutput {
    val isPlaying: Boolean
    fun playPcmChunk(pcmData: ByteArray)
    fun stopAndFlush()
    fun release()
}

/**
 * Real Android hardware implementation of [AudioInput] using AudioRecord.
 * Configured for 16kHz, 16-bit PCM, Mono.
 * Chunks are emitted in small, regular intervals (~20-40ms).
 */
class HardwarePcmAudioInput(
    private val context: Context,
    private val sampleRate: Int = 16000
) : AudioInput {
    private val tag = "RafiqahLive"
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    override var isRecording: Boolean = false
        private set

    override fun startRecording(onChunkCaptured: (ByteArray) -> Unit): Boolean {
        if (isRecording) return true

        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(tag, "Microphone permission RECORD_AUDIO is not granted.")
            return false
        }

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(tag, "Invalid AudioRecord buffer size.")
            return false
        }

        // Chunk size: 30ms -> 16000 * 2 bytes * 0.03 = 960 bytes
        val chunkSize = 960
        val bufferSize = maxOf(minBufferSize * 2, chunkSize * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord failed to initialize.")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch {
                val buffer = ByteArray(chunkSize)
                while (isActive && isRecording) {
                    val readBytes = audioRecord?.read(buffer, 0, chunkSize) ?: -1
                    if (readBytes > 0) {
                        val chunk = buffer.copyOf(readBytes)
                        onChunkCaptured(chunk)
                    }
                }
            }

            Log.i(tag, "Hardware microphone capture started at ${sampleRate}Hz PCM 16-bit Mono.")
            return true
        } catch (e: SecurityException) {
            Log.e(tag, "Security exception when initializing AudioRecord: ${e.message}")
            stopRecording()
            return false
        } catch (e: Exception) {
            Log.e(tag, "Failed to start AudioRecord: ${e.message}")
            stopRecording()
            return false
        }
    }

    override fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w(tag, "Error releasing AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }
        Log.i(tag, "Hardware microphone capture stopped.")
    }

    override fun release() {
        stopRecording()
    }
}

/**
 * Real Android hardware implementation of [AudioOutput] using AudioTrack.
 * Plays raw PCM 16-bit audio on the speaker with instant interruption flush.
 */
class HardwarePcmAudioOutput(
    private val sampleRate: Int = 24000
) : AudioOutput {
    private val tag = "RafiqahLive"
    private var audioTrack: AudioTrack? = null

    @Volatile
    override var isPlaying: Boolean = false
        private set

    init {
        initializeTrack()
    }

    private fun initializeTrack() {
        try {
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize * 2, 4096)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize AudioTrack: ${e.message}")
        }
    }

    override fun playPcmChunk(pcmData: ByteArray) {
        if (pcmData.isEmpty()) return
        try {
            if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                initializeTrack()
            }
            audioTrack?.let { track ->
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                }
                isPlaying = true
                track.write(pcmData, 0, pcmData.size)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error writing PCM to AudioTrack: ${e.message}")
        }
    }

    override fun stopAndFlush() {
        isPlaying = false
        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.pause()
                    track.flush()
                }
            }
            Log.d(tag, "AudioTrack stopped and buffer flushed (Interruption / Barge-in).")
        } catch (e: Exception) {
            Log.w(tag, "Error flushing AudioTrack: ${e.message}")
        }
    }

    override fun release() {
        stopAndFlush()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(tag, "Error releasing AudioTrack: ${e.message}")
        } finally {
            audioTrack = null
        }
    }
}

/**
 * Test/Mock implementation of [AudioInput] for JVM and unit testing.
 */
class MockAudioInput : AudioInput {
    override var isRecording: Boolean = false
    var capturedChunkCount: Int = 0

    override fun startRecording(onChunkCaptured: (ByteArray) -> Unit): Boolean {
        isRecording = true
        return true
    }

    fun simulateMicInput(data: ByteArray, onChunkCaptured: (ByteArray) -> Unit) {
        if (isRecording) {
            capturedChunkCount++
            onChunkCaptured(data)
        }
    }

    override fun stopRecording() {
        isRecording = false
    }

    override fun release() {
        stopRecording()
    }
}

/**
 * Test/Mock implementation of [AudioOutput] for JVM and unit testing.
 */
class MockAudioOutput : AudioOutput {
    override var isPlaying: Boolean = false
    val playedChunks = mutableListOf<ByteArray>()
    var flushCallCount: Int = 0

    override fun playPcmChunk(pcmData: ByteArray) {
        isPlaying = true
        playedChunks.add(pcmData)
    }

    override fun stopAndFlush() {
        isPlaying = false
        flushCallCount++
        playedChunks.clear()
    }

    override fun release() {
        isPlaying = false
    }
}
