package com.gleanread.android.core.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.repository.WorkspaceRepository
import com.gleanread.android.feature.fast_capture.FastCaptureViewModel
import com.gleanread.android.feature.workspace.WorkspaceViewModel
import com.gleanread.android.feature.workspace.capture.QuickCaptureViewModel
import com.gleanread.android.feature.workspace.data.WorkspaceSnapshotStore
import com.gleanread.android.feature.workspace.feed.FeedViewModel
import com.gleanread.android.feature.workspace.summary.AiSummaryViewModel

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val workspaceDatabase: WorkspaceDatabase by lazy { WorkspaceDatabase.get(appContext) }

    val workspaceRepository: WorkspaceRepository by lazy {
        WorkspaceRepository(workspaceDatabase)
    }

    val workspaceSnapshotStore: WorkspaceSnapshotStore by lazy {
        WorkspaceSnapshotStore(workspaceRepository)
    }

    val workspaceViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory {
            WorkspaceViewModel(
                repository = workspaceRepository,
                snapshotStore = workspaceSnapshotStore,
            )
        }
    }

    val feedViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { FeedViewModel() }
    }

    val quickCaptureViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { QuickCaptureViewModel(workspaceRepository) }
    }

    val aiSummaryViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { AiSummaryViewModel(workspaceRepository) }
    }

    val fastCaptureViewModelFactory: ViewModelProvider.Factory by lazy {
        AppViewModelFactory { FastCaptureViewModel(workspaceRepository) }
    }
}
