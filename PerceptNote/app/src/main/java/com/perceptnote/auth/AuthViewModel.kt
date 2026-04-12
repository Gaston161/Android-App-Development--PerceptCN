// 📄 app/src/main/java/com/perceptnote/auth/AuthViewModel.kt
package com.perceptnote.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptnote.data.remote.firebase.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val biometricAvailable: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
        checkBiometricAvailability()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        _uiState.update {
                            it.copy(
                                isAuthenticated = true,
                                isLoading = false,
                                displayName = state.user.displayName ?: "",
                                email = state.user.email ?: "",
                                photoUrl = state.user.photoUrl?.toString()
                            )
                        }
                        // Créer/mettre à jour le profil Firestore
                        userRepository.createOrUpdateUserProfile(
                            appVersion = com.perceptnote.BuildConfig.VERSION_NAME,
                            deviceModel = android.os.Build.MODEL
                        )
                    }
                    is AuthState.Unauthenticated ->
                        _uiState.update { it.copy(isAuthenticated = false, isLoading = false) }
                    is AuthState.Loading ->
                        _uiState.update { it.copy(isLoading = true) }
                    is AuthState.Error ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = state.message) }
                }
            }
        }
    }

    private fun checkBiometricAvailability() {
        val available = authManager.getBiometricAvailability() == BiometricAvailability.Available
        _uiState.update { it.copy(biometricAvailable = available) }
    }

    fun signInWithGoogle(activity: FragmentActivity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authManager.signInWithGoogle(activity)
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
        }
    }

    fun tryBiometricLogin(activity: FragmentActivity, onSuccess: () -> Unit) {
        authManager.authenticateWithBiometrics(
            activity = activity,
            onSuccess = {
                _uiState.update { it.copy(isAuthenticated = true) }
                onSuccess()
            },
            onFallback = { /* Rester sur l'écran de login pour Google Sign-In */ },
            onError = { msg -> _uiState.update { it.copy(errorMessage = msg) } }
        )
    }

    fun signOut() {
        authManager.signOut()
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
