// 📄 app/src/main/java/com/perceptnote/features/notes/NoteDetailScreen.kt
package com.perceptnote.features.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perceptnote.domain.usecase.QuizQuestion
import com.perceptnote.ui.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    sessionId: Long,
    onOpenChat: () -> Unit,
    onOpenEchoes: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTranslateDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(sessionId) { viewModel.loadSession(sessionId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.session?.title ?: "Session",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenEchoes) {
                        Icon(Icons.Default.Hearing, "EchoesClass")
                    }
                    IconButton(onClick = { showTranslateDialog = true }) {
                        Icon(Icons.Default.Translate, "Traduire")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // === Onglets : Notes / Résumé IA ===
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Notes (${uiState.notes.size})") },
                    icon = { Icon(Icons.Default.Notes, null, Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Résumé IA") },
                    icon = { Icon(Icons.Default.Psychology, null, Modifier.size(16.dp)) }
                )
            }

            when (selectedTab) {
                0 -> NotesTab(uiState = uiState, onOpenChat = onOpenChat)
                1 -> SummaryTab(uiState = uiState, viewModel = viewModel)
            }
        }
    }

    // Quiz modal
    if (uiState.showQuiz && uiState.quizQuestions.isNotEmpty()) {
        QuizDialog(
            questions = uiState.quizQuestions,
            onDismiss = viewModel::dismissQuiz
        )
    }

    // Dialog traduction
    if (showTranslateDialog) {
        TranslateDialog(
            onTranslate = { lang ->
                viewModel.translateNotes(lang)
                selectedTab = 1
                showTranslateDialog = false
            },
            onDismiss = { showTranslateDialog = false }
        )
    }

    // Snackbar erreurs
    uiState.errorMessage?.let {
        LaunchedEffect(it) { viewModel.clearError() }
    }
}

@Composable
private fun NotesTab(uiState: NotesUiState, onOpenChat: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bouton Chat IA
        item {
            Button(
                onClick = onOpenChat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Chat, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Poser une question sur ces notes")
            }
        }

        if (uiState.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(uiState.notes, key = { it.id }) { note ->
                NoteCard(note = note)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SummaryTab(uiState: NotesUiState, viewModel: NotesViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Actions IA
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = viewModel::generateSummary,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSummaryLoading && uiState.notes.isNotEmpty()
            ) {
                if (uiState.isSummaryLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("Résumer")
            }
            OutlinedButton(
                onClick = viewModel::generateQuiz,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isQuizLoading && uiState.notes.isNotEmpty()
            ) {
                if (uiState.isQuizLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Quiz, null, Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("Quiz")
            }
        }

        // Contenu résumé
        if (uiState.summary != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = uiState.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        } else if (!uiState.isSummaryLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Psychology,
                        null,
                        Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Appuie sur Résumer pour générer une fiche IA",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizDialog(questions: List<QuizQuestion>, onDismiss: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableIntStateOf(-1) }
    var showResult by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    val question = questions.getOrNull(currentIndex) ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Quiz — Question ${currentIndex + 1}/${questions.size}",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(question.question, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                question.options.forEachIndexed { index, option ->
                    val containerColor = when {
                        !showResult -> if (selectedAnswer == index)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                        index == question.correctIndex -> MaterialTheme.colorScheme.tertiaryContainer
                        selectedAnswer == index -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                    Surface(
                        onClick = { if (!showResult) selectedAnswer = index },
                        shape = RoundedCornerShape(8.dp),
                        color = containerColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${('A' + index)}. $option",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (showResult && question.explanation.isNotEmpty()) {
                    Text(
                        text = "💡 ${question.explanation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!showResult) {
                    showResult = true
                    if (selectedAnswer == question.correctIndex) score++
                } else {
                    if (currentIndex < questions.lastIndex) {
                        currentIndex++
                        selectedAnswer = -1
                        showResult = false
                    } else {
                        onDismiss()
                    }
                }
            }) {
                Text(if (!showResult) "Valider" else if (currentIndex < questions.lastIndex) "Suivant" else "Terminer ($score/${questions.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Quitter") }
        }
    )
}

@Composable
private fun TranslateDialog(onTranslate: (String) -> Unit, onDismiss: () -> Unit) {
    val languages = listOf("Anglais", "Espagnol", "Allemand", "Arabe", "Chinois")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Traduire les notes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                languages.forEach { lang ->
                    TextButton(
                        onClick = { onTranslate(lang) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(lang) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
