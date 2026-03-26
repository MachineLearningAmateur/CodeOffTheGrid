package dev.kaixinguo.codeoffthegrid.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchPoint
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStroke
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStrokeKind
import org.junit.Assert.assertEquals
import org.junit.Test

class SketchCodeStrokeLayoutTest {
    @Test
    fun buildRecognitionLines_groupsCodeStrokesIntoSeparateLines() {
        val strokes = listOf(
            codeStroke(x = 10f, y = 10f, timestampStart = 0L),
            codeStroke(x = 60f, y = 12f, timestampStart = 50L),
            codeStroke(x = 30f, y = 74f, timestampStart = 100L)
        )

        val lines = SketchCodeStrokeLayout.buildRecognitionLines(strokes)

        assertEquals(2, lines.size)
        assertEquals(10f, lines[0].minX)
        assertEquals(30f, lines[1].minX)
    }

    @Test
    fun reconstructCode_appliesIndentationAndBlankLines() {
        val topLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 10f, timestampStart = 0L)),
            minX = 10f,
            maxX = 80f,
            minY = 10f,
            maxY = 30f,
            lineHeight = 24f
        )
        val indentedLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 34f, y = 48f, timestampStart = 100L)),
            minX = 34f,
            maxX = 104f,
            minY = 48f,
            maxY = 68f,
            lineHeight = 24f
        )
        val gapLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 118f, timestampStart = 200L)),
            minX = 10f,
            maxX = 92f,
            minY = 118f,
            maxY = 138f,
            lineHeight = 24f
        )

        val reconstructed = SketchCodeStrokeLayout.reconstructCode(
            listOf(
                topLine to "if ready:",
                indentedLine to "return value",
                gapLine to "print('done')"
            )
        )

        assertEquals("if ready:\n    return value\n\nprint('done')", reconstructed)
    }

    @Test
    fun reconstructCode_appliesPythonIndentationHeuristics() {
        val ifLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 10f, timestampStart = 0L)),
            minX = 10f,
            maxX = 80f,
            minY = 10f,
            maxY = 30f,
            lineHeight = 24f
        )
        val bodyLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 48f, timestampStart = 100L)),
            minX = 10f,
            maxX = 96f,
            minY = 48f,
            maxY = 68f,
            lineHeight = 24f
        )
        val elseLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 34f, y = 86f, timestampStart = 200L)),
            minX = 34f,
            maxX = 104f,
            minY = 86f,
            maxY = 106f,
            lineHeight = 24f
        )
        val elseBodyLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 34f, y = 124f, timestampStart = 300L)),
            minX = 34f,
            maxX = 112f,
            minY = 124f,
            maxY = 144f,
            lineHeight = 24f
        )

        val reconstructed = SketchCodeStrokeLayout.reconstructCode(
            listOf(
                ifLine to "if ready:",
                bodyLine to "return value",
                elseLine to "else:",
                elseBodyLine to "return fallback"
            )
        )

        assertEquals(
            "if ready:\n    return value\nelse:\n    return fallback",
            reconstructed
        )
    }

    @Test
    fun reconstructCode_splitsInlinePythonBlockBodiesDuringConversion() {
        val loopLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 10f, timestampStart = 0L)),
            minX = 10f,
            maxX = 140f,
            minY = 10f,
            maxY = 30f,
            lineHeight = 24f
        )
        val elseLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 48f, timestampStart = 100L)),
            minX = 10f,
            maxX = 140f,
            minY = 48f,
            maxY = 68f,
            lineHeight = 24f
        )

        val reconstructed = SketchCodeStrokeLayout.reconstructCode(
            listOf(
                loopLine to "for i in nums: total += i",
                elseLine to "if total: return total"
            )
        )

        assertEquals(
            "for i in nums:\n    total += i\nif total:\n    return total",
            reconstructed
        )
    }

    @Test
    fun reconstructCode_splitsEmbeddedNewlinesAfterPythonBlockColon() {
        val loopLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 10f, timestampStart = 0L)),
            minX = 10f,
            maxX = 160f,
            minY = 10f,
            maxY = 30f,
            lineHeight = 24f
        )
        val conditionalLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 48f, timestampStart = 100L)),
            minX = 10f,
            maxX = 170f,
            minY = 48f,
            maxY = 68f,
            lineHeight = 24f
        )

        val reconstructed = SketchCodeStrokeLayout.reconstructCode(
            listOf(
                loopLine to "for i in nums:\ntotal += i",
                conditionalLine to "if total:\nreturn total"
            )
        )

        assertEquals(
            "for i in nums:\n    total += i\nif total:\n    return total",
            reconstructed
        )
    }

    @Test
    fun reconstructCode_preservesDedentForEmbeddedElifAndElseBlocks() {
        val ifLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 10f, timestampStart = 0L)),
            minX = 10f,
            maxX = 180f,
            minY = 10f,
            maxY = 30f,
            lineHeight = 24f
        )
        val elifLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 48f, timestampStart = 100L)),
            minX = 10f,
            maxX = 190f,
            minY = 48f,
            maxY = 68f,
            lineHeight = 24f
        )
        val elseLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 86f, timestampStart = 200L)),
            minX = 10f,
            maxX = 150f,
            minY = 86f,
            maxY = 106f,
            lineHeight = 24f
        )

        val reconstructed = SketchCodeStrokeLayout.reconstructCode(
            listOf(
                ifLine to "if first:\nreturn first",
                elifLine to "elif second:\nreturn second",
                elseLine to "else:\nreturn fallback"
            )
        )

        assertEquals(
            "if first:\n    return first\nelif second:\n    return second\nelse:\n    return fallback",
            reconstructed
        )
    }

    @Test
    fun reconstructCode_indentsNestedEmbeddedBlocks() {
        val outerIfLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 10f, y = 10f, timestampStart = 0L)),
            minX = 10f,
            maxX = 200f,
            minY = 10f,
            maxY = 30f,
            lineHeight = 24f
        )
        val loopLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 36f, y = 48f, timestampStart = 100L)),
            minX = 36f,
            maxX = 220f,
            minY = 48f,
            maxY = 68f,
            lineHeight = 24f
        )
        val returnLine = CodeRecognitionLine(
            strokes = listOf(codeStroke(x = 62f, y = 86f, timestampStart = 200L)),
            minX = 62f,
            maxX = 180f,
            minY = 86f,
            maxY = 106f,
            lineHeight = 24f
        )

        val reconstructed = SketchCodeStrokeLayout.reconstructCode(
            listOf(
                outerIfLine to "if ready:",
                loopLine to "for value in nums:\ntotal += value",
                returnLine to "if total:\nreturn total"
            )
        )

        assertEquals(
            "if ready:\n    for value in nums:\n        total += value\n        if total:\n            return total",
            reconstructed
        )
    }

    @Test
    fun formatRecognizedPythonDraftForEditor_preservesElifElseChainsFromRecognizedDraft() {
        val formatted = formatRecognizedPythonDraftForEditor(
            recognizedDraft = "if value:\n    return value\nelif fallback:\n    return fallback\nelse:\n    return default",
            currentDraftCode = """
                class Solution:
                    def solve(self, value, fallback, default):
                        pass
            """.trimIndent(),
            append = false
        )

        assertEquals(
            """
                class Solution:
                    def solve(self, value, fallback, default):
                        if value:
                            return value
                        elif fallback:
                            return fallback
                        else:
                            return default
            """.trimIndent(),
            formatted
        )
    }

    @Test
    fun normalizeRecognizedCodeLine_replacesSmartPunctuation() {
        assertEquals(
            "\"value\" - 'item'",
            normalizeRecognizedCodeLine("\u201Cvalue\u201D \u2014 \u2018item\u2019")
        )
    }

    @Test
    fun normalizeRecognizedCodeLine_fixesCommonPythonOperatorSpacing() {
        assertEquals(
            "for i in nums: total += i if total != limit and current <= max_value and next >= min_value and value == target:",
            normalizeRecognizedCodeLine(
                "for i in nums : total + = i if total ! = limit and current < = max_value and next > = min_value and value = = target :"
            )
        )
    }

    @Test
    fun normalizeRecognizedCodeLine_fixesMisreadBlockColonDigit() {
        assertEquals(
            "for i in nums: total += i",
            normalizeRecognizedCodeLine("for i in nums 8 total + = i")
        )
        assertEquals(
            "if total:",
            normalizeRecognizedCodeLine("if total 8")
        )
    }

    @Test
    fun normalizeRecognizedCodeLine_keepsLiteralEightComparisons() {
        assertEquals(
            "if value == 8",
            normalizeRecognizedCodeLine("if value == 8")
        )
        assertEquals(
            "if value is 8",
            normalizeRecognizedCodeLine("if value is 8")
        )
    }

    @Test
    fun formatRecognizedPythonDraftForEditor_nestsBodyUnderExistingFunctionHeader() {
        val formatted = formatRecognizedPythonDraftForEditor(
            recognizedDraft = "counts = {}\nreturn counts",
            currentDraftCode = """
                class Solution:
                    def solve(self, nums):
                        pass
            """.trimIndent(),
            append = false
        )

        assertEquals(
            """
                class Solution:
                    def solve(self, nums):
                        counts = {}
                        return counts
            """.trimIndent(),
            formatted
        )
    }

    @Test
    fun formatRecognizedPythonDraftForEditor_appendsIndentedBodyWithoutDroppingScaffold() {
        val formatted = formatRecognizedPythonDraftForEditor(
            recognizedDraft = "if value:\n    return value",
            currentDraftCode = """
                class Solution:
                    def solve(self, nums):
                        total = 0
            """.trimIndent(),
            append = true
        )

        assertEquals(
            """
                class Solution:
                    def solve(self, nums):
                        total = 0
                        if value:
                            return value
            """.trimIndent(),
            formatted
        )
    }

    private fun codeStroke(x: Float, y: Float, timestampStart: Long): SketchStroke {
        return SketchStroke(
            points = listOf(
                SketchPoint(Offset(x, y), timestampStart),
                SketchPoint(Offset(x + 20f, y + 8f), timestampStart + 10L)
            ),
            color = Color.Red,
            kind = SketchStrokeKind.Code
        )
    }
}
