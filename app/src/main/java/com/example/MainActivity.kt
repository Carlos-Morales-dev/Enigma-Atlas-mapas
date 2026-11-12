package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.EnigmaAtlasScreen
import com.example.ui.theme.EnigmaAtlasTheme
import com.example.ui.theme.MysteryDarkBackground
import com.example.ui.viewmodel.MapViewModel

class MainActivity : ComponentActivity() {

    private val mapViewModel: MapViewModel by viewModels()

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EnigmaAtlasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MysteryDarkBackground
                ) {
                    EnigmaAtlasScreen(viewModel = mapViewModel)
                }
            }
        }
    }
}
