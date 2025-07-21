package com.jellyjellypractical.domain.state.home.gallery

import com.jellyjellypractical.data.model.home.gallery.VideoItem
import com.jellyjellypractical.data.model.home.gallery.VideoPlaybackDetails

enum class LoadingState {
    IDLE, LOADING, SUCCESS, ERROR
}

data class GalleryState(
    val videoItems: List<VideoItem> = emptyList(),
    val loadingState: LoadingState = LoadingState.IDLE,
    val errorMessage: String? = null,
    val selectedVideoPlayback: VideoPlaybackDetails? = null // New: To manage full screen video playback
)