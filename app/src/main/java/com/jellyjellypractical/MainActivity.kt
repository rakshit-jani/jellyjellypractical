package com.jellyjellypractical

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.jellyjellypractical.ui.theme.JellyJellyPracticalTheme
import com.jellyjellypractical.presentation.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JellyJellyPracticalTheme {
                Surface {
                    AppNavHost()
                }
            }
        }
    }
}

