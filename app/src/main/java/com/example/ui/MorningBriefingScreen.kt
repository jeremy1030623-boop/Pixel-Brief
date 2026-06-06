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
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.model.*
import com.example.model.WeatherInfo
import com.example.data.UserSettings
import androidx.compose.ui.draw.scale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.lazy.LazyColumn
import com.example.ui.theme.Typography
import com.example.ui.theme.*
import java.util.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.shape.CircleShape

enum class PremiumLayoutTheme(
    val themeName: String,
    val desc: String, 
    val bgGradient: List<Color>,
    val cardBg: Color,
    val cardBorderGlowColors: List<Color>,
    val accentColor: Color,
    val secondaryAccent: Color,
    val previewIcon: String
) {
    SLATE_AURORA(
        "極致夜螢",
        "深邃極夜彩螢綠",
        listOf(Color(0xFF060B18), Color(0xFF0F1833), Color(0xFF1E1B4B), Color(0xFF03050C)),
        Color(0xFF111728),
        listOf(Color(0xFF34D399).copy(alpha = 0.25f), Color(0xFF818CF8).copy(alpha = 0.08f), Color.Transparent),
        Color(0xFF34D399),
        Color(0xFF818CF8),
        "🌌"
    ),
    IMPERIAL_GOLD(
        "帝國流金",
        "皇家香檳熔岩金",
        listOf(Color(0xFF1B1105), Color(0xFF2C1E0C), Color(0xFF201305), Color(0xFF0E0802)),
        Color(0xFF251A0D),
        listOf(Color(0xFFF59E0B).copy(alpha = 0.3f), Color(0xFFFCD34D).copy(alpha = 0.08f), Color.Transparent),
        Color(0xFFF59E0B),
        Color(0xFFFCD34D),
        "👑"
    ),
    ARCTIC_MINT(
        "冰川極光",
        "冰川極光晨曦綠",
        listOf(Color(0xFF021B1B), Color(0xFF082D2D), Color(0xFF0A1E29), Color(0xFF02070A)),
        Color(0xFF0E2226),
        listOf(Color(0xFF10B981).copy(alpha = 0.28f), Color(0xFF06B6D4).copy(alpha = 0.08f), Color.Transparent),
        Color(0xFF10B981),
        Color(0xFF06B6D4),
        "❄️"
    ),
    TWILIGHT_PURPLE(
        "賽博紫幽",
        "霓虹粉紫迷幻境",
        listOf(Color(0xFF0F051D), Color(0xFF1C093A), Color(0xFF0C0315), Color(0xFF05010B)),
        Color(0xFF1D0E32),
        listOf(Color(0xFFEC4899).copy(alpha = 0.3f), Color(0xFF8B5CF6).copy(alpha = 0.08f), Color.Transparent),
        Color(0xFFEC4899),
        Color(0xFF8B5CF6),
        "🔮"
    )
}

