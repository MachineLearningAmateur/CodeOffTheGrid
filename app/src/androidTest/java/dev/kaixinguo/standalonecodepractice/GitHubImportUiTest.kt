package dev.kaixinguo.standalonecodepractice

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GitHubImportUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun importsNeetCodeRepoFromDialog() {
        composeRule.onNodeWithTag("githubImportOpen").performClick()
        composeRule.onNodeWithTag("githubImportUrlField")
            .performTextInput("https://github.com/dipjul/NeetCode-150")
        composeRule.onNodeWithTag("githubImportSubmit").performClick()

        composeRule.waitUntil(timeoutMillis = 120_000) {
            runCatching {
                composeRule.onNodeWithTag("githubImportFeedback").fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }

        val feedbackText = composeRule.onNodeWithTag("githubImportFeedback")
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString(separator = "") { it.text }

        assertTrue("Expected success feedback, got: $feedbackText", feedbackText.contains("Imported"))
        assertTrue("Expected 150 imported problems, got: $feedbackText", feedbackText.contains("150"))
        assertTrue("Expected imported repo title, got: $feedbackText", feedbackText.contains("NeetCode 150"))
    }
}
