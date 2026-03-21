package dev.kaixinguo.standalonecodepractice.ui.workspace

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import dev.kaixinguo.standalonecodepractice.R
import dev.kaixinguo.standalonecodepractice.ai.AiAssistant
import dev.kaixinguo.standalonecodepractice.ai.AiModelPreset
import dev.kaixinguo.standalonecodepractice.ai.AiRuntimeController
import dev.kaixinguo.standalonecodepractice.ai.AiRuntimePhase
import dev.kaixinguo.standalonecodepractice.ai.AiStoredModel
import dev.kaixinguo.standalonecodepractice.ai.ProblemPromptFormatter
import dev.kaixinguo.standalonecodepractice.ai.PromptMode
import dev.kaixinguo.standalonecodepractice.data.ProblemInputNormalizer
import dev.kaixinguo.standalonecodepractice.data.ProblemSubmissionSuiteFactory
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentAmber
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentBlue
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentGreen
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentRed
import dev.kaixinguo.standalonecodepractice.ui.theme.AppThemeMode
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBackgroundAlt
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBorder
import dev.kaixinguo.standalonecodepractice.ui.theme.DividerColor
import dev.kaixinguo.standalonecodepractice.ui.theme.SidebarBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.TextMuted
import dev.kaixinguo.standalonecodepractice.ui.theme.TextPrimary
import dev.kaixinguo.standalonecodepractice.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
internal fun SidebarPane(
    folders: List<ProblemFolderState>,
    selectedProblemSetId: String,
    selectedProblemId: String,
    onProblemSetSelected: (String) -> Unit,
    onProblemSelected: (String, String) -> Unit,
    onDeleteProblem: (String, ProblemListItem) -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateSet: (String, String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onMoveProblem: (String, String, ProblemListItem, Int) -> Unit,
    onOpenProblemComposer: () -> Unit,
    problemComposerActive: Boolean,
    selectedMode: SidebarMode,
    onModeSelected: (SidebarMode) -> Unit,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    protectedFolderIds: Set<String>,
    protectedSetIds: Set<String>,
    protectedProblemIds: Set<String>,
    askAiFullscreen: Boolean,
    onToggleAskAiFullscreen: () -> Unit,
    selectedProblem: ProblemListItem?,
    composerSession: ProblemComposerSession?,
    onComposerSessionChange: (ProblemComposerSession) -> Unit,
    currentDraftCode: String,
    currentCustomTestSuite: ProblemTestSuite,
    customExecutionResult: ProblemExecutionResult,
    submissionExecutionResult: ProblemExecutionResult,
    aiAssistant: AiAssistant,
    onGeneratedCustomTests: (ProblemTestSuite) -> Unit,
    aiRuntimeController: AiRuntimeController,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SidebarBackground,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        val showAskAiFullscreen = selectedMode == SidebarMode.AskAi && askAiFullscreen
        val showNavigationChrome = !showAskAiFullscreen

        if (collapsed && !showAskAiFullscreen) {
            CollapsedSidebarPane(
                selectedMode = selectedMode,
                modeSelectionLocked = problemComposerActive,
                onModeSelected = onModeSelected,
                onToggleCollapsed = onToggleCollapsed,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showNavigationChrome) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .weight(1f)
                        )
                        ArrowActionChip(
                            direction = ArrowDirection.Collapse,
                            onClick = onToggleCollapsed
                        )
                    }
                }

                if (showNavigationChrome) {
                    SidebarModeSelector(
                        selectedMode = selectedMode,
                        modeSelectionLocked = problemComposerActive,
                        onModeSelected = onModeSelected
                    )
                }

                when (selectedMode) {
                    SidebarMode.Problems -> ProblemsSidebarContent(
                        folders = folders,
                        selectedProblemSetId = selectedProblemSetId,
                        selectedProblemId = selectedProblemId,
                        onProblemSetSelected = onProblemSetSelected,
                        onProblemSelected = onProblemSelected,
                        onDeleteProblem = onDeleteProblem,
                        onCreateFolder = onCreateFolder,
                        onCreateSet = onCreateSet,
                        onDeleteSet = onDeleteSet,
                        onDeleteFolder = onDeleteFolder,
                        protectedFolderIds = protectedFolderIds,
                        protectedSetIds = protectedSetIds,
                        protectedProblemIds = protectedProblemIds,
                        onMoveProblem = onMoveProblem,
                        onOpenProblemComposer = onOpenProblemComposer,
                        problemComposerActive = problemComposerActive
                    )
                    SidebarMode.Stats -> StatsSidebarContent(
                        folders = folders
                    )
                    SidebarMode.AskAi -> AskAiSidebarContent(
                        fullscreen = showAskAiFullscreen,
                        onToggleFullscreen = onToggleAskAiFullscreen,
                        selectedProblem = selectedProblem,
                        composerSession = composerSession,
                        onComposerSessionChange = onComposerSessionChange,
                        currentDraftCode = currentDraftCode,
                        currentCustomTestSuite = currentCustomTestSuite,
                        customExecutionResult = customExecutionResult,
                        submissionExecutionResult = submissionExecutionResult,
                        aiAssistant = aiAssistant,
                        onGeneratedCustomTests = onGeneratedCustomTests,
                        aiRuntimeController = aiRuntimeController
                    )
                    SidebarMode.Settings -> SettingsSidebarContent(
                        aiRuntimeController = aiRuntimeController,
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.StatsSidebarContent(
    folders: List<ProblemFolderState>
) {
    val overallSolved = folders.sumOf { folder ->
        folder.sets.sumOf { set -> set.problems.count { it.solved } }
    }
    val overallTotal = folders.sumOf { folder ->
        folder.sets.sumOf { set -> set.problems.size }
    }

    CardBlock(title = "Overview", modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (overallTotal == 0) {
                "No problems loaded yet."
            } else {
                "$overallSolved / $overallTotal solved"
            },
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (overallTotal == 0) {
                "Import or load a catalog to start tracking progress."
            } else {
                "${((overallSolved.toFloat() / overallTotal.toFloat()) * 100f).toInt()}% complete across all folders."
            },
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (folders.isEmpty()) {
            CardBlock(title = "Folders", modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Folder progress will show up here once the catalog is loaded.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            folders.forEach { folder ->
                val folderSolved = folder.sets.sumOf { set -> set.problems.count { it.solved } }
                val folderTotal = folder.sets.sumOf { set -> set.problems.size }
                CardBlock(title = folder.title, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "$folderSolved / $folderTotal solved",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (folderTotal == 0) {
                            "No problems in this folder yet."
                        } else {
                            "${((folderSolved.toFloat() / folderTotal.toFloat()) * 100f).toInt()}% complete"
                        },
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (folder.sets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        folder.sets.forEachIndexed { index, set ->
                            val setSolved = set.problems.count { it.solved }
                            val setTotal = set.problems.size
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = set.title,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$setSolved / $setTotal",
                                    color = if (setSolved == setTotal && setTotal > 0) AccentGreen else TextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (index < folder.sets.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedSidebarPane(
    selectedMode: SidebarMode,
    modeSelectionLocked: Boolean,
    onModeSelected: (SidebarMode) -> Unit,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ArrowActionChip(
            direction = ArrowDirection.Expand,
            onClick = onToggleCollapsed
        )

        Surface(
            color = CardBackground,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SidebarMode.entries.forEach { mode ->
                    RailModeButton(
                        label = mode.shortLabel(),
                        selected = selectedMode == mode,
                        enabled = !modeSelectionLocked,
                        onClick = {
                            onModeSelected(mode)
                            onToggleCollapsed()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SidebarModeSelector(
    selectedMode: SidebarMode,
    modeSelectionLocked: Boolean,
    onModeSelected: (SidebarMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!modeSelectionLocked) {
                            Modifier.clickable { expanded = true }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedMode.label,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                SmallActionChip(
                    label = if (modeSelectionLocked) "Locked" else "Swap",
                    enabled = !modeSelectionLocked
                )
            }

            DropdownMenu(
                expanded = expanded && !modeSelectionLocked,
                onDismissRequest = { expanded = false }
            ) {
                SidebarMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(text = mode.label) },
                        onClick = {
                            onModeSelected(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProblemsSidebarContent(
    folders: List<ProblemFolderState>,
    selectedProblemSetId: String,
    selectedProblemId: String,
    onProblemSetSelected: (String) -> Unit,
    onProblemSelected: (String, String) -> Unit,
    onDeleteProblem: (String, ProblemListItem) -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateSet: (String, String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    protectedFolderIds: Set<String>,
    protectedSetIds: Set<String>,
    protectedProblemIds: Set<String>,
    onMoveProblem: (String, String, ProblemListItem, Int) -> Unit,
    onOpenProblemComposer: () -> Unit,
    problemComposerActive: Boolean
) {
    var query by remember { mutableStateOf("") }
    var problemFilter by remember { mutableStateOf(ProblemFilter.All) }
    var creatingFolder by remember { mutableStateOf(false) }
    var newFolderTitle by remember { mutableStateOf("") }
    var draggedProblem by remember { mutableStateOf<DraggedProblemState?>(null) }
    var hoveredSetId by remember { mutableStateOf<String?>(null) }
    var hoveredInsertionTarget by remember { mutableStateOf<InsertionTarget?>(null) }
    var hoveredTrash by remember { mutableStateOf(false) }
    val setBounds = remember { mutableStateMapOf<String, Rect>() }
    val problemBounds = remember { mutableStateMapOf<String, Rect>() }
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    val density = LocalDensity.current

    CardBlock(title = null, modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            cursorBrush = SolidColor(AccentBlue),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text(
                        text = "Search problems...",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                innerTextField()
            }
        )
        Spacer(modifier = Modifier.padding(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProblemFilter.entries.forEach { filter ->
                ProblemFilterChip(
                    label = filter.label,
                    selected = problemFilter == filter,
                    onClick = { problemFilter = filter },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Text(
        text = "Problem Sets",
        color = TextMuted,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(horizontal = 4.dp)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items = folders, key = { it.id }) { folder ->
                    FolderTreeBlock(
                        folder = folder,
                        selectedProblemSetId = selectedProblemSetId,
                        selectedProblemId = selectedProblemId,
                        draggedProblemKey = draggedProblem?.let { "${it.sourceSetId}::${it.problem.id}" },
                        hoveredInsertionTarget = hoveredInsertionTarget,
                        query = query,
                        problemFilter = problemFilter,
                        hoveredSetId = hoveredSetId,
                        onProblemSetSelected = onProblemSetSelected,
                        onProblemSelected = onProblemSelected,
                        onCreateSet = onCreateSet,
                        onDeleteSet = onDeleteSet,
                        onDeleteFolder = onDeleteFolder,
                        protectedFolderIds = protectedFolderIds,
                        protectedSetIds = protectedSetIds,
                        onSetBoundsChange = { setId, bounds ->
                            if (bounds != null) {
                                setBounds[setId] = bounds
                            } else {
                                setBounds.remove(setId)
                            }
                        },
                        onProblemBoundsChange = { key, bounds ->
                            if (bounds != null) {
                                problemBounds[key] = bounds
                            } else {
                                problemBounds.remove(key)
                            }
                        },
                        onDragStart = { sourceSetId, problem, pointer, grabOffset, previewSizePx ->
                            draggedProblem = DraggedProblemState(
                                sourceSetId = sourceSetId,
                                problem = problem,
                                pointer = pointer,
                                grabOffset = grabOffset,
                                previewSizePx = previewSizePx
                            )
                            hoveredSetId = null
                            hoveredInsertionTarget = null
                            hoveredTrash = false
                        },
                        onDragMove = { dragDelta ->
                            val updatedDrag = draggedProblem?.let { drag ->
                                drag.copy(pointer = drag.pointer + dragDelta)
                            }
                            draggedProblem = updatedDrag
                            val canTrashDraggedProblem = updatedDrag?.problem?.id !in protectedProblemIds
                            val dragRect = updatedDrag?.previewRect()
                            val isOverTrash = canTrashDraggedProblem &&
                                dragRect != null &&
                                trashBounds?.overlapsWith(dragRect) == true
                            hoveredTrash = isOverTrash
                            if (isOverTrash) {
                                hoveredSetId = null
                                hoveredInsertionTarget = null
                            } else {
                                val currentPointer = updatedDrag?.pointer ?: return@FolderTreeBlock
                                val hoveredProblemEntry = problemBounds.entries.firstOrNull { (_, bounds) ->
                                    bounds.contains(currentPointer)
                                }
                                if (hoveredProblemEntry != null) {
                                    hoveredSetId = hoveredProblemEntry.key.substringBefore("::")
                                    hoveredInsertionTarget = resolveProblemInsertionTarget(
                                        entryKey = hoveredProblemEntry.key,
                                        pointer = currentPointer,
                                        entryBounds = hoveredProblemEntry.value,
                                        folders = folders
                                    )
                                } else {
                                    val targetSetId = setBounds.entries.firstOrNull { (_, bounds) ->
                                        bounds.contains(currentPointer)
                                    }?.key
                                    hoveredSetId = targetSetId
                                    hoveredInsertionTarget = targetSetId?.let { setId ->
                                        resolveSetInsertionTarget(
                                            setId = setId,
                                            folders = folders,
                                            problemBounds = problemBounds
                                        )
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            val drag = draggedProblem
                            if (drag != null) {
                                val canTrashDraggedProblem = drag.problem.id !in protectedProblemIds
                                val finalPointer = drag.pointer
                                val droppingOnTrash = canTrashDraggedProblem &&
                                    trashBounds?.overlapsWith(drag.previewRect()) == true
                                val finalTarget = hoveredInsertionTarget ?: run {
                                    val problemEntry = problemBounds.entries.firstOrNull { (_, bounds) ->
                                        bounds.contains(finalPointer)
                                    }
                                    if (problemEntry != null) {
                                        resolveProblemInsertionTarget(
                                            entryKey = problemEntry.key,
                                            pointer = finalPointer,
                                            entryBounds = problemEntry.value,
                                            folders = folders
                                        )
                                    } else {
                                        setBounds.entries.firstOrNull { (_, bounds) ->
                                            bounds.contains(finalPointer)
                                        }?.key?.let { setId ->
                                            resolveSetInsertionTarget(
                                                setId = setId,
                                                folders = folders,
                                                problemBounds = problemBounds
                                            )
                                        }
                                    }
                                }
                                when {
                                    hoveredTrash || droppingOnTrash -> onDeleteProblem(drag.sourceSetId, drag.problem)
                                    finalTarget != null -> {
                                        onMoveProblem(
                                            drag.sourceSetId,
                                            finalTarget.setId,
                                            drag.problem,
                                            finalTarget.index
                                        )
                                        onProblemSetSelected(finalTarget.setId)
                                        onProblemSelected(finalTarget.setId, drag.problem.id)
                                    }
                                }
                            }
                            draggedProblem = null
                            hoveredSetId = null
                            hoveredInsertionTarget = null
                            hoveredTrash = false
                        }
                    )
                }
            }
        }
        if (draggedProblem != null && draggedProblem!!.problem.id !in protectedProblemIds) {
            TrashDropTarget(
                active = hoveredTrash,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        trashBounds = coordinates.boundsInRoot()
                    }
            )
        } else {
            CardBlock(
                title = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                if (creatingFolder) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = newFolderTitle,
                            onValueChange = { newFolderTitle = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            cursorBrush = SolidColor(AccentBlue),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (newFolderTitle.isBlank()) {
                                    Text(
                                        text = "Folder name...",
                                        color = TextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                innerTextField()
                            }
                        )
                        PlainActionChip("Add") {
                            val title = newFolderTitle.trim()
                            if (title.isNotEmpty()) {
                                onCreateFolder(title)
                                newFolderTitle = ""
                                creatingFolder = false
                            }
                        }
                        PlainActionChip("Cancel") {
                            newFolderTitle = ""
                            creatingFolder = false
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlainActionChip(
                            label = "New Folder",
                            iconRes = R.drawable.ic_plus,
                            modifier = Modifier.weight(1f)
                        ) {
                            creatingFolder = true
                        }
                        PlainActionChip(
                            label = if (problemComposerActive) "Editing" else "Add / Import",
                            iconRes = R.drawable.ic_plus,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (!problemComposerActive) {
                                onOpenProblemComposer()
                            }
                        }
                    }
                }
            }
        }
    }

    if (draggedProblem != null) {
        Popup(
            offset = IntOffset(
                (draggedProblem!!.pointer.x - draggedProblem!!.grabOffset.x).toInt(),
                (draggedProblem!!.pointer.y - draggedProblem!!.grabOffset.y).toInt()
            )
        ) {
            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.35f)),
                modifier = Modifier.width(with(density) { draggedProblem!!.previewSizePx.width.toDp() })
            ) {
                ProblemRowContent(
                    problem = draggedProblem!!.problem,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun FolderTreeBlock(
    folder: ProblemFolderState,
    selectedProblemSetId: String,
    selectedProblemId: String,
    draggedProblemKey: String?,
    hoveredInsertionTarget: InsertionTarget?,
    query: String,
    problemFilter: ProblemFilter,
    hoveredSetId: String?,
    onProblemSetSelected: (String) -> Unit,
    onProblemSelected: (String, String) -> Unit,
    onCreateSet: (String, String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    protectedFolderIds: Set<String>,
    protectedSetIds: Set<String>,
    onSetBoundsChange: (String, Rect?) -> Unit,
    onProblemBoundsChange: (String, Rect?) -> Unit,
    onDragStart: (String, ProblemListItem, Offset, Offset, IntSize) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var expanded by remember(folder.id) { mutableStateOf(true) }

    CardBlock(title = null, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(if (expanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_right),
                    contentDescription = if (expanded) "Collapse folder" else "Expand folder",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = folder.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            FolderActionMenu(
                onCreateSet = { title -> onCreateSet(folder.id, title) },
                onDeleteFolder = { onDeleteFolder(folder.id) },
                allowDelete = folder.id !in protectedFolderIds
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (folder.sets.isNotEmpty()) {
                    folder.sets.forEach { set ->
                        ProblemSetTree(
                            problemSet = set,
                            selectedProblemSetId = selectedProblemSetId,
                            selectedProblemId = selectedProblemId,
                            draggedProblemKey = draggedProblemKey,
                            hoveredInsertionTarget = hoveredInsertionTarget,
                            query = query,
                            problemFilter = problemFilter,
                            hoveredSetId = hoveredSetId,
                            onProblemSetSelected = onProblemSetSelected,
                            onProblemSelected = onProblemSelected,
                            onDeleteSet = onDeleteSet,
                            allowDeleteSet = set.id !in protectedSetIds,
                            onSetBoundsChange = onSetBoundsChange,
                            onProblemBoundsChange = onProblemBoundsChange,
                            onDragStart = onDragStart,
                            onDragMove = onDragMove,
                            onDragEnd = onDragEnd
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderActionMenu(
    onCreateSet: (String) -> Unit,
    onDeleteFolder: () -> Unit,
    allowDelete: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var creatingSet by remember { mutableStateOf(false) }
    var newSetTitle by remember { mutableStateOf("") }

    Box {
        Surface(
            color = CardBackgroundAlt,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Text(
                text = "...",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("New Set") },
                onClick = {
                    creatingSet = true
                    expanded = false
                }
            )
            if (allowDelete) {
                DropdownMenuItem(
                    text = { Text("Delete Folder") },
                    onClick = {
                        confirmingDelete = true
                        expanded = false
                    }
                )
            }
        }
    }

    if (creatingSet) {
        AlertDialog(
            onDismissRequest = {
                creatingSet = false
                newSetTitle = ""
            },
            title = {
                Text(
                    text = "Create set",
                    color = TextPrimary
                )
            },
            text = {
                BasicTextField(
                    value = newSetTitle,
                    onValueChange = { newSetTitle = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(AccentBlue),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (newSetTitle.isBlank()) {
                            Text(
                                text = "Set name...",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newSetTitle.trim()
                        if (title.isNotEmpty()) {
                            creatingSet = false
                            newSetTitle = ""
                            onCreateSet(title)
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        creatingSet = false
                        newSetTitle = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (confirmingDelete && allowDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = {
                Text(
                    text = "Delete folder?",
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove the folder and all of its sets from the sidebar.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDeleteFolder()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmingDelete = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SetActionMenu(
    onDeleteSet: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    Box {
        Surface(
            color = CardBackgroundAlt,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Text(
                text = "...",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete Set") },
                onClick = {
                    confirmingDelete = true
                    expanded = false
                }
            )
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = {
                Text(
                    text = "Delete set?",
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove the set and all of its problems from the sidebar.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDeleteSet()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmingDelete = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProblemSetTree(
    problemSet: ProblemSetState,
    selectedProblemSetId: String,
    selectedProblemId: String,
    draggedProblemKey: String?,
    hoveredInsertionTarget: InsertionTarget?,
    query: String,
    problemFilter: ProblemFilter,
    hoveredSetId: String?,
    onProblemSetSelected: (String) -> Unit,
    onProblemSelected: (String, String) -> Unit,
    onDeleteSet: (String) -> Unit,
    allowDeleteSet: Boolean,
    onSetBoundsChange: (String, Rect?) -> Unit,
    onProblemBoundsChange: (String, Rect?) -> Unit,
    onDragStart: (String, ProblemListItem, Offset, Offset, IntSize) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var expanded by remember(problemSet.id) { mutableStateOf(selectedProblemSetId == problemSet.id) }
    val filteredProblems = problemSet.problems.mapIndexedNotNull { index, problem ->
        val matchesQuery = query.isBlank() || problem.title.contains(query.trim(), ignoreCase = true)
        val matchesFilter = when (problemFilter) {
            ProblemFilter.All -> true
            ProblemFilter.Open -> !problem.solved
            ProblemFilter.Solved -> problem.solved
        }
        if (matchesQuery && matchesFilter) VisibleProblem(index, problem) else null
    }
    val insertionSlot = hoveredInsertionTarget
        ?.takeIf { it.setId == problemSet.id }
        ?.let { target -> filteredProblems.count { it.index < target.index } }

    DisposableEffect(problemSet.id) {
        onDispose {
            onSetBoundsChange(problemSet.id, null)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        onSetBoundsChange(problemSet.id, coordinates.boundsInRoot())
                    }
                    .clickable {
                        expanded = !expanded
                        onProblemSetSelected(problemSet.id)
                    }
                    .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(if (expanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_right),
                    contentDescription = if (expanded) "Collapse set" else "Expand set",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = problemSet.title,
                    color = when {
                        hoveredSetId == problemSet.id -> AccentGreen
                        selectedProblemSetId == problemSet.id -> AccentBlue
                        else -> TextSecondary
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (allowDeleteSet) {
                SetActionMenu(
                    onDeleteSet = { onDeleteSet(problemSet.id) }
                )
            }
        }
        if (expanded) {
            Column {
                filteredProblems.forEachIndexed { slotIndex, visibleProblem ->
                    InsertionGap(active = insertionSlot == slotIndex)
                    key(visibleProblem.problem.id) {
                        ProblemRow(
                            problem = visibleProblem.problem,
                            selected = selectedProblemSetId == problemSet.id && selectedProblemId == visibleProblem.problem.id,
                            dragging = draggedProblemKey == "${problemSet.id}::${visibleProblem.problem.id}",
                            boundsKey = "${problemSet.id}::${visibleProblem.problem.id}",
                            onClick = { onProblemSelected(problemSet.id, visibleProblem.problem.id) },
                            onBoundsChange = onProblemBoundsChange,
                            onDragStart = { pointer, grabOffset, previewSizePx ->
                                onDragStart(problemSet.id, visibleProblem.problem, pointer, grabOffset, previewSizePx)
                            },
                            onDragMove = onDragMove,
                            onDragEnd = onDragEnd,
                            modifier = Modifier.padding(start = 18.dp)
                        )
                    }
                }
                InsertionGap(active = insertionSlot == filteredProblems.size)
            }
        }
    }
}

@Composable
private fun ColumnScope.AskAiSidebarContent(
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    selectedProblem: ProblemListItem?,
    composerSession: ProblemComposerSession?,
    onComposerSessionChange: (ProblemComposerSession) -> Unit,
    currentDraftCode: String,
    currentCustomTestSuite: ProblemTestSuite,
    customExecutionResult: ProblemExecutionResult,
    submissionExecutionResult: ProblemExecutionResult,
    aiAssistant: AiAssistant,
    onGeneratedCustomTests: (ProblemTestSuite) -> Unit,
    aiRuntimeController: AiRuntimeController
) {
    val scope = rememberCoroutineScope()
    val chatScrollState = rememberScrollState()
    val aiRuntimeState by aiRuntimeController.runtimeState.collectAsState()
    val composerActive = composerSession != null
    val latestComposerSession = rememberUpdatedState(composerSession)
    val availableModes = if (composerActive) {
        listOf(PromptMode.CREATE_PROBLEM)
    } else {
        PromptMode.entries.filterNot { it == PromptMode.CREATE_PROBLEM }
    }
    var selectedMode by remember(selectedProblem?.id, composerActive) {
        mutableStateOf(if (composerActive) PromptMode.CREATE_PROBLEM else PromptMode.HINT)
    }
    var promptDraft by remember(selectedProblem?.id, composerActive) { mutableStateOf("") }
    var messages by remember(selectedProblem?.id, composerActive) { mutableStateOf(listOf<AiChatMessage>()) }
    var isLoading by remember { mutableStateOf(false) }
    val composerStarterCode = remember(composerSession?.draft) {
        composerSession?.draft
            ?.effectiveStarterCode()
            ?.takeIf { it.isNotBlank() }
    }
    val availableDraftCode = remember(currentDraftCode) {
        normalizedCodeForReview(currentDraftCode).takeIf { it.isNotBlank() }
    }
    val userWrittenDraftCode = remember(selectedProblem?.id, currentDraftCode) {
        selectedProblem
            ?.let { problem ->
                normalizedCodeForReview(currentDraftCode)
                    .takeIf { draft -> draft.isNotBlank() && draft != normalizedCodeForReview(problem.starterCode) }
            }
    }
    val codeForMode = when {
        composerActive && selectedMode == PromptMode.CREATE_PROBLEM -> composerStarterCode
        selectedMode == PromptMode.EXPLAIN -> availableDraftCode
        selectedMode == PromptMode.REVIEW_CODE -> userWrittenDraftCode
        selectedMode == PromptMode.TEST_CASES -> null
        else -> userWrittenDraftCode
    }
    val submissionTestSuite = remember(
        selectedProblem?.id,
        selectedProblem?.submissionTestSuite,
        selectedProblem?.statementMarkdown,
        selectedProblem?.exampleInput,
        selectedProblem?.exampleOutput,
        selectedProblem?.starterCode,
        selectedProblem?.executionPipeline
    ) {
        selectedProblem?.let { problem ->
            ProblemInputNormalizer.normalizeSubmissionTestSuite(
                problem = problem,
                testSuite = problem.submissionTestSuite.takeIf {
                    it.cases.isNotEmpty()
                } ?: ProblemSubmissionSuiteFactory.build(problem)
            )
        } ?: ProblemTestSuite()
    }
    val canSend = (selectedProblem != null || composerActive) && !isLoading
    val shouldIncludeDraft = codeForMode != null
    var modeMenuExpanded by remember { mutableStateOf(false) }
    val compactLayout = !fullscreen
    val supportsFreeText = modeSupportsFreeText(selectedMode)
    val panelTitle = when {
        composerActive -> composerSession?.draft?.title?.trim().orEmpty().ifBlank { "Problem Composer" }
        else -> selectedProblem?.title ?: "No problem selected"
    }
    val panelContextLabel = when {
        composerActive && shouldIncludeDraft -> "Composer context and starter code included"
        composerActive -> "Composer context included"
        shouldIncludeDraft -> "Draft included"
        else -> "Draft not included"
    }

    LaunchedEffect(messages.size, isLoading) {
        chatScrollState.scrollTo(chatScrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = CardBackground,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = panelTitle,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = if (compactLayout) 2 else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = panelContextLabel,
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiModeSelectorChip(
                            selectedMode = selectedMode,
                            availableModes = availableModes,
                            expanded = modeMenuExpanded,
                            onExpandedChange = { modeMenuExpanded = it },
                            onModeSelected = { selectedMode = it },
                            modifier = Modifier.weight(1f)
                        )
                        PlainActionChip(
                            label = if (fullscreen) "Exit Full Screen" else "Expand",
                            accentColor = AccentBlue,
                            onClick = onToggleFullscreen
                        )
                    }
                }
                Text(
                    text = if (compactLayout) {
                        compactModeDescription(selectedMode)
                    } else {
                        selectedMode.description
                    },
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Surface(
            color = CardBackground,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty() && !isLoading) {
                    Text(
                        text = emptyStateMessage(selectedMode, compactLayout),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(chatScrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    messages.forEach { message ->
                        AiChatBubble(
                            message = message,
                            fullscreen = fullscreen
                        )
                    }
                    if (isLoading) {
                        AiChatBubble(
                            message = AiChatMessage(
                                sender = AiChatSender.Assistant,
                                mode = selectedMode,
                                text = when (selectedMode) {
                                    PromptMode.CREATE_PROBLEM -> "Drafting the problem..."
                                    PromptMode.HINT -> "Thinking through a hint..."
                                    PromptMode.EXPLAIN -> "Putting the explanation together..."
                                    PromptMode.REVIEW_CODE -> "Reviewing the current draft..."
                                    PromptMode.TEST_CASES -> "Generating importable custom tests..."
                                    PromptMode.SOLVE -> "Writing the solution..."
                                }
                            ),
                            fullscreen = fullscreen,
                            loading = true
                        )
                    }
                }
            }
        }

        Surface(
            color = CardBackground,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = composerLabel(selectedMode, supportsFreeText),
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
                if (supportsFreeText) {
                    EditableSupportTextBlock(
                        value = promptDraft,
                        onValueChange = { promptDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = if (compactLayout) 96.dp else 112.dp,
                        maxHeight = if (compactLayout) 160.dp else 220.dp,
                        scrollable = true
                    )
                } else {
                    Text(
                        text = fixedActionMessage(selectedMode),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlainActionChip(
                        label = "Clear Chat",
                        accentColor = AccentAmber,
                        onClick = if (messages.isNotEmpty() && !isLoading) {
                            { messages = emptyList() }
                        } else {
                            null
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    PlainActionChip(
                        label = actionButtonLabel(selectedMode, compactLayout),
                        accentColor = if (canSend) AccentBlue else TextMuted,
                        onClick = if (canSend) {
                            {
                                val activeProblem = selectedProblem ?: run {
                                    if (!composerActive) {
                                        messages = messages + AiChatMessage(
                                            sender = AiChatSender.System,
                                            mode = selectedMode,
                                            text = "Select a problem first."
                                        )
                                        return@PlainActionChip
                                    }
                                    null
                                }
                                if (!composerActive && selectedMode == PromptMode.REVIEW_CODE && userWrittenDraftCode == null) {
                                    messages = messages + AiChatMessage(
                                        sender = AiChatSender.System,
                                        mode = selectedMode,
                                        text = "There is no user-written code in the editor to review yet."
                                    )
                                    return@PlainActionChip
                                }
                                val promptText = if (supportsFreeText) promptDraft.trim() else ""
                                val userMessage = promptText.ifBlank { defaultChatMessageForMode(selectedMode) }
                                val problemPrompt = if (composerActive) {
                                    buildAiComposerContext(
                                        draft = composerSession!!.draft,
                                        generatorModelName = aiRuntimeState.modelName
                                    )
                                } else {
                                    buildAiProblemContext(
                                        problem = activeProblem!!,
                                        mode = selectedMode,
                                        customTestSuite = currentCustomTestSuite,
                                        submissionTestSuite = submissionTestSuite,
                                        customExecutionResult = customExecutionResult,
                                        submissionExecutionResult = submissionExecutionResult
                                    )
                                }
                                val explicitRequest = promptText.takeIf { it.isNotBlank() }
                                messages = messages + AiChatMessage(
                                    sender = AiChatSender.User,
                                    mode = selectedMode,
                                    text = userMessage
                                )
                                if (supportsFreeText) {
                                    promptDraft = ""
                                }
                                isLoading = true
                                scope.launch {
                                    runCatching {
                                        when (selectedMode) {
                                            PromptMode.CREATE_PROBLEM -> {
                                                val generatedProblem = aiAssistant.createProblem(
                                                    problem = problemPrompt,
                                                    code = codeForMode,
                                                    request = explicitRequest
                                                )
                                                val currentSession = latestComposerSession.value
                                                    ?: error("Problem composer is no longer open.")
                                                val updatedDraft = currentSession.draft.withGeneratedProblem(generatedProblem)
                                                onComposerSessionChange(
                                                    currentSession.copy(
                                                        draft = updatedDraft,
                                                        errorMessage = null
                                                    )
                                                )
                                                buildGeneratedProblemDraftSummary(
                                                    previousDraft = currentSession.draft,
                                                    updatedDraft = updatedDraft
                                                )
                                            }
                                            PromptMode.HINT -> aiAssistant.generateHint(problemPrompt, codeForMode, explicitRequest)
                                            PromptMode.EXPLAIN -> aiAssistant.explainSolution(problemPrompt, codeForMode, explicitRequest)
                                            PromptMode.REVIEW_CODE -> aiAssistant.reviewCode(problemPrompt, codeForMode, explicitRequest)
                                            PromptMode.TEST_CASES -> {
                                                val generatedSuite = aiAssistant.generateCustomTests(
                                                    problem = problemPrompt,
                                                    code = null,
                                                    request = explicitRequest
                                                )
                                                onGeneratedCustomTests(generatedSuite)
                                                buildString {
                                                    append("Imported ${generatedSuite.cases.size} custom cases into the Custom tab.\n\n")
                                                    append(formatGeneratedTestSuiteSummary(generatedSuite))
                                                }
                                            }
                                            PromptMode.SOLVE -> aiAssistant.solveProblem(problemPrompt, codeForMode, explicitRequest)
                                        }
                                    }.onSuccess { response ->
                                        messages = messages + AiChatMessage(
                                            sender = AiChatSender.Assistant,
                                            mode = selectedMode,
                                            text = response.trim()
                                        )
                                    }.onFailure { throwable ->
                                        messages = messages + AiChatMessage(
                                            sender = AiChatSender.System,
                                            mode = selectedMode,
                                            text = throwable.message ?: "Unknown AI runtime error"
                                        )
                                    }
                                    isLoading = false
                                }
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiModeSelectorChip(
    selectedMode: PromptMode,
    availableModes: List<PromptMode>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onModeSelected: (PromptMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Surface(
            color = CardBackgroundAlt,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(true) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = selectedMode.label,
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = "Select AI mode",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            availableModes.forEach { promptMode ->
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = promptMode.label,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = promptMode.description,
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    onClick = {
                        onModeSelected(promptMode)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun AiChatBubble(
    message: AiChatMessage,
    fullscreen: Boolean,
    loading: Boolean = false
) {
    val bubbleColor = when (message.sender) {
        AiChatSender.User -> AccentBlue.copy(alpha = 0.14f)
        AiChatSender.Assistant -> CardBackgroundAlt
        AiChatSender.System -> AccentAmber.copy(alpha = 0.16f)
    }
    val borderColor = when (message.sender) {
        AiChatSender.User -> AccentBlue.copy(alpha = 0.4f)
        AiChatSender.Assistant -> CardBorder
        AiChatSender.System -> AccentAmber.copy(alpha = 0.34f)
    }
    val labelColor = when (message.sender) {
        AiChatSender.User -> AccentBlue
        AiChatSender.Assistant -> AccentGreen
        AiChatSender.System -> AccentAmber
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.sender == AiChatSender.User) Alignment.End else Alignment.Start
    ) {
        Text(
            text = when (message.sender) {
                AiChatSender.User -> "${message.mode.label} request"
                AiChatSender.Assistant -> "${message.mode.label} reply"
                AiChatSender.System -> "System"
            },
            color = labelColor,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(max = if (fullscreen) 920.dp else 460.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = labelColor
                    )
                }
                SelectionContainer {
                    if (message.sender == AiChatSender.Assistant && !loading) {
                        MarkdownStatementText(
                            markdown = message.text,
                            modifier = Modifier.fillMaxWidth(),
                            copyableCodeFences = true
                        )
                    } else {
                        Text(
                            text = message.text,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun formatGeneratedTestSuiteSummary(testSuite: ProblemTestSuite): String {
    return testSuite.cases.joinToString(separator = "\n\n") { testCase ->
        buildString {
            append(testCase.label)
            append("\nInput:\n")
            append(testCase.stdin.ifBlank { "(empty)" })
            append("\n\nExpected:\n")
            append(testCase.expectedOutput.ifBlank { "(empty)" })
            if (testCase.comparisonMode != ProblemTestComparisonMode.Exact) {
                append("\n\nComparison:\n")
                append(testCase.comparisonMode.storageValue)
            }
            if (testCase.acceptableOutputs.isNotEmpty()) {
                append("\n\nAcceptable outputs:\n")
                append(testCase.acceptableOutputs.joinToString("\n"))
            }
        }
    }
}

private fun buildAiProblemContext(
    problem: ProblemListItem,
    mode: PromptMode,
    customTestSuite: ProblemTestSuite,
    submissionTestSuite: ProblemTestSuite,
    customExecutionResult: ProblemExecutionResult,
    submissionExecutionResult: ProblemExecutionResult
): String {
    return if (mode == PromptMode.EXPLAIN || mode == PromptMode.SOLVE) {
        ProblemPromptFormatter.format(
            problem = problem,
            customTestSuite = customTestSuite,
            submissionTestSuite = submissionTestSuite,
            customExecutionResult = customExecutionResult,
            submissionExecutionResult = submissionExecutionResult
        )
    } else {
        ProblemPromptFormatter.format(problem = problem)
    }
}

private fun buildAiComposerContext(
    draft: ProblemComposerDraft,
    generatorModelName: String?
): String {
    val composerContext = ProblemPromptFormatter.formatComposer(
        draft = draft,
        effectiveStarterCode = draft.effectiveStarterCode()
    )
    val modelLine = generatorModelName
        ?.takeIf { it.isNotBlank() }
        ?.let { "Current AI model: $it" }
        ?: "Current AI model: unavailable"
    return "$modelLine\n\n$composerContext"
}

private fun buildGeneratedProblemDraftSummary(
    previousDraft: ProblemComposerDraft,
    updatedDraft: ProblemComposerDraft
): String {
    val changedFields = buildList {
        if (previousDraft.title != updatedDraft.title) add("Title")
        if (previousDraft.difficulty != updatedDraft.difficulty) add("Difficulty")
        if (previousDraft.summary != updatedDraft.summary) add("About This Problem")
        if (previousDraft.statementMarkdown != updatedDraft.statementMarkdown) add("Statement")
        if (previousDraft.exampleInput != updatedDraft.exampleInput) add("Example Input")
        if (previousDraft.exampleOutput != updatedDraft.exampleOutput) add("Example Output")
        if (previousDraft.hintsText != updatedDraft.hintsText) add("Hints")
        if (previousDraft.starterCode != updatedDraft.starterCode) add("Starter Code")
        if (previousDraft.submissionTestSuiteJson != updatedDraft.submissionTestSuiteJson) add("Submission Suite")
        if (previousDraft.executionPipelineOverride != updatedDraft.executionPipelineOverride) add("Execution Pipeline")
    }

    return if (changedFields.isEmpty()) {
        "The AI draft parsed successfully, but it did not change any composer fields."
    } else {
        "Updated composer fields: ${changedFields.joinToString(", ")}."
    }
}

private fun defaultChatMessageForMode(mode: PromptMode): String {
    return when (mode) {
        PromptMode.CREATE_PROBLEM -> "Help me draft this problem."
        PromptMode.HINT -> "Give me a hint."
        PromptMode.EXPLAIN -> "Explain the solution."
        PromptMode.REVIEW_CODE -> "Review my current code with inline comments."
        PromptMode.TEST_CASES -> "Generate importable custom tests."
        PromptMode.SOLVE -> "Show me the solution."
    }
}

private fun compactModeDescription(mode: PromptMode): String {
    return when (mode) {
        PromptMode.CREATE_PROBLEM -> "Draft problem fields from the current composer."
        PromptMode.HINT -> "Get a small next-step nudge."
        PromptMode.EXPLAIN -> "Get the idea and complexity."
        PromptMode.REVIEW_CODE -> "Review the draft with inline comments."
        PromptMode.TEST_CASES -> "Generate tests for the Custom tab."
        PromptMode.SOLVE -> "Get the full approach and code."
    }
}

private fun modeSupportsFreeText(mode: PromptMode): Boolean {
    return when (mode) {
        PromptMode.REVIEW_CODE, PromptMode.SOLVE -> false
        PromptMode.CREATE_PROBLEM, PromptMode.HINT, PromptMode.EXPLAIN, PromptMode.TEST_CASES -> true
    }
}

private fun normalizedCodeForReview(code: String): String {
    return code
        .replace("\r\n", "\n")
        .trim()
}

private fun composerLabel(mode: PromptMode, supportsFreeText: Boolean): String {
    if (supportsFreeText) {
        return when (mode) {
            PromptMode.CREATE_PROBLEM -> "Problem draft request"
            PromptMode.HINT -> "Hint request"
            PromptMode.EXPLAIN -> "Explain request"
            PromptMode.REVIEW_CODE -> "Code review request"
            PromptMode.TEST_CASES -> "Test generation request"
            PromptMode.SOLVE -> "Solve request"
        }
    }

    return when (mode) {
        PromptMode.REVIEW_CODE -> "Code review action"
        PromptMode.SOLVE -> "Solve action"
        PromptMode.CREATE_PROBLEM -> "Problem draft request"
        PromptMode.HINT -> "Hint request"
        PromptMode.EXPLAIN -> "Explain request"
        PromptMode.TEST_CASES -> "Test generation request"
    }
}

private fun fixedActionMessage(mode: PromptMode): String {
    return when (mode) {
        PromptMode.REVIEW_CODE -> "Returns an inline-commented copy of the current draft showing where runtime, space, and obvious issues come from."
        PromptMode.SOLVE -> "Runs a fixed full-solution request for the current problem."
        PromptMode.CREATE_PROBLEM,
        PromptMode.HINT,
        PromptMode.EXPLAIN,
        PromptMode.TEST_CASES -> ""
    }
}

private fun emptyStateMessage(mode: PromptMode, compact: Boolean): String {
    return if (!compact) {
        when (mode) {
            PromptMode.CREATE_PROBLEM -> "Ask for a full draft, better wording, cleaner examples, hints, or starter code based on the current composer."
            PromptMode.HINT -> "Ask for a hint, or leave the box empty to get a small nudge."
            PromptMode.EXPLAIN -> "Ask for an explanation, or add what you want emphasized."
            PromptMode.REVIEW_CODE -> "Review the current draft and get an inline-commented copy that explains runtime, space, and obvious issues."
            PromptMode.TEST_CASES -> "Ask for importable custom tests. They append into the Custom tab."
            PromptMode.SOLVE -> "Ask for a full solution, optionally steering the approach."
        }
    } else {
        when (mode) {
            PromptMode.CREATE_PROBLEM -> "Ask for a better problem draft."
            PromptMode.HINT -> "Ask for a small hint."
            PromptMode.EXPLAIN -> "Ask for a clear walkthrough."
            PromptMode.REVIEW_CODE -> "Review the draft with inline comments."
            PromptMode.TEST_CASES -> "Generate importable custom tests."
            PromptMode.SOLVE -> "Ask for the full solution."
        }
    }
}

private fun actionButtonLabel(mode: PromptMode, compact: Boolean): String {
    return if (!compact) {
        mode.actionLabel
    } else {
        when (mode) {
            PromptMode.CREATE_PROBLEM -> "Draft"
            PromptMode.HINT -> "Hint"
            PromptMode.EXPLAIN -> "Explain"
            PromptMode.REVIEW_CODE -> "Review"
            PromptMode.TEST_CASES -> "Generate"
            PromptMode.SOLVE -> "Solve"
        }
    }
}

private data class AiChatMessage(
    val sender: AiChatSender,
    val mode: PromptMode,
    val text: String
)

private enum class AiChatSender {
    User,
    Assistant,
    System
}

@Composable
private fun ColumnScope.SettingsSidebarContent(
    aiRuntimeController: AiRuntimeController,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    val runtimeState by aiRuntimeController.runtimeState.collectAsState()
    val scope = rememberCoroutineScope()
    val settingsScrollState = rememberScrollState()
    val modelImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                aiRuntimeController.importModel(uri)
            }
        }
    }
    val runtimeBusy = runtimeState.phase in setOf(
        AiRuntimePhase.Importing,
        AiRuntimePhase.Downloading,
        AiRuntimePhase.Loading,
        AiRuntimePhase.Generating,
        AiRuntimePhase.Unloading
    )
    val downloadingModel = runtimeState.phase == AiRuntimePhase.Downloading
    val hasConfiguredModel = runtimeState.currentModelPath != null
    val installedModels = runtimeState.installedModels
    val installedCustomModels = installedModels.filter { it.preset == null }
    val showPresetChooser = !downloadingModel
    val visiblePresets = if (showPresetChooser) listOf(AiModelPreset.QwenCoder15BQ4) else emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(settingsScrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CardBlock(title = "Appearance", modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Theme",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeModeOption(
                    themeMode = AppThemeMode.Night,
                    selected = themeMode == AppThemeMode.Night,
                    onClick = { onThemeModeChange(AppThemeMode.Night) },
                    modifier = Modifier.weight(1f)
                )
                ThemeModeOption(
                    themeMode = AppThemeMode.Light,
                    selected = themeMode == AppThemeMode.Light,
                    onClick = { onThemeModeChange(AppThemeMode.Light) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        CardBlock(title = "AI Runtime", modifier = Modifier.fillMaxWidth()) {
            SettingLine("Inference", "On-device llama.cpp")
            Spacer(modifier = Modifier.height(6.dp))
            SettingLine("Status", runtimeState.phase.displayLabel())
            runtimeState.modelName?.let { modelName ->
                Spacer(modifier = Modifier.height(6.dp))
                SettingLine("Model", modelName)
            }
            runtimeState.preset?.let { preset ->
                Spacer(modifier = Modifier.height(6.dp))
                SettingLine("Preset", preset.shortLabel)
            }
            runtimeState.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = detail,
                    color = if (runtimeState.phase == AiRuntimePhase.Error) AccentRed else TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (runtimeState.phase == AiRuntimePhase.Downloading) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { runtimeState.progress ?: 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentBlue,
                    trackColor = AccentBlue.copy(alpha = 0.18f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Bring your own model",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            PlainActionChip(
                label = if (hasConfiguredModel) "Import Another .gguf" else "Import .gguf",
                accentColor = AccentBlue,
                onClick = if (!runtimeBusy) {
                    {
                        modelImporter.launch(arrayOf("*/*"))
                    }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (installedCustomModels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Imported models",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    installedCustomModels.forEach { model ->
                        InstalledModelCard(
                            model = model,
                            currentModelPath = runtimeState.currentModelPath,
                            busy = runtimeBusy,
                            onSelect = {
                                scope.launch {
                                    runCatching {
                                        aiRuntimeController.selectStoredModel(model.path)
                                    }
                                }
                            }
                        )
                    }
                }
            }
            if (visiblePresets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Recommended models",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visiblePresets.forEach { preset ->
                        val installedPresetModel = installedModels.firstOrNull { it.preset == preset }
                        ModelPresetCard(
                            preset = preset,
                            installedModel = installedPresetModel,
                            currentModelPath = runtimeState.currentModelPath,
                            phase = runtimeState.phase,
                            busy = runtimeBusy,
                            onDownload = {
                                scope.launch {
                                    runCatching {
                                        aiRuntimeController.downloadPresetModel(preset)
                                    }
                                }
                            },
                            onSelect = {
                                scope.launch {
                                    runCatching {
                                        aiRuntimeController.selectStoredModel(installedPresetModel!!.path)
                                    }
                                }
                            }
                        )
                    }
                }
            }
            if (hasConfiguredModel && !downloadingModel) {
                Spacer(modifier = Modifier.height(8.dp))
                PlainActionChip(
                    label = "Remove Model",
                    accentColor = AccentRed,
                    onClick = if (!runtimeBusy) {
                        {
                            scope.launch {
                                runCatching {
                                    aiRuntimeController.removeConfiguredModel()
                                }
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        CardBlock(title = "Settings", modifier = Modifier.fillMaxWidth()) {
            SettingLine("Runtime", "Embedded Python")
            Spacer(modifier = Modifier.height(6.dp))
            SettingLine(
                "AI",
                when {
                    runtimeState.phase == AiRuntimePhase.Ready -> "On-device model loaded"
                    hasConfiguredModel -> "On-device model stored"
                    else -> "On-device model idle"
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
            SettingLine("Theme", themeMode.label)
        }
    }
}

private fun AiRuntimePhase.displayLabel(): String {
    return when (this) {
        AiRuntimePhase.Unconfigured -> "No model selected"
        AiRuntimePhase.Importing -> "Importing"
        AiRuntimePhase.Downloading -> "Downloading"
        AiRuntimePhase.Configured -> "Configured"
        AiRuntimePhase.Loading -> "Loading"
        AiRuntimePhase.Ready -> "Ready"
        AiRuntimePhase.Generating -> "Generating"
        AiRuntimePhase.Unloading -> "Unloading"
        AiRuntimePhase.Error -> "Error"
    }
}

@Composable
private fun ModelPresetCard(
    preset: AiModelPreset,
    installedModel: AiStoredModel?,
    currentModelPath: String?,
    phase: AiRuntimePhase,
    busy: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit
) {
    val selected = installedModel?.path == currentModelPath
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.12f) else CardBackgroundAlt,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.42f) else CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.shortLabel,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = preset.sizeLabel,
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (installedModel == null) {
                    PlainActionChip(
                        label = if (phase == AiRuntimePhase.Downloading) "Downloading" else "Download",
                        accentColor = AccentBlue,
                        onClick = if (!busy && phase != AiRuntimePhase.Downloading) onDownload else null
                    )
                } else {
                    PlainActionChip(
                        label = when {
                            selected && phase == AiRuntimePhase.Loading -> "Loading"
                            selected && phase == AiRuntimePhase.Ready -> "Current"
                            selected -> "Selected"
                            else -> "Switch"
                        },
                        accentColor = when {
                            selected && phase == AiRuntimePhase.Ready -> AccentGreen
                            selected -> AccentAmber
                            else -> AccentBlue
                        },
                        onClick = if (!busy && !selected) onSelect else null
                    )
                }
            }
            Text(
                text = preset.label,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = preset.description,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun InstalledModelCard(
    model: AiStoredModel,
    currentModelPath: String?,
    busy: Boolean,
    onSelect: () -> Unit
) {
    val selected = model.path == currentModelPath
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.12f) else CardBackgroundAlt,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.42f) else CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Imported .gguf",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            PlainActionChip(
                label = if (selected) "Current" else "Switch",
                accentColor = if (selected) AccentGreen else AccentBlue,
                onClick = if (!busy && !selected) onSelect else null
            )
        }
    }
}

@Composable
private fun ThemeModeOption(
    themeMode: AppThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.14f) else CardBackgroundAlt,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.42f) else CardBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = themeMode.label,
                color = if (selected) TextPrimary else TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = themeMode.description,
                color = if (selected) AccentBlue else TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun ProblemRow(
    problem: ProblemListItem,
    selected: Boolean,
    dragging: Boolean,
    boundsKey: String,
    onClick: () -> Unit,
    onBoundsChange: (String, Rect?) -> Unit,
    onDragStart: (Offset, Offset, IntSize) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) CardBackground else CardBackgroundAlt
    val borderColor = if (selected) AccentBlue.copy(alpha = 0.24f) else Color.Transparent
    var rowOrigin by remember { mutableStateOf(Offset.Zero) }
    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var contentHeightPx by remember { mutableStateOf(0) }
    var handleOrigin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val rowAlpha by animateFloatAsState(
        targetValue = if (dragging) 0f else 1f,
        animationSpec = spring(stiffness = 550f, dampingRatio = 0.85f),
        label = "problemRowAlpha"
    )
    val collapsedHeight by animateDpAsState(
        targetValue = if (dragging) 0.dp else with(density) { contentHeightPx.toDp() },
        animationSpec = spring(stiffness = 550f, dampingRatio = 0.85f),
        label = "problemRowHeight"
    )

    DisposableEffect(boundsKey, dragging) {
        if (dragging) {
            onBoundsChange(boundsKey, null)
        }
        onDispose {
            onBoundsChange(boundsKey, null)
        }
    }

    Column(
        modifier = modifier
            .then(if (dragging && contentHeightPx > 0) Modifier.height(collapsedHeight) else Modifier)
            .alpha(rowAlpha)
            .clipToBounds()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    rowOrigin = coordinates.boundsInRoot().topLeft
                    if (!dragging) {
                        onBoundsChange(boundsKey, coordinates.boundsInRoot())
                    }
                }
                .onSizeChanged { rowSize = it }
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            ProblemRowContent(
                problem = problem,
                modifier = Modifier.weight(1f)
            )
            DragHandle(
                onDragStart = { offset ->
                    val absolutePointer = handleOrigin + offset
                    onDragStart(
                        absolutePointer,
                        absolutePointer - rowOrigin,
                        rowSize
                    )
                },
                onDragMove = onDragMove,
                onPositioned = { handleOrigin = it },
                onDragEnd = onDragEnd
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = DividerColor,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .onSizeChanged { dividerSize ->
                    val totalHeight = rowSize.height + dividerSize.height
                    if (totalHeight > 0) {
                        contentHeightPx = totalHeight
                    }
                }
        )
    }
}

@Composable
private fun InsertionGap(active: Boolean) {
    val gapHeight by animateDpAsState(
        targetValue = if (active) 18.dp else 0.dp,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.82f),
        label = "insertionGapHeight"
    )
    val lineHeight by animateDpAsState(
        targetValue = if (active) 2.dp else 0.dp,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.82f),
        label = "insertionGapLineHeight"
    )
    val lineAlpha by animateFloatAsState(
        targetValue = if (active) 0.7f else 0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.82f),
        label = "insertionGapAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 6.dp)
            .height(gapHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(lineHeight)
                .background(AccentBlue.copy(alpha = lineAlpha), RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun ProblemRowContent(
    problem: ProblemListItem,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = problem.title,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (problem.solved) {
            SolvedFlag()
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun DragHandle(
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onPositioned: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Surface(
        color = CardBackgroundAlt,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.boundsInRoot().topLeft)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = onDragStart,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                ) { change, dragAmount ->
                    change.consume()
                    onDragMove(dragAmount)
                }
            }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = "Reorder problem",
            tint = TextMuted,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .size(16.dp)
        )
    }
}

@Composable
private fun TrashDropTarget(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (active) AccentRed.copy(alpha = 0.3f) else AccentRed.copy(alpha = 0.18f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AccentRed.copy(alpha = if (active) 0.82f else 0.5f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Trash",
                color = AccentRed,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun DraggedProblemState.previewRect(): Rect {
    val left = pointer.x - grabOffset.x
    val top = pointer.y - grabOffset.y
    return Rect(
        left = left,
        top = top,
        right = left + previewSizePx.width,
        bottom = top + previewSizePx.height
    )
}

private fun Rect.overlapsWith(other: Rect): Boolean {
    return left < other.right &&
        right > other.left &&
        top < other.bottom &&
        bottom > other.top
}

private data class VisibleProblem(
    val index: Int,
    val problem: ProblemListItem
)

private fun resolveProblemInsertionTarget(
    entryKey: String,
    pointer: Offset,
    entryBounds: Rect,
    folders: List<ProblemFolderState>
): InsertionTarget? {
    val setId = entryKey.substringBefore("::")
    val problemId = entryKey.substringAfter("::", missingDelimiterValue = "")
    val set = folders.flatMap { it.sets }.firstOrNull { it.id == setId } ?: return null
    val rowIndex = set.problems.indexOfFirst { it.id == problemId }
    if (rowIndex == -1) return null
    val insertionIndex = if (pointer.y < entryBounds.center.y) rowIndex else rowIndex + 1
    return InsertionTarget(setId, insertionIndex)
}

private fun resolveSetInsertionTarget(
    setId: String,
    folders: List<ProblemFolderState>,
    problemBounds: Map<String, Rect>
): InsertionTarget? {
    val set = folders.flatMap { it.sets }.firstOrNull { it.id == setId } ?: return null
    val visibleRowsForSet = problemBounds.entries
        .filter { it.key.substringBefore("::") == setId }
        .sortedBy { it.value.top }
    val insertionIndex = visibleRowsForSet.lastOrNull()?.let { entry ->
        val problemId = entry.key.substringAfter("::", missingDelimiterValue = "")
        set.problems.indexOfFirst { it.id == problemId }
            .takeIf { it >= 0 }
            ?.plus(1)
    } ?: set.problems.size
    return InsertionTarget(setId, insertionIndex.coerceIn(0, set.problems.size))
}

@Composable
private fun SolvedFlag() {
    Surface(
        color = AccentGreen.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.35f))
    ) {
        Text(
            text = "Solved",
            color = AccentGreen,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ProblemFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.12f) else CardBackgroundAlt,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.36f) else CardBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) AccentBlue else TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

