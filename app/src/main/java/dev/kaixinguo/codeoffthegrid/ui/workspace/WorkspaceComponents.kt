package dev.kaixinguo.codeoffthegrid.ui.workspace

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.kaixinguo.codeoffthegrid.R
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentAmber
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentBlue
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentGreen
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBackground
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBackgroundAlt
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBorder
import dev.kaixinguo.codeoffthegrid.ui.theme.DividerColor
import dev.kaixinguo.codeoffthegrid.ui.theme.InsetSurface
import dev.kaixinguo.codeoffthegrid.ui.theme.MediaSurface
import dev.kaixinguo.codeoffthegrid.ui.theme.TextPrimary
import dev.kaixinguo.codeoffthegrid.ui.theme.TextSecondary

@Composable
internal fun CardBlock(
    title: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = CardBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column {
            if (title != null || trailing != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    trailing?.invoke()
                }
                HorizontalDivider(color = DividerColor)
            }

            Column(
                modifier = Modifier.padding(14.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun ExampleValueBlock(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Surface(
            color = InsetSurface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = if (onClick != null) {
                modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
            } else {
                modifier.fillMaxWidth()
            }
        ) {
            Text(
                text = value,
                color = valueColor,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
internal fun MarkdownStatementText(
    markdown: String,
    modifier: Modifier = Modifier,
    copyableCodeFences: Boolean = false,
    codeFenceCopyLabel: String = "Code snippet"
) {
    val normalizedMarkdown = remember(markdown) { normalizeMarkdownForDisplay(markdown) }
    val blocks = remember(normalizedMarkdown) { markdownToBlocks(normalizedMarkdown) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildMarkdownAnnotatedString(
                            markdown = block.text,
                            emphasisColor = TextPrimary
                        ),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is MarkdownBlock.Heading -> {
                    Text(
                        text = buildMarkdownAnnotatedString(
                            markdown = block.text,
                            emphasisColor = TextPrimary
                        ),
                        color = TextPrimary,
                        style = if (block.level <= 2) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        }
                    )
                }
                is MarkdownBlock.Bullet -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "-",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = buildMarkdownAnnotatedString(
                                markdown = block.text,
                                emphasisColor = TextPrimary
                            ),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is MarkdownBlock.CodeFence -> {
                    Surface(
                        color = InsetSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (copyableCodeFences) {
                                    Modifier.clickable {
                                        clipboardManager.setText(AnnotatedString(block.code))
                                        Toast.makeText(
                                            context,
                                            "$codeFenceCopyLabel copied",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Text(
                            text = block.code,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                is MarkdownBlock.Image -> {
                    MarkdownAssetImage(
                        source = block.source,
                        alt = block.alt,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is MarkdownBlock.Example -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = block.title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        block.images.forEach { image ->
                            MarkdownAssetImage(
                                source = image.source,
                                alt = image.alt,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (block.input.isNotBlank()) {
                            ExampleValueBlock(
                                label = "Input",
                                value = block.input,
                                labelColor = dev.kaixinguo.codeoffthegrid.ui.theme.AccentRed,
                                valueColor = TextSecondary
                            )
                        }
                        if (block.output.isNotBlank()) {
                            ExampleValueBlock(
                                label = "Output",
                                value = block.output,
                                labelColor = dev.kaixinguo.codeoffthegrid.ui.theme.AccentGreen,
                                valueColor = TextPrimary
                            )
                        }
                        if (block.explanation.isNotBlank()) {
                            ExampleValueBlock(
                                label = "Explanation",
                                value = block.explanation,
                                labelColor = AccentBlue,
                                valueColor = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownAssetImage(
    source: String,
    alt: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val image = remember(source) { loadMarkdownAssetImage(context = context, source = source) }

    if (image != null) {
        Surface(
            color = MediaSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = modifier
        ) {
            Image(
                bitmap = image,
                contentDescription = alt.ifBlank { "Problem diagram" },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(image.width.toFloat() / image.height.toFloat())
            )
        }
    } else if (alt.isNotBlank()) {
        Text(
            text = alt,
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun EditableSupportTextBlock(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = Dp.Unspecified,
    maxHeight: Dp = Dp.Unspecified,
    scrollable: Boolean = false
) {
    val scrollState = rememberScrollState()
    val textFieldModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = minHeight, max = maxHeight)
        .then(
            if (scrollable) {
                Modifier.verticalScroll(scrollState)
            } else {
                Modifier
            }
        )
        .padding(horizontal = 12.dp, vertical = 10.dp)

    Surface(
        color = InsetSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(AccentBlue),
            modifier = textFieldModifier
        )
    }
}

@Composable
internal fun CustomCaseEditor(
    testCase: ProblemTestCase,
    onCaseChange: (ProblemTestCase) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    CardBlock(
        title = testCase.label,
        modifier = modifier.fillMaxWidth(),
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlainActionChip(
                    label = if (testCase.enabled) "Enabled" else "Disabled",
                    accentColor = if (testCase.enabled) AccentGreen else AccentAmber
                ) {
                    onCaseChange(testCase.copy(enabled = !testCase.enabled))
                }
                PlainActionChip("Remove", onClick = onRemove)
            }
        }
    ) {
        Text(
            text = "Input",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        EditableSupportTextBlock(
            value = testCase.stdin,
            onValueChange = { onCaseChange(testCase.copy(stdin = it)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Expected Output",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        EditableSupportTextBlock(
            value = testCase.expectedOutput,
            onValueChange = { onCaseChange(testCase.copy(expectedOutput = it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun HintLine(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Hint $number:",
            color = TextPrimary,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = text,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
internal fun SupportTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.4f) else CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) AccentBlue else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
        )
    }
}

@Composable
internal fun ActionChip(
    label: String,
    active: Boolean,
    emphasized: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        active -> AccentBlue.copy(alpha = 0.14f)
        emphasized -> dev.kaixinguo.codeoffthegrid.ui.theme.AccentGreen.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val borderColor = when {
        active -> AccentBlue.copy(alpha = 0.45f)
        emphasized -> dev.kaixinguo.codeoffthegrid.ui.theme.AccentGreen.copy(alpha = 0.38f)
        else -> CardBorder
    }
    val textColor = when {
        active -> AccentBlue
        emphasized -> dev.kaixinguo.codeoffthegrid.ui.theme.AccentGreen
        else -> TextSecondary
    }

    Surface(
        color = background,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
internal fun StatusBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
internal fun SmallActionChip(
    label: String,
    enabled: Boolean = true
) {
    Surface(
        color = if (enabled) CardBackgroundAlt else CardBackgroundAlt.copy(alpha = 0.62f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (enabled) CardBorder else CardBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StackIcon(enabled = enabled)
            Text(
                text = label,
                color = if (enabled) TextSecondary else TextSecondary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
internal fun StackIcon(enabled: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (enabled) TextSecondary else TextSecondary.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
internal fun PlainActionChip(
    label: String,
    iconRes: Int? = null,
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor = accentColor?.copy(alpha = 0.12f) ?: CardBackgroundAlt
    val borderColor = accentColor?.copy(alpha = 0.34f) ?: CardBorder
    val contentColor = accentColor ?: TextSecondary

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
internal fun WorkspaceModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.18f) else CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.55f) else CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun RailModeButton(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        color = when {
            !enabled -> CardBackgroundAlt.copy(alpha = 0.62f)
            selected -> AccentBlue.copy(alpha = 0.16f)
            else -> CardBackgroundAlt
        },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            when {
                !enabled -> CardBorder.copy(alpha = 0.6f)
                selected -> AccentBlue.copy(alpha = 0.5f)
                else -> CardBorder
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = when {
                    !enabled -> TextSecondary.copy(alpha = 0.7f)
                    selected -> TextPrimary
                    else -> TextSecondary
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class CodeFence(val code: String) : MarkdownBlock
    data class Image(val alt: String, val source: String) : MarkdownBlock
    data class Example(
        val title: String,
        val images: List<Image>,
        val input: String,
        val output: String,
        val explanation: String
    ) : MarkdownBlock
}

private fun markdownToBlocks(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return emptyList()

    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphLines = mutableListOf<String>()
    val codeFenceLines = mutableListOf<String>()
    var inCodeFence = false

    fun flushParagraph() {
        if (paragraphLines.isEmpty()) return
        blocks += MarkdownBlock.Paragraph(paragraphLines.joinToString("\n").trim())
        paragraphLines.clear()
    }

    fun flushCodeFence() {
        if (codeFenceLines.isEmpty()) return
        blocks += MarkdownBlock.CodeFence(codeFenceLines.joinToString("\n").trimEnd())
        codeFenceLines.clear()
    }

    markdown.lines().forEach { rawLine ->
        val trimmedEnd = rawLine.trimEnd()
        val normalized = trimmedEnd.trim()

        if (normalized.startsWith("```")) {
            if (inCodeFence) {
                flushCodeFence()
            } else {
                flushParagraph()
            }
            inCodeFence = !inCodeFence
            return@forEach
        }

        if (inCodeFence) {
            codeFenceLines += rawLine
            return@forEach
        }

        if (normalized.isBlank()) {
            flushParagraph()
            return@forEach
        }

        val headingMatch = Regex("""^(#{1,6})\s+(.*)$""").matchEntire(normalized)
        if (headingMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.Heading(
                level = headingMatch.groupValues[1].length,
                text = headingMatch.groupValues[2].trim()
            )
            return@forEach
        }

        val imageMatch = Regex("""^!\[([^\]]*)\]\(([^)]+)\)$""").matchEntire(normalized)
        if (imageMatch != null) {
            flushParagraph()
            blocks += MarkdownBlock.Image(
                alt = imageMatch.groupValues[1].trim(),
                source = imageMatch.groupValues[2].trim()
            )
            return@forEach
        }

        if (normalized.startsWith("- ") || normalized.startsWith("* ")) {
            flushParagraph()
            blocks += MarkdownBlock.Bullet(text = normalized.drop(2).trim())
            return@forEach
        }

        paragraphLines += trimmedEnd
    }

    flushParagraph()
    flushCodeFence()
    return mergeExampleBlocks(blocks)
}

private fun mergeExampleBlocks(blocks: List<MarkdownBlock>): List<MarkdownBlock> {
    if (blocks.isEmpty()) return blocks

    val merged = mutableListOf<MarkdownBlock>()
    var index = 0

    while (index < blocks.size) {
        val current = blocks[index]
        val title = (current as? MarkdownBlock.Paragraph)?.text?.takeIf(::isExampleTitle)

        if (title != null) {
            val images = mutableListOf<MarkdownBlock.Image>()
            var nextIndex = index + 1
            while (blocks.getOrNull(nextIndex) is MarkdownBlock.Image) {
                images += blocks[nextIndex] as MarkdownBlock.Image
                nextIndex += 1
            }
            val codeFence = blocks.getOrNull(nextIndex) as? MarkdownBlock.CodeFence
            if (codeFence != null) {
                val parsedExample = parseExampleCodeFence(codeFence.code)
                if (parsedExample != null) {
                var explanation = parsedExample.explanation
                var consumed = nextIndex - index + 1
                val trailingParagraph = blocks.getOrNull(index + consumed) as? MarkdownBlock.Paragraph
                if (trailingParagraph != null) {
                    parseExplanationParagraph(trailingParagraph.text)?.let { trailingExplanation ->
                        explanation = listOf(explanation, trailingExplanation)
                            .filter { it.isNotBlank() }
                            .joinToString("\n\n")
                        consumed += 1
                    }
                }
                merged += MarkdownBlock.Example(
                    title = normalizeExampleTitle(title),
                    images = images,
                    input = parsedExample.input,
                    output = parsedExample.output,
                    explanation = explanation
                )
                index += consumed
                continue
                }
            }
        }

        merged += current
        index += 1
    }

    return merged
}

private data class ParsedExampleFence(
    val input: String,
    val output: String,
    val explanation: String
)

private fun isExampleTitle(text: String): Boolean {
    val normalized = text.trim()
    return Regex("""^\*\*Example(?:\s+\d+)?:\*\*$""", RegexOption.IGNORE_CASE).matches(normalized) ||
        Regex("""^Example(?:\s+\d+)?:$""", RegexOption.IGNORE_CASE).matches(normalized)
}

private fun normalizeExampleTitle(text: String): String {
    return text
        .trim()
        .removePrefix("**")
        .removeSuffix("**")
        .removeSuffix(":")
        .trim()
}

private fun parseExampleCodeFence(code: String): ParsedExampleFence? {
    val sections = linkedMapOf<String, StringBuilder>()
    var currentLabel: String? = null

    code.lines().forEach { rawLine ->
        val trimmed = rawLine.trim()
        val labelMatch = Regex("""^(Input|Output|Explanation):\s*(.*)$""", RegexOption.IGNORE_CASE)
            .matchEntire(trimmed)

        if (labelMatch != null) {
            currentLabel = labelMatch.groupValues[1].lowercase()
            val content = labelMatch.groupValues[2]
            val builder = sections.getOrPut(currentLabel!!) { StringBuilder() }
            if (content.isNotBlank()) {
                if (builder.isNotEmpty() && builder[builder.lastIndex] != '\n') {
                    builder.append('\n')
                }
                builder.append(content.trimEnd())
            }
            return@forEach
        }

        if (trimmed.isBlank()) {
            if (currentLabel != null) {
                val builder = sections.getOrPut(currentLabel!!) { StringBuilder() }
                if (builder.isNotEmpty() && builder[builder.lastIndex] != '\n') {
                    builder.append('\n')
                }
            }
            return@forEach
        }

        if (currentLabel == null) return null

        val builder = sections.getOrPut(currentLabel!!) { StringBuilder() }
        if (builder.isNotEmpty() && builder[builder.lastIndex] != '\n') {
            builder.append('\n')
        }
        builder.append(rawLine.trimEnd())
    }

    val input = sections["input"]?.toString()?.trim().orEmpty()
    val output = sections["output"]?.toString()?.trim().orEmpty()
    val explanation = sections["explanation"]?.toString()?.trim().orEmpty()
    return if (input.isBlank() && output.isBlank() && explanation.isBlank()) {
        null
    } else {
        ParsedExampleFence(
            input = input,
            output = output,
            explanation = explanation
        )
    }
}

private fun parseExplanationParagraph(text: String): String? {
    val normalized = text.trim()
    val parsed = when {
        normalized.startsWith("**Explanation:**") -> normalized.removePrefix("**Explanation:**").trim()
        normalized.startsWith("Explanation:") -> normalized.removePrefix("Explanation:").trim()
        else -> null
    }
    return parsed?.ifBlank { null }
}

private fun loadMarkdownAssetImage(
    context: android.content.Context,
    source: String
): ImageBitmap? {
    val assetPath = source
        .takeIf { it.startsWith("asset://") }
        ?.removePrefix("asset://")
        ?: return null

    return runCatching {
        context.assets.open(assetPath).use { inputStream ->
            BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
        }
    }.getOrNull()
}

private fun buildMarkdownAnnotatedString(
    markdown: String,
    emphasisColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < markdown.length) {
            if (markdown.startsWith("**", index)) {
                val closing = markdown.indexOf("**", startIndex = index + 2)
                if (closing > index + 2) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = emphasisColor))
                    append(markdown.substring(index + 2, closing))
                    pop()
                    index = closing + 2
                    continue
                }
            }

            if (
                markdown[index] == '*' &&
                (index + 1 >= markdown.length || markdown[index + 1] != '*')
            ) {
                val closing = markdown.indexOf('*', startIndex = index + 1)
                if (
                    closing > index + 1 &&
                    (closing + 1 >= markdown.length || markdown[closing + 1] != '*')
                ) {
                    pushStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = emphasisColor
                        )
                    )
                    append(markdown.substring(index + 1, closing))
                    pop()
                    index = closing + 1
                    continue
                }
            }

            if (markdown[index] == '_') {
                val closing = markdown.indexOf('_', startIndex = index + 1)
                if (closing > index + 1) {
                    pushStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = emphasisColor
                        )
                    )
                    append(markdown.substring(index + 1, closing))
                    pop()
                    index = closing + 1
                    continue
                }
            }

            if (markdown[index] == '`') {
                val closing = markdown.indexOf('`', startIndex = index + 1)
                if (closing > index + 1) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = emphasisColor
                        )
                    )
                    append(markdown.substring(index + 1, closing))
                    pop()
                    index = closing + 1
                    continue
                }
            }

            append(markdown[index])
            index += 1
        }
    }
}

internal fun normalizeMarkdownForDisplay(markdown: String): String {
    if (markdown.isBlank()) return markdown

    val normalizedLines = mutableListOf<String>()
    var inCodeFence = false

    markdown.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            inCodeFence = !inCodeFence
            normalizedLines += line
        } else if (inCodeFence) {
            normalizedLines += line
        } else {
            normalizedLines += line.normalizeLatexMath()
        }
    }

    return normalizedLines.joinToString("\n")
}

private fun String.normalizeLatexMath(): String {
    return this
        .replace(Regex("""\\\((.+?)\\\)"""), "$1")
        .replace(Regex("""\\\[(.+?)\\\]"""), "$1")
        .replace(Regex("""\\sqrt\{([^}]+)\}"""), "âˆš($1)")
        .replace("""\cdot""", "Â·")
        .replace("""\times""", "Ã—")
        .replace("""\leq""", "â‰¤")
        .replace("""\geq""", "â‰¥")
}

private fun markdownToDisplayText(markdown: String): String {
    val renderedLines = mutableListOf<String>()
    var inCodeFence = false

    normalizeMarkdownForDisplay(markdown).lines().forEach { rawLine ->
        val trimmed = rawLine.trimEnd()
        val normalized = trimmed.trim()

        if (normalized.startsWith("```")) {
            inCodeFence = !inCodeFence
            return@forEach
        }

        renderedLines += when {
            normalized.startsWith("#") -> normalized.trimStart('#', ' ')
            normalized.startsWith("- ") -> "â€¢ ${normalized.removePrefix("- ").trim()}"
            inCodeFence -> rawLine
            else -> trimmed
        }
    }

    return renderedLines
        .joinToString("\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

@Composable
internal fun ArrowActionChip(
    direction: ArrowDirection,
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Icon(
            painter = painterResource(direction.iconRes),
            contentDescription = contentDescription
                ?: if (direction == ArrowDirection.Expand) "Expand sidebar" else "Collapse sidebar",
            tint = TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun EraserSizeChip(
    size: EraserSize,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.18f) else CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.55f) else CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = size.label,
            color = if (selected) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun ToolChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.18f) else CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.55f) else CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun ColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = CardBackgroundAlt,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (selected) TextPrimary else CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
internal fun SuggestionLine(text: String) {
    Text(
        text = text,
        color = TextPrimary,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
internal fun SettingLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = value,
            color = AccentBlue,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

