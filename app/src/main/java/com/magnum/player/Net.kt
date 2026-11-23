package com.magnum.player

import java.net.HttpURLConnection
import java.net.URL

object Net {
    fun get(url: String): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "Magnum/0.2")
        try {
            return c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
    }

    fun getString(url: String): String = String(get(url), Charsets.UTF_8)
}
