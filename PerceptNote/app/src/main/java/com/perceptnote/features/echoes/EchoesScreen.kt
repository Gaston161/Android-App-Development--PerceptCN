// 📄 app/src/main/java/com/perceptnote/features/echoes/EchoesScreen.kt
package com.perceptnote.features.echoes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perceptnote.ui.theme.DyslexiaBackground
import com.perceptnote.ui.theme.PerceptNoteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoesScreen(
    sessionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EchoesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) { viewModel.loadSession(sessionId) }

    // Wrapper avec mode dyslexie conditionnel
    PerceptNoteTheme(dyslexiaMode = uiState.isDyslexiaMode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Hearing, null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("EchoesClass", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(uiState.sessionTitle, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Retour")
                        }
                    },
                    actions = {
                        // Toggle mode dyslexie
                        IconButton(onClick = viewModel::toggleDyslexiaMode) {
                            Icon(
                                Icons.Default.TextFields,
                                contentDescription = "Mode dyslexie",
                                tint = if (uiState.isDyslexiaMode)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // === Indicateur de section ===
                SectionProgressCard(
                    current = uiState.currentSectionIndex + 1,
                    total = uiState.notes.size,
                    statusMessage = uiState.statusMessage
                )

                // === Contenu de la note courante ===
                val currentNote = uiState.notes.getOrNull(uiState.currentSectionIndex)
                if (currentNote != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isDyslexiaMode)
                                DyslexiaBackground
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = currentNote.content,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                } else {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Aucune note disponible", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // === Navigation par gestes (indicateurs visuels) ===
                GestureNavHint()

                // === Contrôles principaux ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Précédent
                    FilledTonalIconButton(
                        onClick = viewModel::previousSection,
                        modifier = Modifier.size(56.dp),
                        enabled = uiState.currentSectionIndex > 0
                    ) {
                        Icon(Icons.Default.SkipPrevious, "Précédent", Modifier.size(28.dp))
                    }

                    // Lecture / Pause — bouton principal
                    val playColor by animateColorAsState(
                        targetValue = if (uiState.isPlaying) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        animationSpec = tween(300),
                        label = "playColor"
                    )
                    Surface(
                        onClick = viewModel::pauseOrResume,
                        shape = CircleShape,
                        color = playColor,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Lire",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Suivant
                    FilledTonalIconButton(
                        onClick = viewModel::nextSection,
                        modifier = Modifier.size(56.dp),
                        enabled = uiState.currentSectionIndex < uiState.notes.size - 1
                    ) {
                        Icon(Icons.Default.SkipNext, "Suivant", Modifier.size(28.dp))
                    }
                }

                // === Bouton commandes vocales ===
                OutlinedButton(
                    onClick = viewModel::startListeningForCommands,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Mic, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isListeningForCommands) "En écoute…" else "Commande vocale")
                }

                // Aide commandes
                Text(
                    text = "💬 Dis : \"Lis\", \"Pause\", \"Stop\", \"Suivant\", \"Précédent\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SectionProgressCard(current: Int, total: Int, statusMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (total > 0) "Note $current / $total" else "Aucune note",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { current.toFloat() / total },
                    modifier = Modifier.width(100.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GestureNavHint() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ArrowBack, null, Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Incline à gauche/droite pour naviguer",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.ArrowForward, null, Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
