// 📄 HomeScreen.kt — version cloud avec profile/settings/admin
package com.perceptnote.features.home

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perceptnote.domain.model.Session
import com.perceptnote.ui.theme.GpsOrange
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartCapture: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PerceptNote", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Pose ton téléphone. Le reste est automatique.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Paramètres") }
                    IconButton(onClick = onOpenProfile) { Icon(Icons.Default.AccountCircle, "Profil") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartCapture,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nouvelle session") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (uiState.hasNearbyNotification && uiState.nearbySessions.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GpsOrange.copy(alpha = 0.12f))) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = GpsOrange)
                            Text("Lieu connu ! ${uiState.nearbySessions.size} session(s) à proximité.",
                                style = MaterialTheme.typography.bodyMedium, color = GpsOrange, modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::dismissNearbyNotification, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LibraryBooks, null, tint = MaterialTheme.colorScheme.primary)
                            Text(uiState.sessions.size.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Sessions", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Sensors, null, tint = MaterialTheme.colorScheme.primary)
                            Text("5", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Capteurs", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AdminPanelSettings, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Admin Dashboard")
                }
            }

            item { Text("Mes sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }

            if (uiState.sessions.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.SelfImprovement, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Text("Aucune session", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = onStartCapture) { Text("Commencer") }
                    }
                }
            } else {
                items(uiState.sessions, key = { it.id }) { session ->
                    var showDelete by remember { mutableStateOf(false) }
                    Card(onClick = { onOpenSession(session.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LibraryBooks, null, tint = MaterialTheme.colorScheme.primary) }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(session.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.FRENCH).format(Date(session.startTimestampMs)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, null) }
                        }
                    }
                    if (showDelete) {
                        AlertDialog(onDismissRequest = { showDelete = false },
                            title = { Text("Supprimer ?") },
                            confirmButton = { TextButton(onClick = { viewModel.deleteSession(session); showDelete = false }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) } },
                            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Annuler") } })
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
