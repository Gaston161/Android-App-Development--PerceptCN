// 📄 app/src/main/java/com/perceptnote/sensors/location/LocationModule.kt
// 📦 Gradle : play-services-location
// 🔑 Permissions : ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
package com.perceptnote.sensors.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Module GPS — fournit la localisation pour la géo-indexation des sessions.
 * Utilise FusedLocationProviderClient pour une consommation batterie optimisée.
 *
 * IMPORTANT : vérifier les permissions ACCESS_FINE_LOCATION avant chaque appel.
 */
@Singleton
class LocationModule @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    /**
     * Récupère la dernière position connue (rapide, économique en batterie).
     * Fallback idéal pour la géo-indexation au moment de la sauvegarde.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? = suspendCancellableCoroutine { continuation ->
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                _currentLocation.value = location
                continuation.resume(location)
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }

    /**
     * Flow de positions continues — pour la détection de retour dans un lieu connu.
     * Intervalle : 30 secondes (compromis précision/batterie pour notre use case).
     */
    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            30_000L // Toutes les 30 secondes
        )
            .setMinUpdateDistanceMeters(10f) // Seulement si déplacé de 10m+
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _currentLocation.value = location
                    trySend(location)
                }
            }
        }

        fusedClient.requestLocationUpdates(locationRequest, callback, context.mainLooper)

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Calcule si deux coordonnées sont dans le rayon spécifié.
     * Utilise Location.distanceTo() — précis pour courtes distances.
     */
    fun isWithinRadius(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        radiusMeters: Float = 20f
    ): Boolean {
        val loc1 = Location("").apply { latitude = lat1; longitude = lon1 }
        val loc2 = Location("").apply { latitude = lat2; longitude = lon2 }
        return loc1.distanceTo(loc2) <= radiusMeters
    }
}
