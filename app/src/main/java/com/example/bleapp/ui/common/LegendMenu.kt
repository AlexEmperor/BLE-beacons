package com.example.bleapp.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.data.Beacon
import com.example.bleapp.data.BeaconSeed
import com.example.bleapp.ui.scan.BeaconDetailsDialog
import com.example.bleapp.util.beaconColor
import com.example.bleapp.util.calculateDistance
import kotlinx.coroutines.launch

private val rssiLegend = listOf(
    Triple("-50…-60 dBm", "Отлично", Color(0xFF00FFA3)),
    Triple("-60…-70 dBm", "Хорошо", Color(0xFF7CFF6B)),
    Triple("-70…-80 dBm", "Удовл.", Color(0xFFFFC107)),
    Triple("-80…-100 dBm", "Слабо", Color(0xFFFF5252)),
    Triple("< -100 dBm", "Нет сигнала", Color(0xFF888888))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegendMenuButton(beacons: List<Beacon> = emptyList(), seeds: List<BeaconSeed> = emptyList()) {
    var open by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFF12141C))
            .border(BorderStroke(1.dp, Color(0xFF1F2330)), CircleShape)
    ) {
        IconButton(onClick = { open = true }, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.MoreVert, "Меню", tint = Color(0xFF00E5FF))
        }
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState,
            containerColor = Color(0xFF161821),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF3A3D48))
                )
            }
        ) {
            LegendSheetContent(
                beacons = beacons,
                seeds = seeds,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) open = false
                    }
                }
            )
        }
    }
}

@Composable
private fun LegendSheetContent(beacons: List<Beacon>, seeds: List<BeaconSeed>, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedBeacon by remember { mutableStateOf<Beacon?>(null) }

    // стабильный индекс маяка в общем перечне (для цвета, независимо от наличия в эфире)
    val filtered = remember(beacons, seeds, query) {
        val withStableIndex = beacons.map { b ->
            val stable = seeds.indexOfFirst { it.mac == b.mac }.coerceAtLeast(0)
            stable to b
        }
        if (query.isBlank()) withStableIndex
        else {
            val q = query.trim().lowercase()
            withStableIndex.filter { (_, b) -> b.mac.lowercase().contains(q) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Маяки (${beacons.size})",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Закрыть", tint = Color(0xFF8A8A95))
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Поиск по MAC", color = Color(0xFF6A6D78), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF8A8A95)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF2A2D38),
                cursorColor = Color(0xFF00E5FF)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (beacons.isEmpty()) "Маяков нет" else "Ничего не найдено",
                    color = Color(0xFF8A8A95),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
            ) {
                itemsIndexed(filtered, key = { _, item -> item.second.mac }) { _, (stableIndex, beacon) ->
                    BeaconLegendRow(
                        color = beaconColor(stableIndex, seeds.size.coerceAtLeast(1)),
                        beacon = beacon,
                        onClick = { selectedBeacon = beacon }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF2A2D38))
        Spacer(Modifier.height(12.dp))

        Text(
            "Цвет RSSI (качество сигнала)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        rssiLegend.forEach { (range, label, color) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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

    selectedBeacon?.let { beacon ->
        BeaconDetailsDialog(
            beacon = beacon,
            onDismiss = { selectedBeacon = null }
        )
    }
}

@Composable
private fun BeaconLegendRow(color: Color, beacon: Beacon, onClick: () -> Unit) {
    val dist = calculateDistance(beacon.rssi, beacon.txPower)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.22f))
            )
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(beacon.id, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(beacon.mac, color = Color(0xFF6A6D78), fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${beacon.rssi} dBm", color = Color.White, fontSize = 12.sp)
            Text("%.1f м".format(dist), color = Color(0xFF8A8A95), fontSize = 11.sp)
        }
    }
}
