package com.gleanread.android.data.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceLocalChangeSyncSchedulerTest {
    @Test
    fun `scheduler debounces local changes into one push-only sync`() = runTest {
        var syncCount = 0
        val scheduler = scheduler {
            syncCount += 1
            WorkspaceSyncResult.Success(completedAt = syncCount.toLong())
        }

        scheduler.onLocalDataChanged()
        scheduler.onLocalDataChanged()
        advanceTimeBy(TEST_DEBOUNCE_MILLIS - 1)
        runCurrent()

        assertEquals(0, syncCount)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(1, syncCount)
    }

    @Test
    fun `scheduler retries failed push with backoff`() = runTest {
        var syncCount = 0
        val scheduler = scheduler {
            syncCount += 1
            if (syncCount == 1) {
                WorkspaceSyncResult.Failure("network")
            } else {
                WorkspaceSyncResult.Success(completedAt = syncCount.toLong())
            }
        }

        scheduler.onLocalDataChanged()
        advanceTimeBy(TEST_DEBOUNCE_MILLIS)
        runCurrent()

        assertEquals(1, syncCount)

        advanceTimeBy(TEST_RETRY_MILLIS)
        runCurrent()

        assertEquals(2, syncCount)
    }

    @Test
    fun `scheduler follows up when sync is already running`() = runTest {
        var syncCount = 0
        val scheduler = scheduler {
            syncCount += 1
            if (syncCount == 1) {
                WorkspaceSyncResult.Skipped(
                    message = "running",
                    reason = WorkspaceSyncSkipReason.ALREADY_RUNNING,
                )
            } else {
                WorkspaceSyncResult.Success(completedAt = syncCount.toLong())
            }
        }

        scheduler.onLocalDataChanged()
        advanceTimeBy(TEST_DEBOUNCE_MILLIS)
        runCurrent()

        assertEquals(1, syncCount)

        advanceTimeBy(TEST_FOLLOW_UP_MILLIS)
        runCurrent()

        assertEquals(2, syncCount)
    }

    @Test
    fun `scheduler does not retry non retryable skipped sync`() = runTest {
        var syncCount = 0
        val scheduler = scheduler {
            syncCount += 1
            WorkspaceSyncResult.Skipped(
                message = "disabled",
                reason = WorkspaceSyncSkipReason.CLOUD_SYNC_DISABLED,
            )
        }

        scheduler.onLocalDataChanged()
        advanceTimeBy(TEST_DEBOUNCE_MILLIS)
        runCurrent()
        advanceTimeBy(TEST_RETRY_MILLIS * 2)
        runCurrent()

        assertEquals(1, syncCount)
    }

    @Test
    fun `scheduler flushes pending delayed sync immediately`() = runTest {
        var syncCount = 0
        val scheduler = scheduler {
            syncCount += 1
            WorkspaceSyncResult.Success(completedAt = syncCount.toLong())
        }

        scheduler.onLocalDataChanged()
        advanceTimeBy(TEST_DEBOUNCE_MILLIS / 2)
        scheduler.flushPendingChanges()
        runCurrent()

        assertEquals(1, syncCount)
    }

    @Test
    fun `scheduler ignores flush when there is no pending work`() = runTest {
        var syncCount = 0
        val scheduler = scheduler {
            syncCount += 1
            WorkspaceSyncResult.Success(completedAt = syncCount.toLong())
        }

        scheduler.flushPendingChanges()
        runCurrent()

        assertEquals(0, syncCount)
    }

    private fun TestScope.scheduler(
        syncNow: suspend () -> WorkspaceSyncResult,
    ): WorkspaceLocalChangeSyncScheduler {
        return WorkspaceLocalChangeSyncScheduler(
            scope = this,
            syncNow = syncNow,
            debounceMillis = TEST_DEBOUNCE_MILLIS,
            followUpMillis = TEST_FOLLOW_UP_MILLIS,
            initialRetryMillis = TEST_RETRY_MILLIS,
            maxRetryMillis = TEST_RETRY_MILLIS * 2,
        )
    }

    private companion object {
        const val TEST_DEBOUNCE_MILLIS = 100L
        const val TEST_FOLLOW_UP_MILLIS = 50L
        const val TEST_RETRY_MILLIS = 200L
    }
}
