// 📄 app/src/main/java/com/perceptnote/features/notes/NotesViewModel.kt
package com.perceptnote.features.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptnote.domain.model.Note
import com.perceptnote.domain.model.Session
import com.perceptnote.domain.repository.NoteRepository
import com.perceptnote.domain.repository.SessionRepository
import com.perceptnote.domain.usecase.GenerateQuizUseCase
import com.perceptnote.domain.usecase.GenerateSummaryUseCase
import com.perceptnote.domain.usecase.QuizQuestion
import com.perceptnote.domain.usecase.TranslateNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val session: Session? = null,
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val isSummaryLoading: Boolean = false,
    val isQuizLoading: Boolean = false,
    val summary: String? = null,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val errorMessage: String? = null,
    val showQuiz: Boolean = false
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val sessionRepository: SessionRepository,
    private val generateSummaryUseCase: GenerateSummaryUseCase,
    private val generateQuizUseCase: GenerateQuizUseCase,
    private val translateNoteUseCase: TranslateNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState(isLoading = true))
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionRepository.getSessionById(sessionId)
            _uiState.update { it.copy(session = session, summary = session?.summary) }
            noteRepository.getNotesBySession(sessionId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { notes ->
                    _uiState.update { it.copy(notes = notes, isLoading = false) }
                }
        }
    }

    fun generateSummary() {
        val sessionId = _uiState.value.session?.id ?: return
        val notes = _uiState.value.notes
        if (notes.isEmpty()) return
        val content = notes.joinToString("\n") { "[${it.type.name}] ${it.content}" }

        viewModelScope.launch {
            _uiState.update { it.copy(isSummaryLoading = true) }
            generateSummaryUseCase(sessionId, content)
                .onSuccess { summary -> _uiState.update { it.copy(summary = summary, isSummaryLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message, isSummaryLoading = false) } }
        }
    }

    fun generateQuiz() {
        val notes = _uiState.value.notes
        if (notes.isEmpty()) return
        val content = notes.joinToString("\n") { it.content }

        viewModelScope.launch {
            _uiState.update { it.copy(isQuizLoading = true) }
            generateQuizUseCase(content)
                .onSuccess { questions ->
                    _uiState.update { it.copy(quizQuestions = questions, isQuizLoading = false, showQuiz = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message, isQuizLoading = false) }
                }
        }
    }

    fun translateNotes(targetLanguage: String) {
        val notes = _uiState.value.notes
        if (notes.isEmpty()) return
        viewModelScope.launch {
            val combined = notes.joinToString("\n---\n") { it.content }
            translateNoteUseCase(combined, targetLanguage)
                .onSuccess { translated ->
                    _uiState.update { it.copy(summary = "**Traduction ($targetLanguage) :**\n\n$translated") }
                }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun dismissQuiz() = _uiState.update { it.copy(showQuiz = false) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
