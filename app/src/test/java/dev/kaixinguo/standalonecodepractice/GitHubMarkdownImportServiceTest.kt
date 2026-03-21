package dev.kaixinguo.standalonecodepractice

import dev.kaixinguo.standalonecodepractice.data.GitHubMarkdownImportService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubMarkdownImportServiceTest {
    @Test
    fun privateRegexHelpers_treatLabelsAndKeysAsLiterals() {
        val service = GitHubMarkdownImportService()

        val extractInlineField = GitHubMarkdownImportService::class.java.getDeclaredMethod(
            "extractInlineField",
            String::class.java,
            String::class.java
        ).apply { isAccessible = true }
        val extractJsonString = GitHubMarkdownImportService::class.java.getDeclaredMethod(
            "extractJsonString",
            String::class.java,
            String::class.java
        ).apply { isAccessible = true }

        val inlineValue = extractInlineField.invoke(
            service,
            "Input(1): nums = [1, 2, 3]",
            "Input(1)"
        ) as String
        val jsonValue = extractJsonString.invoke(
            service,
            """{"na(me":"value"}""",
            "na(me"
        ) as String

        assertEquals("nums = [1, 2, 3]", inlineValue)
        assertEquals("value", jsonValue)
    }

    @Test
    fun importsNeetCode150Repo() = runBlocking {
        val folder = GitHubMarkdownImportService()
            .importRepo("https://github.com/dipjul/NeetCode-150")

        assertEquals("NeetCode 150", folder.title)
        assertTrue(folder.sets.isNotEmpty())
        assertEquals(150, folder.sets.sumOf { it.problems.size })
        assertTrue(folder.sets.any { it.title.contains("Arrays", ignoreCase = true) })
        assertFalse(folder.sets.any { it.title.contains("Misc", ignoreCase = true) })
        assertTrue(
            folder.sets.flatMap { it.problems }
                .any { it.title.equals("Contains Duplicate", ignoreCase = true) }
        )
        assertTrue(
            folder.sets.flatMap { it.problems }
                .any { it.title.equals("Design Twitter", ignoreCase = true) }
        )
        assertTrue(
            folder.sets.flatMap { it.problems }
                .any { it.title.equals("Word Search II", ignoreCase = true) }
        )
        val containsDuplicate = folder.sets
            .flatMap { it.problems }
            .first { it.title.equals("Contains Duplicate", ignoreCase = true) }
        assertEquals(
            "Given an integer array nums, return true if any value appears at least twice in the array, and return false if every element is distinct.",
            containsDuplicate.summary
        )
        assertTrue(containsDuplicate.statementMarkdown.contains("Constraints:"))
    }
}
