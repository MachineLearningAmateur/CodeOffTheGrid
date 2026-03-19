package dev.kaixinguo.standalonecodepractice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.kaixinguo.standalonecodepractice.data.BundledProblemCatalogLoader
import dev.kaixinguo.standalonecodepractice.data.GitHubMarkdownImportService
import dev.kaixinguo.standalonecodepractice.data.LocalPythonExecutionService
import dev.kaixinguo.standalonecodepractice.data.ProblemCatalogRepository
import dev.kaixinguo.standalonecodepractice.data.WorkspaceDocumentRepository
import dev.kaixinguo.standalonecodepractice.data.local.StandaloneCodePracticeDatabase
import dev.kaixinguo.standalonecodepractice.ui.workspace.LandscapeWorkspaceScreen
import dev.kaixinguo.standalonecodepractice.ui.theme.StandaloneCodePracticeTheme

class MainActivity : ComponentActivity() {
    private val database by lazy {
        StandaloneCodePracticeDatabase.getInstance(applicationContext)
    }

    private val problemCatalogRepository by lazy {
        ProblemCatalogRepository(database.problemCatalogDao())
    }

    private val bundledProblemCatalogLoader by lazy {
        BundledProblemCatalogLoader(applicationContext)
    }

    private val workspaceDocumentRepository by lazy {
        WorkspaceDocumentRepository(
            database.workspaceDocumentDao()
        )
    }

    private val gitHubMarkdownImportService by lazy { GitHubMarkdownImportService() }
    private val localPythonExecutionService by lazy {
        LocalPythonExecutionService(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StandaloneCodePracticeTheme {
                LandscapeWorkspaceScreen(
                    problemCatalogRepository = problemCatalogRepository,
                    workspaceDocumentRepository = workspaceDocumentRepository,
                    gitHubMarkdownImportService = gitHubMarkdownImportService,
                    localPythonExecutionService = localPythonExecutionService,
                    seedCatalogProvider = { bundledProblemCatalogLoader.loadCatalog() }
                )
            }
        }
    }
}

