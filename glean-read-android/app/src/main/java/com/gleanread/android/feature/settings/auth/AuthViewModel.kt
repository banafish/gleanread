package com.gleanread.android.feature.settings.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.data.auth.AuthResult
import com.gleanread.android.data.auth.LocalDataOwnershipChoice
import com.gleanread.android.data.auth.MagicLinkRequestResult
import com.gleanread.android.data.auth.SupabaseAuthRepository
import com.gleanread.android.data.sync.WorkspaceSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: SupabaseAuthRepository,
    private val syncRepository: WorkspaceSyncRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, message = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, message = null) }
    }
    
    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null, message = null) }
    }

    fun toggleAuthMode() {
        _uiState.update { 
            it.copy(
                isSignUpMode = !it.isSignUpMode, 
                errorMessage = null, 
                message = null 
            ) 
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        if (state.isSignUpMode) {
            if (state.password != state.confirmPassword) {
                _uiState.update { it.copy(errorMessage = "两次密码不一致") }
                return
            }
            signUp()
        } else {
            signIn()
        }
    }

    private fun signIn() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            when (val result = authRepository.signInWithEmailPassword(state.email, state.password)) {
                is AuthResult.Success -> handleAuthSuccess(result)
                is AuthResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                }
            }
        }
    }

    private fun signUp() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            when (val result = authRepository.signUpWithEmailPassword(state.email, state.password)) {
                is AuthResult.Success -> handleAuthSuccess(result)
                is AuthResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                }
            }
        }
    }

    private suspend fun handleAuthSuccess(result: AuthResult.Success) {
        val hasLocalData = authRepository.hasLocalUserData()
        if (hasLocalData) {
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    password = "",
                    confirmPassword = "",
                    showOwnershipDialog = true,
                )
            }
        } else {
            syncRepository.setCloudSyncEnabled(true)
            syncRepository.syncNow()
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    isSuccessAndFinished = true
                )
            }
        }
    }

    fun sendMagicLink() {
        val state = _uiState.value
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            when (val result = authRepository.sendMagicLink(state.email)) {
                MagicLinkRequestResult.Sent -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            password = "",
                            confirmPassword = "",
                            message = "Magic Link 已发送，请打开邮箱中的登录链接",
                        )
                    }
                }
                is MagicLinkRequestResult.Failure -> {
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun chooseOwnership(choice: LocalDataOwnershipChoice) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (choice) {
                LocalDataOwnershipChoice.MERGE_TO_ACCOUNT -> {
                    authRepository.mergeLocalDataIntoCurrentAccount()
                    syncRepository.setCloudSyncEnabled(true)
                    syncRepository.syncNow()
                }
                LocalDataOwnershipChoice.KEEP_LOCAL -> {
                    syncRepository.setCloudSyncEnabled(false)
                }
                LocalDataOwnershipChoice.USE_CLOUD -> {
                    authRepository.clearLocalWorkspaceData()
                    syncRepository.setCloudSyncEnabled(true)
                    syncRepository.syncNow()
                }
            }
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    showOwnershipDialog = false,
                    isSuccessAndFinished = true
                )
            }
        }
    }

    fun dismissOwnershipDialog() {
        _uiState.update { it.copy(showOwnershipDialog = false, isSuccessAndFinished = true) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }
}
