package com.magnum.player

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        // FLAC files are included even if the scanner did not flag them as "music".
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
            "${MediaStore.Audio.Media.MIME_TYPE} IN ('audio/flac','audio/x-flac')) AND " +
            "${MediaStore.Audio.Media.DURATION} >= 30000"
        val order = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        val out = mutableListOf<Song>()

        context.contentResolver.query(collection, projection, selection, null, order)?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            while (c.moveToNext()) {
                val sid = c.getLong(iId)
                val name = c.getString(iName) ?: ""
                out += Song(
                    id = sid,
                    title = c.getString(iTitle) ?: "Unknown",
                    artist = c.getString(iArtist)?.takeUnless { it == "<unknown>" } ?: "Unknown Artist",
                    album = c.getString(iAlbum) ?: "Unknown Album",
                    albumId = c.getLong(iAlbumId),
                    durationMs = c.getLong(iDur),
                    track = c.getInt(iTrack),
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, sid),
                    format = name.substringAfterLast('.', "").uppercase(),
                )
            }
        }
        out
    }
}
