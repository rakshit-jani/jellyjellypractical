package com.jellyjellypractical.domain.state.home.feed

import androidx.media3.common.MediaItem
import com.jellyjellypractical.data.model.home.feed.VideoItem

data class VideoUiState(
    val isLoading: Boolean = true,
    val videos: List<VideoItem> = emptyList(),
    val currentIndex: Int = 0,
    val mediaItemList: List<MediaItem> = emptyList(),
    val isMuted: Boolean = false
)