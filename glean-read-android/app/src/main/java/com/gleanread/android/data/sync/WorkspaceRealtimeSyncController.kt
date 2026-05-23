package com.gleanread.android.data.sync

import android.util.Log
import com.gleanread.android.data.auth.AuthSession
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

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
                    val session = currentSessionForRealtime() ?: return@collectLatest
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

    private suspend fun currentSessionForRealtime(): AuthSession? {
        return try {
            sessionRefresher?.currentSessionOrRefresh()
                ?: sessionStore.session.value
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Log.w(TAG, "Realtime session refresh failed.", error)
            null
        }
    }

    private suspend fun subscribeForCurrentUser(accessToken: String, userId: String) {
        supabaseClient?.realtime?.setAuth(accessToken)
        val channel = supabaseClient?.channel("workspace-sync-$userId") ?: return
        try {
            val changeFlows: List<Flow<WorkspaceRealtimeChange>> = WorkspaceRealtimeTables.map { tableName ->
                channel.tableChangeFlow(tableName, userId)
            }
            coroutineScope {
                val collectJob = launch {
                    merge(*changeFlows.toTypedArray()).collect { change ->
                        val record = change.action.recordOrNull()
                        if (record == null) {
                            Log.d(TAG, "Realtime change ignored because payload has no record.")
                            return@collect
                        }
                        try {
                            syncRepository.applyRealtimeChange(
                                userId = userId,
                                tableName = change.tableName,
                                record = record,
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            Log.w(TAG, "Realtime change apply failed for ${change.tableName}.", error)
                        }
                    }
                }
                try {
                    supabaseClient.realtime.connect()
                    channel.subscribe(blockUntilSubscribed = true)
                    collectJob.join()
                } finally {
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
    ): Flow<WorkspaceRealtimeChange> = postgresChangeFlow<PostgresAction>(schema = "public") {
        table = tableName
        filter("user_id", FilterOperator.EQ, userId)
    }.map { action -> WorkspaceRealtimeChange(tableName, action) }

    private fun PostgresAction.recordOrNull(): JsonObject? {
        return when (this) {
            is PostgresAction.Insert -> record
            is PostgresAction.Update -> record
            // The app syncs deletions as soft-delete updates, while physical DELETE payloads may only contain keys.
            is PostgresAction.Delete -> null
            is PostgresAction.Select -> record
        }
    }

    private data class WorkspaceRealtimeChange(
        val tableName: String,
        val action: PostgresAction,
    )

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
