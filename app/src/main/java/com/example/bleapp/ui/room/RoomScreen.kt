package com.example.bleapp.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.example.bleapp.data.Beacon
import com.example.bleapp.data.BeaconSeed
import com.example.bleapp.ui.theme.BgPrimary

@Composable
fun RoomScreen(
    beacons: List<Beacon>,
    userPos: Offset,
    seeds: List<BeaconSeed>,
    roomSizeM: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        PositionView(beacons, userPos, seeds, roomSizeM)
    }
}
