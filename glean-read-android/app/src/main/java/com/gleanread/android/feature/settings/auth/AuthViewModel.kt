package com.gleanread.android.feature.settings.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.data.auth.AuthResult
import com.gleanread.android.data.auth.LocalDataOwnershipChoice
import com.gleanread.android.data.auth.MagicLinkRequestResult
import com.gleanread.android.data.auth.SupabaseAuthRepository
import com.gleanread.android.data.local.WorkspaceDatabaseManager
import com.gleanread.android.data.sync.WorkspaceSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: SupabaseAuthRepository,
    private val syncRepository: WorkspaceSyncRepository,
    private val databaseManager: WorkspaceDatabaseManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null, message = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null, message = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, errorMessage = null, message = null) }
    }

    fun updateOtp(value: String) {
        _uiState.update { it.copy(otp = value, errorMessage = null) }
        if (value.length == 6) {
            verifyOtp()
        }
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

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return "请输入邮箱地址"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "邮箱格式不正确"
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.isBlank()) return "请输入密码"
        if (password.length < 6) return "密码长度至少为 6 位"
        return null
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)
        var confirmPasswordError: String? = null

        if (state.isSignUpMode) {
            if (state.confirmPassword.isBlank()) {
                confirmPasswordError = "请确认密码"
            } else if (state.password != state.confirmPassword) {
                confirmPasswordError = "两次输入的密码不一致"
            }
        }

        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
            return
        }

        if (state.isSignUpMode) {
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

    fun startMagicLinkFlow() {
        _uiState.update { it.copy(showMagicLinkScreen = true, currentFlow = AuthFlow.MAGIC_LINK, errorMessage = null) }
    }

    fun startOtpFlow() {
        _uiState.update { it.copy(showOtpScreen = false, currentFlow = AuthFlow.OTP, errorMessage = null) }
    }

    fun sendMagicLink() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val emailError = validateEmail(state.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            when (val result = authRepository.signInWithOtp(state.email, authRepository.config.magicLinkRedirectUrl)) {
                MagicLinkRequestResult.Sent -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            message = "Magic Link 已发送到您的邮箱，请点击邮件中的链接完成登录。",
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

    fun sendOtpCode() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val emailError = validateEmail(state.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, message = null) }
            when (val result = authRepository.signInWithOtp(state.email, null)) {
                MagicLinkRequestResult.Sent -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showOtpScreen = true,
                            currentFlow = AuthFlow.OTP
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

    fun verifyOtp() {
        val state = _uiState.value
        if (state.isSubmitting || state.otp.length != 6) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (val result = authRepository.verifyOtp(state.email, state.otp, type = "email")) {
                is AuthResult.Success -> handleAuthSuccess(result)
                is AuthResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun backToEmail() {
        _uiState.update {
            it.copy(
                showOtpScreen = false,
                showMagicLinkScreen = false,
                currentFlow = AuthFlow.PASSWORD,
                otp = "",
                errorMessage = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                message = null
            )
        }
    }

    private suspend fun handleAuthSuccess(result: AuthResult.Success) {
        val hasLocalData = databaseManager.hasGuestData()
        if (hasLocalData) {
            _uiState.update {
                it.copy(
                    isSubmitting = false,
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

    fun chooseOwnership(choice: LocalDataOwnershipChoice) {
        viewModelScope.launch {
            if (choice == LocalDataOwnershipChoice.MERGE_TO_ACCOUNT) {
                _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }
            when (choice) {
                LocalDataOwnershipChoice.MERGE_TO_ACCOUNT -> {
                    authRepository.mergeLocalDataIntoCurrentAccount()
                    syncRepository.setCloudSyncEnabled(true)
                    syncRepository.syncNow()
                }
                LocalDataOwnershipChoice.KEEP_LOCAL -> {
                    syncRepository.setCloudSyncEnabled(true)
                    syncRepository.syncNow()
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
        _uiState.update {
            it.copy(
                message = null,
                errorMessage = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null
            )
        }
    }
}
