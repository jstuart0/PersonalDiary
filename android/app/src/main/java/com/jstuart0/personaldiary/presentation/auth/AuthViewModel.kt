package com.jstuart0.personaldiary.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstuart0.personaldiary.data.repository.AuthRepository
import com.jstuart0.personaldiary.domain.model.EncryptionTier
import com.jstuart0.personaldiary.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screens
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            if (isLoggedIn) {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    _uiState.value = AuthUiState.Authenticated(user)
                }
            }
        }
    }

    fun signup(email: String, password: String, encryptionTier: EncryptionTier) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.signup(email, password, encryptionTier)
            _uiState.value = result.fold(
                onSuccess = { user ->
                    if (encryptionTier == EncryptionTier.E2E) {
                        // Need to show recovery codes before completing signup
                        AuthUiState.RecoveryCodesRequired(user)
                    } else {
                        AuthUiState.Authenticated(user)
                    }
                },
                onFailure = { error ->
                    AuthUiState.Error(error.message ?: "Signup failed")
                }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.login(email, password)
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Authenticated(user) },
                onFailure = { error -> AuthUiState.Error(error.message ?: "Login failed") }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            authRepository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.requestPasswordReset(email)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.PasswordResetSent },
                onFailure = { error -> AuthUiState.Error(error.message ?: "Reset request failed") }
            )
        }
    }

    fun verifyRecoveryCode(email: String, recoveryCode: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.verifyRecoveryCode(email, recoveryCode)
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Authenticated(user) },
                onFailure = { error -> AuthUiState.Error(error.message ?: "Invalid recovery code") }
            )
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun acknowledgeRecoveryCodes() {
        val state = _uiState.value
        if (state is AuthUiState.RecoveryCodesRequired) {
            _uiState.value = AuthUiState.Authenticated(state.user)
        }
    }
}

/**
 * UI state for authentication
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val user: User) : AuthUiState()
    data class RecoveryCodesRequired(val user: User) : AuthUiState()
    object PasswordResetSent : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
