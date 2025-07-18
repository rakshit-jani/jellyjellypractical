package com.jellyjellypractical.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jellyjellypractical.presentation.screens.home.HomeScreen
import com.jellyjellypractical.presentation.screens.home.tabs.GalleryScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.MainScope.route,
        modifier = Modifier.padding(0.dp)
    ) {
        composable(NavigationRoutes.MainScope.route) { HomeScreen(navController) }
        composable(NavigationRoutes.Gallery.route) { GalleryScreen(navController) }
    }
}

