package com.example.bleapp.ui.welcome

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleapp.auth.AuthClient
import com.example.bleapp.auth.LoginError
import com.example.bleapp.auth.TokenStore
import com.example.bleapp.ui.theme.BgPrimary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenStore = remember(context) { TokenStore(context) }

    var mode by remember { mutableStateOf(AuthMode.Login) }
    var login by remember { mutableStateOf(tokenStore.login.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var passwordRepeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val isRegister = mode == AuthMode.Register

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF161821))
                .border(1.dp, Color(0xFF2A2D38), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isRegister) "Регистрация" else "Авторизация",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (isRegister) "Придумай логин и пароль (от 6 символов)"
                else "Введите логин и пароль для входа",
                color = Color(0xFF8A8A95),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = login,
                onValueChange = { login = it; error = null },
                label = { Text("Логин") },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Пароль") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            if (isRegister) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = passwordRepeat,
                    onValueChange = { passwordRepeat = it; error = null },
                    label = { Text("Повтори пароль") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )
            }

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val l = login.trim()
                    val p = password
                    when {
                        l.isEmpty() || p.isEmpty() -> { error = "Заполни логин и пароль"; return@Button }
                        isRegister && p != passwordRepeat -> { error = "Пароли не совпадают"; return@Button }
                    }
                    busy = true
                    error = null
                    scope.launch {
                        val result = runCatching {
                            if (isRegister) AuthClient.register(l, p)
                            else AuthClient.login(l, p)
                        }
                        busy = false
                        result.onSuccess { res ->
                            tokenStore.token = res.token
                            tokenStore.login = res.login.ifEmpty { l }
                            onLoggedIn()
                        }.onFailure { e ->
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
                    Text(
                        if (isRegister) "Зарегистрироваться" else "Войти",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isRegister) "Уже есть аккаунт?" else "Нет аккаунта?",
                    color = Color(0xFF8A8A95),
                    fontSize = 13.sp
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    if (isRegister) "Войти" else "Регистрация",
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = !busy) {
                        mode = if (isRegister) AuthMode.Login else AuthMode.Register
                        passwordRepeat = ""
                        error = null
                    }
                )
            }
        }
    }
}

private enum class AuthMode { Login, Register }

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
