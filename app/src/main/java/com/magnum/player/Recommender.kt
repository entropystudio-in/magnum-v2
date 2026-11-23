package com.magnum.player

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Online catalog:
 *  - iTunes Search API (no key): songs, artwork and 30s previews (search + recommendations)
 *  - Deezer public API (no key): related artists
 */
class Recommender {

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private fun parseSongs(body: String): List<Song> {
        val arr = JSONObject(body).optJSONArray("results") ?: return emptyList()
        val out = mutableListOf<Song>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val preview = o.optString("previewUrl")
            if (preview.isEmpty()) continue
            val tid = o.optLong("trackId")
            out += Song(
                id = -tid,
                title = o.optString("trackName"),
                artist = o.optString("artistName"),
                album = o.optString("collectionName"),
                albumId = 0L,
                durationMs = 30000L,
                track = 0,
                uri = Uri.parse(preview),
                format = "PREVIEW",
                artworkUrl = o.optString("artworkUrl100").replace("100x100bb", "600x600bb"),
                storeUrl = o.optString("trackViewUrl").ifEmpty { null },
                isRemote = true,
            )
        }
        return out
    }

    /** Free-text song search (used by the Search tab). */
    suspend fun search(term: String, limit: Int): List<Song> = withContext(Dispatchers.IO) {
        try {
            parseSongs(
                Net.getString(
                    "https://itunes.apple.com/search?term=${enc(term)}&media=music&entity=song&limit=$limit"
                )
            )
        } catch (e: Exception) {
            emptyList<Song>()
        }
    }

    suspend fun itunesSongs(artist: String, limit: Int): List<Song> = withContext(Dispatchers.IO) {
        try {
            parseSongs(
                Net.getString(
                    "https://itunes.apple.com/search?term=${enc(artist)}" +
                        "&media=music&entity=song&attribute=artistTerm&limit=$limit"
                )
            )
        } catch (e: Exception) {
            emptyList<Song>()
        }
    }

    suspend fun relatedArtists(artist: String, limit: Int): List<String> = withContext(Dispatchers.IO) {
        try {
            val s = JSONObject(Net.getString("https://api.deezer.com/search/artist?q=${enc(artist)}&limit=1"))
            val id = s.optJSONArray("data")?.optJSONObject(0)?.optLong("id")
                ?: return@withContext emptyList<String>()
            val r = JSONObject(Net.getString("https://api.deezer.com/artist/$id/related?limit=$limit"))
            val arr = r.optJSONArray("data") ?: return@withContext emptyList<String>()
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.optString("name")?.takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            emptyList<String>()
        }
    }

    suspend fun build(seeds: List<String>, library: List<Song>): List<RecSection> = coroutineScope {
        val have = library.map { norm(it.title) + "|" + norm(it.artist) }.toHashSet()
        fun fresh(l: List<Song>): List<Song> =
            l.filter { (norm(it.title) + "|" + norm(it.artist)) !in have }.distinctBy { it.id }

        val more = seeds.take(3).map { a ->
            async { a to fresh(itunesSongs(a, 15)).take(10) }
        }
        val because = seeds.take(2).map { a ->
            async {
                val rel = relatedArtists(a, 4)
                val lists = rel.map { r -> async { fresh(itunesSongs(r, 4)).take(3) } }.awaitAll()
                a to lists.flatten().distinctBy { it.id }
            }
        }

        val out = mutableListOf<RecSection>()
        for ((a, s) in because.awaitAll()) {
            if (s.isNotEmpty()) out += RecSection("Because you listen to $a", s)
        }
        for ((a, s) in more.awaitAll()) {
            if (s.isNotEmpty()) out += RecSection("More from $a", s)
        }
        out
    }
}
