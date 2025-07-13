package com.jellyjellypractical.domain.state.home.feed

import com.jellyjellypractical.data.model.home.feed.VideoItem

data class VideoUiState(
    val isLoading: Boolean = true,
    val videos: List<VideoItem> = emptyList()
)