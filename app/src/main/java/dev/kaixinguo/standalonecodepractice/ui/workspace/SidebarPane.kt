package dev.kaixinguo.standalonecodepractice.ui.workspace

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentBlue
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentGreen
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentRed
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBackgroundAlt
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBorder
import dev.kaixinguo.standalonecodepractice.ui.theme.DividerColor
import dev.kaixinguo.standalonecodepractice.ui.theme.SidebarBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.TextMuted
import dev.kaixinguo.standalonecodepractice.ui.theme.TextPrimary
import dev.kaixinguo.standalonecodepractice.ui.theme.TextSecondary

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
    onImportGitHubRepo: (String) -> Unit,
    importInProgress: Boolean,
    importFeedback: String?,
    selectedMode: SidebarMode,
    onModeSelected: (SidebarMode) -> Unit,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SidebarBackground,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        if (collapsed) {
            CollapsedSidebarPane(
                selectedMode = selectedMode,
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

                SidebarModeSelector(
                    selectedMode = selectedMode,
                    onModeSelected = onModeSelected
                )

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
                        onMoveProblem = onMoveProblem,
                        onImportGitHubRepo = onImportGitHubRepo,
                        importInProgress = importInProgress,
                        importFeedback = importFeedback
                    )
                    SidebarMode.AskAi -> AskAiSidebarContent()
                    SidebarMode.Settings -> SettingsSidebarContent()
                }
            }
        }
    }
}

@Composable
private fun CollapsedSidebarPane(
    selectedMode: SidebarMode,
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
                    .clickable { expanded = true }
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
                SmallActionChip("Swap")
            }

            DropdownMenu(
                expanded = expanded,
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
    onMoveProblem: (String, String, ProblemListItem, Int) -> Unit,
    onImportGitHubRepo: (String) -> Unit,
    importInProgress: Boolean,
    importFeedback: String?
) {
    var query by remember { mutableStateOf("") }
    var problemFilter by remember { mutableStateOf(ProblemFilter.All) }
    var creatingFolder by remember { mutableStateOf(false) }
    var newFolderTitle by remember { mutableStateOf("") }
    var importingRepo by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }
    var draggedProblem by remember { mutableStateOf<DraggedProblemState?>(null) }
    var hoveredSetId by remember { mutableStateOf<String?>(null) }
    var hoveredInsertionTarget by remember { mutableStateOf<InsertionTarget?>(null) }
    var hoveredTrash by remember { mutableStateOf(false) }
    val setBounds = remember { mutableStateMapOf<String, Rect>() }
    val problemBounds = remember { mutableStateMapOf<String, Rect>() }
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    val density = LocalDensity.current

    LaunchedEffect(importFeedback, importInProgress) {
        if (!importInProgress && importFeedback?.startsWith("Imported") == true) {
            importUrl = ""
            importingRepo = false
        }
    }

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
                            val dragRect = updatedDrag?.previewRect()
                            val isOverTrash = dragRect != null && trashBounds?.overlapsWith(dragRect) == true
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
                                val finalPointer = drag.pointer
                                val droppingOnTrash = trashBounds?.overlapsWith(drag.previewRect()) == true
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
        if (draggedProblem != null) {
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
                } else if (importingRepo) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BasicTextField(
                            value = importUrl,
                            onValueChange = { importUrl = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            cursorBrush = SolidColor(AccentBlue),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("githubImportUrlField"),
                            decorationBox = { innerTextField ->
                                if (importUrl.isBlank()) {
                                    Text(
                                        text = "GitHub repo URL...",
                                        color = TextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                innerTextField()
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlainActionChip(
                                label = if (importInProgress) "Importing..." else "Import",
                                modifier = Modifier.testTag("githubImportSubmit")
                            ) {
                                val url = importUrl.trim()
                                if (url.isNotEmpty() && !importInProgress) {
                                    onImportGitHubRepo(url)
                                }
                            }
                            PlainActionChip("Cancel") {
                                importUrl = ""
                                importingRepo = false
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlainActionChip("New Folder") {
                            importingRepo = false
                            creatingFolder = true
                        }
                        PlainActionChip(
                            label = "+ Repo",
                            modifier = Modifier.testTag("githubImportOpen")
                        ) {
                            creatingFolder = false
                            importingRepo = true
                        }
                    }
                }
                if (!importFeedback.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = importFeedback,
                        color = if (importFeedback.startsWith("Imported")) AccentGreen else AccentRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("githubImportFeedback")
                    )
                }
                if (!creatingFolder && !importingRepo && importInProgress) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Importing repository...",
                        color = AccentBlue,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                Text(
                    text = if (expanded) "v" else ">",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(18.dp)
                )
                Text(
                    text = folder.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            FolderActionMenu(
                onCreateSet = { title -> onCreateSet(folder.id, title) },
                onDeleteFolder = { onDeleteFolder(folder.id) }
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
    onDeleteFolder: () -> Unit
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
            DropdownMenuItem(
                text = { Text("Delete Folder") },
                onClick = {
                    confirmingDelete = true
                    expanded = false
                }
            )
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

    if (confirmingDelete) {
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
                Text(
                    text = if (expanded) "v" else ">",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(18.dp)
                )
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
            SetActionMenu(
                onDeleteSet = { onDeleteSet(problemSet.id) }
            )
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
private fun ColumnScope.AskAiSidebarContent() {
    CardBlock(title = "Ask AI", modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Use local AI later for hints, explanations, and study prompts.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    CardBlock(title = "Quick Prompts", modifier = Modifier.fillMaxWidth()) {
        SuggestionLine("Explain the traversal order")
        Spacer(modifier = Modifier.padding(6.dp))
        SuggestionLine("Give me a recursion hint")
        Spacer(modifier = Modifier.padding(6.dp))
        SuggestionLine("Compare recursion vs stack")
    }

    Spacer(modifier = Modifier.weight(1f))
}

@Composable
private fun ColumnScope.SettingsSidebarContent() {
    CardBlock(title = "Settings", modifier = Modifier.fillMaxWidth()) {
        SettingLine("Runtime", "Embedded Python")
        Spacer(modifier = Modifier.padding(6.dp))
        SettingLine("Theme", "Dark tablet layout")
        Spacer(modifier = Modifier.padding(6.dp))
        SettingLine("Catalog source", "Local + GitHub imports")
    }

    CardBlock(title = "Later", modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Export, backups, and model controls can live here when the real settings flow exists.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    Spacer(modifier = Modifier.weight(1f))
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
        Text(
            text = ":::",
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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

