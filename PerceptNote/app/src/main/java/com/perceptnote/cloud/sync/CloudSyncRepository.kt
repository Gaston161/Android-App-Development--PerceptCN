// 📄 app/src/main/java/com/perceptnote/cloud/sync/CloudSyncRepository.kt
// 📦 Gradle : firebase-firestore, firebase-storage
package com.perceptnote.cloud.sync

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.perceptnote.domain.model.Note
import com.perceptnote.domain.model.NoteType
import com.perceptnote.domain.model.Session
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestion de la synchronisation cloud (Firestore + Storage).
 *
 * Structure Firestore :
 * users/{uid}/
 *   sessions/{sessionId}    → SessionDocument
 *   notes/{noteId}          → NoteDocument
 *   chatHistory/{chatId}    → ChatDocument (utilisé pour améliorer l'IA)
 *
 * Structure Storage :
 * users/{uid}/audio/{sessionId}.m4a
 */
@Singleton
class CloudSyncRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val uid get() = auth.currentUser?.uid ?: throw Exception("Non authentifié")

    private fun sessionsRef() = firestore.collection("users/$uid/sessions")
    private fun notesRef() = firestore.collection("users/$uid/notes")
    private fun chatRef() = firestore.collection("users/$uid/chatHistory")

    // ==============================
    // SYNCHRONISATION SESSIONS
    // ==============================

    /** Sauvegarde une session complète dans le cloud */
    suspend fun backupSession(session: Session): Result<Unit> = try {
        val doc = mapOf(
            "id" to session.id,
            "title" to session.title,
            "latitude" to session.latitude,
            "longitude" to session.longitude,
            "locationName" to session.locationName,
            "startTimestampMs" to session.startTimestampMs,
            "endTimestampMs" to session.endTimestampMs,
            "summary" to session.summary,
            "syncedAt" to System.currentTimeMillis()
        )
        sessionsRef().document(session.id.toString()).set(doc).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    /** Sauvegarde une liste de notes d'une session */
    suspend fun backupNotes(notes: List<Note>): Result<Unit> = try {
        val batch = firestore.batch()
        notes.forEach { note ->
            val ref = notesRef().document(note.id.toString())
            val doc = mapOf(
                "id" to note.id,
                "sessionId" to note.sessionId,
                "content" to note.content,
                "type" to note.type.name,
                "timestampMs" to note.timestampMs,
                "audioSegmentMs" to note.audioSegmentMs
            )
            batch.set(ref, doc)
        }
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    /** Récupère toutes les sessions depuis le cloud (pour restaurer sur un autre appareil) */
    suspend fun fetchAllSessions(): Result<List<Map<String, Any>>> = try {
        val snapshot = sessionsRef()
            .orderBy("startTimestampMs", Query.Direction.DESCENDING)
            .get().await()
        val sessions = snapshot.documents.mapNotNull { it.data }
        Result.success(sessions)
    } catch (e: Exception) { Result.failure(e) }

    /** Récupère les notes d'une session depuis le cloud */
    suspend fun fetchNotesForSession(sessionId: Long): Result<List<Note>> = try {
        val snapshot = notesRef()
            .whereEqualTo("sessionId", sessionId)
            .orderBy("timestampMs")
            .get().await()
        val notes = snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            Note(
                id = (data["id"] as? Long) ?: 0L,
                sessionId = (data["sessionId"] as? Long) ?: sessionId,
                content = (data["content"] as? String) ?: "",
                type = NoteType.valueOf((data["type"] as? String) ?: "MANUAL"),
                timestampMs = (data["timestampMs"] as? Long) ?: 0L
            )
        }
        Result.success(notes)
    } catch (e: Exception) { Result.failure(e) }

    // ==============================
    // UPLOAD AUDIO (Firebase Storage)
    // ==============================

    /**
     * Upload du fichier audio de session vers Firebase Storage.
     * @return URL de téléchargement ou null si erreur.
     */
    suspend fun uploadAudioFile(sessionId: Long, localFilePath: String): String? {
        return try {
            val file = File(localFilePath)
            if (!file.exists()) return null
            val ref = storage.reference.child("users/$uid/audio/session_$sessionId.m4a")
            ref.putFile(Uri.fromFile(file)).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==============================
    // HISTORIQUE CHAT (pour amélioration IA)
    // ==============================

    /**
     * Sauvegarde l'historique d'un chat dans Firestore.
     * IMPORTANT : Ces données sont utilisées de façon anonymisée pour améliorer l'IA.
     * L'utilisateur doit être informé et avoir accepté dans les CGU.
     */
    suspend fun saveChatForAITraining(
        sessionId: Long,
        messages: List<Map<String, String>>
    ) {
        try {
            val doc = mapOf(
                "sessionId" to sessionId,
                "messages" to messages,
                "timestamp" to System.currentTimeMillis(),
                "deviceModel" to android.os.Build.MODEL,
                "appVersion" to com.perceptnote.BuildConfig.VERSION_NAME
            )
            chatRef().add(doc).await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ==============================
    // STATUT DE SYNCHRONISATION
    // ==============================

    /** Vérifie si une session existe déjà dans le cloud */
    suspend fun isSessionSynced(sessionId: Long): Boolean = try {
        val doc = sessionsRef().document(sessionId.toString()).get().await()
        doc.exists()
    } catch (e: Exception) { false }
}
