package com.gleanread.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.data.appearance.AppearancePreferencesRepository
import com.gleanread.android.data.appearance.ThemeColor
import com.gleanread.android.data.appearance.ThemeMode
import com.gleanread.android.data.avatar.AvatarRepository
import com.gleanread.android.data.avatar.CompressedImage
import com.gleanread.android.data.auth.LocalDataOwnershipChoice
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive

class SettingsViewModel(
    private val authRepository: SupabaseAuthRepository,
    private val syncRepository: WorkspaceSyncRepository,
    private val appearancePreferencesRepository: AppearancePreferencesRepository,
    private val avatarRepository: AvatarRepository,
) : ViewModel() {
    private val formState = MutableStateFlow(SettingsFormState())

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.session,
        syncRepository.syncState,
        syncRepository.isCloudSyncEnabled,
        appearancePreferencesRepository.themeModeFlow,
        appearancePreferencesRepository.themeColorFlow,
        appearancePreferencesRepository.avatarUrlFlow,
        formState,
    ) { args ->
        val session = args[0] as com.gleanread.android.data.auth.AuthSession?
        val syncState = args[1] as com.gleanread.android.data.sync.WorkspaceSyncUiState
        val isCloudSyncEnabled = args[2] as Boolean
        val themeMode = args[3] as ThemeMode
        val themeColor = args[4] as ThemeColor
        val avatarUrl = args[5] as String?
        val form = args[6] as SettingsFormState

        SettingsUiState(
            isLoggedIn = session != null,
            sessionEmail = session?.email,
            avatarUrl = avatarUrl,
            isAvatarUploading = form.isAvatarUploading,
            themeMode = themeMode,
            themeColor = themeColor,
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

    init {
        refreshUserProfile()
    }

    private fun refreshUserProfile() {
        viewModelScope.launch {
            authRepository.fetchCurrentUserProfile().onSuccess { userResponse ->
                val avatarUrl = userResponse.userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull
                if (avatarUrl != null) {
                    appearancePreferencesRepository.setAvatarUrl(avatarUrl)
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appearancePreferencesRepository.setThemeMode(mode)
        }
    }

    fun setThemeColor(color: ThemeColor) {
        viewModelScope.launch {
            appearancePreferencesRepository.setThemeColor(color)
        }
    }

    fun uploadAvatar(image: CompressedImage) {
        val session = authRepository.session.value ?: return
        viewModelScope.launch {
            formState.update { it.copy(isAvatarUploading = true) }
            val result = avatarRepository.uploadAvatar(
                userId = session.userId,
                imageBytes = image.bytes,
                contentType = image.mimeType,
                extension = image.extension
            )
            if (result.isSuccess) {
                val publicUrl = result.getOrNull()
                // 同时更新 Supabase Auth 中的用户元数据，以便多设备同步
                authRepository.updateUserMetadata(
                    mapOf("avatar_url" to JsonPrimitive(publicUrl))
                )
                appearancePreferencesRepository.setAvatarUrl(publicUrl)
                formState.update { it.copy(isAvatarUploading = false, message = "头像更新成功") }
            } else {
                formState.update { it.copy(isAvatarUploading = false, message = "头像上传失败: ${result.exceptionOrNull()?.message}") }
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
            refreshUserProfile()
            val message = when (val result = syncRepository.syncNow(repairMissingRemote = true)) {
                is WorkspaceSyncResult.Success -> "同步完成"
                is WorkspaceSyncResult.Failure -> result.message
                is WorkspaceSyncResult.Skipped -> result.message
            }
            formState.update { it.copy(message = message) }
        }
    }

    fun clearMessage() {
        formState.update { it.copy(message = null) }
    }
}

private data class SettingsFormState(
    val isSubmitting: Boolean = false,
    val isAvatarUploading: Boolean = false,
    val message: String? = null,
    val showOwnershipDialog: Boolean = false,
)
