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
import com.example.ui.MorningBriefingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    companion object {
        @Volatile
        var globalExceptionStr: String? = null
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, refresh UI if needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            globalExceptionStr = "Thread [${thread.name}]: ${throwable.stackTraceToString()}"
            defaultHandler?.uncaughtException(thread, throwable)
        }

        var buildError: String? = null
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            buildError = "EdgeToEdge configuration error: ${e.stackTraceToString()}"
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var errorState by remember { mutableStateOf(buildError ?: globalExceptionStr) }

                    // Periodically poll for background exception updates
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        while (true) {
                            if (globalExceptionStr != null && errorState != globalExceptionStr) {
                                errorState = globalExceptionStr
                            }
                            kotlinx.coroutines.delay(1000)
                        }
                    }

                    if (errorState != null) {
                        DiagnosticErrorScreen(errorText = errorState!!)
                    } else {
                        MorningBriefingScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticErrorScreen(errorText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2E))
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "⚠️ pixel brief 診斷監測中心",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF38BA8),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "應用程式在初始化或運作期間發生了未預期的例外異常。診斷模式已主動捕捉並彙整了詳細的錯誤追蹤堆疊：",
            fontSize = 14.sp,
            color = Color(0xFFCDD6F4),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "【錯誤異常堆疊追蹤詳細】",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF9E2AF)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color(0xFF313244),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = errorText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFFA6E3A1),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
