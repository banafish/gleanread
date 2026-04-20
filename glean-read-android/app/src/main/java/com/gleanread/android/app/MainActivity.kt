package com.gleanread.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gleanread.android.app.navigation.MainApp
import com.gleanread.android.core.ui.theme.GleanReadTheme

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



