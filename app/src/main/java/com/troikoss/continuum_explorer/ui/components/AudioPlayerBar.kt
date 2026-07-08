package com.troikoss.continuum_explorer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import com.troikoss.continuum_explorer.managers.AudioManager

@Composable
fun AudioPlayerBar(modifier: Modifier = Modifier) {
    val currentTrack = AudioManager.currentTrack
    val isPlaying = AudioManager.isAudioPlaying
    val position = AudioManager.currentPosition
    val duration = AudioManager.duration
    val playlist = AudioManager.playlist
    val currentIndex = AudioManager.currentIndex
    val context = LocalContext.current

    var isExpanded by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    // Calculate max height by subtracting bottom constraints so it doesn't overflow the top
    val bottomClearance = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) 84.dp else 16.dp
    val maxHeight = configuration.screenHeightDp.dp - statusBarHeight - navBarHeight - bottomClearance - 23.dp
    
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) maxHeight.coerceAtLeast(120.dp) else 120.dp,
        label = "PillHeight"
    )

    AnimatedVisibility(
        visible = currentTrack != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(animatedHeight),
            shape = RoundedCornerShape(60.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Expanded Playlist View
                if (isExpanded) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, start = 32.dp, end = 32.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Playlist",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isExpanded = false }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse")
                            }
                        }

                        val listState = rememberLazyListState()
                        LaunchedEffect(currentIndex) {
                            if (currentIndex >= 0 && isExpanded) {
                                listState.animateScrollToItem(currentIndex)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            itemsIndexed(playlist) { index, file ->
                                val isCurrent = index == currentIndex
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = file.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingContent = {
                                        if (isCurrent && isPlaying) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .clickable {
                                            AudioManager.play(context, file, playlist)
                                        },
                                    colors = ListItemDefaults.colors(
                                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                                    )
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp))
                    }
                }

                // Player Controls Area
                Column(
                    modifier = Modifier
                        .then(if (isExpanded) Modifier.height(130.dp) else Modifier.fillMaxSize())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTrack?.name ?: "",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatTime(position) + " / " + formatTime(duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isExpanded = !isExpanded }) {
                                Icon(
                                    if (isExpanded) Icons.AutoMirrored.Filled.QueueMusic else Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = "Playlist",
                                    tint = if (isExpanded) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { AudioManager.playPrevious() }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                            }
                            FilledIconButton(
                                onClick = { AudioManager.togglePlayPause() },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play"
                                )
                            }
                            IconButton(onClick = { AudioManager.playNext() }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next")
                            }
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = { AudioManager.stop() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }

                    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f

                    WavySlider(
                        value = progress,
                        onValueChange = { AudioManager.seekTo((it * duration).toLong()) },
                        enabled = true,
                        waveLength = 37.dp,
                        waveHeight = if (isPlaying) 20.dp else 0.dp,
                        waveVelocity = 20.dp to WaveDirection.TAIL,
                        waveThickness = 4.dp,
                        trackThickness = 10.dp,
                        incremental = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    val locale = java.util.Locale.getDefault()
    return if (hours > 0) {
        String.format(locale, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(locale, "%02d:%02d", minutes, seconds)
    }
}
