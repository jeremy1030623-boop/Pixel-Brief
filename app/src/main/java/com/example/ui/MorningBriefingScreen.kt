package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.*
import com.example.model.WeatherInfo
import com.example.ui.theme.Typography
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

    val backgroundBrush = getBackgroundBrush(weather.condition)
    var visible by remember { mutableStateOf(false) }
    var selectedNewsItem by remember { mutableStateOf<NewsItem?>(null) }

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

    val checkAndRequestPermission = {
        if (!calendarPermissionGranted.value) {
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
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
        AnimatedContent(
            targetState = selectedNewsItem,
            label = "ScreenContent"
        ) { newsItem ->
            if (newsItem == null) {
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
                        GreetingSection(username, currentTime, weather)
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
                            onAuthorize = { checkAndRequestPermission() },
                            onNewsClick = { item ->
                                selectedNewsItem = item
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(64.dp))
                }
            } else {
                NewsDetailScreen(newsItem) {
                    selectedNewsItem = null
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
            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
        }
    }
}

@Composable
fun UserAvatar(username: String) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6)))),
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
fun GreetingSection(username: String, time: String, weather: WeatherInfo) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "現在時間 $time",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${weather.condition} ${weather.currentTemp}°",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFF1F5F9)
            )
            Text(
                text = "最高 ${weather.maxTemp}° / 最低 ${weather.minTemp}°",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "早安, $username",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(end = 8.dp)
            )
            UserAvatar(username)
        }
    }
}

val ExpressiveShape = RoundedCornerShape(32.dp)

@Composable
fun getCardBackgroundColor(condition: String): Color {
    return when {
        condition.contains("雨") -> Color(0xFF1E293B) // Solid Slate Blue
        condition.contains("多雲") -> Color(0xFF273549) // Solid Cool Grey
        else -> Color(0xFF4A2A14) // Solid Terracotta Espresso
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
    onAuthorize: () -> Unit,
    onNewsClick: (NewsItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SleepCard(sleepInfo, weather.condition)
        WeatherCard(weather, weather.condition)
        ActivityCard(events, weather.condition, onAuthorize)
        NewsCard(news, weather.condition, onNewsClick)
    }
}

@Composable
fun SleepCard(sleep: SleepInfo, condition: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = getCardBackgroundColor(condition)),
        shape = ExpressiveShape
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
                    Text("昨晚睡眠", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${sleep.hours}小時睡眠", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("打鼾: ${sleep.snoringMinutes}m", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sick, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("咳嗽: ${sleep.coughCount}次", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun WeatherCard(weather: WeatherInfo, condition: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("dyn-weather://"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=weather"))
                    genericIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    try {
                        context.startActivity(genericIntent)
                    } catch (e2: Exception) {
                        try {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather"))
                            webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(webIntent)
                        } catch (e3: Exception) {
                            // Gratefully catch all to make sure it never crashes
                            android.widget.Toast.makeText(context, "無法打開天氣資訊", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
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
                    Text("最高 ${weather.maxTemp}° / 最低 ${weather.minTemp}°", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                }
            }
            
            Text(
                text = "${weather.currentTemp}°",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun ActivityCard(events: List<com.example.data.CalendarEvent>, condition: String, onAuthorize: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onAuthorize() },
        colors = CardDefaults.cardColors(containerColor = getCardBackgroundColor(condition)),
        shape = ExpressiveShape,
        border = null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("最近活動", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
            Spacer(modifier = Modifier.height(16.dp))
            if (events.isEmpty()) {
                Text("無近期活動\n(可點擊授權日曆權限以同步行程)", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodyMedium)
            } else {
                events.take(3).forEach { event ->
                    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF81D4FA))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(news: List<NewsItem>, condition: String, onItemClick: (NewsItem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = getCardBackgroundColor(condition)),
        shape = ExpressiveShape,
        border = null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("今日重點", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(16.dp))
            if (news.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFCE93D8), strokeWidth = 2.dp)
                }
            } else {
                news.take(3).forEachIndexed { index, item ->
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
                    if (index < 2 && index < news.size - 1) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun getBackgroundBrush(condition: String): Brush {
    return when {
        condition.contains("雨") -> Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF000000)))
        condition.contains("多雲") -> Brush.verticalGradient(listOf(Color(0xFF7F8C8D), Color(0xFF2C3E50)))
        else -> Brush.verticalGradient(listOf(Color(0xFFF39C12), Color(0xFFD35400))) // Sunny
    }
}

fun getTimeString(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

