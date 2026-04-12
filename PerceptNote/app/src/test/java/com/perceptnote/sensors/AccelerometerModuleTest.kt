// 📄 app/src/test/java/com/perceptnote/sensors/AccelerometerModuleTest.kt
package com.perceptnote.sensors

import android.content.Context
import android.hardware.SensorManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.perceptnote.sensors.motion.AccelerometerGesture
import com.perceptnote.sensors.motion.AccelerometerModule

/**
 * Tests unitaires — AccelerometerModule
 * Vérifie la logique de détection de gestes sans matériel réel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccelerometerModuleTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sensorManager = mockk(relaxed = true)
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
    }

    @Test
    fun `initial gesture state should be None`() {
        val module = AccelerometerModule(context)
        assertEquals(AccelerometerGesture.None, module.gestureEvent.value)
    }

    @Test
    fun `initial phone flat state should be false`() {
        val module = AccelerometerModule(context)
        assertFalse(module.isPhoneFlat.value)
    }

    @Test
    fun `resetGesture should set state to None`() {
        val module = AccelerometerModule(context)
        module.resetGesture()
        assertEquals(AccelerometerGesture.None, module.gestureEvent.value)
    }
}
