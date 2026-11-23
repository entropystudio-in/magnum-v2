package com.magnum.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: PlayerViewModel by viewModels()
    private val settings: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MagnumTheme(settings.themeMode, settings.fontFamily) {
                MagnumApp(vm, settings)
            }
        }
    }
}
