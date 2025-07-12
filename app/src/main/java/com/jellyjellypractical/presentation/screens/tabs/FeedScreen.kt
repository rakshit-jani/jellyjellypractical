package com.jellyjellypractical.presentation.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun FeedScreen(@Suppress("unused") navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Feed Tab", modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
    }
}

