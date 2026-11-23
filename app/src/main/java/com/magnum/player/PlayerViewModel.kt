package com.magnum.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository(app)
    private val recommender = Recommender()
    private val prefs = app.getSharedPreferences("plays", Context.MODE_PRIVATE)
    private val remote = HashMap<Long, Song>()
    private var lastCountedId: Long? = null
    private var controller: MediaController? = null

    var songs by mutableStateOf<List<Song>>(emptyList()); private set
    var loaded by mutableStateOf(false); private set
    var current by mutableStateOf<Song?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var position by mutableLongStateOf(0L); private set
    var duration by mutableLongStateOf(0L); private set
    var shuffle by mutableStateOf(false); private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF); private set

    var recs by mutableStateOf<List<RecSection>>(emptyList()); private set
    var recsLoading by mutableStateOf(false); private set
    var recsMessage by mutableStateOf<String?>(null); private set

    var onlineResults by mutableStateOf<List<Song>>(emptyList()); private set
    var onlineSearching by mutableStateOf(false); private set
    private var searchJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                sync(c)
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            while (true) {
                delay(250)
                controller?.let {
                    if (it.isPlaying) position = it.currentPosition
                    duration = it.duration.coerceAtLeast(0L)
                }
            }
        }
    }

    private fun sync(p: Player) {
        isPlaying = p.isPlaying
        shuffle = p.shuffleModeEnabled
        repeatMode = p.repeatMode
        val id = p.currentMediaItem?.mediaId?.toLongOrNull()
        val s = if (id == null) null else (songs.firstOrNull { it.id == id } ?: remote[id])
        if (s != null && id != lastCountedId) {
            lastCountedId = id
            if (!s.isRemote) countPlay(s)
        }
        current = s
        position = p.currentPosition
    }

    private fun countPlay(s: Song) {
        val k = "a:${s.artist}"
        prefs.edit().putInt(k, prefs.getInt(k, 0) + 1).apply()
    }

    fun load() {
        viewModelScope.launch {
            songs = repo.loadSongs()
            loaded = true
            controller?.let { sync(it) }
        }
    }

    // ---- recommendations ----

    private fun seedArtists(): List<String> {
        val skip = "Unknown Artist"
        val now = current?.takeIf { !it.isRemote }?.artist
        val fromPlays = prefs.all
            .filterKeys { it.startsWith("a:") }
            .mapNotNull { (k, v) -> (v as? Int)?.let { k.removePrefix("a:") to it } }
            .sortedByDescending { it.second }
            .map { it.first }
        val fromLibrary = songs.groupingBy { it.artist }.eachCount().entries
            .sortedByDescending { it.value }
            .map { it.key }
        return (listOfNotNull(now) + fromPlays + fromLibrary)
            .filter { it != skip }
            .distinct()
            .take(3)
    }

    fun refreshRecs() {
        if (recsLoading) return
        viewModelScope.launch {
            recsLoading = true
            recsMessage = null
            val seeds = seedArtists()
            if (seeds.isEmpty()) {
                recs = emptyList()
                recsMessage = "Play some music and Magnum will start recommending."
            } else {
                val r = recommender.build(seeds, songs)
                r.forEach { sec -> sec.songs.forEach { remote[it.id] = it } }
                recs = r
                if (r.isEmpty()) {
                    recsMessage = "Couldn't reach the music catalog. Check your connection and try again."
                }
            }
            recsLoading = false
        }
    }

    // ---- online search (30s previews) ----

    fun searchOnline(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.isEmpty()) {
            onlineResults = emptyList()
            onlineSearching = false
            return
        }
        searchJob = viewModelScope.launch {
            onlineSearching = true
            val r = recommender.search(q, 25)
            r.forEach { remote[it.id] = it }
            onlineResults = r
            onlineSearching = false
        }
    }

    // ---- playback ----

    fun play(list: List<Song>, index: Int, shuffleOn: Boolean = false) {
        val c = controller ?: return
        if (list.isEmpty()) return
        list.forEach { if (it.isRemote) remote[it.id] = it }
        c.shuffleModeEnabled = shuffleOn
        c.setMediaItems(list.map { it.toMediaItem() }, index, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun prev() {
        controller?.let {
            if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem()
        }
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        position = ms
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }
}
