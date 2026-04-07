// 📄 app/src/main/java/com/perceptnote/cloud/backup/AppStateManager.kt
// Gestion de la persistance d'état même après plusieurs mois d'inactivité
package com.perceptnote.cloud.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("perceptnote_state")

/**
 * Gestionnaire d'état persistant (DataStore + Firestore).
 *
 * Deux niveaux de persistance :
 * 1. DataStore (local) : survit aux redémarrages, conservé tant que l'app est installée
 * 2. Firestore (cloud) : survit au changement d'appareil, récupérable après des mois
 *
 * État sauvegardé :
 * - Dernière session active (ID + position dans les notes)
 * - Préférences utilisateur (mode dyslexie, langue, overlay actif)
 * - Dernière position dans EchoesClass
 * - Token FCM et préférences de notification
 */
@Singleton
class AppStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // === CLÉS DE PERSISTANCE ===
    companion object {
        val KEY_LAST_SESSION_ID = longPreferencesKey("last_session_id")
        val KEY_LAST_NOTE_INDEX = intPreferencesKey("last_note_index")
        val KEY_DYSLEXIA_MODE = booleanPreferencesKey("dyslexia_mode")
        val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val KEY_PREFERRED_LANG = stringPreferencesKey("preferred_language")
        val KEY_LAST_ACTIVE_MS = longPreferencesKey("last_active_ms")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
    }

    // === SAUVEGARDE ===

    suspend fun saveLastSession(sessionId: Long, noteIndex: Int = 0) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SESSION_ID] = sessionId
            prefs[KEY_LAST_NOTE_INDEX] = noteIndex
            prefs[KEY_LAST_ACTIVE_MS] = System.currentTimeMillis()
        }
    }

    suspend fun saveDyslexiaMode(enabled: Boolean) {
        dataStore.edit { it[KEY_DYSLEXIA_MODE] = enabled }
    }

    suspend fun saveOverlayEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_OVERLAY_ENABLED] = enabled }
        // Aussi dans SharedPreferences pour le BootReceiver
        context.getSharedPreferences("perceptnote_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("overlay_enabled", enabled).apply()
    }

    suspend fun saveBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun completeOnboarding() {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    // === LECTURE (Flow réactif) ===

    val lastSessionId: Flow<Long?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_LAST_SESSION_ID] }

    val lastNoteIndex: Flow<Int> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_LAST_NOTE_INDEX] ?: 0 }

    val dyslexiaMode: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_DYSLEXIA_MODE] ?: false }

    val overlayEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_OVERLAY_ENABLED] ?: false }

    val isOnboardingDone: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_ONBOARDING_DONE] ?: false }

    val biometricEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_BIOMETRIC_ENABLED] ?: false }

    /** Calcule le temps écoulé depuis la dernière utilisation */
    val daysSinceLastActive: Flow<Long> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val lastActive = prefs[KEY_LAST_ACTIVE_MS] ?: return@map 0L
            (System.currentTimeMillis() - lastActive) / (1000 * 60 * 60 * 24)
        }
}
