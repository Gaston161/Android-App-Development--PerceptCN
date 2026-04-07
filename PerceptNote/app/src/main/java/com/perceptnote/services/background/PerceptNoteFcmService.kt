// 📄 app/src/main/java/com/perceptnote/services/background/PerceptNoteFcmService.kt
// 📦 Gradle : firebase-messaging
// 🔑 Permissions : POST_NOTIFICATIONS (Android 13+)
package com.perceptnote.services.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.perceptnote.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service FCM — reçoit les notifications push envoyées depuis :
 * - La console Firebase (manuel)
 * - L'Admin Dashboard PerceptNote (broadcast)
 * - Cloud Functions (automatisé)
 *
 * Champ "type" dans le data payload :
 *   "broadcast"      → annonce admin pour tous les utilisateurs
 *   "session_sync"   → mise à jour cloud disponible
 *   "feedback_reply" → réponse à un feedback utilisateur
 *   "maintenance"    → maintenance planifiée
 */
@AndroidEntryPoint
class PerceptNoteFcmService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepo: com.perceptnote.data.remote.firebase.UserRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Mettre à jour le token FCM dans Firestore pour les futures notifications
        CoroutineScope(Dispatchers.IO).launch {
            userRepo.updateFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "PerceptNote"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""
        val type = message.data["type"] ?: "general"
        val deepLink = message.data["deepLink"]

        showNotification(title, body, type, deepLink)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        deepLink: String?
    ) {
        val channelId = when (type) {
            "broadcast"      -> CHANNEL_BROADCAST
            "feedback_reply" -> CHANNEL_FEEDBACK
            else             -> CHANNEL_GENERAL
        }

        ensureChannelExists(channelId)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            deepLink?.let { putExtra("deep_link", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannelExists(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = when (channelId) {
                CHANNEL_BROADCAST -> "Annonces"
                CHANNEL_FEEDBACK  -> "Réponses feedback"
                else              -> "Général"
            }
            val channel = NotificationChannel(
                channelId, name, NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_BROADCAST = "broadcast"
        const val CHANNEL_FEEDBACK  = "feedback_replies"
        const val CHANNEL_GENERAL   = "general"
    }
}
