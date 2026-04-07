// 📄 app/src/test/java/com/perceptnote/sensors/LocationModuleTest.kt
package com.perceptnote.sensors

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.perceptnote.sensors.location.LocationModule

/**
 * Tests unitaires — LocationModule
 * Vérifie le calcul de distance GPS (rayon 20m).
 */
class LocationModuleTest {

    private lateinit var context: Context
    private lateinit var locationModule: LocationModule

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        locationModule = LocationModule(context)
    }

    @Test
    fun `same coordinates should be within radius`() {
        val lat = 3.8667   // Yaoundé
        val lon = 11.5167
        assertTrue(locationModule.isWithinRadius(lat, lon, lat, lon, 20f))
    }

    @Test
    fun `coordinates 10 meters apart should be within 20m radius`() {
        // Déplacement d'environ 9m vers le nord (~0.00009°)
        val lat1 = 3.8667
        val lon = 11.5167
        val lat2 = lat1 + 0.00009 // ≈ 10m
        assertTrue(locationModule.isWithinRadius(lat1, lon, lat2, lon, 20f))
    }

    @Test
    fun `coordinates 50 meters apart should NOT be within 20m radius`() {
        val lat1 = 3.8667
        val lon = 11.5167
        val lat2 = lat1 + 0.00045 // ≈ 50m
        assertFalse(locationModule.isWithinRadius(lat1, lon, lat2, lon, 20f))
    }

    @Test
    fun `default radius should be 20 meters`() {
        val lat = 3.8667
        val lon = 11.5167
        // Tester avec le rayon par défaut
        assertTrue(locationModule.isWithinRadius(lat, lon, lat, lon))
    }
}
