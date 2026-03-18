package dev.kaixinguo.standalonecodepractice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.kaixinguo.standalonecodepractice.data.WorkspaceDocumentRepository
import dev.kaixinguo.standalonecodepractice.data.local.StandaloneCodePracticeDatabase
import dev.kaixinguo.standalonecodepractice.ui.workspace.LandscapeWorkspaceScreen
import dev.kaixinguo.standalonecodepractice.ui.theme.StandaloneCodePracticeTheme

class MainActivity : ComponentActivity() {
    private val workspaceDocumentRepository by lazy {
        WorkspaceDocumentRepository(
            StandaloneCodePracticeDatabase.getInstance(applicationContext).workspaceDocumentDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StandaloneCodePracticeTheme {
                LandscapeWorkspaceScreen(workspaceDocumentRepository = workspaceDocumentRepository)
            }
        }
    }
}

