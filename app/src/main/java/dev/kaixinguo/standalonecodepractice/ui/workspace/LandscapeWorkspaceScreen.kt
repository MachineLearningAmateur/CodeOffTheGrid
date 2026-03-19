package dev.kaixinguo.standalonecodepractice.ui.workspace

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.kaixinguo.standalonecodepractice.data.GitHubMarkdownImportService
import dev.kaixinguo.standalonecodepractice.data.LocalPythonExecutionService
import dev.kaixinguo.standalonecodepractice.data.ProblemCatalogRepository
import dev.kaixinguo.standalonecodepractice.data.WorkspaceDocumentRepository
import dev.kaixinguo.standalonecodepractice.ui.theme.AppBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.StandaloneCodePracticeTheme
import kotlinx.coroutines.launch

@Composable
internal fun LandscapeWorkspaceScreen(
    problemCatalogRepository: ProblemCatalogRepository,
    workspaceDocumentRepository: WorkspaceDocumentRepository,
    gitHubMarkdownImportService: GitHubMarkdownImportService,
    localPythonExecutionService: LocalPythonExecutionService,
    seedCatalogProvider: suspend () -> List<ProblemFolderState>,
    modifier: Modifier = Modifier
) {
    val folders = remember { mutableStateListOf<ProblemFolderState>() }
    var selectedProblemId by remember { mutableStateOf("") }
    var selectedProblemSetId by remember { mutableStateOf("") }
    var sidebarMode by remember { mutableStateOf(SidebarMode.Problems) }
    var sidebarCollapsed by remember { mutableStateOf(false) }
    var workspaceInputMode by remember { mutableStateOf(WorkspaceInputMode.Keyboard) }
    val sketchStrokes = remember { mutableStateListOf<SketchStroke>() }
    var currentDraftCode by remember { mutableStateOf("") }
    var currentCustomTestSuite by remember { mutableStateOf(ProblemTestSuite()) }
    var importInProgress by remember { mutableStateOf(false) }
    var importFeedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarCollapsed) 84.dp else 288.dp,
        animationSpec = spring(),
        label = "sidebarWidth"
    )

    val allSets = folders.flatMap { it.sets }
    val selectedProblemSet = allSets
        .firstOrNull { it.id == selectedProblemSetId }
        ?: allSets.firstOrNull()
    val selectedProblem = selectedProblemSet?.problems
        ?.firstOrNull { it.id == selectedProblemId }
        ?: selectedProblemSet?.problems?.firstOrNull()
        ?: allSets.flatMap { it.problems }.firstOrNull()

    LaunchedEffect(Unit) {
        val seedFolders = seedCatalogProvider()
        val loadedFolders = problemCatalogRepository.loadCatalog()
        val resolvedFolders = when {
            loadedFolders.isEmpty() -> {
                problemCatalogRepository.persistCatalog(seedFolders)
                problemCatalogRepository.loadCatalog()
            }
            loadedFolders.all { it.id in LEGACY_TEMPLATE_FOLDER_IDS } -> {
                problemCatalogRepository.persistCatalog(seedFolders)
                problemCatalogRepository.loadCatalog()
            }
            else -> {
                val synchronizedFolders = synchronizeSeedCatalog(
                    loadedFolders = loadedFolders,
                    seedFolders = seedFolders
                )
                if (catalogChanged(loadedFolders, synchronizedFolders)) {
                    problemCatalogRepository.persistCatalog(synchronizedFolders)
                    problemCatalogRepository.loadCatalog()
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
            sketchStrokes.clear()

            val document = workspaceDocumentRepository.loadDocument(selectedProblem)
            currentDraftCode = document.draftCode
            currentCustomTestSuite = document.customTestSuite
            sketchStrokes.clear()
            sketchStrokes.addAll(document.sketches)
        } else {
            currentDraftCode = ""
            currentCustomTestSuite = ProblemTestSuite()
            sketchStrokes.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A202D), AppBackground)
                )
            )
            .padding(20.dp)
    ) {
        fun persistCatalogSnapshot() {
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
                onImportGitHubRepo = { repoUrl ->
                    importInProgress = true
                    importFeedback = null
                    scope.launch {
                        try {
                            val importedFolder = gitHubMarkdownImportService.importRepo(repoUrl)
                            val existingIndex = folders.indexOfFirst { it.id == importedFolder.id }
                            if (existingIndex >= 0) {
                                folders[existingIndex] = importedFolder
                            } else {
                                folders.add(importedFolder)
                            }
                            val firstSet = importedFolder.sets.firstOrNull()
                            val firstProblem = firstSet?.problems?.firstOrNull()
                            selectedProblemSetId = firstSet?.id.orEmpty()
                            selectedProblemId = firstProblem?.id.orEmpty()
                            updateActiveProblemSelection(folders, selectedProblemSetId, selectedProblemId)
                            persistCatalogSnapshot()
                            val importedCount = importedFolder.sets.sumOf { it.problems.size }
                            importFeedback = "Imported $importedCount problems from ${importedFolder.title}."
                        } catch (error: Exception) {
                            importFeedback = error.message ?: "Import failed."
                        } finally {
                            importInProgress = false
                        }
                    }
                },
                importInProgress = importInProgress,
                importFeedback = importFeedback,
                selectedMode = sidebarMode,
                onModeSelected = { sidebarMode = it },
                collapsed = sidebarCollapsed,
                onToggleCollapsed = { sidebarCollapsed = !sidebarCollapsed },
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
            )
            if (selectedProblem != null) {
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
                ProblemPane(
                    problem = selectedProblem,
                    draftCode = currentDraftCode,
                    customTestSuite = currentCustomTestSuite,
                    onCustomTestSuiteChange = { updatedCustomTestSuite ->
                        currentCustomTestSuite = updatedCustomTestSuite
                        persistWorkspaceDocument(
                            problemId = selectedProblem.id,
                            draftCode = currentDraftCode,
                            customTestSuite = updatedCustomTestSuite,
                            sketches = sketchStrokes.toList()
                        )
                    },
                    localPythonExecutionService = localPythonExecutionService,
                    modifier = Modifier
                        .width(356.dp)
                        .fillMaxHeight()
                )
            }
        }
    }
}

