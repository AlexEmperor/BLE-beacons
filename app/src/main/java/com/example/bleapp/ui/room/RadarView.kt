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
import com.example.bleapp.data.BeaconSeed
import com.example.bleapp.util.beaconColor
import com.example.bleapp.util.calculateDistance
import androidx.compose.ui.draw.clipToBounds

@Composable
fun PositionView(beacons: List<Beacon>, userPos: Offset, seeds: List<BeaconSeed>, roomSizeM: Float) {

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

            val pxPerMeter = minOf(width, height) / roomSizeM

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

            // 1️⃣ круги дистанций — для каждого маяка, очень мягко, чтобы не рябило
            seeds.forEachIndexed { index, pos ->
                val beacon = beacons.find { it.id == pos.id } ?: return@forEachIndexed

                val bScreen = worldToScreen(pos.x, pos.y)
                val color = beaconColor(index, seeds.size.coerceAtLeast(1))
                val distMeters = calculateDistance(beacon.rssi).toFloat()
                val radiusPx = distMeters * pxPerMeter * scale

                drawCircle(
                    color = color.copy(alpha = 0.08f),
                    radius = radiusPx,
                    center = bScreen
                )
                drawCircle(
                    color = color.copy(alpha = 0.55f),
                    radius = radiusPx,
                    center = bScreen,
                    style = Stroke(width = 1.6f)
                )
            }

            // 2️⃣ иконки маяков и подписи
            seeds.forEachIndexed { index, pos ->
                val beacon = beacons.find { it.id == pos.id } ?: return@forEachIndexed
                val bScreen = worldToScreen(pos.x, pos.y)
                val color = beaconColor(index, seeds.size.coerceAtLeast(1))

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

        // 🏷️ индикатор масштаба
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