package com.example.bleapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bleapp.ui.common.LegendMenuButton
import com.example.bleapp.ui.common.SegmentedTab
import com.example.bleapp.ui.common.SegmentedTabs
import com.example.bleapp.ui.common.TabIconKind
import com.example.bleapp.ui.map.MapScreen
import com.example.bleapp.ui.room.RoomScreen
import com.example.bleapp.ui.scan.ScanScreen
import com.example.bleapp.ui.theme.BgTertiary
import com.example.bleapp.ui.welcome.SplashScreen
import com.example.bleapp.ui.welcome.WelcomeScreen
import com.example.bleapp.viewmodel.MainViewModel

private enum class Stage { Splash, Welcome, Main }

@Composable
fun App() {
    val vm: MainViewModel = viewModel()
    var stage by remember { mutableStateOf(Stage.Splash) }

    when (stage) {
        Stage.Splash -> SplashScreen(onFinished = { stage = Stage.Welcome })
        Stage.Welcome -> WelcomeScreen(
            onStart = {
                if (!vm.isScanning.value) vm.toggleScanning()
                stage = Stage.Main
            }
        )
        Stage.Main -> MainContent(vm)
    }
}

@Composable
private fun MainContent(vm: MainViewModel) {
    val allBeacons by vm.allBeacons.collectAsState()
    val currentBeacons by vm.currentBeacons.collectAsState()
    val isScanning by vm.isScanning.collectAsState()
    val userPos by vm.userPos.collectAsState()
    val seeds by vm.currentSeeds.collectAsState()
    val selectedFloor by vm.selectedFloor.collectAsState()
    val roomSize = remember(selectedFloor.id) { vm.currentFloorMaxMeters() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        SegmentedTab("Сканер", TabIconKind.Scan),
        SegmentedTab("Маяки", TabIconKind.Beacons),
        SegmentedTab("Помещение", TabIconKind.Floor)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgTertiary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SegmentedTabs(
                tabs = tabs,
                selectedIndex = selectedTab,
                onSelected = { selectedTab = it },
                modifier = Modifier.weight(1f),
                scanActive = isScanning
            )

            LegendMenuButton(allBeacons, vm.allSeeds)
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (selectedTab) {
                0 -> ScanScreen(allBeacons, isScanning) { vm.toggleScanning() }
                1 -> RoomScreen(currentBeacons, userPos, seeds, roomSize)
                2 -> MapScreen(currentBeacons, userPos, seeds, selectedFloor) { vm.selectFloor(it) }
            }
        }
    }
}
