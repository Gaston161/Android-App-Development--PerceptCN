// 📄 app/src/main/java/com/perceptnote/data/remote/firebase/UserRepository.kt
// 📦 Gradle : firebase-auth, firebase-firestore, firebase-storage
package com.perceptnote.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modèle utilisateur stocké dans Firestore.
 * Collection : "users/{uid}"
 */
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val fcmToken: String? = null,           // Token FCM pour les notifications push
    val isAdmin: Boolean = false,           // Droits admin (géré côté Firestore Rules)
    val sessionCount: Int = 0,              // Nombre de sessions créées
    val totalNotes: Int = 0,                // Total de notes capturées
    val appVersion: String = "",
    val deviceModel: String = "",
    val feedbackCount: Int = 0,
    val subscriptionTier: String = "free"  // "free" | "pro"
)

/**
 * Rapport de bug / feedback envoyé par un utilisateur.
 * Collection : "feedback/{feedbackId}"
 */
data class FeedbackReport(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val type: String = "bug",              // "bug" | "feature" | "general"
    val title: String = "",
    val description: String = "",
    val appVersion: String = "",
    val deviceInfo: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
    val status: String = "open",           // "open" | "in_review" | "resolved"
    val screenshotUrl: String? = null
)

@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")
    private val feedbackCollection = firestore.collection("feedback")

    /** Utilisateur Firebase actuellement connecté */
    val currentFirebaseUser: FirebaseUser? get() = auth.currentUser
    val currentUid: String? get() = auth.currentUser?.uid

    /** Flow du statut de connexion */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Crée ou met à jour le profil utilisateur dans Firestore après connexion.
     * Appelé automatiquement après Google Sign-In ou toute autre auth.
     */
    suspend fun createOrUpdateUserProfile(
        fcmToken: String? = null,
        appVersion: String = "",
        deviceModel: String = ""
    ): Result<UserProfile> {
        val user = auth.currentUser ?: return Result.failure(Exception("Non connecté"))
        return try {
            val existingDoc = usersCollection.document(user.uid).get().await()
            val profile = if (existingDoc.exists()) {
                // Mise à jour partielle
                val updates = mutableMapOf<String, Any>(
                    "lastActiveAt" to System.currentTimeMillis(),
                    "displayName" to (user.displayName ?: ""),
                    "email" to (user.email ?: "")
                )
                fcmToken?.let { updates["fcmToken"] = it }
                if (appVersion.isNotEmpty()) updates["appVersion"] = appVersion
                usersCollection.document(user.uid).update(updates).await()
                existingDoc.toObject(UserProfile::class.java)!!.copy(lastActiveAt = System.currentTimeMillis())
            } else {
                // Nouveau profil
                val newProfile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "",
                    photoUrl = user.photoUrl?.toString(),
                    fcmToken = fcmToken,
                    appVersion = appVersion,
                    deviceModel = deviceModel
                )
                usersCollection.document(user.uid).set(newProfile).await()
                newProfile
            }
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Récupère le profil de l'utilisateur connecté */
    suspend fun getUserProfile(): Result<UserProfile> {
        val uid = currentUid ?: return Result.failure(Exception("Non connecté"))
        return try {
            val doc = usersCollection.document(uid).get().await()
            val profile = doc.toObject(UserProfile::class.java)
                ?: return Result.failure(Exception("Profil introuvable"))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Met à jour le token FCM (appelé par PerceptNoteFcmService) */
    suspend fun updateFcmToken(token: String) {
        val uid = currentUid ?: return
        try {
            usersCollection.document(uid).update("fcmToken", token).await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    /** Incrémente les compteurs d'activité */
    suspend fun incrementStats(sessionCount: Int = 0, noteCount: Int = 0) {
        val uid = currentUid ?: return
        val updates = mutableMapOf<String, Any>()
        if (sessionCount > 0) updates["sessionCount"] = FieldValue.increment(sessionCount.toLong())
        if (noteCount > 0) updates["totalNotes"] = FieldValue.increment(noteCount.toLong())
        if (updates.isNotEmpty()) {
            usersCollection.document(uid).update(updates).await()
        }
    }

    /** Envoie un rapport de bug/feedback */
    suspend fun submitFeedback(report: FeedbackReport): Result<String> {
        return try {
            val docRef = feedbackCollection.document()
            val reportWithId = report.copy(
                id = docRef.id,
                userId = currentUid ?: "",
                userEmail = auth.currentUser?.email ?: ""
            )
            docRef.set(reportWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Déconnexion */
    fun signOut() = auth.signOut()
}
