package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.log10

class AudioRecorderController(private val context: Context) {

    private val tag = "JarvisAudioController"

    // Recording States
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0L

    // Playback States
    private var mediaPlayer: MediaPlayer? = null

    // Speech Recognition States
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionIntent: Intent? = null

    // Reactive UI States
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude // Value between 0.0f and 1.0f

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress

    // Polling Scope
    private val controllerScope = CoroutineScope(Dispatchers.Main + Job())
    private var pollingJob: Job? = null
    private var playbackJob: Job? = null

    // Noise cancellation toggle (true uses VOICE_RECOGNITION source)
    private var useNoiseCancellation = true

    init {
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(tag, "Speech recognition is not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(tag, "SpeechRecognizer Ready")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(tag, "SpeechRecognizer Begin Speech")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Speech recognition volume levels: map from roughly -2dB to 10dB into 0f..1f
                    if (_isRecording.value) {
                        val normalized = ((rmsdB + 2).coerceIn(0f, 12f) / 12f)
                        _amplitude.value = normalized
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d(tag, "SpeechRecognizer End Speech")
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permissions missing"
                        SpeechRecognizer.ERROR_NETWORK -> "Network required for speech"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "HUD engine calibrated"
                        SpeechRecognizer.ERROR_SERVER -> "Hologram server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Calibrating diagnostics ($error)"
                    }
                    Log.e(tag, "SpeechRecognizer Error: $message")
                    // Do not break the recording, just list error in logs
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val newText = matches[0]
                        if (newText.isNotBlank()) {
                            _transcription.value += " $newText"
                        }
                    }
                    // Loop listener if we are still recording and speech recognizer timed out
                    if (_isRecording.value) {
                        restartSpeechRecognizer()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val partialText = matches[0]
                        if (partialText.isNotBlank()) {
                            // Temporary append to show live reaction, but will be replaced with final outputs
                            // Realtime transcript can be shown dynamically
                            Log.d(tag, "Partial transcript: $partialText")
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun restartSpeechRecognizer() {
        speechRecognizer?.stopListening()
        try {
            recognitionIntent?.let { speechRecognizer?.startListening(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error restarting speech recognizer", e)
        }
    }

    fun startRecording(noiseCancellation: Boolean): File? {
        if (_isRecording.value) return null
        
        stopPlaying()
        _transcription.value = ""
        _durationMs.value = 0L
        _amplitude.value = 0f
        useNoiseCancellation = noiseCancellation

        // Create M4A file in App's Internal Files directory (no storage permission required on modern Android)
        val filesDir = context.filesDir
        val timestamp = System.currentTimeMillis()
        val file = File(filesDir, "JARVIS_REC_$timestamp.m4a")
        currentRecordingFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            // Android official noise isolation: use VOICE_RECOGNITION which applies aggressive
            // DSP-based noise cancellation and acoustic filtering automatically if requested!
            val source = if (noiseCancellation) {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            } else {
                MediaRecorder.AudioSource.MIC
            }
            setAudioSource(source)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(192000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
                _isRecording.value = true
                recordingStartTime = System.currentTimeMillis()
                
                // Start speech recognition listening in sync
                recognitionIntent?.let { speechRecognizer?.startListening(it) }
                
                // Start UI polling for recording duration and amplitude fallback
                startPollingRecording()
                Log.d(tag, "Recording started at ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(tag, "Error starting recorder: ${e.message}", e)
                currentRecordingFile = null
                _isRecording.value = false
            }
        }
        return currentRecordingFile
    }

    private fun startPollingRecording() {
        pollingJob?.cancel()
        pollingJob = controllerScope.launch {
            while (_isRecording.value) {
                _durationMs.value = System.currentTimeMillis() - recordingStartTime
                
                // Fallback amplitude if visualizer needs backup inputs from recorder directly
                try {
                    mediaRecorder?.let {
                        val maxAmp = it.maxAmplitude
                        if (maxAmp > 0) {
                            // Map amplitude (0..32767) cleanly to 0.0 .. 1.0 (with safe logarithmic scale)
                            val db = 20 * log10(maxAmp / 32767.0)
                            // Map -60dB ... 0dB to 0f..1f
                            val factor = ((db + 60).coerceIn(0.0, 60.0) / 60.0).toFloat()
                            if (factor > _amplitude.value) {
                                _amplitude.value = factor
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Silent ignore
                }
                delay(100)
            }
        }
    }

    fun stopRecording(): RecordingResult? {
        if (!_isRecording.value) return null

        pollingJob?.cancel()
        pollingJob = null

        // Stop media recorder
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
        }

        // Stop speech recognizer
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping SpeechRecognizer", e)
        }

        _isRecording.value = false
        val finalFile = currentRecordingFile
        val finalDuration = _durationMs.value
        val finalTranscript = _transcription.value.trim()

        Log.d(tag, "Recording stopped: file=${finalFile?.absolutePath}, duration=$finalDuration ms, transcript=$finalTranscript")

        _amplitude.value = 0f

        if (finalFile != null && finalFile.exists() && finalDuration > 500L) {
            return RecordingResult(
                file = finalFile,
                durationMs = finalDuration,
                transcript = if (finalTranscript.isEmpty()) "Autonomous vocal scan complete (Empty audio input)." else finalTranscript
            )
        }
        return null
    }

    fun startPlaying(file: File, pitch: Float, speed: Float, onFinished: () -> Unit) {
        stopPlaying()

        val filePath = file.absolutePath
        if (!File(filePath).exists()) {
            Log.e(tag, "Playback file does not exist: $filePath")
            return
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                
                // Apply speed and pitch shifting (API 23+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params = PlaybackParams().apply {
                        this.pitch = pitch // e.g. 0.5f to 2.0f
                        this.speed = speed // e.g. 0.5f to 2.0f
                    }
                    playbackParams = params
                }

                setOnCompletionListener {
                    stopPlaying()
                    onFinished()
                }

                start()
                _isPlaying.value = true

                // Start playback tracking
                startTrackingPlayback()
                Log.d(tag, "Playback started with pitch=$pitch, speed=$speed")
            } catch (e: Exception) {
                Log.e(tag, "Failed to start playback", e)
                onFinished()
            }
        }
    }

    private fun startTrackingPlayback() {
        playbackJob?.cancel()
        playbackJob = controllerScope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let {
                    try {
                        val duration = it.duration
                        val position = it.currentPosition
                        if (duration > 0) {
                            _playbackProgress.value = position.toFloat() / duration.toFloat()
                        }
                    } catch (e: Exception) {
                        _playbackProgress.value = 0f
                    }
                }
                delay(50)
            }
        }
    }

    fun updatePlaybackPitchAndSpeed(pitch: Float, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        val params = PlaybackParams().apply {
                            this.pitch = pitch
                            this.speed = speed
                        }
                        it.playbackParams = params
                        Log.d(tag, "Updated ongoing playback pitch=$pitch, speed=$speed")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to update dynamic playback parameters on system player", e)
                }
            }
        }
    }

    fun stopPlaying() {
        playbackJob?.cancel()
        playbackJob = null
        _playbackProgress.value = 0f

        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
        }
    }

    fun destroy() {
        stopRecording()
        stopPlaying()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore
        }
        speechRecognizer = null
    }

    data class RecordingResult(
        val file: File,
        val durationMs: Long,
        val transcript: String
    )
}
