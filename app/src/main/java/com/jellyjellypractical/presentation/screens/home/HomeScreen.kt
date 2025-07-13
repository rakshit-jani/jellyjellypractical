package com.jellyjellypractical.presentation.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.jellyjellypractical.presentation.screens.home.tabs.CameraScreen
import com.jellyjellypractical.presentation.screens.home.tabs.feed.FeedScreen
import com.jellyjellypractical.presentation.screens.home.tabs.GalleryScreen
import com.jellyjellypractical.ui.theme.JellyJellyPracticalTheme
import com.jellyjellypractical.utils.AppConstants

@Composable
fun HomeScreen(@Suppress("unused") navController: NavController) {
    val navController = rememberNavController()
    val tabs = listOf(
        AppConstants.Routes.Tabs.FEED,
        AppConstants.Routes.Tabs.CAMERA,
        AppConstants.Routes.Tabs.GALLERY
    )
    var selectedTab by remember { mutableStateOf(AppConstants.Routes.Tabs.FEED) }

    val onTabSelected: (Int) -> Unit = { index ->
        selectedTab = tabs[index]
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
                    AppConstants.Routes.Tabs.FEED -> FeedScreen(navController)
                    AppConstants.Routes.Tabs.CAMERA -> CameraScreen(navController)
                    AppConstants.Routes.Tabs.GALLERY -> GalleryScreen(navController)
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
                icon = {}
            )
        }
    }
}