package com.gleanread.android.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WorkspaceLocalChangeSyncScheduler internal constructor(
    private val scope: CoroutineScope,
    private val syncNow: suspend () -> WorkspaceSyncResult,
    private val debounceMillis: Long = DIRTY_SYNC_DELAY_MS,
    private val followUpMillis: Long = FOLLOW_UP_SYNC_DELAY_MS,
    private val initialRetryMillis: Long = RETRY_SYNC_DELAY_MS,
    private val maxRetryMillis: Long = MAX_RETRY_SYNC_DELAY_MS,
) : LocalChangeSyncTrigger {
    constructor(
        syncRepository: WorkspaceSyncRepository,
        scope: CoroutineScope,
    ) : this(
        scope = scope,
        syncNow = { syncRepository.syncNow(pullRemote = false) },
    )

    private val lock = Any()
    private var scheduledJob: Job? = null
    private var isRunning = false
    private var pendingAfterRun = false
    private var retryDelayMillis = initialRetryMillis

    override fun onLocalDataChanged() {
        scheduleSync(debounceMillis)
    }

    override fun flushPendingChanges() {
        val hasPendingWork = synchronized(lock) {
            scheduledJob != null || pendingAfterRun
        }
        if (!hasPendingWork) {
            return
        }
        scheduleSync(delayMillis = 0L)
    }

    private fun scheduleSync(delayMillis: Long) {
        synchronized(lock) {
            if (isRunning) {
                pendingAfterRun = true
                return
            }
            scheduledJob?.cancel()
            scheduledJob = scope.launch {
                if (delayMillis > 0L) {
                    delay(delayMillis)
                }
                runSyncJob()
            }
        }
    }

    private suspend fun runSyncJob() {
        synchronized(lock) {
            scheduledJob = null
            isRunning = true
        }
        var nextDelayMillis: Long? = null
        try {
            nextDelayMillis = when (val result = syncNow()) {
                is WorkspaceSyncResult.Success -> {
                    retryDelayMillis = initialRetryMillis
                    null
                }

                is WorkspaceSyncResult.Failure -> consumeRetryDelayMillis()

                is WorkspaceSyncResult.Skipped -> {
                    if (result.reason == WorkspaceSyncSkipReason.ALREADY_RUNNING) {
                        followUpMillis
                    } else {
                        retryDelayMillis = initialRetryMillis
                        null
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            nextDelayMillis = consumeRetryDelayMillis()
        } finally {
            synchronized(lock) {
                isRunning = false
                if (pendingAfterRun && nextDelayMillis == null) {
                    nextDelayMillis = followUpMillis
                }
                pendingAfterRun = false
            }
            nextDelayMillis?.let(::scheduleSync)
        }
    }

    private fun consumeRetryDelayMillis(): Long {
        val delayMillis = retryDelayMillis
        retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(maxRetryMillis)
        return delayMillis
    }

    private companion object {
        const val DIRTY_SYNC_DELAY_MS = 2_000L
        const val FOLLOW_UP_SYNC_DELAY_MS = 1_000L
        const val RETRY_SYNC_DELAY_MS = 5_000L
        const val MAX_RETRY_SYNC_DELAY_MS = 5 * 60_000L
    }
}
