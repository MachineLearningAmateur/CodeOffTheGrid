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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = CardBackgroundAlt,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
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

