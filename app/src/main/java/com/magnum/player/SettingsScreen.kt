package com.magnum.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(settings: SettingsViewModel) {
    val p = LocalPalette.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) settings.importFont(uri)
    }
    LaunchedEffect(settings.message) {
        if (settings.message != null) {
            delay(3500)
            settings.clearMessage()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        LargeTitle("Settings")

        SectionTitle("Appearance")
        ThemeSegmented(settings.themeMode) { settings.setTheme(it) }

        SectionTitle("Font")
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(p.panel)
                .padding(16.dp),
        ) {
            Text(
                settings.fontName ?: "System default",
                color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                "The quick brown fox jumps over the lazy dog",
                color = p.text.copy(alpha = 0.7f), fontSize = 15.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { picker.launch(arrayOf("*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = AppleRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Import font (.ttf / .otf)") }
            if (settings.fontName != null) {
                TextButton(onClick = { settings.resetFont() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Reset to default", color = AppleRed)
                }
            }
        }
        Text(
            "The imported font is used everywhere in Magnum.",
            color = p.text.copy(alpha = 0.45f), fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        settings.message?.let {
            Text(it, color = AppleRed, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ThemeSegmented(mode: ThemeMode, onChange: (ThemeMode) -> Unit) {
    val p = LocalPalette.current
    val options = listOf(
        ThemeMode.SYSTEM to "System",
        ThemeMode.LIGHT to "Light",
        ThemeMode.DARK to "Dark",
    )
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.panel)
            .padding(4.dp),
    ) {
        options.forEach { (m, label) ->
            val sel = m == mode
            val bg by animateColorAsState(if (sel) AppleRed else Color.Transparent, tween(250), label = "seg")
            val fg by animateColorAsState(
                if (sel) Color.White else p.text.copy(alpha = 0.7f), tween(250), label = "segFg",
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .pressable(0.96f) { onChange(m) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = fg, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
