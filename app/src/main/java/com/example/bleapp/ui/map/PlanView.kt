package com.example.bleapp.ui.map

import android.graphics.Bitmap
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.data.Beacon
import com.example.bleapp.data.BeaconSeed
import com.example.bleapp.data.PlanFloor
import com.example.bleapp.util.beaconColor
import com.example.bleapp.util.loadPlanBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlanView(floor: PlanFloor, beacons: List<Beacon>, userPos: Offset, seeds: List<BeaconSeed>) {
    val context = LocalContext.current
    var bitmap by remember(floor.id) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(floor.id) { mutableStateOf(true) }

    LaunchedEffect(floor.id) {
        loading = true
        bitmap = withContext(Dispatchers.IO) {
            loadPlanBitmap(context, floor.assetPath, floor.isSvg)
        }
        loading = false
    }

    var scale by remember(floor.id) { mutableStateOf(1f) }
    var offset by remember(floor.id) { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(floor.id) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.2f, 8f)
                        offset += pan
                    }
                }
        ) {
            val padding = 16f
            val areaW = size.width - padding * 2
            val areaH = size.height - padding * 2
            if (areaW <= 0 || areaH <= 0) return@Canvas

            // Если bitmap ещё не загружен, кладём план в область канваса как
            // прямоугольник «по умолчанию» — чтобы маяки/пользователь
            // отображались сразу.
            val planAspect = bitmap?.let { it.width.toFloat() / it.height.toFloat() }
                ?: (floor.widthMeters / floor.heightMeters)
            val areaAspect = areaW / areaH
            val drawW: Float
            val drawH: Float
            if (planAspect >= areaAspect) {
                drawW = areaW
                drawH = areaW / planAspect
            } else {
                drawH = areaH
                drawW = areaH * planAspect
            }

            val cx = size.width / 2
            val cy = size.height / 2
            val baseLeft = cx - drawW / 2
            val baseTop = cy - drawH / 2
            val planLeft = cx + (baseLeft - cx) * scale + offset.x
            val planTop = cy + (baseTop - cy) * scale + offset.y
            val planW = drawW * scale
            val planH = drawH * scale

            drawRect(
                color = Color(0xFF0F1420),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height)
            )

            bitmap?.let { bmp ->
                drawContext.canvas.nativeCanvas.apply {
                    val src = android.graphics.Rect(0, 0, bmp.width, bmp.height)
                    val dst = android.graphics.RectF(planLeft, planTop, planLeft + planW, planTop + planH)
                    drawBitmap(bmp, src, dst, null)
                }
            }

            // 📡 маяки выбранного этажа (Y уже инвертирован в seedsForFloor)
            seeds.forEachIndexed { index, pos ->
                beacons.find { it.id == pos.id } ?: return@forEachIndexed
                val color = beaconColor(index, seeds.size.coerceAtLeast(1))
                val px = planLeft + pos.x * planW
                val py = planTop + pos.y * planH
                val p = Offset(px, py)
                drawCircle(color.copy(alpha = 0.20f), 22f, p)
                drawCircle(color.copy(alpha = 0.40f), 16f, p)
                drawCircle(color, 11f, p)

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize = 28f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                    drawText(pos.id, px - 18f, py - 22f, paint)
                }
            }

            // 👤 пользователь
            val u = Offset(planLeft + userPos.x * planW, planTop + userPos.y * planH)
            drawCircle(Color(0x2240C4FF), 50f, u)
            drawCircle(Color(0x4440C4FF), 35f, u)
            drawCircle(Color.White, 24f, u, style = Stroke(3f))
            drawCircle(Color(0xFF40C4FF), 22f, u)
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Color(0xFF00E5FF)) }
        } else if (bitmap == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Не удалось загрузить план",
                    color = Color(0xFFFF8080),
                    fontSize = 14.sp
                )
            }
        }

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
