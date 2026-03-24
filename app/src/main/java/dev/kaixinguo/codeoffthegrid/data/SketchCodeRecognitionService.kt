package dev.kaixinguo.codeoffthegrid.data

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchPoint
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStroke
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStrokeKind
import java.nio.charset.Charset
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max

internal interface SketchCodeRecognitionService {
    suspend fun isModelDownloaded(): Boolean
    suspend fun downloadModel()
    suspend fun recognizeCode(strokes: List<SketchStroke>): String
}

internal class MlKitSketchCodeRecognitionService : SketchCodeRecognitionService {
    private val logTag = "SketchCodeRecognition"
    private val modelIdentifier = try {
        DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
    } catch (error: MlKitException) {
        throw IllegalStateException("Unable to configure the English handwriting model.", error)
    } ?: throw IllegalStateException("No handwriting model is available for en-US.")

    private val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
    private val remoteModelManager = RemoteModelManager.getInstance()
    private val recognizer = DigitalInkRecognition.getClient(
        DigitalInkRecognizerOptions.builder(model).build()
    )

    override suspend fun isModelDownloaded(): Boolean {
        val isDownloaded = remoteModelManager.isModelDownloaded(model).awaitTask()
        Log.i(logTag, "Handwriting model downloaded=$isDownloaded")
        return isDownloaded
    }

    override suspend fun downloadModel() {
        val startTimeMillis = System.currentTimeMillis()
        Log.i(logTag, "Starting handwriting model download")
        remoteModelManager.download(model, DownloadConditions.Builder().build()).awaitTask()
        Log.i(
            logTag,
            "Handwriting model download completed in ${System.currentTimeMillis() - startTimeMillis} ms"
        )
    }

    override suspend fun recognizeCode(strokes: List<SketchStroke>): String {
        val lines = SketchCodeStrokeLayout.buildRecognitionLines(strokes)
        if (lines.isEmpty()) return ""

        val recognizedLines = mutableListOf<Pair<CodeRecognitionLine, String>>()
        var preContext = ""

        lines.forEach { line ->
            val ink = line.toInk()
            val writingArea = WritingArea(line.writingAreaWidth, line.writingAreaHeight)
            val recognitionContext = RecognitionContext.builder()
                .setPreContext(preContext)
                .setWritingArea(writingArea)
                .build()
            val result = recognizer.recognize(ink, recognitionContext).awaitTask()
            val recognizedLine =
                normalizeRecognizedCodeLine(result.candidates.firstOrNull()?.text.orEmpty())
            recognizedLines += line to recognizedLine
            preContext = recognizedLines.buildRecognitionPreContext()
        }

        return SketchCodeStrokeLayout.reconstructCode(recognizedLines)
    }
}

internal object SketchCodeStrokeLayout {
    fun buildRecognitionLines(strokes: List<SketchStroke>): List<CodeRecognitionLine> {
        val metrics = strokes.asSequence()
            .filter { it.kind == SketchStrokeKind.Code && it.points.isNotEmpty() }
            .map { stroke -> stroke.toMetrics() }
            .toList()
        if (metrics.isEmpty()) return emptyList()

        val medianStrokeHeight = median(metrics.map { max(it.height, 18f) })
        val groupingThreshold = max(medianStrokeHeight * 0.9f, 24f)
        val groupedLines = mutableListOf<MutableList<SketchStrokeMetrics>>()

        metrics.sortedWith(compareBy<SketchStrokeMetrics> { it.centerY }.thenBy { it.firstTimestampMillis })
            .forEach { metric ->
                val targetLine = groupedLines.minByOrNull { line ->
                    val centerY = line.averageOf { it.centerY }
                    abs(centerY - metric.centerY)
                }?.takeIf { line ->
                    val centerY = line.averageOf { it.centerY }
                    abs(centerY - metric.centerY) <= groupingThreshold ||
                        metric.minY <= line.maxOf { it.maxY } + groupingThreshold
                }

                if (targetLine == null) {
                    groupedLines += mutableListOf(metric)
                } else {
                    targetLine += metric
                }
            }

        val lineHeight = max(median(groupedLines.map { line ->
            line.maxOf { it.maxY } - line.minOf { it.minY }
        }), 28f)

        return groupedLines.map { line ->
            val orderedStrokes = line.sortedWith(
                compareBy<SketchStrokeMetrics> { it.firstTimestampMillis }.thenBy { it.minX }
            ).map { it.stroke }
            val minX = line.minOf { it.minX }
            val maxX = line.maxOf { it.maxX }
            val minY = line.minOf { it.minY }
            val maxY = line.maxOf { it.maxY }
            CodeRecognitionLine(
                strokes = orderedStrokes,
                minX = minX,
                maxX = maxX,
                minY = minY,
                maxY = maxY,
                lineHeight = lineHeight
            )
        }.sortedBy { it.minY }
    }

