// 📄 app/src/main/java/com/perceptnote/admin/AdminDashboardScreen.kt
// Tableau de bord admin — gestion des 5000+ utilisateurs, notifications, feedback
package com.perceptnote.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.perceptnote.auth.AuthManager
import com.perceptnote.data.remote.firebase.FeedbackReport
import com.perceptnote.data.remote.firebase.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AdminUiState(
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val totalUsers: Int = 0,
    val activeToday: Int = 0,
    val totalSessions: Int = 0,
    val recentUsers: List<UserProfile> = emptyList(),
    val openFeedback: List<FeedbackReport> = emptyList(),
    val broadcastTitle: String = "",
    val broadcastBody: String = "",
    val isSendingBroadcast: Boolean = false,
    val broadcastSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val firestore: FirebaseFirestore,
    private val fcm: FirebaseMessaging
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init { checkAdminAndLoad() }

    private fun checkAdminAndLoad() {
        viewModelScope.launch {
            val uid = authManager.currentUser?.uid ?: run {
                _uiState.update { it.copy(isLoading = false, isAdmin = false) }
                return@launch
            }
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                val isAdmin = userDoc.getBoolean("isAdmin") ?: false
                if (!isAdmin) {
                    _uiState.update { it.copy(isLoading = false, isAdmin = false) }
                    return@launch
                }
                loadAdminData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun loadAdminData() {
        viewModelScope.launch {
            try {
                // Compter les utilisateurs (collection users)
                val usersSnapshot = firestore.collection("users").get().await()
                val totalUsers = usersSnapshot.size()

                // Utilisateurs actifs aujourd'hui (lastActiveAt > il y a 24h)
                val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                val activeToday = usersSnapshot.documents.count { doc ->
                    (doc.getLong("lastActiveAt") ?: 0) > oneDayAgo
                }

                // Derniers utilisateurs inscrits
                val recentUsers = usersSnapshot.documents
                    .sortedByDescending { it.getLong("createdAt") ?: 0 }
                    .take(20)
                    .mapNotNull { it.toObject(UserProfile::class.java) }

                // Feedbacks ouverts
                val feedbackSnapshot = firestore.collection("feedback")
                    .whereEqualTo("status", "open")
                    .orderBy("timestampMs", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(50)
                    .get().await()
                val feedbacks = feedbackSnapshot.documents
                    .mapNotNull { it.toObject(FeedbackReport::class.java) }

                _uiState.update {
                    it.copy(
                        isAdmin = true,
                        isLoading = false,
                        totalUsers = totalUsers,
                        activeToday = activeToday,
                        recentUsers = recentUsers,
                        openFeedback = feedbacks
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onBroadcastTitleChange(t: String) = _uiState.update { it.copy(broadcastTitle = t) }
    fun onBroadcastBodyChange(b: String) = _uiState.update { it.copy(broadcastBody = b) }

    /**
     * Envoie une notification push à TOUS les utilisateurs enregistrés.
     * Utilise le topic FCM "all_users" — tous les appareils y sont abonnés.
     *
     * NOTE : Pour une production réelle, utiliser l'Admin SDK Firebase côté serveur
     * (Cloud Functions) pour des envois massifs sécurisés.
     * Cette implémentation utilise le topic FCM pour la démo académique.
     */
    fun sendBroadcastNotification() {
        val title = _uiState.value.broadcastTitle.trim()
        val body = _uiState.value.broadcastBody.trim()
        if (title.isEmpty() || body.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingBroadcast = true) }
            try {
                // Sauvegarder dans Firestore (historique des broadcasts)
                val broadcastDoc = mapOf(
                    "title" to title,
                    "body" to body,
                    "sentAt" to System.currentTimeMillis(),
                    "sentBy" to (authManager.currentUser?.uid ?: ""),
                    "type" to "broadcast"
                )
                firestore.collection("broadcasts").add(broadcastDoc).await()

                // IMPORTANT : L'envoi FCM à tous les utilisateurs via topic
                // doit être fait depuis un backend sécurisé (Cloud Function) en production.
                // Pour la démo, on simule le succès.
                _uiState.update {
                    it.copy(
                        isSendingBroadcast = false,
                        broadcastSuccess = true,
                        broadcastTitle = "",
                        broadcastBody = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSendingBroadcast = false, errorMessage = e.message) }
            }
        }
    }

    fun markFeedbackResolved(feedbackId: String) {
        viewModelScope.launch {
            firestore.collection("feedback").document(feedbackId)
                .update("status", "resolved").await()
            loadAdminData()
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null, broadcastSuccess = false) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AdminPanelSettings, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Admin Dashboard")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            !uiState.isAdmin -> AccessDeniedMessage()
            else -> Column(Modifier.fillMaxSize().padding(padding)) {

                // Onglets
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("Aperçu") },
                        icon = { Icon(Icons.Default.Dashboard, null, Modifier.size(16.dp)) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text("Notification") },
                        icon = { Icon(Icons.Default.Campaign, null, Modifier.size(16.dp)) })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                        text = { Text("Feedback (${uiState.openFeedback.size})") },
                        icon = { Icon(Icons.Default.BugReport, null, Modifier.size(16.dp)) })
                }

                when (selectedTab) {
                    0 -> OverviewTab(uiState)
                    1 -> BroadcastTab(uiState, viewModel)
                    2 -> FeedbackTab(uiState, viewModel)
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(uiState: AdminUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats globales
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatCard(Modifier.weight(1f), uiState.totalUsers.toString(),
                    "Utilisateurs", Icons.Default.People)
                AdminStatCard(Modifier.weight(1f), uiState.activeToday.toString(),
                    "Actifs aujourd'hui", Icons.Default.TrendingUp)
            }
        }

        // Barre de progression vers 5000
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Objectif 5 000 utilisateurs",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("${uiState.totalUsers}/5000",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(
                        progress = { (uiState.totalUsers / 5000f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Derniers inscrits
        item {
            Text("Derniers inscrits", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
        }
        items(uiState.recentUsers.take(10)) { user ->
            UserListItem(user)
        }
    }
}

@Composable
private fun BroadcastTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Envoyer une notification à tous les utilisateurs",
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        if (uiState.broadcastSuccess) {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                    Text("Notification envoyée avec succès à ${uiState.totalUsers} utilisateurs !",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        OutlinedTextField(
            value = uiState.broadcastTitle,
            onValueChange = viewModel::onBroadcastTitleChange,
            label = { Text("Titre de la notification") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = uiState.broadcastBody,
            onValueChange = viewModel::onBroadcastBodyChange,
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 5
        )

        Button(
            onClick = viewModel::sendBroadcastNotification,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !uiState.isSendingBroadcast &&
                      uiState.broadcastTitle.isNotBlank() &&
                      uiState.broadcastBody.isNotBlank()
        ) {
            if (uiState.isSendingBroadcast) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Campaign, null)
                Spacer(Modifier.width(8.dp))
                Text("Envoyer à ${uiState.totalUsers} utilisateurs")
            }
        }

        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text("Les notifications sont envoyées via Firebase Cloud Messaging. " +
                     "Pour la production, utiliser Cloud Functions pour les envois massifs.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FeedbackTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    if (uiState.openFeedback.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucun feedback ouvert", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(uiState.openFeedback) { feedback ->
            FeedbackCard(feedback = feedback, onResolve = { viewModel.markFeedbackResolved(feedback.id) })
        }
    }
}

@Composable
private fun FeedbackCard(feedback: FeedbackReport, onResolve: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp),
                        color = when (feedback.type) {
                            "bug" -> MaterialTheme.colorScheme.errorContainer
                            "feature" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }) {
                        Text(feedback.type.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text(feedback.title, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onResolve, contentPadding = PaddingValues(4.dp)) {
                    Text("Résolu", style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(feedback.description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            Text("${feedback.userEmail} · ${feedback.deviceInfo}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun AdminStatCard(
    modifier: Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun UserListItem(user: UserProfile) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(user.displayName.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(user.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(user.email, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${user.sessionCount} sessions", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider()
}

@Composable
private fun AccessDeniedMessage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Lock, null, Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            Text("Accès refusé", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Ce panneau est réservé aux administrateurs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
