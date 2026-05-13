package com.example.bleapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bleapp.auth.AuthClient
import com.example.bleapp.auth.LoginError
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordDialog(
    token: String,
    onDismiss: () -> Unit,
    onChanged: (newToken: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newRepeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
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
                "Смена пароля",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it; error = null },
                label = { Text("Текущий пароль") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; error = null },
                label = { Text("Новый пароль") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = newRepeat,
                onValueChange = { newRepeat = it; error = null },
                label = { Text("Повтори новый пароль") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp)
            }

            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            oldPassword.isEmpty() || newPassword.isEmpty() ->
                                { error = "Заполни оба поля"; return@Button }
                            newPassword != newRepeat ->
                                { error = "Новые пароли не совпадают"; return@Button }
                            newPassword == oldPassword ->
                                { error = "Новый пароль совпадает со старым"; return@Button }
                        }
                        busy = true
                        error = null
                        scope.launch {
                            val result = runCatching {
                                AuthClient.changePassword(token, oldPassword, newPassword)
                            }
                            busy = false
                            result.onSuccess { onChanged(it.token) }
                                .onFailure { e ->
                                    error = (e as? LoginError)?.message ?: "Ошибка: ${e.message}"
                                }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FFA3),
                        contentColor = Color(0xFF0A0A0F),
                        disabledContainerColor = Color(0xFF1B7A4E),
                        disabledContentColor = Color(0xFF0A0A0F)
                    )
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0A0A0F)
                        )
                    } else {
                        Text("Сменить", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF00E5FF),
    unfocusedBorderColor = Color(0xFF2A2D38),
    focusedLabelColor = Color(0xFF00E5FF),
    unfocusedLabelColor = Color(0xFF8A8A95),
    cursorColor = Color(0xFF00E5FF)
)
