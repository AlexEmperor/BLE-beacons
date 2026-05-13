package com.example.bleapp.ui.scan

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.data.Beacon
import com.example.bleapp.ui.theme.BgPrimary
import com.example.bleapp.viewmodel.ScannerFilter

@Composable
fun ScanScreen(
    beacons: List<Beacon>,
    isScanning: Boolean,
    showSavedBeacons: Boolean,
    scannerFilter: ScannerFilter,
    onToggleScanning: () -> Unit,
    onToggleSavedBeacons: () -> Unit,
    onFilterSelected: (ScannerFilter) -> Unit
) {
    var selectedBeacon by remember { mutableStateOf<Beacon?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        FilterSwitch(
            current = scannerFilter,
            onSelected = onFilterSelected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
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

        Column(modifier = Modifier.padding(16.dp)) {
            if (scannerFilter == ScannerFilter.OurFormat) {
                SavedBeaconsButton(
                    enabled = showSavedBeacons,
                    onToggle = onToggleSavedBeacons
                )
                Spacer(Modifier.height(10.dp))
            }
            ScanPillButton(
                isScanning = isScanning,
                onToggle = onToggleScanning
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

@Composable
private fun FilterSwitch(
    current: ScannerFilter,
    onSelected: (ScannerFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF12141C))
            .border(1.dp, Color(0xFF1F2330), RoundedCornerShape(22.dp))
            .padding(4.dp)
    ) {
        FilterSegment(
            label = "Наш формат",
            selected = current == ScannerFilter.OurFormat,
            onClick = { onSelected(ScannerFilter.OurFormat) },
            modifier = Modifier.weight(1f)
        )
        FilterSegment(
            label = "Все остальные",
            selected = current == ScannerFilter.Others,
            onClick = { onSelected(ScannerFilter.Others) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FilterSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) {
        Brush.linearGradient(listOf(Color(0xFF0E3548), Color(0xFF06121A)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF12141C), Color(0xFF12141C)))
    }
    val borderColor = if (selected) Color(0xFF00E5FF) else Color.Transparent
    val textColor = if (selected) Color(0xFFE6FBFF) else Color(0xFFB8BDC9)

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SavedBeaconsButton(enabled: Boolean, onToggle: () -> Unit) {
    val gradient = if (enabled) {
        Brush.linearGradient(listOf(Color(0xFF173B2D), Color(0xFF071911)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF1A1D26), Color(0xFF0F1117)))
    }
    val borderColor = if (enabled) Color(0xFF00FFA3) else Color(0xFF2A2F3A)
    val dotColor = if (enabled) Color(0xFF00FFA3) else Color(0xFF5A6070)
    val textColor = if (enabled) Color(0xFFE8FFF6) else Color(0xFFB8BDC9)
    val label = if (enabled) "СКРЫТЬ СУЩЕСТВУЮЩИЕ МАЯКИ" else "ОТОБРАЗИТЬ СУЩЕСТВУЮЩИЕ МАЯКИ"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(24.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun ScanPillButton(isScanning: Boolean, onToggle: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "scanPill")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scanPillPulse"
    )

    val gradient = if (isScanning) {
        Brush.linearGradient(listOf(Color(0xFF0E3548), Color(0xFF06121A)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF1A1D26), Color(0xFF0F1117)))
    }
    val borderColor = if (isScanning) Color(0xFF00E5FF) else Color(0xFF2A2F3A)
    val dotColor = if (isScanning) Color(0xFF00E5FF) else Color(0xFF5A6070)
    val textColor = if (isScanning) Color(0xFFE6FBFF) else Color(0xFFB8BDC9)
    val label = if (isScanning) "Сканирование…" else "НАЧАТЬ СКАНИРОВАНИЕ"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(28.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (isScanning) (8 * pulse + 4).dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isScanning) dotColor.copy(alpha = pulse) else dotColor
                    )
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}
