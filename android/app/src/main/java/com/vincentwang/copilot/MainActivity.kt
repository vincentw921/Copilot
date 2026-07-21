package com.vincentwang.copilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vincentwang.copilot.ui.RootScreen
import com.vincentwang.copilot.ui.theme.CopilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CopilotTheme {
                RootScreen()
            }
        }
    }
}