sealed class ScreenState {
    object Home : ScreenState()
    object Settings : ScreenState()
    data class NewsDetail(val item: NewsItem) : ScreenState()
    object WeatherDetail : ScreenState()
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MorningBriefingScreen(viewModel: MorningViewModel = viewModel()) {
    val weather by viewModel.weatherInfo.collectAsState()
    val username by viewModel.username.collectAsState()
    val timeState by viewModel.timeState.collectAsState()
    val sleepInfo by viewModel.sleepInfo.collectAsState()
    val healthInfo by viewModel.healthInfo.collectAsState()
    val events by viewModel.nextEvents.collectAsState()
    val news by viewModel.newsDetail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val goalSuggestion by viewModel.goalSuggestion.collectAsState()
    val tasksList by viewModel.tasksList.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearError()
        }
    }

    val isSleepSynced = userSettings?.isSleepSynced ?: false
    val lastSyncTime = userSettings?.lastSyncTime ?: ""

    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val isNight = hour !in 6..18

    var isAutoTheme by remember { mutableStateOf(true) }
    var activePremiumTheme by remember { mutableStateOf(PremiumLayoutTheme.SLATE_AURORA) }
    
    // 自動主題機制 (Auto-Theme based on Time & Weather)
    LaunchedEffect(isAutoTheme, hour, weather.condition, isNight) {
        if (isAutoTheme) {
            val newTheme = when {
                // 雨天系 (Cozy rain / moody twilight storm)
                weather.condition in listOf("雷陣雨", "大雨", "局部陣雨", "短暫陣雨", "毛毛雨") -> {
                    if (isNight) PremiumLayoutTheme.TWILIGHT_PURPLE else PremiumLayoutTheme.SLATE_AURORA
                }
                // 晴天系 (Glowing solar dawn / golden sunset)
                weather.condition == "晴朗" -> {
                    when {
                        hour in 5..9 -> PremiumLayoutTheme.ARCTIC_MINT
                        hour in 16..18 -> PremiumLayoutTheme.IMPERIAL_GOLD
                        isNight -> PremiumLayoutTheme.TWILIGHT_PURPLE
                        else -> PremiumLayoutTheme.IMPERIAL_GOLD
                    }
                }
                // 陰雪霧多雲系 (Cozy soft overcast/snowy landscapes)
                weather.condition in listOf("陰天", "多雲", "多雲時晴", "起霧", "降雪") -> {
                    if (weather.condition == "降雪" || hour in 5..9) PremiumLayoutTheme.ARCTIC_MINT
                    else PremiumLayoutTheme.SLATE_AURORA
                }
                // 預設時辰切換
                else -> {
                    when {
                        hour in 22..23 || hour in 0..4 -> PremiumLayoutTheme.TWILIGHT_PURPLE // 深夜賽博
                        hour in 5..9 -> PremiumLayoutTheme.ARCTIC_MINT // 清晨冰川
                        hour in 16..18 -> PremiumLayoutTheme.IMPERIAL_GOLD // 黃昏流金
                        isNight -> PremiumLayoutTheme.SLATE_AURORA // 一般夜晚
                        else -> PremiumLayoutTheme.ARCTIC_MINT // 一般白天
                    }
                }
            }
            activePremiumTheme = newTheme
        }
    }

    val backgroundBrush = remember(weather.condition, isNight) { getBackgroundBrush(weather.condition, isNight) }
    var visible by remember { mutableStateOf(false) }
    var weatherVisible by remember { mutableStateOf(false) }
    var agendaVisible by remember { mutableStateOf(false) }
    var healthVisible by remember { mutableStateOf(false) }
    var newsVisible by remember { mutableStateOf(false) }
    var selectedNewsItem by remember { mutableStateOf<NewsItem?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isWeatherDetailOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val isBiometricEnabled = userSettings?.isBiometricEnabled ?: false
    var isAppUnlocked by remember { mutableStateOf(false) }

    val activity = context as? FragmentActivity
    val triggerUnlock = remember {
        {
            if (activity != null) {
                SecurityHelper.authenticate(
                    activity = activity,
                    onSuccess = {
                        isAppUnlocked = true
                    },
                    onError = { error ->
                        android.util.Log.d("SecurityLock", "Unlock error: $error")
                    }
                )
            }
        }
    }

    LaunchedEffect(userSettings) {
        val settings = userSettings
        if (settings != null && settings.isBiometricEnabled && !isAppUnlocked) {
            triggerUnlock()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isBiometricEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isBiometricEnabled) {
                    isAppUnlocked = false
                    triggerUnlock()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    LaunchedEffect(visible, isRefreshing) {
        if (visible) {
            if (isRefreshing) {
                // Dim/hide cards while refreshing so they bounce back gracefully when done
                weatherVisible = false
                agendaVisible = false
                healthVisible = false
                newsVisible = false
            } else {
                // Sequenced delays for the boot ceremony:
                // Spaced by ~200ms with custom Spring settings
                delay(100)
                weatherVisible = true
                delay(200)
                agendaVisible = true
                delay(200)
                healthVisible = true
                delay(200)
                newsVisible = true
            }
        } else {
            weatherVisible = false
            agendaVisible = false
            healthVisible = false
            newsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        if (isBiometricEnabled && !isAppUnlocked) {
            SecurityLockScreen(
                activeTheme = activePremiumTheme,
                onUnlockClick = triggerUnlock
            )
        } else {
            // Dynamic, high-fidelity atmosphere overlay reflecting current weather
            WeatherAtmosphereOverlay(condition = weather.condition, isNight = isNight)

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        val currentNewsItem = selectedNewsItem
        val screenState = remember(currentNewsItem, isSettingsOpen, isWeatherDetailOpen) {
            when {
                currentNewsItem != null -> ScreenState.NewsDetail(currentNewsItem)
                isSettingsOpen -> ScreenState.Settings
                isWeatherDetailOpen -> ScreenState.WeatherDetail
                else -> ScreenState.Home
            }
        }

        // Handle back navigation for sub-screens
        BackHandler(enabled = currentNewsItem != null || isSettingsOpen || isWeatherDetailOpen) {
            when {
                currentNewsItem != null -> selectedNewsItem = null
                isSettingsOpen -> isSettingsOpen = false
                isWeatherDetailOpen -> isWeatherDetailOpen = false
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
                is ScreenState.WeatherDetail -> {
                    WeatherDetailScreen(weather, isNight, userSettings?.weatherUnit ?: "C") {
                        isWeatherDetailOpen = false
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
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.fetchData(isManual = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(horizontal = 24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f))
                            ) {
                                GreetingSection(
                                    username = username,
                                    avatarEmoji = userSettings?.avatarEmoji ?: "🦊",
                                    avatarGradientIndex = userSettings?.avatarGradientIndex ?: 0,
                                    greeting = timeState.greeting,
                                    time = timeState.time,
                                    secondaryMessage = timeState.secondaryMessage,
                                    isInteraction = timeState.isInteraction,
                                    weather = weather,
                                    events = events,
                                    weatherUnit = userSettings?.weatherUnit ?: "C",
                                    isNight = isNight,
                                    goalSuggestion = goalSuggestion,
                                    isRefreshing = isRefreshing,
                                    isGoalSuggestionAdded = tasksList.any { it.text == goalSuggestion },
                                    onSettingsClick = { isSettingsOpen = true },
                                    onAddTask = { text -> viewModel.addTask(text) },
                                    onProfileChange = { newName, newEmoji, newGradientIndex ->
                                        val current = userSettings ?: UserSettings()
                                        viewModel.updateUserSettings(
                                            current.copy(
                                                username = newName,
                                                avatarEmoji = newEmoji,
                                                avatarGradientIndex = newGradientIndex
                                            )
                                        )
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(animationSpec = spring(dampingRatio = 0.75f, stiffness = 200f)) + expandVertically(animationSpec = spring(dampingRatio = 0.75f, stiffness = 200f))
                            ) {
                                DailyGoalsCard(
                                    tasks = tasksList,
                                    isNight = isNight,
                                    activeTheme = activePremiumTheme,
                                    onAddTask = { text -> viewModel.addTask(text) },
                                    onToggleTask = { task -> viewModel.toggleTask(task) },
                                    onDeleteTask = { task -> viewModel.deleteTask(task) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            AnimatedVisibility(
                                visible = agendaVisible,
                                enter = fadeIn(animationSpec = spring(dampingRatio = 0.65f, stiffness = 150f)) + slideInVertically(animationSpec = spring(dampingRatio = 0.65f, stiffness = 150f)) { it / 3 }
                            ) {
                                AgendaSection(events, weather.condition, isNight, activePremiumTheme) {
                                    checkAndRequestCalendarPermission()
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
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
                                activeTheme = activePremiumTheme,
                                healthVisible = healthVisible,
                                weatherVisible = weatherVisible,
                                newsVisible = newsVisible,
                                onSync = { viewModel.syncHealthData() },
                                onClearSync = { viewModel.clearSleepData() },
                                onAuthorize = { checkAndRequestCalendarPermission() },
                                onNewsModeChange = { newMode ->
                                    val current = userSettings ?: UserSettings()
                                    viewModel.updateUserSettings(current.copy(newsMode = newMode))
                                },
                                onNewsClick = { item -> selectedNewsItem = item },
                                onWeatherClick = { 
                                    isWeatherDetailOpen = true
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }
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
            .background(if (isNight) Color(0xFF0F172A) else Color(0xFFF8FAFC))
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
                    Text("閱讀完整即時新聞報導", fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB so it doesn't overlap text
        }
        
        FloatingActionButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 32.dp),
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
    }
}

@Composable
fun WeatherDetailScreen(weather: WeatherInfo, isNight: Boolean, weatherUnit: String = "C", onBack: () -> Unit) {
    val hourlyData = remember(weather) {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val list = mutableListOf<Pair<String, Int>>()
        for (i in 0..11) {
            val h = (currentHour + i) % 24
            val timeStr = String.format("%02d:00", h)
            val angle = (h - 15) * Math.PI / 12
            val range = (weather.maxTemp - weather.minTemp).coerceAtLeast(4)
            val mid = (weather.maxTemp + weather.minTemp) / 2.0
            val modelTemp = (mid + Math.cos(angle) * (range / 2.0)).toInt()
            
            val temp = if (i == 0) {
                weather.currentTemp
            } else {
                val alpha = (i / 11.0).toFloat().coerceIn(0f, 1f)
                (weather.currentTemp * (1f - alpha) + modelTemp * alpha).toInt()
            }
            list.add(timeStr to temp)
        }
        list
    }

    val warnings = remember(weather) {
        val list = mutableListOf<String>()
        if (weather.precipitationProb >= 50) {
            list.add("⚠️ 降雨機率高（${weather.precipitationProb}%）：出門請記得攜帶雨具！")
        } else if (weather.precipitationProb >= 30) {
            list.add("☁️ 有局部短暫雨機會（${weather.precipitationProb}%）：建議隨身攜帶折疊傘。")
        }
        
        if (weather.uvIndex >= 6.0f) {
            list.add("☀️ 紫外線指數偏高（${weather.uvIndex}）：外出請注意防曬、配戴墨鏡，並定時補充水分。")
        }
        
        if (weather.maxTemp - weather.minTemp >= 8) {
            list.add("🌡️ 溫差較大（達 ${weather.maxTemp - weather.minTemp}°C）：早晚偏涼，請採取洋蔥式穿法。")
        }
        
        if (weather.maxTemp >= 32) {
            list.add("🥵 高溫警報（最高溫 ${weather.maxTemp}°C）：天氣炎熱，請嚴防中暑，多待在陰涼通風處。")
        } else if (weather.minTemp <= 15) {
            list.add("🥶 低溫注意（最低溫 ${weather.minTemp}°C）：天氣寒冷，請做好保暖禦寒修護。")
        }
        
        if (weather.windSpeed >= 15f) {
            list.add("💨 風力強勁（時速 ${weather.windSpeed} km/h）：風速較大，外出注意強風，小心掉落物。")
        }
        
        if (list.isEmpty()) {
            list.add("✨ 今日天氣狀況良好，適合外出走走！")
        }
        list
    }
    
    val clothingRecommend = remember(weather) {
        val avgTemp = (weather.maxTemp + weather.minTemp) / 2
        val list = mutableListOf<String>()
        
        when {
            avgTemp >= 28 -> {
                list.add("👕 精選短袖 T-Shirt / 背心、短褲、涼鞋或透氣運動鞋。")
                list.add("🕶️ 外出可加一邊薄防曬外套或戴遮陽帽。")
                list.add("🧵 推薦材質：純棉、亞麻等吸濕排汗的涼爽布料。")
            }
            avgTemp in 22..27 -> {
                list.add("👕 薄長袖、襯衫、短袖外搭薄款針織外套或防風外套。")
                list.add("👖 搭配休閒長褲、牛仔褲與運動鞋。")
                list.add("🧵 面對室內冷氣房，洋蔥式穿搭（短袖 + 卡迪根）是完美的選擇。")
            }
            avgTemp in 16..21 -> {
                list.add("🧥 長袖衛衣、毛衣，或休閒西裝外套。")
                list.add("🧣 風衣、夾克、中等厚度的外套，適合早晚加穿。")
                list.add("👖 燈芯絨褲或稍微防風、偏厚的休閒長褲。")
            }
            else -> {
                list.add("🧤 加厚毛衣、發熱衣作打底。")
                list.add("🧥 大衣、羽絨外套、防風防雨衝鋒衣，提供完美的禦寒保暖。")
                list.add("🧣 可搭配圍巾、手套或毛帽防止熱量流失。")
            }
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isNight) Color(0xFF0F172A) else Color(0xFFF8FAFC))
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "天氣小工具",
                    tint = if (isNight) AuroraMint else Color(0xFF0F172A),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "天氣小工具說明",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isNight) Color.White else Color(0xFF0F172A)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "目前定位城市：${weather.locationName} (${weather.condition})",
                style = MaterialTheme.typography.titleMedium,
                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Text(
                "今日氣溫：${weather.minTemp}°C ~ ${weather.maxTemp}°C  (體感 ${weather.apparentTemp}°C)",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isNight) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            
            Spacer(modifier = Modifier.height(28.dp))

            // Section: Temperature Trend (Next 12 Hours)
            Text(
                "• 氣溫趨勢 (未來 12 小時)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isNight) AuroraMint else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNight) Color(0xFF151B26) else Color(0xFFF1F5F9)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TemperatureTrendLineChart(
                        data = hourlyData,
                        weatherUnit = weatherUnit,
                        isDark = isNight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Section: 今日關注
            Text(
                "• 今日關注",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isNight) AuroraMint else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNight) Color(0xFF151B26) else Color(0xFFF1F5F9)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    warnings.forEachIndexed { index, warning ->
                        Text(
                            warning,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isNight) Color.White else Color(0xFF334155)
                        )
                        if (index < warnings.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Section: 今日穿搭
            Text(
                "· 今日穿搭",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isNight) AuroraMint else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNight) Color(0xFF151B26) else Color(0xFFF1F5F9)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    clothingRecommend.forEachIndexed { index, recommendation ->
                        Text(
                            recommendation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isNight) Color.White else Color(0xFF334155)
                        )
                        if (index < clothingRecommend.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
        
        FloatingActionButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 32.dp).testTag("weather_detail_back_button"),
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            shape = MaterialTheme.shapes.large
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
    secondaryMessage: String,
    isInteraction: Boolean,
    weather: WeatherInfo,
    events: List<com.example.data.CalendarEvent>,
    weatherUnit: String,
    isNight: Boolean,
    goalSuggestion: String,
    isRefreshing: Boolean,
    isGoalSuggestionAdded: Boolean,
    onSettingsClick: () -> Unit,
    onAddTask: (String) -> Unit,
    onProfileChange: (name: String, emoji: String, gradientIndex: Int) -> Unit
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Crossfade(
                        targetState = secondaryMessage,
                        label = "greeting_secondary"
                    ) { targetMsg ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            val (icon, tint) = if (isInteraction) {
                                Pair(Icons.Default.AutoAwesome, AuroraMint)
                            } else {
                                if (isNight) {
                                    Pair(Icons.Default.Bedtime, Color(0xFFC084FC)) // Soft lavender purple moon
                                } else {
                                    Pair(Icons.Default.WbSunny, Color(0xFFFBBF24)) // Radiant golden gold sun
                                }
                            }
                            
                            Icon(
                                imageVector = icon,
                                contentDescription = "氣氛圖示",
                                tint = tint,
                                modifier = Modifier.size(14.dp)
                            )
                            
                            Text(
                                text = targetMsg,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isInteraction) FontWeight.SemiBold else FontWeight.Normal,
                                    letterSpacing = 0.2.sp
                                ),
                                color = if (isInteraction) tint else {
                                    if (isNight) Color.White.copy(alpha = 0.75f) else Color(0xFF475569)
                                },
                                maxLines = 2
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "（自訂：點擊或長按頭像即可自訂專屬名稱與頭像 ✨）",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isNight) Color.White.copy(alpha = 0.35f) else Color(0xFF1E293B).copy(alpha = 0.35f),
                        fontSize = 10.sp
                    )
                }
            }
            
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "設定",
                    tint = if (isNight) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TimeGreetingText(time, eventText, weather, weatherUnit, isNight)

        if (goalSuggestion.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            GoalSuggestionCard(
                goalSuggestion = goalSuggestion,
                isNight = isNight,
                isTaskAdded = isGoalSuggestionAdded,
                onAddTask = onAddTask
            )
        }
    }
}

@Composable
fun GoalSuggestionCard(
    goalSuggestion: String,
    isNight: Boolean,
    isTaskAdded: Boolean,
    onAddTask: (String) -> Unit
) {
    if (goalSuggestion.isBlank()) return

    // Setup an infinite transition for subtle, beautiful interactive animation (pulsing glow/size of the lightbulb)
    val infiniteTransition = rememberInfiniteTransition(label = "goal_icon_glow")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val iconShadowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadowAlpha"
    )

    // Layout colors based on isNight
    val containerColor = if (isNight) {
        Color(0xFF1E293B).copy(alpha = 0.85f) // Rich celestial twilight dark indigo slate
    } else {
        Color(0xFFF1F5F9).copy(alpha = 0.95f) // Soft cool light mineral slate
    }

    val primaryTextColor = if (isNight) Color.White else Color(0xFF1E293B)
    val secondaryTextColor = if (isNight) Color.White.copy(alpha = 0.8f) else Color(0xFF334155)
    val borderBrush = if (isNight) {
        Brush.linearGradient(listOf(AuroraMint.copy(alpha = 0.5f), AuroraLavender.copy(alpha = 0.3f)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF10B981).copy(alpha = 0.4f), Color(0xFF3B82F6).copy(alpha = 0.3f)))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderBrush),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Glow animated lightbulb container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isNight) Color(0xFF065F46).copy(alpha = 0.35f)
                            else Color(0xFFD1FAE5)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawCircle(
                            color = if (isNight) AuroraMint.copy(alpha = iconShadowAlpha) else Color(0xFF10B981).copy(alpha = iconShadowAlpha * 0.4f),
                            radius = size.width * 0.42f * iconScale
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "💡",
                        tint = if (isNight) AuroraMint else Color(0xFF047857),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "DAILY INSPIRATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = if (isNight) AuroraMint.copy(alpha = 0.8f) else Color(0xFF047857)
                    )
                    Text(
                        text = "💡 今日智慧生活目標",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = primaryTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body suggestion text
            Text(
                text = goalSuggestion,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = secondaryTextColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action button to accept challenge and set as a Task
            Button(
                onClick = {
                    if (!isTaskAdded) {
                        onAddTask(goalSuggestion)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = !isTaskAdded,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNight) AuroraMint else Color(0xFF059669),
                    contentColor = if (isNight) Color(0xFF0F172A) else Color.White,
                    disabledContainerColor = if (isNight) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFE2E8F0),
                    disabledContentColor = if (isNight) Color.White.copy(alpha = 0.4f) else Color(0xFF94A3B8)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accept_daily_goal_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isTaskAdded) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTaskAdded) "已加入今日待辦清單 🎉" else "🌟 接受並設為今日待辦挑戰",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DailyGoalsCard(
    tasks: List<com.example.data.TaskItem>,
    isNight: Boolean,
    activeTheme: PremiumLayoutTheme,
    onAddTask: (String) -> Unit,
    onToggleTask: (com.example.data.TaskItem) -> Unit,
    onDeleteTask: (com.example.data.TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = tasks.size
    val completedCount = tasks.count { it.isCompleted }
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 100f),
        label = "daily_goals_progress"
    )

    var newGoalText by remember { mutableStateOf("") }

    GlassmorphicCard(
        modifier = modifier,
        containerColor = activeTheme.cardBg,
        borderColors = activeTheme.cardBorderGlowColors,
        glowColor = activeTheme.accentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with visual icon & title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(activeTheme.accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Goals Icon",
                        tint = activeTheme.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "🎯 今日生活目標",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                    
                    if (totalCount > 0) {
                        val progressMessage = when {
                            completedCount == totalCount -> "太棒了！已完成所有今日目標！🌟"
                            completedCount > 0 -> "加油！已完成 $completedCount / $totalCount 個目標！💪"
                            else -> "加油！今天還有 $totalCount 個目標等待挑戰！🚀"
                        }
                        Text(
                            text = progressMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Beautiful progress line if there is any task
            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "完成進度",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = activeTheme.accentColor
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = activeTheme.accentColor,
                    trackColor = activeTheme.secondaryAccent.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goals list section
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "無目標",
                            tint = activeTheme.accentColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "今日尚未建立生活目標",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "可接受上方的「今日智慧生活目標」\n或在下方新增自訂生活目標！🌟",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tasks.forEach { item ->
                        key(item.id) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (item.isCompleted) Color.White.copy(alpha = 0.04f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .clickable { onToggleTask(item) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("goal_item_${item.id}")
                            ) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { onToggleTask(item) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = activeTheme.accentColor,
                                        checkmarkColor = AuroraMidnight,
                                        uncheckedColor = Color.White.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("goal_checkbox_${item.id}")
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium
                                    ),
                                    color = if (item.isCompleted) Color.White.copy(alpha = 0.45f) else Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(
                                    onClick = { onDeleteTask(item) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("delete_goal_button_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "刪除目標",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive in-place input row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newGoalText,
                    onValueChange = { newGoalText = it },
                    placeholder = {
                        Text(
                            "新增自訂生活目標...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("add_custom_goal_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = activeTheme.accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.02f),
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                IconButton(
                    onClick = {
                        if (newGoalText.isNotBlank()) {
                            onAddTask(newGoalText.trim())
                            newGoalText = ""
                        }
                    },
                    enabled = newGoalText.isNotBlank(),
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (newGoalText.isNotBlank()) activeTheme.accentColor
                            else Color.White.copy(alpha = 0.1f)
                        )
                        .testTag("add_custom_goal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新增目標",
                        tint = if (newGoalText.isNotBlank()) AuroraMidnight else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TimeGreetingText(
    time: String,
    eventText: String,
    weather: WeatherInfo,
    weatherUnit: String,
    isNight: Boolean
) {
    val mainBrief = remember(time, eventText, weather, weatherUnit, isNight) {
        "現在時間 $time，今天天氣狀況 ${weather.condition}，目前 ${formatTemperature(weather.currentTemp, weatherUnit)}°，今天最高溫 ${formatTemperature(weather.maxTemp, weatherUnit)}°；最低溫 ${formatTemperature(weather.minTemp, weatherUnit)}°。$eventText"
    }

    Column {
        Text(
            text = mainBrief,
            style = MaterialTheme.typography.titleLarge.copy(lineHeight = 34.sp),
            color = if (isNight) Color.White else Color(0xFF1E293B),
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }
}

val ExpressiveShape = RoundedCornerShape(32.dp)

fun getCardBackgroundColor(condition: String, isNight: Boolean = false): Color {
    if (!isNight) {
        // High-end, premium sapphire-indigo and misty ocean-slate translucent cards to ensure white text is perfectly legible
        return when {
            condition.contains("雷") -> Color(0xFF2E2A4F).copy(alpha = 0.82f) // Stormy: Mystic electric indigo
            condition.contains("雨") -> Color(0xFF1E3A5F).copy(alpha = 0.82f) // Rainy: Deep ocean navy
            condition.contains("雪") -> Color(0xFF2C4C5E).copy(alpha = 0.82f) // Snowy: Polar blue-grey
            condition.contains("霧") || condition.contains("陰") -> Color(0xFF374151).copy(alpha = 0.82f) // Overcast/Mist: Clean charcoal slate
            condition.contains("多雲") -> Color(0xFF3B3B5E).copy(alpha = 0.82f) // Cloudy: Sophisticated slate-indigo
            condition == "晴朗" -> Color(0xFF0F2C59).copy(alpha = 0.82f) // Clear: Luxurious celestial navy
            else -> Color(0xFF1E293B).copy(alpha = 0.82f) // Deep slate
        }
    }

    return when {
        condition.contains("雷") -> Color(0xFF1B1838) // Stormy night: Deep celestial indigo purple
        condition.contains("雨") -> Color(0xFF0D1C30) // Rainy night: Cool damp ocean dark navy
        condition.contains("雪") -> Color(0xFF1E2633) // Snowy night: Soft frosted dark slate
        condition.contains("霧") || condition.contains("陰") || condition.contains("多雲") -> Color(0xFF151924) // Cloudy/Foggy night: Cohesive dark slate gray
        else -> Color(0xFF121829) // Clear/Sunny night (晴朗): Elegant obsidian/celestial navy
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    containerColor: Color,
    borderColors: List<Color>? = null,
    glowColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val finalModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Card(
        modifier = finalModifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun AgendaSection(events: List<com.example.data.CalendarEvent>, condition: String, isNight: Boolean, activeTheme: PremiumLayoutTheme, onAuthorize: () -> Unit) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAuthorize,
        containerColor = activeTheme.cardBg,
        borderColors = activeTheme.cardBorderGlowColors,
        glowColor = activeTheme.accentColor
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
                            shape = MaterialTheme.shapes.small,
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
    activeTheme: PremiumLayoutTheme,
    healthVisible: Boolean,
    weatherVisible: Boolean,
    newsVisible: Boolean,
    onSync: () -> Unit,
    onClearSync: () -> Unit,
    onAuthorize: () -> Unit,
    onNewsModeChange: (String) -> Unit,
    onNewsClick: (NewsItem) -> Unit,
    onWeatherClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedVisibility(
            visible = healthVisible,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.65f, stiffness = 150f)) + slideInVertically(animationSpec = spring(dampingRatio = 0.65f, stiffness = 150f)) { it / 3 }
        ) {
            if (isSleepSynced) {
                SleepCard(
                    sleep = sleepInfo,
                    condition = weather.condition,
                    lastSyncTime = lastSyncTime,
                    isNight = isNight,
                    activeTheme = activeTheme,
                    onReSync = onSync
                )
            } else {
                SyncHealthReminderCard(
                    condition = weather.condition,
                    isNight = isNight,
                    activeTheme = activeTheme,
                    onSync = onSync
                )
            }
        }

        AnimatedVisibility(
            visible = weatherVisible,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.65f, stiffness = 150f)) + slideInVertically(animationSpec = spring(dampingRatio = 0.65f, stiffness = 150f)) { it / 3 }
        ) {
            WeatherCard(
                weather = weather,
                condition = weather.condition,
                weatherUnit = weatherUnit,
                isNight = isNight,
                activeTheme = activeTheme,
                onWeatherClick = onWeatherClick
            )
        }

        AnimatedVisibility(
            visible = newsVisible,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.65f, stiffness = 150f)) + slideInVertically(animationSpec = spring(dampingRatio = 0.65f, stiffness = 150f)) { it / 3 }
        ) {
            NewsCard(
                news = news,
                condition = weather.condition,
                displayedNewsCount = displayedNewsCount,
                newsMode = newsMode,
                isNight = isNight,
                activeTheme = activeTheme,
                onNewsModeChange = onNewsModeChange,
                onItemClick = onNewsClick
            )
        }
    }
}

@Composable
fun SyncHealthReminderCard(
    condition: String,
    isNight: Boolean,
    activeTheme: PremiumLayoutTheme,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    GlassmorphicCard(
        modifier = modifier,
        containerColor = activeTheme.cardBg,
        borderColors = activeTheme.cardBorderGlowColors,
        glowColor = activeTheme.accentColor
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
                        .background(activeTheme.accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.HealthAndSafety,
                        contentDescription = "Sync Health Reminder",
                        tint = AuroraMidnight,
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
                    containerColor = activeTheme.accentColor,
                    contentColor = AuroraMidnight
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
    activeTheme: PremiumLayoutTheme,
    onReSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    GlassmorphicCard(
        modifier = modifier,
        containerColor = activeTheme.cardBg,
        borderColors = activeTheme.cardBorderGlowColors,
        glowColor = activeTheme.accentColor
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
                            .clip(MaterialTheme.shapes.small)
                            .background(activeTheme.accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = AuroraMidnight,
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SleepMetricItem(
                    icon = Icons.Default.Bedtime,
                    label = "睡眠時數",
                    value = "${"%.1f".format(sleep.hours)}h",
                    tint = activeTheme.accentColor
                )
                SleepMetricItem(
                    icon = Icons.Default.Air,
                    label = "打鼾",
                    value = "${sleep.snoringMinutes}m",
                    tint = activeTheme.secondaryAccent
                )
                SleepMetricItem(
                    icon = Icons.Default.Sick,
                    label = "咳嗽",
                    value = "${sleep.coughCount}次",
                    tint = Color(0xFFF87171)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "健康報告建議",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "您的健康狀況分析與建議將顯示於此。",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
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
    activeTheme: PremiumLayoutTheme,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier,
        onClick = onWeatherClick,
        containerColor = activeTheme.cardBg,
        borderColors = activeTheme.cardBorderGlowColors,
        glowColor = activeTheme.accentColor
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeatherAnimatedIcon(
                    condition = weather.condition,
                    isNight = isNight,
                    modifier = Modifier.size(52.dp)
                )
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
    activeTheme: PremiumLayoutTheme,
    onNewsModeChange: (String) -> Unit,
    onItemClick: (NewsItem) -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier,
        containerColor = activeTheme.cardBg,
        borderColors = activeTheme.cardBorderGlowColors,
        glowColor = activeTheme.accentColor
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "今日重點新聞",
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

fun getWeatherSolidColor(weather: WeatherInfo, isNight: Boolean): Color {
    // 1. Initial base color selection based on weather condition
    val baseColor = if (isNight) {
        when {
            weather.condition.contains("雷") -> Color(0xFF0E0E1B) // Deep stormy indigo
            weather.condition.contains("雨") -> Color(0xFF0A111E) // Deep rainy navy
            weather.condition.contains("雪") -> Color(0xFF131B26) // Icy midnight slate
            weather.condition.contains("霧") || weather.condition.contains("陰") -> Color(0xFF0E1117) // Darkest overcast slate
            weather.condition.contains("多雲") -> Color(0xFF0D111D) // Multi-cloud midnight
            weather.condition == "晴朗" -> Color(0xFF070C18) // Absolute deep celestial navy
            else -> Color(0xFF0B0F19) // Elegant midnight black
        }
    } else {
        when {
            weather.condition.contains("雷") -> Color(0xFFD1D5DB) // Soft slate grey
            weather.condition.contains("雨") -> Color(0xFFE4E9F2) // Rainy light-blue-grey
            weather.condition.contains("雪") -> Color(0xFFFFFFFF) // Pristine winter white
            weather.condition.contains("霧") || weather.condition.contains("陰") -> Color(0xFFE2E8F0) // Mist/Overcast zinc
            weather.condition.contains("多雲") -> Color(0xFFEFF4FC) // Airy light grey-blue
            weather.condition == "晴朗" -> Color(0xFFFFF7ED) // Vibrant sunny peach white
            else -> Color(0xFFF8FAFC) // Minimal slate white
        }
    }

    // Convert to float color components for precise mathematical adjustment
    var r = baseColor.red
    var g = baseColor.green
    var b = baseColor.blue

    // 2. Adjust for Temperature (currentTemp: typically 0 to 45)
    // Cold: increase blue/cyan. Hot: increase red/yellow.
    val tempNormalized = ((weather.currentTemp - 15f) / 25f).coerceIn(-1.0f, 1.0f) // -1 is icy, +1 is hot
    if (tempNormalized > 0) {
        // Warm/Hot: subtle shift towards amber/red (more red, less blue)
        if (isNight) {
            r += tempNormalized * 0.04f
            b -= tempNormalized * 0.02f
        } else {
            r += tempNormalized * 0.03f
            g += tempNormalized * 0.015f
            b -= tempNormalized * 0.03f
        }
    } else {
        // Cold/Freezing: shift towards ice-blue (more blue, less red)
        val coldFactor = -tempNormalized
        if (isNight) {
            r -= coldFactor * 0.02f
            b += coldFactor * 0.04f
        } else {
            r -= coldFactor * 0.03f
            g += coldFactor * 0.01f
            b += coldFactor * 0.03f
        }
    }

    // 3. Adjust for Precipitation Probability (precipitationProb: 0 to 100)
    // Higher rain chance: desaturate/grey-out and lower luminance (darker)
    val rainFactor = (weather.precipitationProb / 100f).coerceIn(0f, 1f)
    if (rainFactor > 0.1f) {
        if (isNight) {
            // Darker, slight deep navy tint
            r = r * (1f - rainFactor * 0.2f)
            g = g * (1f - rainFactor * 0.15f)
            b = b * (1f - rainFactor * 0.1f) + rainFactor * 0.02f
        } else {
            // Muted, less vibrant, slaty gray tint
            r = r * (1f - rainFactor * 0.1f)
            g = g * (1f - rainFactor * 0.08f)
            b = b * (1f - rainFactor * 0.05f) + rainFactor * 0.03f
        }
    }

    // 4. Adjust for UV Index (uvIndex: typically 0 to 12)
    // High UV (exclusive to day or moonlit bright night): adds warm, energetic intensity tint
    val uvNormalized = (weather.uvIndex / 11f).coerceIn(0f, 1f)
    if (uvNormalized > 0.1f) {
        if (!isNight) {
            // Vibrant gold/yellow hint
            r += uvNormalized * 0.04f
            g += uvNormalized * 0.02f
            b -= uvNormalized * 0.01f
        } else {
            // Stellar silver hint
            r += uvNormalized * 0.01f
            g += uvNormalized * 0.01f
            b += uvNormalized * 0.02f
        }
    }

    // 5. Adjust for Wind Speed (windSpeed: 0 to 45 km/h)
    // High wind speed: add fresh breeze, high-contrast cool steel tint
    val windFactor = (weather.windSpeed / 30f).coerceIn(0f, 1f)
    if (windFactor > 0.1f) {
        r -= windFactor * 0.015f
        g += windFactor * 0.01f
        b += windFactor * 0.025f
    }

    // 6. Adjust for Humidity (humidity: 0 to 100)
    // Extremely humid: shift slightly towards deep mist forest/emerald tint; Dry: crisp clean tones
    val humidityFactor = (weather.humidity / 100f).coerceIn(0f, 1f)
    if (humidityFactor > 0.7f) {
        val extraHumid = (humidityFactor - 0.7f) / 0.3f
        // Mildly shift towards emerald-cyan
        g += extraHumid * 0.015f
        r -= extraHumid * 0.01f
    }

    // Ensure components remain inside valid [0f, 1f] range
    return Color(
        red = r.coerceIn(0f, 1f),
        green = g.coerceIn(0f, 1f),
        blue = b.coerceIn(0f, 1f),
        alpha = baseColor.alpha
    )
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
                Color(0xFFE4E4E7), // Light mist grey
                Color(0xFFC7D2FE), // Pale electric lavender-blue
                Color(0xFF93C5FD)  // Airy pale sky
            )
        )
        condition.contains("雨") -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),                
                Color(0xFFF0F9FF), // Extremely light blue
                Color(0xFFE0F2FE), // Soft misty blue
                Color(0xFFBAE6FD)  // Fresh light sky rain
            )
        )
        condition.contains("雪") -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF1F5F9), // Ice-white
                Color(0xFFE0F2FE), // Cool cyan-white glow
                Color(0xFFE2E8F0)  // Nordic soft slate
            )
        )
        condition.contains("霧") || condition.contains("陰") -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF4F4F5), // Diffused light grey
                Color(0xFFE4E4E7), // Elegant misty zinc
                Color(0xFFD4D4D8)  // Soft cloud mist
            )
        )
        condition.contains("多雲") -> Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC), // Air-light white
                Color(0xFFEFF6FF), // Soft morning light blue
                Color(0xFFDBEAFE)  // Sweet lavender-tinged pale indigo
            )
        )
        condition == "晴朗" -> Brush.verticalGradient(
            listOf(
                Color(0xFFBAE6FD), // Glowing light sky blue
                Color(0xFFE0F2FE), // Soft azure transition
                Color(0xFFFEF3C7), // Warm golden sunrise sunbeams
                Color(0xFFFFFBEB)  // Premium morning cream white
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color(0xFFE0F2FE), // Soft sky blue
                Color(0xFFEFF6FF), // Airy clouds
                Color(0xFFFEF3C7), // Gentle sunrise rays
                Color(0xFFFFFDF5)  // Clean white sand
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
fun SettingsScreen(
    settings: UserSettings,
    onBack: () -> Unit,
    onSave: (UserSettings) -> Unit
) {
    // Local copy of editable options state
    var editUsername by remember { mutableStateOf(settings.username) }
    var editDefaultCity by remember { mutableStateOf(settings.defaultCity) }
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
    var editIsBiometricEnabled by remember { mutableStateOf(settings.isBiometricEnabled) }


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
                            defaultCity = editDefaultCity,
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
                            geminiModelSelected = editGeminiModelSelected,
                            isBiometricEnabled = editIsBiometricEnabled
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
                        
                        Text(
                            "備用/預設城市",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "當 GPS 定位失敗、未取得授權或不支援時，系統將使用此城市作為預設氣象與在地新聞依據",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editDefaultCity,
                            onValueChange = { editDefaultCity = it },
                            placeholder = { Text("例如：高雄、花蓮、台中") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("default_city_settings_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            singleLine = true
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
                            listOf(
                                "gemini-1.5-flash" to "1.5 Flash",
                                "gemini-1.5-pro" to "1.5 Pro",
                                "aicore" to "AICore"
                            ).forEach { (code, label) ->
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
                                    Text(label, fontWeight = FontWeight.Bold, maxLines = 1)
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

                // Section 7: 隱私與生物辨識防護 (Privacy & Biometrics)
                item {
                    SettingsSectionCard(title = "七、隱私與生物辨識防護") {
                        val biometricAvailable = remember { SecurityHelper.isBiometricAvailable(context) }
                        
                        SettingsRow(
                            label = "啟用安全鎖 (指紋、面孔或裝置密碼)",
                            description = if (biometricAvailable) {
                                "開啟後，每次開啟 App 或自背景返回時都會進行系統安全解鎖 verify，防護個人隱私與昨夜睡眠等敏感醫療與日程行程資訊安全"
                            } else {
                                "您的裝置不支援或尚未在系統設定中登記指紋/面部等密碼鎖 🔒"
                            },
                            checked = editIsBiometricEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (biometricAvailable) {
                                        val act = context as? FragmentActivity
                                        if (act != null) {
                                            SecurityHelper.authenticate(
                                                activity = act,
                                                title = "驗證解鎖設定",
                                                subtitle = "請感應一次生物識能 verify，核配解鎖管道能正常運作",
                                                onSuccess = {
                                                    editIsBiometricEnabled = true
                                                    android.widget.Toast.makeText(context, "防護鎖設定連結成功！已安全守護 🛡️", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    android.widget.Toast.makeText(context, "安全鎖未配對成功：$err", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            editIsBiometricEnabled = true
                                        }
                                    } else {
                                        android.widget.Toast.makeText(context, "裝置尚未登記任何安全螢幕鎖或不支持生物辨識裝置 🔒", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    editIsBiometricEnabled = false
                                }
                            },
                            testTag = "biometric_settings_switch"
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
        shape = MaterialTheme.shapes.medium
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

@Composable
fun WeatherAtmosphereOverlay(
    condition: String,
    isNight: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_anim")
    
    when (condition) {
        "雷陣雨", "大雨", "局部陣雨", "短暫陣雨", "毛毛雨" -> {
            // Animated Rain Drops & Glass Ripple Rings
            val fallProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rain_fall"
            )
            
            val rippleScale by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ripple_expand"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                if (width <= 0 || height <= 0) return@Canvas
                
                // Draw slanted rain needles
                val rainCount = 20
                for (i in 0 until rainCount) {
                    val xSeed = (i * 137.5f) % width
                    val ySeed = (i * 243.7f) % height
                    
                    val currentY = (ySeed + fallProgress * height) % height
                    val currentX = (xSeed + fallProgress * 150f) % width
                    
                    drawLine(
                        color = Color(0x1E818CF8),
                        start = androidx.compose.ui.geometry.Offset(currentX, currentY),
                        end = androidx.compose.ui.geometry.Offset(currentX + 10f, currentY + 30f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
                
                // Draw dynamic splash ripples on simulated surface glass
                val splashPoints = listOf(
                    androidx.compose.ui.geometry.Offset(width * 0.25f, height * 0.45f),
                    androidx.compose.ui.geometry.Offset(width * 0.75f, height * 0.25f),
                    androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.75f)
                )
                
                splashPoints.forEachIndexed { index, point ->
                    val progress = (rippleScale + index * 0.33f) % 1f
                    val alpha = (1f - progress) * 0.12f
                    val radius = progress * 50.dp.toPx()
                    
                    drawCircle(
                        color = Color(0xFFC084FC).copy(alpha = alpha),
                        radius = radius,
                        center = point,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
        "晴朗" -> {
            // Elegant pulsing light rays (Sun beams) from top-right
            val sunGlow by infiniteTransition.animateFloat(
                initialValue = 0.12f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sunny_glow"
            )
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                if (width <= 0 || height <= 0) return@Canvas
                
                // Pulsing solar source at top right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFBBF24).copy(alpha = sunGlow),
                            Color(0xFFFCD34D).copy(alpha = sunGlow * 0.3f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(width, 0f),
                        radius = width * 0.9f
                    ),
                    radius = width * 0.9f,
                    center = androidx.compose.ui.geometry.Offset(width, 0f)
                )
            }
        }
        "陰天", "多雲", "多雲時晴", "起霧" -> {
            // Drifting mist cloud particles
            val driftProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(28000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "mist_drift"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                if (width <= 0 || height <= 0) return@Canvas
                
                val cloudCount = 3
                for (i in 0 until cloudCount) {
                    val basePercentY = 0.2f + i * 0.25f
                    val x = ((driftProgress + i.toFloat() * 0.33f) % 1f) * (width + 300.dp.toPx()) - 150.dp.toPx()
                    val radius = (100 + 35 * i).dp.toPx()
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.03f),
                                Color.White.copy(alpha = 0.01f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(x, height * basePercentY),
                            radius = radius
                        ),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(x, height * basePercentY)
                    )
                }
            }
        }
        "降雪" -> {
            // Elegant fluttering snow
            val snowFall by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "snow_fall"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                if (width <= 0 || height <= 0) return@Canvas
                
                val flakeCount = 15
                for (i in 0 until flakeCount) {
                    val xSeed = (i * 153.3f) % width
                    val ySeed = (i * 261.2f) % height
                    
                    val currentY = (ySeed + snowFall * height) % height
                    // side sway using a sine curve
                    val sway = kotlin.math.sin(snowFall * 3.14159f * 2f + i.toFloat()) * 15.dp.toPx()
                    val currentX = (xSeed + sway) % width
                    
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = (2.dp + (i % 3).dp).toPx(),
                        center = androidx.compose.ui.geometry.Offset(currentX, currentY)
                    )
                }
            }
        }
    }
}

// Recharts-inspired gorgeous Temperature Trend Line Chart in Native Compose
@Composable
fun TemperatureTrendLineChart(
    data: List<Pair<String, Int>>,
    weatherUnit: String,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val scrollState = rememberScrollState()

    // Comfortable width per data point to ensure clear text and smooth line curves
    val itemWidth = 72.dp
    val totalWidth = itemWidth * data.size

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .height(200.dp)
                    .padding(vertical = 12.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                if (canvasWidth <= 0 || canvasHeight <= 0 || data.isEmpty()) return@Canvas

                val leftOffset = 45f
                val rightOffset = 45f
                val topOffset = 45f
                val bottomOffset = 45f

                val chartWidth = canvasWidth - leftOffset - rightOffset
                val chartHeight = canvasHeight - topOffset - bottomOffset

                val temps = data.map { it.second }
                val maxTemp = temps.maxOrNull() ?: 30
                val minTemp = temps.minOrNull() ?: 20
                val tempRange = (maxTemp - minTemp).coerceAtLeast(2)

                // 1. Cartesian Grid Lines (Horizontal)
                val gridLineCount = 3
                for (j in 0 until gridLineCount) {
                    val ratio = j.toFloat() / (gridLineCount - 1)
                    val y = topOffset + chartHeight * (1f - ratio)
                    
                    drawLine(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                        start = Offset(leftOffset, y),
                        end = Offset(canvasWidth - rightOffset, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                }

                // 2. Compute coordinates for each time-temperature point
                val stepX = chartWidth / (data.size - 1)
                val points = data.mapIndexed { index, pair ->
                    val x = leftOffset + index * stepX
                    val tempRatio = (pair.second - minTemp).toFloat() / tempRange
                    val y = topOffset + chartHeight * (1f - tempRatio)
                    Offset(x, y)
                }

                // 3. Draw Area Gradient under the curve (Matching Recharts' smooth fade)
                val areaPath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            // Using smooth cubic bezier to connect coordinates
                            val prevPoint = points[i - 1]
                            val currPoint = points[i]
                            val controlPoint1 = Offset(prevPoint.x + stepX / 2f, prevPoint.y)
                            val controlPoint2 = Offset(currPoint.x - stepX / 2f, currPoint.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, currPoint.x, currPoint.y)
                        }
                        // Close the shape beneath the path
                        lineTo(points.last().x, topOffset + chartHeight)
                        lineTo(points.first().x, topOffset + chartHeight)
                        close()
                    }
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.35f), // Recharts primary sky blue fill
                            Color(0xFF0284C7).copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        startY = topOffset,
                        endY = topOffset + chartHeight
                    )
                )

                // 4. Draw the actual temperature trend curve (Sky Blue)
                val linePath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prevPoint = points[i - 1]
                            val currPoint = points[i]
                            val controlPoint1 = Offset(prevPoint.x + stepX / 2f, prevPoint.y)
                            val controlPoint2 = Offset(currPoint.x - stepX / 2f, currPoint.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, currPoint.x, currPoint.y)
                        }
                    }
                }

                drawPath(
                    path = linePath,
                    color = Color(0xFF38BDF8),
                    style = Stroke(
                        width = 4.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 5. Draw Circular Nodes, temperature value texts, and time details
                points.forEachIndexed { index, point ->
                    val tempVal = data[index].second
                    val formattedTemp = formatTemperature(tempVal, weatherUnit)
                    val timeVal = data[index].first

                    // Circle outer glow base
                    drawCircle(
                        color = Color(0xFF0284C7),
                        radius = 8f,
                        center = point
                    )

                    // Inner core
                    drawCircle(
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        radius = 4.5f,
                        center = point
                    )

                    // Temperature value label
                    val tempTextResult = textMeasurer.measure(
                        text = formattedTemp,
                        style = TextStyle(
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = tempTextResult,
                        topLeft = Offset(
                            x = point.x - tempTextResult.size.width / 2f,
                            y = point.y - tempTextResult.size.height - 6f
                        )
                    )

                    // Time description label
                    val timeTextResult = textMeasurer.measure(
                        text = timeVal,
                        style = TextStyle(
                            color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF0F172A).copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    drawText(
                        textLayoutResult = timeTextResult,
                        topLeft = Offset(
                            x = point.x - timeTextResult.size.width / 2f,
                            y = topOffset + chartHeight + 10f
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityLockScreen(
    activeTheme: PremiumLayoutTheme,
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuroraMidnight)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background color glow reflecting active theme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(40.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            activeTheme.accentColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            containerColor = activeTheme.cardBg,
            borderColors = activeTheme.cardBorderGlowColors,
            glowColor = activeTheme.accentColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive glowing lock pulse ceremony
                val infiniteTransition = rememberInfiniteTransition(label = "lock_glow_anim")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(activeTheme.accentColor.copy(alpha = 0.15f))
                        .border(2.dp, activeTheme.accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "密碼鎖屏狀態",
                        tint = activeTheme.accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pixel Brief 安全防護鎖",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "為確保您的健康指數、昨夜深度睡眠數據、以及即時行事曆與今日生活目標等隱私安全，請進行裝置與生物特徵驗證來解鎖首頁。",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onUnlockClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.accentColor,
                        contentColor = AuroraMidnight
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("trigger_biometric_unlock_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "解鎖",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "感應身分解鎖 🛡️",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

