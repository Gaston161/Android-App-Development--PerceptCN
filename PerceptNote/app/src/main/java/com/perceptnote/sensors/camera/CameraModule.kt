// 📄 app/src/main/java/com/perceptnote/sensors/camera/CameraModule.kt
// 📦 Gradle : camerax-core, camerax-camera2, camerax-lifecycle, camerax-view
// 🔑 Permissions : CAMERA
package com.perceptnote.sensors.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Module caméra — configure CameraX avec prévisualisation + analyse OCR.
 * Utilise un thread d'analyse dédié pour ne pas bloquer le thread UI.
 *
 * IMPORTANT : toujours appeler release() dans onDestroy() pour libérer la caméra.
 */
@Singleton
class CameraModule @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Executor dédié pour l'analyse d'image (OCR) — ne pas utiliser le main thread
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Démarre la caméra avec prévisualisation et analyse OCR.
     *
     * @param lifecycleOwner L'owner du cycle de vie (Fragment ou Activity)
     * @param previewView Le composant Compose/View pour afficher le flux caméra
     * @param ocrAnalyzer L'analyseur ML Kit qui traitera chaque frame
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        ocrAnalyzer: OcrAnalyzer
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Use case 1 : prévisualisation temps réel
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            // Use case 2 : analyse d'image pour OCR
            // IMPORTANT : STRATEGY_KEEP_ONLY_LATEST = pas d'accumulation de frames
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, ocrAnalyzer) }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Détacher tous les use cases précédents avant de rebind
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Libère les ressources caméra.
     * IMPORTANT : appeler dans onDestroy() pour éviter que la caméra reste ouverte.
     */
    fun release() {
        cameraExecutor.shutdown()
    }
}
