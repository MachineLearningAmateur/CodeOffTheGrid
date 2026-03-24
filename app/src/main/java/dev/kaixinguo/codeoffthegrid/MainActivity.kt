package dev.kaixinguo.codeoffthegrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kaixinguo.codeoffthegrid.data.BundledProblemCatalogLoader
import dev.kaixinguo.codeoffthegrid.data.LocalPythonExecutionService
import dev.kaixinguo.codeoffthegrid.data.MlKitSketchCodeRecognitionService
import dev.kaixinguo.codeoffthegrid.data.ProblemCatalogRepository
import dev.kaixinguo.codeoffthegrid.data.SketchCodeRecognitionService
import dev.kaixinguo.codeoffthegrid.data.WorkspaceDocumentRepository
import dev.kaixinguo.codeoffthegrid.data.local.CodeOffTheGridDatabase
import dev.kaixinguo.codeoffthegrid.ui.theme.AppThemeMode
import dev.kaixinguo.codeoffthegrid.ui.theme.CodeOffTheGridTheme
import dev.kaixinguo.codeoffthegrid.ui.workspace.AskAiViewModel
import dev.kaixinguo.codeoffthegrid.ui.workspace.LandscapeWorkspaceScreen

class MainActivity : ComponentActivity() {
    private val database by lazy {
        CodeOffTheGridDatabase.getInstance(applicationContext)
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

    private val localPythonExecutionService by lazy {
        LocalPythonExecutionService(applicationContext)
    }

    private val sketchCodeRecognitionService: SketchCodeRecognitionService by lazy {
        MlKitSketchCodeRecognitionService()
    }

    private val askAiViewModel by lazy {
        ViewModelProvider(this)[AskAiViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appSettings = remember {
                getSharedPreferences(APP_SETTINGS_PREFS, MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(
                    AppThemeMode.fromStorageValue(
                        appSettings.getString(KEY_THEME_MODE, null)
                    )
                )
            }

            CodeOffTheGridTheme(themeMode = themeMode) {
                LandscapeWorkspaceScreen(
                    problemCatalogRepository = problemCatalogRepository,
                    workspaceDocumentRepository = workspaceDocumentRepository,
                    localPythonExecutionService = localPythonExecutionService,
                    sketchCodeRecognitionService = sketchCodeRecognitionService,
                    askAiController = askAiViewModel,
                    seedCatalogProvider = { bundledProblemCatalogLoader.loadCatalog() },
                    themeMode = themeMode,
                    onThemeModeChange = { selectedMode ->
                        themeMode = selectedMode
                        appSettings.edit()
                            .putString(KEY_THEME_MODE, selectedMode.storageValue)
                            .apply()
                    }
                )
            }
        }
    }

    private companion object {
        const val APP_SETTINGS_PREFS = "app_settings"
        const val KEY_THEME_MODE = "theme_mode"
    }
}

