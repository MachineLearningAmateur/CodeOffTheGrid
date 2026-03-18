package dev.kaixinguo.standalonecodepractice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.kaixinguo.standalonecodepractice.ui.workspace.LandscapeWorkspaceScreen
import dev.kaixinguo.standalonecodepractice.ui.theme.StandaloneLeetCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StandaloneLeetCodeTheme {
                LandscapeWorkspaceScreen()
            }
        }
    }
}

