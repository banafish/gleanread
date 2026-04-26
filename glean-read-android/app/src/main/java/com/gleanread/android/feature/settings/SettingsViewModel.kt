package com.gleanread.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.data.auth.AuthResult
import com.gleanread.android.data.auth.LocalDataOwnershipChoice
import com.gleanread.android.data.auth.MagicLinkRequestResult
import com.gleanread.android.data.auth.SupabaseAuthRepository
import com.gleanread.android.data.sync.WorkspaceSyncRepository
import com.gleanread.android.data.sync.WorkspaceSyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: SupabaseAuthRepository,
    private val syncRepository: WorkspaceSyncRepository,
) : ViewModel() {
    private val formState = MutableStateFlow(SettingsFormState())

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.session,
        syncRepository.syncState,
        syncRepository.isCloudSyncEnabled,
        formState,
    ) { session, syncState, isCloudSyncEnabled, form ->
        SettingsUiState(
            email = form.email,
            password = form.password,
            isLoggedIn = session != null,
            sessionEmail = session?.email,
            isCloudSyncEnabled = isCloudSyncEnabled,
            isSubmitting = form.isSubmitting,
            isSyncing = syncState.isSyncing,
            lastSyncTime = syncState.lastSyncTime,
            failedCount = syncState.failedCount,
            conflictCount = syncState.conflictCount,
            message = form.message ?: syncState.errorMessage,
            showOwnershipDialog = form.showOwnershipDialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun updateEmail(value: String) {
        formState.update { it.copy(email = value, message = null) }
    }

    fun updatePassword(value: String) {
        formState.update { it.copy(password = value, message = null) }
    }

    fun signIn() {
        val form = formState.value
        if (form.isSubmitting) return

        viewModelScope.launch {
            formState.update { it.copy(isSubmitting = true, message = null) }
            when (val result = authRepository.signInWithEmailPassword(form.email, form.password)) {
                is AuthResult.Success -> handleSignInSuccess(result)

                is AuthResult.Failure -> {
                    formState.update {
                        it.copy(isSubmitting = false, message = result.message)
                    }
                }
            }
        }
    }

    fun sendMagicLink() {
        val form = formState.value
        if (form.isSubmitting) return

        viewModelScope.launch {
            formState.update { it.copy(isSubmitting = true, message = null) }
            when (val result = authRepository.sendMagicLink(form.email)) {
                MagicLinkRequestResult.Sent -> {
                    formState.update {
                        it.copy(
                            isSubmitting = false,
                            password = "",
                            message = "Magic Link 已发送，请打开邮箱中的登录链接",
                        )
                    }
                }

                is MagicLinkRequestResult.Failure -> {
                    formState.update {
                        it.copy(isSubmitting = false, message = result.message)
                    }
                }
            }
        }
    }

    fun chooseOwnership(choice: LocalDataOwnershipChoice) {
        viewModelScope.launch {
            formState.update { it.copy(isSubmitting = true, message = null) }
            when (choice) {
                LocalDataOwnershipChoice.MERGE_TO_ACCOUNT -> {
                    authRepository.mergeLocalDataIntoCurrentAccount()
                    syncRepository.setCloudSyncEnabled(true)
                    syncRepository.syncNow()
                    formState.update {
                        it.copy(
                            isSubmitting = false,
                            showOwnershipDialog = false,
                            message = "本地数据已合并到当前账号",
                        )
                    }
                }

                LocalDataOwnershipChoice.KEEP_LOCAL -> {
                    syncRepository.setCloudSyncEnabled(false)
                    formState.update {
                        it.copy(
                            isSubmitting = false,
                            showOwnershipDialog = false,
                            message = "已保留本地模式，云同步未开启",
                        )
                    }
                }

                LocalDataOwnershipChoice.USE_CLOUD -> {
                    authRepository.clearLocalWorkspaceData()
                    syncRepository.setCloudSyncEnabled(true)
                    syncRepository.syncNow()
                    formState.update {
                        it.copy(
                            isSubmitting = false,
                            showOwnershipDialog = false,
                            message = "已切换为当前账号的云端数据",
                        )
                    }
                }
            }
        }
    }

    fun dismissOwnershipDialog() {
        formState.update { it.copy(showOwnershipDialog = false) }
    }

    fun signOut() {
        viewModelScope.launch {
            syncRepository.setCloudSyncEnabled(false)
            authRepository.signOut()
            formState.update { it.copy(message = "已退出登录") }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val message = when (val result = syncRepository.syncNow(repairMissingRemote = true)) {
                is WorkspaceSyncResult.Success -> "同步完成"
                is WorkspaceSyncResult.Failure -> result.message
                is WorkspaceSyncResult.Skipped -> result.message
            }
            formState.update { it.copy(message = message) }
        }
    }

    private suspend fun handleSignInSuccess(result: AuthResult.Success) {
        val hasLocalData = authRepository.hasLocalUserData()
        if (hasLocalData) {
            formState.update {
                it.copy(
                    isSubmitting = false,
                    password = "",
                    showOwnershipDialog = true,
                    message = "请选择本地数据归属",
                )
            }
        } else {
            syncRepository.setCloudSyncEnabled(true)
            syncRepository.syncNow()
            formState.update {
                it.copy(
                    isSubmitting = false,
                    password = "",
                    message = "已登录 ${result.session.email.orEmpty()}",
                )
            }
        }
    }
}

private data class SettingsFormState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val showOwnershipDialog: Boolean = false,
)
