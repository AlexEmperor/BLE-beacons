package com.example.bleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.bleapp.ui.App
import com.example.bleapp.ui.theme.BleAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BleAppTheme {
                App()
            }
        }
    }
}