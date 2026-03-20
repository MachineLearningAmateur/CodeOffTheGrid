package dev.kaixinguo.standalonecodepractice.ai

import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestComparisonMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedTestSuiteJsonParserTest {
    private val subject = GeneratedTestSuiteJsonParser()

    @Test
    fun parse_acceptsFencedJsonObject() {
        val suite = subject.parse(
            """
                ```json
                {
                  "cases": [
                    {
                      "label": "Edge duplicate",
                      "stdin": "nums = [1,1]",
                      "expectedOutput": "true"
                    }
                  ]
                }
                ```
            """.trimIndent()
        )

        assertEquals(1, suite.cases.size)
        assertEquals("Case 1", suite.cases.single().label)
        assertEquals("nums = [1,1]", suite.cases.single().stdin)
    }

    @Test
    fun parse_readsOptionalComparisonFields() {
        val suite = subject.parse(
            """
                [
                  {
                    "stdin": "graph = [[1],[0]]",
                    "expectedOutput": "[[1],[0]]",
                    "comparisonMode": "unordered_nested_collections",
                    "acceptableOutputs": ["[[1],[0]]", "[[0],[1]]"]
                  }
                ]
            """.trimIndent()
        )

        assertEquals(ProblemTestComparisonMode.UnorderedNestedCollections, suite.cases.single().comparisonMode)
        assertTrue(suite.cases.single().acceptableOutputs.contains("[[0],[1]]"))
    }

    @Test
    fun parse_stripsTrailingOutputAssignmentFromStdin() {
        val suite = subject.parse(
            """
                {
                  "cases": [
                    {
                      "stdin": "nums = [2,0,4,6]\noutput = [0,48,0,0]",
                      "expectedOutput": "[0,48,0,0]"
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals("nums = [2,0,4,6]", suite.cases.single().stdin)
        assertEquals("[0,48,0,0]", suite.cases.single().expectedOutput)
    }

    @Test
    fun parse_promotesTrailingOutputAssignmentWhenExpectedOutputMissing() {
        val suite = subject.parse(
            """
                {
                  "cases": [
                    {
                      "stdin": "nums = [2,0,4,6]\nexpected = [0,48,0,0]"
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals("nums = [2,0,4,6]", suite.cases.single().stdin)
        assertEquals("[0,48,0,0]", suite.cases.single().expectedOutput)
    }
}
