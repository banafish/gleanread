package com.gleanread.android.feature.settings.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.core.ui.theme.GleanReadTheme
import com.gleanread.android.data.auth.LocalDataOwnershipChoice

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onToggleAuthMode: () -> Unit,
    onSubmit: () -> Unit,
    onMagicLinkClick: () -> Unit,
    onOtpLoginClick: () -> Unit,
    onSendMagicLink: () -> Unit,
    onSendOtpCode: () -> Unit,
    onVerifyOtp: () -> Unit,
    onBackToEmail: () -> Unit,
    onChooseOwnership: (LocalDataOwnershipChoice) -> Unit,
    onDismissOwnershipDialog: () -> Unit,
    onClearMessage: () -> Unit,
    onBackClick: () -> Unit
) {
    if (uiState.message != null) {
        AlertDialog(
            onDismissRequest = onClearMessage,
            title = { Text("提示") },
            text = { Text(uiState.message) },
            confirmButton = {
                TextButton(onClick = onClearMessage) {
                    Text("确定")
                }
            }
        )
    }

    if (uiState.showOwnershipDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isSubmitting) onDismissOwnershipDialog() },
            title = { Text("本地数据处理") },
            text = { Text("您在本地有未同步的数据。登录后您希望如何处理这些数据？") },
            confirmButton = {
                TextButton(
                    onClick = { onChooseOwnership(LocalDataOwnershipChoice.MERGE_TO_ACCOUNT) },
                    enabled = !uiState.isSubmitting
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("合并到当前账号")
                }
            },
            dismissButton = {
                if (!uiState.isSubmitting) {
                    TextButton(
                        onClick = { onChooseOwnership(LocalDataOwnershipChoice.KEEP_LOCAL) }
                    ) {
                        Text("不合并本地数据")
                    }
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (uiState.showOtpScreen || uiState.showMagicLinkScreen) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackToEmail,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "返回",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = when {
                    uiState.showOtpScreen -> "otp"
                    uiState.showMagicLinkScreen -> "magic_link"
                    else -> "main"
                },
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                },
                label = "AuthScreenContent"
            ) { targetState ->
                when (targetState) {
                    "otp" -> {
                        OtpInputScreen(
                            email = uiState.email,
                            otp = uiState.otp,
                            isSubmitting = uiState.isSubmitting,
                            errorMessage = uiState.errorMessage,
                            onOtpChange = onOtpChange,
                            onVerify = onVerifyOtp,
                            onResend = onSendOtpCode
                        )
                    }
                    "magic_link" -> {
                        MagicLinkInputScreen(
                            email = uiState.email,
                            isSubmitting = uiState.isSubmitting,
                            errorMessage = uiState.errorMessage,
                            onEmailChange = onEmailChange,
                            onSendMagicLink = onSendMagicLink
                        )
                    }
                    else -> {
                        EmailPasswordInputScreen(
                            email = uiState.email,
                            password = uiState.password,
                            confirmPassword = uiState.confirmPassword,
                            isSignUpMode = uiState.isSignUpMode,
                            isSubmitting = uiState.isSubmitting,
                            errorMessage = uiState.errorMessage,
                            emailError = uiState.emailError,
                            passwordError = uiState.passwordError,
                            confirmPasswordError = uiState.confirmPasswordError,
                            onEmailChange = onEmailChange,
                            onPasswordChange = onPasswordChange,
                            onConfirmPasswordChange = onConfirmPasswordChange,
                            onSubmit = onSubmit,
                            onMagicLinkClick = onMagicLinkClick,
                            onSendOtpCode = onSendOtpCode,
                            onToggleMode = onToggleAuthMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailPasswordInputScreen(
    email: String,
    password: String,
    confirmPassword: String,
    isSignUpMode: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    emailError: String? = null,
    passwordError: String? = null,
    confirmPasswordError: String? = null,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMagicLinkClick: () -> Unit,
    onSendOtpCode: () -> Unit,
    onToggleMode: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = if (isSignUpMode) "注册账号" else "欢迎回来",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSignUpMode) "加入 GleanRead，开启更智能的阅读与知识管理体验" else "登录以同步您的阅读心得、摘录与知识库",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            // Email Field
            Text(
                text = "电子邮箱",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            TextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = { Text("请输入邮箱地址") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                isError = emailError != null,
                shape = RoundedCornerShape(32.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (emailError != null) {
                Text(
                    text = emailError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            Text(
                text = "密码",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            TextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = { Text("请输入密码") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done
                ),
                isError = passwordError != null,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                shape = RoundedCornerShape(32.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (passwordError != null) {
                Text(
                    text = passwordError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                )
            }

            if (isSignUpMode) {
                Spacer(modifier = Modifier.height(12.dp))
                // Confirm Password Field
                Text(
                    text = "确认密码",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                TextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    placeholder = { Text("请再次输入密码") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    isError = confirmPasswordError != null,
                    shape = RoundedCornerShape(32.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (confirmPasswordError != null) {
                    Text(
                        text = confirmPasswordError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                    )
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp).align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Sign In Button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isSignUpMode) "立即注册" else "登录",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // OR Divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "OR",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // OTP Code Button
        Button(
            onClick = onSendOtpCode,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            enabled = !isSubmitting
        ) {
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "通过邮箱验证码登录",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Magic Link Button
        Button(
            onClick = onMagicLinkClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            enabled = !isSubmitting
        ) {
            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "通过 Magic Link 登录",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Text(
                text = if (isSignUpMode) "已有账号？" else "还没有账号？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (isSignUpMode) "立即登录" else "立即注册",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun MagicLinkInputScreen(
    email: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onSendMagicLink: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Magic Link 登录",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "我们将向您的邮箱发送一个特殊的链接，您只需点击该链接即可自动登录，无需输入密码。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "电子邮箱",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            TextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = { Text("请输入邮箱地址") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(32.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp).align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSendMagicLink,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "发送 Magic Link",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "如何使用：",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "1. 输入您的邮箱并点击上方按钮。\n2. 打开您的电子邮箱应用。\n3. 找到来自 GleanRead 的邮件。\n4. 点击邮件中的“登录”链接。\n5. 您将自动返回 App 并完成登录。",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OtpInputScreen(
    email: String,
    otp: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "输入验证码",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "我们已向 $email 发送了一个 6 位验证码，请输入验证码以完成身份验证。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        OtpInputField(
            value = otp,
            onValueChange = onOtpChange,
            length = 6,
            enabled = !isSubmitting
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = !isSubmitting && otp.length == 6
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "确认并继续",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
        ) {
            Text(
                text = "没有收到验证码？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onResend, enabled = !isSubmitting) {
                Text(
                    text = "重新发送",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    length: Int,
    enabled: Boolean
) {
    BasicTextField(
        value = value,
        onValueChange = {
            if (it.length <= length && it.all { char -> char.isDigit() }) {
                onValueChange(it)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        enabled = enabled,
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(length) { index ->
                    val char = when {
                        index >= value.length -> ""
                        else -> value[index].toString()
                    }
                    val isFocused = value.length == index && enabled

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .height(56.dp)
                            .widthIn(max = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        if (isFocused) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                                    .size(width = 20.dp, height = 2.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AuthScreenLoginPreview() {
    GleanReadTheme {
        AuthScreen(
            uiState = AuthUiState(
                email = "user@example.com",
                isSignUpMode = false
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onOtpChange = {},
            onToggleAuthMode = {},
            onSubmit = {},
            onMagicLinkClick = {},
            onOtpLoginClick = {},
            onSendMagicLink = {},
            onSendOtpCode = {},
            onVerifyOtp = {},
            onBackToEmail = {},
            onChooseOwnership = {},
            onDismissOwnershipDialog = {},
            onClearMessage = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenSignUpPreview() {
    GleanReadTheme {
        AuthScreen(
            uiState = AuthUiState(
                email = "newuser@example.com",
                isSignUpMode = true
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onOtpChange = {},
            onToggleAuthMode = {},
            onSubmit = {},
            onMagicLinkClick = {},
            onOtpLoginClick = {},
            onSendMagicLink = {},
            onSendOtpCode = {},
            onVerifyOtp = {},
            onBackToEmail = {},
            onChooseOwnership = {},
            onDismissOwnershipDialog = {},
            onClearMessage = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenOtpPreview() {
    GleanReadTheme {
        AuthScreen(
            uiState = AuthUiState(
                email = "user@example.com",
                showOtpScreen = true,
                otp = "1234"
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onOtpChange = {},
            onToggleAuthMode = {},
            onSubmit = {},
            onMagicLinkClick = {},
            onOtpLoginClick = {},
            onSendMagicLink = {},
            onSendOtpCode = {},
            onVerifyOtp = {},
            onBackToEmail = {},
            onChooseOwnership = {},
            onDismissOwnershipDialog = {},
            onClearMessage = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenMagicLinkPreview() {
    GleanReadTheme {
        AuthScreen(
            uiState = AuthUiState(
                email = "user@example.com",
                showMagicLinkScreen = true
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onOtpChange = {},
            onToggleAuthMode = {},
            onSubmit = {},
            onMagicLinkClick = {},
            onOtpLoginClick = {},
            onSendMagicLink = {},
            onSendOtpCode = {},
            onVerifyOtp = {},
            onBackToEmail = {},
            onChooseOwnership = {},
            onDismissOwnershipDialog = {},
            onClearMessage = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun AuthScreenLoadingPreview() {
    GleanReadTheme {
        AuthScreen(
            uiState = AuthUiState(
                email = "user@example.com",
                isSubmitting = true
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onOtpChange = {},
            onToggleAuthMode = {},
            onSubmit = {},
            onMagicLinkClick = {},
            onOtpLoginClick = {},
            onSendMagicLink = {},
            onSendOtpCode = {},
            onVerifyOtp = {},
            onBackToEmail = {},
            onChooseOwnership = {},
            onDismissOwnershipDialog = {},
            onClearMessage = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Error Message")
@Composable
fun AuthScreenErrorPreview() {
    GleanReadTheme {
        AuthScreen(
            uiState = AuthUiState(
                email = "user@example.com",
                errorMessage = "输入的邮箱或密码错误，请重试。"
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onOtpChange = {},
            onToggleAuthMode = {},
            onSubmit = {},
            onMagicLinkClick = {},
            onOtpLoginClick = {},
            onSendMagicLink = {},
            onSendOtpCode = {},
            onVerifyOtp = {},
            onBackToEmail = {},
            onChooseOwnership = {},
            onDismissOwnershipDialog = {},
            onClearMessage = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Ownership Dialog")
@Composable
fun AuthScreenOwnershipDialogPreview() {
    GleanReadTheme {
        AuthScreen(
            uiState = AuthUiState(
                showOwnershipDialog = true
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onOtpChange = {},
            onToggleAuthMode = {},
            onSubmit = {},
            onMagicLinkClick = {},
            onOtpLoginClick = {},
            onSendMagicLink = {},
            onSendOtpCode = {},
            onVerifyOtp = {},
            onBackToEmail = {},
            onChooseOwnership = {},
            onDismissOwnershipDialog = {},
            onClearMessage = {},
            onBackClick = {}
        )
    }
}
