package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.MorningBriefingScreen
import com.example.ui.MorningViewModel
import com.example.ui.theme.PixelBriefTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MorningViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup gorgeous full-bleed content
        enableEdgeToEdge()

        setContent {
            PixelBriefTheme {
                MorningBriefingScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
