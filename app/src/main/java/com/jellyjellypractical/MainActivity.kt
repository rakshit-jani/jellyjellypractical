package com.jellyjellypractical

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.jellyjellypractical.presentation.navigation.AppNavHost
import com.jellyjellypractical.presentation.screens.home.TempFile
import com.jellyjellypractical.ui.theme.JellyJellyPracticalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()// Call the function directly
        setContent {
//            TestFunction()
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    JellyJellyPracticalTheme {
        Surface {
            AppNavHost()
        }
    }
}

@Suppress("unused")
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun TestFunction() {
    val context = androidx.compose.ui.platform.LocalContext.current
    TempFile.scrapeJellyFeed(context)
}

