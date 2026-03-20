package dev.kaixinguo.standalonecodepractice

import dev.kaixinguo.standalonecodepractice.data.ProblemSubmissionSuiteFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import java.io.File

class ProblemSubmissionSuiteFactoryTest {
    @Test
    fun build_extractsMultipleExampleCasesFromMarkdown() {
        val suite = ProblemSubmissionSuiteFactory.build(
            statementMarkdown = """
                Given an integer array `nums`, return `true` if any value appears **more than once** in the array, otherwise return `false`.

                **Example 1:**

                ```java
                Input: nums = [1, 2, 3, 3]

                Output: true
                ```

                **Example 2:**

                ```java
                Input: nums = [1, 2, 3, 4]

                Output: false
                ```
            """.trimIndent(),
            exampleInput = "nums = [1, 2, 3, 3]",
            exampleOutput = "true"
        )

        assertEquals(2, suite.cases.size)
        assertEquals("nums = [1, 2, 3, 3]", suite.cases[0].stdin)
        assertEquals("true", suite.cases[0].expectedOutput)
        assertEquals("nums = [1, 2, 3, 4]", suite.cases[1].stdin)
        assertEquals("false", suite.cases[1].expectedOutput)
    }

    @Test
    fun bundledCatalog_generatesSubmissionCasesForAllProblems() {
        val catalogFile = File("src/main/assets/neetcode150_catalog.json")
            .takeIf { it.exists() }
            ?: File("app/src/main/assets/neetcode150_catalog.json")
        val catalogText = catalogFile.readText()
        val submissionSuiteCount = Regex(""""submissionTestSuite"\s*:""")
            .findAll(catalogText)
            .count()

        assertEquals(150, submissionSuiteCount)
        assertFalse(
            "Expected every bundled submission suite to contain at least one case.",
            Regex(""""submissionTestSuite"\s*:\s*\{\s*"draft"\s*:\s*"[^"]*"\s*,\s*"cases"\s*:\s*\[\s*]""")
                .containsMatchIn(catalogText)
        )

        val root = JSONObject(catalogText)
        val folders = root.getJSONArray("folders")
        var maxExpectedOutputLength = 0
        for (folderIndex in 0 until folders.length()) {
            val sets = folders.getJSONObject(folderIndex).getJSONArray("sets")
            for (setIndex in 0 until sets.length()) {
                val problems = sets.getJSONObject(setIndex).getJSONArray("problems")
                for (problemIndex in 0 until problems.length()) {
                    val problem = problems.getJSONObject(problemIndex)
                    val title = problem.getString("title")
                    val exampleInput = problem.optString("exampleInput")
                    assertFalse("$title still uses dummy_input in exampleInput.", exampleInput.contains("dummy_input"))

                    val cases = problem
                        .getJSONObject("submissionTestSuite")
                        .getJSONArray("cases")
                    assertEquals("$title should have 10 bundled submission cases.", 10, cases.length())

                    for (caseIndex in 0 until cases.length()) {
                        val testCase = cases.getJSONObject(caseIndex)
                        assertEquals("example-${caseIndex + 1}", testCase.getString("id"))
                        assertEquals("Example ${caseIndex + 1}", testCase.getString("label"))
                        assertFalse(
                            "$title still uses dummy_input in bundled stdin.",
                            testCase.optString("stdin").contains("dummy_input")
                        )
                        maxExpectedOutputLength = maxOf(
                            maxExpectedOutputLength,
                            testCase.optString("expectedOutput").length
                        )
                    }
                }
            }
        }

        assertTrue(
            "Bundled expected outputs should stay below the mobile-safe size threshold.",
            maxExpectedOutputLength <= 200_000
        )
    }
}
