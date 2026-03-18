package dev.kaixinguo.standaloneleetcode.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.kaixinguo.standaloneleetcode.ui.theme.AccentAmber
import dev.kaixinguo.standaloneleetcode.ui.theme.AccentBlue
import dev.kaixinguo.standaloneleetcode.ui.theme.AccentGreen
import dev.kaixinguo.standaloneleetcode.ui.theme.AccentRed
import dev.kaixinguo.standaloneleetcode.ui.theme.AppBackground
import dev.kaixinguo.standaloneleetcode.ui.theme.CardBackground
import dev.kaixinguo.standaloneleetcode.ui.theme.CardBackgroundAlt
import dev.kaixinguo.standaloneleetcode.ui.theme.CardBorder
import dev.kaixinguo.standaloneleetcode.ui.theme.DividerColor
import dev.kaixinguo.standaloneleetcode.ui.theme.PaneBackground
import dev.kaixinguo.standaloneleetcode.ui.theme.SidebarBackground
import dev.kaixinguo.standaloneleetcode.ui.theme.StandaloneLeetCodeTheme
import dev.kaixinguo.standaloneleetcode.ui.theme.TextMuted
import dev.kaixinguo.standaloneleetcode.ui.theme.TextPrimary
import dev.kaixinguo.standaloneleetcode.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.hypot

private data class ProblemListItem(
    val title: String,
    val active: Boolean = false,
    val solved: Boolean = false
)

private enum class SidebarMode(val label: String) {
    Problems("Problems"),
    AskAi("Ask AI"),
    Settings("Settings")
}

private enum class ProblemFilter(val label: String) {
    All("All"),
    Open("Open"),
    Solved("Solved")
}

private enum class WorkspaceInputMode {
    Keyboard,
    Sketch
}

private enum class SupportTab(val label: String) {
    Problem("Problem"),
    Custom("Custom"),
    Results("Results")
}

private enum class SketchTool {
    Pen,
    Eraser
}

private enum class EraserSize(val label: String, val radius: Float) {
    Small("S", 16f),
    Medium("M", 28f),
    Large("L", 42f)
}

private data class SketchStroke(
    val points: List<Offset>,
    val color: Color
)

private val PythonKeywordColor = Color(0xFF7DB2FF)
private val PythonStringColor = Color(0xFFE8B66B)
private val PythonCommentColor = Color(0xFF7A8A9F)
private val PythonNumberColor = Color(0xFF7ED2C3)
private val PythonFunctionColor = Color(0xFFC58BFF)
private val PythonDecoratorColor = Color(0xFFFF9B7A)

private val PythonKeywords = setOf(
    "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del",
    "elif", "else", "except", "False", "finally", "for", "from", "global", "if", "import",
    "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "return",
    "True", "try", "while", "with", "yield"
)

