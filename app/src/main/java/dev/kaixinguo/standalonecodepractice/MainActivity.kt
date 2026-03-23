package dev.kaixinguo.standalonecodepractice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.kaixinguo.standalonecodepractice.data.BundledProblemCatalogLoader
import dev.kaixinguo.standalonecodepractice.data.LocalPythonExecutionService
import dev.kaixinguo.standalonecodepractice.data.ProblemCatalogRepository
import dev.kaixinguo.standalonecodepractice.data.WorkspaceDocumentRepository
import dev.kaixinguo.standalonecodepractice.data.local.CodeOffTheGridDatabase
import dev.kaixinguo.standalonecodepractice.ui.theme.AppThemeMode
import dev.kaixinguo.standalonecodepractice.ui.theme.CodeOffTheGridTheme
import dev.kaixinguo.standalonecodepractice.ui.workspace.AskAiViewModel
import dev.kaixinguo.standalonecodepractice.ui.workspace.LandscapeWorkspaceScreen

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

