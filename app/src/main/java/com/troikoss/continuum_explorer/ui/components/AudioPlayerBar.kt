package com.troikoss.continuum_explorer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.SliderDefaults as SliderDefaults2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import com.troikoss.continuum_explorer.managers.AudioManager
import com.troikoss.continuum_explorer.managers.MusicMetadataManager
import com.troikoss.continuum_explorer.R

@Composable
fun AudioPlayerBar(modifier: Modifier = Modifier) {
    val currentTrack = AudioManager.currentTrack
    val currentTitle = AudioManager.currentTitle
    val currentArtist = AudioManager.currentArtist
    val isPlaying = AudioManager.isAudioPlaying
    val isShuffleEnabled = AudioManager.isShuffleEnabled
    val isRepeatEnabled = AudioManager.isRepeatEnabled
    val position = AudioManager.currentPosition
    val duration = AudioManager.duration
    val playlist = AudioManager.playlist
    val currentIndex = AudioManager.currentIndex
    val isMinimized = AudioManager.isMinimized
    val isExpanded = AudioManager.isExpanded
    val context = LocalContext.current
    val density = LocalDensity.current

    val configuration = LocalConfiguration.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    // Calculate max height by subtracting bottom constraints so it doesn't overflow the top
    val bottomClearance = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) 84.dp else 16.dp
    val maxHeight = configuration.screenHeightDp.dp - statusBarHeight - navBarHeight - bottomClearance - 23.dp
    
    var verticalDragOffset by remember { mutableStateOf(0f) }
    var horizontalDragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val interactiveHeight = remember(verticalDragOffset, isExpanded) {
        with(density) {
            val offsetDp = -verticalDragOffset.toDp()
            if (isExpanded) {
                (maxHeight + offsetDp).coerceIn(120.dp, maxHeight)
            } else {
                (120.dp + offsetDp).coerceIn(120.dp, maxHeight)
            }
        }
    }

    val animatedHeight by animateDpAsState(
        targetValue = if (isDragging) interactiveHeight else (if (isExpanded) maxHeight.coerceAtLeast(120.dp) else 120.dp),
        animationSpec = if (isDragging) snap() else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "PillHeight"
    )

    val animatedHorizontalOffset by animateFloatAsState(
        targetValue = if (isDragging) horizontalDragOffset.coerceAtLeast(0f) else 0f,
        animationSpec = if (isDragging) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "CloseOffset"
    )

    AnimatedVisibility(
        visible = currentTrack != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier.fillMaxWidth()
    ) {
        AnimatedContent(
            targetState = isMinimized,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            label = "PlayerMinimization"
        ) { minimized ->
            if (minimized) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Surface(
                        onClick = { AudioManager.isMinimized = false },
                        modifier = Modifier
                            .padding(16.dp)
                            .size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 8.dp,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isPlaying) {
                                AXWaveAnimation(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_music_logo),
                                contentDescription = "Expand Player",
                                modifier = Modifier.size(32.dp),
                                tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(animatedHeight)
                        .graphicsLayer {
                            translationX = animatedHorizontalOffset
                            // Fade out as it slides away
                            alpha = (1f - (animatedHorizontalOffset / size.width)).coerceIn(0f, 1f)
                        },
                    shape = RoundedCornerShape(60.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val availableWidth = maxWidth
                        val showShuffleRepeatInMain = availableWidth > 450.dp
                        val showName = availableWidth > 380.dp
                        val horizontalPadding = if (availableWidth < 480.dp) 28.dp else 40.dp

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Expanded Playlist View
                            if (isExpanded || isDragging) {
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
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!showShuffleRepeatInMain) {
                                            IconButton(onClick = { AudioManager.toggleShuffle() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Shuffle,
                                                    contentDescription = "Shuffle",
                                                    tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                                )
                                            }
                                            IconButton(onClick = { AudioManager.toggleRepeat() }) {
                                                Icon(
                                                    imageVector = if (isRepeatEnabled) Icons.Default.RepeatOn else Icons.Default.Repeat,
                                                    contentDescription = "Repeat",
                                                    tint = if (isRepeatEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                                )
                                            }
                                        }
                                        IconButton(onClick = { AudioManager.isExpanded = false }) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse")
                                        }
                                    }

                                    val listState = rememberLazyListState()
                                    LaunchedEffect(currentIndex) {
                                        if (currentIndex >= 0) {
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
                                            val isFavourite = MusicMetadataManager.isFavourite(file.providerId)
                                            
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
                                                trailingContent = {
                                                    IconButton(onClick = { MusicMetadataManager.toggleFavourite(context, file.providerId) }) {
                                                        Icon(
                                                            painter = painterResource(id = if (isFavourite) R.drawable.ic_music_favourite else R.drawable.ic_music_not_favourite),
                                                            contentDescription = "Favourite",
                                                            tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(24.dp)
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
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { 
                                                isDragging = true
                                                verticalDragOffset = 0f
                                                horizontalDragOffset = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                verticalDragOffset += dragAmount.y
                                                horizontalDragOffset += dragAmount.x
                                            },
                                            onDragEnd = {
                                                isDragging = false
                                                val vThreshold = 100f
                                                val hThreshold = 150f
                                                
                                                if (kotlin.math.abs(horizontalDragOffset) > kotlin.math.abs(verticalDragOffset) && horizontalDragOffset > hThreshold) {
                                                    AudioManager.stop()
                                                } else {
                                                    if (verticalDragOffset < -vThreshold) { // Swipe Up
                                                        AudioManager.isExpanded = true
                                                    } else if (verticalDragOffset > vThreshold) { // Swipe Down
                                                        if (AudioManager.isExpanded) {
                                                            AudioManager.isExpanded = false
                                                        } else {
                                                            AudioManager.isMinimized = true
                                                        }
                                                    }
                                                }
                                                verticalDragOffset = 0f
                                                horizontalDragOffset = 0f
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                                verticalDragOffset = 0f
                                                horizontalDragOffset = 0f
                                            }
                                        )
                                    }
                                    .clickable { 
                                        if (AudioManager.isExpanded) {
                                            AudioManager.isExpanded = false
                                        } else {
                                            AudioManager.isMinimized = true 
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = horizontalPadding),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BoxWithConstraints(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (maxWidth >= 60.dp) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                if (showName) {
                                                    val displayName = remember(currentTitle, currentArtist, currentTrack) {
                                                        if (!currentTitle.isNullOrBlank()) {
                                                            if (!currentArtist.isNullOrBlank()) "$currentArtist - $currentTitle" else currentTitle
                                                        } else {
                                                            currentTrack?.name ?: ""
                                                        }
                                                    }
                                                    Text(
                                                        text = displayName,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = formatTime(position) + " / " + formatTime(duration),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { AudioManager.isExpanded = !AudioManager.isExpanded }) {
                                            Icon(
                                                if (isExpanded) Icons.AutoMirrored.Filled.QueueMusic else Icons.AutoMirrored.Filled.PlaylistPlay,
                                                contentDescription = "Playlist",
                                                tint = if (isExpanded) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                            )
                                        }
                                        if (showShuffleRepeatInMain) {
                                            IconButton(onClick = { AudioManager.toggleShuffle() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Shuffle,
                                                    contentDescription = "Shuffle",
                                                    tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                                )
                                            }
                                            IconButton(onClick = { AudioManager.toggleRepeat() }) {
                                                Icon(
                                                    imageVector = if (isRepeatEnabled) Icons.Default.RepeatOn else Icons.Default.Repeat,
                                                    contentDescription = "Repeat",
                                                    tint = if (isRepeatEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                                )
                                            }
                                        }
                                        IconButton(onClick = { AudioManager.playPrevious() }) {
                                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                                        }
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isPlaying) {
                                                PlayPulseAnimation(
                                                    modifier = Modifier.size(48.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            FilledIconButton(
                                                onClick = { AudioManager.togglePlayPause() },
                                                modifier = Modifier.size(48.dp),
                                                shape = CircleShape,
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            ) {
                                                AnimatedContent(
                                                    targetState = isPlaying,
                                                    transitionSpec = {
                                                        fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.5f) togetherWith
                                                                fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.5f)
                                                    },
                                                    label = "PlayPauseAnimation"
                                                ) { playing ->
                                                    Icon(
                                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = if (playing) "Pause" else "Play"
                                                    )
                                                }
                                            }
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
                                    waveLength = 45.dp,
                                    waveHeight = if (isPlaying) 12.dp else 0.dp,
                                    waveVelocity = 20.dp to WaveDirection.TAIL,
                                    waveThickness = 4.dp,
                                    trackThickness = 10.dp,
                                    incremental = true,
                                    colors = SliderDefaults2.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = if (availableWidth < 480.dp) 24.dp else 40.dp) // Match horizontal padding for alignment
                                        .padding(bottom = 12.dp)
                                )
                            }
                        }
                    }
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

@Composable
fun AXWaveAnimation(
    modifier: Modifier = Modifier,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AXWave")
    
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "Phase1"
    )
    
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "Phase2"
    )

    val waveHeight by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "WaveHeight"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        fun drawWave(phase: Float, alpha: Float, ampScale: Float, freq: Float, baseOffset: Float) {
            val path = Path()
            val baseLine = height * baseOffset
            val amplitude = height * 0.2f * ampScale
            
            path.moveTo(0f, baseLine)
            for (x in 0..width.toInt() step 5) {
                val relX = x.toFloat() / width
                val y = baseLine + kotlin.math.sin((relX * freq + phase) * 2f * Math.PI.toFloat()) * amplitude
                path.lineTo(x.toFloat(), y)
            }
            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()
            drawPath(path, color.copy(alpha = alpha))
        }

        drawWave(phase1, 1f, 1f, 1.2f, 1f - waveHeight)
        drawWave(phase2, 0.5f, 0.7f, 1.8f, 1f - (waveHeight * 0.9f))
    }
}

@Composable
fun PlayPulseAnimation(
    modifier: Modifier = Modifier,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PlayPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Alpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(color, CircleShape)
    )
}
