package com.magnum.player

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Artwork that fades in once loaded (local covers, embedded FLAC covers, or iTunes artwork). */
@Composable
fun Artwork(song: Song?, modifier: Modifier = Modifier, size: Int = 256) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    val bmp by produceState<Bitmap?>(null, song?.id, size) {
        value = if (song == null) null else ArtworkLoader.load(ctx, song, size)
    }
    val b = bmp
    val a by animateFloatAsState(if (b != null) 1f else 0f, tween(350), label = "artIn")
    Box(
        modifier.background(Brush.linearGradient(listOf(p.panelHigh, p.panel))),
        contentAlignment = Alignment.Center,
    ) {
        if (b == null) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = p.text.copy(alpha = 0.4f))
        } else {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = a },
            )
        }
    }
}

/**
 * Icon button with: springy press, directional nudge (for skip buttons),
 * haptic tick, tint cross-fade, and a bouncy icon swap (play <-> pause).
 */
@Composable
fun BounceIconButton(
    icon: ImageVector,
    buttonSize: Dp,
    iconSize: Dp,
    tint: Color = Color.Unspecified,
    nudge: Dp = 0.dp,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.8f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press",
    )
    val dx by animateFloatAsState(
        targetValue = if (pressed) nudge.value else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "nudge",
    )
    val resolved = if (tint == Color.Unspecified) LocalPalette.current.text else tint
    val c by animateColorAsState(resolved, tween(250), label = "iconTint")
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = src,
        modifier = Modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = s
                scaleY = s
                translationX = dx * density
            },
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(tween(150))) togetherWith
                    (scaleOut() + fadeOut(tween(120)))
            },
            label = "iconSwap",
        ) { ic ->
            Icon(ic, contentDescription = null, tint = c, modifier = Modifier.size(iconSize))
        }
    }
}

/** Tappable with a subtle spring-down, like rows and cards in Apple Music. */
fun Modifier.pressable(down: Float = 0.97f, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) down else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "pressable",
    )
    this
        .graphicsLayer {
            scaleX = s
            scaleY = s
        }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Staggered fade + rise when an item first appears. */
fun Modifier.appear(index: Int): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(if (index < 12) index * 35L else 0L)
        p.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }
    this.graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 36f
    }
}

/** The little bouncing bars shown on the track that is playing. */
@Composable
fun Equalizer(playing: Boolean, color: Color = Color.White, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    val a by t.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "a",
    )
    val b by t.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse),
        label = "b",
    )
    val c by t.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "c",
    )
    Row(
        modifier.height(16.dp).width(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(a, b, c).forEach { h ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(if (playing) h else 0.25f)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun LargeTitle(
    title: String,
    subtitle: String? = null,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
) {
    val p = LocalPalette.current
    Column(Modifier.padding(padding)) {
        Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = p.text)
        if (subtitle != null) {
            Text(subtitle, fontSize = 14.sp, color = p.text.copy(alpha = 0.55f))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        color = LocalPalette.current.text,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}
