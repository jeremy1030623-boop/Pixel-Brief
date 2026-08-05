package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.fragment.app.FragmentActivity
import com.example.ui.MorningBriefingScreen
import com.example.ui.theme.MyApplicationTheme
import java.util.Calendar

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable high refresh rate (120Hz / maximum supported refresh rate) for ultra-smooth layout rendering
        setupHighRefreshRate()

        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "enableEdgeToEdge failed", e)
        }

        setContent {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val isDarkMode = hour < 6 || hour >= 18
            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    MorningBriefingScreen()
                }
            }
        }
    }

    /**
     * Configures the display to prefer the maximum supported refresh rate (e.g. 120Hz).
     */
    private fun setupHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                
                if (display != null) {
                    val modes = display.supportedModes
                    // Look for 120Hz mode specifically; fallback to maximum supported rate
                    val targetMode = modes.find { Math.round(it.refreshRate) == 120 }
                        ?: modes.maxByOrNull { it.refreshRate }
                        
                    if (targetMode != null) {
                        val params = window.attributes
                        params.preferredDisplayModeId = targetMode.modeId
                        params.preferredRefreshRate = targetMode.refreshRate
                        window.attributes = params
                        android.util.Log.d("MainActivity", "Successfully set display mode to ${targetMode.refreshRate}Hz (Mode ID: ${targetMode.modeId})")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to set high refresh rate display mode", e)
            }
        }
    }
}
