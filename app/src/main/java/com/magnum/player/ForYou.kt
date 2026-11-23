package com.magnum.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ForYouScreen(vm: PlayerViewModel) {
    val p = LocalPalette.current
    LaunchedEffect(vm.loaded) {
        if (vm.loaded && vm.recs.isEmpty() && !vm.recsLoading) vm.refreshRecs()
    }
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { LargeTitle("For You", "Based on what you play") }
            IconButton(onClick = { vm.refreshRecs() }, modifier = Modifier.padding(end = 8.dp)) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = AppleRed,
                    modifier = Modifier.rotate(if (vm.recsLoading) angle else 0f),
                )
            }
        }

        if (vm.recsLoading && vm.recs.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppleRed)
            }
        }
        vm.recsMessage?.let {
            Text(it, color = p.text.copy(alpha = 0.6f), modifier = Modifier.padding(16.dp))
        }

        vm.recs.forEach { sec -> RecRow(sec, vm) }

        if (vm.recs.isNotEmpty()) {
            Text(
                "Tracks here are 30-second previews. Catalog data from iTunes Search and Deezer.",
                color = p.text.copy(alpha = 0.35f),
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RecRow(sec: RecSection, vm: PlayerViewModel) {
    val p = LocalPalette.current
    Text(
        sec.title,
        color = p.text,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 10.dp),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(sec.songs, key = { _, s -> s.id }) { i, s ->
            Column(
                Modifier
                    .width(150.dp)
                    .appear(i)
                    .pressable(0.95f) { vm.play(sec.songs, i) }
            ) {
                Artwork(s, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)), size = 600)
                Spacer(Modifier.height(8.dp))
                Text(
                    s.title,
                    color = if (vm.current?.id == s.id) AppleRed else p.text,
                    fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    s.artist,
                    color = p.text.copy(alpha = 0.55f),
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
