package io.battlewithbytes.carlauncher.ui

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MediaPlayerWidget(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 100) { // Swipe down threshold
                            onDismiss()
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 0) { // Only track downward drags
                            dragOffset += dragAmount
                        }
                    }
                )
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

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Album art placeholder
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Album Art",
                    modifier = Modifier.size(120.dp),
                    tint = Color(0xFF404040)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Track info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No media playing",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unknown Artist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF808080)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Progress bar
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = 0f,
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
                    Text("0:00", color = Color(0xFF808080), fontSize = 12.sp)
                    Text("0:00", color = Color(0xFF808080), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Previous track */ }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }

                // Play/Pause button (larger)
                IconButton(
                    onClick = { /* Play/Pause */ },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(56.dp),
                        tint = Color.White
                    )
                }

                IconButton(onClick = { /* Next track */ }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Additional controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { /* Shuffle */ }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color(0xFF808080)
                    )
                }
                IconButton(onClick = { /* Repeat */ }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = Color(0xFF808080)
                    )
                }
                IconButton(onClick = { /* Queue */ }) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color(0xFF808080)
                    )
                }
            }
        }
    }
}
