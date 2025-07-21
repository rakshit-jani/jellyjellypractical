package com.jellyjellypractical.presentation.screens.home.tabs.gallery.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jellyjellypractical.data.model.home.gallery.VideoPlaybackDetails

@Composable
fun VideoPlaybackDialog(
    playbackDetails: VideoPlaybackDetails,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Initialize ExoPlayers
    val frontPlayer = remember { ExoPlayer.Builder(context).build() }
    val backPlayer = remember { ExoPlayer.Builder(context).build() }

    // Load media items
    LaunchedEffect(playbackDetails) {
        frontPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(playbackDetails.frontVideoPath)))
        backPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(playbackDetails.backVideoPath)))

        frontPlayer.prepare()
        backPlayer.prepare()

        // Sync playback: start both, and ensure seeking/pausing applies to both
        // This is a basic sync. For more complex sync (e.g., when one player buffers),
        // you might need a more sophisticated listener/state management.
        frontPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) backPlayer.play() else backPlayer.pause()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_ENDED) {
                    backPlayer.seekTo(0)
                    frontPlayer.seekTo(0)
                    frontPlayer.pause()
                    backPlayer.pause()
                }
            }
        })
        backPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) frontPlayer.play() else frontPlayer.pause()
            }
        })
    }

    // Release players when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            frontPlayer.release()
            backPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Full screen dialog
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // Use your app's background color
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = playbackDetails.alias,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Front Video Player
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = frontPlayer
                        useController = false // We'll use a custom controller
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes available vertical space
                    .padding(bottom = 2.dp)
            )

            // Back Video Player
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = backPlayer
                        useController = false // We'll use a custom controller
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes available vertical space
                    .padding(top = 2.dp)
            )

            // Custom Player Controls
            VideoPlaybackControls(
                frontPlayer = frontPlayer,
                backPlayer = backPlayer,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun VideoPlaybackControls(
    frontPlayer: ExoPlayer,
    backPlayer: ExoPlayer,
    onDismiss: () -> Unit
) {
    // Collect playback state from one player and apply to both
    val isPlaying by remember(frontPlayer.isPlaying) { mutableStateOf(frontPlayer.isPlaying) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dismiss Button
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit Fullscreen", modifier = Modifier.size(36.dp))
        }

        // Rewind
        IconButton(onClick = {
            frontPlayer.seekTo(frontPlayer.currentPosition - 5000) // Rewind by 5 seconds
            backPlayer.seekTo(backPlayer.currentPosition - 5000)
        }) {
            Icon(Icons.Filled.FastRewind, contentDescription = "Rewind", modifier = Modifier.size(48.dp))
        }

        // Play/Pause Toggle
        IconButton(onClick = {
            if (isPlaying) {
                frontPlayer.pause()
                backPlayer.pause()
            } else {
                frontPlayer.play()
                backPlayer.play()
            }
        }) {
            Icon(
                if (isPlaying) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Fast Forward
        IconButton(onClick = {
            frontPlayer.seekTo(frontPlayer.currentPosition + 5000) // Forward by 5 seconds
            backPlayer.seekTo(backPlayer.currentPosition + 5000)
        }) {
            Icon(Icons.Filled.FastForward, contentDescription = "Fast Forward", modifier = Modifier.size(48.dp))
        }
    }
}