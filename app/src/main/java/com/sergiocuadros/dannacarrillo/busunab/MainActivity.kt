package com.sergiocuadros.dannacarrillo.busunab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sergiocuadros.dannacarrillo.busunab.ui.theme.DriverUnabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DriverUnabTheme {
                NavigationApp()
            }
        }
    }
}

