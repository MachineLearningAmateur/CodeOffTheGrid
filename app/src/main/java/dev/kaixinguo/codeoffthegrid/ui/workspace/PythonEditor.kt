package dev.kaixinguo.codeoffthegrid.ui.workspace

import android.view.MotionEvent
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentBlue
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBackground
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBorder
import dev.kaixinguo.codeoffthegrid.ui.theme.InsetSurface
import dev.kaixinguo.codeoffthegrid.ui.theme.TextMuted
import dev.kaixinguo.codeoffthegrid.ui.theme.TextPrimary
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

private val PythonKeywordColor = Color(0xFF7DB2FF)
private val PythonStringColor = Color(0xFFE8B66B)
private val PythonCommentColor = Color(0xFF7A8A9F)
private val PythonNumberColor = Color(0xFF7ED2C3)
private val PythonFunctionColor = Color(0xFFC58BFF)
private val PythonDecoratorColor = Color(0xFFFF9B7A)

private val PythonKeywords = setOf(
    "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del",
    "elif", "else", "except", "False", "finally", "for", "from", "global", "if", "import",
    "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "return",
    "True", "try", "while", "with", "yield"
)

private const val PythonIndent = "    "

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun UnifiedWorkspaceSurface(
    inputMode: WorkspaceInputMode,
    pythonCode: String,
    onPythonCodeChange: (String) -> Unit,
    strokes: MutableList<SketchStroke>,
    onStrokesChange: (List<SketchStroke>) -> Unit,
    activeStrokePoints: List<SketchPoint>,
    onActiveStrokePointsChange: (List<SketchPoint>) -> Unit,
    activeColor: Color,
    sketchTool: SketchTool,
    eraserSize: EraserSize,
    modifier: Modifier = Modifier
) {
    var activeStrokeColor by remember { mutableStateOf(activeColor) }
    var activeStrokeKind by remember { mutableStateOf(SketchStrokeKind.Diagram) }
    var eraserCursor by remember { mutableStateOf<Offset?>(null) }
    val eraserOutlineColor = TextPrimary.copy(alpha = 0.6f)
    val activeStroke = remember(activeStrokePoints, activeStrokeColor, activeStrokeKind) {
        if (activeStrokePoints.isEmpty()) {
            null
        } else {
            SketchStroke(
                points = activeStrokePoints,
                color = activeStrokeColor,
                kind = activeStrokeKind
            )
        }
    }
    val drawModifier = if (inputMode == WorkspaceInputMode.Sketch) {
        Modifier.pointerInteropFilter { event ->
            val pointerIndex = event.actionIndex.coerceAtLeast(0)
            val pointerOffset = Offset(event.getX(pointerIndex), event.getY(pointerIndex))
            var currentStroke = activeStrokePoints.toMutableList()
            val effectiveTool = resolveEffectiveSketchTool(
                event = event,
                configuredTool = sketchTool
            )

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (effectiveTool == SketchTool.DiagramPen || effectiveTool == SketchTool.CodePen) {
                        activeStrokeColor = activeColor
                        activeStrokeKind = if (effectiveTool == SketchTool.CodePen) {
                            SketchStrokeKind.Code
                        } else {
                            SketchStrokeKind.Diagram
                        }
                        currentStroke = mutableListOf(
                            SketchPoint(
                                offset = pointerOffset,
                                timestampMillis = event.eventTime
                            )
                        )
                        onActiveStrokePointsChange(currentStroke.toList())
                        eraserCursor = null
                    } else {
                        currentStroke = mutableListOf()
                        onActiveStrokePointsChange(emptyList())
                        eraserCursor = pointerOffset
                        eraseAt(strokes, pointerOffset, eraserSize.radius)
                        onStrokesChange(strokes.toList())
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val currentTool = if (currentStroke.isEmpty()) {
                        effectiveTool
                    } else if (activeStrokeKind == SketchStrokeKind.Code) {
                        SketchTool.CodePen
                    } else {
                        SketchTool.DiagramPen
                    }

                    if (currentTool != effectiveTool) {
                        commitSketchStrokeIfNeeded(
                            strokes = strokes,
                            currentStroke = currentStroke,
                            color = activeStrokeColor,
                            kind = activeStrokeKind,
                            onStrokesChange = onStrokesChange
                        )
                        currentStroke = mutableListOf()
                        onActiveStrokePointsChange(emptyList())

                        if (effectiveTool == SketchTool.Eraser) {
                            eraserCursor = pointerOffset
                            eraseAt(strokes, pointerOffset, eraserSize.radius)
                            onStrokesChange(strokes.toList())
                        } else {
                            activeStrokeColor = activeColor
                            activeStrokeKind = if (effectiveTool == SketchTool.CodePen) {
                                SketchStrokeKind.Code
                            } else {
                                SketchStrokeKind.Diagram
                            }
                            currentStroke = mutableListOf(
                                SketchPoint(
                                    offset = pointerOffset,
                                    timestampMillis = event.eventTime
                                )
                            )
                            onActiveStrokePointsChange(currentStroke.toList())
                            eraserCursor = null
                        }
                        return@pointerInteropFilter true
                    }

                    if (effectiveTool == SketchTool.DiagramPen || effectiveTool == SketchTool.CodePen) {
                        for (historyIndex in 0 until event.historySize) {
                            appendInterpolatedPoints(
                                stroke = currentStroke,
                                nextPoint = Offset(
                                    event.getHistoricalX(pointerIndex, historyIndex),
                                    event.getHistoricalY(pointerIndex, historyIndex)
                                ),
                                timestampMillis = event.getHistoricalEventTime(historyIndex)
                            )
                        }
                        appendInterpolatedPoints(
                            stroke = currentStroke,
                            nextPoint = pointerOffset,
                            timestampMillis = event.eventTime
                        )
                        onActiveStrokePointsChange(currentStroke.toList())
                        eraserCursor = null
                    } else {
                        eraserCursor = pointerOffset
                        for (historyIndex in 0 until event.historySize) {
                            eraseAt(
                                strokes,
                                Offset(
                                    event.getHistoricalX(pointerIndex, historyIndex),
                                    event.getHistoricalY(pointerIndex, historyIndex)
                                ),
                                eraserSize.radius
                            )
                        }
                        eraseAt(strokes, pointerOffset, eraserSize.radius)
                        onStrokesChange(strokes.toList())
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (effectiveTool == SketchTool.DiagramPen || effectiveTool == SketchTool.CodePen) {
                        appendInterpolatedPoints(
                            stroke = currentStroke,
                            nextPoint = pointerOffset,
                            timestampMillis = event.eventTime
                        )
                        commitSketchStrokeIfNeeded(
                            strokes = strokes,
                            currentStroke = currentStroke,
                            color = activeStrokeColor,
                            kind = activeStrokeKind,
                            onStrokesChange = onStrokesChange
                        )
                    }
                    eraserCursor = null
                    onActiveStrokePointsChange(emptyList())
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    eraserCursor = null
                    onActiveStrokePointsChange(emptyList())
                    true
                }

                else -> false
            }
        }
    } else {
        Modifier
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        ) {
            EditablePythonSurface(
                code = pythonCode,
                onCodeChange = onPythonCodeChange,
                readOnly = inputMode == WorkspaceInputMode.Sketch,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(drawModifier)
            ) {
                val allStrokes = strokes + listOfNotNull(activeStroke)
                allStrokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        for (index in 0 until stroke.points.lastIndex) {
                            drawLine(
                                color = stroke.color.copy(alpha = 0.95f),
                                start = stroke.points[index].offset,
                                end = stroke.points[index + 1].offset,
                                strokeWidth = 6f
                            )
                        }
                    }
                }

                if (inputMode == WorkspaceInputMode.Sketch &&
                    sketchTool == SketchTool.Eraser &&
                    eraserCursor != null
                ) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = eraserSize.radius,
                        center = eraserCursor!!
                    )
                    drawCircle(
                        color = eraserOutlineColor,
                        radius = eraserSize.radius,
                        center = eraserCursor!!,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
            }
        }
    }
}

