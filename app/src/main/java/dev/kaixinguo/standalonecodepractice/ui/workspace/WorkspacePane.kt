package dev.kaixinguo.standalonecodepractice.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentAmber
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentBlue
import dev.kaixinguo.standalonecodepractice.ui.theme.AccentGreen
import dev.kaixinguo.standalonecodepractice.ui.theme.CardBorder
import dev.kaixinguo.standalonecodepractice.ui.theme.PaneBackground

@Composable
internal fun WorkspacePane(
    problem: ProblemListItem,
    inputMode: WorkspaceInputMode,
    onInputModeChange: (WorkspaceInputMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var pythonCode by remember(problem.title) { mutableStateOf(problem.starterCode) }
    val strokes = remember(problem.title) { mutableStateListOf<SketchStroke>() }
    var activeStrokePoints by remember(problem.title) { mutableStateOf<List<Offset>>(emptyList()) }
    var activeColor by remember(problem.title) { mutableStateOf(AccentBlue) }
    var sketchTool by remember(problem.title) { mutableStateOf(SketchTool.Pen) }
    var eraserSize by remember(problem.title) { mutableStateOf(EraserSize.Medium) }

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
                        WorkspaceModeChip(
                            label = "Keyboard",
                            selected = inputMode == WorkspaceInputMode.Keyboard,
                            onClick = { onInputModeChange(WorkspaceInputMode.Keyboard) }
                        )
                        WorkspaceModeChip(
                            label = "Sketch",
                            selected = inputMode == WorkspaceInputMode.Sketch,
                            onClick = { onInputModeChange(WorkspaceInputMode.Sketch) }
                        )
                        if (inputMode == WorkspaceInputMode.Sketch) {
                            ToolChip(
                                label = "Draw",
                                selected = sketchTool == SketchTool.Pen,
                                onClick = { sketchTool = SketchTool.Pen }
                            )
                            ToolChip(
                                label = "Eraser",
                                selected = sketchTool == SketchTool.Eraser,
                                onClick = { sketchTool = SketchTool.Eraser }
                            )
                            if (sketchTool == SketchTool.Pen) {
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
                                strokes.clear()
                                activeStrokePoints = emptyList()
                            }
                        }
                    }
                }
            ) {
                UnifiedWorkspaceSurface(
                    inputMode = inputMode,
                    pythonCode = pythonCode,
                    onPythonCodeChange = { pythonCode = it },
                    strokes = strokes,
                    activeStrokePoints = activeStrokePoints,
                    onActiveStrokePointsChange = { activeStrokePoints = it },
                    activeColor = activeColor,
                    sketchTool = sketchTool,
                    eraserSize = eraserSize,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

