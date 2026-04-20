package com.gleanread.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gleanread.android.feature.main.MainApp
import com.gleanread.android.ui.theme.GleanReadTheme

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
}
