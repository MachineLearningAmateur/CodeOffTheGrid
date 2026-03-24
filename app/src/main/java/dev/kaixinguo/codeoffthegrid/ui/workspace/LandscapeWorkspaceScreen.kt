package dev.kaixinguo.codeoffthegrid.ui.workspace

import android.content.Context
import android.database.sqlite.SQLiteBlobTooBigException
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.kaixinguo.codeoffthegrid.R
import dev.kaixinguo.codeoffthegrid.ai.AiRuntimeController
import dev.kaixinguo.codeoffthegrid.ai.AiRuntimePhase
import dev.kaixinguo.codeoffthegrid.ai.AiRuntimeState
import dev.kaixinguo.codeoffthegrid.data.formatRecognizedPythonDraftForEditor
import dev.kaixinguo.codeoffthegrid.data.LocalPythonExecutionService
import dev.kaixinguo.codeoffthegrid.data.SketchCodeRecognitionService
import dev.kaixinguo.codeoffthegrid.data.ProblemCatalogRepository
import dev.kaixinguo.codeoffthegrid.data.WorkspaceDocumentRepository
import dev.kaixinguo.codeoffthegrid.ui.theme.AppBackground
import dev.kaixinguo.codeoffthegrid.ui.theme.AppThemeMode
import dev.kaixinguo.codeoffthegrid.ui.theme.SidebarBackground
import dev.kaixinguo.codeoffthegrid.ui.theme.CodeOffTheGridTheme
import dev.kaixinguo.codeoffthegrid.ui.theme.TextPrimary
import dev.kaixinguo.codeoffthegrid.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MIN_WORKSPACE_PREPARATION_MILLIS = 3_000L

