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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
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

sealed class ScreenState {
    object Home : ScreenState()
    object Settings : ScreenState()
    data class NewsDetail(val item: NewsItem) : ScreenState()
}

@Composable
fun MorningBriefingScreen(viewModel: MorningViewModel = viewModel()) {
    val weather by viewModel.weatherInfo.collectAsState()
    val username by viewModel.username.collectAsState()
    val timeState by viewModel.timeState.collectAsState()
    val sleepInfo by viewModel.sleepInfo.collectAsState()
    val healthInfo by viewModel.healthInfo.collectAsState()
    val events by viewModel.nextEvents.collectAsState()
    val news by viewModel.newsDetail.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val goalSuggestion by viewModel.goalSuggestion.collectAsState()

    val isSleepSynced = userSettings?.isSleepSynced ?: false
    val lastSyncTime = userSettings?.lastSyncTime ?: ""

    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val isNight = hour !in 6..18
    val backgroundBrush = remember(weather.condition, isNight) { getBackgroundBrush(weather.condition, isNight) }
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

    val locationPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        calendarPermissionGranted.value = isGranted
        if (isGranted) {
            viewModel.fetchData()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionGranted.value = isGranted
        viewModel.fetchData()
    }

    val checkAndRequestCalendarPermission = remember(calendarPermissionGranted.value) {
        {
            if (!calendarPermissionGranted.value) {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        }
    }

    val checkAndRequestLocationPermission = remember(locationPermissionGranted.value) {
        {
            if (!locationPermissionGranted.value) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
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
        val currentNewsItem = selectedNewsItem
        val screenState = remember(currentNewsItem, isSettingsOpen) {
            when {
                currentNewsItem != null -> ScreenState.NewsDetail(currentNewsItem)
                isSettingsOpen -> ScreenState.Settings
                else -> ScreenState.Home
            }
        }

        // Handle back navigation for sub-screens
        BackHandler(enabled = currentNewsItem != null || isSettingsOpen) {
            when {
                currentNewsItem != null -> selectedNewsItem = null
                isSettingsOpen -> isSettingsOpen = false
            }
        }

        Crossfade(
            targetState = screenState,
            label = "ScreenContent"
        ) { state ->
            when (state) {
                is ScreenState.NewsDetail -> {
                    NewsDetailScreen(state.item, isNight) {
                        selectedNewsItem = null
                    }
                }
                is ScreenState.Settings -> {
                    SettingsScreen(
                        settings = userSettings ?: UserSettings(),
                        onBack = { isSettingsOpen = false },
                        onSave = { updated -> viewModel.updateUserSettings(updated) }
                    )
                }
                is ScreenState.Home -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        GeminiNanoStatusCard(modifier = Modifier.padding(bottom = 16.dp))
                        
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn() + expandVertically()
                        ) {
                            GreetingSection(
                                username = username,
                                avatarEmoji = userSettings?.avatarEmoji ?: "🦊",
                                avatarGradientIndex = userSettings?.avatarGradientIndex ?: 0,
                                greeting = timeState.greeting,
                                time = timeState.time,
                                weather = weather,
                                events = events,
                                weatherUnit = userSettings?.weatherUnit ?: "C",
                                isNight = isNight,
                                goalSuggestion = goalSuggestion,
                                onSettingsClick = { isSettingsOpen = true },
                                onProfileChange = { newName, newEmoji, newGradientIndex ->
                                    val current = userSettings ?: UserSettings()
                                    viewModel.updateUserSettings(
                                        current.copy(
                                            username = newName,
                                            avatarEmoji = newEmoji,
                                            avatarGradientIndex = newGradientIndex
                                        )
                                    )
                                },
                                onRefreshLocation = {
                                    checkAndRequestLocationPermission()
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = spring()) + slideInVertically { it / 2 }
                        ) {
                            AgendaSection(events, weather.condition, isNight) {
                                checkAndRequestCalendarPermission()
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
                                sleepSyncDurationMs = userSettings?.sleepSyncDurationMs ?: 0L,
                                weatherUnit = userSettings?.weatherUnit ?: "C",
                                isCoughColorAlertEnabled = userSettings?.isCoughColorAlertEnabled ?: false,
                                displayedNewsCount = userSettings?.displayedNewsCount ?: 3,
                                newsMode = userSettings?.newsMode ?: "local",
                                isNight = isNight,
                                onSync = { viewModel.syncHealthData() },
                                onClearSync = { viewModel.clearSleepData() },
                                onAuthorize = { checkAndRequestCalendarPermission() },
                                onNewsModeChange = { newMode ->
                                    val current = userSettings ?: UserSettings()
                                    viewModel.updateUserSettings(current.copy(newsMode = newMode))
                                },
                                onNewsClick = { item -> selectedNewsItem = item },
                                onWeatherClick = { 
                                    try {
                                        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.weather")
                                        if (intent != null) {
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } else {
                                            android.widget.Toast.makeText(context, "無法打開 Pixel Weather", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Throwable) {
                                        android.widget.Toast.makeText(context, "無法打開 Pixel Weather: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
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
fun NewsDetailScreen(item: NewsItem, isNight: Boolean, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isNight) AuroraMidnight else Color(0xFFF8FAFC))
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                item.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = if (isNight) Color.White else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                item.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isNight) Color(0xFFF1F5F9) else Color(0xFF334155)
            )
            
            val isUrlValid = remember(item.url) {
                !item.url.isNullOrBlank() && (item.url.startsWith("http://") || item.url.startsWith("https://"))
            }
            if (isUrlValid) {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        try {
                            item.url?.let { uriHandler.openUri(it) }
                        } catch (e: Throwable) {
                            android.util.Log.e("NewsDetailScreen", "Failed to open link", e)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuroraMint,
                        contentColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("open_news_url_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = "閱讀新聞")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("前往 Google 新聞閱讀完整報導", fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB so it doesn't overlap text
        }
        
        FloatingActionButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 32.dp),
            containerColor = if (isNight) Color.White else AuroraMidnight,
            contentColor = if (isNight) Color.Black else Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
    }
}


val AvatarGradients = listOf(
    listOf(Color(0xFF00A896), Color(0xFFB388FF)), // Oceanic Aurora
    listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)), // Lavender Dream
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)), // Tropical Sunset
    listOf(Color(0xFF10B981), Color(0xFF3B82F6)), // Emerald Mint
    listOf(Color(0xFF3F51B5), Color(0xFFE91E63)), // Cosmic Indigo
    listOf(Color(0xFF374151), Color(0xFFFCD34D))  // Charcoal Gold
)

val AvatarEmojis = listOf(
    "🦊", "🐼", "🦁", "🐨", "🐱", "🐶", "🐯", "🐻", 
    "🚀", "🎨", "🌟", "🎯", "☕", "☀️", "🌈", "🔥"
)

@Composable
fun UserAvatar(
    username: String,
    avatarEmoji: String,
    gradientIndex: Int,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    textSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    modifier: Modifier = Modifier
) {
    val gradientColors = AvatarGradients.getOrElse(gradientIndex) { AvatarGradients[0] }
    val displayChar = if (avatarEmoji.isBlank()) username.take(1).uppercase() else avatarEmoji
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(100))
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayChar,
            fontSize = textSize,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GreetingSection(
    username: String,
    avatarEmoji: String,
    avatarGradientIndex: Int,
    time: String,
    greeting: String,
    weather: WeatherInfo,
    events: List<com.example.data.CalendarEvent>,
    weatherUnit: String,
    isNight: Boolean,
    goalSuggestion: String,
    onSettingsClick: () -> Unit,
    onProfileChange: (name: String, emoji: String, gradientIndex: Int) -> Unit,
    onRefreshLocation: () -> Unit
) {
    var isEditingProfile by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(username) }
    var selectedEmoji by remember { mutableStateOf(avatarEmoji.ifBlank { "🦊" }) }
    var selectedGradientIndex by remember { mutableStateOf(avatarGradientIndex) }

    LaunchedEffect(username, avatarEmoji, avatarGradientIndex) {
        tempName = username
        selectedEmoji = avatarEmoji.ifBlank { "🦊" }
        selectedGradientIndex = avatarGradientIndex
    }
    
    val nextEvent = events.firstOrNull()
    val eventText = if (nextEvent != null) {
        "\n今天的活動:\n${nextEvent.title} 在 ${getTimeString(nextEvent.startTime)}"
    } else {
        ""
    }

    if (isEditingProfile) {
        AlertDialog(
            onDismissRequest = { isEditingProfile = false },
            title = {
                Text(
                    "自訂您的個人簡報檔案",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "您可以自訂大頭像風格與稱呼，這些將會精緻呈現於全天簡報標題中！",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Huge visual preview of the Big Avatar
                    UserAvatar(
                        username = tempName,
                        avatarEmoji = selectedEmoji,
                        gradientIndex = selectedGradientIndex,
                        size = 80.dp,
                        textSize = 36.sp,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(100))
                    )
                    
                    // Name textfield
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("您的名字") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_username_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AuroraMint,
                            unfocusedBorderColor = AuroraSlate,
                            focusedLabelColor = AuroraMint,
                            unfocusedLabelColor = AuroraSlate
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Emoji Grid Selector
                    Text(
                        "一、選擇個人 Emoji 符號",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AvatarEmojis.chunked(4).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                chunk.forEach { emoji ->
                                    val isSelected = selectedEmoji == emoji
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.25f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isSelected) AuroraMint else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedEmoji = emoji }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Gradient Color Selector
                    Text(
                        "二、選擇頭像漸層背景",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarGradients.forEachIndexed { index, colors ->
                            val isSelected = selectedGradientIndex == index
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(Brush.linearGradient(colors))
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(100)
                                    )
                                    .clickable { selectedGradientIndex = index }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已選取",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp).align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            onProfileChange(tempName.trim(), selectedEmoji, selectedGradientIndex)
                            isEditingProfile = false
                        }
                    },
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    Text("儲存", color = AuroraMint, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingProfile = false }) {
                    Text("取消", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = AuroraDeepIndigo,
            textContentColor = Color.White
        )
    }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { isEditingProfile = true },
                    onLongClick = { isEditingProfile = true }
                )
                .padding(vertical = 4.dp)
                .testTag("greeting_section_interactive_row"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                username = username,
                avatarEmoji = avatarEmoji,
                gradientIndex = avatarGradientIndex,
                size = 52.dp,
                textSize = 24.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = greeting.replace(",", "") + ", $username!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isNight) Color.White else Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "編輯姓名",
                        tint = if (isNight) Color.White.copy(alpha = 0.6f) else Color(0xFF1E293B).copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "點擊或長按頭像以自訂大頭像與名稱",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isNight) Color.White.copy(alpha = 0.4f) else Color(0xFF1E293B).copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TimeGreetingText(time, eventText, weather, weatherUnit, onRefreshLocation, isNight, goalSuggestion)
    }
}

@Composable
fun TimeGreetingText(
    time: String,
    eventText: String,
    weather: WeatherInfo,
    weatherUnit: String,
    onRefreshLocation: () -> Unit,
    isNight: Boolean,
    goalSuggestion: String
) {
    Column {
        Text(
            text = "現在時間 $time，今天天氣狀況 ${weather.condition}，目前 ${formatTemperature(weather.currentTemp, weatherUnit)}°，今天最高溫 ${formatTemperature(weather.maxTemp, weatherUnit)}°；最低溫 ${formatTemperature(weather.minTemp, weatherUnit)}°。$eventText",
            style = MaterialTheme.typography.titleLarge,
            color = if (isNight) Color.White else Color(0xFF1E293B),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
        if (goalSuggestion.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "💡 $goalSuggestion",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isNight) Color.White.copy(alpha = 0.85f) else Color(0xFF334155),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }
    }
}

val ExpressiveShape = RoundedCornerShape(32.dp)

fun getCardBackgroundColor(condition: String, isNight: Boolean = false): Color {
    if (!isNight) {
        // Safe dark glassmorphic backgrounds for cards during the day to keep white text ultra-legible
        return when {
            condition.contains("雷") -> Color(0xFF78350F).copy(alpha = 0.85f) // Warm dark brown-amber
            condition.contains("雨") -> Color(0xFF451A03).copy(alpha = 0.85f) // Warm deep rust
            condition.contains("雪") -> Color(0xFF713F12).copy(alpha = 0.82f) // Warm gold-slate
            condition.contains("霧") || condition.contains("陰") -> Color(0xFF5D4037).copy(alpha = 0.82f) // Warm brown
            condition.contains("多雲") -> Color(0xFFB45309).copy(alpha = 0.85f) // Warm amber
            condition == "晴朗" -> Color(0xFFD97706).copy(alpha = 0.85f) // Sunny gold
            else -> Color(0xFF78350F).copy(alpha = 0.85f) // Dark amber
        }
    }

    return when {
        condition.contains("雷") -> Color(0xFF4F46E5).copy(alpha = 0.45f) // Stormy: Intense Indigo
        condition.contains("雨") -> Color(0xFF2563EB).copy(alpha = 0.45f) // Rainy: Royal Blue
        condition.contains("雪") -> Color(0xFFF1F5F9).copy(alpha = 0.25f) // Snowy: Soft Slate
        condition.contains("霧") -> Color(0xFF94A3B8).copy(alpha = 0.35f) // Foggy: Slate
        condition.contains("陰") || condition.contains("多雲") -> Color(0xFF475569).copy(alpha = 0.45f) // Cloudy: Slate/Gray
        else -> Color(0xFFF59E0B).copy(alpha = 0.35f) // Sunny: Amber/Gold
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    containerColor: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val finalModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Box(
        modifier = finalModifier
            .fillMaxWidth()
            .clip(ExpressiveShape)
    ) {
        // High-end frosted glass refraction simulation using smooth multi-gradient backing.
        // This is 100% safe from RenderThread crashes on virtualized GPUs while providing rich depth & non-solid translucency.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Translucent background card with a modern, non-solid light-leaking gradient fill
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = ExpressiveShape
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                containerColor.copy(alpha = 0.55f),
                                containerColor.copy(alpha = 0.35f),
                                containerColor.copy(alpha = 0.45f)
                            )
                        )
                    )
            ) {
                Column(content = content)
            }
        }
    }
}

