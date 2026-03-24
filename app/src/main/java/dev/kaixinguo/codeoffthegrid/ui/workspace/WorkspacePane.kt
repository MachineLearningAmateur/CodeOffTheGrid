package dev.kaixinguo.codeoffthegrid.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.BoxScope
import dev.kaixinguo.codeoffthegrid.R
import dev.kaixinguo.codeoffthegrid.ui.workspace.EraserSize
import dev.kaixinguo.codeoffthegrid.ui.workspace.PlainActionChip
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemListItem
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchPoint
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStroke
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStrokeKind
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchTool
import dev.kaixinguo.codeoffthegrid.ui.workspace.UnifiedWorkspaceSurface
import dev.kaixinguo.codeoffthegrid.ui.workspace.WorkspaceInputMode
import dev.kaixinguo.codeoffthegrid.ui.workspace.CardBlock
import dev.kaixinguo.codeoffthegrid.ui.workspace.ColorChip
import dev.kaixinguo.codeoffthegrid.ui.workspace.SelectorChip
import dev.kaixinguo.codeoffthegrid.ui.workspace.ToolChip
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentAmber
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentBlue
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentGreen
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentTeal
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBackgroundAlt
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBorder
import dev.kaixinguo.codeoffthegrid.ui.theme.PaneBackground
import dev.kaixinguo.codeoffthegrid.ui.theme.TextPrimary