private val LEGACY_TEMPLATE_FOLDER_IDS = setOf(
    "folder-interview-prep",
    "folder-graph-study"
)

private fun synchronizeSeedCatalog(
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
    return ProblemFolderState(
        id = seedFolder.id,
        title = seedFolder.title,
        sets = mutableStateListOf<ProblemSetState>().apply {
            addAll(
                seedFolder.sets.map { seedSet ->
                    val existingSet = existingSetsById[seedSet.id]
                    mergeSeedSet(seedSet = seedSet, existingSet = existingSet)
                }
            )
        }
    )
}

private fun mergeSeedSet(
    seedSet: ProblemSetState,
    existingSet: ProblemSetState?
): ProblemSetState {
    val existingProblemsById = existingSet?.problems?.associateBy { it.id }.orEmpty()
    return ProblemSetState(
        id = seedSet.id,
        title = seedSet.title,
        problems = mutableStateListOf<ProblemListItem>().apply {
            addAll(
                seedSet.problems.map { seedProblem ->
                    val existingProblem = existingProblemsById[seedProblem.id]
                    seedProblem.copy(
                        active = existingProblem?.active ?: seedProblem.active,
                        solved = existingProblem?.solved ?: seedProblem.solved,
                        customTests = existingProblem?.customTests ?: seedProblem.customTests
                    )
                }
            )
        }
    )
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
            gitHubMarkdownImportService = GitHubMarkdownImportService(),
            localPythonExecutionService = LocalPythonExecutionService(androidx.compose.ui.platform.LocalContext.current),
            seedCatalogProvider = { previewCatalog() }
        )
    }
}
