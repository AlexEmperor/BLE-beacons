package com.example.bleapp.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.auth.AdminClient
import com.example.bleapp.auth.AdminState
import com.example.bleapp.auth.AdminUser
import com.example.bleapp.ui.theme.BgPrimary
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(
    token: String,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<AdminState?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedUser by remember { mutableStateOf<AdminUser?>(null) }

    LaunchedEffect(Unit) {
        runCatching { AdminClient.state(token) }
            .onSuccess {
                state = it
                loadError = null
            }
            .onFailure { loadError = it.message ?: "Не удалось загрузить" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (selectedUser != null) selectedUser = null else onClose()
                }) {
                    Icon(Icons.Default.ArrowBack, "Назад", tint = Color.White)
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    if (selectedUser == null) "Доступы пользователей"
                    else "Доступ: ${selectedUser?.login.orEmpty()}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            when {
                loadError != null -> ErrorBox(loadError ?: "")
                state == null -> LoadingBox()
                selectedUser == null -> UsersList(
                    users = state!!.users,
                    onUserClick = { selectedUser = it }
                )
                else -> UserAccessList(
                    user = selectedUser!!,
                    state = state!!,
                    onToggle = { locCode, granted ->
                        val user = selectedUser ?: return@UserAccessList
                        // Optimistic update + rollback в случае ошибки.
                        val prev = state!!
                        val grants = prev.grants.toMutableList()
                        if (granted) {
                            if (grants.none { it.userId == user.id && it.locationCode == locCode }) {
                                grants.add(com.example.bleapp.auth.AdminGrant(user.id, locCode))
                            }
                        } else {
                            grants.removeAll { it.userId == user.id && it.locationCode == locCode }
                        }
                        state = prev.copy(grants = grants)
                        scope.launch {
                            val result = runCatching {
                                if (granted) AdminClient.grant(token, user.id, locCode)
                                else AdminClient.revoke(token, user.id, locCode)
                            }
                            if (result.isFailure) {
                                state = prev  // откат
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF00E5FF))
    }
}

@Composable
private fun ErrorBox(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color(0xFFFF6B6B), fontSize = 13.sp)
    }
}

@Composable
private fun UsersList(users: List<AdminUser>, onUserClick: (AdminUser) -> Unit) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет пользователей", color = Color(0xFF8A8A95), fontSize = 13.sp)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users, key = { it.id }) { u ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161821))
                    .clickable { onUserClick(u) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(u.login, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        if (u.role == "admin") "admin" else "user",
                        color = if (u.role == "admin") Color(0xFF00FFA3) else Color(0xFF8A8A95),
                        fontSize = 12.sp
                    )
                }
                Icon(Icons.Default.KeyboardArrowRight, null, tint = Color(0xFF8A8A95))
            }
        }
    }
}

@Composable
private fun UserAccessList(
    user: AdminUser,
    state: AdminState,
    onToggle: (locationCode: String, granted: Boolean) -> Unit
) {
    val grantedSet = remember(state.grants, user.id) {
        state.grants.asSequence()
            .filter { it.userId == user.id }
            .map { it.locationCode }
            .toHashSet()
    }
    val isAdmin = user.role == "admin"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.locations, key = { it.code }) { loc ->
            val effectiveGranted = isAdmin || loc.isPublic || loc.code in grantedSet
            val toggleable = !isAdmin && !loc.isPublic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161821))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = effectiveGranted,
                    enabled = toggleable,
                    onCheckedChange = { newValue -> onToggle(loc.code, newValue) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF00FFA3),
                        uncheckedColor = Color(0xFF8A8A95),
                        checkmarkColor = Color(0xFF0A0A0F)
                    )
                )
                Spacer(Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(loc.name, color = Color.White, fontSize = 14.sp)
                    Text(
                        when {
                            isAdmin -> "admin: всё"
                            loc.isPublic -> "публичная — у всех"
                            else -> loc.code
                        },
                        color = Color(0xFF8A8A95),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
