package com.jstuart0.personaldiary.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jstuart0.personaldiary.data.repository.AuthRepository
import com.jstuart0.personaldiary.data.repository.SocialRepository
import com.jstuart0.personaldiary.domain.model.SocialAccount
import com.jstuart0.personaldiary.domain.model.User
import com.jstuart0.personaldiary.service.AppLockManager
import com.jstuart0.personaldiary.service.BiometricAuthManager
import com.jstuart0.personaldiary.service.BiometricAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val socialRepository: SocialRepository,
    private val appLockManager: AppLockManager,
    private val biometricManager: BiometricAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val socialAccounts: StateFlow<List<SocialAccount>> = socialRepository.getSocialAccounts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isAppLockEnabled: StateFlow<Boolean> = appLockManager.isAppLockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isBiometricEnabled: StateFlow<Boolean> = appLockManager.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val lockTimeout: StateFlow<Long> = appLockManager.lockTimeout
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLockManager.TIMEOUT_IMMEDIATE)

    val isAutoLockEnabled: StateFlow<Boolean> = appLockManager.isAutoLockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val biometricAvailability: BiometricAvailability = biometricManager.isBiometricAvailable()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = SettingsUiState.Ready
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to load settings")
            }
        }
    }

    /**
     * Update app lock setting
     */
    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appLockManager.setAppLockEnabled(enabled)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update setting")
            }
        }
    }

    /**
     * Update biometric setting
     */
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled && !biometricAvailability.isAvailable()) {
                    _uiState.value = SettingsUiState.Error(biometricAvailability.getMessage())
                    return@launch
                }
                appLockManager.setBiometricEnabled(enabled)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update setting")
            }
        }
    }

    /**
     * Update lock timeout
     */
    fun setLockTimeout(timeout: Long) {
        viewModelScope.launch {
            try {
                appLockManager.setLockTimeout(timeout)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update setting")
            }
        }
    }

    /**
     * Update auto lock setting
     */
    fun setAutoLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appLockManager.setAutoLockEnabled(enabled)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to update setting")
            }
        }
    }

    /**
     * Disconnect social account
     */
    fun disconnectSocialAccount(accountId: String) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                val result = socialRepository.disconnectAccount(accountId)
                result.onSuccess {
                    _uiState.value = SettingsUiState.Ready
                }.onFailure { error ->
                    _uiState.value = SettingsUiState.Error(
                        error.message ?: "Failed to disconnect account"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to disconnect account")
            }
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                appLockManager.reset()
                _uiState.value = SettingsUiState.LoggedOut
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "Failed to logout")
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        if (_uiState.value is SettingsUiState.Error) {
            _uiState.value = SettingsUiState.Ready
        }
    }

    /**
     * Get lock timeout display name
     */
    fun getLockTimeoutDisplayName(timeout: Long): String {
        return appLockManager.getLockTimeoutDisplayName(timeout)
    }

    /**
     * Get all lock timeout options
     */
    fun getLockTimeoutOptions(): List<Pair<Long, String>> {
        return appLockManager.getLockTimeoutOptions()
    }
}

/**
 * UI state for settings screen
 */
sealed class SettingsUiState {
    object Loading : SettingsUiState()
    object Ready : SettingsUiState()
    object LoggedOut : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}
