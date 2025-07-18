package com.jellyjellypractical.presentation.screens.home.tabs.camera

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.TextureView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jellyjellypractical.domain.intent.camera.CameraIntent
import java.util.concurrent.TimeUnit

@Composable
fun CameraScreen(
    onRecordingFinished: () -> Unit
) {
    val viewModel: CameraViewModel = hiltViewModel()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var allPermissionsGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        allPermissionsGranted = permissions.all { it.value }
        if (!allPermissionsGranted) {
            Toast.makeText(context, "Camera and audio permissions are required.", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.handleIntent(CameraIntent.InitCamera(context))
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val arePermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (arePermissionsGranted) {
            allPermissionsGranted = true
            viewModel.handleIntent(CameraIntent.InitCamera(context))
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && uiState.isRecording) {
                viewModel.handleIntent(CameraIntent.StopRecording)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    /*LaunchedEffect(Unit) {
        val permissionsToRequest = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val arePermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (arePermissionsGranted) {
            allPermissionsGranted = true
            viewModel.handleIntent(CameraIntent.InitCamera(context))
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && uiState.isRecording) {
                viewModel.handleIntent(CameraIntent.StopRecording)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }*/

    Column(modifier = Modifier.fillMaxSize()) {
        if (allPermissionsGranted) {
            // Camera Previews
            AndroidView(
                factory = { viewModel.getBackTextureView() ?: TextureView(context) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            AndroidView(
                factory = { viewModel.getFrontTextureView() ?: TextureView(context) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // Timer
            if (uiState.isRecording) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(uiState.recordingTime)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(uiState.recordingTime) % 60
                Text(
                    text = String.format("⏱ %02d:%02d", minutes, seconds),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }

            // Icon Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!uiState.isRecording) {
                    IconWithLabel(
                        icon = Icons.Default.PlayArrow,
                        label = "Start",
                        tint = Color.Green
                    ) {
                        viewModel.handleIntent(CameraIntent.StartRecording)
                    }
                } else {
                    if (uiState.isPaused) {
                        IconWithLabel(
                            icon = Icons.Default.PlayArrow,
                            label = "Resume",
                            tint = Color.Blue
                        ) {
                            viewModel.handleIntent(CameraIntent.ResumeRecording)
                        }
                    } else {
                        IconWithLabel(
                            icon = Icons.Default.Pause,
                            label = "Pause",
                            tint = Color.Yellow
                        ) {
                            viewModel.handleIntent(CameraIntent.PauseRecording)
                        }
                    }

                    IconWithLabel(
                        icon = Icons.Default.Stop,
                        label = "Stop",
                        tint = Color.Red
                    ) {
                        viewModel.handleIntent(CameraIntent.StopRecording)
                    }
                }
            }
        } else {
            // Permissions Not Granted UI
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera and audio permissions are required for video recording.",
                    modifier = Modifier.padding(16.dp)
                )
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    }
                ) {
                    Text("Grant Permissions")
                }
            }
        }

        // Trigger when URIs become available
        LaunchedEffect(uiState.frontVideoUri, uiState.backVideoUri) {
            if (uiState.frontVideoUri != null || uiState.backVideoUri != null) {
                Log.d("CameraScreen", "LaunchedEffect triggered! URIs are not null.")
                Toast.makeText(context, "Recording finished", Toast.LENGTH_SHORT).show()
                onRecordingFinished()
            }
        }
    }
}

@Composable
fun IconWithLabel(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
