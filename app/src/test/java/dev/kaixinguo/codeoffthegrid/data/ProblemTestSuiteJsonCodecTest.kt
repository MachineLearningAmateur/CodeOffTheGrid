package dev.kaixinguo.codeoffthegrid.data

import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestCase
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestSuite
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemTestSuiteJsonCodecTest {
    @Test
    fun encodeToJson_canOmitIdsForEditableComposerJson() {
        val json = ProblemTestSuiteJsonCodec.encodeToJson(
            ProblemTestSuite(
                cases = listOf(
                    ProblemTestCase(
                        label = "Case 1",
                        stdin = "nums = [1,2,3]",
                        expectedOutput = "false"
                    )
                )
            ),
            includeIds = false
        )

        val caseJson = json.getJSONArray("cases").getJSONObject(0)

        assertFalse(json.has("draft"))
        assertFalse(caseJson.has("id"))
        assertTrue(caseJson.has("label"))
    }

    @Test
    fun decodeFromString_generatesUniqueIdsWhenMissing() {
        val decoded = ProblemTestSuiteJsonCodec.decodeFromString(
            """
                {
                  "cases": [
                    {
                      "label": "Case 1",
                      "stdin": "nums = [1,2,3]",
                      "expectedOutput": "false"
                    },
                    {
                      "label": "Case 2",
                      "stdin": "nums = [1,2,2]",
                      "expectedOutput": "true"
                    }
                  ]
                }
            """.trimIndent()
        )!!

        assertTrue(decoded.cases.all { it.id.isNotBlank() })
        assertNotEquals(decoded.cases[0].id, decoded.cases[1].id)
    }
}
