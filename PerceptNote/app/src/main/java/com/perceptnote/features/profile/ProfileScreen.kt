// 📄 app/src/main/java/com/perceptnote/features/profile/ProfileScreen.kt
package com.perceptnote.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.perceptnote.auth.AuthManager
import com.perceptnote.cloud.sync.CloudSyncRepository
import com.perceptnote.cloud.sync.SyncWorker
import com.perceptnote.data.remote.firebase.UserProfile
import com.perceptnote.data.remote.firebase.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val userRepository: UserRepository,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            userRepository.getUserProfile()
                .onSuccess { profile -> _uiState.update { it.copy(profile = profile, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
        }
    }

    fun triggerManualSync(context: android.content.Context) {
        _uiState.update { it.copy(isSyncing = true) }
        SyncWorker.triggerNow(context)
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(isSyncing = false, lastSyncMessage = "Synchronisation lancée") }
        }
    }

    fun logout() = authManager.signOut()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onOpenFeedback: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon profil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // === Avatar ===
            uiState.profile?.photoUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Photo de profil",
                    modifier = Modifier.size(88.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } ?: Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = uiState.profile?.displayName ?: "Utilisateur",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = uiState.profile?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // === Statistiques ===
            uiState.profile?.let { profile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(Modifier.weight(1f), profile.sessionCount.toString(), "Sessions")
                    StatCard(Modifier.weight(1f), profile.totalNotes.toString(), "Notes")
                    StatCard(Modifier.weight(1f), profile.subscriptionTier.uppercase(), "Plan")
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            // === Actions ===
            ProfileAction(
                icon = Icons.Default.CloudSync,
                title = "Synchroniser maintenant",
                subtitle = uiState.lastSyncMessage ?: "Sauvegarder toutes les sessions",
                isLoading = uiState.isSyncing,
                onClick = { viewModel.triggerManualSync(context) }
            )
            ProfileAction(
                icon = Icons.Default.BugReport,
                title = "Signaler un problème",
                subtitle = "Envoyer un rapport de bug ou une suggestion",
                onClick = onOpenFeedback
            )
            ProfileAction(
                icon = Icons.Default.Security,
                title = "Confidentialité",
                subtitle = "Tes données sont chiffrées et privées",
                onClick = {}
            )
            ProfileAction(
                icon = Icons.Default.Logout,
                title = "Se déconnecter",
                subtitle = "Tu devras te reconnecter pour accéder à tes données",
                isDestructive = true,
                onClick = { showLogoutDialog = true }
            )

            Spacer(Modifier.height(32.dp))
            Text(
                "PerceptNote v${com.perceptnote.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Se déconnecter ?") },
            text = { Text("Tes données restent sauvegardées dans le cloud. Tu pourras te reconnecter sur n'importe quel appareil.") },
            confirmButton = {
                TextButton(onClick = { viewModel.logout(); onLogout() }) {
                    Text("Déconnecter", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun ProfileAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isLoading: Boolean = false,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    color = if (isDestructive) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(Modifier.padding(start = 56.dp))
}
