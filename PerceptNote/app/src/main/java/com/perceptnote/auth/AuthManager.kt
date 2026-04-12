// 📄 app/src/main/java/com/perceptnote/auth/AuthManager.kt
// 📦 Gradle : firebase-auth, credentials, credentials-play, google-id, biometric
// 🔑 Permissions : USE_BIOMETRIC
package com.perceptnote.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.fragment.app.FragmentActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.perceptnote.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class BiometricAvailability {
    object Available : BiometricAvailability()
    object NotEnrolled : BiometricAvailability()
    object NotAvailable : BiometricAvailability()
}

/**
 * Gestionnaire d'authentification unifié.
 * Gère : Google Sign-In (Credential Manager) + Biométrie (empreinte/face).
 *
 * Flux recommandé :
 * 1. Premier lancement → Google Sign-In obligatoire
 * 2. Connexions suivantes → Biométrie si disponible (sinon retour Google Sign-In)
 */
@Singleton
class AuthManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val context: Context
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Observer l'état d'auth Firebase
        auth.addAuthStateListener { firebaseAuth ->
            _authState.value = firebaseAuth.currentUser?.let {
                AuthState.Authenticated(it)
            } ?: AuthState.Unauthenticated
        }
    }

    // ==============================
    // GOOGLE SIGN-IN (Credential Manager)
    // ==============================

    /**
     * Lance le flux Google Sign-In via Credential Manager (API moderne).
     * Remplace l'ancien GoogleSignInClient déprécié.
     *
     * IMPORTANT : nécessite un Web Client ID dans google-services.json.
     */
    suspend fun signInWithGoogle(activity: FragmentActivity): Result<FirebaseUser> {
        return try {
            _authState.value = AuthState.Loading
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Montre tous les comptes Google
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(true) // Auto-sélection si un seul compte
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                val googleIdToken = GoogleIdTokenCredential
                    .createFrom(credential.data).idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()

                authResult.user?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Connexion Google échouée"))
            } else {
                Result.failure(Exception("Type de credential non reconnu"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Erreur inconnue")
            Result.failure(e)
        }
    }

    // ==============================
    // BIOMÉTRIE (Empreinte / Face ID)
    // ==============================

    /**
     * Vérifie si la biométrie est disponible et configurée sur l'appareil.
     */
    fun getBiometricAvailability(): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NotEnrolled
            else -> BiometricAvailability.NotAvailable
        }
    }

    /**
     * Lance le prompt biométrique.
     * Si l'utilisateur a déjà un compte Firebase actif → utilisé pour confirmer l'identité.
     * Sinon → redirige vers Google Sign-In.
     *
     * @param activity L'activité hôte pour afficher le dialog biométrique
     * @param onSuccess Callback en cas de succès biométrique
     * @param onFallback Callback si biométrie échoue → retour au Google Sign-In
     */
    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFallback: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Si pas de session Firebase active → impossible d'utiliser la biométrie seule
        if (auth.currentUser == null) {
            onFallback()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onFallback()
                        else -> onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Biométrie non reconnue — l'utilisateur peut réessayer
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Connexion à PerceptNote")
            .setSubtitle("Utilisez votre empreinte ou votre visage pour vous connecter")
            .setNegativeButtonText("Utiliser mon compte Google")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    val isAuthenticated: Boolean get() = auth.currentUser != null
    val currentUser: FirebaseUser? get() = auth.currentUser
}