private fun eraseAt(strokes: MutableList<SketchStroke>, position: Offset, radius: Float) {
    val radiusSquared = radius * radius
    val updatedStrokes = buildList {
        strokes.forEach { stroke ->
            val survivingSegments = mutableListOf<MutableList<SketchPoint>>()
            var currentSegment = mutableListOf<SketchPoint>()

            stroke.points.forEach { point ->
                val dx = point.offset.x - position.x
                val dy = point.offset.y - position.y
                val insideEraseRadius = (dx * dx) + (dy * dy) <= radiusSquared

                if (insideEraseRadius) {
                    if (currentSegment.size > 1) {
                        survivingSegments.add(currentSegment)
                    }
                    currentSegment = mutableListOf()
                } else {
                    currentSegment.add(point)
                }
            }

            if (currentSegment.size > 1) {
                survivingSegments.add(currentSegment)
            }

            survivingSegments.forEach { segment ->
                add(
                    SketchStroke(
                        points = segment.toList(),
                        color = stroke.color,
                        kind = stroke.kind
                    )
                )
            }
        }
    }
    strokes.clear()
    strokes.addAll(updatedStrokes)
}

private fun commitSketchStrokeIfNeeded(
    strokes: MutableList<SketchStroke>,
    currentStroke: List<SketchPoint>,
    color: Color,
    kind: SketchStrokeKind,
    onStrokesChange: (List<SketchStroke>) -> Unit
) {
    if (currentStroke.size <= 1) return
    strokes.add(
        SketchStroke(
            points = currentStroke.toList(),
            color = color,
            kind = kind
        )
    )
    onStrokesChange(strokes.toList())
}

