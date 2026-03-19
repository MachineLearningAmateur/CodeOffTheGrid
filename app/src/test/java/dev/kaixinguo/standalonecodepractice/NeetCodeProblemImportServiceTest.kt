package dev.kaixinguo.standalonecodepractice

import dev.kaixinguo.standalonecodepractice.data.NeetCodeProblemImportService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeetCodeProblemImportServiceTest {
    @Test
    fun importsDistinctSubsequencesByTitle() = runBlocking {
        val problem = NeetCodeProblemImportService()
            .importProblemByTitle("Distinct Subsequences")

        assertEquals("Distinct Subsequences", problem.title)
        assertEquals("Hard", problem.difficulty)
        assertTrue(problem.summary.startsWith("You are given two strings"))
        assertTrue(problem.statementMarkdown.contains("**Constraints:**"))
        assertEquals("s = \"caaat\", t = \"cat\"", problem.exampleInput)
        assertEquals("3", problem.exampleOutput)
        assertTrue(problem.starterCode.contains("def numDistinct"))
        assertFalse(problem.hints.isEmpty())
        assertTrue(problem.questionUrl.contains("/problems/count-subsequences/question"))
    }
}