@Composable
internal fun WorkspacePane(
    problem: ProblemListItem,
    draftCode: String,
    sketchStrokes: SnapshotStateList<SketchStroke>,
    onDraftCodeChange: (String) -> Unit,
    onSketchesChange: (List<SketchStroke>) -> Unit,
    recognizedCodeDraft: String,
    isRecognizingCode: Boolean,
    onConvertCode: () -> Unit,
    onOpenRecognizedCodeDraft: () -> Unit,
    inputMode: WorkspaceInputMode,
    onInputModeChange: (WorkspaceInputMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeStrokePoints by remember(problem.id) { mutableStateOf<List<SketchPoint>>(emptyList()) }
    var activeColor by remember(problem.id) { mutableStateOf(AccentBlue) }
    var sketchTool by remember(problem.id) { mutableStateOf(SketchTool.DiagramPen) }
    var selectedPenTool by remember(problem.id) { mutableStateOf(SketchTool.DiagramPen) }
    var colorMenuExpanded by remember(problem.id) { mutableStateOf(false) }
    var eraserMenuExpanded by remember(problem.id) { mutableStateOf(false) }
    var eraserSize by remember(problem.id) { mutableStateOf(EraserSize.Medium) }
    val hasCodeStrokes = remember(sketchStrokes.size, sketchStrokes.toList()) {
        sketchStrokes.any { it.kind == SketchStrokeKind.Code }
    }
    val sketchControlsEnabled = inputMode == WorkspaceInputMode.Sketch

    Surface(
        modifier = modifier,
        color = PaneBackground,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            CardBlock(
                title = "Workspace",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                trailing = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WorkspaceInputModeSwitch(
                            checked = inputMode == WorkspaceInputMode.Sketch,
                            onCheckedChange = { checked ->
                                colorMenuExpanded = false
                                eraserMenuExpanded = false
                                onInputModeChange(
                                    if (checked) WorkspaceInputMode.Sketch else WorkspaceInputMode.Keyboard
                                )
                            }
                        )
                        Row(
                            modifier = Modifier.alpha(if (sketchControlsEnabled) 1f else 0.42f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PenModeSwitch(
                                checked = selectedPenTool == SketchTool.CodePen,
                                activeTool = sketchTool,
                                onCheckedChange = { checked ->
                                    if (!sketchControlsEnabled) return@PenModeSwitch
                                    colorMenuExpanded = false
                                    eraserMenuExpanded = false
                                    selectedPenTool = if (checked) SketchTool.CodePen else SketchTool.DiagramPen
                                    sketchTool = selectedPenTool
                                }
                            )
                            ToolChip(
                                label = "Eraser",
                                selected = sketchTool == SketchTool.Eraser,
                                onClick = {
                                    if (!sketchControlsEnabled) return@ToolChip
                                    colorMenuExpanded = false
                                    eraserMenuExpanded = false
                                    sketchTool = if (sketchTool == SketchTool.Eraser) {
                                        selectedPenTool
                                    } else {
                                        SketchTool.Eraser
                                    }
                                }
                            )
                            if (sketchTool == SketchTool.DiagramPen) {
                                DiagramColorDropdown(
                                    selectedColor = activeColor,
                                    expanded = sketchControlsEnabled && colorMenuExpanded,
                                    enabled = sketchControlsEnabled,
                                    onExpandedChange = { colorMenuExpanded = it },
                                    onColorSelected = { color ->
                                        activeColor = color
                                        colorMenuExpanded = false
                                    }
                                )
                            } else if (sketchTool == SketchTool.CodePen) {
                                StaticSelectorShell {
                                    ColorChip(
                                        color = AccentTeal,
                                        selected = true,
                                        onClick = {}
                                    )
                                }
                            } else {
                                EraserSizeDropdown(
                                    selectedSize = eraserSize,
                                    expanded = sketchControlsEnabled && eraserMenuExpanded,
                                    enabled = sketchControlsEnabled,
                                    onExpandedChange = { eraserMenuExpanded = it },
                                    onSizeSelected = { size ->
                                        eraserSize = size
                                        eraserMenuExpanded = false
                                    }
                                )
                            }
                            PlainActionChip("Clear") {
                                if (!sketchControlsEnabled) return@PlainActionChip
                                sketchStrokes.clear()
                                onSketchesChange(emptyList())
                                activeStrokePoints = emptyList()
                            }
                        }
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    UnifiedWorkspaceSurface(
                        inputMode = inputMode,
                        pythonCode = draftCode,
                        onPythonCodeChange = onDraftCodeChange,
                        strokes = sketchStrokes,
                        onStrokesChange = onSketchesChange,
                        activeStrokePoints = activeStrokePoints,
                        onActiveStrokePointsChange = { activeStrokePoints = it },
                        activeColor = if (sketchTool == SketchTool.CodePen) AccentTeal else activeColor,
                        sketchTool = sketchTool,
                        eraserSize = eraserSize,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (inputMode == WorkspaceInputMode.Sketch) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            if (recognizedCodeDraft.isNotBlank()) {
                                PlainActionChip(
                                    label = "OCR Draft",
                                    accentColor = AccentBlue,
                                    onClick = onOpenRecognizedCodeDraft
                                )
                            }
                            PlainActionChip(
                                label = if (isRecognizingCode) "Converting..." else "Convert Code",
                                accentColor = if (hasCodeStrokes && !isRecognizingCode) AccentGreen else null,
                                onClick = if (hasCodeStrokes && !isRecognizingCode) {
                                    { onConvertCode() }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagramColorDropdown(
    selectedColor: Color,
    expanded: Boolean,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val orderedColors = listOf(selectedColor, AccentBlue, AccentGreen, AccentAmber).distinct()

    CompactOverlaySelector(
        expanded = expanded,
        enabled = enabled,
        contentDescription = "Show color choices",
        onExpandedChange = onExpandedChange,
        triggerContent = {
            ColorChip(
                color = selectedColor,
                selected = true,
                onClick = { if (enabled) onExpandedChange(!expanded) }
            )
        }
    ) {
        orderedColors.forEach { color ->
            ColorChip(
                color = color,
                selected = color == selectedColor,
                onClick = { if (enabled) onColorSelected(color) }
            )
        }
    }
}

@Composable
private fun EraserSizeDropdown(
    selectedSize: EraserSize,
    expanded: Boolean,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSizeSelected: (EraserSize) -> Unit
) {
    val orderedSizes = listOf(selectedSize) + EraserSize.entries.filter { it != selectedSize }

    CompactOverlaySelector(
        expanded = expanded,
        enabled = enabled,
        contentDescription = "Show eraser sizes",
        onExpandedChange = onExpandedChange,
        triggerContent = {
            EraserSizeDot(size = selectedSize, selected = true, onClick = {
                if (enabled) onExpandedChange(!expanded)
            })
        }
    ) {
        orderedSizes.forEach { size ->
            EraserSizeDot(
                size = size,
                selected = size == selectedSize,
                onClick = { if (enabled) onSizeSelected(size) }
            )
        }
    }
}

@Composable
private fun CompactOverlaySelector(
    expanded: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onExpandedChange: (Boolean) -> Unit,
    triggerContent: @Composable () -> Unit,
    menuContent: @Composable RowScope.() -> Unit
) {
    Box {
        CompactSelectorShell(
            onClick = { if (enabled) onExpandedChange(!expanded) }
        ) { triggerContent() }
        SelectorChevronOverlay(
            visible = true,
            contentDescription = contentDescription
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            Surface(
                color = PaneBackground,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    menuContent()
                }
            }
        }
    }
}

@Composable
private fun StaticSelectorShell(
    content: @Composable RowScope.() -> Unit
) {
    Box {
        CompactSelectorShell(
            onClick = null,
            content = content
        )
        SelectorChevronOverlay(
            visible = false,
            contentDescription = null
        )
    }
}

@Composable
private fun BoxScope.SelectorChevronOverlay(
    visible: Boolean,
    contentDescription: String?
) {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_down),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = 6.dp)
            .size(10.dp)
            .alpha(if (visible) 0.52f else 0f)
    )
}

@Composable
private fun CompactSelectorShell(
    onClick: (() -> Unit)?,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = PaneBackground,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun EraserSizeDot(
    size: EraserSize,
    selected: Boolean,
    onClick: () -> Unit
) {
    SelectorChip(
        selected = selected,
        onClick = onClick
    ) {
        Text(
            text = size.label,
            color = if (selected) TextPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun WorkspaceInputModeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ToggleLabeledSwitch(
        leftLabel = "Keyboard",
        rightLabel = "Sketch",
        checked = checked,
        leftAccentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        rightAccentColor = AccentBlue,
        trackAccentColor = AccentBlue,
        labelWidth = 64.dp,
        switchWidth = 108.dp,
        onCheckedChange = onCheckedChange
    )
}

@Composable
private fun PenModeSwitch(
    checked: Boolean,
    activeTool: SketchTool,
    onCheckedChange: (Boolean) -> Unit
) {
    val accentColor = when {
        activeTool == SketchTool.CodePen -> AccentTeal
        activeTool == SketchTool.DiagramPen -> AccentBlue
        else -> if (checked) AccentTeal else AccentBlue
    }

    ToggleLabeledSwitch(
        leftLabel = "Draw",
        rightLabel = "Code",
        checked = checked,
        leftAccentColor = AccentBlue,
        rightAccentColor = AccentTeal,
        trackAccentColor = accentColor,
        labelWidth = 40.dp,
        switchWidth = 86.dp,
        onCheckedChange = onCheckedChange
    )
}

@Composable
private fun ToggleLabeledSwitch(
    leftLabel: String,
    rightLabel: String,
    checked: Boolean,
    leftAccentColor: Color,
    rightAccentColor: Color,
    trackAccentColor: Color,
    labelWidth: androidx.compose.ui.unit.Dp,
    switchWidth: androidx.compose.ui.unit.Dp,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.width(switchWidth),
        color = PaneBackground,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, CardBorder),
        onClick = { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (checked) {
                ToggleActionLabel(
                    label = rightLabel,
                    accentColor = rightAccentColor,
                    width = labelWidth
                )
                ToggleThumb(trackAccentColor)
            } else {
                ToggleThumb(trackAccentColor)
                ToggleActionLabel(
                    label = leftLabel,
                    accentColor = leftAccentColor,
                    width = labelWidth
                )
            }
        }
    }
}

@Composable
private fun ToggleThumb(accentColor: Color) {
    Surface(
        color = accentColor.copy(alpha = 0.22f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 14.dp)
                    .padding(1.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = accentColor,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun ToggleActionLabel(
    label: String,
    accentColor: Color,
    width: androidx.compose.ui.unit.Dp,
) {
    Text(
        text = label,
        color = accentColor,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .width(width)
            .padding(vertical = 5.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

