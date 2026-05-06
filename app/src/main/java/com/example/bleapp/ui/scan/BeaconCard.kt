package com.example.bleapp.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.data.Beacon
import com.example.bleapp.util.calculateDistance
import com.example.bleapp.util.rssiColor

@Composable
fun BeaconCard(
    beacon: Beacon,
    onClick: () -> Unit = {}
) {
    val dist = calculateDistance(beacon.rssi)
    val color = rssiColor(beacon.rssi)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 📡 круглая иконка маяка с glow (как на макете)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            BeaconIconLarge(color)
        }

        Spacer(Modifier.width(16.dp))

        // 🪪 имя + MAC + RSSI (компактнее, как на макете)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                beacon.id,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                beacon.mac,
                color = Color(0xFF8A8A95),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "RSSI: ${beacon.rssi} dBm",
                color = Color(0xFF8A8A95),
                fontSize = 11.sp
            )
        }

        // 📏 расстояние справа
        Text(
            "≈ %.1f м".format(dist),
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// 📡 иконка маяка побольше, ярче — как на макете
@Composable
private fun BeaconIconLarge(color: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        // три «волны» — побольше и поярче
        drawCircle(color, 3.5f, Offset(cx, cy))
        drawCircle(color, 8f, Offset(cx, cy), style = Stroke(2.2f))
        drawCircle(color, 13f, Offset(cx, cy), style = Stroke(2.2f))
    }
}