package com.gleanread.android.data.auth

data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val userId: String,
    val email: String?,
    val expiresAtMillis: Long?,
)

sealed interface AuthResult {
    data class Success(val session: AuthSession) : AuthResult
    data class Failure(val message: String) : AuthResult
}

sealed interface MagicLinkRequestResult {
    data object Sent : MagicLinkRequestResult
    data class Failure(val message: String) : MagicLinkRequestResult
}

enum class LocalDataOwnershipChoice {
    MERGE_TO_ACCOUNT,
    KEEP_LOCAL,
    USE_CLOUD,
}

sealed interface LocalDataOwnershipResult {
    data object Applied : LocalDataOwnershipResult
    data class Failure(val message: String) : LocalDataOwnershipResult
}
