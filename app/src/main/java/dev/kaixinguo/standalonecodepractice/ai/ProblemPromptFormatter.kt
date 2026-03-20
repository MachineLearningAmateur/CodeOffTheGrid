package dev.kaixinguo.standalonecodepractice.ai

import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemListItem
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemExecutionResult
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestCase
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestCaseResult
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestComparisonMode
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestSuite
import dev.kaixinguo.standalonecodepractice.ui.workspace.TestCaseStatus

internal object ProblemPromptFormatter {
    fun format(
        problem: ProblemListItem,
        customTestSuite: ProblemTestSuite? = null,
        submissionTestSuite: ProblemTestSuite? = null,
        customExecutionResult: ProblemExecutionResult? = null,
        submissionExecutionResult: ProblemExecutionResult? = null
    ): String {
        val statement = truncateBlock(
            value = problem.statementMarkdown.trim().ifBlank { problem.summary.trim() },
            maxChars = MAX_STATEMENT_CHARS
        )

        val context = buildString {
            appendLine("Title: ${problem.title}")
            if (problem.difficulty.isNotBlank()) {
                appendLine("Difficulty: ${problem.difficulty}")
            }
            appendLine()
            appendLine("Problem statement:")
            appendLine(statement.ifBlank { "No problem statement provided." })
            if (problem.exampleInput.isNotBlank() || problem.exampleOutput.isNotBlank()) {
                appendLine()
                appendLine("Example:")
                if (problem.exampleInput.isNotBlank()) {
                    appendLine("Input:")
                    appendLine(truncateBlock(problem.exampleInput.trim(), MAX_EXAMPLE_CHARS))
                }
                if (problem.exampleOutput.isNotBlank()) {
                    appendLine("Output:")
                    appendLine(truncateBlock(problem.exampleOutput.trim(), MAX_EXAMPLE_CHARS))
                }
            }
            appendTestSuiteSection(
                title = "Current custom test cases",
                testSuite = customTestSuite
            )
            appendTestSuiteSection(
                title = "Current local submission test cases",
                testSuite = submissionTestSuite
            )
            appendExecutionResultSection(
                title = "Latest custom run result",
                result = customExecutionResult
            )
            appendExecutionResultSection(
                title = "Latest local submission result",
                result = submissionExecutionResult
            )
        }.trim()

        return truncateBlock(context, MAX_CONTEXT_CHARS)
    }

    private fun StringBuilder.appendTestSuiteSection(
        title: String,
        testSuite: ProblemTestSuite?
    ) {
        appendLine()
        appendLine("$title:")
        val cases = testSuite?.cases.orEmpty()
        if (cases.isEmpty()) {
            appendLine("No saved cases.")
            return
        }

        val enabledCases = cases.filter { it.enabled }
        appendLine("Saved cases: ${cases.size} total, ${enabledCases.size} enabled.")

        val displayedCases = enabledCases
            .ifEmpty { cases }
            .take(MAX_CASES_PER_TEST_SUITE_SECTION)

        displayedCases.forEachIndexed { index, testCase ->
            appendTestCase(index = index, testCase = testCase)
        }
        val omittedCount = cases.size - displayedCases.size
        if (omittedCount > 0) {
            appendLine("... $omittedCount more saved cases omitted.")
        }
    }

    private fun StringBuilder.appendTestCase(
        index: Int,
        testCase: ProblemTestCase
    ) {
        appendLine("${index + 1}. ${testCase.label} (${if (testCase.enabled) "enabled" else "disabled"})")
        appendLine("Input:")
        appendLine(truncateBlock(testCase.stdin.ifBlank { "(empty)" }, MAX_TEST_CASE_FIELD_CHARS))
        appendLine("Expected output:")
        appendLine(truncateBlock(testCase.expectedOutput.ifBlank { "(empty)" }, MAX_TEST_CASE_FIELD_CHARS))
        if (testCase.comparisonMode != ProblemTestComparisonMode.Exact) {
            appendLine("Comparison mode: ${testCase.comparisonMode.storageValue}")
        }
        if (testCase.acceptableOutputs.isNotEmpty()) {
            appendLine("Acceptable outputs:")
            testCase.acceptableOutputs.forEach { output ->
                appendLine("- ${truncateBlock(output, MAX_TEST_CASE_FIELD_CHARS)}")
            }
        }
    }

