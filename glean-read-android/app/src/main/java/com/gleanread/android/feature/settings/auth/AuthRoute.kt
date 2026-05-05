package com.gleanread.android.feature.settings.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.gleanread.android.app.appContainer

@Composable
fun AuthRoute(
    onNavigateBack: () -> Unit
) {
    val appContainer = LocalContext.current.appContainer
    val viewModel: AuthViewModel = viewModel(
        factory = appContainer.authViewModelFactory
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccessAndFinished) {
        if (uiState.isSuccessAndFinished) {
            onNavigateBack()
        }
    }

    AuthScreen(
        uiState = uiState,
        onEmailChange = viewModel::updateEmail,
        onPasswordChange = viewModel::updatePassword,
        onConfirmPasswordChange = viewModel::updateConfirmPassword,
        onOtpChange = viewModel::updateOtp,
        onToggleAuthMode = viewModel::toggleAuthMode,
        onSubmit = viewModel::submit,
        onMagicLinkClick = viewModel::startMagicLinkFlow,
        onOtpLoginClick = viewModel::startOtpFlow,
        onSendMagicLink = viewModel::sendMagicLink,
        onSendOtpCode = viewModel::sendOtpCode,
        onVerifyOtp = viewModel::verifyOtp,
        onBackToEmail = viewModel::backToEmail,
        onChooseOwnership = viewModel::chooseOwnership,
        onDismissOwnershipDialog = viewModel::dismissOwnershipDialog,
        onClearMessage = viewModel::clearMessage,
        onBackClick = onNavigateBack
    )
}
