package com.magnum.player

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NowPlaying(vm: PlayerViewModel, onClose: () -> Unit) {
    val song = vm.current ?: return
    val ctx = LocalContext.current

    val tint by produceState(Color(0xFF2A1517), song.id) {
        val b = ArtworkLoader.load(ctx, song, 256)
        if (b != null) value = ArtworkLoader.dominantColor(b)
    }
    val animTint by animateColorAsState(tint, tween(800), label = "tint")

    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Slowly drifting, blurred artwork behind everything (like Apple Music's ambient background).
    val ambient = rememberInfiniteTransition(label = "ambient")
    val rot by ambient.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(70000, easing = LinearEasing)),
        label = "rot",
    )
    val drift by ambient.animateFloat(
        initialValue = 1.4f, targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(14000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )

    CompositionLocalProvider(LocalPalette provides DarkPalette) {
    Box(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragY.value.roundToInt()) }
            .background(Brush.verticalGradient(listOf(animTint, Color.Black)))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY.value > 250f) onClose()
                        else scope.launch { dragY.animateTo(0f, spring(0.7f, Spring.StiffnessMedium)) }
                    },
                    onDragCancel = { scope.launch { dragY.animateTo(0f) } },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        scope.launch { dragY.snapTo((dragY.value + dy).coerceAtLeast(0f)) }
                    },
                )
            },
    ) {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            Artwork(
                song,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = rot
                        scaleX = drift
                        scaleY = drift
                        alpha = 0.4f
                    }
                    .blur(70.dp),
                size = 256,
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
            val wide = maxWidth > maxHeight && maxWidth >= 600.dp
            val scale by animateFloatAsState(
                targetValue = if (vm.isPlaying) 1f else 0.84f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                label = "artScale",
            )

            if (wide) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        BigArtwork(song, scale, Modifier.fillMaxWidth(0.9f))
                    }
                    Column(Modifier.weight(1f)) {
                        PlayerControls(vm, song)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .width(40.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.weight(1f))
                    BigArtwork(song, scale, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(32.dp))
                    PlayerControls(vm, song)
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    }
}

@Composable
private fun BigArtwork(song: Song, scale: Float, modifier: Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = 60f
                shape = RoundedCornerShape(14.dp)
                clip = true
            },
    ) {
        Crossfade(targetState = song, animationSpec = tween(500), label = "art") { s ->
            Artwork(s, Modifier.fillMaxSize(), size = 1024)
        }
    }
}

@Composable
private fun PlayerControls(vm: PlayerViewModel, song: Song) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = song,
            transitionSpec = {
                (slideInVertically(tween(380)) { it / 3 } + fadeIn(tween(380))) togetherWith
                    (slideOutVertically(tween(250)) { -it / 3 } + fadeOut(tween(200)))
            },
            label = "meta",
        ) { s ->
            Column(Modifier.fillMaxWidth()) {
                Text(
                    s.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    s.artist, color = Color.White.copy(alpha = 0.65f), fontSize = 18.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (song.lossless || song.isRemote) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (song.isRemote) "30s PREVIEW" else "LOSSLESS \u00B7 ${song.format}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        val progress = if (vm.duration > 0) (vm.position.toFloat() / vm.duration).coerceIn(0f, 1f) else 0f
        Scrubber(progress) { f -> vm.seekTo((f * vm.duration).toLong()) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(vm.position), color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
            Text(formatTime(vm.duration), color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BounceIconButton(
                Icons.Filled.Shuffle, 48.dp, 24.dp,
                tint = if (vm.shuffle) AppleRed else Color.White.copy(alpha = 0.5f),
            ) { vm.toggleShuffle() }
            BounceIconButton(Icons.Filled.SkipPrevious, 64.dp, 40.dp, nudge = (-6).dp) { vm.prev() }
            BounceIconButton(
                if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, 84.dp, 60.dp,
            ) { vm.togglePlay() }
            BounceIconButton(Icons.Filled.SkipNext, 64.dp, 40.dp, nudge = 6.dp) { vm.next() }
            BounceIconButton(
                if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                48.dp, 24.dp,
                tint = if (vm.repeatMode == Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.5f) else AppleRed,
            ) { vm.cycleRepeat() }
        }

        val store = song.storeUrl
        if (song.isRemote && store != null) {
            TextButton(
                onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(store))) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Open full song in Apple Music", color = AppleRed)
            }
        }
    }
}

/** Thin progress bar that thickens while you scrub, like Apple Music. Tap or drag to seek. */
@Composable
private fun Scrubber(progress: Float, onSeek: (Float) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    val seek by rememberUpdatedState(onSeek)
    val haptic = LocalHapticFeedback.current
    val h by animateDpAsState(
        targetValue = if (dragging) 12.dp else 5.dp,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "scrubHeight",
    )
    val shown = if (dragging) dragValue else progress

    Box(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        dragging = true
                        dragValue = (o.x / widthPx).coerceIn(0f, 1f)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragEnd = {
                        seek(dragValue)
                        dragging = false
                    },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragValue = (change.position.x / widthPx).coerceIn(0f, 1f)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { o -> seek((o.x / widthPx).coerceIn(0f, 1f)) }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(h)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(shown.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }
    }
}
