package dev.kaixinguo.codeoffthegrid.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownDisplayNormalizerTest {
    @Test
    fun normalizeMarkdownForDisplay_strips_inline_latex_delimiters() {
        val normalized = normalizeMarkdownForDisplay(
            "Runtime is \\(O(n)\\) and space is \\(O(1)\\)."
        )

        assertEquals("Runtime is O(n) and space is O(1).", normalized)
    }

    @Test
    fun normalizeMarkdownForDisplay_preserves_code_fences() {
        val normalized = normalizeMarkdownForDisplay(
            """
                Use \(O(n)\) time.

                ```python
                text = "\(O(n)\)"
                ```
            """.trimIndent()
        )

        assertEquals(
            """
                Use O(n) time.

                ```python
                text = "\(O(n)\)"
                ```
            """.trimIndent(),
            normalized
        )
    }
}
