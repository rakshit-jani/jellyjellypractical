package com.jellyjellypractical.presentation.screens.home.tabs.gallery

// Import the new VideoPlaybackDialog component
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jellyjellypractical.data.model.home.gallery.VideoItem
import com.jellyjellypractical.domain.intent.gallery.GalleryIntent
import com.jellyjellypractical.domain.state.home.gallery.LoadingState
import com.jellyjellypractical.presentation.screens.home.tabs.gallery.components.VideoPlaybackDialog

@Composable
fun GalleryScreen(
    @Suppress("unused") navController: NavController,
) {
    val viewModel: GalleryViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.processIntent(GalleryIntent.LoadVideos)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.loadingState) {
            LoadingState.IDLE -> {
                Text("Waiting to load gallery...", modifier = Modifier.align(Alignment.Center))
            }
            LoadingState.LOADING -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            LoadingState.ERROR -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: ${state.errorMessage ?: "Unknown error"}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            LoadingState.SUCCESS -> {
                if (state.videoItems.isEmpty()) {
                    Text("No videos found in gallery.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "Your Video Gallery",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(state.videoItems) { videoItem ->
                            VideoGalleryGridItem(
                                videoItem = videoItem,
                                onClick = { clickedVideoItem ->
                                    viewModel.processIntent(
                                        GalleryIntent.SelectVideoForPlayback(clickedVideoItem.commonName)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Full-screen video playback dialog (now using the separate component)
        state.selectedVideoPlayback?.let { playbackDetails ->
            VideoPlaybackDialog(
                playbackDetails = playbackDetails,
                onDismiss = { viewModel.processIntent(GalleryIntent.DismissVideoPlayback) }
            )
        }
    }
}

@Composable
fun VideoGalleryGridItem(videoItem: VideoItem, onClick: (VideoItem) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick(videoItem) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayCircleFilled,
                contentDescription = "Video Icon",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = videoItem.alias,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}