package com.gleanread.android.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.gleanread.android.app.navigation.MainApp
import com.gleanread.android.core.ui.theme.GleanReadTheme
import com.gleanread.android.data.auth.AuthResult
import com.gleanread.android.data.auth.SupabaseAuthRepository
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
        consumeMagicLinkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeMagicLinkIntent(intent)
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

    private fun consumeMagicLinkIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (!SupabaseAuthRepository.isMagicLinkRedirect(uri)) return

        setIntent(Intent(intent).setData(null))
        lifecycleScope.launch {
            when (val result = appContainer.supabaseAuthRepository.completeMagicLinkSignIn(uri)) {
                is AuthResult.Success -> {
                    if (appContainer.supabaseAuthRepository.hasLocalUserData()) {
                        toast("Magic Link 登录成功，请在设置中选择本地数据归属")
                    } else {
                        appContainer.workspaceSyncRepository.setCloudSyncEnabled(true)
                        appContainer.workspaceSyncRepository.syncNow()
                        toast("Magic Link 登录成功")
                    }
                }

                is AuthResult.Failure -> toast(result.message)
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}



