package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AuroraMint
import com.google.mlkit.genai.prompt.*
import com.google.mlkit.genai.common.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@Composable
fun GeminiNanoStatusCard(modifier: Modifier = Modifier) {
    var statusText by remember { mutableStateOf("檢查 Gemini Nano 狀態中...") }
    var promptResponse by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val generativeModelAndError = remember {
        val isEmulator = android.os.Build.FINGERPRINT.contains("generic") || 
                         android.os.Build.MODEL.contains("Emulator") ||
                         android.os.Build.MODEL.contains("sdk")
        
        if (isEmulator) {
            Pair(null, "本模擬器環境由於架構限制不支援完整 Gemini Nano 本機模型，請於實機測試。")
        } else {
            try {
                Pair(Generation.getClient(), null)
            } catch (e: Throwable) {
                Pair(null, "不支援此裝置: ${e.message}")
            }
        }
    }
    
    val generativeModel = generativeModelAndError.first
    val initError = generativeModelAndError.second
    
    LaunchedEffect(initError) {
        if (initError != null && statusText == "檢查 Gemini Nano 狀態中...") {
            statusText = initError
        }
    }

    LaunchedEffect(generativeModel) {
        if (generativeModel != null) {
            try {
                val status = generativeModel.checkStatus()
                when (status) {
                    FeatureStatus.UNAVAILABLE -> statusText = "本裝置不支援 Gemini Nano 模型"
                    FeatureStatus.AVAILABLE -> statusText = "Gemini Nano 已就緒 (可離線使用)"
                    FeatureStatus.DOWNLOADING -> statusText = "Gemini Nano 正在下載中..."
                    FeatureStatus.DOWNLOADABLE -> {
                        statusText = "Gemini Nano 可供下載"
                    }
                    else -> statusText = "未知狀態: $status"
                }
            } catch (e: Exception) {
                statusText = "檢查狀態失敗: ${e.message}"
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AuroraMint)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ML Kit On-Device GenAI", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = statusText, style = MaterialTheme.typography.bodyMedium)
            
            if (promptResponse.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = promptResponse, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.DarkGray,
                    modifier = Modifier.background(Color.LightGray.copy(alpha=0.3f), RoundedCornerShape(8.dp)).padding(8.dp).fillMaxWidth()
                )
            }

            if (statusText == "Gemini Nano 可供下載") {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        generativeModel?.download()?.collect { progressStatus ->
                            when (progressStatus) {
                                is DownloadStatus.DownloadStarted -> statusText = "開始下載..."
                                is DownloadStatus.DownloadProgress -> statusText = "下載中..."
                                is DownloadStatus.DownloadFailed -> statusText = "下載失敗"
                                DownloadStatus.DownloadCompleted -> statusText = "Gemini Nano 已就緒 (可離線使用)"
                                else -> statusText = "下載狀態: $progressStatus"
                            }
                        }
                    }
                }) {
                    Text("下載模型")
                }
            } else if (statusText == "Gemini Nano 已就緒 (可離線使用)") {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        promptResponse = "生成中..."
                        try {
                            val response = generativeModel?.generateContent("請用繁體中文寫一句早安問候語，要有活力！")
                            val textContent = response?.candidates?.firstOrNull()?.text ?: "無內容"
                            promptResponse = textContent
                        } catch (e: Exception) {
                            promptResponse = "生成失敗: ${e.message}"
                        }
                    }
                }) {
                    Text("測試生成問候語")
                }
            }
        }
    }
}
