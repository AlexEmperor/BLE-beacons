package com.example.bleapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bleapp.auth.Jwt
import com.example.bleapp.auth.TokenStore
import com.example.bleapp.data.PlansRepository
import com.example.bleapp.ui.auth.AdminScreen
import com.example.bleapp.ui.auth.ChangePasswordDialog
import com.example.bleapp.ui.auth.ProfileMenuButton
import com.example.bleapp.ui.common.LegendMenuButton
import com.example.bleapp.ui.common.SegmentedTab
import com.example.bleapp.ui.common.SegmentedTabs
import com.example.bleapp.ui.common.TabIconKind
import com.example.bleapp.ui.map.MapScreen
import com.example.bleapp.ui.room.RoomScreen
import com.example.bleapp.ui.scan.ScanScreen
import com.example.bleapp.ui.theme.BgTertiary
import com.example.bleapp.ui.welcome.LoginScreen
import com.example.bleapp.ui.welcome.PlansLoadingScreen
import com.example.bleapp.ui.welcome.SplashScreen
import com.example.bleapp.ui.welcome.WelcomeScreen
import com.example.bleapp.update.AppUpdate
import com.example.bleapp.update.UpdateInstallResult
import com.example.bleapp.update.UpdateManager
import com.example.bleapp.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Stage { Splash, Login, Loading, Welcome, Main }

@Composable
fun App() {
    val vm: MainViewModel = viewModel()
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }
    var stage by remember { mutableStateOf(Stage.Splash) }

    val logout: () -> Unit = {
        if (vm.isScanning.value) vm.toggleScanning()
        tokenStore.clear()
        PlansRepository.reset()
        stage = Stage.Login
    }

    when (stage) {
        Stage.Splash -> SplashScreen(onFinished = {
            stage = if (tokenStore.isTokenAlive()) Stage.Loading else Stage.Login
        })
        Stage.Login -> LoginScreen(onLoggedIn = { stage = Stage.Loading })
        Stage.Loading -> {
            val token = tokenStore.token
            if (token.isNullOrEmpty()) {
                stage = Stage.Login
            } else {
                PlansLoadingScreen(
                    token = token,
                    onLoaded = { stage = Stage.Welcome },
                    onCancel = logout
                )
            }
        }
        Stage.Welcome -> WelcomeScreen(
            onStart = {
                if (!vm.isScanning.value) vm.toggleScanning()
                stage = Stage.Main
            }
        )
        Stage.Main -> MainContent(vm, tokenStore, onLogout = logout)
    }
}

@Composable
private fun MainContent(vm: MainViewModel, tokenStore: TokenStore, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember(context) { UpdateManager(context.applicationContext) }
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var updateBusy by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    val currentLogin = tokenStore.login
    val currentToken = tokenStore.token
    val isAdmin = Jwt.isAdmin(currentToken)
    val allBeacons by vm.allBeacons.collectAsState()
    val currentBeacons by vm.currentBeacons.collectAsState()
    val scanList by vm.scanList.collectAsState()
    val scannerFilter by vm.scannerFilter.collectAsState()
    val geoBeacons by vm.geoBeacons.collectAsState()
    val isScanning by vm.isScanning.collectAsState()
    val showSavedBeacons by vm.showSavedBeacons.collectAsState()
    val userPos by vm.userPos.collectAsState()
    val seeds by vm.currentSeeds.collectAsState()
    val selectedFloor by vm.selectedFloor.collectAsState()
    val locations by PlansRepository.locations.collectAsState()
    val roomSize = remember(selectedFloor.id) { vm.currentFloorMaxMeters() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        SegmentedTab("Сканер", TabIconKind.Scan),
        SegmentedTab("Маяки", TabIconKind.Beacons),
        SegmentedTab("Корпус", TabIconKind.Floor)
    )

    LaunchedEffect(Unit) {
        availableUpdate = withContext(Dispatchers.IO) {
            runCatching { updateManager.checkForUpdate() }.getOrNull()
        }
    }

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

            ProfileMenuButton(
                login = currentLogin,
                isAdmin = isAdmin,
                onAdmin = { showAdmin = true },
                onChangePassword = { showChangePassword = true },
                onLogout = onLogout
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (selectedTab) {
                0 -> ScanScreen(
                    beacons = scanList,
                    isScanning = isScanning,
                    showSavedBeacons = showSavedBeacons,
                    scannerFilter = scannerFilter,
                    onToggleScanning = { vm.toggleScanning() },
                    onToggleSavedBeacons = { vm.toggleSavedBeacons() },
                    onFilterSelected = { vm.setScannerFilter(it) }
                )
                1 -> RoomScreen(currentBeacons, userPos, seeds, roomSize)
                2 -> MapScreen(
                    beacons = currentBeacons,
                    userPos = userPos,
                    seeds = seeds,
                    selectedFloor = selectedFloor,
                    geoBeacons = geoBeacons,
                    locations = locations,
                    onFloorSelected = { vm.selectFloor(it) }
                )
            }
        }
    }

    if (showChangePassword) {
        val token = tokenStore.token
        if (token == null) {
            showChangePassword = false
            onLogout()
        } else {
            ChangePasswordDialog(
                token = token,
                onDismiss = { showChangePassword = false },
                onChanged = { newToken ->
                    tokenStore.token = newToken
                    showChangePassword = false
                }
            )
        }
    }

    if (showAdmin) {
        val token = tokenStore.token
        if (token == null || !isAdmin) {
            showAdmin = false
        } else {
            AdminScreen(
                token = token,
                onClose = { showAdmin = false }
            )
        }
    }

    availableUpdate?.let { update ->
        UpdateDialog(
            update = update,
            busy = updateBusy,
            status = updateStatus,
            onDismiss = { availableUpdate = null },
            onOpenRelease = { updateManager.openRelease(update) },
            onInstall = {
                updateBusy = true
                updateStatus = "Скачиваем APK..."
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching { updateManager.downloadAndInstall(update) }
                    }
                    updateBusy = false
                    result.onSuccess { installResult ->
                        when (installResult) {
                            UpdateInstallResult.Started -> {
                                updateStatus = null
                                availableUpdate = null
                            }
                            UpdateInstallResult.InstallPermissionRequired -> {
                                updateStatus = "Разреши установку из этого источника и нажми обновить ещё раз."
                            }
                        }
                    }.onFailure {
                        updateStatus = "Не удалось скачать обновление. Можно открыть релиз вручную."
                    }
                }
            }
        )
    }
}

@Composable
private fun UpdateDialog(
    update: AppUpdate,
    busy: Boolean,
    status: String?,
    onDismiss: () -> Unit,
    onOpenRelease: () -> Unit,
    onInstall: () -> Unit
) {
    Dialog(onDismissRequest = {
        if (!busy) onDismiss()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF161821))
                .border(1.dp, Color(0xFF2A2D38), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                "Хорошие новости!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${update.message}\nВерсия ${update.versionName} доступна для установки.",
                color = Color(0xFFB8BDC9),
                fontSize = 14.sp
            )
            status?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Color(0xFF00E5FF), fontSize = 13.sp)
            }
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onInstall,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Обновить")
                    }
                }
                OutlinedButton(
                    onClick = onOpenRelease,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Открыть GitHub")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Позже")
                }
            }
        }
    }
}
