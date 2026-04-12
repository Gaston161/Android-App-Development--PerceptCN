// 📄 app/src/main/java/com/perceptnote/PerceptNoteApplication.kt
package com.perceptnote

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application principale — point d'entrée Hilt.
 * Implémente Configuration.Provider pour WorkManager + Hilt.
 */
@HiltAndroidApp
class PerceptNoteApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
    }
}
