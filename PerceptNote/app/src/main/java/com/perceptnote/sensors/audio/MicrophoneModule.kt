// 📄 app/src/main/java/com/perceptnote/sensors/audio/MicrophoneModule.kt
// 📦 Gradle : aucune dépendance supplémentaire (API Android native)
// 🔑 Permissions : RECORD_AUDIO, WRITE_EXTERNAL_STORAGE (API < 29)
package com.perceptnote.sensors.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Module enregistrement audio — utilise MediaRecorder pour sauvegarder en .m4a.
 * L'accéléromètre (AccelerometerModule) déclenche start/stop automatiquement.
 */
@Singleton
class MicrophoneModule @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private var startTimeMs = 0L

    /**
     * Démarre l'enregistrement audio dans un fichier .m4a.
     * @return Le chemin du fichier en cours d'enregistrement, ou null si erreur.
     */
    fun startRecording(): String? {
        if (_isRecording.value) return _currentFilePath.value

        val outputFile = createAudioFile()
        _currentFilePath.value = outputFile.absolutePath

        recorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)

            try {
                prepare()
                start()
                _isRecording.value = true
                startTimeMs = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
                release()
                recorder = null
                _isRecording.value = false
                return null
            }
        }
        return outputFile.absolutePath
    }

    /**
     * Arrête l'enregistrement et retourne le chemin du fichier sauvegardé.
     */
    fun stopRecording(): String? {
        if (!_isRecording.value) return null

        val filePath = _currentFilePath.value
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
            _isRecording.value = false
            _recordingDurationMs.value = System.currentTimeMillis() - startTimeMs
        }
        return filePath
    }

    /** Crée le fichier de sortie dans le répertoire cache de l'app */
    private fun createAudioFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val audioDir = File(context.filesDir, "audio").also { it.mkdirs() }
        return File(audioDir, "session_$timestamp.m4a")
    }

    /** Compatibilité API 26+ pour MediaRecorder */
    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

    fun release() {
        recorder?.release()
        recorder = null
        _isRecording.value = false
    }
}
