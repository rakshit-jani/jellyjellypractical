package com.jellyjellypractical.presentation.screens.home.tabs.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.jellyjellypractical.domain.intent.VideoIntent

@Composable
fun FeedScreen(@Suppress("unused") navController: NavController) {
    val viewModel: FeedViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onIntent(VideoIntent.LoadVideos)
    }

    if (state.value.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
           items(count = state.value.videos.size) {index ->
               val item = state.value.videos[index]
                Text(text = "Video URL: ${item.videoUrl}", style = MaterialTheme.typography.bodyMedium)
                item.posterUrl?.let {
                    AsyncImage(model = it, contentDescription = "Poster", modifier = Modifier.fillMaxWidth().height(200.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
           }

        }
    }
}

