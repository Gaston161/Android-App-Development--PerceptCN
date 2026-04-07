// 📄 app/src/main/java/com/perceptnote/features/capture/CaptureViewModel.kt
package com.perceptnote.features.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptnote.data.local.dao.AudioDao
import com.perceptnote.data.local.entities.AudioEntity
import com.perceptnote.domain.model.Note
import com.perceptnote.domain.model.NoteType
import com.perceptnote.domain.model.Session
import com.perceptnote.domain.repository.SessionRepository
import com.perceptnote.domain.usecase.SaveNoteUseCase
import com.perceptnote.sensors.audio.MicrophoneModule
import com.perceptnote.sensors.audio.SpeechModule
import com.perceptnote.sensors.camera.OcrAnalyzer
import com.perceptnote.sensors.location.LocationModule
import com.perceptnote.sensors.motion.AccelerometerGesture
import com.perceptnote.sensors.motion.AccelerometerModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CaptureUiState(
    val isRecording: Boolean = false,
    val isPhoneFlat: Boolean = false,
    val isCameraActive: Boolean = false,
    val ocrText: String = "",
    val partialTranscription: String = "",
    val sessionId: Long? = null,
    val sessionTitle: String = "",
    val errorMessage: String? = null,
    val savedNoteCount: Int = 0,
    val isSaving: Boolean = false
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val accelerometerModule: AccelerometerModule,
    private val microphoneModule: MicrophoneModule,
    private val speechModule: SpeechModule,
    private val locationModule: LocationModule,
    private val sessionRepository: SessionRepository,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val audioDao: AudioDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    // Analyseur OCR partagé avec CaptureScreen
    val ocrAnalyzer = OcrAnalyzer { detectedText ->
        viewModelScope.launch {
            onOcrTextDetected(detectedText)
        }
    }

    init {
        observeAccelerometer()
        observeSpeech()
        observeMicrophone()
        startAccelerometer()
    }

    private fun startAccelerometer() {
        accelerometerModule.start()
        _uiState.update { it.copy(isPhoneFlat = false) }
    }

    private fun observeAccelerometer() {
        viewModelScope.launch {
            accelerometerModule.gestureEvent.collect { gesture ->
                when (gesture) {
                    is AccelerometerGesture.PhoneFlat -> {
                        // LE GESTE FONDATEUR : pose → enregistrement automatique
                        if (!_uiState.value.isRecording) {
                            startSessionAutomatically()
                        }
                        _uiState.update { it.copy(isPhoneFlat = true) }
                        accelerometerModule.resetGesture()
                    }
                    is AccelerometerGesture.PhonePickedUp -> {
                        _uiState.update { it.copy(isPhoneFlat = false) }
                        accelerometerModule.resetGesture()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeSpeech() {
        viewModelScope.launch {
            speechModule.partialTranscription.collect { partial ->
                _uiState.update { it.copy(partialTranscription = partial) }
            }
        }
    }

    private fun observeMicrophone() {
        viewModelScope.launch {
            microphoneModule.isRecording.collect { recording ->
                _uiState.update { it.copy(isRecording = recording) }
            }
        }
    }

    /**
     * Déclenché automatiquement par la détection de pose (accéléromètre).
     * Crée une session géo-indexée et démarre audio + transcription.
     */
    private fun startSessionAutomatically() {
        viewModelScope.launch {
            // 1. Récupérer la position GPS
            val location = locationModule.getLastLocation()

            // 2. Générer un titre automatique
            val title = "Session du ${SimpleDateFormat("dd/MM à HH:mm", Locale.FRENCH).format(Date())}"

            // 3. Créer la session en base
            val session = Session(
                title = title,
                latitude = location?.latitude,
                longitude = location?.longitude,
                startTimestampMs = System.currentTimeMillis(),
                isActive = true
            )
            sessionRepository.deactivateAllSessions()
            val sessionId = sessionRepository.createSession(session)
            _uiState.update { it.copy(sessionId = sessionId, sessionTitle = title) }

            // 4. Démarrer l'enregistrement audio
            microphoneModule.startRecording()

            // 5. Démarrer la transcription
            speechModule.startTranscription { text, startMs, endMs ->
                viewModelScope.launch {
                    // Sauvegarder segment de transcription
                    audioDao.insert(AudioEntity(
                        sessionId = sessionId,
                        transcriptionText = text,
                        startMs = startMs,
                        endMs = endMs
                    ))
                    // Créer une note de type TRANSCRIPTION
                    saveNoteUseCase(Note(
                        sessionId = sessionId,
                        content = text,
                        type = NoteType.TRANSCRIPTION,
                        timestampMs = endMs,
                        audioSegmentMs = startMs
                    ))
                    _uiState.update { it.copy(savedNoteCount = it.savedNoteCount + 1) }
                }
            }
        }
    }

    /** Appelé manuellement si l'utilisateur veut démarrer sans poser le téléphone */
    fun startSessionManually() {
        startSessionAutomatically()
    }

    /** OCR texte détecté par la caméra → sauvegardé comme note */
    private suspend fun onOcrTextDetected(text: String) {
        val sessionId = _uiState.value.sessionId ?: return
        _uiState.update { it.copy(ocrText = text) }
        saveNoteUseCase(Note(
            sessionId = sessionId,
            content = text,
            type = NoteType.OCR,
            timestampMs = System.currentTimeMillis(),
            // Ancrage temporel : position dans l'audio correspondante
            audioSegmentMs = if (microphoneModule.isRecording.value)
                System.currentTimeMillis() - (microphoneModule.recordingDurationMs.value)
            else null
        ))
        _uiState.update { it.copy(savedNoteCount = it.savedNoteCount + 1) }
    }

    fun toggleCamera() {
        _uiState.update { it.copy(isCameraActive = !it.isCameraActive) }
    }

    /** Arrête la session et retourne l'ID pour navigation */
    fun stopSession(): Long? {
        val sessionId = _uiState.value.sessionId ?: return null
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            speechModule.stopListening()
            val audioPath = microphoneModule.stopRecording()
            // Mettre à jour la session avec la fin + chemin audio
            val session = sessionRepository.getSessionById(sessionId)
            session?.let {
                sessionRepository.updateSession(it.copy(
                    endTimestampMs = System.currentTimeMillis(),
                    audioFilePath = audioPath,
                    isActive = false
                ))
            }
            _uiState.update { it.copy(isSaving = false) }
        }
        return sessionId
    }

    override fun onCleared() {
        super.onCleared()
        accelerometerModule.stop()
        microphoneModule.release()
        speechModule.stopListening()
        ocrAnalyzer.close()
    }
}