@Composable
fun AgendaSection(events: List<com.example.data.CalendarEvent>, condition: String, isNight: Boolean, onAuthorize: () -> Unit) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAuthorize,
        containerColor = getCardBackgroundColor(condition, isNight)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("今日行程", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            if (events.isEmpty()) {
                Text("今天沒有行程", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
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
                                color = Color.White.copy(alpha = 0.85f)
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
    sleepSyncDurationMs: Long,
    weatherUnit: String,
    isCoughColorAlertEnabled: Boolean,
    displayedNewsCount: Int,
    newsMode: String,
    isNight: Boolean,
    onSync: () -> Unit,
    onClearSync: () -> Unit,
    onAuthorize: () -> Unit,
    onNewsModeChange: (String) -> Unit,
    onNewsClick: (NewsItem) -> Unit,
    onWeatherClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isSleepSynced) {
            SleepCard(
                sleep = sleepInfo,
                condition = weather.condition,
                lastSyncTime = lastSyncTime,
                isNight = isNight,
                onReSync = onSync
            )
        } else {
            SyncHealthReminderCard(
                condition = weather.condition,
                isNight = isNight,
                onSync = onSync
            )
        }
        WeatherCard(
            weather = weather,
            condition = weather.condition,
            weatherUnit = weatherUnit,
            isNight = isNight,
            onWeatherClick = onWeatherClick
        )
        NewsCard(
            news = news,
            condition = weather.condition,
            displayedNewsCount = displayedNewsCount,
            newsMode = newsMode,
            isNight = isNight,
            onNewsModeChange = onNewsModeChange,
            onItemClick = onNewsClick
        )
    }
}

