// 📄 app/src/main/java/com/perceptnote/sensors/audio/SpeechModule.kt
// 📦 Gradle : aucune dépendance supplémentaire (API Android native)
// 🔑 Permissions : RECORD_AUDIO
package com.perceptnote.sensors.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Module de reconnaissance vocale — transcription temps réel via SpeechRecognizer.
 * Deux modes :
 *  - Mode transcription : capture continue du discours pour les notes
 *  - Mode commandes (EchoesClass) : écoute de commandes vocales spécifiques
 *
 * IMPORTANT : SpeechRecognizer DOIT être créé et utilisé sur le Main Thread.
 */
@Singleton
class SpeechModule @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    private val _partialTranscription = MutableStateFlow("")
    val partialTranscription: StateFlow<String> = _partialTranscription.asStateFlow()

    private val _voiceCommand = MutableStateFlow<VoiceCommand>(VoiceCommand.None)
    val voiceCommand: StateFlow<VoiceCommand> = _voiceCommand.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var isCommandMode = false
    private var segmentStartMs = 0L
    private var onSegmentCompleted: ((String, Long, Long) -> Unit)? = null

    /**
     * Démarre la transcription continue.
     * @param onSegment Callback appelé pour chaque segment de transcription terminé (texte, startMs, endMs)
     */
    fun startTranscription(onSegment: (text: String, startMs: Long, endMs: Long) -> Unit) {
        isCommandMode = false
        onSegmentCompleted = onSegment
        segmentStartMs = System.currentTimeMillis()
        startListening()
    }

    /** Mode EchoesClass : écoute de commandes vocales */
    fun startCommandListening() {
        isCommandMode = true
        startListening()
    }

    private fun startListening() {
        if (_isListening.value) stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)     // Résultats partiels en streaming
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }

        speechRecognizer?.startListening(intent)
        _isListening.value = true
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            _partialTranscription.value = partial
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return

            val endMs = System.currentTimeMillis()
            _transcription.value = text
            _partialTranscription.value = ""

            if (isCommandMode) {
                processVoiceCommand(text)
            } else {
                onSegmentCompleted?.invoke(text, segmentStartMs, endMs)
                segmentStartMs = endMs
                // Redémarrer automatiquement pour la transcription continue
                startListening()
            }
        }

        override fun onError(error: Int) {
            _isListening.value = false
            // Redémarrer si erreur transitoire (silence, réseau)
            if (!isCommandMode && error == SpeechRecognizer.ERROR_NO_MATCH
                || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                startListening()
            }
        }

        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Analyse le texte transcrit pour identifier une commande EchoesClass.
     * Commandes supportées : "lis", "pause", "stop", "suivant", "précédent", "résumé"
     */
    private fun processVoiceCommand(text: String) {
        val lower = text.lowercase(Locale.getDefault())
        val command = when {
            "lis" in lower || "lecture" in lower || "lire" in lower -> VoiceCommand.Read
            "pause" in lower -> VoiceCommand.Pause
            "stop" in lower || "arrête" in lower || "arreter" in lower -> VoiceCommand.Stop
            "suivant" in lower || "next" in lower -> VoiceCommand.Next
            "précédent" in lower || "previous" in lower || "retour" in lower -> VoiceCommand.Previous
            "résumé" in lower || "resume" in lower || "synthèse" in lower -> VoiceCommand.Summary
            else -> VoiceCommand.Unknown(text)
        }
        _voiceCommand.value = command
        // Redémarrer l'écoute en mode commandes
        startCommandListening()
    }

    fun stopListening() {
        speechRecognizer?.apply {
            stopListening()
            destroy()
        }
        speechRecognizer = null
        _isListening.value = false
        _partialTranscription.value = ""
    }

    fun resetCommand() {
        _voiceCommand.value = VoiceCommand.None
    }
}

/** Commandes vocales reconnues par EchoesClass */
sealed class VoiceCommand {
    object None : VoiceCommand()
    object Read : VoiceCommand()
    object Pause : VoiceCommand()
    object Stop : VoiceCommand()
    object Next : VoiceCommand()
    object Previous : VoiceCommand()
    object Summary : VoiceCommand()
    data class Unknown(val text: String) : VoiceCommand()
}
