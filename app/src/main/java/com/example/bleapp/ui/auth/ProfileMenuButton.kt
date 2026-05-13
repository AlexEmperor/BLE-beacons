package com.example.bleapp.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileMenuButton(
    login: String?,
    isAdmin: Boolean = false,
    onAdmin: () -> Unit = {},
    onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    var open by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFF12141C))
            .border(BorderStroke(1.dp, Color(0xFF1F2330)), CircleShape)
    ) {
        IconButton(onClick = { open = true }, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.AccountCircle, "Профиль", tint = Color(0xFF00E5FF))
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(Color(0xFF161821))
        ) {
            if (!login.isNullOrEmpty()) {
                DropdownMenuItem(
                    text = { Text(login, color = Color(0xFF8A8A95), fontSize = 13.sp) },
                    enabled = false,
                    onClick = {}
                )
            }
            if (isAdmin) {
                DropdownMenuItem(
                    text = { Text("Админ", color = Color(0xFF00FFA3)) },
                    onClick = {
                        open = false
                        onAdmin()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Сменить пароль", color = Color.White) },
                onClick = {
                    open = false
                    onChangePassword()
                }
            )
            DropdownMenuItem(
                text = { Text("Выйти", color = Color(0xFFFF6B6B)) },
                onClick = {
                    open = false
                    onLogout()
                }
            )
        }
    }
}
