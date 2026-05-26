package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.ui.MorningBriefingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "enableEdgeToEdge failed", e)
        }

        setContent {
            MyApplicationTheme {
                var aiCoreStatus by remember { mutableStateOf<Boolean?>(null) }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    try {
                        val pm = context.packageManager
                        val aicorePackages = listOf("com.google.android.aicore", "com.google.android.apps.aicore")
                        var supported = false
                        
                        for (pkg in aicorePackages) {
                            try {
                                val info = pm.getApplicationInfo(pkg, 0)
                                if (info.enabled) {
                                    supported = true
                                    android.util.Log.d("AICoreCheck", "Supported package found: $pkg")
                                    break
                                }
                            } catch (e: PackageManager.NameNotFoundException) {
                                // Continue to next
                            }
                        }
                        
                        aiCoreStatus = supported
                    } catch (e: Exception) {
                        android.util.Log.e("AICoreCheck", "Error checking AICore", e)
                        aiCoreStatus = false
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (aiCoreStatus) {
                        null -> {
                            // Loading screen with background for consistency
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0F172A)), 
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("正在獲取系統能力...", color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                        true -> {
                            MorningBriefingScreen()
                        }
                        false -> {
                            // Not supported screen with a retry/bypass for developers
                            AICoreNotSupportedScreen(onRetry = { aiCoreStatus = null })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AICoreNotSupportedScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "設備不支援",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "此應用程式專為具備 Android AI Core (Gemini Nano) 的高效能設備設計。您的手機目前不符合執行此 AI 簡報應用的硬體需求。",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            lineHeight = 24.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Text("重新檢測", color = Color.White)
        }
    }
}
