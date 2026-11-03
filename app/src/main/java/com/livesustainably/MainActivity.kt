package com.livesustainably

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livesustainably.ui.MainViewModel
import com.livesustainably.ui.navigation.RootNav
import com.livesustainably.ui.theme.LiveSustainablyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by mainViewModel.session.collectAsStateWithLifecycle()

            LiveSustainablyTheme {
                RootNav(state = state)
            }
        }
    }
}
