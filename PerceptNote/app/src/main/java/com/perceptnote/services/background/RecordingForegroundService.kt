// 📄 app/src/main/java/com/perceptnote/services/background/RecordingForegroundService.kt
// 📦 Gradle : lifecycle-service
// 🔑 Permissions : FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE
package com.perceptnote.services.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.perceptnote.MainActivity
import com.perceptnote.sensors.audio.MicrophoneModule
import com.perceptnote.sensors.audio.SpeechModule
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service foreground qui maintient l'enregistrement audio actif
 * même quand l'app est en arrière-plan ou que l'écran est éteint.
 *
 * Affiche une notification persistante (obligatoire Android 8+).
 * L'utilisateur peut voir/arrêter l'enregistrement depuis la notification.
 */
@AndroidEntryPoint
class RecordingForegroundService : LifecycleService() {

    @Inject lateinit var microphoneModule: MicrophoneModule
    @Inject lateinit var speechModule: SpeechModule

    private val binder = LocalBinder()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var currentSessionId: Long = -1L

    inner class LocalBinder : Binder() {
        fun getService(): RecordingForegroundService = this@RecordingForegroundService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                currentSessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                startForegroundRecording()
            }
            ACTION_STOP -> stopForegroundRecording()
        }
        // START_STICKY : le système redémarre le service si tué
        return START_STICKY
    }

    private fun startForegroundRecording() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Enregistrement en cours…"))
        microphoneModule.startRecording()
        _isRecording.value = true

        // Mise à jour de la notification avec la durée
        lifecycleScope.launch {
            microphoneModule.recordingDurationMs.collect { durationMs ->
                val minutes = durationMs / 60000
                val seconds = (durationMs % 60000) / 1000
                updateNotification("● En cours : %02d:%02d".format(minutes, seconds))
            }
        }
    }

    private fun stopForegroundRecording() {
        microphoneModule.stopRecording()
        speechModule.stopListening()
        _isRecording.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, RecordingForegroundService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PerceptNote")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Arrêter", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Enregistrement PerceptNote",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification d'enregistrement actif"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.perceptnote.START_RECORDING"
        const val ACTION_STOP = "com.perceptnote.STOP_RECORDING"
        const val EXTRA_SESSION_ID = "session_id"
        private const val CHANNEL_ID = "perceptnote_recording"
        private const val NOTIFICATION_ID = 1001

        fun startIntent(context: Context, sessionId: Long) =
            Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
            }

        fun stopIntent(context: Context) =
            Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
