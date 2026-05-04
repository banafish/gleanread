package com.gleanread.android.feature.settings.component

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.gleanread.android.data.avatar.CompressedImage
import com.gleanread.android.data.avatar.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAvatarSection(
    isLoggedIn: Boolean,
    email: String?,
    avatarUrl: String?,
    isAvatarUploading: Boolean,
    onNavigateToAuth: () -> Unit,
    onAvatarSelected: (CompressedImage) -> Unit
) {
    var showOptionsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val compressed = withContext(Dispatchers.IO) {
                    ImageUtils.compressImage(context, uri)
                }
                if (compressed != null) {
                    onAvatarSelected(compressed)
                } else {
                    Toast.makeText(context, "图片处理失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            coroutineScope.launch {
                val compressed = withContext(Dispatchers.IO) {
                    ImageUtils.compressImage(context, photoUri!!)
                }
                if (compressed != null) {
                    onAvatarSelected(compressed)
                } else {
                    Toast.makeText(context, "图片处理失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val createTempImageUri = {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.cacheDir
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    }

    if (showOptionsDialog) {
        ModalBottomSheet(onDismissRequest = { showOptionsDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("更换头像", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                        showOptionsDialog = false
                        photoUri = createTempImageUri()
                        takePicture.launch(photoUri!!)
                    }.padding(16.dp)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "拍照", modifier = Modifier.size(48.dp))
                        Text("拍照", modifier = Modifier.padding(top = 8.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                        showOptionsDialog = false
                        pickMedia.launch("image/*")
                    }.padding(16.dp)) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "相册", modifier = Modifier.size(48.dp))
                        Text("相册", modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable(enabled = isLoggedIn && !isAvatarUploading) {
                    showOptionsDialog = true
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoggedIn) {
                // 始终显示首字母作为底图/占位符
                Text(
                    text = email?.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
 
                if (avatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "用户头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                if (isAvatarUploading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "未登录",
                    modifier = Modifier.fillMaxSize(0.6f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoggedIn) {
            Text(
                text = email ?: "未知用户",
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            Text(
                text = "登录解锁完整体验，开启云端同步，实现多设备数据自动同步",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateToAuth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("登录 / 注册")
            }
        }
    }
}
