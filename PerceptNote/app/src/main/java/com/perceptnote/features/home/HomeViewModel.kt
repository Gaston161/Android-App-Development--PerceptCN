// 📄 app/src/main/java/com/perceptnote/features/home/HomeViewModel.kt
package com.perceptnote.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptnote.domain.model.Session
import com.perceptnote.domain.repository.SessionRepository
import com.perceptnote.domain.usecase.GetNotesByLocationUseCase
import com.perceptnote.sensors.location.LocationModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val sessions: List<Session> = emptyList(),
    val nearbySessions: List<Session> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasNearbyNotification: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getNotesByLocationUseCase: GetNotesByLocationUseCase,
    private val locationModule: LocationModule
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeSessions()
        checkNearbyLocation()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            sessionRepository.getAllSessions()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { sessions ->
                    _uiState.update { it.copy(sessions = sessions, isLoading = false) }
                }
        }
    }

    /** Vérifie si des notes ont été prises à la position actuelle (rayon 20m) */
    fun checkNearbyLocation() {
        viewModelScope.launch {
            val location = locationModule.getLastLocation() ?: return@launch
            val nearby = getNotesByLocationUseCase(location.latitude, location.longitude)
            _uiState.update {
                it.copy(
                    nearbySessions = nearby,
                    hasNearbyNotification = nearby.isNotEmpty()
                )
            }
        }
    }

    fun dismissNearbyNotification() {
        _uiState.update { it.copy(hasNearbyNotification = false) }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            sessionRepository.deleteSession(session)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
