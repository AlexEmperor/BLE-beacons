package com.example.bleapp.ui.room

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.data.Beacon
import com.example.bleapp.data.roomBeacons
import com.example.bleapp.util.calculateDistance
import androidx.compose.ui.draw.clipToBounds

private val beaconColors = listOf(
    Color(0xFFB388FF), // 1 — фиолетовый
    Color(0xFF40C4FF), // 2 — голубой
    Color(0xFFFFC107), // 3 — жёлтый
    Color(0xFFFF5252), // 4 — красный
    Color(0xFF69F0AE), // 5 — зелёный (дальний)
    Color(0xFFFF80AB), // 6 — розовый (дальний)
    Color(0xFFFFAB40), // 7 — оранжевый (дальний)
    Color(0xFF80D8FF)  // 8 — светло-голубой (дальний)
)

@Composable
fun PositionView(beacons: List<Beacon>, userPos: Offset) {

    // 🔍 состояние масштаба и сдвига
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 💫 пульс пользователя
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label = "pulseAnim"
    )

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // 🎯 ловим жесты: pinch-to-zoom и pan (drag)
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        // ограничиваем масштаб: 0.2x .. 5x
                        scale = (scale * zoomChange).coerceIn(0.2f, 5f)
                        offset += panChange
                    }
                }
        ) {

            val padding = 50f
            val left = padding
            val top = padding
            val width = size.width - padding * 2
            val height = size.height - padding * 2

            val pxPerMeter = minOf(width, height) / 6f

            // центр радара (вокруг него масштабируем)
            val cx = left + width / 2
            val cy = top + height / 2

            // ☑️ функция: переводит координаты "мира" в экранные с учётом zoom+pan
            fun worldToScreen(wx: Float, wy: Float): Offset {
                val baseX = left + wx * width
                val baseY = top + wy * height
                // масштабирование относительно центра + сдвиг
                val sx = cx + (baseX - cx) * scale + offset.x
                val sy = cy + (baseY - cy) * scale + offset.y
                return Offset(sx, sy)
            }

            val userScreen = worldToScreen(userPos.x, userPos.y)

            // 1️⃣ круги дистанций (тоже масштабируются вместе с миром)
            beacons.forEachIndexed { index, beacon ->
                if (index >= roomBeacons.size) return@forEachIndexed
                val pos = roomBeacons[index]
                val bScreen = worldToScreen(pos.x, pos.y)

                val color = beaconColors[index % beaconColors.size]
                val distMeters = calculateDistance(beacon.rssi).toFloat()
                val radiusPx = distMeters * pxPerMeter * scale

                drawCircle(
                    color = color.copy(alpha = 0.06f),
                    radius = radiusPx,
                    center = bScreen
                )
                drawCircle(
                    color = color,
                    radius = radiusPx,
                    center = bScreen,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    )
                )
            }

            // 2️⃣ иконки маяков и подписи
            beacons.forEachIndexed { index, beacon ->
                if (index >= roomBeacons.size) return@forEachIndexed
                val pos = roomBeacons[index]
                val bScreen = worldToScreen(pos.x, pos.y)
                val color = beaconColors[index % beaconColors.size]

                drawCircle(color.copy(alpha = 0.18f), 32f, bScreen)
                drawCircle(color.copy(alpha = 0.30f), 24f, bScreen)

                drawCircle(color, 4f, bScreen)
                drawCircle(color, 9f, bScreen, style = Stroke(2f))
                drawCircle(color, 15f, bScreen, style = Stroke(2f))

                val dist = calculateDistance(beacon.rssi)
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize = 28f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val text = "${pos.id}  %.1f м".format(dist)
                    val textWidth = paint.measureText(text)
                    drawText(text, bScreen.x - textWidth / 2, bScreen.y + 50f, paint)
                }
            }

            // 3️⃣ пользователь
            drawCircle(
                color = Color(0xFF40C4FF).copy(alpha = 0.3f * (1 - pulse)),
                radius = 18f + 35f * pulse,
                center = userScreen
            )
            drawCircle(Color(0x4440C4FF), 22f, userScreen)
            drawCircle(Color(0xFF40C4FF), 13f, userScreen)
        }

        // 🏷️ индикатор масштаба в углу
        Text(
            text = "%.1fx".format(scale),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color(0x99121826))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // 🔄 кнопка сброса масштаба и позиции
        IconButton(
            onClick = {
                scale = 1f
                offset = Offset.Zero
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color(0x99121826))
                .size(40.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Сбросить масштаб",
                tint = Color.White
            )
        }
    }
}