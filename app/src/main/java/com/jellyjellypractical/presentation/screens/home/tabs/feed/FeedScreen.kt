package com.jellyjellypractical.presentation.screens.home.tabs.feed

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.jellyjellypractical.domain.intent.VideoIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(@Suppress("unused") navController: NavController) {
    val viewModel: FeedViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState()
    val context = LocalContext.current

    // Remember the PullToRefreshState
    val pullRefreshState = rememberPullToRefreshState()
    var pullRefreshIsLoading by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        pullRefreshIsLoading = true
        viewModel.onIntent(VideoIntent.LoadVideos)
    }

    LaunchedEffect(state.value.mediaItemList) {
        Log.d("MediaItems", "Count: ${state.value.mediaItemList.size}")
        state.value.mediaItemList.forEach {
            Log.d("MediaItem", it.localConfiguration?.uri.toString())
        }
    }

    val videoList = state.value.videos

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = pullRefreshIsLoading,
        onRefresh = { viewModel.onIntent(VideoIntent.LoadVideos) },
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (state.value.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    when (pullRefreshIsLoading) {
                        true -> Text(
                            "Loading...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        false -> {
                            CircularProgressIndicator()
                        }
                    }
                }
            } else {
                pullRefreshIsLoading = false
                if (videoList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No videos found", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        VideoPlayerWithControls(
                            context = context,
                            mediaItems = state.value.mediaItemList,
                            isMuted = state.value.isMuted,
                            changeMuteState = { viewModel.changeMuteState(it) },
                            currentIndex = state.value.currentIndex,
                            onPrevious = { viewModel.previousIndex() },
                            onNext = { viewModel.nextIndex() }

                        )
                    }
                }
            }
        }
    }

}

/*

@Composable
fun VideoPlayerWithControls(
    context: Context,
    mediaItems: List<MediaItem>,
    isMuted: Boolean,
    changeMuteState: (Boolean) -> Unit,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 1f
        }
    }
    val isPlaying = remember { mutableStateOf(false) }

    var isPlaylistSet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying.value = playing
            }
        })
    }

    LaunchedEffect(mediaItems) {
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
        isPlaylistSet = true
    }

    // Seek to current index when it changes
    LaunchedEffect(currentIndex, isPlaylistSet) {
        if (isPlaylistSet && currentIndex in mediaItems.indices) {
            exoPlayer.seekTo(currentIndex, C.TIME_UNSET)
            exoPlayer.playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false // Disable default controls
                }
            },
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.background),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(onClick = {
                    if (isPlaying.value) exoPlayer.pause() else exoPlayer.play()
                }) {
                    Icon(
                        imageVector = if (isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(onClick = {
                    val changedMuteState = !isMuted
                    changeMuteState(changedMuteState)
                    exoPlayer.volume = if (changedMuteState) 0f else 1f
                }) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Default.VolumeOff else Icons.AutoMirrored.Default.VolumeUp,
                        contentDescription = "Toggle Mute",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
*/
@Composable
fun VideoPlayerWithControls(
    context: Context,
    mediaItems: List<MediaItem>,
    isMuted: Boolean,
    changeMuteState: (Boolean) -> Unit,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = if (isMuted) 0f else 1f
        }
    }

    val isPlaying = remember { mutableStateOf(false) }
    val isBuffering = remember { mutableStateOf(false) }

    var isPlaylistSet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering.value = state == Player.STATE_BUFFERING
            }
        })
    }

    LaunchedEffect(mediaItems) {
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
        isPlaylistSet = true
    }

    LaunchedEffect(currentIndex, isPlaylistSet) {
        if (isPlaylistSet && currentIndex in mediaItems.indices) {
            exoPlayer.seekTo(currentIndex, C.TIME_UNSET)
            exoPlayer.playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.matchParentSize()
        )

        // ⏳ Loader when buffering
        if (isBuffering.value) {
            Box(
                modifier = Modifier
                    .matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // 🎛 Player controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(onClick = {
                    if (isPlaying.value) exoPlayer.pause() else exoPlayer.play()
                }) {
                    Icon(
                        imageVector = if (isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(onClick = {
                    val changedMuteState = !isMuted
                    changeMuteState(changedMuteState)
                    exoPlayer.volume = if (changedMuteState) 0f else 1f
                }) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Default.VolumeOff else Icons.AutoMirrored.Default.VolumeUp,
                        contentDescription = "Toggle Mute",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
