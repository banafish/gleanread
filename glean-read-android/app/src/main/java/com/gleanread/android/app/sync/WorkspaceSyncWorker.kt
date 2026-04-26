package com.gleanread.android.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gleanread.android.app.appContainer
import com.gleanread.android.data.sync.WorkspaceSyncResult
import java.util.concurrent.TimeUnit

class WorkspaceSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return when (applicationContext.appContainer.workspaceSyncRepository.syncNow()) {
            is WorkspaceSyncResult.Success,
            is WorkspaceSyncResult.Skipped,
            -> Result.success()
            is WorkspaceSyncResult.Failure -> Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "workspace-cloud-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WorkspaceSyncWorker>(
                6,
                TimeUnit.HOURS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