    fun reconstructCode(recognizedLines: List<Pair<CodeRecognitionLine, String>>): String {
        if (recognizedLines.isEmpty()) return ""

        val nonBlankLines = recognizedLines.filter { (_, text) -> text.isNotBlank() }
        if (nonBlankLines.isEmpty()) return ""

        val indentationLevels = buildIndentLevels(nonBlankLines.map { it.first })
        val formattedLines = buildPythonFormattedLines(nonBlankLines, indentationLevels)
        val gapThreshold = max(median(nonBlankLines.map { it.first.height }) * 1.6f, 44f)
        val builder = StringBuilder()
        var previousLine: FormattedRecognitionLine? = null

        formattedLines.forEachIndexed { index, formattedLine ->
            val line = formattedLine.line
            if (index > 0) {
                builder.append('\n')
                val previous = previousLine
                if (previous != null && line.minY - previous.line.maxY > gapThreshold) {
                    builder.append('\n')
                }
            }

            builder.append("    ".repeat(formattedLine.indentLevel))
            builder.append(formattedLine.text)
            previousLine = formattedLine
        }

        return builder.toString().trimEnd()
    }

    private fun buildIndentLevels(lines: List<CodeRecognitionLine>): Map<CodeRecognitionLine, Int> {
        val tolerance = max(median(lines.map { it.height }) * 0.35f, 12f)
        val clusters = mutableListOf<Float>()

        lines.map { it.minX }
            .sorted()
            .forEach { startX ->
                val lastCluster = clusters.lastOrNull()
                if (lastCluster == null || abs(startX - lastCluster) > tolerance) {
                    clusters += startX
                } else {
                    clusters[clusters.lastIndex] = (lastCluster + startX) / 2f
                }
            }

        return lines.associateWith { line ->
            clusters.indices.minByOrNull { abs(clusters[it] - line.minX) } ?: 0
        }
    }

    private fun buildPythonFormattedLines(
        recognizedLines: List<Pair<CodeRecognitionLine, String>>,
        visualIndentLevels: Map<CodeRecognitionLine, Int>
    ): List<FormattedRecognitionLine> {
        val formattedLines = mutableListOf<FormattedRecognitionLine>()

        recognizedLines.forEach { (line, text) ->
            val expandedTexts = text.expandInlinePythonBlockLine()
            expandedTexts.forEachIndexed { expandedIndex, expandedText ->
                val previousLine = formattedLines.lastOrNull()
                val visualIndentLevel = visualIndentLevels[line] ?: 0
                var indentLevel = if (expandedIndex == 0) {
                    visualIndentLevel
                } else {
                    (formattedLines.lastOrNull()?.indentLevel ?: visualIndentLevel) + 1
                }

                if (previousLine != null) {
                    if (expandedText.isPythonDedentFollower()) {
                        indentLevel = minOf(indentLevel, (previousLine.indentLevel - 1).coerceAtLeast(0))
                    }
                    if (previousLine.text.isPythonBlockOpener() && indentLevel <= previousLine.indentLevel) {
                        indentLevel = previousLine.indentLevel + 1
                    }
                    if (!previousLine.text.isPythonBlockOpener() && indentLevel > previousLine.indentLevel + 1) {
                        indentLevel = previousLine.indentLevel + 1
                    }
                }

                formattedLines += FormattedRecognitionLine(
                    line = line,
                    text = expandedText,
                    indentLevel = indentLevel.coerceAtLeast(0)
                )
            }
        }

        return formattedLines
    }
}

