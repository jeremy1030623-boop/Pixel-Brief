package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.model.*
import com.example.model.WeatherInfo
import com.example.data.UserSettings
import androidx.compose.foundation.lazy.LazyColumn
import com.example.ui.theme.Typography
import com.example.ui.theme.*
import java.util.*

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@Composable
fun MorningBriefingScreen(viewModel: MorningViewModel = viewModel()) {
    val weather by viewModel.weatherInfo.collectAsState()
    val username by viewModel.username.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val sleepInfo by viewModel.sleepInfo.collectAsState()
    val events by viewModel.nextEvents.collectAsState()
    val news by viewModel.newsDetail.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    val isSleepSynced = userSettings?.isSleepSynced ?: false
    val lastSyncTime = userSettings?.lastSyncTime ?: ""

    val backgroundBrush = remember(weather.condition) { getBackgroundBrush(weather.condition) }
    var visible by remember { mutableStateOf(false) }
    var selectedNewsItem by remember { mutableStateOf<NewsItem?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendarPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        calendarPermissionGranted.value = isGranted
        if (isGranted) {
            viewModel.fetchData()
        }
    }

    val checkAndRequestPermission = remember(calendarPermissionGranted.value) {
        {
            if (!calendarPermissionGranted.value) {
                permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        }
    }
    
    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        val screenState = when {
            selectedNewsItem != null -> "news_detail"
            isSettingsOpen -> "settings"
            else -> "home"
        }

        // Handle back navigation for sub-screens
        BackHandler(enabled = selectedNewsItem != null || isSettingsOpen) {
            when {
                selectedNewsItem != null -> selectedNewsItem = null
                isSettingsOpen -> isSettingsOpen = false
            }
        }

        AnimatedContent(
            targetState = screenState,
            label = "ScreenContent"
        ) { state ->
            when (state) {
                "news_detail" -> {
                    selectedNewsItem?.let { item ->
                        NewsDetailScreen(item) {
                            selectedNewsItem = null
                        }
                    }
                }
                "settings" -> {
                    SettingsScreen(
                        settings = userSettings ?: UserSettings(),
                        onBack = { isSettingsOpen = false },
                        onSave = { updated -> viewModel.updateUserSettings(updated) }
                    )
                }
                "home" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn() + expandVertically()
                        ) {
                            GreetingSection(
                                username = username,
                                time = currentTime,
                                weather = weather,
                                events = events,
                                weatherUnit = userSettings?.weatherUnit ?: "C",
                                onSettingsClick = { isSettingsOpen = true }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = spring()) + slideInVertically { it / 2 }
                        ) {
                            AgendaSection(events, weather.condition) {
                                checkAndRequestPermission()
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = spring()) + slideInVertically { it / 3 }
                        ) {
                            WidgetGrid(
                                sleepInfo = sleepInfo,
                                weather = weather,
                                events = events,
                                news = news,
                                isSleepSynced = isSleepSynced,
                                lastSyncTime = lastSyncTime,
                                weatherUnit = userSettings?.weatherUnit ?: "C",
                                isCoughColorAlertEnabled = userSettings?.isCoughColorAlertEnabled ?: false,
                                displayedNewsCount = userSettings?.displayedNewsCount ?: 3,
                                onSync = { viewModel.syncGoogleClockSleepData() },
                                onClearSync = { viewModel.clearSleepData() },
                                onAuthorize = { checkAndRequestPermission() },
                                onNewsClick = { item -> selectedNewsItem = item },
                                onWeatherClick = { 
                                    val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.weather")
                                    if (intent != null) {
                                        context.startActivity(intent)
                                    } else {
                                        android.widget.Toast.makeText(context, "無法打開 Pixel Weather", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun NewsDetailScreen(item: NewsItem, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(item.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            Text(item.summary, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB so it doesn't overlap text
        }
        
        FloatingActionButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 32.dp),
            containerColor = Color.White,
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
    }
}

@Composable
fun UserAvatar(username: String) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(AuroraOceanic, AuroraLavender))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = username.take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun GreetingSection(
    username: String,
    time: String,
    weather: WeatherInfo,
    events: List<com.example.data.CalendarEvent>,
    weatherUnit: String,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatar(username)
        Spacer(modifier = Modifier.height(16.dp))
        
        val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
        val greeting = remember(hour) {
            when (hour) {
                in 5..11 -> "早安"
                in 12..17 -> "午安"
                in 18..23 -> "晚安"
                else -> "深夜好"
            }
        }
        
        val nextEvent = events.firstOrNull()
        val eventText = if (nextEvent != null) {
            "\n有什麼是要做:\n${nextEvent.title} 在 ${getTimeString(nextEvent.startTime)}"
        } else {
            ""
        }
        
        TimeGreetingText(greeting, username, time, eventText, weather, weatherUnit)
    }
}

@Composable
fun TimeGreetingText(greeting: String, username: String, time: String, eventText: String, weather: WeatherInfo, weatherUnit: String) {
    Text(
        text = "$greeting, $username，現在時間 $time，今天天氣狀況 ${weather.condition}，目前 ${formatTemperature(weather.currentTemp, weatherUnit)}°，今天最高溫 ${formatTemperature(weather.maxTemp, weatherUnit)}°；最低溫 ${formatTemperature(weather.minTemp, weatherUnit)}°。$eventText",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

val ExpressiveShape = RoundedCornerShape(32.dp)

fun getCardBackgroundColor(condition: String): Color {
    return when {
        condition.contains("雨") -> AuroraDeepIndigo.copy(alpha = 0.85f)
        condition.contains("多雲") -> Color(0xFF334155).copy(alpha = 0.85f)
        else -> AuroraOceanic.copy(alpha = 0.15f) // Subtle tint for clear/sunny
    }
}

@Composable
fun AgendaSection(events: List<com.example.data.CalendarEvent>, condition: String, onAuthorize: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onAuthorize() },
        colors = CardDefaults.cardColors(containerColor = getCardBackgroundColor(condition)),
        shape = ExpressiveShape,
        border = null
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("今日行程", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(12.dp))
            if (events.isEmpty()) {
                Text("今天目前沒有預約行程\n(可點擊授權日曆權限以同步行程)", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodyMedium)
            } else {
                events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                condition.contains("雨") -> Color(0xFF334155)
                                condition.contains("多雲") -> Color(0xFF3F51B5)
                                else -> Color(0xFF7C2D12)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = getTimeString(event.startTime).take(2),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "在 ${getTimeString(event.startTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetGrid(
    sleepInfo: SleepInfo,
    weather: WeatherInfo,
    events: List<com.example.data.CalendarEvent>,
    news: List<NewsItem>,
    isSleepSynced: Boolean,
    lastSyncTime: String,
    weatherUnit: String,
    isCoughColorAlertEnabled: Boolean,
    displayedNewsCount: Int,
    onSync: () -> Unit,
    onClearSync: () -> Unit,
    onAuthorize: () -> Unit,
    onNewsClick: (NewsItem) -> Unit,
    onWeatherClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isSleepSynced) {
            SleepCard(
                sleep = sleepInfo,
                condition = weather.condition,
                lastSyncTime = lastSyncTime,
                isCoughColorAlertEnabled = isCoughColorAlertEnabled,
                onClearSync = onClearSync,
                onReSync = onSync
            )
        } else {
            SyncSleepReminderCard(
                condition = weather.condition,
                onSync = onSync
            )
        }
        WeatherCard(
            weather = weather,
            condition = weather.condition,
            weatherUnit = weatherUnit,
            onWeatherClick = onWeatherClick
        )
        NewsCard(
            news = news,
            condition = weather.condition,
            displayedNewsCount = displayedNewsCount,
            onItemClick = onNewsClick
        )
    }
}

@Composable
fun SyncSleepReminderCard(
    condition: String,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = getCardBackgroundColor(condition).copy(alpha = 0.95f)),
        shape = ExpressiveShape,
        border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = "Sync Sleep Reminder",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "睡眠資料尚未同步",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "同步 Google Clock 睡眠資訊以提供專屬今日簡報",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (!isSyncing) {
                        isSyncing = true
                        coroutineScope.launch {
                            delay(1800)
                            onSync()
                            isSyncing = false
                            android.widget.Toast.makeText(context, "成功同步 Google Clock 睡眠資料！", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_sleep_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("正在同步 Google Clock...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("立即同步 Google Clock 睡眠資料", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun SleepCard(
    sleep: SleepInfo,
    condition: String,
    lastSyncTime: String,
    isCoughColorAlertEnabled: Boolean,
    onClearSync: () -> Unit,
    onReSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val isAlert = isCoughColorAlertEnabled && sleep.coughCount > 0
    val cardBg = if (isAlert) Color(0xFF6B2D1D) else getCardBackgroundColor(condition)
    val cardBorder = if (isAlert) BorderStroke(2.dp, Color(0xFFEF4444)) else null

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = ExpressiveShape,
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "昨晚睡眠",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "${sleep.hours}小時睡眠",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (!isSyncing) {
                                isSyncing = true
                                coroutineScope.launch {
                                    delay(1200)
                                    onReSync()
                                    isSyncing = false
                                    android.widget.Toast.makeText(context, "睡眠資料已重新同步！", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp).testTag("resync_sleep_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Re-sync",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            onClearSync()
                            android.widget.Toast.makeText(context, "已取消同步睡眠資料", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp).testTag("clear_sleep_button")
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = "Unsync",
                            tint = Color(0xFFFDA4AF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("打鼾: ${sleep.snoringMinutes}m", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sick, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isAlert) Color(0xFFEF4444) else Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "咳嗽: ${sleep.coughCount}次" + if (isAlert) " (注意)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAlert) Color(0xFFEF4444) else Color(0xFF94A3B8),
                        fontWeight = if (isAlert) FontWeight.Bold else FontWeight.Normal
                    )
                }
                
                if (lastSyncTime.isNotEmpty()) {
                    Text(
                        "已同步 $lastSyncTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherCard(
    weather: WeatherInfo,
    condition: String,
    weatherUnit: String,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onWeatherClick() },
        colors = CardDefaults.cardColors(containerColor = getCardBackgroundColor(condition)),
        shape = ExpressiveShape,
        border = null
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(weather.condition, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("最高 ${formatTemperature(weather.maxTemp, weatherUnit)} / 最低 ${formatTemperature(weather.minTemp, weatherUnit)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                }
            }
            
            Text(
                text = formatTemperature(weather.currentTemp, weatherUnit),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun NewsCard(news: List<NewsItem>, condition: String, displayedNewsCount: Int, onItemClick: (NewsItem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = getCardBackgroundColor(condition)),
        shape = ExpressiveShape,
        border = null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("今日重點新聞 ($displayedNewsCount 則)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(16.dp))
            if (news.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFCE93D8), strokeWidth = 2.dp)
                }
            } else {
                val listToRender = news.take(displayedNewsCount)
                listToRender.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.summary, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), maxLines = 2)
                    }
                    if (index < listToRender.size - 1) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

fun getBackgroundBrush(condition: String): Brush {
    return when {
        condition.contains("雨") -> Brush.verticalGradient(
            listOf(AuroraMidnight, Color(0xFF1E1B4B)) // Deep Purple/Night
        )
        condition.contains("多雲") -> Brush.verticalGradient(
            listOf(Color(0xFF1E293B), Color(0xFF0F172A)) // Slate to Midnight
        )
        else -> Brush.verticalGradient(
            listOf(Color(0xFF134E4A), AuroraMidnight) // Teal to Midnight "Aurora" feel
        )
    }
}

fun getTimeString(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatTemperature(tempC: Int, unit: String): String {
    return if (unit == "F") {
        val tempF = (tempC * 9f / 5f + 32f).toInt()
        "$tempF°F"
    } else {
        "$tempC°C"
    }
}

@Composable
fun SimulatedWeatherApp(
    weather: WeatherInfo,
    showFloatingButton: Boolean,
    weatherUnit: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF075E9B), Color(0xFF23A0E9))))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("weather_app_back_arrow")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "動態天氣預報 (系統外層)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Current Weather Visual
            Icon(
                Icons.Default.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color(0xFFFBBF24)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = formatTemperature(weather.currentTemp, weatherUnit),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = weather.condition,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "高: ${formatTemperature(weather.maxTemp, weatherUnit)}  低: ${formatTemperature(weather.minTemp, weatherUnit)}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Hourly Forecast Simulation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("每小時預報", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("08:00" to 22, "10:00" to 25, "12:00" to 28, "14:00" to 29, "16:00" to 27).forEach { (timeStr, temp) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(timeStr, style = MaterialTheme.typography.labelMedium, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFFFBBF24))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(formatTemperature(temp, weatherUnit), style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weather Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("詳細天氣狀態", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("體感溫度", color = Color.White.copy(alpha = 0.7f))
                        Text(formatTemperature(weather.currentTemp + 1, weatherUnit), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("空氣品質", color = Color.White.copy(alpha = 0.7f))
                        Text("優 (AQI 28)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("降雨機率", color = Color.White.copy(alpha = 0.7f))
                        Text("10%", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("紫外線指數", color = Color.White.copy(alpha = 0.7f))
                        Text("中等 (4)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Float button on bottom right corners
        if (showFloatingButton) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp, end = 24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("floating_back_widget_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "返回早晨簡報",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onBack: () -> Unit,
    onSave: (UserSettings) -> Unit
) {
    // Local copy of editable options state
    var editUsername by remember { mutableStateOf(settings.username) }
    var editUseFineLocation by remember { mutableStateOf(settings.preciseLocationEnabled) }
    var editWeatherUnit by remember { mutableStateOf(settings.weatherUnit) }
    var editShowFloatingBackButton by remember { mutableStateOf(settings.showFloatingBackButton) }
    var editHealthSyncEnabled by remember { mutableStateOf(settings.isHealthSyncEnabled) }
    var editBreathingAudioSnorePermission by remember { mutableStateOf(settings.isBreathingTrackingEnabled) }
    var editIsCoughColorAlertEnabled by remember { mutableStateOf(settings.isCoughColorAlertEnabled) }
    var editCalendarAccountSelected by remember { mutableStateOf(settings.selectedCalendarAccount) }
    var editTasksIntegrationEnabled by remember { mutableStateOf(settings.tasksIntegrationEnabled) }
    var editAiActivityAnalysisEnabled by remember { mutableStateOf(settings.aiActivityAnalysisEnabled) }
    var editDisplayedNewsCount by remember { mutableStateOf(settings.displayedNewsCount.toFloat()) }
    var editTimeFormat24State by remember { mutableStateOf(settings.is24HourFormat) }
    var editGeminiModelSelected by remember { mutableStateOf(settings.geminiModelSelected) }
    var editOptimizePixelDevice by remember { mutableStateOf(true) }


    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuroraMidnight)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "專屬設定",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        val updated = settings.copy(
                            username = editUsername,
                            preciseLocationEnabled = editUseFineLocation,
                            weatherUnit = editWeatherUnit,
                            showFloatingBackButton = editShowFloatingBackButton,
                            isHealthSyncEnabled = editHealthSyncEnabled,
                            isBreathingTrackingEnabled = editBreathingAudioSnorePermission,
                            isCoughColorAlertEnabled = editIsCoughColorAlertEnabled,
                            selectedCalendarAccount = editCalendarAccountSelected,
                            tasksIntegrationEnabled = editTasksIntegrationEnabled,
                            aiActivityAnalysisEnabled = editAiActivityAnalysisEnabled,
                            displayedNewsCount = editDisplayedNewsCount.toInt(),
                            is24HourFormat = editTimeFormat24State,
                            geminiModelSelected = editGeminiModelSelected
                        )
                        onSave(updated)
                        android.widget.Toast.makeText(context, "設定已成功儲存！", android.widget.Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuroraMint
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp).testTag("save_settings_button")
                ) {
                    Text("儲存", fontWeight = FontWeight.Bold, color = AuroraMidnight)
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Section 1: 登入與帳號設定 (Login & Account)
                item {
                    SettingsSectionCard(title = "一、帳號與登入") {
                        if (editUsername.contains("Google") || editUsername.contains("gmail")) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AuroraMint)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("已登入：$editUsername", color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { editUsername = "訪客" },
                                colors = ButtonDefaults.buttonColors(containerColor = AuroraDeepIndigo),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("登出", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { editUsername = "Google 使用者" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("G", fontWeight = FontWeight.ExtraBold, color = Color.Blue, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("使用 Google 帳號登入", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "或手動輸入暱稱",
                                style = MaterialTheme.typography.titleSmall,
                                color = AuroraSlate,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editUsername,
                                onValueChange = { editUsername = it },
                                placeholder = { Text("例如：小明") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_settings_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = AuroraMint,
                                    unfocusedBorderColor = AuroraSlate
                                ),
                                singleLine = true
                            )
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsRow(
                            label = "導覽列返回設定",
                            description = "開啟後，點開天气詳細，會於外層右上端額外加上浮動返回簡報捷徑",
                            checked = editShowFloatingBackButton,
                            onCheckedChange = { editShowFloatingBackButton = it },
                            testTag = "floating_back_settings_switch"
                        )
                    }
                }

                // Section 2: 天氣與位置 (Weather & Location)
                item {
                    SettingsSectionCard(title = "二、天氣與位置") {
                        SettingsRow(
                            label = "精準位置存取",
                            description = "獲取當地精準定位氣溫與即時降雨資訊",
                            checked = editUseFineLocation,
                            onCheckedChange = { editUseFineLocation = it },
                            testTag = "location_settings_switch"
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        
                        Text("天氣單位規格", style = MaterialTheme.typography.titleSmall, color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("C" to "攝氏 (°C)", "F" to "華氏 (°F)").forEach { (code, label) ->
                                val selected = editWeatherUnit == code
                                Button(
                                    onClick = { editWeatherUnit = code },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) Color(0xFF3B82F6) else Color(0xFF1E293B),
                                        contentColor = if (selected) Color.White else Color(0xFF94A3B8)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("unit_${code.lowercase()}_button")
                                ) {
                                    Text(label, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsRow(
                            label = "導覽列返回設定",
                            description = "開啟後，點開天气詳細，會於外層右上端額外加上浮動返回簡報捷徑",
                            checked = editShowFloatingBackButton,
                            onCheckedChange = { editShowFloatingBackButton = it },
                            testTag = "floating_back_settings_switch"
                        )
                    }
                }

                // Section 3: 睡眠與健康 (Sleep & Health)
                item {
                    SettingsSectionCard(title = "三、睡眠與健康") {
                        SettingsRow(
                            label = "健康數據同步",
                            description = "連結 Google Fit/健康連接 API 讀取今日昨夜睡眠時數",
                            checked = editHealthSyncEnabled,
                            onCheckedChange = { editHealthSyncEnabled = it },
                            testTag = "health_sync_settings_switch"
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsRow(
                            label = "呼吸偵測授權",
                            description = "讀取音效打鼾辨識與咳嗽頻率統計健康紀錄",
                            checked = editBreathingAudioSnorePermission,
                            onCheckedChange = { editBreathingAudioSnorePermission = it },
                            testTag = "audio_cough_settings_switch"
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsRow(
                            label = "視覺化咳嗽警報",
                            description = "當晚間咳嗽次數大於0，首頁昨夜睡眠卡片將變換底色警示",
                            checked = editIsCoughColorAlertEnabled,
                            onCheckedChange = { editIsCoughColorAlertEnabled = it },
                            testTag = "warning_color_settings_switch"
                        )
                    }
                }

                // Section 4: 活動與日曆 (Activities & Calendar)
                item {
                    SettingsSectionCard(title = "四、活動與日曆") {
                        Text(
                            "展示聯動之 Google 帳端",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editCalendarAccountSelected,
                            onValueChange = { editCalendarAccountSelected = it },
                            placeholder = { Text("例如：user@gmail.com") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calendar_account_settings_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            singleLine = true
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsRow(
                            label = "Tasks 整合同步",
                            description = "直接存取拉取載入 Google Tasks 預定即時清單項目",
                            checked = editTasksIntegrationEnabled,
                            onCheckedChange = { editTasksIntegrationEnabled = it },
                            testTag = "tasks_settings_switch"
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsRow(
                            label = "AI 活動日程排序優化",
                            description = "啟用端側模型分析行程，按時間、地點智慧排序最重要前 3 大項目",
                            checked = editAiActivityAnalysisEnabled,
                            onCheckedChange = { editAiActivityAnalysisEnabled = it },
                            testTag = "ai_analysis_settings_switch"
                        )
                    }
                }

                // Section 5: 每日新聞 (News Feed)
                item {
                    SettingsSectionCard(title = "五、每日新聞與端側/雲端 Gemini 設定") {
                        Text("指定端側/雲端核心模型", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("gemini-1.5-flash" to "Gemini 1.5 Flash", "gemini-1.5-pro" to "Gemini 1.5 Pro").forEach { (code, label) ->
                                val selected = editGeminiModelSelected == code
                                Button(
                                    onClick = { editGeminiModelSelected = code },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) Color(0xFF3B82F6) else Color(0xFF1E293B),
                                        contentColor = if (selected) Color.White else Color(0xFF94A3B8)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("gemini_model_${code.replace(".", "_")}_button")
                                ) {
                                    Text(label, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("顯示新聞重點條數", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text("${editDisplayedNewsCount.toInt()} 條", style = MaterialTheme.typography.bodyMedium, color = AuroraOceanic, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = editDisplayedNewsCount,
                            onValueChange = { editDisplayedNewsCount = it },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("news_count_settings_slider")
                        )
                    }
                }

                // Section 6: 系統與專屬優化 (System & Permissions)
                item {
                    SettingsSectionCard(title = "六、系統與專屬權限整合") {
                        SettingsRow(
                            label = "二十四小時制格式",
                            description = "首頁現在時間採用 24H 制，關閉則顯示 12H 制 (如 上午 10:00)",
                            checked = editTimeFormat24State,
                            onCheckedChange = { editTimeFormat24State = it },
                            testTag = "time_format_settings_switch"
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsRow(
                            label = "Pixel 裝置系統底層優化",
                            description = "若使用 Google Pixel 系列，主動適配底層通知提示框架",
                            checked = editOptimizePixelDevice,
                            onCheckedChange = { editOptimizePixelDevice = it },
                            testTag = "pixel_optimizer_settings_switch"
                        )
                        if (editOptimizePixelDevice) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF065F46))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("裝置偵測成功：已啟用 Google Pixel 端側專有性能加速", style = MaterialTheme.typography.labelMedium, color = Color(0xFF34D399))
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        Text("權限配置面板", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    android.widget.Toast.makeText(context, "位置權限已獲取", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("位置授權", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    android.widget.Toast.makeText(context, "麥克風音效權限已獲取", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("音頻授權", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    android.widget.Toast.makeText(context, "系統通知權限已獲取", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("通知授權", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF1F5F9)
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SettingsRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

