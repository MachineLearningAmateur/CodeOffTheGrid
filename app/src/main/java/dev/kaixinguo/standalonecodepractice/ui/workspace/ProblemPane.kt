package dev.kaixinguo.standalonecodepractice.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.kaixinguo.standalonecodepractice.data.LocalPythonExecutionService
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentBlue
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentGreen
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentRed
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBorder
import dev.kaixinguo.standalonecodepractice.ui.theme.DividerColor
import dev.kaixinguo.standalonecodepractice.ui.theme.PaneBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.TextMuted
import dev.kaixinguo.standalonecodepractice.ui.theme.TextPrimary
import dev.kaixinguo.standalonecodepractice.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
internal fun ProblemPane(
    problem: ProblemListItem,
    draftCode: String,
    customTestSuite: ProblemTestSuite,
    onCustomTestSuiteChange: (ProblemTestSuite) -> Unit,
    localPythonExecutionService: LocalPythonExecutionService,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SupportTab.Problem) }
    var revealedHintCount by remember(problem.id) { mutableStateOf(0) }
    var executionResult by remember(problem.id) {
        mutableStateOf(
            ProblemExecutionResult(
                target = ExecutionTarget.Custom,
                status = ExecutionStatus.Idle,
                title = "Ready",
                summary = "Run local checks to see validation details here."
            )
        )
    }
    val scope = rememberCoroutineScope()
    val hints = remember(problem.id) { problem.hints }
    val statementMarkdown = remember(problem.id) {
        problem.statementMarkdown.ifBlank { problem.summary }
    }
    val contentScrollState = rememberScrollState()
    val hasExamples = problem.exampleInput.isNotBlank() || problem.exampleOutput.isNotBlank()
    val activeJobTarget = executionResult
        .takeIf { it.status == ExecutionStatus.Running }
        ?.target
    val resultAccent = when (executionResult.status) {
        ExecutionStatus.Passed -> AccentGreen
        ExecutionStatus.Failed, ExecutionStatus.Error -> AccentRed
        ExecutionStatus.Running -> AccentBlue
        ExecutionStatus.Idle -> TextSecondary
    }
    val enabledCustomCases = customTestSuite.cases.count { it.enabled }
    val hasStructuredCases = customTestSuite.cases.isNotEmpty()
    val hasRunnableExample = problem.exampleInput.isNotBlank() || problem.exampleOutput.isNotBlank()

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
                            label = if (activeJobTarget == ExecutionTarget.LocalSubmission) "Checking..." else "Submit",
                            active = activeJobTarget == ExecutionTarget.LocalSubmission,
                            emphasized = true,
                            enabled = activeJobTarget == null,
                            onClick = {
                                if (activeJobTarget != null) return@ActionChip
                                selectedTab = SupportTab.Results
                                if (enabledCustomCases == 0) {
                                    executionResult = ProblemExecutionResult(
                                        target = ExecutionTarget.LocalSubmission,
                                        status = ExecutionStatus.Error,
                                        title = "Local Submission Error",
                                        summary = "Add at least one enabled custom case before running local submission."
                                    )
                                    return@ActionChip
                                }
                                executionResult = ProblemExecutionResult(
                                    target = ExecutionTarget.LocalSubmission,
                                    status = ExecutionStatus.Running,
                                    title = "Running Local Submission",
                                    summary = "Executing the current Python draft against your saved local submission suite."
                                )
                                scope.launch {
                                    executionResult = localPythonExecutionService.runLocalSubmission(
                                        customTestSuite = customTestSuite,
                                        draftCode = draftCode
                                    )
                                }
                            }
                        )
                    }
                    HorizontalDivider(color = DividerColor)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .verticalScroll(contentScrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                                Spacer(modifier = Modifier.height(0.dp))
                                Text(
                                    text = problem.title,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                )
                                if (problem.difficulty.isNotBlank()) {
                                    StatusBadge(
                                        label = problem.difficulty,
                                        color = when (problem.difficulty.lowercase()) {
                                            "easy" -> AccentGreen
                                            "hard" -> AccentRed
                                            else -> AccentBlue
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            MarkdownStatementText(
                                markdown = statementMarkdown.ifBlank { problem.summary },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (hasExamples) {
                            CardBlock(title = "Examples", modifier = Modifier.fillMaxWidth()) {
                                if (problem.exampleInput.isNotBlank()) {
                                    ExampleValueBlock(
                                        label = "Input",
                                        value = problem.exampleInput,
                                        labelColor = AccentRed,
                                        valueColor = TextSecondary
                                    )
                                }
                                if (problem.exampleInput.isNotBlank() && problem.exampleOutput.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                                if (problem.exampleOutput.isNotBlank()) {
                                    ExampleValueBlock(
                                        label = "Output",
                                        value = problem.exampleOutput,
                                        labelColor = AccentGreen,
                                        valueColor = TextPrimary
                                    )
                                }
                            }
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
                                    PlainActionChip("Add") {
                                        val nextIndex = customTestSuite.cases.size + 1
                                        onCustomTestSuiteChange(
                                            customTestSuite.copy(
                                                cases = customTestSuite.cases + ProblemTestCase(
                                                    label = "Case $nextIndex",
                                                    stdin = if (customTestSuite.cases.isEmpty()) {
                                                        problem.exampleInput
                                                    } else {
                                                        ""
                                                    },
                                                    expectedOutput = if (customTestSuite.cases.isEmpty()) {
                                                        problem.exampleOutput
                                                    } else {
                                                        ""
                                                    }
                                                )
                                            )
                                        )
                                    }
                                    ActionChip(
                                        label = if (activeJobTarget == ExecutionTarget.Custom) "Checking..." else "Run Custom",
                                        active = activeJobTarget == ExecutionTarget.Custom,
                                        emphasized = false,
                                        enabled = activeJobTarget == null,
                                        onClick = {
                                            if (activeJobTarget != null) return@ActionChip
                                            selectedTab = SupportTab.Results
                                            if (enabledCustomCases == 0) {
                                                executionResult = ProblemExecutionResult(
                                                    target = ExecutionTarget.Custom,
                                                    status = ExecutionStatus.Error,
                                                    title = "Custom Run Error",
                                                    summary = "Add at least one enabled custom case before running local checks."
                                                )
                                                return@ActionChip
                                            }
                                            executionResult = ProblemExecutionResult(
                                                target = ExecutionTarget.Custom,
                                                status = ExecutionStatus.Running,
                                                title = "Checking Custom Cases",
                                                summary = "Evaluating the current Python draft against your local custom cases."
                                            )
                                            scope.launch {
                                                executionResult = localPythonExecutionService.runCustomSuite(
                                                    customTestSuite = customTestSuite,
                                                    draftCode = draftCode
                                                )
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Use JSON input to call the current Solution method directly. Use expr: for a Python expression, or script: for a snippet that assigns the final value to result.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$enabledCustomCases enabled of ${customTestSuite.cases.size} saved custom cases.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (hasStructuredCases) {
                                customTestSuite.cases.forEachIndexed { index, testCase ->
                                    CustomCaseEditor(
                                        testCase = testCase,
                                        onCaseChange = { updatedCase ->
                                            val updatedCases = customTestSuite.cases.toMutableList()
                                            updatedCases[index] = updatedCase
                                            onCustomTestSuiteChange(customTestSuite.copy(cases = updatedCases))
                                        },
                                        onRemove = {
                                            val updatedCases = customTestSuite.cases.toMutableList().apply {
                                                removeAt(index)
                                            }
                                            onCustomTestSuiteChange(customTestSuite.copy(cases = updatedCases))
                                        }
                                    )
                                    if (index < customTestSuite.cases.lastIndex) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "No local cases yet. Add a case to start building a reusable offline suite.",
                                        color = TextMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (hasRunnableExample) {
                                        PlainActionChip("Use Problem Example") {
                                            onCustomTestSuiteChange(
                                                customTestSuite.copy(
                                                    cases = listOf(
                                                        ProblemTestCase(
                                                            label = "Case 1",
                                                            stdin = problem.exampleInput,
                                                            expectedOutput = problem.exampleOutput
                                                        )
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        CardBlock(title = "Case Notes", modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Use this space for scratch notes, alternate inputs, or Python snippets you want to keep nearby.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            EditableSupportTextBlock(
                                value = customTestSuite.draft,
                                onValueChange = { updatedDraft ->
                                    onCustomTestSuiteChange(customTestSuite.copy(draft = updatedDraft))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    SupportTab.Results -> {
                        CardBlock(
                            title = "Results",
                            modifier = Modifier.fillMaxWidth(),
                            trailing = {
                                StatusBadge(
                                    label = executionResult.title,
                                    color = resultAccent
                                )
                            }
                        ) {
                            Text(
                                text = executionResult.title,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = executionResult.summary,
                                color = if (executionResult.status == ExecutionStatus.Running) TextSecondary else TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            ExampleValueBlock(
                                label = "Last Check",
                                value = formatExecutionResult(executionResult),
                                labelColor = AccentBlue,
                                valueColor = TextPrimary
                            )
                            if (executionResult.stdout.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                ExampleValueBlock(
                                    label = "Stdout",
                                    value = executionResult.stdout,
                                    labelColor = AccentGreen,
                                    valueColor = TextPrimary
                                )
                            }
                            if (executionResult.stderr.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                ExampleValueBlock(
                                    label = "Errors",
                                    value = executionResult.stderr,
                                    labelColor = AccentRed,
                                    valueColor = TextPrimary
                                )
                            }
                            if (executionResult.cases.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                executionResult.cases.forEachIndexed { index, caseResult ->
                                    ExampleValueBlock(
                                        label = caseResult.label,
                                        value = buildCaseResultText(caseResult),
                                        labelColor = when (caseResult.status) {
                                            TestCaseStatus.Passed -> AccentGreen
                                            TestCaseStatus.Failed, TestCaseStatus.Error -> AccentRed
                                            else -> AccentBlue
                                        },
                                        valueColor = TextPrimary
                                    )
                                    if (index < executionResult.cases.lastIndex) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatExecutionResult(result: ProblemExecutionResult): String {
    val targetLabel = when (result.target) {
        ExecutionTarget.Custom -> "custom run"
        ExecutionTarget.LocalSubmission -> "local submission"
    }
    val caseSummary = when {
        result.cases.isEmpty() -> "--"
        else -> {
            val passed = result.cases.count { it.status == TestCaseStatus.Passed }
            "$passed / ${result.cases.size}"
        }
    }
    return buildString {
        append("Status: ")
        append(result.status.name.lowercase().replaceFirstChar(Char::uppercase))
        append('\n')
        append("Cases: ")
        append(caseSummary)
        append('\n')
        append("Scope: ")
        append(targetLabel)
    }
}

private fun buildCaseResultText(result: ProblemTestCaseResult): String {
    return buildString {
        append("Status: ")
        append(result.status.name.lowercase().replaceFirstChar(Char::uppercase))
        if (result.expectedOutput.isNotBlank()) {
            append('\n')
            append("Expected: ")
            append(result.expectedOutput)
        }
        if (result.actualOutput.isNotBlank()) {
            append('\n')
            append("Actual: ")
            append(result.actualOutput)
        }
        if (result.errorOutput.isNotBlank()) {
            append('\n')
            append("Error: ")
            append(result.errorOutput)
        }
        result.durationMillis?.let { duration ->
            append('\n')
            append("Duration: ")
            append(duration)
            append(" ms")
        }
    }
}

