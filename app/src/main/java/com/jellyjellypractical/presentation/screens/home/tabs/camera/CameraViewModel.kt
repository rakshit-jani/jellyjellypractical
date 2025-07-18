package com.jellyjellypractical.presentation.screens.home.tabs.camera

import android.view.TextureView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jellyjellypractical.domain.intent.camera.CameraIntent
import com.jellyjellypractical.domain.state.home.camera.CameraUiState
import com.jellyjellypractical.utils.DualCameraController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var controller: DualCameraController? = null
    private var timerJob: Job? = null

    fun handleIntent(intent: CameraIntent) {
        when (intent) {
            is CameraIntent.InitCamera -> initCamera(intent)
            is CameraIntent.StartRecording -> startRecording()
            is CameraIntent.PauseRecording -> pauseRecording()
            is CameraIntent.ResumeRecording -> resumeRecording()
            is CameraIntent.StopRecording -> stopRecording()
            is CameraIntent.Tick -> updateRecordingTime()
        }
    }

    private fun initCamera(intent: CameraIntent.InitCamera) {
        if (controller == null) {
            controller = DualCameraController(
                context = intent.context,
                onRecordingFinished = { frontUri, backUri ->
                    _uiState.value = _uiState.value.copy(
                        isRecording = false,
                        isPaused = false,
                        recordingTime = 0L,
                        frontVideoUri = frontUri,
                        backVideoUri = backUri
                    )
                    stopTimer()
                }
            )
            controller?.startPreviewOnly()
        }
    }

    private fun startRecording() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRecording = true,
                isPaused = false,
                recordingTime = 0L
            )
            controller?.startRecording()
            startTimer()
        }
    }

    private fun pauseRecording() {
        _uiState.value = _uiState.value.copy(isPaused = true)
        stopTimer()
    }

    private fun resumeRecording() {
        _uiState.value = _uiState.value.copy(isPaused = false)
        startTimer()
    }

    private fun stopRecording() {
        viewModelScope.launch {
            controller?.stopRecording()
            stopTimer()
        }
    }

    private fun updateRecordingTime() {
        _uiState.value = _uiState.value.copy(
            recordingTime = _uiState.value.recordingTime + 1000
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                handleIntent(CameraIntent.Tick)
            }
        }
    }

    fun resetUris() {
        _uiState.value = _uiState.value.copy(
            frontVideoUri = null,
            backVideoUri = null
        )
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun getBackTextureView(): TextureView? = controller?.getBackTextureView()
    fun getFrontTextureView(): TextureView? = controller?.getFrontTextureView()

    override fun onCleared() {
        super.onCleared()
        controller?.closeCameras()
        stopTimer()
    }
}
