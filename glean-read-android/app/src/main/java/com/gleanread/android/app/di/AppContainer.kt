package com.gleanread.android.app.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.gleanread.android.data.auth.SupabaseAuthRepository
import com.gleanread.android.data.auth.SupabaseSessionRefresher
import com.gleanread.android.data.auth.SupabaseSessionStore
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.local.WorkspaceDatabaseManager
import com.gleanread.android.data.appearance.AppearancePreferencesRepository
import com.gleanread.android.data.avatar.AvatarRepository
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.remote.SupabaseConfig
import com.gleanread.android.data.remote.SupabaseHttpClientFactory
import com.gleanread.android.data.remote.SupabaseRealtimeClientFactory
import com.gleanread.android.data.repository.AiSummaryRepository
import com.gleanread.android.data.repository.CurrentUserIdProvider
import com.gleanread.android.data.repository.ExcerptRepository
import com.gleanread.android.data.repository.KnowledgeTreeRepository
import com.gleanread.android.data.repository.SeedDataInitializer
import com.gleanread.android.data.repository.TagRepository
import com.gleanread.android.data.repository.WorkspaceSnapshotProvider
import com.gleanread.android.data.sync.DeviceIdentityStore
import com.gleanread.android.data.sync.SupabaseWorkspaceRemoteDataSource
import com.gleanread.android.data.sync.WorkspaceRealtimeSyncController
import com.gleanread.android.data.sync.WorkspaceSyncRepository
import com.gleanread.android.data.sync.WorkspaceSyncStateStore
import com.gleanread.android.feature.capture.fast_capture.FastCaptureViewModel
import com.gleanread.android.feature.excerpts.feed.FeedViewModel
import com.gleanread.android.feature.excerpts.summary.AiSummaryViewModel
import com.gleanread.android.app.navigation.MainAppViewModel
import com.gleanread.android.feature.settings.SettingsViewModel
import com.gleanread.android.feature.settings.auth.AuthViewModel
import com.gleanread.android.feature.tags.TagsViewModel
import com.gleanread.android.core.data.AppSnapshotStore
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val databaseManager: WorkspaceDatabaseManager by lazy {
        WorkspaceDatabaseManager(appContext)
    }

    val workspaceDatabase: WorkspaceDatabase
        get() = databaseManager.activeWorkspace.value.database

    /**
     * 根据已保存的 session 恢复数据库连接。
     * 应在 Application.onCreate() 中调用。
     */
    fun restoreDatabaseFromSession() {
        val savedUserId = supabaseSessionStore.session.value?.userId
        if (savedUserId != null) {
            databaseManager.switchToUser(savedUserId)
        }
    }

    private val deviceIdentityStore: DeviceIdentityStore by lazy {
        DeviceIdentityStore(appContext)
    }

    private val supabaseConfig: SupabaseConfig by lazy {
        SupabaseConfig.fromBuildConfig()
    }

    private val supabaseHttpClient: HttpClient by lazy {
        SupabaseHttpClientFactory.create()
    }

    private val supabaseRealtimeClient: SupabaseClient? by lazy {
        SupabaseRealtimeClientFactory.create(supabaseConfig) {
            supabaseSessionRefresher.currentSessionOrRefresh()?.accessToken.orEmpty()
        }
    }

    private val supabaseSessionStore: SupabaseSessionStore by lazy {
        SupabaseSessionStore(appContext)
    }

    private val workspaceSyncStateStore: WorkspaceSyncStateStore by lazy {
        WorkspaceSyncStateStore(appContext)
    }

    private val supabaseSessionRefresher: SupabaseSessionRefresher by lazy {
        SupabaseSessionRefresher(
            config = supabaseConfig,
            httpClient = supabaseHttpClient,
            sessionStore = supabaseSessionStore,
        )
    }

    private val currentUserIdProvider: CurrentUserIdProvider by lazy {
        CurrentUserIdProvider {
            supabaseSessionStore.session.value?.userId ?: LOCAL_USER_ID
        }
    }

    val supabaseAuthRepository: SupabaseAuthRepository by lazy {
        SupabaseAuthRepository(
            config = supabaseConfig,
            httpClient = supabaseHttpClient,
            sessionStore = supabaseSessionStore,
            databaseManager = databaseManager,
            deviceIdProvider = deviceIdentityStore,
        )
    }

    val workspaceSyncRepository: WorkspaceSyncRepository by lazy {
        WorkspaceSyncRepository(
            databaseManager = databaseManager,
            remoteDataSource = SupabaseWorkspaceRemoteDataSource(
                config = supabaseConfig,
                httpClient = supabaseHttpClient,
            ),
            sessionStore = supabaseSessionStore,
            stateStore = workspaceSyncStateStore,
            sessionRefresher = supabaseSessionRefresher,
        )
    }

    val workspaceRealtimeSyncController: WorkspaceRealtimeSyncController by lazy {
        WorkspaceRealtimeSyncController(
            supabaseClient = supabaseRealtimeClient,
            sessionStore = supabaseSessionStore,
            syncRepository = workspaceSyncRepository,
            sessionRefresher = supabaseSessionRefresher,
        )
    }

    val excerptRepository: ExcerptRepository by lazy {
        ExcerptRepository(databaseManager, deviceIdentityStore, currentUserIdProvider)
    }

    val tagRepository: TagRepository by lazy {
        TagRepository(databaseManager, deviceIdentityStore, currentUserIdProvider)
    }

    val knowledgeTreeRepository: KnowledgeTreeRepository by lazy {
        KnowledgeTreeRepository(databaseManager, deviceIdentityStore, currentUserIdProvider)
    }

    val snapshotProvider: WorkspaceSnapshotProvider by lazy {
        WorkspaceSnapshotProvider(databaseManager)
    }

    val seedDataInitializer: SeedDataInitializer by lazy {
        SeedDataInitializer(databaseManager, deviceIdentityStore, currentUserIdProvider)
    }

    val aiSummaryRepository: AiSummaryRepository by lazy {
        AiSummaryRepository(databaseManager, deviceIdentityStore, currentUserIdProvider)
    }

    val appSnapshotStore: AppSnapshotStore by lazy {
        AppSnapshotStore(snapshotProvider)
    }

    val appearancePreferencesRepository: AppearancePreferencesRepository by lazy {
        AppearancePreferencesRepository(appContext)
    }

    val avatarRepository: AvatarRepository by lazy {
        AvatarRepository(
            config = supabaseConfig,
            httpClient = supabaseHttpClient,
            sessionStore = supabaseSessionStore
        )
    }

    val mainAppViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory {
            MainAppViewModel(
                authRepository = supabaseAuthRepository,
                excerptRepository = excerptRepository,
                tagRepository = tagRepository,
                knowledgeTreeRepository = knowledgeTreeRepository,
                seedDataInitializer = seedDataInitializer,
                syncRepository = workspaceSyncRepository,
                snapshotStore = appSnapshotStore,
                backgroundSyncScope = applicationScope,
            )
        }
    }

    val feedViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { FeedViewModel() }
    }

    val aiSummaryViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { AiSummaryViewModel(aiSummaryRepository) }
    }

    val tagsViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { TagsViewModel() }
    }

    val settingsViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory {
            SettingsViewModel(
                authRepository = supabaseAuthRepository,
                syncRepository = workspaceSyncRepository,
                appearancePreferencesRepository = appearancePreferencesRepository,
                avatarRepository = avatarRepository,
                backgroundSyncScope = applicationScope,
            )
        }
    }

    val authViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory {
            AuthViewModel(
                authRepository = supabaseAuthRepository,
                syncRepository = workspaceSyncRepository,
                databaseManager = databaseManager,
                backgroundSyncScope = applicationScope,
            )
        }
    }

    val fastCaptureViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory {
            FastCaptureViewModel(
                tagRepository = tagRepository,
                excerptRepository = excerptRepository,
            )
        }
    }
}
