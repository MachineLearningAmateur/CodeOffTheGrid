package dev.kaixinguo.codeoffthegrid.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import dev.kaixinguo.codeoffthegrid.R
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentAmber
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentBlue
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentGreen
import dev.kaixinguo.codeoffthegrid.ui.theme.AccentTeal
import dev.kaixinguo.codeoffthegrid.ui.theme.CardBorder
import dev.kaixinguo.codeoffthegrid.ui.theme.PaneBackground

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
    var modeMenuExpanded by remember(problem.id) { mutableStateOf(false) }
    var penMenuExpanded by remember(problem.id) { mutableStateOf(false) }
    var eraserSize by remember(problem.id) { mutableStateOf(EraserSize.Medium) }
    val hasCodeStrokes = remember(sketchStrokes.size, sketchStrokes.toList()) {
        sketchStrokes.any { it.kind == SketchStrokeKind.Code }
    }

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
                        Box {
                            PlainActionChip(
                                label = if (inputMode == WorkspaceInputMode.Sketch) "Sketch" else "Keyboard",
                                iconRes = R.drawable.ic_chevron_down,
                                accentColor = if (inputMode == WorkspaceInputMode.Sketch) AccentBlue else null,
                                onClick = { modeMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = modeMenuExpanded,
                                onDismissRequest = { modeMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { androidx.compose.material3.Text("Keyboard") },
                                    onClick = {
                                        onInputModeChange(WorkspaceInputMode.Keyboard)
                                        modeMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { androidx.compose.material3.Text("Sketch") },
                                    onClick = {
                                        onInputModeChange(WorkspaceInputMode.Sketch)
                                        modeMenuExpanded = false
                                    }
                                )
                            }
                        }
                        if (inputMode == WorkspaceInputMode.Sketch) {
                            Box {
                                PlainActionChip(
                                    label = if (selectedPenTool == SketchTool.CodePen) "Code" else "Draw",
                                    iconRes = R.drawable.ic_chevron_down,
                                    accentColor = when {
                                        sketchTool == SketchTool.CodePen -> AccentTeal
                                        sketchTool == SketchTool.DiagramPen -> AccentBlue
                                        else -> null
                                    },
                                    onClick = { penMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = penMenuExpanded,
                                    onDismissRequest = { penMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { androidx.compose.material3.Text("Draw") },
                                        onClick = {
                                            selectedPenTool = SketchTool.DiagramPen
                                            sketchTool = SketchTool.DiagramPen
                                            penMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { androidx.compose.material3.Text("Code") },
                                        onClick = {
                                            selectedPenTool = SketchTool.CodePen
                                            sketchTool = SketchTool.CodePen
                                            penMenuExpanded = false
                                        }
                                    )
                                }
                            }
                            ToolChip(
                                label = "Eraser",
                                selected = sketchTool == SketchTool.Eraser,
                                onClick = { sketchTool = SketchTool.Eraser }
                            )
                            if (sketchTool == SketchTool.DiagramPen) {
                                ColorChip(
                                    color = AccentBlue,
                                    selected = activeColor == AccentBlue,
                                    onClick = { activeColor = AccentBlue }
                                )
                                ColorChip(
                                    color = AccentGreen,
                                    selected = activeColor == AccentGreen,
                                    onClick = { activeColor = AccentGreen }
                                )
                                ColorChip(
                                    color = AccentAmber,
                                    selected = activeColor == AccentAmber,
                                    onClick = { activeColor = AccentAmber }
                                )
                            } else if (sketchTool == SketchTool.CodePen) {
                                ColorChip(
                                    color = AccentTeal,
                                    selected = true,
                                    onClick = {}
                                )
                            } else {
                                EraserSize.entries.forEach { size ->
                                    EraserSizeChip(
                                        size = size,
                                        selected = eraserSize == size,
                                        onClick = { eraserSize = size }
                                    )
                                }
                            }
                            PlainActionChip("Clear") {
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