@Composable
fun LandscapeWorkspaceScreen(modifier: Modifier = Modifier) {
    val problems = listOf(
        ProblemListItem(title = "Tree Traversal", active = true, solved = true),
        ProblemListItem(title = "Two Sum", solved = true),
        ProblemListItem(title = "Binary Search"),
        ProblemListItem(title = "Longest Subsequence")
    )
    var sidebarMode by remember { mutableStateOf(SidebarMode.Problems) }
    var sidebarCollapsed by remember { mutableStateOf(false) }
    var workspaceInputMode by remember { mutableStateOf(WorkspaceInputMode.Keyboard) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarCollapsed) 84.dp else 268.dp,
        animationSpec = spring(),
        label = "sidebarWidth"
    )

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
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SidebarPane(
                problems = problems,
                selectedMode = sidebarMode,
                onModeSelected = { sidebarMode = it },
                collapsed = sidebarCollapsed,
                onToggleCollapsed = { sidebarCollapsed = !sidebarCollapsed },
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
            )
            CodePane(
                inputMode = workspaceInputMode,
                onInputModeChange = { workspaceInputMode = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ProblemPane(
                modifier = Modifier
                    .width(356.dp)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SidebarPane(
    problems: List<ProblemListItem>,
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
                    SidebarMode.Problems -> ProblemsSidebarContent(problems = problems)
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
private fun ProblemsSidebarContent(problems: List<ProblemListItem>) {
    var query by remember { mutableStateOf("") }
    var problemFilter by remember { mutableStateOf(ProblemFilter.All) }
    val filteredProblems = remember(problems, query, problemFilter) {
        problems.filter { problem ->
            val matchesQuery = query.isBlank() || problem.title.contains(query.trim(), ignoreCase = true)
            val matchesFilter = when (problemFilter) {
                ProblemFilter.All -> true
                ProblemFilter.Open -> !problem.solved
                ProblemFilter.Solved -> problem.solved
            }
            matchesQuery && matchesFilter
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
        Spacer(modifier = Modifier.height(12.dp))
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(filteredProblems) { problem ->
            ProblemRow(problem)
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
        Spacer(modifier = Modifier.height(12.dp))
        SuggestionLine("Give me a recursion hint")
        Spacer(modifier = Modifier.height(12.dp))
        SuggestionLine("Compare recursion vs stack")
    }

    Spacer(modifier = Modifier.weight(1f))
}

@Composable
private fun ColumnScope.SettingsSidebarContent() {
    CardBlock(title = "Settings", modifier = Modifier.fillMaxWidth()) {
        SettingLine("Problem imports", "Local JSON")
        Spacer(modifier = Modifier.height(12.dp))
        SettingLine("Runtime", "Embedded Python")
        Spacer(modifier = Modifier.height(12.dp))
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
private fun ProblemRow(problem: ProblemListItem) {
    val background = if (problem.active) CardBackground else Color.Transparent
    val borderColor = if (problem.active) AccentBlue.copy(alpha = 0.24f) else Color.Transparent

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = problem.title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (problem.solved) {
                    SolvedFlag()
                }
                if (!problem.active) {
                    Text(
                        text = "...",
                        color = TextMuted,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = DividerColor,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
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

@Composable
private fun ProblemPane(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(SupportTab.Problem) }
    var activeJobLabel by remember { mutableStateOf<String?>(null) }
    var revealedHintCount by remember { mutableStateOf(0) }
    var customTestCode by remember {
        mutableStateOf(
            """
            case_1 = {
                "input": [1, 2, 3, None, 4],
                "expected": [1, 2, 4, 3]
            }
            
            case_2 = {
                "input": [],
                "expected": []
            }
            """.trimIndent()
        )
    }
    var resultTitle by remember { mutableStateOf("Ready") }
    var resultMessage by remember { mutableStateOf("Run or submit your solution to see validation details here.") }
    var resultAccent by remember { mutableStateOf(TextSecondary) }
    val scope = rememberCoroutineScope()
    val hints = remember {
        listOf(
            "Remember the order of traversal methods.",
            "Think about recursion or using a stack.",
            "Pre-order visits root first. In-order centers the root. Post-order delays the root until both sides are explored."
        )
    }

    Surface(
        modifier = modifier,
        color = PaneBackground,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SupportTab.entries.forEach { tab ->
                            SupportTabChip(
                                label = tab.label,
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        ActionChip(
                            label = if (activeJobLabel == "Submit") "Submitting..." else "Submit",
                            active = activeJobLabel == "Submit",
                            emphasized = true,
                            enabled = activeJobLabel == null,
                            onClick = {
                                if (activeJobLabel != null) return@ActionChip
                                activeJobLabel = "Submit"
                                selectedTab = SupportTab.Results
                                resultTitle = "Submitting"
                                resultMessage = "Running the Python solution against the full local validation set."
                                resultAccent = AccentBlue
                                scope.launch {
                                    delay(1500)
                                    resultTitle = "Submission Accepted"
                                    resultMessage = "All local validation cases passed. This attempt is now saved as your latest submitted solution."
                                    resultAccent = AccentGreen
                                    activeJobLabel = null
                                }
                            }
                        )
                    }
                    HorizontalDivider(color = DividerColor)
                }
            }

            when (selectedTab) {
                SupportTab.Problem -> {
                    CardBlock(title = "Problem", modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(AccentGreen, AccentBlue)
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Tree Traversal",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Given a binary tree, return the nodes in pre-order, in-order, and post-order traversal.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    CardBlock(title = "Examples", modifier = Modifier.fillMaxWidth()) {
                        ExampleValueBlock(
                            label = "Input",
                            value = "root = [1, 2, 3, null, 4]",
                            labelColor = AccentRed,
                            valueColor = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        ExampleValueBlock(
                            label = "Output",
                            value = "Pre-order: [1, 2, 4, 3]\nIn-order: [2, 4, 1, 3]\nPost-order: [4, 2, 3, 1]",
                            labelColor = AccentGreen,
                            valueColor = TextPrimary
                        )
                    }

                    CardBlock(
                        title = "Hints",
                        modifier = Modifier.fillMaxWidth(),
                        trailing = {
                            PlainActionChip(
                                when {
                                    revealedHintCount == 0 -> "Show"
                                    revealedHintCount < hints.size -> "Next"
                                    else -> "Reset"
                                }
                            ) {
                                revealedHintCount = if (revealedHintCount < hints.size) {
                                    revealedHintCount + 1
                                } else {
                                    0
                                }
                            }
                        }
                    ) {
                        if (revealedHintCount > 0) {
                            hints.take(revealedHintCount).forEachIndexed { index, hint ->
                                HintLine(number = index + 1, text = hint)
                                if (index < revealedHintCount - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = DividerColor
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Hints are hidden until you reveal them.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                SupportTab.Custom -> {
                    CardBlock(
                        title = "Custom Tests",
                        modifier = Modifier.fillMaxWidth(),
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PlainActionChip("Add")
                                ActionChip(
                                    label = if (activeJobLabel == "Custom") "Checking..." else "Run Custom",
                                    active = activeJobLabel == "Custom",
                                    emphasized = false,
                                    enabled = activeJobLabel == null,
                                    onClick = {
                                        if (activeJobLabel != null) return@ActionChip
                                        activeJobLabel = "Custom"
                                        selectedTab = SupportTab.Results
                                        resultTitle = "Checking Custom Cases"
                                        resultMessage = "Evaluating the current Python draft against your local unit tests."
                                        resultAccent = AccentBlue
                                        scope.launch {
                                            delay(1100)
                                            resultTitle = "Custom Run Passed"
                                            resultMessage = "2 local cases passed with no runtime errors."
                                            resultAccent = AccentGreen
                                            activeJobLabel = null
                                        }
                                    }
                                )
                            }
                        }
                    ) {
                        Text(
                            text = "Edit your local cases here. This stays offline and never touches submit validation.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        EditableSupportTextBlock(
                            value = customTestCode,
                            onValueChange = { customTestCode = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    CardBlock(title = "Case Notes", modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Use this space for custom edge cases, expected outputs, and quick assertions you want to revisit.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                SupportTab.Results -> {
                    CardBlock(
                        title = "Results",
                        modifier = Modifier.fillMaxWidth(),
                        trailing = {
                            StatusBadge(
                                label = activeJobLabel ?: resultTitle,
                                color = resultAccent
                            )
                        }
                    ) {
                        Text(
                            text = resultTitle,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resultMessage,
                            color = if (activeJobLabel == null) TextPrimary else TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        ExampleValueBlock(
                            label = "Last Check",
                            value = if (activeJobLabel == "Submit") {
                                "Compiling local Python runtime\nExecuting hidden validation cases\nSaving submitted draft"
                            } else if (activeJobLabel == "Custom") {
                                "Loading custom inputs\nExecuting Python draft\nCollecting local output"
                            } else if (resultTitle == "Submission Accepted") {
                                "Status: Accepted\nCases: 24 / 24\nSaved: latest local submission"
                            } else if (resultTitle == "Custom Run Passed") {
                                "Status: Passed\nCases: 2 / 2\nScope: custom run"
                            } else {
                                "Status: Idle\nCases: --\nScope: none"
                            },
                            labelColor = AccentBlue,
                            valueColor = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CodePane(
    inputMode: WorkspaceInputMode,
    onInputModeChange: (WorkspaceInputMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var pythonCode by remember {
        mutableStateOf(
            """
            def preorder(root):
                if root is None:
                    return []

                result = [root.val]
                result += preorder(root.left)
                result += preorder(root.right)
                return result
            """.trimIndent()
        )
    }
    val strokes = remember { mutableStateListOf<SketchStroke>() }
    var activeStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var activeColor by remember { mutableStateOf(AccentBlue) }
    var sketchTool by remember { mutableStateOf(SketchTool.Pen) }
    var eraserSize by remember { mutableStateOf(EraserSize.Medium) }

    Surface(
        modifier = modifier,
        color = PaneBackground,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxSize()
                .padding(14.dp)
        ) {
            CardBlock(
                title = "Workspace",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        WorkspaceModeChip(
                            label = "Keyboard",
                            selected = inputMode == WorkspaceInputMode.Keyboard,
                            onClick = { onInputModeChange(WorkspaceInputMode.Keyboard) }
                        )
                        WorkspaceModeChip(
                            label = "Sketch",
                            selected = inputMode == WorkspaceInputMode.Sketch,
                            onClick = { onInputModeChange(WorkspaceInputMode.Sketch) }
                        )
                        if (inputMode == WorkspaceInputMode.Sketch) {
                            ToolChip(
                                label = "Draw",
                                selected = sketchTool == SketchTool.Pen,
                                onClick = { sketchTool = SketchTool.Pen }
                            )
                            ToolChip(
                                label = "Eraser",
                                selected = sketchTool == SketchTool.Eraser,
                                onClick = { sketchTool = SketchTool.Eraser }
                            )
                            if (sketchTool == SketchTool.Pen) {
                                ColorChip(
                                    color = AccentBlue,
                                    selected = activeColor == AccentBlue,
                                    onClick = { activeColor = AccentBlue }
                                )
                                ColorChip(
                                    color = AccentGreen,
                                    selected = activeColor == AccentGreen,
                                    onClick = { activeColor = AccentGreen }
                                )
                                ColorChip(
                                    color = AccentAmber,
                                    selected = activeColor == AccentAmber,
                                    onClick = { activeColor = AccentAmber }
                                )
                            } else {
                                EraserSize.entries.forEach { size ->
                                    EraserSizeChip(
                                        size = size,
                                        selected = eraserSize == size,
                                        onClick = { eraserSize = size }
                                    )
                                }
                            }
                            PlainActionChip("Clear") {
                                strokes.clear()
                                activeStrokePoints = emptyList()
                            }
                        }
                    }
                }
            ) {
                UnifiedWorkspaceSurface(
                    inputMode = inputMode,
                    pythonCode = pythonCode,
                    onPythonCodeChange = { pythonCode = it },
                    strokes = strokes,
                    activeStrokePoints = activeStrokePoints,
                    onActiveStrokePointsChange = { activeStrokePoints = it },
                    activeColor = activeColor,
                    sketchTool = sketchTool,
                    eraserSize = eraserSize,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CardBlock(
    title: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = CardBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column {
            if (title != null || trailing != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    trailing?.invoke()
                }
                HorizontalDivider(color = DividerColor)
            }

            Column(
                modifier = Modifier.padding(14.dp),
                content = content
            )
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String, labelColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = value,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ExampleValueBlock(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Surface(
            color = Color(0xFF1A2230),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                color = valueColor,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun EditableSupportTextBlock(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1A2230),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(AccentBlue),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun HintLine(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Hint $number:",
            color = TextPrimary,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = text,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SegmentedRunBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentLabel(
                label = "Run",
                selected = true,
                modifier = Modifier.weight(1f)
            )
            SegmentDivider()
            SegmentLabel(
                label = "Output",
                selected = false,
                modifier = Modifier.weight(1f)
            )
            SegmentDivider()
            SegmentLabel(
                label = "Alert",
                selected = false,
                modifier = Modifier.width(72.dp)
            )
        }
    }
}

@Composable
private fun SupportSegmentedBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentLabel(
                label = "Problem",
                selected = true,
                modifier = Modifier.weight(1f)
            )
            SegmentDivider()
            SegmentLabel(
                label = "Hints",
                selected = false,
                modifier = Modifier.weight(1f)
            )
            SegmentDivider()
            SegmentLabel(
                label = "Output",
                selected = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SupportTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.4f) else CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) AccentBlue else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun SegmentLabel(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    active: Boolean,
    emphasized: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        active -> AccentBlue.copy(alpha = 0.14f)
        emphasized -> AccentGreen.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val borderColor = when {
        active -> AccentBlue.copy(alpha = 0.45f)
        emphasized -> AccentGreen.copy(alpha = 0.38f)
        else -> CardBorder
    }
    val textColor = when {
        active -> AccentBlue
        emphasized -> AccentGreen
        else -> TextSecondary
    }

    Surface(
        color = background,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SegmentDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(DividerColor)
    )
}

@Composable
private fun SmallActionChip(label: String) {
    Surface(
        color = CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StackIcon()
            Text(
                text = label,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun StackIcon() {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .width(if (index == 1) 12.dp else 10.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(TextSecondary)
            )
            if (index < 2) {
                Spacer(modifier = Modifier.height(0.dp))
            }
        }
    }
}

@Composable
private fun PlainActionChip(
    label: String,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun WorkspaceModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.18f) else CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.55f) else CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RailModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.16f) else CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.5f) else CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) TextPrimary else TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private enum class ArrowDirection(val glyph: String) {
    Expand(">"),
    Collapse("<")
}

@Composable
private fun ArrowActionChip(
    direction: ArrowDirection,
    onClick: () -> Unit
) {
    Surface(
        color = CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = direction.glyph,
            color = TextSecondary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun SidebarMode.shortLabel(): String = when (this) {
    SidebarMode.Problems -> "List"
    SidebarMode.AskAi -> "AI"
    SidebarMode.Settings -> "Set"
}

@Composable
private fun CodeEditorMock(modifier: Modifier = Modifier) {
    // Retained for preview fallback; the real workspace uses EditablePythonSurface.
    EditablePythonSurface(
        code = "",
        onCodeChange = {},
        readOnly = true,
        modifier = modifier
    )
}

@Composable
private fun UnifiedWorkspaceSurface(
    inputMode: WorkspaceInputMode,
    pythonCode: String,
    onPythonCodeChange: (String) -> Unit,
    strokes: MutableList<SketchStroke>,
    activeStrokePoints: List<Offset>,
    onActiveStrokePointsChange: (List<Offset>) -> Unit,
    activeColor: Color,
    sketchTool: SketchTool,
    eraserSize: EraserSize,
    modifier: Modifier = Modifier
) {
    var activeStrokeColor by remember { mutableStateOf(activeColor) }
    var eraserCursor by remember { mutableStateOf<Offset?>(null) }
    val activeStroke = remember(activeStrokePoints, activeColor) {
        if (activeStrokePoints.isEmpty()) null else SketchStroke(activeStrokePoints, activeStrokeColor)
    }
    val drawModifier = if (inputMode == WorkspaceInputMode.Sketch) {
        Modifier.pointerInput(inputMode, sketchTool, eraserSize, activeColor) {
            var currentStroke = mutableListOf<Offset>()
            detectDragGestures(
                onDragStart = { offset ->
                    if (sketchTool == SketchTool.Pen) {
                        activeStrokeColor = activeColor
                        currentStroke = mutableListOf(offset)
                        onActiveStrokePointsChange(currentStroke.toList())
                        eraserCursor = null
                    } else {
                        eraserCursor = offset
                        eraseAt(strokes, offset, eraserSize.radius)
                    }
                },
                onDragEnd = {
                    if (sketchTool == SketchTool.Pen && currentStroke.size > 1) {
                        strokes.add(SketchStroke(currentStroke.toList(), activeStrokeColor))
                    }
                    currentStroke = mutableListOf()
                    eraserCursor = null
                    onActiveStrokePointsChange(emptyList())
                },
                onDragCancel = {
                    currentStroke = mutableListOf()
                    eraserCursor = null
                    onActiveStrokePointsChange(emptyList())
                }
            ) { change, _ ->
                change.consume()
                if (sketchTool == SketchTool.Pen) {
                    appendInterpolatedPoints(currentStroke, change.position)
                    onActiveStrokePointsChange(currentStroke.toList())
                } else {
                    eraserCursor = change.position
                    eraseAt(strokes, change.position, eraserSize.radius)
                }
            }
        }
    } else {
        Modifier
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        ) {
            EditablePythonSurface(
                code = pythonCode,
                onCodeChange = onPythonCodeChange,
                readOnly = inputMode == WorkspaceInputMode.Sketch,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(drawModifier)
            ) {
                val allStrokes = strokes + listOfNotNull(activeStroke)
                allStrokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        for (index in 0 until stroke.points.lastIndex) {
                            drawLine(
                                color = stroke.color.copy(alpha = 0.95f),
                                start = stroke.points[index],
                                end = stroke.points[index + 1],
                                strokeWidth = 6f
                            )
                        }
                    }
                }

                if (inputMode == WorkspaceInputMode.Sketch &&
                    sketchTool == SketchTool.Eraser &&
                    eraserCursor != null
                ) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = eraserSize.radius,
                        center = eraserCursor!!
                    )
                    drawCircle(
                        color = TextPrimary.copy(alpha = 0.6f),
                        radius = eraserSize.radius,
                        center = eraserCursor!!,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
            }

            Surface(
                color = CardBackground.copy(alpha = 0.84f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Text(
                    text = when {
                        inputMode == WorkspaceInputMode.Keyboard -> "Keyboard Input"
                        sketchTool == SketchTool.Eraser -> "Eraser ${eraserSize.label}"
                        else -> "Draw Input"
                    },
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun eraseAt(strokes: MutableList<SketchStroke>, position: Offset, radius: Float) {
    val radiusSquared = radius * radius
    val updatedStrokes = buildList {
        strokes.forEach { stroke ->
            val survivingSegments = mutableListOf<MutableList<Offset>>()
            var currentSegment = mutableListOf<Offset>()

            stroke.points.forEach { point ->
                val dx = point.x - position.x
                val dy = point.y - position.y
                val insideEraseRadius = (dx * dx) + (dy * dy) <= radiusSquared

                if (insideEraseRadius) {
                    if (currentSegment.size > 1) {
                        survivingSegments.add(currentSegment)
                    }
                    currentSegment = mutableListOf()
                } else {
                    currentSegment.add(point)
                }
            }

            if (currentSegment.size > 1) {
                survivingSegments.add(currentSegment)
            }

            survivingSegments.forEach { segment ->
                add(
                    SketchStroke(
                        points = segment.toList(),
                        color = stroke.color
                    )
                )
            }
        }
    }
    strokes.clear()
    strokes.addAll(updatedStrokes)
}

private fun appendInterpolatedPoints(
    stroke: MutableList<Offset>,
    nextPoint: Offset,
    spacing: Float = 4f
) {
    if (stroke.isEmpty()) {
        stroke.add(nextPoint)
        return
    }

    val lastPoint = stroke.last()
    val distance = hypot(nextPoint.x - lastPoint.x, nextPoint.y - lastPoint.y)
    if (distance <= spacing) {
        stroke.add(nextPoint)
        return
    }

    val steps = ceil(distance / spacing).toInt()
    for (step in 1..steps) {
        val t = step.toFloat() / steps.toFloat()
        stroke.add(
            Offset(
                x = lastPoint.x + (nextPoint.x - lastPoint.x) * t,
                y = lastPoint.y + (nextPoint.y - lastPoint.y) * t
            )
        )
    }
}

private object PythonSyntaxVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = highlightPython(text.text),
            offsetMapping = OffsetMapping.Identity
        )
    }
}

private fun highlightPython(source: String): AnnotatedString {
    val commentRegex = Regex("#.*$")
    val stringRegex = Regex("'([^'\\\\]|\\\\.)*'|\"([^\"\\\\]|\\\\.)*\"")
    val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
    val decoratorRegex = Regex("(?m)^\\s*@\\w+")
    val definitionRegex = Regex("\\b(def|class)\\s+([A-Za-z_][A-Za-z0-9_]*)")
    val functionCallRegex = Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*(?=\\()")
    val keywordRegex = Regex(
        "\\b(" + PythonKeywords.joinToString("|") { Regex.escape(it) } + ")\\b"
    )
    val commentRanges = commentRegex.findAll(source).map { it.range }.toList()
    val stringRanges = stringRegex.findAll(source).map { it.range }.toList()
    val decoratorRanges = decoratorRegex.findAll(source).map { it.range }.toList()
    val protectedRanges = commentRanges + stringRanges + decoratorRanges
    val definitionNameRanges = definitionRegex.findAll(source)
        .mapNotNull { it.groups[2]?.range }
        .toList()

    return buildAnnotatedString {
        append(source)

        commentRanges.forEach { range ->
            addStyle(SpanStyle(color = PythonCommentColor), range.first, range.last + 1)
        }
        stringRanges.forEach { range ->
            addStyle(SpanStyle(color = PythonStringColor), range.first, range.last + 1)
        }
        numberRegex.findAll(source).forEach { match ->
            addStyle(SpanStyle(color = PythonNumberColor), match.range.first, match.range.last + 1)
        }
        decoratorRanges.forEach { range ->
            addStyle(SpanStyle(color = PythonDecoratorColor), range.first, range.last + 1)
        }
        keywordRegex.findAll(source).forEach { match ->
            addStyle(SpanStyle(color = PythonKeywordColor), match.range.first, match.range.last + 1)
        }
        functionCallRegex.findAll(source).forEach { match ->
            val nameGroup = match.groups[1] ?: return@forEach
            val name = nameGroup.value
            val nameRange = nameGroup.range

            if (name in PythonKeywords) return@forEach
            if (protectedRanges.any { nameRange.first >= it.first && nameRange.last <= it.last }) return@forEach
            if (definitionNameRanges.any { nameRange.first == it.first && nameRange.last == it.last }) return@forEach

            addStyle(
                SpanStyle(color = PythonFunctionColor, fontWeight = FontWeight.Medium),
                nameRange.first,
                nameRange.last + 1
            )
        }
        definitionRegex.findAll(source).forEach { match ->
            val keywordRange = match.groups[1]?.range
            val nameRange = match.groups[2]?.range
            if (keywordRange != null) {
                addStyle(SpanStyle(color = PythonKeywordColor), keywordRange.first, keywordRange.last + 1)
            }
            if (nameRange != null) {
                addStyle(
                    SpanStyle(color = PythonFunctionColor, fontWeight = FontWeight.SemiBold),
                    nameRange.first,
                    nameRange.last + 1
                )
            }
        }
    }
}

@Composable
private fun EraserSizeChip(
    size: EraserSize,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.18f) else CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.55f) else CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = size.label,
            color = if (selected) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun EditablePythonSurface(
    code: String,
    onCodeChange: (String) -> Unit,
    readOnly: Boolean,
    modifier: Modifier = Modifier
) {
    val lines = remember(code) { (code.lines().size.coerceAtLeast(1)) }
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier,
        color = Color(0xFF1A2230),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(24.dp)
            ) {
                for (lineNumber in 1..lines) {
                    Text(
                        text = lineNumber.toString(),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                readOnly = readOnly,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(AccentBlue),
                visualTransformation = PythonSyntaxVisualTransformation,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (code.isEmpty()) {
                            Text(
                                text = "Write your Python solution here...",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun ToolChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.18f) else CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.55f) else CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = CardBackgroundAlt,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (selected) TextPrimary else CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun SuggestionLine(text: String) {
    Text(
        text = text,
        color = TextPrimary,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun SettingLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = value,
            color = AccentBlue,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun LandscapeWorkspacePreview() {
    StandaloneLeetCodeTheme {
        LandscapeWorkspaceScreen()
    }
}
