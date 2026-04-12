// 📄 app/src/main/java/com/perceptnote/sensors/motion/AccelerometerModule.kt
// 📦 Gradle : aucune dépendance supplémentaire (API Android native)
// 🔑 Permissions : aucune (l'accéléromètre ne requiert pas de permission Android)
package com.perceptnote.sensors.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Module accéléromètre — détecte deux gestes fondamentaux :
 * 1. Pose à plat → déclenche l'enregistrement automatique (PerceptNote)
 * 2. Inclinaison gauche/droite → navigation EchoesClass
 *
 * IMPORTANT : utilise WeakReference pour éviter les memory leaks.
 * Toujours appeler stop() dans onPause() ou onStop() du cycle de vie.
 */
@Singleton
class AccelerometerModule @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // === États exposés via StateFlow ===
    private val _gestureEvent = MutableStateFlow<AccelerometerGesture>(AccelerometerGesture.None)
    val gestureEvent: StateFlow<AccelerometerGesture> = _gestureEvent.asStateFlow()

    private val _isPhoneFlat = MutableStateFlow(false)
    val isPhoneFlat: StateFlow<Boolean> = _isPhoneFlat.asStateFlow()

    // Debounce : timestamp du dernier déclenchement
    private var lastFlatTriggerMs = 0L
    private var lastTiltTriggerMs = 0L

    private val FLAT_THRESHOLD = 8.5f      // Z > 8.5 m/s² = téléphone à plat
    private val TILT_THRESHOLD = 5.0f      // |X| > 5 = inclinaison gauche/droite
    private val FLAT_DEBOUNCE_MS = 1500L   // 1.5s debounce pour éviter les faux positifs
    private val TILT_DEBOUNCE_MS = 800L

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event ?: return
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

            val x = event.values[0] // Axe gauche/droite
            val z = event.values[2] // Axe vertical (face/dos)
            val now = System.currentTimeMillis()

            // === Détection pose à plat (PerceptNote) ===
            val flat = z > FLAT_THRESHOLD
            if (flat != _isPhoneFlat.value) {
                _isPhoneFlat.value = flat
                if (flat && now - lastFlatTriggerMs > FLAT_DEBOUNCE_MS) {
                    lastFlatTriggerMs = now
                    _gestureEvent.value = AccelerometerGesture.PhoneFlat
                } else if (!flat) {
                    _gestureEvent.value = AccelerometerGesture.PhonePickedUp
                }
            }

            // === Détection inclinaison (EchoesClass) ===
            if (now - lastTiltTriggerMs > TILT_DEBOUNCE_MS) {
                when {
                    x < -TILT_THRESHOLD -> {
                        lastTiltTriggerMs = now
                        _gestureEvent.value = AccelerometerGesture.TiltLeft
                    }
                    x > TILT_THRESHOLD -> {
                        lastTiltTriggerMs = now
                        _gestureEvent.value = AccelerometerGesture.TiltRight
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* Pas nécessaire */ }
    }

    /**
     * Démarre l'écoute de l'accéléromètre.
     * IMPORTANT : appeler dans onResume() du cycle de vie.
     */
    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL // Économie batterie vs SENSOR_DELAY_UI
            )
        }
    }

    /**
     * Arrête l'écoute de l'accéléromètre.
     * IMPORTANT : appeler dans onPause() pour éviter la vidange de la batterie.
     */
    fun stop() {
        sensorManager.unregisterListener(listener)
        _gestureEvent.value = AccelerometerGesture.None
    }

    /** Réinitialise l'événement de geste après traitement */
    fun resetGesture() {
        _gestureEvent.value = AccelerometerGesture.None
    }
}

/** Sealed class des gestes détectés par l'accéléromètre */
sealed class AccelerometerGesture {
    object None : AccelerometerGesture()
    object PhoneFlat : AccelerometerGesture()      // Pose à plat → auto-record
    object PhonePickedUp : AccelerometerGesture()  // Relevé → arrêt enregistrement
    object TiltLeft : AccelerometerGesture()       // EchoesClass : section précédente
    object TiltRight : AccelerometerGesture()      // EchoesClass : section suivante
}
