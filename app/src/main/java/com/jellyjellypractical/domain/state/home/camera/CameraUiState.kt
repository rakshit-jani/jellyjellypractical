package com.jellyjellypractical.domain.state.home.camera

import android.net.Uri

data class CameraUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val recordingTime: Long = 0L,
    val frontVideoUri: Uri? = null,
    val backVideoUri: Uri? = null,
    val error: String? = null
)