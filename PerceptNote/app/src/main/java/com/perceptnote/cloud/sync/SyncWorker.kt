// 📄 app/src/main/java/com/perceptnote/cloud/sync/SyncWorker.kt
// 📦 Gradle : work-runtime, hilt-work
package com.perceptnote.cloud.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.perceptnote.domain.repository.NoteRepository
import com.perceptnote.domain.repository.SessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Worker de synchronisation automatique en arrière-plan.
 * Planifié par WorkManager — s'exécute même si l'app est fermée.
 *
 * Planification : toutes les 6h, uniquement si réseau disponible.
 * Utiliser SyncWorker.schedule() pour enregistrer le worker.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sessionRepository: SessionRepository,
    private val noteRepository: NoteRepository,
    private val cloudSyncRepository: CloudSyncRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Récupérer toutes les sessions locales
            val sessions = sessionRepository.getAllSessions().first()

            // 2. Pour chaque session non encore synchronisée
            var syncedCount = 0
            sessions.forEach { session ->
                if (!cloudSyncRepository.isSessionSynced(session.id)) {
                    // Sauvegarder la session dans Firestore
                    cloudSyncRepository.backupSession(session)
                        .onSuccess {
                            // Sauvegarder les notes associées
                            val notes = noteRepository.getNotesBySession(session.id).first()
                            cloudSyncRepository.backupNotes(notes)
                            // Upload du fichier audio si présent
                            session.audioFilePath?.let { path ->
                                cloudSyncRepository.uploadAudioFile(session.id, path)
                            }
                            syncedCount++
                        }
                }
            }

            Result.success(
                workDataOf("synced_sessions" to syncedCount)
            )
        } catch (e: Exception) {
            // Retry automatique si erreur réseau
            if (runAttemptCount < 3) Result.retry()
            else Result.failure(workDataOf("error" to e.message))
        }
    }

    companion object {
        const val WORK_NAME = "perceptnote_sync"

        /**
         * Planifie la synchronisation périodique.
         * Appeler dans MainActivity après connexion réussie.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Ne pas remplacer si déjà planifié
                request
            )
        }

        /** Déclenche une synchronisation immédiate (one-shot) */
        fun triggerNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /** Annule la synchronisation périodique */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
