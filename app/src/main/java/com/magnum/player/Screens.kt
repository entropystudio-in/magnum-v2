package com.magnum.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SongRow(song: Song, index: Int, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .appear(index)
            .pressable(0.98f, onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Artwork(song, Modifier.fillMaxSize(), size = 128)
            if (isCurrent) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) { Equalizer(isPlaying, Color.White) }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) AppleRed else p.text,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${song.artist} \u2022 ${song.album}",
                color = p.text.copy(alpha = 0.55f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (song.lossless || song.isRemote) {
            Text(
                if (song.isRemote) "30s" else song.format,
                color = p.text.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(p.text.copy(alpha = 0.1f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(formatTime(song.durationMs), color = p.text.copy(alpha = 0.45f), fontSize = 13.sp)
    }
}

@Composable
fun SongList(
    songs: List<Song>,
    currentId: Long?,
    playing: Boolean,
    onClick: (Int) -> Unit,
    state: LazyListState = rememberLazyListState(),
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(bottom = 16.dp)) {
        if (header != null) item { header() }
        itemsIndexed(songs, key = { _, s -> s.id }) { i, s ->
            SongRow(s, i, s.id == currentId, playing) { onClick(i) }
        }
    }
}

@Composable
fun SongsScreen(vm: PlayerViewModel) {
    if (vm.loaded && vm.songs.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No music found on this device", color = LocalPalette.current.text.copy(alpha = 0.6f))
        }
    } else {
        SongList(
            songs = vm.songs,
            currentId = vm.current?.id,
            playing = vm.isPlaying,
            onClick = { vm.play(vm.songs, it) },
            header = { LargeTitle("Songs", "${vm.songs.size} songs") },
        )
    }
}

@Composable
fun AlbumsScreen(albums: List<AlbumItem>, onOpen: (AlbumItem) -> Unit) {
    val p = LocalPalette.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LargeTitle("Albums", padding = PaddingValues(vertical = 12.dp))
        }
        itemsIndexed(albums, key = { _, a -> a.id }) { i, a ->
            Column(Modifier.appear(i).pressable(0.95f) { onOpen(a) }) {
                Artwork(
                    a.songs.first(),
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)),
                    size = 512,
                )
                Spacer(Modifier.size(8.dp))
                Text(a.name, color = p.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(a.artist, color = p.text.copy(alpha = 0.55f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun AlbumDetail(album: AlbumItem, vm: PlayerViewModel, onBack: () -> Unit) {
    val p = LocalPalette.current
    val state = rememberLazyListState()
    SongList(
        songs = album.songs,
        currentId = vm.current?.id,
        playing = vm.isPlaying,
        onClick = { vm.play(album.songs, it) },
        state = state,
        header = {
            Column(Modifier.padding(16.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back", tint = AppleRed)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    // Cover shrinks and fades as you scroll, with a little parallax.
                    Artwork(
                        album.songs.first(),
                        Modifier
                            .size(170.dp)
                            .graphicsLayer {
                                val off = if (state.firstVisibleItemIndex == 0) {
                                    state.firstVisibleItemScrollOffset.toFloat()
                                } else 2000f
                                val t = (off / 600f).coerceIn(0f, 1f)
                                scaleX = 1f - 0.3f * t
                                scaleY = scaleX
                                alpha = 1f - t
                                translationY = off * 0.4f
                                shadowElevation = 40f
                                shape = RoundedCornerShape(12.dp)
                                clip = true
                            },
                        size = 512,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(album.name, color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(album.artist, color = AppleRed, fontSize = 16.sp)
                        Text("${album.songs.size} songs", color = p.text.copy(alpha = 0.55f), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.size(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { vm.play(album.songs, 0) },
                        colors = ButtonDefaults.buttonColors(containerColor = p.panelHigh, contentColor = AppleRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                    Button(
                        onClick = { vm.play(album.songs, album.songs.indices.random(), true) },
                        colors = ButtonDefaults.buttonColors(containerColor = p.panelHigh, contentColor = AppleRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Shuffle, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
            }
        },
    )
}

/** Searches your library instantly and Apple Music (iTunes) after a short pause; online hits play 30s previews. */
@Composable
fun SearchScreen(vm: PlayerViewModel) {
    val p = LocalPalette.current
    var q by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(q) {
        delay(400) // debounce typing
        vm.searchOnline(q)
    }

    val local = remember(q, vm.songs) {
        if (q.isBlank()) emptyList()
        else vm.songs.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { LargeTitle("Search") }
        item {
            TextField(
                value = q,
                onValueChange = { q = it },
                singleLine = true,
                placeholder = { Text("Songs, Artists, Albums") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = p.panel,
                    unfocusedContainerColor = p.panel,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AppleRed,
                    focusedTextColor = p.text,
                    unfocusedTextColor = p.text,
                    focusedPlaceholderColor = p.text.copy(alpha = 0.5f),
                    unfocusedPlaceholderColor = p.text.copy(alpha = 0.5f),
                    focusedLeadingIconColor = p.text.copy(alpha = 0.6f),
                    unfocusedLeadingIconColor = p.text.copy(alpha = 0.6f),
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (local.isNotEmpty()) {
            item { SectionTitle("In Your Library") }
            itemsIndexed(local, key = { _, s -> "l${s.id}" }) { i, s ->
                SongRow(s, i, s.id == vm.current?.id, vm.isPlaying) { vm.play(local, i) }
            }
        }

        if (q.isNotBlank()) {
            item { SectionTitle("Apple Music \u2022 30-second previews") }
            if (vm.onlineResults.isEmpty()) {
                item {
                    if (vm.onlineSearching) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppleRed)
                        }
                    } else {
                        Text(
                            "No previews found. Check your internet connection or try another search.",
                            color = p.text.copy(alpha = 0.55f),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                itemsIndexed(vm.onlineResults, key = { _, s -> "o${s.id}" }) { i, s ->
                    SongRow(s, i, s.id == vm.current?.id, vm.isPlaying) { vm.play(vm.onlineResults, i) }
                }
            }
        }
    }
}
