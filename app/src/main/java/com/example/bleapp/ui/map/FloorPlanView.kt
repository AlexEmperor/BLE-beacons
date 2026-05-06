package com.example.bleapp.ui.map

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.data.Beacon
import com.example.bleapp.data.mapBeacons

// 🎨 цвета маяков (те же, что и на радаре)
private val beaconColors = listOf(
    Color(0xFFB388FF),
    Color(0xFF40C4FF),
    Color(0xFFFFC107),
    Color(0xFFFF5252),
    Color(0xFF69F0AE),
    Color(0xFFFF80AB),
    Color(0xFFFFAB40),
    Color(0xFF80D8FF)
)

@Composable
fun FloorPlanView(beacons: List<Beacon>, userPos: Offset) {

    // 🔍 масштаб и сдвиг
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.2f, 5f)
                        offset += pan
                    }
                }
        ) {

            val padding = 24f
            val left = padding
            val top = padding
            val width = size.width - padding * 2
            val height = size.height - padding * 2

            val cx = left + width / 2
            val cy = top + height / 2

            // переводит мир (0..1) в экран с учётом zoom и pan
            fun worldToScreen(wx: Float, wy: Float): Offset {
                val baseX = left + wx * width
                val baseY = top + wy * height
                val sx = cx + (baseX - cx) * scale + offset.x
                val sy = cy + (baseY - cy) * scale + offset.y
                return Offset(sx, sy)
            }

            // 🟦 фон плана
            drawRect(
                color = Color(0xFF0F1420),
                topLeft = Offset(left, top),
                size = Size(width, height)
            )

            // 🔲 сетка (тоже масштабируется)
            val cells = 8
            for (i in 0..cells) {
                val tFrac = i.toFloat() / cells
                val pTop = worldToScreen(tFrac, 0f)
                val pBot = worldToScreen(tFrac, 1f)
                val pLft = worldToScreen(0f, tFrac)
                val pRgt = worldToScreen(1f, tFrac)
                drawLine(Color(0x1500E5FF), pTop, pBot, 1f)
                drawLine(Color(0x1500E5FF), pLft, pRgt, 1f)
            }

            // 🧱 стены (рамка комнаты тоже масштабируется)
            val tl = worldToScreen(0f, 0f)
            val tr = worldToScreen(1f, 0f)
            val bl = worldToScreen(0f, 1f)
            val br = worldToScreen(1f, 1f)
            drawLine(Color(0xFF3A4A60), tl, tr, 4f)
            drawLine(Color(0xFF3A4A60), tr, br, 4f)
            drawLine(Color(0xFF3A4A60), br, bl, 4f)
            drawLine(Color(0xFF3A4A60), bl, tl, 4f)

            // 📡 ВСЕ маяки (включая дальние)
            mapBeacons.forEachIndexed { index, pos ->
                // если для этого маяка нет данных — пропускаем
                val beacon = beacons.getOrNull(index) ?: return@forEachIndexed

                val color = beaconColors[index % beaconColors.size]
                val p = worldToScreen(pos.x, pos.y)

                // glow + точка
                drawCircle(color.copy(alpha = 0.20f), 22f, p)
                drawCircle(color.copy(alpha = 0.35f), 16f, p)
                drawCircle(color, 11f, p)

                // подпись
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize = 28f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    drawText(pos.id, p.x - 18f, p.y - 22f, paint)
                }
            }

            // 👤 пользователь — БОЛЬШОЙ, чтобы не сливался с маяками
            val u = worldToScreen(userPos.x, userPos.y)
            // широкий glow
            drawCircle(Color(0x2240C4FF), 50f, u)
            drawCircle(Color(0x4440C4FF), 35f, u)
            // белая обводка для контраста
            drawCircle(Color.White, 24f, u, style = Stroke(3f))
            // ядро
            drawCircle(Color(0xFF40C4FF), 22f, u)
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

        // 🔄 сброс
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
            Icon(Icons.Default.Refresh, "Сбросить", tint = Color.White)
        }
    }
}