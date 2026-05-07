package com.example.bleapp.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cyan = Color(0xFF00E5FF)
private val Pill = Color(0xFF12141C)
private val PillBorder = Color(0xFF1F2330)

enum class TabIconKind { Scan, Beacons, Floor }

data class SegmentedTab(val title: String, val icon: TabIconKind)

@Composable
fun SegmentedTabs(
    tabs: List<SegmentedTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scanActive: Boolean = false
) {
    val animated by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
        label = "tabIndicator"
    )

    val scanPulse by rememberInfiniteTransition(label = "scanTabDot").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scanTabDotAnim"
    )

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Pill)
            .border(BorderStroke(1.dp, PillBorder), RoundedCornerShape(28.dp))
            .padding(5.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / tabs.size

            // 💫 анимированный селектор-пилюля
            Box(
                modifier = Modifier
                    .offset(x = tabWidth * animated)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(2.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Cyan,
                        spotColor = Cyan
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F4A5E),
                                Color(0xFF0A2733)
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.dp, Cyan.copy(alpha = 0.55f)),
                        RoundedCornerShape(22.dp)
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { i, tab ->
                    val selected = i == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSelected(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TabIcon(
                                kind = tab.icon,
                                tint = if (selected) Cyan else Color(0xFF6A6D78)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                tab.title,
                                color = if (selected) Color.White else Color(0xFF8A8A95),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                letterSpacing = 0.3.sp
                            )
                            if (tab.icon == TabIconKind.Scan && scanActive) {
                                Spacer(Modifier.width(6.dp))
                                Canvas(modifier = Modifier.size(6.dp)) {
                                    drawCircle(
                                        color = Cyan.copy(alpha = scanPulse),
                                        radius = size.minDimension / 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIcon(kind: TabIconKind, tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val s = size.minDimension
        val cx = size.width / 2
        val cy = size.height / 2
        val stroke = s * 0.13f
        when (kind) {
            TabIconKind.Scan -> {
                // дуги Wi-Fi сверху + точка снизу
                for (i in 0..2) {
                    val r = s * (0.18f + 0.18f * i)
                    drawArc(
                        color = tint.copy(alpha = if (i == 2) 0.55f else 1f),
                        startAngle = -135f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r + s * 0.10f),
                        size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                drawCircle(tint, radius = s * 0.07f, center = Offset(cx, cy + s * 0.30f))
            }
            TabIconKind.Beacons -> {
                // концентрические кольца + центральная точка (радар)
                drawCircle(tint.copy(alpha = 0.35f), radius = s * 0.45f, center = Offset(cx, cy), style = Stroke(width = stroke * 0.9f))
                drawCircle(tint.copy(alpha = 0.7f), radius = s * 0.28f, center = Offset(cx, cy), style = Stroke(width = stroke * 0.9f))
                drawCircle(tint, radius = s * 0.10f, center = Offset(cx, cy))
            }
            TabIconKind.Floor -> {
                // план: рамка + перегородка
                val pad = s * 0.10f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(pad, pad),
                    size = androidx.compose.ui.geometry.Size(s - pad * 2, s - pad * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.10f, s * 0.10f),
                    style = Stroke(width = stroke)
                )
                // вертикальная стена
                drawLine(
                    tint,
                    start = Offset(s * 0.55f, pad),
                    end = Offset(s * 0.55f, s * 0.55f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                // горизонтальная стена
                drawLine(
                    tint,
                    start = Offset(s * 0.55f, s * 0.55f),
                    end = Offset(s - pad, s * 0.55f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
