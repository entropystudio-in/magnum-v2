package com.magnum.player

import android.content.ContentUris
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val track: Int,
    val uri: Uri,
    val format: String = "",
    val artworkUrl: String? = null,
    val storeUrl: String? = null,
    val isRemote: Boolean = false,
) {
    val lossless: Boolean
        get() = format in setOf("FLAC", "WAV", "AIFF", "ALAC")

    val albumArtUri: Uri
        get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(if (artworkUrl != null) Uri.parse(artworkUrl) else albumArtUri)
                .build()
        )
        .build()
}

data class AlbumItem(
    val id: Long,
    val name: String,
    val artist: String,
    val songs: List<Song>,
)

data class RecSection(val title: String, val songs: List<Song>)

fun buildAlbums(songs: List<Song>): List<AlbumItem> =
    songs.groupBy { it.albumId }.values.map { list ->
        val sorted = list.sortedBy { it.track }
        AlbumItem(sorted.first().albumId, sorted.first().album, sorted.first().artist, sorted)
    }.sortedBy { it.name.lowercase() }

fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
