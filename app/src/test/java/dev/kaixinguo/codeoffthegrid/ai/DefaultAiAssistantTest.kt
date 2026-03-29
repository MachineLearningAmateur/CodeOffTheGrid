package dev.kaixinguo.codeoffthegrid.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAiAssistantTest {
    @Test
    fun createProblem_buildsCreateProblemPromptBeforeCallingEngine() = runBlocking {
        val engine = RecordingQwenEngine(
            response = """
                {
                  "schemaVersion": 1,
                  "type": "standalone_code_practice_problem",
                  "problem": {
                    "title": "Two Sum",
                    "difficulty": "Easy",
                    "summary": "Generated array hash map problem.",
                    "statementMarkdown": "Find two indices whose values sum to a target.",
                    "exampleInput": "nums = [2,7,11,15]\ntarget = 9",
                    "exampleOutput": "[0,1]",
                    "starterCode": "class Solution:\n    def twoSum(self, nums, target):\n        pass",
                    "hints": ["Use a hash map."],
                    "executionPipeline": "single_method"
                  }
                }
            """.trimIndent()
        )
        val subject = DefaultAiAssistant(
            promptBuilder = DefaultPromptBuilder(),
            localQwenEngine = engine
        )

        val response = subject.createProblem(
            problem = "Composer draft context",
            code = "class Solution:\n    pass",
            request = "make the wording more interview-like"
        )

        assertEquals("Two Sum", response.title)
        assertEquals("Easy", response.difficulty)
        assertEquals("Generated array hash map problem.", response.summary)
        assertEquals("single_method", response.executionPipeline?.storageValue)
        assertTrue(engine.prompts.single().contains("Composer draft context"))
        assertTrue(engine.prompts.single().contains("make the wording more interview-like"))
        assertTrue(engine.prompts.single().contains("coding-problem authoring assistant"))
    }

    @Test
    fun generateHint_buildsHintPromptBeforeCallingEngine() = runBlocking {
        val engine = RecordingQwenEngine(response = "hint-response")
        val subject = DefaultAiAssistant(
            promptBuilder = DefaultPromptBuilder(),
            localQwenEngine = engine
        )

        val response = subject.generateHint(
            problem = "Contains Duplicate",
            code = null,
            request = "focus on duplicates only"
        )

        assertEquals("hint-response", response)
        assertTrue(engine.prompts.single().contains("focus on duplicates only"))
        assertTrue(engine.prompts.single().contains("Contains Duplicate"))
    }

    @Test
    fun generateCustomTests_parsesStructuredJson() = runBlocking {
        val engine = RecordingQwenEngine(
            response = """
                {
                  "cases": [
                    {
                      "label": "whatever",
                      "stdin": "nums = [1,2,3]",
                      "expectedOutput": "false"
                    }
                  ]
                }
            """.trimIndent()
        )
        val subject = DefaultAiAssistant(
            promptBuilder = DefaultPromptBuilder(),
            localQwenEngine = engine
        )

        val suite = subject.generateCustomTests(
            problem = "Contains Duplicate",
            code = null,
            request = "prefer edge cases"
        )

        assertEquals(1, suite.cases.size)
        assertEquals("Case 1", suite.cases.single().label)
        assertEquals("nums = [1,2,3]", suite.cases.single().stdin)
        assertTrue(engine.prompts.single().contains("prefer edge cases"))
    }

    @Test
    fun reviewCode_buildsReviewPromptBeforeCallingEngine() = runBlocking {
        val engine = RecordingQwenEngine(response = "review-response")
        val subject = DefaultAiAssistant(
            promptBuilder = DefaultPromptBuilder(),
            localQwenEngine = engine
        )

        val response = subject.reviewCode(
            problem = "Contains Duplicate",
            code = "return len(nums) != len(set(nums))",
            request = "focus on complexity"
        )

        assertEquals("review-response", response)
        assertTrue(engine.prompts.single().contains("focus on complexity"))
        assertTrue(engine.prompts.single().contains("Return exactly one ```python fenced block and no prose outside it"))
        assertTrue(engine.prompts.single().contains("Literal draft facts:"))
        assertTrue(engine.prompts.single().contains("return len(nums) != len(set(nums))"))
    }

    private class RecordingQwenEngine(
        private val response: String
    ) : LocalQwenEngine {
        val prompts = mutableListOf<String>()

        override suspend fun generate(prompt: String): String {
            prompts += prompt
            return response
        }
    }
}
