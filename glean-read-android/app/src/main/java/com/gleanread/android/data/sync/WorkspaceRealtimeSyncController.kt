package com.gleanread.android.data.sync

import android.util.Log
import com.gleanread.android.data.auth.SupabaseSessionStore
import com.gleanread.android.data.auth.SupabaseSessionRefresher
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class WorkspaceRealtimeSyncController(
    private val supabaseClient: SupabaseClient?,
    private val sessionStore: SupabaseSessionStore,
    private val syncRepository: WorkspaceSyncRepository,
    private val sessionRefresher: SupabaseSessionRefresher? = null,
) {
    private var realtimeJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (supabaseClient == null) {
            Log.w(TAG, "Realtime disabled because Supabase client is not configured.")
            return
        }
        if (realtimeJob?.isActive == true) return
        realtimeJob = scope.launch {
            combine(
                sessionStore.session,
                syncRepository.isCloudSyncEnabled,
            ) { session, isCloudSyncEnabled ->
                RealtimeSession(
                    accessToken = session?.accessToken,
                    userId = session?.userId,
                    isCloudSyncEnabled = isCloudSyncEnabled,
                )
            }
                .distinctUntilChanged()
                .collectLatest { realtimeSession ->
                    if (!realtimeSession.canSubscribe) {
                        Log.d(
                            TAG,
                            "Realtime subscription skipped. isCloudSyncEnabled=" +
                                "${realtimeSession.isCloudSyncEnabled}, " +
                                "hasAccessToken=${!realtimeSession.accessToken.isNullOrBlank()}, " +
                                "hasUserId=${!realtimeSession.userId.isNullOrBlank()}",
                        )
                        return@collectLatest
                    }
                    val session = sessionRefresher?.currentSessionOrRefresh()
                        ?: sessionStore.session.value
                        ?: return@collectLatest
                    try {
                        subscribeForCurrentUser(
                            accessToken = session.accessToken,
                            userId = session.userId,
                        )
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        Log.w(TAG, "Realtime subscription failed.", error)
                    }
                }
        }
    }

    fun stop(scope: CoroutineScope) {
        val activeJob = realtimeJob ?: return
        realtimeJob = null
        scope.launch {
            activeJob.cancelAndJoin()
        }
    }

    private suspend fun subscribeForCurrentUser(accessToken: String, userId: String) {
        supabaseClient?.realtime?.setAuth(accessToken)
        val channel = supabaseClient?.channel("workspace-sync-$userId") ?: return
        Log.d(TAG, "Subscribing workspace realtime changes for userId=$userId.")
        try {
            val changeFlows: List<Flow<PostgresAction>> = WorkspaceRealtimeTables.map { tableName ->
                channel.tableChangeFlow(tableName, userId)
            }
            coroutineScope {
                val statusJob = launch {
                    channel.status.collect { status ->
                        Log.d(TAG, "Realtime channel status=$status for userId=$userId.")
                    }
                }
                val collectJob = launch {
                    merge(*changeFlows.toTypedArray()).collectLatest {
                        Log.d(TAG, "Workspace realtime change received for userId=$userId.")
                        syncRepository.syncNow()
                    }
                }
                try {
                    supabaseClient.realtime.connect()
                    channel.subscribe(blockUntilSubscribed = true)
                    Log.d(TAG, "Workspace realtime channel subscribed for userId=$userId.")
                    collectJob.join()
                } finally {
                    statusJob.cancel()
                    collectJob.cancel()
                }
            }
        } finally {
            channel.unsubscribe()
            Log.d(TAG, "Workspace realtime channel unsubscribed for userId=$userId.")
        }
    }

    private fun RealtimeChannel.tableChangeFlow(
        tableName: String,
        userId: String,
    ): Flow<PostgresAction> = postgresChangeFlow<PostgresAction>(schema = "public") {
        table = tableName
        filter("user_id", FilterOperator.EQ, userId)
    }

    private data class RealtimeSession(
        val accessToken: String?,
        val userId: String?,
        val isCloudSyncEnabled: Boolean,
    ) {
        val canSubscribe: Boolean
            get() = !accessToken.isNullOrBlank() && !userId.isNullOrBlank() && isCloudSyncEnabled
    }

    private companion object {
        const val TAG = "WorkspaceRealtimeSync"

        val WorkspaceRealtimeTables = listOf(
            REMOTE_TABLE_KNOWLEDGE_TREE_NODE,
            REMOTE_TABLE_TAGS,
            REMOTE_TABLE_EXCERPTS,
            REMOTE_TABLE_EXCERPT_TAGS,
        )
    }
}
