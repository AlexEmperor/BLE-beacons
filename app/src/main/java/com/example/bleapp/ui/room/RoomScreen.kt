package com.example.bleapp.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.data.Beacon
import com.example.bleapp.ui.common.LegendMenuButton
import com.example.bleapp.ui.theme.BgPrimary
import com.example.bleapp.ui.theme.BgSecondary
import com.example.bleapp.ui.theme.BgTertiary
import com.example.bleapp.util.calculateDistance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(beacons: List<Beacon>, userPos: Offset) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        TopAppBar(
            title = { Text("Позиция", color = Color.White, fontWeight = FontWeight.SemiBold) },
            actions = { LegendMenuButton() },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BgTertiary)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PositionView(beacons, userPos)
        }

        StatusPanel(beacons, userPos)
    }
}

@Composable
private fun StatusPanel(beacons: List<Beacon>, userPos: Offset) {
    val xMeters = userPos.x * 6f
    val yMeters = userPos.y * 6f
    val accuracy = if (beacons.isNotEmpty()) {
        beacons.map { calculateDistance(it.rssi) }.average() * 0.2
    } else 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgSecondary)
            .padding(16.dp)
    ) {
        Column {
            Text(
                "Позиция: (%.1f, %.1f) м".format(xMeters, yMeters),
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                "Точность: ~%.1f м".format(accuracy),
                color = Color(0xFF8A8A95),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}