package com.jellyjellypractical.presentation.navigation

import com.jellyjellypractical.utils.AppConstants

sealed class NavigationRoutes(val route: String) {
    object MainScope : NavigationRoutes(AppConstants.Routes.HOME)
    object Feed : NavigationRoutes(AppConstants.Routes.Tabs.FEED)
    object Camera : NavigationRoutes(AppConstants.Routes.Tabs.CAMERA)
    object Gallery : NavigationRoutes(AppConstants.Routes.Tabs.GALLERY)
}
