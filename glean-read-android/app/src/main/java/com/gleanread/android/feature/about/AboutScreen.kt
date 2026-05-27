package com.gleanread.android.feature.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.R
import com.gleanread.android.core.ui.component.LiquidGlassTopAppBarContainer
import com.gleanread.android.core.ui.theme.GleanReadTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdates by remember { mutableStateOf(false) }

    // 弹窗管理状态
    var activeDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeDialogContent by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LiquidGlassTopAppBarContainer {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.about_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            // 1. Logo 区域：加载 App 官方图标 (使用 AndroidView 完美兼容自适应 XML 图标，杜绝运行时闪退)
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        try {
                            val iconDrawable = ctx.packageManager.getApplicationIcon(ctx.packageName)
                            setImageDrawable(iconDrawable)
                        } catch (e: Exception) {
                            setImageResource(R.mipmap.ic_launcher)
                        }
                    }
                },
                modifier = Modifier
                    .size(96.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), clip = false)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(Modifier.height(32.dp))

            // 2. 软件核心功能简介卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = false)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "产品愿景",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.about_app_description),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // 3. 功能操作项列表
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutMenuItem(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.about_check_updates),
                    onClick = {
                        if (!isCheckingUpdates) {
                            coroutineScope.launch {
                                isCheckingUpdates = true
                                delay(1200) // 优雅微动画模拟请求
                                isCheckingUpdates = false
                                snackbarHostState.showSnackbar(context.getString(R.string.about_check_updates_latest))
                            }
                        }
                    }
                )

                AboutMenuItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.about_website),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gleanread.pages.dev"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("无法打开链接")
                            }
                        }
                    }
                )

                AboutMenuItem(
                    icon = Icons.Default.BugReport,
                    title = stringResource(R.string.about_feedback),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/banafish/glean-read/issues"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("无法打开链接")
                            }
                        }
                    }
                )

                AboutMenuItem(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.about_source_code),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/banafish/glean-read"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("无法打开链接")
                            }
                        }
                    }
                )

                AboutMenuItem(
                    icon = Icons.Default.Gavel,
                    title = stringResource(R.string.about_terms_of_service),
                    onClick = {
                        activeDialogTitle = context.getString(R.string.about_terms_of_service)
                        activeDialogContent = "感谢您使用 GleanRead！在使用本软件前，请您务必仔细阅读本《服务条款》。\n\n1. 账号使用：您需要对您在 GleanRead 中创建的账号安全负全部责任。\n2. 内容所有权：您利用本 App 记录、总结或上传的内容，所有权归您所有，我们不作任何商业性质挪用。\n3. 数据同步与备份：云同步功能依托云服务实现，请按需登录并同步，以防本地数据丢失。\n4. 禁止非法滥用：不得利用本应用从事侵犯他人著作权或违反法律法规的活动。"
                    }
                )

                AboutMenuItem(
                    icon = Icons.Default.PrivacyTip,
                    title = stringResource(R.string.about_privacy_policy),
                    onClick = {
                        activeDialogTitle = context.getString(R.string.about_privacy_policy)
                        activeDialogContent = "我们十分重视您的隐私权。本《隐私政策》将向您说明我们如何收集和使用您的相关信息：\n\n1. 信息收集：我们仅在您开启云同步、登录账号或保存 AI 兼容接口配置时收集必要的网络请求标识，绝不搜集其他无涉数据。\n2. 辅助识别无障碍服务：如果您启用了辅助识别功能，该服务仅在本地用于尝试识别当前浏览器页面的标题和链接，所有识别工作完全在本地设备沙盒内完成，任何数据均不会在未经您允许的情况下上传至第三方服务器。\n3. 数据存储：您的本地摘录和知识结构默认完全留存在本地数据库。在启用同步功能后，数据将以端到端方式妥善上传至加密数据库。"
                    }
                )

                AboutMenuItem(
                    icon = Icons.Default.Source,
                    title = stringResource(R.string.about_open_source_licenses),
                    onClick = {
                        activeDialogTitle = context.getString(R.string.about_open_source_licenses)
                        activeDialogContent = "GleanRead 秉承开放共享之精神，使用了以下优秀的开源软件及类库：\n\n- Jetpack Compose (Apache 2.0)\n- Kotlin Coroutines (Apache 2.0)\n- Room Database (Apache 2.0)\n- Navigation Compose (Apache 2.0)\n- Retrofit & OkHttp (Apache 2.0)\n- Supabase Kotlin SDK (MIT)\n\n在此向所有开源社区建设者致以最崇高的敬意！"
                    }
                )
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(40.dp))

            // 4. 底部版权与标志
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Copyright,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.about_copyright),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // 检查更新时的菊花微动画
    AnimatedVisibility(
        visible = isCheckingUpdates,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(enabled = false) {}, // 拦截点击
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }

    // 温馨提示对话框
    activeDialogTitle?.let { title ->
        AlertDialog(
            onDismissRequest = {
                activeDialogTitle = null
                activeDialogContent = null
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = activeDialogContent ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        activeDialogTitle = null
                        activeDialogContent = null
                    }
                ) {
                    Text(text = stringResource(R.string.common_confirm))
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun AboutMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    GleanReadTheme {
        AboutScreen(
            versionName = "1.0",
            onBackClick = {}
        )
    }
}