internal data class CodeRecognitionLine(
    val strokes: List<SketchStroke>,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val lineHeight: Float
) {
    val height: Float
        get() = max(maxY - minY, lineHeight)

    val writingAreaWidth: Float
        get() = max((maxX - minX) + 32f, 96f)

    val writingAreaHeight: Float
        get() = max(height + 24f, 48f)

    fun toInk(): Ink {
        val inkBuilder = Ink.builder()
        strokes.forEach { stroke ->
            val strokeBuilder = Ink.Stroke.builder()
            stroke.points.forEach { point ->
                strokeBuilder.addPoint(
                    Ink.Point.create(
                        point.offset.x - minX + 16f,
                        point.offset.y - minY + 12f,
                        point.timestampMillis
                    )
                )
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        return inkBuilder.build()
    }
}

internal data class SketchStrokeMetrics(
    val stroke: SketchStroke,
    val firstTimestampMillis: Long,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
) {
    val centerY: Float
        get() = (minY + maxY) / 2f

    val height: Float
        get() = maxY - minY
}

internal data class FormattedRecognitionLine(
    val line: CodeRecognitionLine,
    val text: String,
    val indentLevel: Int
)

internal fun formatRecognizedPythonDraftForEditor(
    recognizedDraft: String,
    currentDraftCode: String,
    append: Boolean
): String {
    val normalizedDraft = recognizedDraft.trimEnd()
    if (normalizedDraft.isBlank()) return ""
    if (normalizedDraft.hasPythonDefinitionHeader()) return normalizedDraft

    val currentLines = currentDraftCode.lines()
    val functionHeaderIndex = currentLines.indexOfLast { line ->
        line.trimStart().startsWith("def ") && line.trimEnd().endsWith(":")
    }
    if (functionHeaderIndex == -1) return normalizedDraft

    val bodyIndent = currentLines.detectPythonBodyIndent(functionHeaderIndex)
    val indentedBody = normalizedDraft.reindentPythonBody(bodyIndent)

    return if (append && currentDraftCode.isNotBlank()) {
        currentDraftCode.trimEnd() + "\n" + indentedBody
    } else {
        buildList {
            addAll(currentLines.take(functionHeaderIndex + 1))
            addAll(indentedBody.lines())
        }.joinToString("\n").trimEnd()
    }
}

internal fun normalizeRecognizedCodeLine(text: String): String {
    val normalizedText = decodeCommonMojibake(text)
    return normalizedText
        .replace('	', ' ')
        .replace("Ã¢â‚¬Å“", "\"")
        .replace("Ã¢â‚¬Â", "\"")
        .replace("Ã¢â‚¬Ëœ", "'")
        .replace("Ã¢â‚¬â„¢", "'")
        .replace("Ã¢â‚¬â€œ", "-")
        .replace("Ã¢â‚¬â€", "-")
        .replace("\u201C", "\"")
        .replace("\u201D", "\"")
        .replace("\u2018", "'")
        .replace("\u2019", "'")
        .replace("\u2013", "-")
        .replace("\u2014", "-")
        .normalizeLikelyMisreadPythonBlockColon()
        .normalizePythonOperatorSpacing()
        .replace(Regex("[ ]{2,}"), " ")
        .trim()
}

private fun String.normalizeLikelyMisreadPythonBlockColon(): String {
    val leadingWhitespace = takeWhile(Char::isWhitespace)
    val trimmed = trim()
    if (!trimmed.startsWithPythonBlockKeyword() || ':' in trimmed) {
        return this
    }

    val inlineBodyMatch = Regex("""^(.+?)(?<![!<>=])\s+[78]\s+(.+)$""").matchEntire(trimmed)
    if (inlineBodyMatch != null) {
        val headerPrefix = inlineBodyMatch.groupValues[1].trimEnd()
        if (headerPrefix.isLikelyPythonBlockHeaderPrefix()) {
            val body = inlineBodyMatch.groupValues[2].trimStart()
            return "$leadingWhitespace$headerPrefix: $body"
        }
    }

    val lineEndMatch = Regex("""^(.+?)(?<![!<>=])\s+[78]$""").matchEntire(trimmed)
    if (lineEndMatch != null) {
        val headerPrefix = lineEndMatch.groupValues[1].trimEnd()
        if (headerPrefix.isLikelyPythonBlockHeaderPrefix()) {
            return "$leadingWhitespace$headerPrefix:"
        }
    }

    return this
}

private fun String.normalizePythonOperatorSpacing(): String {
    return this
        .replace(Regex("""!\s*="""), "!=")
        .replace(Regex("""=\s*="""), "==")
        .replace(Regex("""<\s*="""), "<=")
        .replace(Regex(""">\s*="""), ">=")
        .replace(Regex("""\+\s*="""), "+=")
        .replace(Regex("""-\s*="""), "-=")
        .replace(Regex("""\*\s*="""), "*=")
        .replace(Regex("""/\s*="""), "/=")
        .replace(Regex("""%\s*="""), "%=")
        .replace(Regex("""\*\s*\*"""), "**")
        .replace(Regex("""\s+:(?=\s|$)"""), ":")
}

private fun String.startsWithPythonBlockKeyword(): Boolean {
    return Regex(
        """^(for|while|if|elif|else|with|try|except|finally|def|class)\b"""
    ).containsMatchIn(this)
}

private fun String.isLikelyPythonBlockHeaderPrefix(): Boolean {
    val trimmed = trimEnd()
    if (!trimmed.startsWithPythonBlockKeyword()) return false
    if (
        Regex("""(==|!=|<=|>=|<|>)\s*$""").containsMatchIn(trimmed) ||
        Regex("""\b(is|is\s+not|in|not\s+in)\s*$""").containsMatchIn(trimmed)
    ) {
        return false
    }
    return true
}

private fun decodeCommonMojibake(text: String): String {
    if ('â' !in text && 'Ã' !in text) {
        return text
    }
    val windows1252 = Charset.forName("windows-1252")
    return runCatching { String(text.toByteArray(windows1252), Charsets.UTF_8) }
        .getOrDefault(text)
}

private fun SketchStroke.toMetrics(): SketchStrokeMetrics {
    val xs = points.map(SketchPoint::offset).map { it.x }
    val ys = points.map(SketchPoint::offset).map { it.y }
    return SketchStrokeMetrics(
        stroke = this,
        firstTimestampMillis = points.minOf { it.timestampMillis },
        minX = xs.min(),
        maxX = xs.max(),
        minY = ys.min(),
        maxY = ys.max()
    )
}

private fun MutableList<SketchStrokeMetrics>.averageOf(selector: (SketchStrokeMetrics) -> Float): Float {
    var total = 0f
    var count = 0
    forEach {
        total += selector(it)
        count += 1
    }
    return if (count == 0) 0f else total / count.toFloat()
}

private fun List<Pair<CodeRecognitionLine, String>>.buildRecognitionPreContext(): String {
    return asSequence()
        .map { (_, text) -> text }
        .filter { it.isNotBlank() }
        .joinToString(separator = "\n")
        .takeLast(20)
}

private fun String.isPythonBlockOpener(): Boolean = trimEnd().endsWith(":")

private fun String.isPythonDedentFollower(): Boolean {
    return trimStart().startsWith("elif ") ||
        trimStart() == "elif" ||
        trimStart().startsWith("else") ||
        trimStart().startsWith("except") ||
        trimStart().startsWith("finally") ||
        trimStart().startsWith("case ")
}

private fun String.expandInlinePythonBlockLine(): List<String> {
    val trimmed = trim()
    val blockHeaderRegex = Regex(
        """^(for\s+.+|while\s+.+|if\s+.+|elif\s+.+|else|with\s+.+|try|except(?:\s+.+)?|finally|def\s+.+|class\s+.+):\s+(.+)$"""
    )
    val match = blockHeaderRegex.matchEntire(trimmed) ?: return listOf(this)
    val header = match.groupValues[1].trimEnd() + ":"
    val body = match.groupValues[2].trim()
    if (body.isBlank()) return listOf(header)
    return listOf(header, body)
}

private fun String.hasPythonDefinitionHeader(): Boolean {
    return lines().any { line ->
        val trimmed = line.trimStart()
        trimmed.startsWith("def ") || trimmed.startsWith("class ")
    }
}

private fun List<String>.detectPythonBodyIndent(functionHeaderIndex: Int): String {
    val existingBodyIndent = drop(functionHeaderIndex + 1)
        .firstOrNull { it.isNotBlank() }
        ?.leadingIndent()
        .orEmpty()
    if (existingBodyIndent.isNotEmpty()) {
        return existingBodyIndent
    }

    val functionIndent = getOrNull(functionHeaderIndex)?.leadingIndent().orEmpty()
    return functionIndent + "    "
}

private fun String.reindentPythonBody(targetIndent: String): String {
    val lines = lines()
    val minimumIndent = lines
        .filter { it.isNotBlank() }
        .minOfOrNull { it.leadingIndentWidth() }
        ?: 0

    return lines.joinToString("\n") { line ->
        if (line.isBlank()) {
            ""
        } else {
            targetIndent + line.drop(minimumIndent.coerceAtMost(line.length))
        }
    }.trimEnd()
}

private fun String.leadingIndent(): String = takeWhile { it == ' ' || it == '\t' }

private fun String.leadingIndentWidth(): Int = leadingIndent().length

private fun median(values: List<Float>): Float {
    if (values.isEmpty()) return 0f
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middle - 1] + sorted[middle]) / 2f
    } else {
        sorted[middle]
    }
}

private suspend fun <T> Task<T>.awaitTask(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
}

