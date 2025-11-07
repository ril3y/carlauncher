package io.battlewithbytes.carlauncher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.battlewithbytes.carlauncher.viewmodel.MediaPlayerViewModel
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun MediaPlayerWidget(
    viewModel: MediaPlayerViewModel,
    onDismiss: () -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsState()

    // Get screen height for offset calculations
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Track drag offset - initialized based on visible state
    var offsetY by remember(visible) {
        mutableStateOf(if (visible) 0f else screenHeight)
    }

    // Animate offset with spring for natural feel
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "MediaPlayerOffset"
    )

    // Update offset when visibility changes
    LaunchedEffect(visible) {
        offsetY = if (visible) 0f else screenHeight
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .background(Color.Black.copy(alpha = 0.95f))
            .pointerInput(visible) {
                if (visible) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // Allow dragging down, and up only when partially dragged
                            offsetY = (offsetY + dragAmount.toFloat()).coerceIn(0f, screenHeight)
                        },
                        onDragEnd = {
                            // Snap logic: if dragged more than 50% down, dismiss
                            if (offsetY > screenHeight * 0.5f) {
                                offsetY = screenHeight
                                onDismiss()
                            } else {
                                // Snap back to open
                                offsetY = 0f
                            }
                        }
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Swipe indicator
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .background(Color(0xFF404040))
                    .padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Album art
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                if (playbackState.albumArt != null) {
                    Image(
                        bitmap = playbackState.albumArt!!.asImageBitmap(),
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Album Art",
                        modifier = Modifier.size(120.dp),
                        tint = Color(0xFF404040)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = playbackState.trackTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = playbackState.artistName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF808080),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (playbackState.albumName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playbackState.albumName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF606060),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress bar
            Column(modifier = Modifier.fillMaxWidth()) {
                val progress = if (playbackState.duration > 0) {
                    (playbackState.position.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color.White,
                    trackColor = Color(0xFF2A2A2A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(playbackState.position),
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatTime(playbackState.duration),
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.skipToPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }

                // Play/Pause button (larger)
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(56.dp),
                        tint = Color.White
                    )
                }

                IconButton(onClick = { viewModel.skipToNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * "Next Up" section showing upcoming track in queue
 * Note: MediaController queue API support varies by player app
 */
@Composable
private fun NextUpSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                tint = Color(0xFF808080),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Next Up",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF808080),
                fontWeight = FontWeight.Medium
            )
        }

        // Placeholder for next track - in production, this would come from MediaController.getQueue()
        // Note: Queue support varies by media player app
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2A2A2A), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF606060),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Queue not available",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF606060),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Player app may not support queue API",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF404040),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Formats milliseconds into MM:SS format
 */
private fun formatTime(millis: Long): String {
    if (millis <= 0) return "0:00"

    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%d:%02d", minutes, seconds)
}
