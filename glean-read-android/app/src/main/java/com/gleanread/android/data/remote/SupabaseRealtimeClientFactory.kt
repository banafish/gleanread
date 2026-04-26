package com.gleanread.android.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp
import kotlin.time.Duration.Companion.seconds

object SupabaseRealtimeClientFactory {
    fun create(
        config: SupabaseConfig,
        accessTokenProvider: suspend () -> String = { "" },
    ): SupabaseClient? {
        if (!config.isConfigured) return null
        return createSupabaseClient(
            supabaseUrl = config.normalizedUrl,
            supabaseKey = config.anonKey,
        ) {
            httpEngine = OkHttp.create()
            install(Realtime) {
                reconnectDelay = 5.seconds
                accessToken = { accessTokenProvider() }
            }
        }
    }
}
