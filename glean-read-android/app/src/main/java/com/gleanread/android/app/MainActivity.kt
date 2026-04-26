package com.gleanread.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.gleanread.android.app.navigation.MainApp
import com.gleanread.android.core.ui.theme.GleanReadTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GleanReadTheme {
                MainApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appContainer.workspaceRealtimeSyncController.start(lifecycleScope)
        lifecycleScope.launch {
            appContainer.workspaceSyncRepository.syncNow()
        }
    }

    override fun onStop() {
        appContainer.workspaceRealtimeSyncController.stop(lifecycleScope)
        super.onStop()
    }
}



