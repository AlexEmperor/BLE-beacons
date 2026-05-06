package com.example.bleapp.ui.scan

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bleapp.data.Beacon
import com.example.bleapp.ui.theme.BgSecondary
import com.example.bleapp.util.calculateDistance
import com.example.bleapp.util.rssiColor

@Composable
fun BeaconDetailsDialog(beacon: Beacon, onDismiss: () -> Unit) {
    val color = rssiColor(beacon.rssi)
    val distance = calculateDistance(beacon.rssi)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // 🪪 заголовок: иконка + имя + RSSI бейдж
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📡", fontSize = 24.sp)
                    }

                    Spacer(Modifier.size(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            beacon.id,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "≈ %.2f м".format(distance),
                            color = color,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 📋 поля
                InfoRow("MAC-адрес", beacon.mac)
                InfoRow("RSSI", "${beacon.rssi} dBm")
                InfoRow("TX Power", "${beacon.txPower} dBm")
                InfoRow("Beacon ID", "0x%08X".format(beacon.beaconId))
                InfoRow("Major", beacon.major.toString())
                InfoRow("Minor", beacon.minor.toString())
                InfoRow("Координаты", "%.6f, %.6f".format(beacon.latitude, beacon.longitude))

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2B5C9E),
                        contentColor = Color.White
                    )
                ) {
                    Text("ЗАКРЫТЬ", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}