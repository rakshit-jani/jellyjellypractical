package com.jellyjellypractical.presentation.screens.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.jellyjellypractical.presentation.screens.home.tabs.GalleryScreen
import com.jellyjellypractical.presentation.screens.home.tabs.camera.CameraScreen
import com.jellyjellypractical.presentation.screens.home.tabs.feed.FeedScreen
import com.jellyjellypractical.ui.theme.JellyJellyPracticalTheme
import com.jellyjellypractical.utils.AppConstants

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(@Suppress("unused") navController: NavController) {
    val context = LocalContext.current
    val internalNavController = rememberNavController()

    val tabs = listOf(
        AppConstants.Routes.Tabs.FEED,
        AppConstants.Routes.Tabs.CAMERA,
        AppConstants.Routes.Tabs.GALLERY
    )
    var selectedTab by remember { mutableStateOf(AppConstants.Routes.Tabs.FEED) }

    // For app settings redirection
    var pendingCameraTabClick by remember { mutableStateOf(false) }

    val permissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (permissionState.allPermissionsGranted && pendingCameraTabClick) {
            selectedTab = AppConstants.Routes.Tabs.CAMERA
            pendingCameraTabClick = false
        }
    }

    val showSettingsDialog = remember { mutableStateOf(false) }

    if (showSettingsDialog.value) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog.value = false },
            title = { Text("Permissions Required") },
            text = { Text("Please enable camera and microphone permissions in app settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog.value = false
                    pendingCameraTabClick = true
                    openAppSettings(context, settingsLauncher)
                }) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSettingsDialog.value = false
                    pendingCameraTabClick = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    val onTabSelected: (Int) -> Unit = { index ->
        val selected = tabs[index]
        if (selected == AppConstants.Routes.Tabs.CAMERA) {
            permissionState.launchMultiplePermissionRequest()
            if (permissionState.allPermissionsGranted) {
                selectedTab = AppConstants.Routes.Tabs.CAMERA
            } else if (permissionState.shouldShowRationale) {
                Toast.makeText(context, "Camera and microphone permissions are required.", Toast.LENGTH_SHORT).show()
            } else {
                showSettingsDialog.value = true
            }
        } else {
            selectedTab = selected
        }
    }

    JellyJellyPracticalTheme {
        Scaffold(
            bottomBar = {
                AppBottomNavigationBar(tabs, selectedTab, onTabSelected)
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    AppConstants.Routes.Tabs.FEED -> FeedScreen(internalNavController)
                    AppConstants.Routes.Tabs.CAMERA -> CameraScreen(
                        onRecordingFinished = {
                            selectedTab = AppConstants.Routes.Tabs.GALLERY
                        },
                    )
                    AppConstants.Routes.Tabs.GALLERY -> GalleryScreen(internalNavController)
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(tabs: List<String>, selectedTab: String, onTabSelected: (Int) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.primary) {
        tabs.forEachIndexed { index, label ->
            NavigationBarItem(
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                    selectedTextColor = MaterialTheme.colorScheme.onTertiary,
                    unselectedTextColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.6f)
                ),
                selected = selectedTab == tabs[index],
                onClick = { onTabSelected(index) },
                label = { Text(label) },
                icon = {} // You can add icons here if needed
            )
        }
    }
}

private fun openAppSettings(
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    launcher.launch(intent)
}