private fun resolveEffectiveSketchTool(
    event: MotionEvent,
    configuredTool: SketchTool
): SketchTool {
    if (!event.isStylusEraserButtonPressed()) return configuredTool
    return configuredTool.toTemporaryEraserOverride()
}

private fun SketchTool.toTemporaryEraserOverride(): SketchTool {
    return when (this) {
        SketchTool.DiagramPen,
        SketchTool.CodePen -> SketchTool.Eraser
        SketchTool.Eraser -> SketchTool.Eraser
    }
}

private fun MotionEvent.isStylusEraserButtonPressed(): Boolean {
    if (pointerCount <= 0) return false
    val toolType = getToolType(actionIndex.coerceAtLeast(0))
    if (toolType != MotionEvent.TOOL_TYPE_STYLUS) return false
    val stylusButtons = MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_STYLUS_SECONDARY
    return (buttonState and stylusButtons) != 0
}

private fun appendInterpolatedPoints(
    stroke: MutableList<SketchPoint>,
    nextPoint: Offset,
    timestampMillis: Long,
    spacing: Float = 4f
) {
    if (stroke.isEmpty()) {
        stroke.add(SketchPoint(nextPoint, timestampMillis))
        return
    }

    val lastPoint = stroke.last()
    val distance = hypot(nextPoint.x - lastPoint.offset.x, nextPoint.y - lastPoint.offset.y)
    if (distance <= spacing) {
        stroke.add(SketchPoint(nextPoint, timestampMillis))
        return
    }

    val steps = ceil(distance / spacing).toInt()
    for (step in 1..steps) {
        val t = step.toFloat() / steps.toFloat()
        val interpolatedTimestamp = lastPoint.timestampMillis +
            ((timestampMillis - lastPoint.timestampMillis) * t).roundToInt()
        stroke.add(
            SketchPoint(
                offset = Offset(
                    x = lastPoint.offset.x + (nextPoint.x - lastPoint.offset.x) * t,
                    y = lastPoint.offset.y + (nextPoint.y - lastPoint.offset.y) * t
                ),
                timestampMillis = interpolatedTimestamp.toLong()
            )
        )
    }
}

private object PythonSyntaxVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = highlightPython(text.text),
            offsetMapping = OffsetMapping.Identity
        )
    }
}

