// 📄 app/src/main/java/com/perceptnote/features/feedback/FeedbackScreen.kt
package com.perceptnote.features.feedback

import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perceptnote.data.remote.firebase.FeedbackReport
import com.perceptnote.data.remote.firebase.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackUiState(
    val type: String = "bug",
    val title: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun onTypeChange(type: String) = _uiState.update { it.copy(type = type) }
    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }
    fun onDescriptionChange(desc: String) = _uiState.update { it.copy(description = desc) }

    fun submit() {
        val state = _uiState.value
        if (state.title.isBlank() || state.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Titre et description requis") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val report = FeedbackReport(
                type = state.type,
                title = state.title,
                description = state.description,
                deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})",
                appVersion = com.perceptnote.BuildConfig.VERSION_NAME
            )
            userRepository.submitFeedback(report)
                .onSuccess { _uiState.update { it.copy(isSuccess = true, isSubmitting = false) } }
                .onFailure { e -> _uiState.update { it.copy(isSubmitting = false, errorMessage = e.message) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onNavigateBack: () -> Unit,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isSuccess) {
        SuccessCard(onNavigateBack = onNavigateBack)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signaler un problème") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type de feedback
            Text("Type de retour", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "bug" to "🐛 Bug",
                    "feature" to "💡 Suggestion",
                    "general" to "💬 Général"
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = uiState.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(label) }
                    )
                }
            }

            // Titre
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Titre court") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description détaillée") },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                maxLines = 8
            )

            // Info automatique
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(16.dp))
                    Text(
                        "Inclus automatiquement : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}, App v${com.perceptnote.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Envoyer le rapport")
                }
            }
        }
    }
}

@Composable
private fun SuccessCard(onNavigateBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Merci pour ton retour !", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Ton rapport a été envoyé. Nous reviendrons vers toi si nécessaire.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onNavigateBack) { Text("Retour à l'accueil") }
        }
    }
}
