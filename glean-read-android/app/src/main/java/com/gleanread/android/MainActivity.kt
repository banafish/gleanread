package com.gleanread.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gleanread.android.ui.theme.GleanReadTheme
import com.gleanread.android.ui.workspace.WorkspaceApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GleanReadTheme {
                WorkspaceApp()
            }
        }
    }
}
