package dev.kaixinguo.standaloneleetcode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.kaixinguo.standaloneleetcode.ui.LandscapeWorkspaceScreen
import dev.kaixinguo.standaloneleetcode.ui.theme.StandaloneLeetCodeTheme

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
