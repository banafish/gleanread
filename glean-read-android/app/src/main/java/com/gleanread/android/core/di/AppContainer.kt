package com.gleanread.android.core.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.core.data.AppSnapshotStore
import com.gleanread.android.data.repository.AiSummaryRepository
import com.gleanread.android.data.repository.ExcerptCaptureRepository
import com.gleanread.android.data.repository.KnowledgeTreeRepository
import com.gleanread.android.data.repository.SnapshotRepository
import com.gleanread.android.feature.fast_capture.FastCaptureViewModel
import com.gleanread.android.feature.capture.QuickCaptureViewModel
import com.gleanread.android.feature.excerpts.feed.FeedViewModel
import com.gleanread.android.feature.excerpts.summary.AiSummaryViewModel
import com.gleanread.android.feature.main.MainAppViewModel

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val workspaceDatabase: WorkspaceDatabase by lazy { WorkspaceDatabase.get(appContext) }

    val snapshotRepository: SnapshotRepository by lazy {
        SnapshotRepository(workspaceDatabase)
    }

    val knowledgeTreeRepository: KnowledgeTreeRepository by lazy {
        KnowledgeTreeRepository(workspaceDatabase)
    }

    val excerptCaptureRepository: ExcerptCaptureRepository by lazy {
        ExcerptCaptureRepository(workspaceDatabase)
    }

    val aiSummaryRepository: AiSummaryRepository by lazy {
        AiSummaryRepository(workspaceDatabase)
    }

    val appSnapshotStore: AppSnapshotStore by lazy {
        AppSnapshotStore(snapshotRepository)
    }

    val mainAppViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory {
            MainAppViewModel(
                snapshotRepository = snapshotRepository,
                knowledgeTreeRepository = knowledgeTreeRepository,
                snapshotStore = appSnapshotStore,
            )
        }
    }

    val feedViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { FeedViewModel() }
    }

    val quickCaptureViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { QuickCaptureViewModel(excerptCaptureRepository) }
    }

    val aiSummaryViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { AiSummaryViewModel(aiSummaryRepository) }
    }

    val fastCaptureViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { FastCaptureViewModel(excerptCaptureRepository) }
    }
}
