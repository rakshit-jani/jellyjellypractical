package com.jellyjellypractical.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class DualCameraController(
    private val context: Context,
    private val onRecordingFinished: (frontUri: android.net.Uri?, backUri: android.net.Uri?) -> Unit
) {

    companion object {
        private const val TAG = "DualCameraController"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val supportsConcurrent = checkConcurrentCameraSupport()

    private var fileNameTimeStamp = ""
    private var currentMode: CameraMode = CameraMode.PREVIEW_ONLY

    private val backCameraState = CameraState(TextureView(context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    })
    private val frontCameraState = CameraState(TextureView(context).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    })

    fun getBackTextureView() = backCameraState.textureView
    fun getFrontTextureView() = frontCameraState.textureView

    fun startPreviewOnly() {
        currentMode = CameraMode.PREVIEW_ONLY
        setupTextureViewListeners()
    }

    fun startRecording() {
        currentMode = CameraMode.RECORDING
        fileNameTimeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        setupTextureViewListeners() // Re-setup listeners to potentially trigger recording session
    }

    fun stopRecording() {
        currentMode = CameraMode.PREVIEW_ONLY // After stopping, go back to preview
        stopRecordingCamera(backCameraState)
        stopRecordingCamera(frontCameraState)

        val backFile = getOutputFile(CameraCharacteristics.LENS_FACING_BACK)
        val frontFile = getOutputFile(CameraCharacteristics.LENS_FACING_FRONT)

        onRecordingFinished(
            if (frontFile.exists()) android.net.Uri.fromFile(frontFile) else null,
            if (backFile.exists()) android.net.Uri.fromFile(backFile) else null
        )

        // After stopping recording, re-establish preview sessions
        setupTextureViewListeners()
    }

    private fun setupTextureViewListeners() {
        setupTextureListener(backCameraState, CameraCharacteristics.LENS_FACING_BACK)
        if (supportsConcurrent) {
            setupTextureListener(frontCameraState, CameraCharacteristics.LENS_FACING_FRONT)
        }
    }

    private fun setupTextureListener(state: CameraState, lensFacing: Int) {
        // Always set the listener, even if available, to handle reconfigurations (e.g., stopping recording and going back to preview)
        state.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "onSurfaceTextureAvailable for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera. Current mode: $currentMode")
                openCamera(state, lensFacing) {
                    when (currentMode) {
                        CameraMode.PREVIEW_ONLY -> startPreview(state)
                        CameraMode.RECORDING -> prepareAndStartRecording(state, lensFacing)
                    }
                }
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "onSurfaceTextureSizeChanged for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera.")
                // Reconfigure session if size changes
                if (state.cameraDevice != null) {
                    when (currentMode) {
                        CameraMode.PREVIEW_ONLY -> startPreview(state)
                        CameraMode.RECORDING -> prepareAndStartRecording(state, lensFacing)
                    }
                }
            }
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d(TAG, "onSurfaceTextureDestroyed for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera.")
                closeCamera(state)
                return true
            }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        // If the TextureView is already available when startPreviewOnly or startRecording is called,
        // manually trigger the camera opening process.
        if (state.textureView.isAvailable && state.cameraDevice == null) { // Only open if not already opened
            Log.d(TAG, "TextureView already available for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera. Opening camera directly.")
            openCamera(state, lensFacing) {
                when (currentMode) {
                    CameraMode.PREVIEW_ONLY -> startPreview(state)
                    CameraMode.RECORDING -> prepareAndStartRecording(state, lensFacing)
                }
            }
        }
    }

    private fun openCamera(state: CameraState, lensFacing: Int, onOpened: () -> Unit) {
        val cameraId = getCameraId(lensFacing) ?: run {
            Log.e(TAG, "No camera found for lens facing: $lensFacing")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted.")
            return
        }

        try {
            if (!state.cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(cameraDevice: CameraDevice) {
                    Log.d(TAG, "Camera ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} opened.")
                    state.cameraDevice = cameraDevice
                    state.cameraOpenCloseLock.release()
                    onOpened()
                }
                override fun onDisconnected(cameraDevice: CameraDevice) {
                    Log.w(TAG, "Camera ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} disconnected.")
                    cameraDevice.close()
                    state.cameraDevice = null
                    state.cameraOpenCloseLock.release()
                }
                override fun onError(cameraDevice: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} error: $error")
                    cameraDevice.close()
                    state.cameraDevice = null
                    state.cameraOpenCloseLock.release()
                }
            }, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera: $e")
        }
    }

    private fun prepareAndStartRecording(state: CameraState, lensFacing: Int) {
        val cameraId = state.cameraDevice?.id ?: run {
            Log.e(TAG, "CameraDevice is null, cannot start recording for $lensFacing.")
            return
        }

        // Close any existing session before creating a new one for recording
        state.captureSession?.close()
        state.captureSession = null

        // Release previous media recorder if it exists
        state.mediaRecorder?.release()
        state.mediaRecorder = null

        state.mediaRecorder = prepareMediaRecorder(lensFacing, cameraId)

        val surfaceTexture = state.textureView.surfaceTexture ?: run {
            Log.e(TAG, "SurfaceTexture is null for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera.")
            return
        }
        val videoSize = getVideoSize(cameraId)
        surfaceTexture.setDefaultBufferSize(videoSize.width, videoSize.height)

        val previewSurface = Surface(surfaceTexture)
        val recorderSurface = state.mediaRecorder!!.surface

        try {
            val requestBuilder = state.cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            requestBuilder.addTarget(previewSurface)
            requestBuilder.addTarget(recorderSurface)

            @Suppress("DEPRECATION")
            state.cameraDevice!!.createCaptureSession(
                listOf(previewSurface, recorderSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "Recording session configured for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera.")
                        if (state.cameraDevice == null) return // Camera is already closed
                        state.captureSession = session
                        try {
                            session.setRepeatingRequest(requestBuilder.build(), null, cameraHandler)
                            state.mediaRecorder?.start()
                            state.isRecording = true // Update internal state here
                            Log.d(TAG, "MediaRecorder started for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera.")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start repeating request or media recorder: $e")
                        }
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure recording session for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera.")
                    }
                    override fun onReady(session: CameraCaptureSession) {
                        Log.d(TAG, "Recording session ready for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera.")
                    }
                    override fun onActive(session: CameraCaptureSession) {
                        Log.d(TAG, "Recording session active for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera.")
                    }
                    override fun onClosed(session: CameraCaptureSession) {
                        Log.d(TAG, "Recording session closed for ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"} camera.")
                    }
                },
                cameraHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording: $e")
        }
    }

    private fun stopRecordingCamera(state: CameraState) {
        state.isRecording = false // Update internal state
        try {
            state.mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording for camera: $e")
        } finally {
            state.mediaRecorder = null
        }
        // No need to close camera device or session here, as we want to transition back to preview
    }

    private fun startPreview(state: CameraState) {
        val cameraDevice = state.cameraDevice ?: run {
            Log.e(TAG, "CameraDevice is null, cannot start preview.")
            return
        }
        val textureView = state.textureView
        val surfaceTexture = textureView.surfaceTexture ?: run {
            Log.e(TAG, "SurfaceTexture is null, cannot start preview.")
            return
        }

        // Close any existing session before creating a new one for preview
        state.captureSession?.close()
        state.captureSession = null

        val targetSize = Size(1920, 1080) // Consider getting this dynamically or from characteristics
        surfaceTexture.setDefaultBufferSize(targetSize.width, targetSize.height)
        val previewSurface = Surface(surfaceTexture)

        try {
            val requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            requestBuilder.addTarget(previewSurface)

            @Suppress("DEPRECATION")
            cameraDevice.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "Preview session configured.")
                        if (state.cameraDevice == null) return
                        state.captureSession = session
                        try {
                            session.setRepeatingRequest(requestBuilder.build(), null, cameraHandler)
                            Log.d(TAG, "Preview started")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start repeating request for preview: $e")
                        }
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure preview session")
                    }
                    override fun onReady(session: CameraCaptureSession) {
                        Log.d(TAG, "Preview session ready.")
                    }
                    override fun onActive(session: CameraCaptureSession) {
                        Log.d(TAG, "Preview session active.")
                    }
                    override fun onClosed(session: CameraCaptureSession) {
                        Log.d(TAG, "Preview session closed.")
                    }
                },
                cameraHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting preview: ${e.message}", e)
        }
    }

    private fun prepareMediaRecorder(lensFacing: Int, cameraId: String): MediaRecorder {
        @Suppress("DEPRECATION") val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

        val videoSize = getVideoSize(cameraId)
        val file = getOutputFile(lensFacing)
        val orientationHint = if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) 270 else 90

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(file.absolutePath)
            setVideoEncodingBitRate(10_000_000)
            setVideoFrameRate(30)
            setVideoSize(videoSize.width, videoSize.height)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOrientationHint(orientationHint)
            prepare()
        }
        return recorder
    }

    private fun getOutputFile(lensFacing: Int): File {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        outputDir?.mkdirs() // Ensure directory exists
        val suffix = if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"
        val fileName = "VIDEO_${fileNameTimeStamp}_$suffix.mp4"
        return File(outputDir, fileName)
    }

    private fun getVideoSize(cameraId: String): Size {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        // Get the largest size that is supported for MediaRecorder
        return map?.getOutputSizes(MediaRecorder::class.java)?.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)
    }

    private fun getCameraId(lensFacing: Int): String? {
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == lensFacing) {
                return id
            }
        }
        return null
    }

    private fun checkConcurrentCameraSupport(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val concurrentCameraIds = cameraManager.concurrentCameraIds
        var frontId: String? = null
        var backId: String? = null

        for (id in cameraManager.cameraIdList) {
            when (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> frontId = id
                CameraCharacteristics.LENS_FACING_BACK -> backId = id
            }
            if (frontId != null && backId != null) break
        }

        if (frontId == null || backId == null) {
            return false // Device must have both front and back cameras to support concurrent
        }

        return concurrentCameraIds.any { it.contains(frontId) && it.contains(backId) }
    }

    fun closeCameras() {
        Log.d(TAG, "Closing all cameras and cleaning up.")
        closeCamera(backCameraState)
        closeCamera(frontCameraState)
        cameraThread.quitSafely()
        try {
            cameraThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "CameraThread interrupted while joining", e)
        }
    }

    private fun closeCamera(state: CameraState) {
        try {
            state.cameraOpenCloseLock.acquire()
            state.captureSession?.close()
            state.captureSession = null
            state.cameraDevice?.close()
            state.cameraDevice = null
            state.mediaRecorder?.release()
            state.mediaRecorder = null
            Log.d(TAG, "Camera and resources closed for a state.")
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while trying to close camera.", e)
        } finally {
            state.cameraOpenCloseLock.release()
        }
    }

    private enum class CameraMode {
        PREVIEW_ONLY,
        RECORDING
    }

    private data class CameraState(
        val textureView: TextureView,
        var cameraDevice: CameraDevice? = null,
        var captureSession: CameraCaptureSession? = null,
        var mediaRecorder: MediaRecorder? = null,
        var isRecording: Boolean = false,
        val cameraOpenCloseLock: Semaphore = Semaphore(1)
    )
}