    private fun StringBuilder.appendExecutionResultSection(
        title: String,
        result: ProblemExecutionResult?
    ) {
        appendLine()
        appendLine("$title:")
        if (result == null || isUnrecorded(result)) {
            appendLine("No recorded run yet.")
            return
        }

        appendLine("Status: ${result.status.name.lowercase().replaceFirstChar(Char::uppercase)}")
        appendLine("Title: ${result.title}")
        appendLine("Summary: ${truncateBlock(result.summary, MAX_SUMMARY_CHARS)}")
        if (result.stdout.isNotBlank()) {
            appendLine("Stdout:")
            appendLine(truncateBlock(result.stdout, MAX_STDIO_CHARS))
        }
        if (result.stderr.isNotBlank()) {
            appendLine("Stderr:")
            appendLine(truncateBlock(result.stderr, MAX_STDIO_CHARS))
        }
        if (result.cases.isEmpty()) {
            appendLine("Per-case results: none")
            return
        }

        val prioritizedCases = result.cases
            .filter { it.status == TestCaseStatus.Failed || it.status == TestCaseStatus.Error }
            .ifEmpty { result.cases.take(1) }
            .take(MAX_CASE_RESULTS_PER_SECTION)

        appendLine(
            if (prioritizedCases.any { it.status == TestCaseStatus.Failed || it.status == TestCaseStatus.Error }) {
                "Most relevant failing case results:"
            } else {
                "Recorded case sample:"
            }
        )
        prioritizedCases.forEachIndexed { index, caseResult ->
            appendCaseResult(index = index, caseResult = caseResult)
        }
        val omittedCount = result.cases.size - prioritizedCases.size
        if (omittedCount > 0) {
            appendLine("... $omittedCount more recorded case results omitted.")
        }
    }

    private fun StringBuilder.appendCaseResult(
        index: Int,
        caseResult: ProblemTestCaseResult
    ) {
        appendLine("${index + 1}. ${caseResult.label}")
        appendLine("Status: ${caseResult.status.name.lowercase().replaceFirstChar(Char::uppercase)}")
        if (caseResult.input.isNotBlank()) {
            appendLine("Input:")
            appendLine(truncateBlock(caseResult.input, MAX_TEST_CASE_FIELD_CHARS))
        }
        if (caseResult.expectedOutput.isNotBlank()) {
            appendLine("Expected output:")
            appendLine(truncateBlock(caseResult.expectedOutput, MAX_TEST_CASE_FIELD_CHARS))
        }
        if (caseResult.actualOutput.isNotBlank()) {
            appendLine("Actual output:")
            appendLine(truncateBlock(caseResult.actualOutput, MAX_TEST_CASE_FIELD_CHARS))
        }
        if (caseResult.errorOutput.isNotBlank()) {
            appendLine("Error:")
            appendLine(truncateBlock(caseResult.errorOutput, MAX_STDIO_CHARS))
        }
        caseResult.durationMillis?.let { duration ->
            appendLine("Duration: $duration ms")
        }
    }

    private fun truncateBlock(
        value: String,
        maxChars: Int
    ): String {
        val trimmed = value.trim()
        if (trimmed.length <= maxChars) return trimmed
        return trimmed.take(maxChars).trimEnd() + "\n... [truncated]"
    }

    private fun isUnrecorded(result: ProblemExecutionResult): Boolean {
        return result.cases.isEmpty() &&
            result.stdout.isBlank() &&
            result.stderr.isBlank() &&
            result.status != dev.kaixinguo.standalonecodepractice.ui.workspace.ExecutionStatus.Running &&
            (
                result.summary == "Run local checks to see validation details here." ||
                    result.summary == "No recorded custom run yet." ||
                    result.summary == "No recorded local submission run yet."
                )
    }

    private const val MAX_CONTEXT_CHARS = 5_200
    private const val MAX_STATEMENT_CHARS = 1_800
    private const val MAX_EXAMPLE_CHARS = 400
    private const val MAX_SUMMARY_CHARS = 240
    private const val MAX_STDIO_CHARS = 320
    private const val MAX_TEST_CASE_FIELD_CHARS = 220
    private const val MAX_CASES_PER_TEST_SUITE_SECTION = 2
    private const val MAX_CASE_RESULTS_PER_SECTION = 2
}
