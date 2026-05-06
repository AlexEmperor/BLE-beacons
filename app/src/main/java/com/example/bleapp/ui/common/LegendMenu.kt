package com.example.bleapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val beaconLegend = listOf(
    "Маяк 1" to Color(0xFFB388FF),
    "Маяк 2" to Color(0xFF40C4FF),
    "Маяк 3" to Color(0xFFFFC107),
    "Маяк 4" to Color(0xFFFF5252),
    "Маяк 5" to Color(0xFF69F0AE),
    "Маяк 6" to Color(0xFFFF80AB),
    "Маяк 7" to Color(0xFFFFAB40),
    "Маяк 8" to Color(0xFF80D8FF)
)

private val rssiLegend = listOf(
    Triple("-50…-60 dBm", "Отлично", Color(0xFF00FFA3)),
    Triple("-60…-70 dBm", "Хорошо", Color(0xFF7CFF6B)),
    Triple("-70…-80 dBm", "Удовл.", Color(0xFFFFC107)),
    Triple("-80…-100 dBm", "Слабо", Color(0xFFFF5252)),
    Triple("< -100 dBm", "Нет сигнала", Color(0xFF888888))
)

/**
 * Кнопка ⋮ с выпадающим меню легенды.
 * Кладёшь её в actions у TopAppBar.
 */
@Composable
fun LegendMenuButton() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, "Меню", tint = Color.White)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF161821))
                .width(260.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Условные обозначения",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(12.dp))

                // 📡 список маяков с цветами
                beaconLegend.forEach { (name, color) ->
                    LegendRow(color = color, label = name)
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF2A2D38))
                Spacer(Modifier.height(8.dp))

                Text(
                    "Цвет RSSI (качество сигнала)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))

                // 📶 уровни сигнала
                rssiLegend.forEach { (range, label, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(range, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(label, color = Color(0xFF8A8A95), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // цветной кружок с glow
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.25f))
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}