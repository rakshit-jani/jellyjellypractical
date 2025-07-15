package com.jellyjellypractical.presentation.screens.home.tabs.feed

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
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

    private var isScraperInitialized = false

    fun onIntent(intent: VideoIntent) {
        when (intent) {
            is VideoIntent.LoadVideos -> {
                _state.value = _state.value.copy(isLoading = true)

                webScraper.initializeScraper {
                    isScraperInitialized = true
                    webScraper.clickNextAndScrape { result ->
                        val mediaItemList = result.map { video ->
                            MediaItem.fromUri(video.videoUrl.toUri())
                        }
                        _state.value = _state.value.copy(
                            isLoading = false,
                            videos = result,
                            mediaItemList = mediaItemList,
                            currentIndex = 0
                        )
                    }
                }
            }

            is VideoIntent.LoadNextPage -> {
                if (!isScraperInitialized) return

                _state.value = _state.value.copy(isLoading = true)

                webScraper.clickNextAndScrape { newVideos ->
                    if (newVideos.isNotEmpty()) {
                        val combinedList = buildList {
                            addAll(_state.value.videos)
                            addAll(newVideos)
                        }
                        val combinedMediaItemList = buildList {
                            addAll(_state.value.mediaItemList)
                            addAll(newVideos.map { video ->
                                MediaItem.fromUri(video.videoUrl.toUri())
                            })
                        }

                        _state.value = _state.value.copy(
                            isLoading = false,
                            videos = combinedList,
                            mediaItemList = combinedMediaItemList,
                            currentIndex = combinedList.lastIndex
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false)
                    }
                }
            }
        }
    }

    fun nextIndex() {
        val currentIndex = _state.value.currentIndex
        if (currentIndex < _state.value.videos.size - 1) {
            _state.value = _state.value.copy(currentIndex = currentIndex + 1)
        } else {
            // Load more videos if we're at the end
            onIntent(VideoIntent.LoadNextPage)
        }
    }

    fun previousIndex() {
        val currentIndex = _state.value.currentIndex
        if (currentIndex > 0) {
            _state.value = _state.value.copy(currentIndex = currentIndex - 1)
        }
    }

    fun changeMuteState(isMute: Boolean) {
        _state.value = _state.value.copy(isMuted = isMute)
    }

    override fun onCleared() {
        webScraper.destroy()
        super.onCleared()
    }
}
