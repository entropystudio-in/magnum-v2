package com.magnum.player

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM") ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
    ); private set
    var fontFamily by mutableStateOf<FontFamily?>(null); private set
    var fontName by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set

    init {
        val path = prefs.getString("font_path", null)
        if (path != null) {
            val f = File(path)
            if (f.exists() && isUsableFont(f)) {
                fontFamily = FontFamily(Font(f))
                fontName = prefs.getString("font_name", f.name)
            } else {
                prefs.edit().remove("font_path").remove("font_name").apply()
            }
        }
    }

    fun setTheme(m: ThemeMode) {
        themeMode = m
        prefs.edit().putString("theme", m.name).apply()
    }

    fun clearMessage() {
        message = null
    }

    fun importFont(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val saved: Pair<File, String>? = withContext(Dispatchers.IO) {
                try {
                    val name = ctx.contentResolver
                        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                        ?: "Custom font"
                    val dir = File(ctx.filesDir, "fonts").apply { mkdirs() }
                    val out = File(dir, "font_${System.currentTimeMillis()}.ttf")
                    val input = ctx.contentResolver.openInputStream(uri) ?: return@withContext null
                    input.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
                    if (isUsableFont(out)) {
                        out to name
                    } else {
                        out.delete()
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (saved == null) {
                message = "Couldn't use that file. Choose a .ttf or .otf font."
                return@launch
            }
            val (file, name) = saved
            prefs.getString("font_path", null)?.let { File(it).delete() }
            prefs.edit().putString("font_path", file.absolutePath).putString("font_name", name).apply()
            fontFamily = FontFamily(Font(file))
            fontName = name
            message = "Font applied: $name"
        }
    }

    fun resetFont() {
        prefs.getString("font_path", null)?.let { File(it).delete() }
        prefs.edit().remove("font_path").remove("font_name").apply()
        fontFamily = null
        fontName = null
        message = "Font reset to default"
    }

    private fun isUsableFont(f: File): Boolean {
        return try {
            val b = ByteArray(4)
            val n = f.inputStream().use { it.read(b) }
            if (n < 4) return false
            val tag = String(b, Charsets.ISO_8859_1)
            val magicOk = (b[0].toInt() == 0 && b[1].toInt() == 1 && b[2].toInt() == 0 && b[3].toInt() == 0) ||
                tag == "OTTO" || tag == "true"
            if (!magicOk) return false
            if (Build.VERSION.SDK_INT >= 29) {
                android.graphics.fonts.Font.Builder(f).build() // throws if the file is corrupt
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
