package com.gleanread.android.feature.settings.auth

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSignUpMode: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val showOwnershipDialog: Boolean = false,
    val showOtpScreen: Boolean = false,
    val showMagicLinkScreen: Boolean = false,
    val currentFlow: AuthFlow = AuthFlow.PASSWORD,
    val otp: String = "",
    val isSuccessAndFinished: Boolean = false
)

enum class AuthFlow {
    PASSWORD, OTP, MAGIC_LINK
}
