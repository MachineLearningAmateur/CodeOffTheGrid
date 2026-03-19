package dev.kaixinguo.standalonecodepractice.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentBlue
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBackgroundAlt
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBorder
import dev.kaixinguo.standalonecodepractice.ui.theme.DividerColor
import dev.kaixinguo.standalonecodepractice.ui.theme.TextPrimary
import dev.kaixinguo.standalonecodepractice.ui.theme.TextSecondary

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
    valueColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Surface(
            color = Color(0xFF1A2230),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
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
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { markdownToBlocks(markdown) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildMarkdownAnnotatedString(block.text),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is MarkdownBlock.Heading -> {
                    Text(
                        text = buildMarkdownAnnotatedString(block.text),
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
                            text = buildMarkdownAnnotatedString(block.text),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is MarkdownBlock.CodeFence -> {
                    Surface(
                        color = Color(0xFF1A2230),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
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
            }
        }
    }
}

@Composable
internal fun EditableSupportTextBlock(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1A2230),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
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
                    label = if (testCase.enabled) "Enabled" else "Disabled"
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
        emphasized -> dev.kaixinguo.standalonecodepractice.ui.theme.AccentGreen.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val borderColor = when {
        active -> AccentBlue.copy(alpha = 0.45f)
        emphasized -> dev.kaixinguo.standalonecodepractice.ui.theme.AccentGreen.copy(alpha = 0.38f)
        else -> CardBorder
    }
    val textColor = when {
        active -> AccentBlue
        emphasized -> dev.kaixinguo.standalonecodepractice.ui.theme.AccentGreen
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
internal fun SmallActionChip(label: String) {
    Surface(
        color = CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StackIcon()
            Text(
                text = label,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
internal fun StackIcon() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(TextSecondary)
            )
        }
    }
}

@Composable
internal fun PlainActionChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    ) {
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
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
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AccentBlue.copy(alpha = 0.16f) else CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) AccentBlue.copy(alpha = 0.5f) else CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) TextPrimary else TextSecondary,
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

        if (normalized.startsWith("- ") || normalized.startsWith("* ")) {
            flushParagraph()
            blocks += MarkdownBlock.Bullet(text = normalized.drop(2).trim())
            return@forEach
        }

        paragraphLines += trimmedEnd
    }

    flushParagraph()
    flushCodeFence()
    return blocks
}

private fun buildMarkdownAnnotatedString(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < markdown.length) {
            if (markdown.startsWith("**", index)) {
                val closing = markdown.indexOf("**", startIndex = index + 2)
                if (closing > index + 2) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary))
                    append(markdown.substring(index + 2, closing))
                    pop()
                    index = closing + 2
                    continue
                }
            }

            if (markdown[index] == '`') {
                val closing = markdown.indexOf('`', startIndex = index + 1)
                if (closing > index + 1) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
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

private fun markdownToDisplayText(markdown: String): String {
    val renderedLines = mutableListOf<String>()
    var inCodeFence = false

    markdown.lines().forEach { rawLine ->
        val trimmed = rawLine.trimEnd()
        val normalized = trimmed.trim()

        if (normalized.startsWith("```")) {
            inCodeFence = !inCodeFence
            return@forEach
        }

        renderedLines += when {
            normalized.startsWith("#") -> normalized.trimStart('#', ' ')
            normalized.startsWith("- ") -> "• ${normalized.removePrefix("- ").trim()}"
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
    onClick: () -> Unit
) {
    Surface(
        color = CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = direction.glyph,
            color = TextSecondary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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

