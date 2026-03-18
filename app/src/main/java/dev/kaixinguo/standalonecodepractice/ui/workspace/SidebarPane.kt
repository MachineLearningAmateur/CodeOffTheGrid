package dev.kaixinguo.standalonecodepractice.ui.workspace

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
    selectedProblemTitle: String,
    onProblemSetSelected: (String) -> Unit,
    onProblemSelected: (String, String) -> Unit,
    onDeleteProblem: (String, ProblemListItem) -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateSet: (String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onMoveProblem: (String, String, ProblemListItem, Int) -> Unit,
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
                        selectedProblemTitle = selectedProblemTitle,
                        onProblemSetSelected = onProblemSetSelected,
                        onProblemSelected = onProblemSelected,
                        onDeleteProblem = onDeleteProblem,
                        onCreateFolder = onCreateFolder,
                        onCreateSet = onCreateSet,
                        onDeleteSet = onDeleteSet,
                        onDeleteFolder = onDeleteFolder,
                        onMoveProblem = onMoveProblem
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
    selectedProblemTitle: String,
    onProblemSetSelected: (String) -> Unit,
    onProblemSelected: (String, String) -> Unit,
    onDeleteProblem: (String, ProblemListItem) -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateSet: (String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onMoveProblem: (String, String, ProblemListItem, Int) -> Unit
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
                items(items = folders, key = { it.title }) { folder ->
                    FolderTreeBlock(
                        folder = folder,
                        selectedProblemSetId = selectedProblemSetId,
                        selectedProblemTitle = selectedProblemTitle,
                        draggedProblemKey = draggedProblem?.let { "${it.sourceSetId}::${it.problem.title}" },
                        hoveredInsertionTarget = hoveredInsertionTarget,
                        query = query,
                        problemFilter = problemFilter,
                        hoveredSetId = hoveredSetId,
                        onProblemSetSelected = onProblemSetSelected,
                        onProblemSelected = onProblemSelected,
                        onCreateSet = onCreateSet,
                        onDeleteSet = onDeleteSet,
                        onDeleteFolder = onDeleteFolder,
                        onSetBoundsChange = { setId, bounds -> setBounds[setId] = bounds },
                        onProblemBoundsChange = { key, bounds -> problemBounds[key] = bounds },
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
                        onDragMove = { pointer ->
                            val updatedDrag = draggedProblem?.copy(pointer = pointer)
                            draggedProblem = updatedDrag
                            val dragRect = updatedDrag?.previewRect()
                            val isOverTrash = dragRect != null && trashBounds?.overlapsWith(dragRect) == true
                            hoveredTrash = isOverTrash
                            if (isOverTrash) {
                                hoveredSetId = null
                                hoveredInsertionTarget = null
                            } else {
                                val hoveredProblemEntry = problemBounds.entries.firstOrNull { (_, bounds) ->
                                    bounds.contains(pointer)
                                }
                                if (hoveredProblemEntry != null) {
                                    val key = hoveredProblemEntry.key
                                    val separatorIndex = key.indexOf("::")
                                    val setId = key.substring(0, separatorIndex)
                                    val rowsForSet = problemBounds.entries
                                        .filter { it.key.startsWith("$setId::") }
                                        .sortedBy { it.value.top }
                                    val rowIndex = rowsForSet.indexOfFirst { it.key == key }
                                    val hoveredBounds = hoveredProblemEntry.value
                                    val insertionIndex = if (pointer.y < hoveredBounds.center.y) rowIndex else rowIndex + 1
                                    hoveredSetId = setId
                                    hoveredInsertionTarget = InsertionTarget(setId, insertionIndex)
                                } else {
                                    val targetSetId = setBounds.entries.firstOrNull { (_, bounds) ->
                                        bounds.contains(pointer)
                                    }?.key
                                    hoveredSetId = targetSetId
                                    hoveredInsertionTarget = targetSetId?.let { setId ->
                                        val rowCount = problemBounds.keys.count { it.startsWith("$setId::") }
                                        InsertionTarget(setId, rowCount)
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
                                        val key = problemEntry.key
                                        val separatorIndex = key.indexOf("::")
                                        val setId = key.substring(0, separatorIndex)
                                        val rowsForSet = problemBounds.entries
                                            .filter { it.key.startsWith("$setId::") }
                                            .sortedBy { it.value.top }
                                        val rowIndex = rowsForSet.indexOfFirst { it.key == key }
                                        val insertionIndex = if (finalPointer.y < problemEntry.value.center.y) rowIndex else rowIndex + 1
                                        InsertionTarget(setId, insertionIndex)
                                    } else {
                                        setBounds.entries.firstOrNull { (_, bounds) ->
                                            bounds.contains(finalPointer)
                                        }?.key?.let { setId ->
                                            InsertionTarget(
                                                setId = setId,
                                                index = problemBounds.keys.count { it.startsWith("$setId::") }
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
                                        onProblemSelected(finalTarget.setId, drag.problem.title)
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
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PlainActionChip("New Folder") {
                            creatingFolder = true
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
    selectedProblemTitle: String,
    draggedProblemKey: String?,
    hoveredInsertionTarget: InsertionTarget?,
    query: String,
    problemFilter: ProblemFilter,
    hoveredSetId: String?,
    onProblemSetSelected: (String) -> Unit,
    onProblemSelected: (String, String) -> Unit,
    onCreateSet: (String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onSetBoundsChange: (String, Rect) -> Unit,
    onProblemBoundsChange: (String, Rect) -> Unit,
    onDragStart: (String, ProblemListItem, Offset, Offset, IntSize) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var expanded by remember(folder.title) { mutableStateOf(true) }

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
                onCreateSet = { onCreateSet(folder.title) },
                onDeleteFolder = { onDeleteFolder(folder.title) }
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
                            selectedProblemTitle = selectedProblemTitle,
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
    onCreateSet: () -> Unit,
    onDeleteFolder: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                    onCreateSet()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Delete Folder") },
                onClick = {
                    onDeleteFolder()
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun SetActionMenu(
    onDeleteSet: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                    onDeleteSet()
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun ProblemSetTree(
    problemSet: ProblemSetState,
    selectedProblemSetId: String,
    selectedProblemTitle: String,
    draggedProblemKey: String?,
    hoveredInsertionTarget: InsertionTarget?,
    query: String,
    problemFilter: ProblemFilter,
    hoveredSetId: String?,
    onProblemSetSelected: (String) -> Unit,
    onProblemSelected: (String, String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onSetBoundsChange: (String, Rect) -> Unit,
    onProblemBoundsChange: (String, Rect) -> Unit,
    onDragStart: (String, ProblemListItem, Offset, Offset, IntSize) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var expanded by remember(problemSet.id) { mutableStateOf(selectedProblemSetId == problemSet.id) }
    val filteredProblems = problemSet.problems.filter { problem ->
        val matchesQuery = query.isBlank() || problem.title.contains(query.trim(), ignoreCase = true)
        val matchesFilter = when (problemFilter) {
            ProblemFilter.All -> true
            ProblemFilter.Open -> !problem.solved
            ProblemFilter.Solved -> problem.solved
        }
        matchesQuery && matchesFilter
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
            filteredProblems.forEachIndexed { index, problem ->
                if (hoveredInsertionTarget?.setId == problemSet.id && hoveredInsertionTarget.index == index) {
                    InsertionGap()
                }
                ProblemRow(
                    problem = problem,
                    selected = selectedProblemSetId == problemSet.id && selectedProblemTitle == problem.title,
                    dragging = draggedProblemKey == "${problemSet.id}::${problem.title}",
                    boundsKey = "${problemSet.id}::${problem.title}",
                    onClick = { onProblemSelected(problemSet.id, problem.title) },
                    onBoundsChange = onProblemBoundsChange,
                    onDragStart = { pointer, grabOffset, previewSizePx ->
                        onDragStart(problemSet.id, problem, pointer, grabOffset, previewSizePx)
                    },
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd,
                    modifier = Modifier.padding(start = 18.dp)
                )
            }
            if (hoveredInsertionTarget?.setId == problemSet.id && hoveredInsertionTarget.index == filteredProblems.size) {
                InsertionGap()
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
        SettingLine("Problem imports", "Local JSON")
        Spacer(modifier = Modifier.padding(6.dp))
        SettingLine("Runtime", "Embedded Python")
        Spacer(modifier = Modifier.padding(6.dp))
        SettingLine("Theme", "Dark tablet layout")
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
    onBoundsChange: (String, Rect) -> Unit,
    onDragStart: (Offset, Offset, IntSize) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) CardBackground else CardBackgroundAlt
    val borderColor = if (selected) AccentBlue.copy(alpha = 0.24f) else Color.Transparent
    var rowOrigin by remember { mutableStateOf(Offset.Zero) }
    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var handleOrigin by remember { mutableStateOf(Offset.Zero) }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (dragging) 0f else 1f)
                .onGloballyPositioned { coordinates ->
                    rowOrigin = coordinates.boundsInRoot().topLeft
                    onBoundsChange(boundsKey, coordinates.boundsInRoot())
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
                onDragMove = { pointer -> onDragMove(handleOrigin + pointer) },
                onPositioned = { handleOrigin = it },
                onDragEnd = onDragEnd
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = DividerColor,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun InsertionGap() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
            .height(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(AccentBlue.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
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
                ) { change, _ ->
                    change.consume()
                    onDragMove(change.position)
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

