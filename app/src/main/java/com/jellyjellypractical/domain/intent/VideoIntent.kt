package com.jellyjellypractical.domain.intent

sealed interface VideoIntent {
    /** Called once when FeedScreen starts */
    object LoadVideos : VideoIntent

    /** Called when user taps "Next" to load more videos */
    @Suppress("unused")
    object LoadNextPage : VideoIntent
}
