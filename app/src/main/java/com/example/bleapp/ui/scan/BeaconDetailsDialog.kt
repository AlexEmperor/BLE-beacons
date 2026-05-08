package com.example.bleapp.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bleapp.data.Beacon
import com.example.bleapp.util.calculateDistance
import com.example.bleapp.util.rssiColor

@Composable
fun BeaconDetailsDialog(beacon: Beacon, onDismiss: () -> Unit) {
    val color = rssiColor(beacon.rssi)
    val distance = calculateDistance(beacon.rssi, beacon.txPower)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF161821))
                .border(1.dp, Color(0xFF2A2D38), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
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

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("×", color = Color(0xFF8A8A95), fontSize = 24.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                MacRow(mac = beacon.mac)
                InfoRow("RSSI", "${beacon.rssi} dBm")
                InfoRow("TX Power", "${beacon.txPower} dBm")
                InfoRow("Beacon ID", "0x%08X".format(beacon.beaconId))
                InfoRow("Major", beacon.major.toString())
                InfoRow("Minor", beacon.minor.toString())
                InfoRow("Координаты", "%.6f, %.6f".format(beacon.latitude, beacon.longitude))
            }
        }
    }
}

@Composable
private fun CopyIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val stroke = s * 0.10f
        val radius = CornerRadius(s * 0.12f, s * 0.12f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(s * 0.30f, s * 0.10f),
            size = Size(s * 0.55f, s * 0.55f),
            cornerRadius = radius,
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(s * 0.10f, s * 0.30f),
            size = Size(s * 0.55f, s * 0.55f),
            cornerRadius = radius,
            style = Stroke(width = stroke)
        )
    }
}

@Composable
private fun MacRow(mac: String) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("MAC-адрес", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            mac,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        IconButton(
            onClick = { clipboard.setText(AnnotatedString(mac)) },
            modifier = Modifier.size(28.dp)
        ) {
            CopyIcon(tint = Color(0xFF8A8A95), modifier = Modifier.size(16.dp))
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
