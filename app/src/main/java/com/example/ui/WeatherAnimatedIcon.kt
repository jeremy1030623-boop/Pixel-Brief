package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WeatherAnimatedIcon(
    condition: String,
    isNight: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_icon_anim")

    // Gentle Breathing / Floating Animation of the entire weather icon
    val breathingTranslationY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_translation"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    // Sun Rotation & Heartbeat Animation
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_rotation"
    )

    val sunPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun_pulse"
    )

    // Cloud Sway / Drift Animation
    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = -4.dp.value,
        targetValue = 4.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_drift"
    )

    // Rain drop falls (3 droplets with staggered timings)
    val rain1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_1"
    )
    val rain2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_2"
    )
    val rain3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_3"
    )

    // Snowflake falls (3 snowflakes with different times and horizontal sways)
    val snow1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_1"
    )
    val snow2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_2"
    )
    val snow3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_3"
    )

    // Mist flowing lines
    val mist1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mist_1"
    )
    val mist2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mist_2"
    )

    // Lightning Flash
    val lightningAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                0f at 0
                0f at 1600
                1f at 1650
                0f at 1700
                1f at 1730
                0f at 1850
                0f at 2400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "lightning"
    )

    // Sparkling stars for night clear sky
    val starSparkle by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_sparkle"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            translationY = breathingTranslationY
            scaleX = breathingScale
            scaleY = breathingScale
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when {
            // Thunderstorm
            condition.contains("雷") -> {
                // Background dark cloud
                drawCloudShape(Color(0xFF334155), offsetX = cloudDrift, offsetY = -h * 0.1f)
                
                // Jagged electric lightning bolt
                if (lightningAlpha > 0.05f) {
                    val lightningPath = Path().apply {
                        moveTo(cx + w * 0.02f, cy + h * 0.02f)
                        lineTo(cx - w * 0.12f, cy + h * 0.22f)
                        lineTo(cx - w * 0.02f, cy + h * 0.22f)
                        lineTo(cx - w * 0.18f, cy + h * 0.44f)
                        lineTo(cx + w * 0.10f, cy + h * 0.18f)
                        lineTo(cx + w * 0.02f, cy + h * 0.18f)
                        close()
                    }
                    drawPath(
                        path = lightningPath,
                        color = Color(0xFFFBBF24).copy(alpha = lightningAlpha)
                    )
                }

                // Staggered heavy rain
                drawRaindrop(cx - w * 0.22f, cy + h * 0.05f, rain1, Color(0xFF60A5FA))
                drawRaindrop(cx + w * 0.18f, cy + h * 0.08f, rain2, Color(0xFF60A5FA))
            }

            // Rainy
            condition.contains("雨") -> {
                // Secondary background cloud
                drawCloudShape(Color(0xFF64748B).copy(alpha = 0.5f), offsetX = cloudDrift * 0.5f - w * 0.1f, offsetY = -h * 0.15f, baseScale = 0.85f)
                // Primary front cloud
                drawCloudShape(Color(0xFF475569), offsetX = cloudDrift, offsetY = -h * 0.08f)

                // Multi-layer rain droplets falling down
                drawRaindrop(cx - w * 0.25f, cy + h * 0.05f, rain1, Color(0xFF60A5FA))
                drawRaindrop(cx, cy + h * 0.08f, rain2, Color(0xFF93C5FD))
                drawRaindrop(cx + w * 0.25f, cy + h * 0.04f, rain3, Color(0xFF3B82F6))
            }

            // Snowy
            condition.contains("雪") -> {
                // Whitecloud base
                drawCloudShape(Color(0xFF94A3B8), offsetX = cloudDrift, offsetY = -h * 0.08f)

                // Beautiful floating ice crystals
                drawSnowflake(cx - w * 0.22f, cy + h * 0.05f, snow1)
                drawSnowflake(cx + w * 0.02f, cy + h * 0.08f, snow2)
                drawSnowflake(cx + w * 0.24f, cy + h * 0.04f, snow3)
            }

            // Foggy, Overcast, Mist
            condition.contains("霧") || condition.contains("陰") -> {
                // Solid overcast cloud
                drawCloudShape(Color(0xFF475569).copy(alpha = 0.6f), offsetX = cloudDrift * 0.6f, offsetY = -h * 0.12f, baseScale = 0.9f)
                drawCloudShape(Color(0xFF64748B), offsetX = -cloudDrift * 0.4f, offsetY = -h * 0.04f, baseScale = 0.95f)

                // Smooth horizontal mist waves
                drawMistLine(cy + h * 0.18f, mist1, w)
                drawMistLine(cy + h * 0.32f, mist2, w)
            }

            // Cloudy
            condition.contains("多雲") -> {
                if (isNight) {
                    // Moon back glow
                    drawCrescentMoon(cx - w * 0.12f, cy - h * 0.15f, w * 0.26f, Color(0xFFFDE68A))
                    // Front drifting fluffy cloud
                    drawCloudShape(Color(0xFFE2E8F0).copy(alpha = 0.92f), offsetX = cloudDrift, offsetY = h * 0.02f)
                } else {
                    // Golden sun behind cloud
                    drawSunBody(cx - w * 0.12f, cy - h * 0.15f, w * 0.23f * sunPulseScale, Color(0xFFFBBF24))
                    // Soft white/slate cloud
                    drawCloudShape(Color(0xFFF1F5F9).copy(alpha = 0.95f), offsetX = cloudDrift, offsetY = h * 0.02f)
                }
            }

            // Clear / Sunny
            condition == "晴朗" -> {
                if (isNight) {
                    // Deep cosmos crescent moon
                    drawCrescentMoon(cx, cy, w * 0.36f, Color(0xFFFDE68A))
                    
                    // Sparkle stars
                    drawSparkleStar(cx - w * 0.28f, cy - h * 0.24f, starSparkle * 0.8f, w * 0.035f)
                    drawSparkleStar(cx + w * 0.28f, cy + h * 0.18f, (1f - starSparkle + 0.3f).coerceIn(0.2f, 1f), w * 0.045f)
                } else {
                    // Rotating sun with radial sunbeams
                    val sunRadius = w * 0.26f * sunPulseScale
                    
                    // Sun rays rotated
                    withTransform({
                        rotate(sunRotation, pivot = Offset(cx, cy))
                    }) {
                        for (i in 0 until 8) {
                            val angleRad = Math.toRadians((i * 45).toDouble()).toFloat()
                            val rayStartDist = sunRadius + w * 0.05f
                            val rayLength = w * 0.10f
                            
                            val startOptX = cx + Math.cos(angleRad.toDouble()).toFloat() * rayStartDist
                            val startOptY = cy + Math.sin(angleRad.toDouble()).toFloat() * rayStartDist
                            val endOptX = cx + Math.cos(angleRad.toDouble()).toFloat() * (rayStartDist + rayLength)
                            val endOptY = cy + Math.sin(angleRad.toDouble()).toFloat() * (rayStartDist + rayLength)
                            
                            drawLine(
                                color = Color(0xFFF59E0B),
                                start = Offset(startOptX, startOptY),
                                end = Offset(endOptX, endOptY),
                                strokeWidth = w * 0.045f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Solid central sun body
                    drawSunBody(cx, cy, sunRadius, Color(0xFFFBBF24))
                }
            }

            // Default: Gentle breeze/windy
            else -> {
                if (isNight) {
                    drawCrescentMoon(cx, cy - h * 0.1f, w * 0.3f, Color(0xFFE2E8F0))
                } else {
                    drawSunBody(cx, cy - h * 0.1f, w * 0.28f, Color(0xFFFEF3C7))
                }
                // Gentle breeze lines
                drawMistLine(cy + h * 0.14f, mist1, w)
                drawMistLine(cy + h * 0.28f, mist2, w)
            }
        }
    }
}

// ---- HELPER DRAW PROCEDURES FOR CLEAN CANVAS DESIGN ----

private fun DrawScope.drawCloudShape(
    color: Color,
    offsetX: Float,
    offsetY: Float,
    baseScale: Float = 1f
) {
    val cloudWidth = size.width * 0.65f * baseScale
    val cloudHeight = size.height * 0.38f * baseScale
    val centerX = size.width / 2f + offsetX
    val centerY = size.height / 2f + offsetY

    // Bottom horizontal pill base
    val pillWidth = cloudWidth
    val pillHeight = cloudHeight * 0.52f
    val pillLeft = centerX - pillWidth / 2f
    val pillTop = centerY + cloudHeight * 0.15f - pillHeight / 2f

    drawRoundRect(
        color = color,
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillWidth, pillHeight),
        cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f)
    )

    // Overlapping bubble circles
    drawCircle(
        color = color,
        radius = cloudHeight * 0.38f,
        center = Offset(centerX - cloudWidth * 0.24f, centerY - cloudHeight * 0.04f)
    )

    drawCircle(
        color = color,
        radius = cloudHeight * 0.52f,
        center = Offset(centerX + cloudWidth * 0.02f, centerY - size.height * 0.08f)
    )

    drawCircle(
        color = color,
        radius = cloudHeight * 0.36f,
        center = Offset(centerX + cloudWidth * 0.26f, centerY + cloudHeight * 0.02f)
    )
}

