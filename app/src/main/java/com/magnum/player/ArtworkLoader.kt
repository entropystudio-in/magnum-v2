package com.magnum.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtworkLoader {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(ctx: Context, song: Song, size: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = if (song.isRemote) "r${song.id}" else "${song.id}_$size"
        cache.get(key)?.let { return@withContext it }

        var bmp: Bitmap? = null
        val url = song.artworkUrl
        if (url != null) {
            bmp = try {
                val bytes = Net.get(url)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            bmp = try {
                if (Build.VERSION.SDK_INT >= 29) {
                    ctx.contentResolver.loadThumbnail(song.uri, Size(size, size), null)
                } else {
                    ctx.contentResolver.openInputStream(song.albumArtUri)?.use { BitmapFactory.decodeStream(it) }
                }
            } catch (e: Exception) {
                null
            }
            if (bmp == null) {
                // Fallback for files (e.g. FLAC) whose embedded cover the system thumbnailer missed.
                bmp = try {
                    val r = MediaMetadataRetriever()
                    try {
                        r.setDataSource(ctx, song.uri)
                        r.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    } finally {
                        r.release()
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
        if (bmp != null) cache.put(key, bmp)
        bmp
    }

    suspend fun dominantColor(bmp: Bitmap): Color = withContext(Dispatchers.Default) {
        val p = Palette.from(bmp).maximumColorCount(16).generate()
        val fallback = 0xFF2A1517.toInt()
        Color(p.getDarkVibrantColor(p.getDarkMutedColor(fallback)))
    }
}
