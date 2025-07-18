package com.jellyjellypractical.presentation.screens.home.tabs.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun GalleryScreen(@Suppress("unused") navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("AppGalleryScreen Tab", modifier = Modifier.align(Alignment.Center))
    }
}