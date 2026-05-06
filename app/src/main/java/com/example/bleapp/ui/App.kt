package com.example.bleapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bleapp.ui.map.MapScreen
import com.example.bleapp.ui.room.RoomScreen
import com.example.bleapp.ui.scan.ScanScreen
import com.example.bleapp.ui.theme.BgSecondary
import com.example.bleapp.ui.theme.BgTertiary
import com.example.bleapp.viewmodel.MainViewModel

@Composable
fun App() {
    val vm: MainViewModel = viewModel()

    val beacons by vm.beacons.collectAsState()
    val isScanning by vm.isScanning.collectAsState()
    val userPos by vm.userPos.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Сканирование", "Позиция", "Карта")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgTertiary)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BgSecondary,
            contentColor = Color(0xFF00E5FF)
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == i) Color(0xFF00E5FF) else Color.Gray
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> ScanScreen(beacons, isScanning) { vm.toggleScanning() }
            1 -> RoomScreen(beacons, userPos)
            2 -> MapScreen(beacons, userPos)
        }
    }
}
