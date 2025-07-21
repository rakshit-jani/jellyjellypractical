package com.jellyjellypractical.domain.intent.camera

import android.content.Context

sealed class CameraIntent {
    object StartRecording : CameraIntent()
    object StopRecording : CameraIntent()
    object PauseRecording : CameraIntent()
    object ResumeRecording : CameraIntent()
    data class InitCamera(val context: Context) : CameraIntent()
    object Tick : CameraIntent()
}