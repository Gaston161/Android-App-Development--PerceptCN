// 📄 app/src/main/java/com/perceptnote/features/echoes/EchoesViewModel.kt
package com.perceptnote.features.echoes

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptnote.domain.model.Note
import com.perceptnote.domain.repository.NoteRepository
import com.perceptnote.domain.repository.SessionRepository
import com.perceptnote.sensors.audio.SpeechModule
import com.perceptnote.sensors.audio.VoiceCommand
import com.perceptnote.sensors.motion.AccelerometerGesture
import com.perceptnote.sensors.motion.AccelerometerModule
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class EchoesUiState(
    val notes: List<Note> = emptyList(),
    val currentSectionIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isDyslexiaMode: Boolean = false,
    val isListeningForCommands: Boolean = false,
    val sessionTitle: String = "",
    val statusMessage: String = "Prêt à lire"
)

@HiltViewModel
class EchoesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val sessionRepository: SessionRepository,
    private val speechModule: SpeechModule,
    private val accelerometerModule: AccelerometerModule
) : ViewModel() {

    private val _uiState = MutableStateFlow(EchoesUiState())
    val uiState: StateFlow<EchoesUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        initTts()
        observeVoiceCommands()
        observeAccelerometerGestures()
        accelerometerModule.start()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRENCH
                isTtsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _uiState.update { it.copy(isPlaying = true) }
                    }
                    override fun onDone(utteranceId: String?) {
                        _uiState.update { it.copy(isPlaying = false, statusMessage = "Lecture terminée") }
                    }
                    override fun onError(utteranceId: String?) {
                        _uiState.update { it.copy(isPlaying = false, statusMessage = "Erreur TTS") }
                    }
                })
            }
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionRepository.getSessionById(sessionId)
            _uiState.update { it.copy(sessionTitle = session?.title ?: "") }
            noteRepository.getNotesBySession(sessionId)
                .first()
                .let { notes ->
                    _uiState.update { it.copy(notes = notes) }
                    if (notes.isNotEmpty()) speak("Session chargée : ${session?.title}. ${notes.size} notes disponibles.")
                }
        }
    }

    /** Lit la note à la position currentSectionIndex */
    fun readCurrentSection() {
        val notes = _uiState.value.notes
        val index = _uiState.value.currentSectionIndex
        if (notes.isEmpty() || index >= notes.size) return
        val note = notes[index]
        val prefix = "Note ${index + 1} sur ${notes.size}. Type : ${note.type.name}. "
        speak(prefix + note.content)
        _uiState.update { it.copy(statusMessage = "Lecture note ${index + 1}/${notes.size}") }
    }

    private fun speak(text: String) {
        if (!isTtsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PN_${System.currentTimeMillis()}")
        _uiState.update { it.copy(isPlaying = true) }
    }

    fun pauseOrResume() {
        if (_uiState.value.isPlaying) {
            tts?.stop()
            _uiState.update { it.copy(isPlaying = false, statusMessage = "Pause") }
        } else {
            readCurrentSection()
        }
    }

    fun nextSection() {
        val size = _uiState.value.notes.size
        if (size == 0) return
        val newIndex = (_uiState.value.currentSectionIndex + 1).coerceAtMost(size - 1)
        _uiState.update { it.copy(currentSectionIndex = newIndex) }
        readCurrentSection()
    }

    fun previousSection() {
        val newIndex = (_uiState.value.currentSectionIndex - 1).coerceAtLeast(0)
        _uiState.update { it.copy(currentSectionIndex = newIndex) }
        readCurrentSection()
    }

    fun toggleDyslexiaMode() {
        _uiState.update { it.copy(isDyslexiaMode = !it.isDyslexiaMode) }
        speak(if (_uiState.value.isDyslexiaMode) "Mode dyslexie activé" else "Mode dyslexie désactivé")
    }

    fun startListeningForCommands() {
        speechModule.startCommandListening()
        _uiState.update { it.copy(isListeningForCommands = true, statusMessage = "En écoute…") }
    }

    private fun observeVoiceCommands() {
        viewModelScope.launch {
            speechModule.voiceCommand.collect { command ->
                when (command) {
                    is VoiceCommand.Read -> readCurrentSection()
                    is VoiceCommand.Pause -> pauseOrResume()
                    is VoiceCommand.Stop -> {
                        tts?.stop()
                        _uiState.update { it.copy(isPlaying = false) }
                    }
                    is VoiceCommand.Next -> nextSection()
                    is VoiceCommand.Previous -> previousSection()
                    is VoiceCommand.Summary -> {
                        // Lire le résumé si disponible
                        speak("Fonctionnalité résumé vocal à venir.")
                    }
                    else -> {}
                }
                speechModule.resetCommand()
            }
        }
    }

    private fun observeAccelerometerGestures() {
        viewModelScope.launch {
            accelerometerModule.gestureEvent.collect { gesture ->
                when (gesture) {
                    // EchoesClass : inclinaison pour naviguer entre sections
                    is AccelerometerGesture.TiltLeft -> {
                        previousSection()
                        accelerometerModule.resetGesture()
                    }
                    is AccelerometerGesture.TiltRight -> {
                        nextSection()
                        accelerometerModule.resetGesture()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        speechModule.stopListening()
        accelerometerModule.stop()
    }
}