private fun DrawScope.drawSunBody(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color
) {
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawCrescentMoon(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color
) {
    val moonPath = Path().apply {
        addOval(Rect(center = Offset(cx, cy), radius = radius))
    }
    // Compute subtraction to get a clean transparent-backed crescent moon
    val subtractPath = Path().apply {
        addOval(Rect(center = Offset(cx + radius * 0.38f, cy - radius * 0.32f), radius = radius))
    }
    val crescent = Path.combine(PathOperation.Difference, moonPath, subtractPath)
    drawPath(crescent, color = color)
}

private fun DrawScope.drawSparkleStar(
    cx: Float,
    cy: Float,
    sparkleScale: Float,
    maxRadius: Float
) {
    val r = maxRadius * sparkleScale
    val path = Path().apply {
        moveTo(cx, cy - r)
        quadraticTo(cx, cy, cx + r, cy)
        quadraticTo(cx, cy, cx, cy + r)
        quadraticTo(cx, cy, cx - r, cy)
        quadraticTo(cx, cy, cx, cy - r)
        close()
    }
    drawPath(path, color = Color.White.copy(alpha = sparkleScale))
}

private fun DrawScope.drawRaindrop(
    dropX: Float,
    baseStartY: Float,
    progress: Float,
    color: Color
) {
    val travelHeight = size.height * 0.35f
    val dropLength = size.height * 0.08f
    val startY = baseStartY + progress * travelHeight
    val endY = startY + dropLength

    // Draw rain angle slightly drifted left
    drawLine(
        color = color.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
        start = Offset(dropX, startY),
        end = Offset(dropX - size.width * 0.05f, endY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawSnowflake(
    dropX: Float,
    baseStartY: Float,
    progress: Float
) {
    val travelHeight = size.height * 0.35f
    val startY = baseStartY + progress * travelHeight
    // Sway horizontally with elegant sin-wave
    val swayX = dropX + sin(progress * Math.PI.toFloat() * 2f) * size.width * 0.06f

    drawCircle(
        color = Color.White.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
        radius = 3.dp.toPx() * (1f - progress * 0.25f),
        center = Offset(swayX, startY)
    )
}

private fun DrawScope.drawMistLine(
    centerY: Float,
    progress: Float,
    canvasWidth: Float
) {
    val lineLength = canvasWidth * 0.38f
    // Seamless sliding window
    val startX = -lineLength + progress * (canvasWidth + lineLength)
    // Sinusoidal alpha fading in and out of edges perfectly
    val alpha = sin(progress * Math.PI.toFloat()).coerceIn(0f, 1f) * 0.65f

    drawRoundRect(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(startX, centerY),
        size = Size(lineLength, 3.55.dp.toPx()),
        cornerRadius = CornerRadius(1.77.dp.toPx(), 1.77.dp.toPx())
    )
}