@Composable
fun SyncHealthReminderCard(
    condition: String,
    isNight: Boolean,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    GlassmorphicCard(
        modifier = modifier,
        containerColor = getCardBackgroundColor(condition, isNight)
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
                        Icons.Default.HealthAndSafety,
                        contentDescription = "Sync Health Reminder",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "健康資料尚未同步",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "同步 Google Fit 與睡眠資訊以提供專屬今日簡報",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (!isSyncing) {
                        isSyncing = true
                        coroutineScope.launch {
                            delay(1000)
                            onSync()
                            isSyncing = false
                            android.widget.Toast.makeText(context, "成功同步健康數據與 AI 分析！", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_health_button"),
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
                    Text("正在同步資料...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("立即同步健康資料", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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
    isNight: Boolean,
    onReSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val cardBg = getCardBackgroundColor(condition, isNight)

    GlassmorphicCard(
        modifier = modifier,
        containerColor = cardBg
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
                            "今日睡眠品質",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "上次同步: $lastSyncTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (!isSyncing) {
                                isSyncing = true
                                coroutineScope.launch {
                                    onReSync()
                                    isSyncing = false
                                    android.widget.Toast.makeText(context, "睡眠資料已更新！", android.widget.Toast.LENGTH_SHORT).show()
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
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Grid of sleep metrics
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                SleepMetricItem(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    icon = Icons.Default.Bedtime,
                    label = "睡眠時數",
                    value = "${sleep.hours.toInt()}h",
                    tint = Color(0xFF818CF8)
                )
            }
        }
    }
}

@Composable
fun SleepMetricItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
fun WeatherCard(
    weather: WeatherInfo,
    condition: String,
    weatherUnit: String,
    isNight: Boolean,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier,
        onClick = onWeatherClick,
        containerColor = getCardBackgroundColor(condition, isNight)
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
                    Text("最高 ${formatTemperature(weather.maxTemp, weatherUnit)} / 最低 ${formatTemperature(weather.minTemp, weatherUnit)}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
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
fun NewsCard(
    news: List<NewsItem>,
    condition: String,
    displayedNewsCount: Int,
    newsMode: String,
    isNight: Boolean,
    onNewsModeChange: (String) -> Unit,
    onItemClick: (NewsItem) -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier,
        containerColor = getCardBackgroundColor(condition, isNight)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "今日重點新聞 ($displayedNewsCount 則)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                
                // Beautiful capsules selector for Local vs International News
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("local" to "在地", "international" to "國際").forEach { (code, label) ->
                        val active = newsMode == code
                        Box(
                            modifier = Modifier
                                .background(
                                    if (active) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                                    RoundedCornerShape(50)
                                )
                                .clickable { if (!active) onNewsModeChange(code) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (active) Color.White else Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (news.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFCE93D8), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("偏好切換中，AI 精準簡報生成中...", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            } else {
                val listToRender = news.take(displayedNewsCount)
                listToRender.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (item.url.isNullOrBlank()) "點擊可開啟深度放大視窗研究" else "點擊深入探討並閱讀 Google News 來源",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (index < listToRender.size - 1) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

fun getBackgroundBrush(condition: String, isNight: Boolean = false): Brush {
    if (isNight) {
        // Deep dark blue / space black background with distinct weather undertones
        return when {
            condition.contains("雷") -> Brush.verticalGradient(
                listOf(
                    Color(0xFF0B0F19), // Midnight black
                    Color(0xFF1E1B4B), // Deep indigo
                    Color(0xFF312E81), // Dark electric purple hint
                    Color(0xFF02040A)  // Space black
                )
            )
            condition.contains("雨") -> Brush.verticalGradient(
                listOf(
                    Color(0xFF0F172A), // Slate black
                    Color(0xFF1E3A8A), // Rainy navy
                    Color(0xFF172554), // Deep wet blue
                    Color(0xFF030712)  // Void black
                )
            )
            condition.contains("雪") -> Brush.verticalGradient(
                listOf(
                    Color(0xFF1E293B), // Cool slate
                    Color(0xFF0F766E), // Arctic teal undertone
                    Color(0xFF0F172A)  // Deep midnight
                )
            )
            condition.contains("霧") || condition.contains("陰") || condition.contains("多雲") -> Brush.verticalGradient(
                listOf(
                    Color(0xFF0F172A), // Midnight gray
                    Color(0xFF334155), // Overcast slate gray
                    Color(0xFF1E293B), // Dark space blue
                    Color(0xFF02040A)  // Pure black
                )
            )
            condition == "晴朗" -> Brush.verticalGradient(
                listOf(
                    Color(0xFF0B132B), // Clear dark night sky
                    Color(0xFF1C2541), // Deep navy
                    Color(0xFF1E1B4B), // Cosmic indigo glow
                    Color(0xFF02040A)  // Space void
                )
            )
            else -> Brush.verticalGradient(
                listOf(
                    Color(0xFF0F172A), // Dark slate
                    Color(0xFF1E1B4B), // Cosmic violet tint
                    Color(0xFF030712)
                )
            )
        }
    }

    // Morning / Daytime: Starts from brilliant whites but transitions into vibrant weather tones ("早上偏白色但對應天氣")
    return when {
        condition.contains("雷") -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),                
                Color(0xFFFEF9C3), // Light cream
                Color(0xFFFDE68A), // Light amber
                Color(0xFFB45309)  // Deep amber
            )
        )
        condition.contains("雨") -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),                
                Color(0xFFFEF3C7), // Light amber
                Color(0xFFFCD34D), // Sunny yellow-amber
                Color(0xFFD97706)  // Rust-amber
            )
        )
        condition.contains("雪") -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFFFBEB), // Soft cream
                Color(0xFFFDE68A), // Light gold
                Color(0xFFD97706)  // Amber base
            )
        )
        condition.contains("霧") || condition.contains("陰") -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFEFCE8), // Creamy
                Color(0xFFFDE68A), // Golden
                Color(0xFFB45309)  // Warm brown amber
            )
        )
        condition.contains("多雲") -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFFFDF2), // Subtle warm hue
                Color(0xFFFDE68A), // Sunny light
                Color(0xFFD97706)  // Warm golden amber
            )
        )
        condition == "晴朗" -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFEF3C7),
                Color(0xFFFCD34D),
                Color(0xFFD97706)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFEF3C7),
                Color(0xFFFDE68A),
                Color(0xFFF59E0B)
            )
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
    var editNewsMode by remember { mutableStateOf(settings.newsMode) }
    var editDisplayedNewsCount by remember { mutableStateOf(settings.displayedNewsCount.toFloat()) }
    var editTimeFormat24State by remember { mutableStateOf(settings.is24HourFormat) }
    var editGeminiModelSelected by remember { mutableStateOf(settings.geminiModelSelected) }


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
                            newsMode = editNewsMode,
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
                                Text("登出並重設為訪客", color = Color.White, fontWeight = FontWeight.Bold)
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
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "設定顯示暱稱",
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
                        Text("新聞內容偏好類型", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("local" to "在地最新新聞", "international" to "國際焦點新聞").forEach { (code, label) ->
                                val selected = editNewsMode == code
                                Button(
                                    onClick = { editNewsMode = code },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) Color(0xFF3B82F6) else Color(0xFF1E293B),
                                        contentColor = if (selected) Color.White else Color(0xFF94A3B8)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("news_mode_${code}_button")
                                ) {
                                    Text(label, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

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
        shape = RoundedCornerShape(20.dp)
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

