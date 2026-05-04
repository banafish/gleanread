package com.gleanread.android.feature.settings.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gleanread.android.data.auth.LocalDataOwnershipChoice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleAuthMode: () -> Unit,
    onSubmit: () -> Unit,
    onSendMagicLink: () -> Unit,
    onChooseOwnership: (LocalDataOwnershipChoice) -> Unit,
    onDismissOwnershipDialog: () -> Unit,
    onClearMessage: () -> Unit,
    onBackClick: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

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
            onDismissRequest = onDismissOwnershipDialog,
            title = { Text("本地数据处理") },
            text = { Text("您在本地有未同步的数据。登录后您希望如何处理这些数据？") },
            confirmButton = {
                TextButton(onClick = { onChooseOwnership(LocalDataOwnershipChoice.MERGE_TO_ACCOUNT) }) {
                    Text("合并到当前账号")
                }
            },
            dismissButton = {
                TextButton(onClick = { onChooseOwnership(LocalDataOwnershipChoice.KEEP_LOCAL) }) {
                    Text("保留本地 (不同步)")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isSignUpMode) "注册" else "登录") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "欢迎使用 GleanRead",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text("邮箱") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (uiState.isSignUpMode) ImeAction.Next else ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = if (uiState.isSignUpMode) 16.dp else 24.dp)
            )

            if (uiState.isSignUpMode) {
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("确认密码") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (uiState.isSignUpMode) "注册" else "登录")
                }
            }

            if (!uiState.isSignUpMode) {
                OutlinedButton(
                    onClick = onSendMagicLink,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !uiState.isSubmitting && uiState.email.isNotBlank()
                ) {
                    Text("发送 Magic Link 登录")
                }
            }

            TextButton(
                onClick = onToggleAuthMode,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(if (uiState.isSignUpMode) "已有账号？去登录" else "没有账号？去注册")
            }
        }
    }
}
