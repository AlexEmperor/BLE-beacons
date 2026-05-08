package com.example.bleapp.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.data.Beacon
import com.example.bleapp.data.BeaconSeed
import com.example.bleapp.data.PlanFloor
import com.example.bleapp.data.PlanLocation
import com.example.bleapp.data.planLocations
import com.example.bleapp.ui.theme.BgPrimary
import com.example.bleapp.ui.theme.BgSecondary

@Composable
fun MapScreen(
    beacons: List<Beacon>,
    userPos: Offset,
    seeds: List<BeaconSeed>,
    selectedFloor: PlanFloor,
    onFloorSelected: (PlanFloor) -> Unit
) {
    val location: PlanLocation = remember(selectedFloor.id) {
        planLocations.first { loc -> loc.floors.any { it.id == selectedFloor.id } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DropdownSelector(
                label = "Корпус",
                value = location.name,
                options = planLocations.map { it.name },
                onSelected = { idx ->
                    val loc = planLocations[idx]
                    if (loc.id != location.id) onFloorSelected(loc.floors.first())
                },
                modifier = Modifier.weight(1f)
            )
            DropdownSelector(
                label = "Этаж",
                value = selectedFloor.name,
                options = location.floors.map { it.name },
                onSelected = { idx -> onFloorSelected(location.floors[idx]) },
                modifier = Modifier.weight(1f),
                enabled = location.floors.size > 1
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgSecondary)
                .weight(1f)
        ) {
            PlanView(selectedFloor, beacons, userPos, seeds)
        }
    }
}

@Composable
private fun DropdownSelector(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF12141C))
                .border(1.dp, Color(0xFF1F2330), RoundedCornerShape(10.dp))
                .clickable(enabled = enabled && options.isNotEmpty()) { open = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(label, color = Color(0xFF6A6D78), fontSize = 11.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    value,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    null,
                    tint = if (enabled) Color(0xFF00E5FF) else Color(0xFF3A3D48)
                )
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(Color(0xFF161821))
        ) {
            options.forEachIndexed { idx, name ->
                DropdownMenuItem(
                    text = { Text(name, color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        open = false
                        onSelected(idx)
                    }
                )
            }
        }
    }
}
