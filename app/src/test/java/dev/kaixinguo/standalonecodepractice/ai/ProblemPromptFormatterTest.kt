package dev.kaixinguo.standalonecodepractice.ai

import dev.kaixinguo.standalonecodepractice.ui.workspace.ExecutionStatus
import dev.kaixinguo.standalonecodepractice.ui.workspace.ExecutionTarget
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemComposerDraft
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemExecutionResult
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemListItem
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestCase
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestCaseResult
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestSuite
import dev.kaixinguo.standalonecodepractice.ui.workspace.TestCaseStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemPromptFormatterTest {
    @Test
    fun formatComposer_includesDraftFieldsAndMissingList() {
        val prompt = ProblemPromptFormatter.formatComposer(
            draft = ProblemComposerDraft(
                title = "Two Sum",
                difficulty = "Easy",
                summary = "Classic warm-up.",
                statementMarkdown = "Find two indices.",
                exampleInput = "nums = [2,7,11,15], target = 9",
                exampleOutput = "[0,1]"
            ),
            effectiveStarterCode = "class Solution:\n    def twoSum(self, nums, target):\n        pass"
        )

        assertTrue(prompt.contains("Composer draft:"))
        assertTrue(prompt.contains("Title: Two Sum"))
        assertTrue(prompt.contains("About This Problem:"))
        assertTrue(prompt.contains("Classic warm-up."))
        assertTrue(prompt.contains("Starter code mode: Auto"))
        assertTrue(prompt.contains("Missing or weak fields:"))
        assertTrue(prompt.contains("- Hints"))
    }

    @Test
    fun format_includes_test_suites_and_recorded_results() {
        val problem = ProblemListItem(
            title = "Two Sum",
            difficulty = "Easy",
            summary = "Find two indices that sum to the target.",
            exampleInput = "nums = [2,7,11,15]\ntarget = 9",
            exampleOutput = "[0,1]"
        )
        val customSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Case 1",
                    stdin = "nums = [3,2,4]\ntarget = 6",
                    expectedOutput = "[1,2]"
                )
            )
        )
        val submissionSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Example 1",
                    stdin = "nums = [3,3]\ntarget = 6",
                    expectedOutput = "[0,1]"
                )
            )
        )
        val customResult = ProblemExecutionResult(
            target = ExecutionTarget.Custom,
            status = ExecutionStatus.Failed,
            title = "Custom Run Failed",
            summary = "1 of 1 custom cases failed.",
            cases = listOf(
                ProblemTestCaseResult(
                    testCaseId = "custom-1",
                    label = "Case 1",
                    status = TestCaseStatus.Failed,
                    input = "nums = [3,2,4]\ntarget = 6",
                    expectedOutput = "[1,2]",
                    actualOutput = "[0,2]"
                )
            )
        )
        val submissionResult = ProblemExecutionResult(
            target = ExecutionTarget.LocalSubmission,
            status = ExecutionStatus.Passed,
            title = "Local Submission Passed",
            summary = "1 of 1 built-in submission cases passed."
        )

        val prompt = ProblemPromptFormatter.format(
            problem = problem,
            customTestSuite = customSuite,
            submissionTestSuite = submissionSuite,
            customExecutionResult = customResult,
            submissionExecutionResult = submissionResult
        )

        assertTrue(prompt.contains("Current custom test cases:"))
        assertTrue(prompt.contains("Case 1 (enabled)"))
        assertTrue(prompt.contains("nums = [3,2,4]\ntarget = 6"))
        assertTrue(prompt.contains("Current local submission test cases:"))
        assertTrue(prompt.contains("Example 1 (enabled)"))
        assertTrue(prompt.contains("Latest custom run result:"))
        assertTrue(prompt.contains("Actual output:\n[0,2]"))
        assertTrue(prompt.contains("Latest local submission result:"))
        assertTrue(prompt.contains("Summary: 1 of 1 built-in submission cases passed."))
    }

    @Test
    fun format_limitsExplainContextSizeForLargeSuites() {
        val longStatement = buildString {
            repeat(500) {
                append("This is a long problem statement sentence. ")
            }
        }
        val manyCases = (1..12).map { index ->
            ProblemTestCase(
                label = "Case $index",
                stdin = "nums = [1,2,3,$index]",
                expectedOutput = "[1,2,3,$index]"
            )
        }
        val manyResults = (1..10).map { index ->
            ProblemTestCaseResult(
                testCaseId = "case-$index",
                label = "Case $index",
                status = if (index % 2 == 0) TestCaseStatus.Failed else TestCaseStatus.Passed,
                input = "nums = [1,2,3,$index]",
                expectedOutput = "[1,2,3,$index]",
                actualOutput = "[9,9,9,$index]",
                errorOutput = "traceback line $index"
            )
        }

        val prompt = ProblemPromptFormatter.format(
            problem = ProblemListItem(
                title = "Huge Problem",
                summary = longStatement
            ),
            customTestSuite = ProblemTestSuite(cases = manyCases),
            submissionTestSuite = ProblemTestSuite(cases = manyCases),
            customExecutionResult = ProblemExecutionResult(
                target = ExecutionTarget.Custom,
                status = ExecutionStatus.Failed,
                title = "Custom Run Failed",
                summary = "Lots of custom cases failed.",
                cases = manyResults
            ),
            submissionExecutionResult = ProblemExecutionResult(
                target = ExecutionTarget.LocalSubmission,
                status = ExecutionStatus.Failed,
                title = "Submission Run Failed",
                summary = "Lots of built-in cases failed.",
                cases = manyResults
            )
        )

        assertTrue(prompt.length <= 5_200)
        assertTrue(prompt.contains("... [truncated]"))
        assertTrue(prompt.contains("more saved cases omitted"))
        assertTrue(prompt.contains("more recorded case results omitted"))
    }
}
