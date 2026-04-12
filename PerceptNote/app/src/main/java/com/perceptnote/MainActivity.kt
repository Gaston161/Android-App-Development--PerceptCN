// 📄 app/src/main/java/com/perceptnote/MainActivity.kt — VERSION CLOUD
package com.perceptnote

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.perceptnote.auth.AuthManager
import com.perceptnote.auth.AuthState
import com.perceptnote.cloud.backup.AppStateManager
import com.perceptnote.cloud.sync.SyncWorker
import com.perceptnote.services.overlay.OverlayService
import com.perceptnote.ui.navigation.NavGraph
import com.perceptnote.ui.navigation.Routes
import com.perceptnote.ui.theme.PerceptNoteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var appStateManager: AppStateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val dyslexiaMode by appStateManager.dyslexiaMode.collectAsState(initial = false)
            val authState by authManager.authState.collectAsState(initial = AuthState.Loading)

            PerceptNoteTheme(dyslexiaMode = dyslexiaMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (authState) {
                        is AuthState.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is AuthState.Authenticated -> {
                            NavGraph(startDestination = Routes.HOME)
                        }
                        is AuthState.Unauthenticated, is AuthState.Error -> {
                            NavGraph(startDestination = Routes.LOGIN)
                        }
                    }
                }
            }
        }

        // Planifier la synchronisation WorkManager après connexion
        lifecycleScope.launch {
            authManager.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    SyncWorker.schedule(this@MainActivity)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val overlayEnabled = appStateManager.overlayEnabled.first()
            if (overlayEnabled && !Settings.canDrawOverlays(this@MainActivity)) {
                appStateManager.saveOverlayEnabled(false)
                stopService(OverlayService.stopIntent(this@MainActivity))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Gérer les deep links des notifications FCM
        intent.getStringExtra("deep_link")?.let { /* navigation deep link */ }
    }
}