private fun highlightPython(source: String): AnnotatedString {
    val commentRegex = Regex("#.*$")
    val stringRegex = Regex("'([^'\\\\]|\\\\.)*'|\"([^\"\\\\]|\\\\.)*\"")
    val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
    val decoratorRegex = Regex("(?m)^\\s*@\\w+")
    val definitionRegex = Regex("\\b(def|class)\\s+([A-Za-z_][A-Za-z0-9_]*)")
    val functionCallRegex = Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*(?=\\()")
    val keywordRegex = Regex(
        "\\b(" + PythonKeywords.joinToString("|") { Regex.escape(it) } + ")\\b"
    )
    val commentRanges = commentRegex.findAll(source).map { it.range }.toList()
    val stringRanges = stringRegex.findAll(source).map { it.range }.toList()
    val decoratorRanges = decoratorRegex.findAll(source).map { it.range }.toList()
    val protectedRanges = commentRanges + stringRanges + decoratorRanges
    val definitionNameRanges = definitionRegex.findAll(source)
        .mapNotNull { it.groups[2]?.range }
        .toList()

    return buildAnnotatedString {
        append(source)

        commentRanges.forEach { range ->
            addStyle(SpanStyle(color = PythonCommentColor), range.first, range.last + 1)
        }
        stringRanges.forEach { range ->
            addStyle(SpanStyle(color = PythonStringColor), range.first, range.last + 1)
        }
        numberRegex.findAll(source).forEach { match ->
            addStyle(SpanStyle(color = PythonNumberColor), match.range.first, match.range.last + 1)
        }
        decoratorRanges.forEach { range ->
            addStyle(SpanStyle(color = PythonDecoratorColor), range.first, range.last + 1)
        }
        keywordRegex.findAll(source).forEach { match ->
            addStyle(SpanStyle(color = PythonKeywordColor), match.range.first, match.range.last + 1)
        }
        functionCallRegex.findAll(source).forEach { match ->
            val nameGroup = match.groups[1] ?: return@forEach
            val name = nameGroup.value
            val nameRange = nameGroup.range

            if (name in PythonKeywords) return@forEach
            if (protectedRanges.any { nameRange.first >= it.first && nameRange.last <= it.last }) return@forEach
            if (definitionNameRanges.any { nameRange.first == it.first && nameRange.last == it.last }) return@forEach

            addStyle(
                SpanStyle(color = PythonFunctionColor, fontWeight = FontWeight.Medium),
                nameRange.first,
                nameRange.last + 1
            )
        }
        definitionRegex.findAll(source).forEach { match ->
            val keywordRange = match.groups[1]?.range
            val nameRange = match.groups[2]?.range
            if (keywordRange != null) {
                addStyle(SpanStyle(color = PythonKeywordColor), keywordRange.first, keywordRange.last + 1)
            }
            if (nameRange != null) {
                addStyle(
                    SpanStyle(color = PythonFunctionColor, fontWeight = FontWeight.SemiBold),
                    nameRange.first,
                    nameRange.last + 1
                )
            }
        }
    }
}

@Composable
internal fun EditablePythonSurface(
    code: String,
    onCodeChange: (String) -> Unit,
    readOnly: Boolean,
    placeholder: String = "Write your Python solution here...",
    modifier: Modifier = Modifier
) {
    val lines = remember(code) { code.lines().size.coerceAtLeast(1) }
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var editorValue by remember {
        mutableStateOf(TextFieldValue(text = code))
    }

    if (editorValue.text != code) {
        editorValue = editorValue.copy(
            text = code,
            selection = TextRange(code.length.coerceAtMost(editorValue.selection.end))
        )
    }

    Surface(
        modifier = modifier,
        color = InsetSurface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(verticalScrollState)
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(24.dp)
            ) {
                for (lineNumber in 1..lines) {
                    Text(
                        text = lineNumber.toString(),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                BasicTextField(
                    value = editorValue,
                    onValueChange = { updatedValue ->
                        editorValue = updatedValue
                        onCodeChange(updatedValue.text)
                    },
                    readOnly = readOnly,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(AccentBlue),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii
                    ),
                    visualTransformation = PythonSyntaxVisualTransformation,
                    modifier = Modifier
                        .wrapContentWidth(unbounded = true)
                        .onPreviewKeyEvent { keyEvent ->
                            if (
                                !readOnly &&
                                keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.Tab
                            ) {
                                val updatedValue = insertIndent(editorValue)
                                editorValue = updatedValue
                                onCodeChange(updatedValue.text)
                                true
                            } else {
                                false
                            }
                        },
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.wrapContentWidth(unbounded = true)) {
                            if (editorValue.text.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

private fun insertIndent(value: TextFieldValue): TextFieldValue {
    val start = value.selection.start
    val end = value.selection.end
    val updatedText = buildString {
        append(value.text.substring(0, start))
        append(PythonIndent)
        append(value.text.substring(end))
    }
    val updatedCursor = start + PythonIndent.length
    return value.copy(
        text = updatedText,
        selection = TextRange(updatedCursor)
    )
}

