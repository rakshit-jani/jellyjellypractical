package com.jellyjellypractical.presentation.screens.home.tabs.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import com.jellyjellypractical.domain.intent.VideoIntent
import com.jellyjellypractical.domain.state.home.feed.VideoUiState
import com.jellyjellypractical.utils.WebScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(VideoUiState())
    val state: StateFlow<VideoUiState> = _state.asStateFlow()

    private val webScraper = WebScraper(context)

    fun onIntent(intent: VideoIntent) {
        when (intent) {
            is VideoIntent.LoadVideos -> {
                _state.value = _state.value.copy(isLoading = true)
                webScraper.scrapeVideos(maxClicks = 2) { result ->
                    _state.value = _state.value.copy(isLoading = false, videos = result)
                }
            }
            // Add LoadNextPage later if you want manual control
            else -> {}
        }
    }

    override fun onCleared() {
        webScraper.destroy()
    }
}