@Composable
internal fun LandscapeWorkspaceScreen(
    problemCatalogRepository: ProblemCatalogRepository,
    workspaceDocumentRepository: WorkspaceDocumentRepository,
    localPythonExecutionService: LocalPythonExecutionService,
    sketchCodeRecognitionService: SketchCodeRecognitionService,
    askAiController: AskAiSessionController,
    seedCatalogProvider: suspend () -> List<ProblemFolderState>,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val logTag = "SketchCodeRecognition"
    val context = LocalContext.current
    val folders = remember { mutableStateListOf<ProblemFolderState>() }
    var protectedFolderIds by remember { mutableStateOf(emptySet<String>()) }
    var protectedSetIds by remember { mutableStateOf(emptySet<String>()) }
    var protectedProblemIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedProblemId by rememberSaveable { mutableStateOf("") }
    var selectedProblemSetId by rememberSaveable { mutableStateOf("") }
    var sidebarMode by rememberSaveable { mutableStateOf(SidebarMode.Problems) }
    var sidebarCollapsed by rememberSaveable { mutableStateOf(false) }
    var supportPaneCollapsed by rememberSaveable { mutableStateOf(false) }
    var askAiFullscreen by rememberSaveable { mutableStateOf(false) }
    var workspaceInputMode by rememberSaveable { mutableStateOf(WorkspaceInputMode.Keyboard) }
    val sketchStrokes = remember { mutableStateListOf<SketchStroke>() }
    var currentDraftCode by remember { mutableStateOf("") }
    var currentRecognizedCodeDraft by remember { mutableStateOf("") }
    var currentCustomTestSuite by remember { mutableStateOf(ProblemTestSuite()) }
    var currentCustomExecutionResult by remember { mutableStateOf(defaultExecutionResult(ExecutionTarget.Custom)) }
    var currentSubmissionExecutionResult by remember { mutableStateOf(defaultExecutionResult(ExecutionTarget.LocalSubmission)) }
    var selectedSupportTab by rememberSaveable { mutableStateOf(SupportTab.Problem) }
    var composerSession by remember { mutableStateOf<ProblemComposerSession?>(null) }
    var showDiscardComposerDialog by remember { mutableStateOf(false) }
    var showRecognizedCodeDraftDialog by remember { mutableStateOf(false) }
    var recognitionErrorMessage by remember { mutableStateOf<String?>(null) }
    var isRecognizingSketchCode by remember { mutableStateOf(false) }
    var isPreparingWorkspace by remember { mutableStateOf(true) }
    var workspacePreparationErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSketchRecognitionModelReady by remember { mutableStateOf(false) }
    var requiresWifiRestartForRecognitionModel by remember { mutableStateOf(false) }
    var workspacePreparationPhase by remember { mutableStateOf("") }
    var preparingWorkspaceMessageIndex by remember { mutableIntStateOf(0) }
    var preparingWorkspaceBounceTick by remember { mutableIntStateOf(0) }
    var catalogVersion by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarCollapsed) 84.dp else 288.dp,
        animationSpec = spring(),
        label = "sidebarWidth"
    )
    val supportPaneWidth by animateDpAsState(
        targetValue = if (supportPaneCollapsed) 84.dp else 356.dp,
        animationSpec = spring(),
        label = "supportPaneWidth"
    )
    val preparingWorkspaceMessages = remember {
        listOf(
            "Preparing workspace",
            "Doodling...",
            "Cooking...",
            "Warming up the sketch pad...",
            "Teaching scribbles to become code..."
        )
    }
    val loadingLogoRotation by rememberInfiniteTransition(label = "loadingLogoRotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingLogoRotationValue"
    )

    suspend fun prepareWorkspaceResources() {
        val preparationStartMillis = System.currentTimeMillis()
        var enforceMinimumPreparationDuration = false
        workspacePreparationErrorMessage = null
        workspacePreparationPhase = ""
        isPreparingWorkspace = true
        try {
            isSketchRecognitionModelReady = sketchCodeRecognitionService.isModelDownloaded()
            if (isSketchRecognitionModelReady) {
                requiresWifiRestartForRecognitionModel = false
                enforceMinimumPreparationDuration = true
                return
            }
            if (!isWifiConnected(context)) {
                requiresWifiRestartForRecognitionModel = true
                return
            }
            requiresWifiRestartForRecognitionModel = false
            enforceMinimumPreparationDuration = true
            if (!isSketchRecognitionModelReady) {
                workspacePreparationPhase = "Downloading handwriting model..."
                sketchCodeRecognitionService.downloadModel()
                workspacePreparationPhase = "Finishing setup..."
                isSketchRecognitionModelReady = true
            }
        } catch (error: Throwable) {
            workspacePreparationErrorMessage = error.message
                ?: "Couldn't prepare the handwriting model for sketch conversion."
        } finally {
            if (enforceMinimumPreparationDuration) {
                val elapsedMillis = System.currentTimeMillis() - preparationStartMillis
                val remainingMillis = MIN_WORKSPACE_PREPARATION_MILLIS - elapsedMillis
                if (remainingMillis > 0L) {
                    delay(remainingMillis)
                }
            }
            isPreparingWorkspace = false
        }
    }

    val catalogSnapshotVersion = catalogVersion
    val allSets = catalogSnapshotVersion.let {
        folders.flatMap { folder -> folder.sets.toList() }
    }
    val selectedProblemSet = allSets
        .firstOrNull { it.id == selectedProblemSetId }
        ?: allSets.firstOrNull()
    val selectedProblem = selectedProblemSet?.problems
        ?.firstOrNull { it.id == selectedProblemId }
        ?: selectedProblemSet?.problems?.firstOrNull()
        ?: allSets.flatMap { it.problems }.firstOrNull()
    val composerPreviewProblem = composerSession?.draft?.toPreviewProblem()
    val effectiveSidebarMode = if (composerSession != null) SidebarMode.AskAi else sidebarMode
    val composerDestinations = catalogSnapshotVersion.let { buildProblemComposerDestinations(folders) }

    LaunchedEffect(Unit) {
        prepareWorkspaceResources()
    }

    LaunchedEffect(Unit) {
        val seedFolders = seedCatalogProvider()
        protectedFolderIds = seedFolders.map { it.id }.toSet()
        protectedSetIds = seedFolders.flatMap { it.sets }.map { it.id }.toSet()
        protectedProblemIds = seedFolders
            .flatMap { it.sets }
            .flatMap { it.problems }
            .map { it.id }
            .toSet()
        val loadedFolders = try {
            problemCatalogRepository.loadCatalog()
        } catch (_: SQLiteBlobTooBigException) {
            problemCatalogRepository.persistCatalog(seedFolders)
            emptyList()
        }
        val resolvedFolders = when {
            loadedFolders.isEmpty() -> {
                problemCatalogRepository.persistCatalog(seedFolders)
                seedFolders.deepCopy()
            }
            loadedFolders.all { it.id in LEGACY_TEMPLATE_FOLDER_IDS } -> {
                problemCatalogRepository.persistCatalog(seedFolders)
                seedFolders.deepCopy()
            }
            else -> {
                val synchronizedFolders = synchronizeSeedCatalog(
                    loadedFolders = loadedFolders,
                    seedFolders = seedFolders
                )
                if (catalogChanged(loadedFolders, synchronizedFolders)) {
                    problemCatalogRepository.persistCatalog(synchronizedFolders)
                    synchronizedFolders.deepCopy()
                } else {
                    loadedFolders
                }
            }
        }
        folders.clear()
        folders.addAll(resolvedFolders)
        val (initialSetId, initialProblemId) = resolveSelection(
            folders = resolvedFolders,
            preferredSetId = selectedProblemSetId,
            preferredProblemId = selectedProblemId
        )
        selectedProblemSetId = initialSetId
        selectedProblemId = initialProblemId
    }

    LaunchedEffect(isPreparingWorkspace, workspacePreparationErrorMessage) {
        if (!isPreparingWorkspace || workspacePreparationErrorMessage != null) {
            preparingWorkspaceMessageIndex = 0
            preparingWorkspaceBounceTick = 0
            return@LaunchedEffect
        }
        while (isPreparingWorkspace && workspacePreparationErrorMessage == null) {
            val currentMessage = preparingWorkspaceMessages[preparingWorkspaceMessageIndex]
            preparingWorkspaceBounceTick += 1
            val waveDurationMillis = (currentMessage.length * 90L) + 1100L
            delay(waveDurationMillis)
            preparingWorkspaceMessageIndex =
                (preparingWorkspaceMessageIndex + 1) % preparingWorkspaceMessages.size
        }
    }

    LaunchedEffect(selectedProblem?.id) {
        if (selectedProblem != null) {
            currentDraftCode = selectedProblem.starterCode
            currentCustomTestSuite = ProblemTestSuite(draft = selectedProblem.customTests)
            currentCustomExecutionResult = defaultExecutionResult(ExecutionTarget.Custom)
            currentSubmissionExecutionResult = defaultExecutionResult(ExecutionTarget.LocalSubmission)
            sketchStrokes.clear()

            val document = workspaceDocumentRepository.loadDocument(selectedProblem)
            currentDraftCode = document.draftCode
            currentRecognizedCodeDraft = document.recognizedCodeDraft
            currentCustomTestSuite = document.customTestSuite
            sketchStrokes.clear()
            sketchStrokes.addAll(document.sketches)
            showRecognizedCodeDraftDialog = false
            recognitionErrorMessage = null
            isRecognizingSketchCode = false
        } else {
            currentDraftCode = ""
            currentRecognizedCodeDraft = ""
            currentCustomTestSuite = ProblemTestSuite()
            currentCustomExecutionResult = defaultExecutionResult(ExecutionTarget.Custom)
            currentSubmissionExecutionResult = defaultExecutionResult(ExecutionTarget.LocalSubmission)
            sketchStrokes.clear()
            showRecognizedCodeDraftDialog = false
            recognitionErrorMessage = null
            isRecognizingSketchCode = false
        }
    }

    LaunchedEffect(effectiveSidebarMode) {
        if (effectiveSidebarMode != SidebarMode.AskAi) {
            askAiFullscreen = false
        }
    }

    LaunchedEffect(composerSession != null) {
        askAiController.syncContext(composerActive = composerSession != null)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SidebarBackground, AppBackground)
                )
            )
            .padding(20.dp)
    ) {
        fun persistCatalogSnapshot() {
            catalogVersion += 1
            val snapshot = folders.deepCopy()
            scope.launch {
                problemCatalogRepository.persistCatalog(snapshot)
            }
        }

        fun persistWorkspaceDocument(
            problemId: String,
            draftCode: String,
            customTestSuite: ProblemTestSuite,
            sketches: List<SketchStroke>,
            recognizedCodeDraft: String
        ) {
            scope.launch {
                workspaceDocumentRepository.saveDocument(
                    problemId = problemId,
                    draftCode = draftCode,
                    customTestSuite = customTestSuite,
                    sketches = sketches,
                    recognizedCodeDraft = recognizedCodeDraft
                )
            }
        }

        fun startSketchCodeRecognition(problemId: String, strokesSnapshot: List<SketchStroke>) {
            scope.launch {
                isRecognizingSketchCode = true
                recognitionErrorMessage = null
                try {
                    if (!isSketchRecognitionModelReady) {
                        recognitionErrorMessage = when {
                            requiresWifiRestartForRecognitionModel ->
                                "Sketch conversion needs a one-time handwriting model install. Restart the app while connected to Wi-Fi to install it."
                            workspacePreparationErrorMessage != null ->
                            "Handwriting recognition is not ready yet. Retry workspace preparation and try again."
                            else -> "Preparing handwriting recognition. Try again in a moment."
                        }
                        return@launch
                    }

                    Log.i(logTag, "Starting code recognition for ${strokesSnapshot.size} strokes")
                    val recognizedCode = sketchCodeRecognitionService.recognizeCode(strokesSnapshot)
                    Log.i(logTag, "Code recognition completed; length=${recognizedCode.length}")
                    if (recognizedCode.isBlank()) {
                        recognitionErrorMessage = "No code could be recognized from the code-pen strokes."
                        return@launch
                    }

                    currentRecognizedCodeDraft = recognizedCode
                    showRecognizedCodeDraftDialog = true
                    persistWorkspaceDocument(
                        problemId = problemId,
                        draftCode = currentDraftCode,
                        customTestSuite = currentCustomTestSuite,
                        sketches = strokesSnapshot,
                        recognizedCodeDraft = recognizedCode
                    )
                } catch (error: Throwable) {
                    Log.e(logTag, "Sketch code recognition failed", error)
                    recognitionErrorMessage = error.message ?: "Sketch code recognition failed."
                } finally {
                    isRecognizingSketchCode = false
                }
            }
        }

        fun applyRecognizedCodeDraft(problemId: String, append: Boolean) {
            val normalizedDraft = currentRecognizedCodeDraft.trimEnd()
            if (normalizedDraft.isBlank()) return

            currentDraftCode = formatRecognizedPythonDraftForEditor(
                recognizedDraft = normalizedDraft,
                currentDraftCode = currentDraftCode,
                append = append
            )
            if (append) {
                val preservedSketches = sketchStrokes.filter { it.kind != SketchStrokeKind.Code }
                sketchStrokes.clear()
                sketchStrokes.addAll(preservedSketches)
            }
            currentRecognizedCodeDraft = ""
            showRecognizedCodeDraftDialog = false
            persistWorkspaceDocument(
                problemId = problemId,
                draftCode = currentDraftCode,
                customTestSuite = currentCustomTestSuite,
                sketches = sketchStrokes.toList(),
                recognizedCodeDraft = ""
            )
        }

        fun openProblemComposer() {
            askAiFullscreen = false
            composerSession = ProblemComposerSession()
        }

        fun dismissProblemComposer() {
            val session = composerSession ?: return
            if (session.isDirty()) {
                showDiscardComposerDialog = true
            } else {
                composerSession = null
            }
        }

        fun saveComposedProblem() {
            val session = composerSession ?: return
            val validationError = session.draft.validationError(
                availableSetIds = composerDestinations.map { it.setId }.toSet()
            )
            if (validationError != null) {
                composerSession = session.copy(errorMessage = validationError)
                return
            }

            val targetSet = folders
                .flatMap { it.sets }
                .firstOrNull { it.id == session.draft.destinationSetId }
                ?: run {
                    composerSession = session.copy(
                        errorMessage = "Choose a valid destination set before saving."
                    )
                    return
                }

            val savedProblem = session.draft.toSavedProblem()
            targetSet.problems.add(savedProblem)
            selectedProblemSetId = targetSet.id
            selectedProblemId = savedProblem.id
            selectedSupportTab = SupportTab.Problem
            updateActiveProblemSelection(folders, selectedProblemSetId, selectedProblemId)
            persistCatalogSnapshot()
            composerSession = null
        }

        LaunchedEffect(askAiController.pendingSideEffect) {
            when (val pendingSideEffect = askAiController.pendingSideEffect) {
                is AskAiPendingSideEffect.GeneratedCustomTests -> {
                    val activeProblem = selectedProblem
                    if (activeProblem != null && activeProblem.id == pendingSideEffect.problemId) {
                        val mergedSuite = mergeCustomTestSuites(
                            existingSuite = currentCustomTestSuite,
                            generatedSuite = pendingSideEffect.generatedSuite
                        )
                        currentCustomTestSuite = mergedSuite
                        selectedSupportTab = SupportTab.Custom
                        persistWorkspaceDocument(
                            problemId = activeProblem.id,
                            draftCode = currentDraftCode,
                            customTestSuite = mergedSuite,
                            sketches = sketchStrokes.toList(),
                            recognizedCodeDraft = currentRecognizedCodeDraft
                        )
                        askAiController.completePendingSideEffect(
                            sender = AiChatSender.Assistant,
                            message = buildString {
                                append("Imported ${pendingSideEffect.generatedSuite.cases.size} custom cases into the Custom tab.\n\n")
                                append(formatGeneratedTestSuiteSummary(pendingSideEffect.generatedSuite))
                            }
                        )
                    } else {
                        askAiController.completePendingSideEffect(
                            sender = AiChatSender.System,
                            message = "Generated ${pendingSideEffect.generatedSuite.cases.size} custom cases, but the active problem changed before they could be imported."
                        )
                    }
                }

                is AskAiPendingSideEffect.GeneratedProblem -> {
                    val currentSession = composerSession
                    if (currentSession != null) {
                        val updatedDraft = currentSession.draft.withGeneratedProblem(pendingSideEffect.generatedProblem)
                        composerSession = currentSession.copy(
                            draft = updatedDraft,
                            errorMessage = null
                        )
                        askAiController.completePendingSideEffect(
                            sender = AiChatSender.Assistant,
                            message = buildGeneratedProblemDraftSummary(
                                previousDraft = currentSession.draft,
                                updatedDraft = updatedDraft
                            )
                        )
                    } else {
                        askAiController.completePendingSideEffect(
                            sender = AiChatSender.System,
                            message = buildGeneratedProblemResultSummary(pendingSideEffect.generatedProblem)
                        )
                    }
                }

                null -> Unit
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SidebarPane(
                folders = folders,
                selectedProblemSetId = selectedProblemSet?.id.orEmpty(),
                selectedProblemId = selectedProblem?.id.orEmpty(),
                onProblemSetSelected = { setId ->
                    selectedProblemSetId = setId
                    val set = folders.flatMap { it.sets }.firstOrNull { it.id == setId }
                    if (set != null) {
                        val activeProblem = set.problems.firstOrNull { it.active } ?: set.problems.firstOrNull()
                        if (activeProblem != null) {
                            selectedProblemId = activeProblem.id
                            updateActiveProblemSelection(folders, selectedProblemSetId, selectedProblemId)
                            persistCatalogSnapshot()
                        }
                    }
                },
                onProblemSelected = { setId, problemId ->
                    selectedProblemSetId = setId
                    selectedProblemId = problemId
                    val set = folders.flatMap { it.sets }.firstOrNull { it.id == setId }
                    if (set != null) {
                        val updated = set.problems.map { it.copy(active = it.id == problemId) }
                        set.problems.clear()
                        set.problems.addAll(updated)
                        persistCatalogSnapshot()
                    }
                },
                onDeleteProblem = { setId, problem ->
                    if (problem.id in protectedProblemIds) return@SidebarPane
                    val set = folders.flatMap { it.sets }.firstOrNull { it.id == setId } ?: return@SidebarPane
                    val removingSelected = selectedProblemSetId == setId && selectedProblemId == problem.id
                    set.problems.removeAll { it.id == problem.id }
                    if (removingSelected) {
                        val fallback = set.problems.firstOrNull()
                            ?: folders.flatMap { it.sets }.firstOrNull { it.problems.isNotEmpty() }?.problems?.firstOrNull()
                        if (fallback != null) {
                            selectedProblemSetId = allSets
                                .first { candidate -> candidate.problems.any { it.id == fallback.id } }.id
                            selectedProblemId = fallback.id
                        } else {
                            selectedProblemSetId = ""
                            selectedProblemId = ""
                        }
                    }
                    updateActiveProblemSelection(folders, selectedProblemSetId, selectedProblemId)
                    persistCatalogSnapshot()
                },
                onCreateFolder = { folderTitle ->
                    folders.add(
                        ProblemFolderState(
                            title = folderTitle,
                            sets = mutableStateListOf()
                        )
                    )
                    persistCatalogSnapshot()
                },
                onCreateSet = { folderId, requestedTitle ->
                    val folder = folders.firstOrNull { it.id == folderId } ?: return@SidebarPane
                    val requestedBaseTitle = requestedTitle.trim().ifBlank { "New Set" }
                    val existingTitles = folder.sets.map { it.title }.toSet()
                    var index = 1
                    var candidate = requestedBaseTitle
                    while (candidate in existingTitles) {
                        index += 1
                        candidate = "$requestedBaseTitle $index"
                    }
                    folder.sets.add(
                        ProblemSetState(
                            title = candidate,
                            problems = mutableStateListOf()
                        )
                    )
                    if (selectedProblemSet == null) {
                        selectedProblemSetId = folder.sets.last().id
                        selectedProblemId = ""
                    }
                    persistCatalogSnapshot()
                },
                onDeleteSet = { setId ->
                    if (setId in protectedSetIds) return@SidebarPane
                    val folder = folders.firstOrNull { folderState ->
                        folderState.sets.any { it.id == setId }
                    } ?: return@SidebarPane
                    val deletingSelected = selectedProblemSetId == setId
                    folder.sets.removeAll { it.id == setId }
                    if (deletingSelected) {
                        val fallbackSet = folders.flatMap { it.sets }.firstOrNull()
                        val fallbackProblem = fallbackSet?.problems?.firstOrNull()
                        selectedProblemSetId = fallbackSet?.id.orEmpty()
                        selectedProblemId = fallbackProblem?.id.orEmpty()
                    }
                    updateActiveProblemSelection(folders, selectedProblemSetId, selectedProblemId)
                    persistCatalogSnapshot()
                },
                onDeleteFolder = { folderId ->
                    if (folderId in protectedFolderIds) return@SidebarPane
                    val folderIndex = folders.indexOfFirst { it.id == folderId }
                    if (folderIndex == -1) return@SidebarPane
                    val deletingFolder = folders[folderIndex]
                    val deletingSelected = deletingFolder.sets.any { it.id == selectedProblemSetId }
                    folders.removeAt(folderIndex)
                    if (deletingSelected) {
                        val fallbackSet = folders.flatMap { it.sets }.firstOrNull()
                        val fallbackProblem = fallbackSet?.problems?.firstOrNull()
                        selectedProblemSetId = fallbackSet?.id.orEmpty()
                        selectedProblemId = fallbackProblem?.id.orEmpty()
                    }
                    updateActiveProblemSelection(folders, selectedProblemSetId, selectedProblemId)
                    persistCatalogSnapshot()
                },
                onMoveProblem = { sourceSetId, targetSetId, problem, targetIndex ->
                    val sourceSet = folders.flatMap { it.sets }.firstOrNull { it.id == sourceSetId } ?: return@SidebarPane
                    val targetSet = folders.flatMap { it.sets }.firstOrNull { it.id == targetSetId } ?: return@SidebarPane
                    val sourceIndex = sourceSet.problems.indexOfFirst { it.id == problem.id }
                    if (sourceIndex == -1) return@SidebarPane
                    if (sourceSetId == targetSetId && (targetIndex == sourceIndex || targetIndex == sourceIndex + 1)) {
                        return@SidebarPane
                    }
                    sourceSet.problems.removeAll { it.id == problem.id }
                    val adjustedIndex = if (sourceSetId == targetSetId && sourceIndex < targetIndex) {
                        targetIndex - 1
                    } else {
                        targetIndex
                    }.coerceIn(0, targetSet.problems.size)
                    targetSet.problems.add(adjustedIndex, problem.copy(active = false))

                    if (selectedProblemId == problem.id && selectedProblemSetId == sourceSetId) {
                        selectedProblemSetId = targetSetId
                        selectedProblemId = problem.id
                    }
                    folders.flatMap { it.sets }.forEach { set ->
                        val updated = set.problems.map { item ->
                            item.copy(active = set.id == selectedProblemSetId && item.id == selectedProblemId)
                        }
                        set.problems.clear()
                        set.problems.addAll(updated)
                    }
                    persistCatalogSnapshot()
                },
                onOpenProblemComposer = { openProblemComposer() },
                problemComposerActive = composerSession != null,
                selectedMode = effectiveSidebarMode,
                onModeSelected = { mode ->
                    if (composerSession == null) {
                        sidebarMode = mode
                    }
                },
                collapsed = sidebarCollapsed,
                onToggleCollapsed = { sidebarCollapsed = !sidebarCollapsed },
                protectedFolderIds = protectedFolderIds,
                protectedSetIds = protectedSetIds,
                protectedProblemIds = protectedProblemIds,
                askAiFullscreen = askAiFullscreen,
                onToggleAskAiFullscreen = {
                    askAiFullscreen = !askAiFullscreen
                    if (askAiFullscreen) {
                        sidebarCollapsed = false
                    }
                },
                selectedProblem = selectedProblem,
                composerSession = composerSession,
                askAiController = askAiController,
                currentDraftCode = currentDraftCode,
                currentCustomTestSuite = currentCustomTestSuite,
                customExecutionResult = currentCustomExecutionResult,
                submissionExecutionResult = currentSubmissionExecutionResult,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                modifier = Modifier
                    .then(
                        if (askAiFullscreen && effectiveSidebarMode == SidebarMode.AskAi) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier.width(sidebarWidth)
                        }
                    )
                    .fillMaxHeight()
            )
            if (!askAiFullscreen) {
                if (composerSession != null) {
                    ProblemComposerPane(
                        session = composerSession!!,
                        destinations = composerDestinations,
                        onSessionChange = { composerSession = it },
                        onSave = { saveComposedProblem() },
                        onCancel = { dismissProblemComposer() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else if (selectedProblem != null) {
                    WorkspacePane(
                        problem = selectedProblem,
                        draftCode = currentDraftCode,
                        sketchStrokes = sketchStrokes,
                        onDraftCodeChange = { updatedCode ->
                            currentDraftCode = updatedCode
                            persistWorkspaceDocument(
                                problemId = selectedProblem.id,
                                draftCode = updatedCode,
                                customTestSuite = currentCustomTestSuite,
                                sketches = sketchStrokes.toList(),
                                recognizedCodeDraft = currentRecognizedCodeDraft
                            )
                        },
                        onSketchesChange = { updatedSketches ->
                            val hasCodeStrokeChanges = codeStrokesChanged(
                                previous = sketchStrokes.toList(),
                                updated = updatedSketches
                            )
                            sketchStrokes.clear()
                            sketchStrokes.addAll(updatedSketches)
                            if (hasCodeStrokeChanges) {
                                currentRecognizedCodeDraft = ""
                                showRecognizedCodeDraftDialog = false
                            }
                            persistWorkspaceDocument(
                                problemId = selectedProblem.id,
                                draftCode = currentDraftCode,
                                customTestSuite = currentCustomTestSuite,
                                sketches = updatedSketches,
                                recognizedCodeDraft = if (hasCodeStrokeChanges) "" else currentRecognizedCodeDraft
                            )
                        },
                        recognizedCodeDraft = currentRecognizedCodeDraft,
                        isRecognizingCode = isRecognizingSketchCode,
                        onConvertCode = {
                            startSketchCodeRecognition(
                                problemId = selectedProblem.id,
                                strokesSnapshot = sketchStrokes.toList()
                            )
                        },
                        onOpenRecognizedCodeDraft = {
                            if (currentRecognizedCodeDraft.isNotBlank()) {
                                showRecognizedCodeDraftDialog = true
                            }
                        },
                        inputMode = workspaceInputMode,
                        onInputModeChange = { workspaceInputMode = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                val problemPaneProblem = composerPreviewProblem ?: selectedProblem
                if (problemPaneProblem != null) {
                    Box(
                        modifier = Modifier
                            .width(supportPaneWidth)
                            .fillMaxHeight()
                    ) {
                        if (!supportPaneCollapsed) {
                            ProblemPane(
                                problem = problemPaneProblem,
                                draftCode = currentDraftCode,
                                selectedTab = if (composerSession != null) SupportTab.Problem else selectedSupportTab,
                                onSelectedTabChange = { selectedSupportTab = it },
                                onSolvedChange = { solved ->
                                    if (composerSession != null || selectedProblem == null) return@ProblemPane
                                    val set = folders
                                        .flatMap { it.sets }
                                        .firstOrNull { it.id == selectedProblemSetId }
                                        ?: return@ProblemPane
                                    val updatedProblems = set.problems.map { item ->
                                        if (item.id == selectedProblem.id) {
                                            item.copy(solved = solved)
                                        } else {
                                            item
                                        }
                                    }
                                    set.problems.clear()
                                    set.problems.addAll(updatedProblems)
                                    persistCatalogSnapshot()
                                },
                                customTestSuite = currentCustomTestSuite,
                                onCustomTestSuiteChange = { updatedCustomTestSuite ->
                                    if (composerSession != null || selectedProblem == null) return@ProblemPane
                                    currentCustomTestSuite = updatedCustomTestSuite
                                    persistWorkspaceDocument(
                                        problemId = selectedProblem.id,
                                        draftCode = currentDraftCode,
                                        customTestSuite = updatedCustomTestSuite,
                                        sketches = sketchStrokes.toList(),
                                        recognizedCodeDraft = currentRecognizedCodeDraft
                                    )
                                },
                                onExecutionResultChange = { result ->
                                    when (result.target) {
                                        ExecutionTarget.Custom -> currentCustomExecutionResult = result
                                        ExecutionTarget.LocalSubmission -> currentSubmissionExecutionResult = result
                                    }
                                },
                                localPythonExecutionService = localPythonExecutionService,
                                previewMode = composerSession != null,
                                previewComments = composerSession?.draft?.summary,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 64.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(top = 12.dp, start = 12.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.width(44.dp),
                                contentAlignment = if (supportPaneCollapsed) Alignment.Center else Alignment.CenterStart
                            ) {
                                ArrowActionChip(
                                    direction = if (supportPaneCollapsed) ArrowDirection.Collapse else ArrowDirection.Expand,
                                    onClick = { supportPaneCollapsed = !supportPaneCollapsed },
                                    contentDescription = if (supportPaneCollapsed) {
                                        "Expand support pane"
                                    } else {
                                        "Collapse support pane"
                                    }
                                )
                            }
                            if (!supportPaneCollapsed) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "CodeOffTheGrid",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.width(44.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showDiscardComposerDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardComposerDialog = false },
                title = {
                    Text(
                        text = "Discard draft?",
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = "This will close the problem composer and lose the unsaved draft.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardComposerDialog = false
                            composerSession = null
                        }
                    ) {
                        Text("Discard")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDiscardComposerDialog = false }
                    ) {
                        Text("Keep Editing")
                    }
                }
            )
        }

        if (showRecognizedCodeDraftDialog && selectedProblem != null) {
            AlertDialog(
                onDismissRequest = { showRecognizedCodeDraftDialog = false },
                title = {
                    Text(
                        text = "Recognized Code Draft",
                        color = TextPrimary
                    )
                },
                text = {
                    EditableSupportTextBlock(
                        value = currentRecognizedCodeDraft,
                        onValueChange = { updatedDraft ->
                            currentRecognizedCodeDraft = updatedDraft
                            persistWorkspaceDocument(
                                problemId = selectedProblem.id,
                                draftCode = currentDraftCode,
                                customTestSuite = currentCustomTestSuite,
                                sketches = sketchStrokes.toList(),
                                recognizedCodeDraft = updatedDraft
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 180.dp,
                        maxHeight = 320.dp,
                        scrollable = true
                    )
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                applyRecognizedCodeDraft(
                                    problemId = selectedProblem.id,
                                    append = false
                                )
                            }
                        ) {
                            Text("Replace Editor")
                        }
                        TextButton(
                            onClick = {
                                applyRecognizedCodeDraft(
                                    problemId = selectedProblem.id,
                                    append = true
                                )
                            }
                        ) {
                            Text("Append")
                        }
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showRecognizedCodeDraftDialog = false }) {
                            Text("Close")
                        }
                        TextButton(
                            onClick = {
                                currentRecognizedCodeDraft = ""
                                showRecognizedCodeDraftDialog = false
                                persistWorkspaceDocument(
                                    problemId = selectedProblem.id,
                                    draftCode = currentDraftCode,
                                    customTestSuite = currentCustomTestSuite,
                                    sketches = sketchStrokes.toList(),
                                    recognizedCodeDraft = ""
                                )
                            }
                        ) {
                            Text("Discard")
                        }
                    }
                }
            )
        }

        if (recognitionErrorMessage != null) {
            AlertDialog(
                onDismissRequest = { recognitionErrorMessage = null },
                title = {
                    Text(
                        text = "Sketch Conversion Error",
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = recognitionErrorMessage.orEmpty(),
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = { recognitionErrorMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }

        if (isPreparingWorkspace || workspacePreparationErrorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (workspacePreparationErrorMessage == null) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_loader_logo),
                            contentDescription = "CodeOffTheGrid loading",
                            modifier = Modifier
                                .size(84.dp)
                                .graphicsLayer(rotationZ = loadingLogoRotation)
                        )
                        Text(
                            text = "CodeOffTheGrid",
                            color = TextPrimary
                        )
                        BouncyLoaderText(
                            text = preparingWorkspaceMessages[preparingWorkspaceMessageIndex],
                            bounceTick = preparingWorkspaceBounceTick,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (workspacePreparationPhase.isNotBlank()) {
                            Text(
                                text = workspacePreparationPhase,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Text(
                            text = "CodeOffTheGrid",
                            color = TextPrimary
                        )
                        Text(
                            text = "Workspace preparation paused",
                            color = TextPrimary
                        )
                        Text(
                            text = workspacePreparationErrorMessage.orEmpty(),
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        prepareWorkspaceResources()
                                    }
                                }
                            ) {
                                Text("Retry")
                            }
                            TextButton(
                                onClick = {
                                    workspacePreparationErrorMessage = null
                                    isPreparingWorkspace = false
                                }
                            ) {
                                Text("Skip For Now")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun codeStrokesChanged(
    previous: List<SketchStroke>,
    updated: List<SketchStroke>
): Boolean {
    return previous.filter { it.kind == SketchStrokeKind.Code } !=
        updated.filter { it.kind == SketchStrokeKind.Code }
}

private fun isWifiConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

@Composable
private fun BouncyLoaderText(
    text: String,
    bounceTick: Int,
    modifier: Modifier = Modifier
) {
    val characterOffsets = remember(text) {
        List(text.length) { Animatable(0f) }
    }
    val characterScales = remember(text) {
        List(text.length) { Animatable(1f) }
    }

    LaunchedEffect(text, bounceTick) {
        characterOffsets.forEachIndexed { index, offset ->
            launch {
                val scale = characterScales[index]
                delay(index * 90L)
                offset.snapTo(0f)
                scale.snapTo(1f)
                offset.animateTo(
                    targetValue = -18f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                scale.animateTo(
                    targetValue = 1.18f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                offset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        text.forEachIndexed { index, character ->
            Text(
                text = character.toString(),
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.graphicsLayer(
                    translationY = characterOffsets[index].value,
                    scaleX = characterScales[index].value,
                    scaleY = characterScales[index].value
                )
            )
        }
    }
}

private fun mergeCustomTestSuites(
    existingSuite: ProblemTestSuite,
    generatedSuite: ProblemTestSuite
): ProblemTestSuite {
    val mergedCases = (existingSuite.cases + generatedSuite.cases).mapIndexed { index, testCase ->
        testCase.copy(label = "Case ${index + 1}")
    }
    return existingSuite.copy(cases = mergedCases)
}

private fun defaultExecutionResult(target: ExecutionTarget): ProblemExecutionResult {
    return ProblemExecutionResult(
        target = target,
        status = ExecutionStatus.Idle,
        title = "Ready",
        summary = when (target) {
            ExecutionTarget.Custom -> "No recorded custom run yet."
            ExecutionTarget.LocalSubmission -> "No recorded local submission run yet."
        }
    )
}

private val LEGACY_TEMPLATE_FOLDER_IDS = setOf(
    "folder-interview-prep",
    "folder-graph-study"
)

internal fun synchronizeSeedCatalog(
    loadedFolders: List<ProblemFolderState>,
    seedFolders: List<ProblemFolderState>
): List<ProblemFolderState> {
    if (seedFolders.isEmpty()) return loadedFolders

    val bundledSeedIds = seedFolders.map { it.id }.toSet()
    val mergedFolders = loadedFolders
        .filterNot { folder -> folder.id in bundledSeedIds }
        .toMutableList()

    seedFolders.asReversed().forEach { seedFolder ->
        val existingFolder = loadedFolders.firstOrNull { it.id == seedFolder.id }
        val mergedSeedFolder = if (existingFolder != null) {
            mergeSeedFolder(seedFolder = seedFolder, existingFolder = existingFolder)
        } else {
            copyFolderState(seedFolder)
        }
        mergedFolders.add(0, mergedSeedFolder)
    }

    return mergedFolders
}

private fun copyFolderState(folder: ProblemFolderState): ProblemFolderState {
    return ProblemFolderState(
        id = folder.id,
        title = folder.title,
        sets = mutableStateListOf<ProblemSetState>().apply {
            addAll(
                folder.sets.map { set ->
                    ProblemSetState(
                        id = set.id,
                        title = set.title,
                        problems = mutableStateListOf<ProblemListItem>().apply {
                            addAll(set.problems.map { it.copy() })
                        }
                    )
                }
            )
        }
    )
}

private fun mergeSeedFolder(
    seedFolder: ProblemFolderState,
    existingFolder: ProblemFolderState
): ProblemFolderState {
    val existingSetsById = existingFolder.sets.associateBy { it.id }
    val seedSetsById = seedFolder.sets.associateBy { it.id }
    val seedProblemsById = seedFolder.sets
        .flatMap { it.problems }
        .associateBy { it.id }
    val existingProblemIds = existingFolder.sets
        .flatMap { it.problems }
        .map { it.id }
        .toSet()
    val orderedSetIds = buildList {
        addAll(existingFolder.sets.map { it.id })
        addAll(seedFolder.sets.map { it.id }.filterNot { it in existingSetsById })
    }.distinct()
    val consumedProblemIds = mutableSetOf<String>()

    return ProblemFolderState(
        id = seedFolder.id,
        title = seedFolder.title,
        sets = mutableStateListOf<ProblemSetState>().apply {
            addAll(
                orderedSetIds.map { setId ->
                    val seedSet = seedSetsById[setId]
                    val existingSet = existingSetsById[setId]
                    if (seedSet != null) {
                        mergeSeedSet(
                            seedSet = seedSet,
                            existingSet = existingSet,
                            seedProblemsById = seedProblemsById,
                            existingProblemIds = existingProblemIds,
                            consumedProblemIds = consumedProblemIds
                        )
                    } else {
                        copyExistingSetState(
                            existingSet = existingSet ?: error("Missing existing set for id=$setId"),
                            seedProblemsById = seedProblemsById,
                            consumedProblemIds = consumedProblemIds
                        )
                    }
                }
            )
        }
    )
}

private fun mergeSeedSet(
    seedSet: ProblemSetState,
    existingSet: ProblemSetState?,
    seedProblemsById: Map<String, ProblemListItem>,
    existingProblemIds: Set<String>,
    consumedProblemIds: MutableSet<String>
): ProblemSetState {
    val existingProblemsById = existingSet?.problems?.associateBy { it.id }.orEmpty()
    return ProblemSetState(
        id = seedSet.id,
        title = seedSet.title,
        problems = mutableStateListOf<ProblemListItem>().apply {
            addAll(
                buildList {
                    existingSet?.problems?.forEach { existingProblem ->
                        if (consumedProblemIds.add(existingProblem.id)) {
                            add(
                                mergeProblemState(
                                    seedProblem = seedProblemsById[existingProblem.id],
                                    existingProblem = existingProblem
                                )
                            )
                        }
                    }
                    seedSet.problems.forEach { seedProblem ->
                        if (seedProblem.id !in existingProblemIds && consumedProblemIds.add(seedProblem.id)) {
                            val existingProblem = existingProblemsById[seedProblem.id]
                            add(
                                seedProblem.copy(
                                    active = existingProblem?.active ?: seedProblem.active,
                                    solved = existingProblem?.solved ?: seedProblem.solved,
                                    customTests = existingProblem?.customTests ?: seedProblem.customTests
                                )
                            )
                        }
                    }
                }
            )
        }
    )
}

private fun copyExistingSetState(
    existingSet: ProblemSetState,
    seedProblemsById: Map<String, ProblemListItem>,
    consumedProblemIds: MutableSet<String>
): ProblemSetState {
    return ProblemSetState(
        id = existingSet.id,
        title = existingSet.title,
        problems = mutableStateListOf<ProblemListItem>().apply {
            addAll(
                existingSet.problems.mapNotNull { existingProblem ->
                    if (!consumedProblemIds.add(existingProblem.id)) {
                        null
                    } else {
                        mergeProblemState(
                            seedProblem = seedProblemsById[existingProblem.id],
                            existingProblem = existingProblem
                        )
                    }
                }
            )
        }
    )
}

private fun mergeProblemState(
    seedProblem: ProblemListItem?,
    existingProblem: ProblemListItem
): ProblemListItem {
    return if (seedProblem != null) {
        seedProblem.copy(
            active = existingProblem.active,
            solved = existingProblem.solved,
            customTests = existingProblem.customTests
        )
    } else {
        existingProblem.copy()
    }
}

private fun catalogChanged(
    previous: List<ProblemFolderState>,
    updated: List<ProblemFolderState>
): Boolean {
    if (previous.size != updated.size) return true
    return previous.zip(updated).any { (before, after) ->
        !folderMatches(before, after)
    }
}

private fun folderMatches(
    left: ProblemFolderState,
    right: ProblemFolderState
): Boolean {
    if (left.id != right.id || left.title != right.title || left.sets.size != right.sets.size) {
        return false
    }
    return left.sets.zip(right.sets).all { (leftSet, rightSet) ->
        setMatches(leftSet, rightSet)
    }
}

private fun setMatches(
    left: ProblemSetState,
    right: ProblemSetState
): Boolean {
    if (left.id != right.id || left.title != right.title || left.problems.size != right.problems.size) {
        return false
    }
    return left.problems.zip(right.problems).all { (leftProblem, rightProblem) ->
        leftProblem == rightProblem
    }
}

private fun previewCatalog(): List<ProblemFolderState> {
    return listOf(
        ProblemFolderState(
            id = "folder-neetcode-150",
            title = "NeetCode 150",
            sets = mutableStateListOf(
                ProblemSetState(
                    id = "set-arrays-hashing",
                    title = "Arrays & Hashing",
                    problems = mutableStateListOf(
                        ProblemListItem(
                            id = "problem-contains-duplicate",
                            title = "Contains Duplicate",
                            difficulty = "Easy",
                            active = true,
                            summary = "Return true when any value appears at least twice in the array.",
                            exampleInput = "nums = [1,2,3,1]",
                            exampleOutput = "true",
                            starterCode = """
                                def containsDuplicate(nums):
                                    seen = set()
                                    for value in nums:
                                        if value in seen:
                                            return True
                                        seen.add(value)
                                    return False
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "nums": [1, 2, 3, 1],
                                    "expected": true
                                }

                                case_2 = {
                                    "nums": [1, 2, 3, 4],
                                    "expected": false
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "A hash set gives you O(1) expected lookups.",
                                "Stop as soon as you see a repeated value."
                            )
                        ),
                        ProblemListItem(
                            id = "problem-valid-anagram",
                            title = "Valid Anagram",
                            difficulty = "Easy",
                            summary = "Check whether two strings contain the same characters with the same frequencies.",
                            exampleInput = "s = \"anagram\", t = \"nagaram\"",
                            exampleOutput = "true",
                            starterCode = """
                                def isAnagram(s, t):
                                    if len(s) != len(t):
                                        return False
                                    counts = {}
                                    for char in s:
                                        counts[char] = counts.get(char, 0) + 1
                                    for char in t:
                                        if char not in counts:
                                            return False
                                        counts[char] -= 1
                                        if counts[char] == 0:
                                            del counts[char]
                                    return not counts
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "s": "anagram",
                                    "t": "nagaram",
                                    "expected": true
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "Equal lengths are a quick early guard.",
                                "Count characters from one string and cancel with the other."
                            )
                        ),
                        ProblemListItem(
                            id = "problem-two-sum",
                            title = "Two Sum",
                            difficulty = "Easy",
                            solved = true,
                            summary = "Find the two indices whose values add up to the target.",
                            exampleInput = "nums = [2,7,11,15], target = 9",
                            exampleOutput = "[0,1]",
                            starterCode = """
                                def twoSum(nums, target):
                                    seen = {}
                                    for index, value in enumerate(nums):
                                        needed = target - value
                                        if needed in seen:
                                            return [seen[needed], index]
                                        seen[value] = index
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "nums": [2, 7, 11, 15],
                                    "target": 9,
                                    "expected": [0, 1]
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "Track prior values in a map keyed by number.",
                                "For each value, check whether its complement has already appeared."
                            )
                        )
                    )
                )
            )
        )
    )
}

private fun resolveSelection(
    folders: List<ProblemFolderState>,
    preferredSetId: String = "",
    preferredProblemId: String = ""
): Pair<String, String> {
    val allSets = folders.flatMap { it.sets }
    val preferredSet = allSets.firstOrNull { it.id == preferredSetId }
    val preferredProblem = preferredSet?.problems?.firstOrNull { it.id == preferredProblemId }
    if (preferredSet != null && preferredProblem != null) {
        return preferredSet.id to preferredProblem.id
    }
    val selectedSetId = allSets
        .firstOrNull { set -> set.problems.any { it.active } }
        ?.id
        ?: folders.firstOrNull()?.sets?.firstOrNull()?.id.orEmpty()
    val selectedProblemId = allSets
        .flatMap { it.problems }
        .firstOrNull { it.active }
        ?.id
        ?: folders.firstOrNull()?.sets?.firstOrNull()?.problems?.firstOrNull()?.id.orEmpty()
    return selectedSetId to selectedProblemId
}

private fun updateActiveProblemSelection(
    folders: List<ProblemFolderState>,
    selectedProblemSetId: String,
    selectedProblemId: String
) {
    folders.flatMap { it.sets }.forEach { set ->
        val updated = set.problems.map { item ->
            item.copy(active = set.id == selectedProblemSetId && item.id == selectedProblemId)
        }
        set.problems.clear()
        set.problems.addAll(updated)
    }
}

internal fun buildProblemComposerDestinations(
    folders: List<ProblemFolderState>
): List<ProblemComposerDestination> {
    return folders.flatMap { folder ->
        folder.sets.map { set ->
            ProblemComposerDestination(
                setId = set.id,
                label = "${folder.title} / ${set.title}"
            )
        }
    }
}

private fun List<ProblemFolderState>.deepCopy(): List<ProblemFolderState> {
    return map { folder ->
        ProblemFolderState(
            id = folder.id,
            title = folder.title,
            sets = mutableStateListOf<ProblemSetState>().apply {
                addAll(
                    folder.sets.map { set ->
                        ProblemSetState(
                            id = set.id,
                            title = set.title,
                            problems = mutableStateListOf<ProblemListItem>().apply {
                                addAll(set.problems.map { it.copy() })
                            }
                        )
                    }
                )
            }
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun LandscapeWorkspacePreview() {
    CodeOffTheGridTheme {
        val previewRuntimeController = object : AiRuntimeController {
            override val runtimeState = kotlinx.coroutines.flow.MutableStateFlow(
                AiRuntimeState(
                    phase = AiRuntimePhase.Ready,
                    preset = null,
                    currentModelPath = "/data/user/0/dev.kaixinguo.codeoffthegrid/files/ai-models/preview.gguf",
                    modelName = "preview.gguf",
                    installedModels = listOf(
                        dev.kaixinguo.codeoffthegrid.ai.AiStoredModel(
                            path = "/data/user/0/dev.kaixinguo.codeoffthegrid/files/ai-models/preview.gguf",
                            name = "preview.gguf",
                            preset = null
                        )
                    ),
                    detail = "Preview runtime."
                )
            )

            override suspend fun importModel(uri: android.net.Uri) = Unit

            override suspend fun downloadPresetModel(preset: dev.kaixinguo.codeoffthegrid.ai.AiModelPreset) = Unit

            override suspend fun selectStoredModel(path: String) = Unit

            override suspend fun loadConfiguredModel() = Unit

            override suspend fun unloadModel() = Unit

            override suspend fun removeConfiguredModel() = Unit

            override fun destroy() = Unit
        }

        LandscapeWorkspaceScreen(
            problemCatalogRepository = ProblemCatalogRepository(
                problemCatalogDao = object : dev.kaixinguo.codeoffthegrid.data.local.ProblemCatalogDao {
                    override suspend fun getFolders() = emptyList<dev.kaixinguo.codeoffthegrid.data.local.ProblemFolderEntity>()
                    override suspend fun getSets() = emptyList<dev.kaixinguo.codeoffthegrid.data.local.ProblemSetEntity>()
                    override suspend fun getProblems() = emptyList<dev.kaixinguo.codeoffthegrid.data.local.StoredProblemEntity>()
                    override suspend fun insertFolders(folders: List<dev.kaixinguo.codeoffthegrid.data.local.ProblemFolderEntity>) = Unit
                    override suspend fun insertSets(sets: List<dev.kaixinguo.codeoffthegrid.data.local.ProblemSetEntity>) = Unit
                    override suspend fun insertProblems(problems: List<dev.kaixinguo.codeoffthegrid.data.local.StoredProblemEntity>) = Unit
                    override suspend fun clearProblems() = Unit
                    override suspend fun clearSets() = Unit
                    override suspend fun clearFolders() = Unit
                }
            ),
            workspaceDocumentRepository = WorkspaceDocumentRepository(
                workspaceDocumentDao = object : dev.kaixinguo.codeoffthegrid.data.local.WorkspaceDocumentDao {
                    override suspend fun getByProblemId(problemId: String) = null
                    override suspend fun upsert(document: dev.kaixinguo.codeoffthegrid.data.local.WorkspaceDocumentEntity) = Unit
                }
            ),
            localPythonExecutionService = LocalPythonExecutionService(androidx.compose.ui.platform.LocalContext.current),
            sketchCodeRecognitionService = object : SketchCodeRecognitionService {
                override suspend fun isModelDownloaded(): Boolean = true

                override suspend fun downloadModel() = Unit

                override suspend fun recognizeCode(strokes: List<SketchStroke>): String {
                    return "def preview_solution():\n    return 42"
                }
            },
            askAiController = object : AskAiSessionController {
                override val sessionState = AskAiSessionState()
                override val aiRuntimeController: AiRuntimeController = previewRuntimeController
                override val activeRequestElapsedMillis: Long = 0L
                override val pendingSideEffect: AskAiPendingSideEffect? = null

                override fun syncContext(composerActive: Boolean) = Unit

                override fun submitRequest(request: AskAiRequest) = Unit

                override fun cancelActiveRequest() = Unit

                override fun clearChat() = Unit

                override fun completePendingSideEffect(sender: AiChatSender, message: String) = Unit
            },
            seedCatalogProvider = { previewCatalog() },
            themeMode = AppThemeMode.Night,
            onThemeModeChange = {}
        )
    }
}
