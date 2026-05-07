package com.example.bleapp.ui.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.bleapp.ui.theme.BgPrimary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(5000)
        onFinished()
    }

    val transition = rememberInfiniteTransition(label = "splashPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "splashAnim"
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "splashSweep"
    )
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "splashOrbit"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A0E36),
                        Color(0xFF0E1530),
                        BgPrimary
                    ),
                    radius = 1400f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // фоновые цветные пятна
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF3DA6).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.22f),
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.18f, size.height * 0.22f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.78f),
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.85f, size.height * 0.78f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFC400).copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.18f),
                    radius = size.minDimension * 0.45f
                ),
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.85f, size.height * 0.18f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val maxR = size.minDimension / 2

                    // вращающийся радужный сектор
                    rotate(degrees = sweep, pivot = Offset(cx, cy)) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                0f to Color.Transparent,
                                0.4f to Color(0xFFFF3DA6).copy(alpha = 0.10f),
                                0.7f to Color(0xFF00E5FF).copy(alpha = 0.22f),
                                1f to Color(0xFF00FFA3).copy(alpha = 0.45f),
                                center = Offset(cx, cy)
                            ),
                            radius = maxR * 0.95f,
                            center = Offset(cx, cy)
                        )
                    }

                    // расходящиеся кольца (cyan)
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.20f * (1 - pulse)),
                        radius = maxR * (0.45f + 0.55f * pulse),
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = Color(0xFFFF3DA6).copy(alpha = 0.16f * (1 - ((pulse + 0.33f) % 1f))),
                        radius = maxR * (0.45f + 0.55f * ((pulse + 0.33f) % 1f)),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )

                    // статичные кольца
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.20f),
                        radius = maxR * 0.55f,
                        style = Stroke(width = 1.2f)
                    )
                    drawCircle(
                        color = Color(0xFFFFC400).copy(alpha = 0.18f),
                        radius = maxR * 0.40f,
                        style = Stroke(width = 1.2f)
                    )

                    // ядро — градиент
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFF00E5FF),
                                Color(0xFF7B2CFF)
                            ),
                            center = Offset(cx, cy),
                            radius = maxR * 0.30f
                        ),
                        radius = maxR * 0.30f,
                        center = Offset(cx, cy)
                    )

                    // орбитальные точки (3 разных цвета)
                    val orbitR = maxR * 0.78f
                    val colors = listOf(
                        Color(0xFFFF3DA6),
                        Color(0xFF00FFA3),
                        Color(0xFFFFC400)
                    )
                    for (i in 0..2) {
                        val a = Math.toRadians((orbit + i * 120f).toDouble())
                        val px = cx + orbitR * cos(a).toFloat()
                        val py = cy + orbitR * sin(a).toFloat()
                        drawCircle(
                            color = colors[i].copy(alpha = 0.35f),
                            radius = 14f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = colors[i],
                            radius = 6f,
                            center = Offset(px, py)
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "BLE Beacon",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Indoor positioning",
                color = Color(0xFF00E5FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp
            )
        }
    }
}
