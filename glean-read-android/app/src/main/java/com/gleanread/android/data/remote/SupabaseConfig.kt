package com.gleanread.android.data.remote

import com.gleanread.android.BuildConfig

data class SupabaseConfig(
    val url: String,
    val anonKey: String,
    val magicLinkRedirectUrl: String = "",
) {
    val isConfigured: Boolean
        get() = url.isNotBlank() && anonKey.isNotBlank()

    val normalizedUrl: String
        get() = url.trim().trimEnd('/')

    companion object {
        fun fromBuildConfig(): SupabaseConfig {
            return SupabaseConfig(
                url = BuildConfig.SUPABASE_URL,
                anonKey = BuildConfig.SUPABASE_ANON_KEY,
                magicLinkRedirectUrl = BuildConfig.SUPABASE_MAGIC_LINK_REDIRECT_URL,
            )
        }
    }
}
