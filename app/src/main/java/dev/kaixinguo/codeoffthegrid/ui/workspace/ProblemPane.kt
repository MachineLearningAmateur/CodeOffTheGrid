package dev.kaixinguo.codeoffthegrid.ui.workspace

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.kaixinguo.codeoffthegrid.data.LocalPythonExecutionService
import dev.kaixinguo.codeoffthegrid.data.ProblemInputNormalizer
import dev.kaixinguo.codeoffthegrid.data.ProblemSubmissionSuiteFactory
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentBlue
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentGreen
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentRed
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBackground
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBorder
import dev.kaixinguo.codeoffthegrid.ui.theme.DividerColor
import dev.kaixinguo.codeoffthegrid.ui.theme.PaneBackground
import dev.kaixinguo.codeoffthegrid.ui.theme.TextMuted
import dev.kaixinguo.codeoffthegrid.ui.theme.TextPrimary
import dev.kaixinguo.codeoffthegrid.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
internal fun ProblemPane(
    problem: ProblemListItem,
    draftCode: String,
    selectedTab: SupportTab,
    onSelectedTabChange: (SupportTab) -> Unit,
    onSolvedChange: (Boolean) -> Unit,
    customTestSuite: ProblemTestSuite,
    onCustomTestSuiteChange: (ProblemTestSuite) -> Unit,
    onExecutionResultChange: (ProblemExecutionResult) -> Unit,
    localPythonExecutionService: LocalPythonExecutionService,
    previewMode: Boolean = false,
    previewComments: String? = null,
    modifier: Modifier = Modifier
) {
    var revealedHintCount by remember(problem.id) { mutableStateOf(0) }
    var commentsVisible by remember(problem.id, previewComments) { mutableStateOf(false) }
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
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val hints = remember(problem.id, problem.hints) { problem.hints }
    val commentsText = remember(problem.id, previewComments, problem.summary, problem.statementMarkdown) {
        val explicitPreviewComments = previewComments?.trim().orEmpty()
        if (explicitPreviewComments.isNotBlank()) {
            explicitPreviewComments
        } else {
            problem.summary
                .trim()
                .takeIf { it.isNotBlank() && it != deriveSummaryFallback(problem.statementMarkdown, problem.title) }
                .orEmpty()
        }
    }
    val hasComments = !previewMode && commentsText.isNotBlank()
    val normalizedExampleInput = remember(
        problem.id,
        problem.exampleInput,
        problem.starterCode,
        problem.executionPipeline
    ) {
        ProblemInputNormalizer.normalizeExampleInput(problem)
    }
    val statementMarkdown = remember(
        problem.id,
        problem.statementMarkdown,
        problem.summary,
        previewMode,
        normalizedExampleInput,
        problem.exampleOutput
    ) {
        val baseStatement = if (previewMode) {
            problem.statementMarkdown
        } else {
            problem.statementMarkdown.ifBlank { problem.summary }
        }
        if (
            Regex("""(?im)^\*\*Example(?:\s+\d+)?:\*\*$""").containsMatchIn(baseStatement) ||
            (normalizedExampleInput.isBlank() && problem.exampleOutput.isBlank())
        ) {
            baseStatement
        } else {
            buildString {
                append(baseStatement)
                append("\n\n**Example 1:**\n\n```text\n")
                if (normalizedExampleInput.isNotBlank()) {
                    append("Input: ")
                    append(normalizedExampleInput)
                    append("\n\n")
                }
                if (problem.exampleOutput.isNotBlank()) {
                    append("Output: ")
                    append(problem.exampleOutput)
                    append('\n')
                }
                append("```")
            }
        }
    }
    val contentScrollState = rememberScrollState()
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
    val submissionTestSuite = remember(
        problem.id,
        problem.submissionTestSuite,
        problem.statementMarkdown,
        problem.exampleInput,
        problem.exampleOutput,
        problem.starterCode,
        problem.executionPipeline
    ) {
        ProblemInputNormalizer.normalizeSubmissionTestSuite(
            problem = problem,
            testSuite = problem.submissionTestSuite.takeIf {
                it.cases.isNotEmpty()
            } ?: ProblemSubmissionSuiteFactory.build(problem)
        )
    }
    val enabledSubmissionCases = submissionTestSuite.cases.count { it.enabled }
    val hasStructuredCases = customTestSuite.cases.isNotEmpty()
    val hasRunnableExample = problem.exampleInput.isNotBlank() || problem.exampleOutput.isNotBlank()
    val canToggleSolved = !previewMode && (problem.solved || (
        executionResult.target == ExecutionTarget.LocalSubmission &&
            executionResult.status == ExecutionStatus.Passed
        ))
    val copyResultBlock: (String, String) -> Unit = { label, value ->
        clipboardManager.setText(AnnotatedString(value))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    androidx.compose.runtime.LaunchedEffect(problem.id, executionResult) {
        onExecutionResultChange(executionResult)
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
                        if (previewMode) {
                            Text(
                                text = "Preview",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            StatusBadge(
                                label = "Read Only",
                                color = AccentBlue
                            )
                        } else {
                            SupportTab.entries.forEach { tab ->
                                SupportTabChip(
                                    label = tab.label,
                                    selected = selectedTab == tab,
                                    onClick = { onSelectedTabChange(tab) }
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
                                    onSelectedTabChange(SupportTab.Results)
                                    if (enabledSubmissionCases == 0) {
                                        executionResult = ProblemExecutionResult(
                                            target = ExecutionTarget.LocalSubmission,
                                            status = ExecutionStatus.Error,
                                            title = "Local Submission Error",
                                            summary = "This problem does not have a bundled local submission suite yet."
                                        )
                                        return@ActionChip
                                    }
                                    executionResult = ProblemExecutionResult(
                                        target = ExecutionTarget.LocalSubmission,
                                        status = ExecutionStatus.Running,
                                        title = "Running Local Submission",
                                        summary = "Executing the current Python draft against the built-in local submission suite."
                                    )
                                    scope.launch {
                                        executionResult = localPythonExecutionService.runLocalSubmission(
                                            executionPipeline = problem.executionPipeline,
                                            testSuite = submissionTestSuite,
                                            draftCode = draftCode
                                        )
                                    }
                                }
                            )
                        }
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
                when (if (previewMode) SupportTab.Problem else selectedTab) {
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

                        if (hasComments) {
                            CardBlock(
                                title = "Comments",
                                modifier = Modifier.fillMaxWidth(),
                                trailing = {
                                    PlainActionChip(
                                        label = if (commentsVisible) "Hide" else "Show"
                                    ) {
                                        commentsVisible = !commentsVisible
                                    }
                                }
                            ) {
                                if (commentsVisible) {
                                    Text(
                                        text = commentsText,
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        text = "Comments are hidden until you reveal them.",
                                        color = TextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        CardBlock(
                            title = "Hints",
                            modifier = Modifier.fillMaxWidth(),
                            trailing = if (hints.isNotEmpty()) {
                                {
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
                            } else {
                                null
                            }
                        ) {
                            if (hints.isEmpty()) {
                                Text(
                                    text = "No hints added yet.",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else if (revealedHintCount > 0) {
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
                                                        normalizedExampleInput
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
                                            onSelectedTabChange(SupportTab.Results)
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
                                                    executionPipeline = problem.executionPipeline,
                                                    testSuite = customTestSuite,
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
                                                            stdin = normalizedExampleInput,
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
                            if (canToggleSolved) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (problem.solved) {
                                            "This problem is currently marked solved."
                                        } else {
                                            "Passing local submission does not auto-mark this problem."
                                        },
                                        color = TextMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PlainActionChip(
                                        label = if (problem.solved) "Mark Open" else "Mark Solved",
                                        accentColor = if (problem.solved) AccentBlue else AccentGreen
                                    ) {
                                        onSolvedChange(!problem.solved)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap any result block to copy it.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            ExampleValueBlock(
                                label = "Last Check",
                                value = formatExecutionResult(executionResult),
                                labelColor = AccentBlue,
                                valueColor = TextPrimary,
                                onClick = {
                                    copyResultBlock(
                                        "Last check",
                                        formatExecutionResult(executionResult)
                                    )
                                }
                            )
                            if (executionResult.stdout.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                ExampleValueBlock(
                                    label = "Stdout",
                                    value = executionResult.stdout,
                                    labelColor = AccentGreen,
                                    valueColor = TextPrimary,
                                    onClick = { copyResultBlock("Stdout", executionResult.stdout) }
                                )
                            }
                            if (executionResult.stderr.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                ExampleValueBlock(
                                    label = "Errors",
                                    value = executionResult.stderr,
                                    labelColor = AccentRed,
                                    valueColor = TextPrimary,
                                    onClick = { copyResultBlock("Errors", executionResult.stderr) }
                                )
                            }
                            if (executionResult.cases.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                executionResult.cases.forEachIndexed { index, caseResult ->
                                    val caseResultText = buildCaseResultText(caseResult)
                                    ExampleValueBlock(
                                        label = caseResult.label,
                                        value = caseResultText,
                                        labelColor = when (caseResult.status) {
                                            TestCaseStatus.Passed -> AccentGreen
                                            TestCaseStatus.Failed, TestCaseStatus.Error -> AccentRed
                                            else -> AccentBlue
                                        },
                                        valueColor = TextPrimary,
                                        onClick = { copyResultBlock(caseResult.label, caseResultText) }
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
        if (
            result.input.isNotBlank() &&
            (result.status == TestCaseStatus.Failed || result.status == TestCaseStatus.Error)
        ) {
            append('\n')
            append("Input: ")
            append(result.input)
        }
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
        if (result.stdout.isNotBlank()) {
            append('\n')
            append("Stdout: ")
            append(result.stdout)
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

private fun deriveSummaryFallback(statementMarkdown: String, fallbackTitle: String): String {
    val paragraph = statementMarkdown
        .lineSequence()
        .map { it.trim() }
        .dropWhile { it.isBlank() }
        .takeWhile { it.isNotBlank() }
        .joinToString(" ")
        .replace("**", "")
        .replace("`", "")
        .replace(Regex("""\s+"""), " ")
        .trim()

    return when {
        paragraph.isBlank() -> fallbackTitle.trim()
        paragraph.length <= 220 -> paragraph
        else -> paragraph.take(217).trimEnd() + "..."
    }
}

