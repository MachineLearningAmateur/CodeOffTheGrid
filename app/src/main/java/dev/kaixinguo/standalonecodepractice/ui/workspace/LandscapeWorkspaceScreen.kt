package dev.kaixinguo.standalonecodepractice.ui.workspace

import android.database.sqlite.SQLiteBlobTooBigException
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.kaixinguo.standalonecodepractice.ai.AiAssistant
import dev.kaixinguo.standalonecodepractice.ai.AiRuntimeController
import dev.kaixinguo.standalonecodepractice.ai.AiRuntimePhase
import dev.kaixinguo.standalonecodepractice.ai.AiRuntimeState
import dev.kaixinguo.standalonecodepractice.data.LocalPythonExecutionService
import dev.kaixinguo.standalonecodepractice.data.ProblemCatalogRepository
import dev.kaixinguo.standalonecodepractice.data.WorkspaceDocumentRepository
import dev.kaixinguo.standalonecodepractice.ui.theme.AppBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.AppThemeMode
import dev.kaixinguo.standalonecodepractice.ui.theme.SidebarBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.StandaloneCodePracticeTheme
import dev.kaixinguo.standalonecodepractice.ui.theme.TextPrimary
import dev.kaixinguo.standalonecodepractice.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
internal fun LandscapeWorkspaceScreen(
    problemCatalogRepository: ProblemCatalogRepository,
    workspaceDocumentRepository: WorkspaceDocumentRepository,
    localPythonExecutionService: LocalPythonExecutionService,
    aiAssistant: AiAssistant,
    aiRuntimeController: AiRuntimeController,
    seedCatalogProvider: suspend () -> List<ProblemFolderState>,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val folders = remember { mutableStateListOf<ProblemFolderState>() }
    var protectedFolderIds by remember { mutableStateOf(emptySet<String>()) }
    var protectedSetIds by remember { mutableStateOf(emptySet<String>()) }
    var protectedProblemIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedProblemId by remember { mutableStateOf("") }
    var selectedProblemSetId by remember { mutableStateOf("") }
    var sidebarMode by remember { mutableStateOf(SidebarMode.Problems) }
    var sidebarCollapsed by remember { mutableStateOf(false) }
    var askAiFullscreen by remember { mutableStateOf(false) }
    var workspaceInputMode by remember { mutableStateOf(WorkspaceInputMode.Keyboard) }
    val sketchStrokes = remember { mutableStateListOf<SketchStroke>() }
    var currentDraftCode by remember { mutableStateOf("") }
    var currentCustomTestSuite by remember { mutableStateOf(ProblemTestSuite()) }
    var currentCustomExecutionResult by remember { mutableStateOf(defaultExecutionResult(ExecutionTarget.Custom)) }
    var currentSubmissionExecutionResult by remember { mutableStateOf(defaultExecutionResult(ExecutionTarget.LocalSubmission)) }
    var selectedSupportTab by remember { mutableStateOf(SupportTab.Problem) }
    var composerSession by remember { mutableStateOf<ProblemComposerSession?>(null) }
    var showDiscardComposerDialog by remember { mutableStateOf(false) }
    var catalogVersion by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarCollapsed) 84.dp else 288.dp,
        animationSpec = spring(),
        label = "sidebarWidth"
    )

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
        val (initialSetId, initialProblemId) = resolveSelection(resolvedFolders)
        selectedProblemSetId = initialSetId
        selectedProblemId = initialProblemId
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
            currentCustomTestSuite = document.customTestSuite
            sketchStrokes.clear()
            sketchStrokes.addAll(document.sketches)
        } else {
            currentDraftCode = ""
            currentCustomTestSuite = ProblemTestSuite()
            currentCustomExecutionResult = defaultExecutionResult(ExecutionTarget.Custom)
            currentSubmissionExecutionResult = defaultExecutionResult(ExecutionTarget.LocalSubmission)
            sketchStrokes.clear()
        }
    }

    LaunchedEffect(effectiveSidebarMode) {
        if (effectiveSidebarMode != SidebarMode.AskAi) {
            askAiFullscreen = false
        }
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
            sketches: List<SketchStroke>
        ) {
            scope.launch {
                workspaceDocumentRepository.saveDocument(
                    problemId = problemId,
                    draftCode = draftCode,
                    customTestSuite = customTestSuite,
                    sketches = sketches
                )
            }
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
                onComposerSessionChange = { composerSession = it },
                currentDraftCode = currentDraftCode,
                currentCustomTestSuite = currentCustomTestSuite,
                customExecutionResult = currentCustomExecutionResult,
                submissionExecutionResult = currentSubmissionExecutionResult,
                aiAssistant = aiAssistant,
                onGeneratedCustomTests = { generatedSuite ->
                    val activeProblem = selectedProblem ?: return@SidebarPane
                    val mergedSuite = mergeCustomTestSuites(
                        existingSuite = currentCustomTestSuite,
                        generatedSuite = generatedSuite
                    )
                    currentCustomTestSuite = mergedSuite
                    selectedSupportTab = SupportTab.Custom
                    persistWorkspaceDocument(
                        problemId = activeProblem.id,
                        draftCode = currentDraftCode,
                        customTestSuite = mergedSuite,
                        sketches = sketchStrokes.toList()
                    )
                },
                aiRuntimeController = aiRuntimeController,
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
                                sketches = sketchStrokes.toList()
                            )
                        },
                        onSketchesChange = { updatedSketches ->
                            sketchStrokes.clear()
                            sketchStrokes.addAll(updatedSketches)
                            persistWorkspaceDocument(
                                problemId = selectedProblem.id,
                                draftCode = currentDraftCode,
                                customTestSuite = currentCustomTestSuite,
                                sketches = updatedSketches
                            )
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
                                sketches = sketchStrokes.toList()
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
                            .width(356.dp)
                            .fillMaxHeight()
                    )
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

