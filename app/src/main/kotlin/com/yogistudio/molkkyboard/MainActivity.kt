package com.yogistudio.molkkyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.navigation.MolkkyBoardNavHost
import com.yogistudio.molkkyboard.startup.StartupLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var startupLoader: StartupLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // « Plein soleil » en mémoire pour l'instant ; persisté dans DataStore au J3.
            var sunlight by rememberSaveable { mutableStateOf(false) }
            val loadState by startupLoader.state.collectAsStateWithLifecycle()
            MolkkyBoardTheme(sunlight = sunlight) {
                MolkkyBoardNavHost(loadState = loadState, sunlight = sunlight, onSunlightChange = { sunlight = it })
            }
        }
    }
}
