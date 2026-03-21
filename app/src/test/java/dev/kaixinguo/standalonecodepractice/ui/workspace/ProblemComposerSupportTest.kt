package dev.kaixinguo.standalonecodepractice.ui.workspace

import dev.kaixinguo.standalonecodepractice.data.ProblemShareCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ProblemComposerSupportTest {
    @Test
    fun composerDraft_defaultsDifficultyToEasy() {
        assertEquals("Easy", ProblemComposerDraft().difficulty)
    }

    @Test
    fun validationError_requiresDestinationTitleAndContent() {
        val blankDraft = ProblemComposerDraft()

        assertEquals(
            "Choose a destination set before saving.",
            blankDraft.validationError(emptySet())
        )

        val missingTitle = blankDraft.copy(destinationSetId = "set-1")
        assertEquals(
            "Enter a problem title before saving.",
            missingTitle.validationError(setOf("set-1"))
        )

        val missingContent = missingTitle.copy(title = "Sample")
        assertEquals(
            "Add a statement before saving.",
            missingContent.validationError(setOf("set-1"))
        )
    }

    @Test
    fun toPreviewProblem_autoBuildsSubmissionSuiteAndNormalizesHints() {
        val draft = ProblemComposerDraft(
            destinationSetId = "set-1",
            title = "Contains Duplicate",
            difficulty = "Easy",
            summary = "Return true when duplicates exist.",
            statementMarkdown = """
                **Example 1:**

                ```text
                Input: nums = [1,2,3,1]
                Output: true
                ```
            """.trimIndent(),
            exampleInput = "nums = [1,2,3,1]",
            exampleOutput = "true",
            hintsText = "Use a set\n\nTrack seen values"
        )

        val preview = draft.toPreviewProblem()

        assertEquals("Contains Duplicate", preview.title)
        assertEquals(listOf("Use a set", "Track seen values"), preview.hints)
        assertEquals(1, preview.submissionTestSuite.cases.size)
        assertEquals("Example 1", preview.submissionTestSuite.cases.single().label)
    }

    @Test
    fun effectiveStarterCode_usesMinimalReturnStubInsteadOfPass() {
        val starterCode = ProblemComposerDraft(
            title = "Contains Duplicate",
            statementMarkdown = "Return true if any value appears at least twice.",
            exampleInput = "nums = [1,2,3,1]",
            exampleOutput = "true"
        ).effectiveStarterCode()

        assertTrue(starterCode.contains("return"))
        assertFalse(starterCode.contains("..."))
        assertFalse(starterCode.contains("pass"))
    }

    @Test
    fun parseAndSerializeComposerHints_preserveBlankEditableRows() {
        val serialized = serializeComposerHints(listOf("First hint", ""))
        val editableHints = parseComposerHintsText(
            raw = serialized,
            preserveBlankEntries = true
        )

        assertEquals(listOf("First hint", ""), editableHints)
        assertEquals(listOf("First hint"), parseComposerHintsText(serialized))
    }

    @Test
    fun submissionSuiteValidationError_rejectsInvalidJson() {
        val draft = ProblemComposerDraft(
            destinationSetId = "set-1",
            title = "Sample",
            summary = "Summary",
            submissionTestSuiteJson = "{not-json}"
        )

        assertEquals(
            "Advanced submission suite JSON is invalid.",
            draft.submissionTestSuiteValidationError()
        )
    }

    @Test
    fun withImportedProblem_preservesDestinationAndLoadsAdvancedFields() {
        val imported = ProblemShareCodec.decodeFromString(
            """
                {
                  "schemaVersion": 1,
                  "type": "standalone_code_practice_problem",
                  "problem": {
                    "title": "Two Sum",
                    "difficulty": "Easy",
                    "summary": "Find indices.",
                    "statementMarkdown": "Statement",
                    "exampleInput": "nums = [2,7,11,15], target = 9",
                    "exampleOutput": "[0,1]",
                    "starterCode": "class Solution:\n    def twoSum(self, nums, target):\n        pass",
                    "hints": ["Store complements"],
                    "executionPipeline": "single_method"
                  }
                }
            """.trimIndent()
        )

        val draft = ProblemComposerDraft(destinationSetId = "set-1")
            .withImportedProblem(imported)

        assertEquals("set-1", draft.destinationSetId)
        assertEquals("Two Sum", draft.title)
        assertEquals("single_method", draft.executionPipelineOverride)
        assertEquals("Store complements", draft.hintsText)
        assertNull(draft.submissionTestSuiteValidationError())
        assertTrue(draft.toSharedProblemJson().contains("\"schemaVersion\": 1"))
    }

    @Test
    fun withImportedProblem_defaultsBlankDifficultyToEasy() {
        val imported = ProblemShareCodec.decodeFromString(
            """
                {
                  "schemaVersion": 1,
                  "type": "standalone_code_practice_problem",
                  "problem": {
                    "title": "Two Sum",
                    "difficulty": "",
                    "summary": "Find indices.",
                    "statementMarkdown": "Statement",
                    "exampleInput": "nums = [2,7,11,15], target = 9",
                    "exampleOutput": "[0,1]",
                    "starterCode": "",
                    "hints": []
                  }
                }
            """.trimIndent()
        )

        val draft = ProblemComposerDraft(destinationSetId = "set-1")
            .withImportedProblem(imported)

        assertEquals("Easy", draft.difficulty)
    }
}
