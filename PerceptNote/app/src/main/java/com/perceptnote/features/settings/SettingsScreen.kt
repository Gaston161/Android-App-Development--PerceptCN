// 📄 app/src/main/java/com/perceptnote/features/settings/SettingsScreen.kt
package com.perceptnote.features.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perceptnote.cloud.backup.AppStateManager
import com.perceptnote.services.overlay.OverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val dyslexiaMode: Boolean = false,
    val overlayEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val autoBackup: Boolean = true,
    val notificationsEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appStateManager: AppStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appStateManager.dyslexiaMode,
                appStateManager.overlayEnabled,
                appStateManager.biometricEnabled
            ) { dyslexia, overlay, biometric ->
                SettingsUiState(
                    dyslexiaMode = dyslexia,
                    overlayEnabled = overlay,
                    biometricEnabled = biometric
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun toggleDyslexia(enabled: Boolean) {
        viewModelScope.launch { appStateManager.saveDyslexiaMode(enabled) }
    }

    fun toggleOverlay(enabled: Boolean, context: android.content.Context) {
        viewModelScope.launch {
            if (enabled && !Settings.canDrawOverlays(context)) {
                // Ouvrir les paramètres système pour accorder la permission
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                return@launch
            }
            appStateManager.saveOverlayEnabled(enabled)
            if (enabled) context.startService(OverlayService.startIntent(context))
            else context.stopService(OverlayService.stopIntent(context))
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch { appStateManager.saveBiometricEnabled(enabled) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // === SECTION ACCESSIBILITÉ ===
            SettingsSectionHeader("Accessibilité")

            SettingsToggle(
                icon = Icons.Default.TextFields,
                title = "Mode dyslexie",
                subtitle = "Police adaptée, espacement renforcé, fond jaune clair",
                checked = uiState.dyslexiaMode,
                onCheckedChange = viewModel::toggleDyslexia
            )

            // === SECTION SÉCURITÉ ===
            SettingsSectionHeader("Sécurité & Connexion")

            SettingsToggle(
                icon = Icons.Default.Fingerprint,
                title = "Connexion biométrique",
                subtitle = "Utiliser empreinte ou visage pour se connecter",
                checked = uiState.biometricEnabled,
                onCheckedChange = viewModel::toggleBiometric
            )

            // === SECTION FONCTIONNALITÉS ===
            SettingsSectionHeader("Fonctionnalités avancées")

            SettingsToggle(
                icon = Icons.Default.Layers,
                title = "Bulle flottante (overlay)",
                subtitle = "Afficher PerceptNote au-dessus des autres apps",
                checked = uiState.overlayEnabled,
                onCheckedChange = { viewModel.toggleOverlay(it, context) }
            )

            SettingsToggle(
                icon = Icons.Default.CloudSync,
                title = "Sauvegarde automatique",
                subtitle = "Synchroniser les notes toutes les 6h via Wi-Fi",
                checked = uiState.autoBackup,
                onCheckedChange = { /* TODO */ }
            )

            // === INFO OVERLAY ===
            if (!Settings.canDrawOverlays(context)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp))
                        Text(
                            "Permission d'overlay non accordée. Active-la dans Paramètres > Apps > PerceptNote.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
    HorizontalDivider(Modifier.padding(start = 56.dp))
}
