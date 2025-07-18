package com.jellyjellypractical.domain.intent.gallery

sealed interface GalleryIntent {
    object LoadVideos : GalleryIntent
    data class SelectVideoForPlayback(val commonName: String) : GalleryIntent
    object DismissVideoPlayback : GalleryIntent
}