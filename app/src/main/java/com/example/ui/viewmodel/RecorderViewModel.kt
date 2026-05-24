package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRecorderController
import com.example.data.database.AppDatabase
import com.example.data.database.RecordingEntity
import com.example.data.gemini.GeminiClient
import com.example.data.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class RecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "JarvisViewModel"

    // Instantiate Room Database and Repository
    private val database = AppDatabase.getDatabase(application)
    private val repository = RecordingRepository(database.recordingDao())

    // Instantiate Audio Controller
    private val audioController = AudioRecorderController(application)

    // Expose recordings list from Repository
    val recordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // States delegated directly from AudioController
    val isRecording: StateFlow<Boolean> = audioController.isRecording
    val isPlaying: StateFlow<Boolean> = audioController.isPlaying
    val currentTranscription: StateFlow<String> = audioController.transcription
    val liveAmplitude: StateFlow<Float> = audioController.amplitude
    val recordingDurationMs: StateFlow<Long> = audioController.durationMs
    val playbackProgress: StateFlow<Float> = audioController.playbackProgress

    // Local UI states
    private val _vocalPitch = MutableStateFlow(1.0f) // 0.5f to 2.0f
    val vocalPitch: StateFlow<Float> = _vocalPitch

    private val _vocalSpeed = MutableStateFlow(1.0f) // 0.5f to 2.0f
    val vocalSpeed: StateFlow<Float> = _vocalSpeed

    private val _noiseCancellationActive = MutableStateFlow(true)
    val noiseCancellationActive: StateFlow<Boolean> = _noiseCancellationActive

    private val _selectedRecording = MutableStateFlow<RecordingEntity?>(null)
    val selectedRecording: StateFlow<RecordingEntity?> = _selectedRecording

    // Playback state of a specific entry
    private val _activePlayingId = MutableStateFlow<Int?>(null)
    val activePlayingId: StateFlow<Int?> = _activePlayingId

    // AI/Jarvis Status states
    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading

    private val _aiResultText = MutableStateFlow<String?>(null)
    val aiResultText: StateFlow<String?> = _aiResultText

    fun setNoiseCancellation(active: Boolean) {
        _noiseCancellationActive.value = active
    }

    fun setPitch(pitch: Float) {
        _vocalPitch.value = pitch.coerceIn(0.5f, 2.0f)
        if (isPlaying.value) {
            audioController.updatePlaybackPitchAndSpeed(_vocalPitch.value, _vocalSpeed.value)
        }
    }

    fun setSpeed(speed: Float) {
        _vocalSpeed.value = speed.coerceIn(0.5f, 2.0f)
        if (isPlaying.value) {
            audioController.updatePlaybackPitchAndSpeed(_vocalPitch.value, _vocalSpeed.value)
        }
    }

    fun selectRecording(recording: RecordingEntity?) {
        _selectedRecording.value = recording
        _aiResultText.value = null
        if (recording != null) {
            _vocalPitch.value = recording.pitch
            _vocalSpeed.value = recording.speed
        }
    }

    // Audio Control Operations
    fun startRecording() {
        viewModelScope.launch {
            try {
                audioController.startRecording(_noiseCancellationActive.value)
            } catch (e: Exception) {
                Log.e(tag, "Error starting voice scan recording process", e)
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                val result = audioController.stopRecording()
                if (result != null) {
                    saveRecordingToDatabase(
                        file = result.file,
                        durationMs = result.durationMs,
                        transcript = result.transcript
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Error stopping voice scan recording process", e)
            }
        }
    }

    private suspend fun saveRecordingToDatabase(file: File, durationMs: Long, transcript: String) {
        val count = recordings.value.size
        val name = String.format("VOCAL_REF_%03d", count + 1)
        
        val newRecord = RecordingEntity(
            title = name,
            filePath = file.absolutePath,
            transcript = transcript,
            durationMs = durationMs,
            pitch = _vocalPitch.value,
            speed = _vocalSpeed.value,
            isNoiseCancelled = _noiseCancellationActive.value
        )
        
        val newId = repository.insertRecording(newRecord)
        Log.d(tag, "Successfully saved recording to DB with ID: $newId")
    }

    fun playRecording(recording: RecordingEntity) {
        if (isPlaying.value && _activePlayingId.value == recording.id) {
            stopPlaying()
            return
        }

        stopPlaying()
        _activePlayingId.value = recording.id
        _vocalPitch.value = recording.pitch
        _vocalSpeed.value = recording.speed

        audioController.startPlaying(
            file = File(recording.filePath),
            pitch = _vocalPitch.value,
            speed = _vocalSpeed.value,
            onFinished = {
                _activePlayingId.value = null
            }
        )
    }

    fun stopPlaying() {
        audioController.stopPlaying()
        _activePlayingId.value = null
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            try {
                // Delete physical local file safely
                val file = File(recording.filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(tag, "Physical audio file $deleted from storage")
                }
                
                // If it was selected, unselect it
                if (_selectedRecording.value?.id == recording.id) {
                    _selectedRecording.value = null
                    _aiResultText.value = null
                }
                
                // If is playing, stop playing first
                if (_activePlayingId.value == recording.id) {
                    stopPlaying()
                }

                // Delete database row
                repository.deleteRecording(recording)
                Log.d(tag, "Database reference deleted for id ${recording.id}")
            } catch (e: Exception) {
                Log.e(tag, "Error deleting audio index record", e)
            }
        }
    }

    fun updateVocalEffectsForSelected(recording: RecordingEntity) {
        viewModelScope.launch {
            val updated = recording.copy(
                pitch = _vocalPitch.value,
                speed = _vocalSpeed.value
            )
            repository.insertRecording(updated)
            if (_selectedRecording.value?.id == recording.id) {
                _selectedRecording.value = updated
            }
            Log.d(tag, "Vocal effects parameters updated for recording: ${recording.id}")
        }
    }

    // Jarvis AI System Diagnostic Calls
    fun analyzeVocalReference(recording: RecordingEntity, actionType: String) {
        _isAILoading.value = true
        _aiResultText.value = "CONNECTING NEURAL NETWORK TO GATEWAY...\nDOWNLOADING DATA TELEMETRY...\nRUNNING HIGH-LEVEL LINGUISTIC ALGORITHM..."
        
        viewModelScope.launch {
            try {
                val response = GeminiClient.analyzeTranscript(recording.transcript, actionType)
                _aiResultText.value = response
            } catch (e: Exception) {
                _aiResultText.value = "ERROR: Connection interrupted. Jarvis systems were unable to formulate a complete diagnostic report."
            } finally {
                _isAILoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioController.destroy()
    }
}
