package com.yogistudio.molkkyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yogistudio.molkkyboard.core.designsystem.component.MolkkyBoardLogotype
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MolkkyBoardTheme {
                StartScreen()
            }
        }
    }
}

/** Écran provisoire, remplacé par l'accueil au J4. */
@Composable
internal fun PlaceholderScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MolkkyBoardLogotype()
            Text(text = stringResource(R.string.placeholder_tagline), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview
@Composable
private fun PlaceholderScreenPreview() {
    MolkkyBoardTheme { PlaceholderScreen() }
}
