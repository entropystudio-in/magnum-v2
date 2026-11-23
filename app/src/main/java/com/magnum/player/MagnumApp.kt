package com.magnum.player

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Songs", Icons.Filled.LibraryMusic),
    TabItem("Albums", Icons.Filled.Album),
    TabItem("For You", Icons.Filled.PlayCircle),
    TabItem("Search", Icons.Filled.Search),
    TabItem("Settings", Icons.Filled.Settings),
)

private val Gold = Color(0xFFC9A24D)

@Composable
fun MagnumApp(vm: PlayerViewModel, settings: SettingsViewModel) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    val view = LocalView.current

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openAlbumId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }

    // Status/navigation bar icons follow the app theme (the Now Playing screen is always dark).
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val c = WindowCompat.getInsetsController(window, view)
            val lightBars = !(p.isDark || showPlayer)
            c.isAppearanceLightStatusBars = lightBars
            c.isAppearanceLightNavigationBars = lightBars
        }
    }

    val audioPerm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE
    val perms = if (Build.VERSION.SDK_INT >= 33) arrayOf(audioPerm, Manifest.permission.POST_NOTIFICATIONS)
    else arrayOf(audioPerm)

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, audioPerm) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        granted = res[audioPerm] == true
    }
    LaunchedEffect(granted) {
        if (granted) vm.load() else launcher.launch(perms)
    }

    if (!granted) {
        Column(
            Modifier.fillMaxSize().background(p.bg).systemBarsPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Magnum", color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Allow access to your music to get started.", color = p.text.copy(alpha = 0.6f))
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { launcher.launch(perms) },
                colors = ButtonDefaults.buttonColors(containerColor = AppleRed, contentColor = Color.White),
            ) { Text("Allow access") }
        }
        return
    }

    val albums = remember(vm.songs) { buildAlbums(vm.songs) }

    BackHandler(enabled = showPlayer) { showPlayer = false }
    BackHandler(enabled = !showPlayer && openAlbumId != null) { openAlbumId = null }

    BoxWithConstraints(Modifier.fillMaxSize().background(p.bg)) {
        val wide = maxWidth >= 600.dp

        Row(Modifier.fillMaxSize().systemBarsPadding()) {
            if (wide) {
                Sidebar(tab) { tab = it; openAlbumId = null }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) },
                        label = "tabs",
                    ) { t ->
                        when (t) {
                            0 -> SongsScreen(vm)
                            1 -> AnimatedContent(
                                targetState = openAlbumId,
                                transitionSpec = {
                                    if (targetState != null) {
                                        (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(320))) togetherWith
                                            (slideOutHorizontally(tween(320)) { -it / 6 } + fadeOut(tween(200)))
                                    } else {
                                        (slideInHorizontally(tween(320)) { -it / 4 } + fadeIn(tween(320))) togetherWith
                                            (slideOutHorizontally(tween(320)) { it / 4 } + fadeOut(tween(200)))
                                    }
                                },
                                label = "albums",
                            ) { id ->
                                val album = albums.firstOrNull { it.id == id }
                                if (album != null) AlbumDetail(album, vm) { openAlbumId = null }
                                else AlbumsScreen(albums) { openAlbumId = it.id }
                            }
                            2 -> ForYouScreen(vm)
                            3 -> SearchScreen(vm)
                            else -> SettingsScreen(settings)
                        }
                    }
                }
                AnimatedVisibility(
                    visible = vm.current != null,
                    enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    MiniPlayer(vm) { showPlayer = true }
                }
                if (!wide) {
                    BottomBar(tab) { tab = it; openAlbumId = null }
                }
            }
        }

        AnimatedVisibility(
            visible = showPlayer && vm.current != null,
            enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { it },
            exit = slideOutVertically(tween(300)) { it },
        ) {
            NowPlaying(vm) { showPlayer = false }
        }
    }
}

@Composable
private fun MiniPlayer(vm: PlayerViewModel, onOpen: () -> Unit) {
    val song = vm.current ?: return
    val p = LocalPalette.current
    var dx by remember { mutableFloatStateOf(0f) }
    val artScale by animateFloatAsState(
        targetValue = if (vm.isPlaying) 1f else 0.88f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "miniArt",
    )
    val progress = if (vm.duration > 0) (vm.position.toFloat() / vm.duration).coerceIn(0f, 1f) else 0f

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(p.panelHigh)
            .clickable(onClick = onOpen)
            // swipe left/right on the mini player to skip, like iOS
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dx < -80f) vm.next() else if (dx > 80f) vm.prev()
                        dx = 0f
                    },
                    onDragCancel = { dx = 0f },
                    onHorizontalDrag = { change, d ->
                        change.consume()
                        dx += d
                    },
                )
            },
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(
                song,
                Modifier
                    .size(44.dp)
                    .graphicsLayer { scaleX = artScale; scaleY = artScale }
                    .clip(RoundedCornerShape(6.dp)),
                size = 128,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = p.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = p.text.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BounceIconButton(
                if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, 44.dp, 30.dp,
            ) { vm.togglePlay() }
            BounceIconButton(Icons.Filled.SkipNext, 44.dp, 30.dp, nudge = 5.dp) { vm.next() }
        }
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(2.dp)
                .background(AppleRed)
        )
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    NavigationBar(containerColor = p.panel, windowInsets = WindowInsets(0)) {
        tabs.forEachIndexed { i, t ->
            NavigationBarItem(
                selected = selected == i,
                onClick = { onSelect(i) },
                icon = {
                    val sc by animateFloatAsState(
                        targetValue = if (selected == i) 1.2f else 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                        label = "tabScale",
                    )
                    Icon(
                        t.icon,
                        contentDescription = t.label,
                        modifier = Modifier.graphicsLayer { scaleX = sc; scaleY = sc },
                    )
                },
                label = { Text(t.label, fontSize = 11.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppleRed,
                    selectedTextColor = AppleRed,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = p.text.copy(alpha = 0.5f),
                    unselectedTextColor = p.text.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

@Composable
private fun Sidebar(selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(p.sidebar)
            .padding(16.dp),
    ) {
        Text(
            "Magnum", color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp),
        )
        Spacer(Modifier.height(16.dp))
        tabs.forEachIndexed { i, t ->
            val sel = selected == i
            val bg by animateColorAsState(if (sel) p.panelHigh else Color.Transparent, tween(250), label = "sideBg")
            val fg by animateColorAsState(if (sel) p.text else p.text.copy(alpha = 0.7f), tween(250), label = "sideFg")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(t.icon, contentDescription = null, tint = if (sel) AppleRed else fg)
                Spacer(Modifier.width(14.dp))
                Text(t.label, color = fg, fontSize = 16.sp)
            }
        }
    }
}
