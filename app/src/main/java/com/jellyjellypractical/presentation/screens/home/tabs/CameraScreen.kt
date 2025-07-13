package com.jellyjellypractical.presentation.screens.home.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun CameraScreen(@Suppress("unused") navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Camera Tab", modifier = Modifier.align(Alignment.Center))
    }
}