private fun resolveSelection(folders: List<ProblemFolderState>): Pair<String, String> {
    val allSets = folders.flatMap { it.sets }
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
    StandaloneCodePracticeTheme {
        LandscapeWorkspaceScreen(
            problemCatalogRepository = ProblemCatalogRepository(
                problemCatalogDao = object : dev.kaixinguo.standalonecodepractice.data.local.ProblemCatalogDao {
                    override suspend fun getFolders() = emptyList<dev.kaixinguo.standalonecodepractice.data.local.ProblemFolderEntity>()
                    override suspend fun getSets() = emptyList<dev.kaixinguo.standalonecodepractice.data.local.ProblemSetEntity>()
                    override suspend fun getProblems() = emptyList<dev.kaixinguo.standalonecodepractice.data.local.StoredProblemEntity>()
                    override suspend fun insertFolders(folders: List<dev.kaixinguo.standalonecodepractice.data.local.ProblemFolderEntity>) = Unit
                    override suspend fun insertSets(sets: List<dev.kaixinguo.standalonecodepractice.data.local.ProblemSetEntity>) = Unit
                    override suspend fun insertProblems(problems: List<dev.kaixinguo.standalonecodepractice.data.local.StoredProblemEntity>) = Unit
                    override suspend fun clearProblems() = Unit
                    override suspend fun clearSets() = Unit
                    override suspend fun clearFolders() = Unit
                }
            ),
            workspaceDocumentRepository = WorkspaceDocumentRepository(
                workspaceDocumentDao = object : dev.kaixinguo.standalonecodepractice.data.local.WorkspaceDocumentDao {
                    override suspend fun getByProblemId(problemId: String) = null
                    override suspend fun upsert(document: dev.kaixinguo.standalonecodepractice.data.local.WorkspaceDocumentEntity) = Unit
                }
            ),
            localPythonExecutionService = LocalPythonExecutionService(androidx.compose.ui.platform.LocalContext.current),
            aiAssistant = object : AiAssistant {
                override suspend fun createProblem(problem: String, code: String?, request: String?) =
                    dev.kaixinguo.standalonecodepractice.data.SharedProblemFile(
                        title = "Preview Problem",
                        difficulty = "Easy",
                        summary = "Generated by preview.gguf as an array problem.",
                        statementMarkdown = "Write a preview problem statement.",
                        exampleInput = "nums = [1, 2, 3]",
                        exampleOutput = "6",
                        starterCode = "class Solution:\n    def solve(self, nums):\n        pass",
                        hints = listOf("Look for the simplest aggregation."),
                        submissionTestSuite = null,
                        executionPipeline = null
                    )
                override suspend fun generateHint(problem: String, code: String?, request: String?) = "Hint preview"
                override suspend fun explainSolution(problem: String, code: String?, request: String?) = "Explain preview"
                override suspend fun reviewCode(problem: String, code: String?, request: String?) = "Review preview"
                override suspend fun generateCustomTests(problem: String, code: String?, request: String?) = ProblemTestSuite()
                override suspend fun solveProblem(problem: String, code: String?, request: String?) = "Solve preview"
            },
            aiRuntimeController = object : AiRuntimeController {
                override val runtimeState = kotlinx.coroutines.flow.MutableStateFlow(
                    AiRuntimeState(
                        phase = AiRuntimePhase.Ready,
                        preset = null,
                        currentModelPath = "/data/user/0/dev.kaixinguo.standalonecodepractice/files/ai-models/preview.gguf",
                        modelName = "preview.gguf",
                        installedModels = listOf(
                            dev.kaixinguo.standalonecodepractice.ai.AiStoredModel(
                                path = "/data/user/0/dev.kaixinguo.standalonecodepractice/files/ai-models/preview.gguf",
                                name = "preview.gguf",
                                preset = null
                            )
                        ),
                        detail = "Preview runtime."
                    )
                )

                override suspend fun importModel(uri: android.net.Uri) = Unit

                override suspend fun downloadPresetModel(preset: dev.kaixinguo.standalonecodepractice.ai.AiModelPreset) = Unit

                override suspend fun selectStoredModel(path: String) = Unit

                override suspend fun loadConfiguredModel() = Unit

                override suspend fun unloadModel() = Unit

                override suspend fun removeConfiguredModel() = Unit

                override fun destroy() = Unit
            },
            seedCatalogProvider = { previewCatalog() },
            themeMode = AppThemeMode.Night,
            onThemeModeChange = {}
        )
    }
}
