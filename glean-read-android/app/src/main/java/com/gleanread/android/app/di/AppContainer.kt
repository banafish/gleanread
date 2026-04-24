package com.gleanread.android.app.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.core.data.AppSnapshotStore
import com.gleanread.android.data.repository.AiSummaryRepository
import com.gleanread.android.data.repository.ExcerptRepository
import com.gleanread.android.data.repository.KnowledgeTreeRepository
import com.gleanread.android.data.repository.SeedDataInitializer
import com.gleanread.android.data.repository.TagRepository
import com.gleanread.android.data.repository.WorkspaceSnapshotProvider
import com.gleanread.android.feature.capture.fast_capture.FastCaptureViewModel
import com.gleanread.android.feature.excerpts.feed.FeedViewModel
import com.gleanread.android.feature.excerpts.summary.AiSummaryViewModel
import com.gleanread.android.app.navigation.MainAppViewModel
import com.gleanread.android.feature.tags.TagsViewModel

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    private val workspaceDatabase: WorkspaceDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            WorkspaceDatabase::class.java,
            "glean_workspace.db",
        ).build()
    }

    val excerptRepository: ExcerptRepository by lazy {
        ExcerptRepository(workspaceDatabase)
    }

    val tagRepository: TagRepository by lazy {
        TagRepository(workspaceDatabase)
    }

    val knowledgeTreeRepository: KnowledgeTreeRepository by lazy {
        KnowledgeTreeRepository(workspaceDatabase)
    }

    val snapshotProvider: WorkspaceSnapshotProvider by lazy {
        WorkspaceSnapshotProvider(workspaceDatabase)
    }

    val seedDataInitializer: SeedDataInitializer by lazy {
        SeedDataInitializer(workspaceDatabase)
    }

    val aiSummaryRepository: AiSummaryRepository by lazy {
        AiSummaryRepository(workspaceDatabase)
    }

    val appSnapshotStore: AppSnapshotStore by lazy {
        AppSnapshotStore(snapshotProvider)
    }

    val mainAppViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory {
            MainAppViewModel(
                excerptRepository = excerptRepository,
                tagRepository = tagRepository,
                knowledgeTreeRepository = knowledgeTreeRepository,
                seedDataInitializer = seedDataInitializer,
                snapshotStore = appSnapshotStore,
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

    val fastCaptureViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory {
            FastCaptureViewModel(
                tagRepository = tagRepository,
                excerptRepository = excerptRepository,
            )
        }
    }
}
