package com.example.bleapp.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.data.Beacon
import com.example.bleapp.ui.common.LegendMenuButton
import com.example.bleapp.ui.theme.BgPrimary
import com.example.bleapp.ui.theme.BgTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    beacons: List<Beacon>,
    isScanning: Boolean,
    onToggleScanning: () -> Unit
) {
    var selectedBeacon by remember { mutableStateOf<Beacon?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        TopAppBar(
            title = {
                Text("Сканирование", color = Color.White, fontWeight = FontWeight.SemiBold)
            },
            actions = { LegendMenuButton() },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BgTertiary)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            items(beacons) { beacon ->
                BeaconCard(
                    beacon = beacon,
                    onClick = { selectedBeacon = beacon }
                )
            }
        }

        Button(
            onClick = onToggleScanning,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2B5C9E),
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (isScanning) "ОСТАНОВИТЬ СКАНИРОВАНИЕ" else "НАЧАТЬ СКАНИРОВАНИЕ",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }

    selectedBeacon?.let { beacon ->
        BeaconDetailsDialog(
            beacon = beacon,
            onDismiss = { selectedBeacon = null }
        )
    }
}