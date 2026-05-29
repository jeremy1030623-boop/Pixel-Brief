package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AgendaEvent
import com.example.model.BriefData
import com.example.model.HourlyForecast
import com.example.model.WeatherInfo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeatherCropImage(
    condition: String,
    isNight: Boolean,
    modifier: Modifier = Modifier
) {
    val imageResult = runCatching { ImageBitmap.imageResource(id = R.drawable.img_weather_sheet) }
    val image = imageResult.getOrNull()

    if (image == null) {
        // Fallback to standard icons if image sheet is loading/missing
        Icon(
            imageVector = if (isNight) Icons.Default.Nightlight else Icons.Default.WbSunny,
            contentDescription = condition,
            modifier = modifier,
            tint = if (isNight) Color(0xFF90CAF9) else Color(0xFFFBC02D)
        )
        return
    }

    val cols = 4
    val rows = 2
    
    val cellWidth = image.width / cols
    val cellHeight = image.height / rows
    
    if (cellWidth <= 0 || cellHeight <= 0) {
        return
    }

    val conditionLower = condition.lowercase()
    val (row, col) = when {
        conditionLower.contains("snow") || conditionLower.contains("雪") -> Pair(1, 2)
        conditionLower.contains("thunder") || conditionLower.contains("雷") -> Pair(0, 3)
        conditionLower.contains("rain") || conditionLower.contains("雨") -> Pair(0, 2)
        conditionLower.contains("cloud") || conditionLower.contains("雲") || conditionLower.contains("陰") -> {
            if (isNight) Pair(1, 1) else Pair(0, 1)
        }
        conditionLower.contains("wind") || conditionLower.contains("風") -> Pair(1, 3)
        else -> { // Sunny / Clear
            if (isNight) Pair(1, 0) else Pair(0, 0)
        }
    }
    
    val srcX = col * cellWidth
    val srcY = row * cellHeight
    
    val srcOffset = IntOffset(srcX, srcY)
    val srcSize = IntSize(cellWidth, cellHeight)
    
    Canvas(modifier = modifier) {
        drawImage(
            image = image,
            srcOffset = srcOffset,
            srcSize = srcSize,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningBriefingScreen(
    viewModel: MorningViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tempUnit by viewModel.tempUnit.collectAsState()

    val currentHour = SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toIntOrNull() ?: 12
    val isNight = currentHour < 6 || currentHour >= 18

    // Background gradient depending on daytime vs nighttime
    val bgGradient = if (isNight) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F121F),
                Color(0xFF1E2035)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1F355A),
                Color(0xFF0F111E)
            )
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Pixel Brief",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "重新載入",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(bgGradient)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                    }
                }
                is UiState.Success -> {
                    SuccessContent(
                        data = state.data,
                        tempUnit = tempUnit,
                        isNight = isNight,
                        onToggleUnit = { viewModel.toggleTempUnit() }
                    )
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.message,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadData() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("重試", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessContent(
    data: BriefData,
    tempUnit: String,
    isNight: Boolean,
    onToggleUnit: () -> Unit
) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "早安，美好的一天！🌅"
        in 12..17 -> "午安，持續加油！☕"
        else -> "晚安，星空陪伴你！🌌"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Warm Greeting & Location Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "位置",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "台北市 • 智慧定位",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray
                    )
                }
            }
        }

        // 2. Weather Highlight Widget
        item {
            WeatherHighlightWidget(
                weather = data.weather,
                tempUnit = tempUnit,
                isNight = isNight,
                onToggleUnit = onToggleUnit
            )
        }

        // 3. AI Generated Briefing Message
        item {
            AIBriefingWidget(briefText = data.aiBrief)
        }

        // 4. Calendar & Agenda Actions List
        item {
            AgendaWidget(events = data.events)
        }

        // 5. Fun Fact Widget
        item {
            FunFactWidget(factText = data.funFact)
        }
    }
}

@Composable
fun WeatherHighlightWidget(
    weather: WeatherInfo,
    tempUnit: String,
    isNight: Boolean,
    onToggleUnit: () -> Unit
) {
    fun formatTemp(temp: Double): String {
        return if (tempUnit == "F") {
            "${((temp * 9 / 5) + 32).toInt()}°F"
        } else {
            "${temp.toInt()}°C"
        }
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x33FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(28.dp)),
        onClick = onToggleUnit
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "目前天氣",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTemp(weather.currentTemp),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Thin
                    )
                    Text(
                        text = weather.condition,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Beautiful custom cropped pixel icon
                WeatherCropImage(
                    condition = weather.condition,
                    isNight = isNight,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Temperature Min/Max bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Thermostat,
                        contentDescription = "體感溫度",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "範圍: ${formatTemp(weather.minTemp)} - ${formatTemp(weather.maxTemp)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = "點擊切換 單位",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Divider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(14.dp))

            // Hourly row
            Text(
                text = "後續小時預報",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weather.hourlyForecasts.take(5).forEach { hour ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = hour.time,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        WeatherCropImage(
                            condition = hour.condition,
                            isNight = isNight,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatTemp(hour.temp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AIBriefingWidget(briefText: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x2200E5FF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x3300E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI 簡報",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI 精靈每日簡報",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = briefText,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.95f),
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun AgendaWidget(events: List<AgendaEvent>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x19FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x22FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = "日程",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "今日日程計畫",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            events.forEachIndexed { idx, event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small glowing indicator bullet
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = when (idx % 3) {
                                        0 -> Color(0xFF80DEEA)
                                        1 -> Color(0xFFFFCC80)
                                        else -> Color(0xFFFBC02D)
                                    },
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = event.dateTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (idx < events.size - 1) {
                    Divider(color = Color.White.copy(alpha = 0.08f))
                }
            }
        }
    }
}

@Composable
fun FunFactWidget(factText: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x11FFCC80)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Lightbulb,
                contentDescription = "冷知識",
                tint = Color(0xFFFFCC80),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "每日冷知識",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC80)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = factText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
