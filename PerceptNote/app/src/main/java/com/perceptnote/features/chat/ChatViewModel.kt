package com.perceptnote.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptnote.data.remote.AIApiService
import com.perceptnote.data.remote.dto.Message
import com.perceptnote.data.remote.dto.MessageRequest
import com.perceptnote.data.remote.dto.extractText
import com.perceptnote.domain.repository.NoteRepository
import com.perceptnote.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val id: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val sessionContext: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiApi: AIApiService,
    private val noteRepository: NoteRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Historique des messages pour le contexte multi-tour
    private val conversationHistory = mutableListOf<Message>()

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionRepository.getSessionById(sessionId)
            noteRepository.getNotesBySession(sessionId)
                .first()
                .let { notes ->
                    val context = buildString {
                        append("Session : ${session?.title}\n")
                        append("Date : ${java.util.Date(session?.startTimestampMs ?: 0)}\n\n")
                        append("Notes capturées :\n")
                        notes.forEach { note -> append("[${note.type}] ${note.content}\n") }
                    }
                    _uiState.update { it.copy(sessionContext = context) }
                    // Message d'accueil de l'IA
                    addAssistantMessage("Bonjour ! J'ai analysé tes notes de la session **${session?.title}**. Pose-moi n'importe quelle question sur le contenu ! 🎓")
                }
        }
    }

    fun onInputChange(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isLoading) return

        _uiState.update { it.copy(
            messages = it.messages + ChatMessage(text, isUser = true),
            inputText = "",
            isLoading = true
        )}

        conversationHistory.add(Message(role = "user", content = text))

        viewModelScope.launch {
            try {
                val response = aiApi.sendMessage(
                    MessageRequest(
                        messages = listOf(
                            Message(
                                role = "system",
                                content = """Tu es un assistant académique expert. 
                                    |Tu réponds aux questions de l'étudiant basées sur ses notes de cours.
                                    |Contexte des notes :\n${_uiState.value.sessionContext}
                                    |Réponds en français, de manière concise et pédagogique.""".trimMargin()
                            )
                        ) + conversationHistory,
                        maxTokens = 800
                    )
                )
                val reply = response.extractText()
                conversationHistory.add(Message(role = "assistant", content = reply))
                addAssistantMessage(reply)
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = "Erreur réseau : ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    private fun addAssistantMessage(content: String) {
        _uiState.update { it.copy(
            messages = it.messages + ChatMessage(content, isUser = false),
            isLoading = false
        )}
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}