package dev.kaixinguo.standalonecodepractice.data

import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemExecutionPipeline
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestCase
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestSuite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemShareCodecTest {
    @Test
    fun decodeFromString_readsVersionedProblemFile() {
        val serialized = """
            {
              "schemaVersion": 1,
              "type": "standalone_code_practice_problem",
              "problem": {
                "title": "Contains Duplicate",
                "difficulty": "Easy",
                "summary": "Return true when duplicates exist.",
                "statementMarkdown": "**Example 1:**",
                "exampleInput": "nums = [1,2,3,1]",
                "exampleOutput": "true",
                "starterCode": "class Solution:\n    def containsDuplicate(self, nums):\n        pass",
                "hints": ["Use a set", "Track seen values"],
                "executionPipeline": "single_method",
                "submissionTestSuite": {
                  "draft": "",
                  "cases": [
                    {
                      "id": "case-1",
                      "label": "Example 1",
                      "stdin": "nums = [1,2,3,1]",
                      "expectedOutput": "true",
                      "enabled": true,
                      "comparisonMode": "exact",
                      "acceptableOutputs": []
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val decoded = ProblemShareCodec.decodeFromString(serialized)

        assertEquals("Contains Duplicate", decoded.title)
        assertEquals("Easy", decoded.difficulty)
        assertEquals(listOf("Use a set", "Track seen values"), decoded.hints)
        assertEquals(ProblemExecutionPipeline.SingleMethod, decoded.executionPipeline)
        assertEquals(1, decoded.submissionTestSuite?.cases?.size)
        assertEquals("Example 1", decoded.submissionTestSuite?.cases?.single()?.label)
    }

    @Test
    fun decodeFromString_rejectsUnsupportedSchemaVersion() {
        val serialized = """
            {
              "schemaVersion": 2,
              "type": "standalone_code_practice_problem",
              "problem": {
                "title": "Sample"
              }
            }
        """.trimIndent()

        val failure = runCatching {
            ProblemShareCodec.decodeFromString(serialized)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals("Unsupported problem file version: 2", failure?.message)
    }

    @Test
    fun encodeToString_roundTripsSharedProblemFile() {
        val sharedProblem = SharedProblemFile(
            title = "Two Sum",
            difficulty = "Easy",
            summary = "Find two indices that sum to target.",
            statementMarkdown = "Statement",
            exampleInput = "nums = [2,7,11,15], target = 9",
            exampleOutput = "[0,1]",
            starterCode = "class Solution:\n    def twoSum(self, nums, target):\n        pass",
            hints = listOf("Store complements"),
            submissionTestSuite = ProblemTestSuite(
                cases = listOf(
                    ProblemTestCase(
                        label = "Example 1",
                        stdin = "nums = [2,7,11,15], target = 9",
                        expectedOutput = "[0,1]"
                    )
                )
            ),
            executionPipeline = ProblemExecutionPipeline.SingleMethod
        )

        val serialized = ProblemShareCodec.encodeToString(sharedProblem)
        val decoded = ProblemShareCodec.decodeFromString(serialized)

        assertEquals(sharedProblem.title, decoded.title)
        assertEquals(sharedProblem.hints, decoded.hints)
        assertEquals(sharedProblem.executionPipeline, decoded.executionPipeline)
        assertEquals(1, decoded.submissionTestSuite?.cases?.size)
    }
}
