package com.gleanread.android.feature.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageView
import com.gleanread.android.app.appContainer
import com.gleanread.android.core.ui.theme.GleanReadTheme
import com.gleanread.android.data.appearance.ThemeColor
import com.gleanread.android.data.appearance.ThemeMode

class AvatarCropActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
        if (imageUri == null) {
            finish()
            return
        }

        setContent {
            val themeMode by appContainer.appearancePreferencesRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val themeColor by appContainer.appearancePreferencesRepository.themeColorFlow.collectAsState(initial = ThemeColor.DYNAMIC)

            GleanReadTheme(themeMode = themeMode, themeColor = themeColor) {
                CropScreen(
                    imageUri = imageUri,
                    onCropSuccess = { croppedUri ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_CROPPED_IMAGE_URI, croppedUri)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_CROPPED_IMAGE_URI = "extra_cropped_image_uri"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    imageUri: Uri,
    onCropSuccess: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    var cropImageView by remember { mutableStateOf<CropImageView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("裁剪头像") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "取消")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        cropImageView?.setOnCropImageCompleteListener { _, result ->
                            if (result.isSuccessful) {
                                result.uriContent?.let { onCropSuccess(it) }
                            }
                        }
                        cropImageView?.croppedImageAsync()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "确定")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    CropImageView(context).apply {
                        // In 4.x, some methods might be properties or have slightly different names
                        setFixedAspectRatio(true)
                        setAspectRatio(1, 1)
                        guidelines = CropImageView.Guidelines.ON
                        cropShape = CropImageView.CropShape.OVAL
                        cropImageView = this
                    }
                },
                update = { view ->
                    view.setImageUriAsync(imageUri)
                }
            )
        }
    }
}
