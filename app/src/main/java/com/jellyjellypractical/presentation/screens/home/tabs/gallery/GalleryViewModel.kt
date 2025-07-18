package com.jellyjellypractical.presentation.screens.home.tabs.gallery

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jellyjellypractical.data.model.home.gallery.VideoItem
import com.jellyjellypractical.data.model.home.gallery.VideoPlaybackDetails
import com.jellyjellypractical.domain.intent.gallery.GalleryIntent
import com.jellyjellypractical.domain.state.home.gallery.GalleryState
import com.jellyjellypractical.domain.state.home.gallery.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(GalleryState())
    val state: StateFlow<GalleryState> = _state.asStateFlow()

    private val videoExtensions = setOf("mp4", "mov", "avi", "mkv", "webm", "3gp")

    fun processIntent(intent: GalleryIntent) {
        when (intent) {
            GalleryIntent.LoadVideos -> loadVideoFiles()
            is GalleryIntent.SelectVideoForPlayback -> selectVideoForPlayback(intent.commonName) // Handle new intent
            GalleryIntent.DismissVideoPlayback -> dismissVideoPlayback() // Handle new intent
        }
    }

    private fun loadVideoFiles() {
        if (_state.value.loadingState == LoadingState.LOADING) {
            return
        }

        _state.update { it.copy(loadingState = LoadingState.LOADING, errorMessage = null) }

        viewModelScope.launch {
            try {
                val videoDirectory = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_MOVIES)

                if (videoDirectory == null) {
                    Log.e("GalleryViewModel", "External storage directory for movies is null.")
                    _state.update {
                        it.copy(
                            loadingState = LoadingState.ERROR,
                            errorMessage = "External storage not available or movies directory not found."
                        )
                    }
                    return@launch
                }

                Log.d("GalleryViewModel", "Attempting to read from: ${videoDirectory.absolutePath}")

                if (!videoDirectory.exists()) {
                    Log.w("GalleryViewModel", "Video directory does not exist: ${videoDirectory.absolutePath}")
                    _state.update {
                        it.copy(
                            loadingState = LoadingState.SUCCESS,
                            videoItems = emptyList(),
                            errorMessage = "No video directory found. Please ensure videos are placed in ${videoDirectory.absolutePath}"
                        )
                    }
                    return@launch
                }

                if (!videoDirectory.isDirectory) {
                    Log.e("GalleryViewModel", "${videoDirectory.absolutePath} is not a directory.")
                    _state.update {
                        it.copy(
                            loadingState = LoadingState.ERROR,
                            errorMessage = "Error: Video path is not a directory."
                        )
                    }
                    return@launch
                }

                // Map to store common names to their available file paths (front and back)
                val videoFilePaths = mutableMapOf<String, MutableMap<String, String>>() // commonName -> (suffix -> filePath)

                val files = videoDirectory.listFiles()

                if (files.isNullOrEmpty()) {
                    Log.d("GalleryViewModel", "No files found in ${videoDirectory.absolutePath}")
                    _state.update {
                        it.copy(
                            loadingState = LoadingState.SUCCESS,
                            videoItems = emptyList(),
                            errorMessage = "No video files found."
                        )
                    }
                    return@launch
                }

                files.forEach { file ->
                    if (file.isFile) {
                        val fileName = file.name
                        val lowerCaseFileName = fileName.lowercase(Locale.getDefault())

                        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
                        if (extension !in videoExtensions) {
                            Log.d("GalleryViewModel", "Skipping file (unsupported extension '$extension'): $fileName")
                            return@forEach
                        }

                        var baseNameRaw = fileName.substringBeforeLast('.') // e.g., VIDEO_..._BACK
                        val suffix = when {
                            lowerCaseFileName.contains("_front") -> "_FRONT"
                            lowerCaseFileName.contains("_back") -> "_BACK"
                            else -> "" // No specific suffix
                        }

                        // Strip _FRONT or _BACK suffixes for the common base name
                        val commonBaseName = baseNameRaw.replace(Regex("(_FRONT|_BACK)$", RegexOption.IGNORE_CASE), "")

                        val commonNameFormatted = commonBaseName.replace("_", " ")
                            .replace("-", " ")
                            .trim()
                            .split(" ")
                            .joinToString(" ") { word ->
                                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            }

                        // Store the file path based on its suffix
                        val pathsForCommonName = videoFilePaths.getOrPut(commonNameFormatted) { mutableMapOf() }
                        pathsForCommonName[suffix] = file.absolutePath
                        Log.d("GalleryViewModel", "Processed file: '$fileName' -> common: '$commonNameFormatted', suffix: '$suffix', path: '${file.absolutePath}'")
                    }
                }

                // Generate VideoItem objects with aliases
                val videoItems = videoFilePaths.entries.sortedBy { it.key }.mapIndexed { index, entry ->
                    // For display in the grid, we just need one path for the thumbnail/click
                    // We can choose _FRONT if available, otherwise _BACK, otherwise any.
                    val representativePath = entry.value["_FRONT"] ?: entry.value["_BACK"] ?: entry.value.values.first()
                    VideoItem(
                        commonName = entry.key,
                        alias = "Video ${index + 1}",
                        filePath = representativePath // Store one path for display
                    )
                }

                _state.update {
                    it.copy(
                        loadingState = LoadingState.SUCCESS,
                        videoItems = videoItems
                    )
                }

            } catch (e: IOException) {
                Log.e("GalleryViewModel", "Error reading video files", e)
                _state.update {
                    it.copy(
                        loadingState = LoadingState.ERROR,
                        errorMessage = "Failed to load videos: ${e.message}"
                    )
                }
            } catch (e: SecurityException) {
                Log.e("GalleryViewModel", "Security error reading video files (permissions issue?)", e)
                _state.update {
                    it.copy(
                        loadingState = LoadingState.ERROR,
                        errorMessage = "Permission denied to load videos. Check app settings."
                    )
                }
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "An unexpected error occurred", e)
                _state.update {
                    it.copy(
                        loadingState = LoadingState.ERROR,
                        errorMessage = "An unknown error occurred: ${e.message}"
                    )
                }
            }
        }
    }

    private fun selectVideoForPlayback(commonName: String) {
        viewModelScope.launch {
            val videoDirectory = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: return@launch
            val files = videoDirectory.listFiles() ?: return@launch

            var frontPath: String? = null
            var backPath: String? = null
            val alias = _state.value.videoItems.find { it.commonName == commonName }?.alias ?: commonName

            files.forEach { file ->
                if (file.isFile) {
                    val fileName = file.name
                    val lowerCaseFileName = fileName.lowercase(Locale.getDefault())

                    var baseNameRaw = fileName.substringBeforeLast('.')
                    val currentCommonName = baseNameRaw.replace(Regex("(_FRONT|_BACK)$", RegexOption.IGNORE_CASE), "")
                        .replace("_", " ")
                        .replace("-", " ")
                        .trim()
                        .split(" ")
                        .joinToString(" ") { word ->
                            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        }

                    if (currentCommonName == commonName) {
                        if (lowerCaseFileName.contains("_front")) {
                            frontPath = file.absolutePath
                        } else if (lowerCaseFileName.contains("_back")) {
                            backPath = file.absolutePath
                        }
                    }
                }
            }

            if (frontPath != null && backPath != null) {
                _state.update {
                    it.copy(
                        selectedVideoPlayback = VideoPlaybackDetails(
                            alias = alias,
                            frontVideoPath = frontPath,
                            backVideoPath = backPath
                        )
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        errorMessage = "Could not find both front and back video files for: $commonName"
                    )
                }
                Log.e("GalleryViewModel", "Missing front/back video for $commonName. Front: $frontPath, Back: $backPath")
            }
        }
    }

    private fun dismissVideoPlayback() {
        _state.update {
            it.copy(selectedVideoPlayback = null)
        }
    }
}