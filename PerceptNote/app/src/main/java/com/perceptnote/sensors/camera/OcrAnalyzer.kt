// 📄 app/src/main/java/com/perceptnote/sensors/camera/OcrAnalyzer.kt
// 📦 Gradle : mlkit-text-recognition
// 🔑 Permissions : CAMERA (héritée de CameraModule)
package com.perceptnote.sensors.camera

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Analyseur OCR basé sur ML Kit Text Recognition.
 * S'exécute sur le cameraExecutor (thread d'analyse dédié).
 *
 * IMPORTANT : ML Kit Text Recognition (latin) fonctionne 100% on-device,
 * sans connexion réseau ni envoi de données. Gratuit et rapide.
 */
class OcrAnalyzer(
    private val onTextDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Initialisation on-device (pas de connexion réseau)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Évite les appels OCR simultanés (throttling)
    private val isProcessing = AtomicBoolean(false)

    private val _detectedText = MutableStateFlow("")
    val detectedText: StateFlow<String> = _detectedText.asStateFlow()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // Si un traitement est déjà en cours, ignorer cette frame
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            isProcessing.set(false)
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.trim()
                if (text.isNotEmpty() && text != _detectedText.value) {
                    _detectedText.value = text
                    onTextDetected(text)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
            .addOnCompleteListener {
                // IMPORTANT : toujours fermer l'ImageProxy après traitement
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    fun close() {
        recognizer.close()
    }
}
