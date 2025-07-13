package com.jellyjellypractical.data.model.home.feed

enum class SourceType {
    VIDEO_TAG, @Suppress("unused")
    PREFETCH_LINK
}

data class VideoItem(
    val videoUrl: String,
    val posterUrl: String? = null,
    val sourceType: SourceType
)