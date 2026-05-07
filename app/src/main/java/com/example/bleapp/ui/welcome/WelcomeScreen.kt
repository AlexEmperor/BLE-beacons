package com.example.bleapp.ui.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.ui.theme.BgPrimary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "welcomePulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Restart),
        label = "welcomeAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "BLE Beacon",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Поиск маяков и определение позиции",
                color = Color(0xFF8A8A95),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            BigStartButton(pulse = pulse, onClick = onStart)

            Spacer(Modifier.height(28.dp))

            Text(
                "Нажмите, чтобы начать сканирование",
                color = Color(0xFF8A8A95),
                fontSize = 13.sp
            )

            Spacer(Modifier.weight(1f))

            Text(
                "v1.0",
                color = Color(0xFF3A3D48),
                fontSize = 11.sp
            )
        }
    }
}

private data class RadarBlip(val angle: Float, val radiusFactor: Float)

private val radarBlips = listOf(
    RadarBlip(angle = 25f, radiusFactor = 0.62f),
    RadarBlip(angle = 78f, radiusFactor = 0.42f),
    RadarBlip(angle = 142f, radiusFactor = 0.74f),
    RadarBlip(angle = 198f, radiusFactor = 0.50f),
    RadarBlip(angle = 240f, radiusFactor = 0.82f),
    RadarBlip(angle = 305f, radiusFactor = 0.58f),
    RadarBlip(angle = 335f, radiusFactor = 0.38f)
)

@Composable
private fun BigStartButton(pulse: Float, onClick: () -> Unit) {
    val bgTransition = rememberInfiniteTransition(label = "welcomeBg")
    val sweep by bgTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "welcomeSweep"
    )
    val ring by bgTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "welcomeRing"
    )

    Box(
        modifier = Modifier.size(420.dp),
        contentAlignment = Alignment.Center
    ) {
        // 🛰 радар-фон: расходящиеся кольца + крутящийся сектор
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val maxR = size.minDimension / 2

            // 3 расходящихся кольца, фазированы
            for (i in 0..2) {
                val p = ((ring + i / 3f) % 1f)
                val r = maxR * (0.18f + 0.82f * p)
                drawCircle(
                    color = Color(0xFF00FFA3).copy(alpha = 0.18f * (1f - p)),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.4f)
                )
            }

            // вращающийся радар-луч: затухающий хвост по всей окружности
            val sectorR = maxR * 0.95f
            rotate(degrees = sweep, pivot = Offset(cx, cy)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.55f to Color(0xFF00FFA3).copy(alpha = 0.05f),
                        0.85f to Color(0xFF00FFA3).copy(alpha = 0.18f),
                        1f to Color(0xFF00FFA3).copy(alpha = 0.36f),
                        center = Offset(cx, cy)
                    ),
                    radius = sectorR,
                    center = Offset(cx, cy)
                )
            }

            // 🎯 точки-цели: подсвечиваются, когда луч проходит мимо, затем гаснут
            for (blip in radarBlips) {
                // фаза = сколько времени назад луч прошёл точку (0..1, где 0 — только что)
                val phase = (((sweep - blip.angle) % 360f) + 360f) % 360f / 360f
                // яркая вспышка при проходе луча, затем затухание
                val intensity = (1f - phase).let { it * it }
                if (intensity < 0.02f) continue
                val r = maxR * blip.radiusFactor
                val a = Math.toRadians(blip.angle.toDouble())
                val px = cx + r * cos(a).toFloat()
                val py = cy + r * sin(a).toFloat()
                // ореол
                drawCircle(
                    color = Color(0xFF00FFA3).copy(alpha = 0.35f * intensity),
                    radius = 22f * intensity + 6f,
                    center = Offset(px, py)
                )
                // ядро точки
                drawCircle(
                    color = Color(0xFF00FFA3).copy(alpha = 0.5f + 0.5f * intensity),
                    radius = 4.5f,
                    center = Offset(px, py)
                )
            }
        }

        // 💫 расходящееся пульсирующее кольцо
        Canvas(modifier = Modifier.size(260.dp)) {
            val cx = size.width / 2
            val cy = size.height / 2
            val maxR = size.minDimension / 2
            drawCircle(
                color = Color(0xFF00FFA3).copy(alpha = 0.20f * (1 - pulse)),
                radius = maxR * (0.65f + 0.35f * pulse),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF00FFA3).copy(alpha = 0.12f),
                radius = maxR * 0.85f,
                style = Stroke(width = 1.5f)
            )
        }

        // ⭕ основная кнопка
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1B7A4E),
                            Color(0xFF0E3826)
                        )
                    )
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // обводка
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF00FFA3),
                    radius = size.minDimension / 2 - 2f,
                    style = Stroke(width = 2.5f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PowerIcon(tint = Color(0xFF00FFA3), size = 56.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "СТАРТ",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun PowerIcon(tint: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.10f
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        // дуга
        drawArc(
            color = tint,
            startAngle = -60f,
            sweepAngle = -240f - 60f, // дуга сверху открыта
            useCenter = false,
            topLeft = Offset(cx - s * 0.38f, cy - s * 0.38f),
            size = androidx.compose.ui.geometry.Size(s * 0.76f, s * 0.76f),
            style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        // вертикальная палочка сверху
        drawLine(
            color = tint,
            start = Offset(cx, cy - s * 0.45f),
            end = Offset(cx, cy - s * 0.10f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
