package dev.kaixinguo.standalonecodepractice.ui.workspace

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import java.util.UUID

internal data class ProblemListItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val difficulty: String = "",
    val active: Boolean = false,
    val solved: Boolean = false,
    val summary: String = "",
    val statementMarkdown: String = "",
    val exampleInput: String = "",
    val exampleOutput: String = "",
    val starterCode: String = "",
    val customTests: String = "",
    val hints: List<String> = emptyList()
)

internal data class ProblemSetState(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val problems: SnapshotStateList<ProblemListItem>
)

internal data class ProblemFolderState(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sets: SnapshotStateList<ProblemSetState>
)

internal data class DraggedProblemState(
    val sourceSetId: String,
    val problem: ProblemListItem,
    val pointer: Offset,
    val grabOffset: Offset,
    val previewSizePx: androidx.compose.ui.unit.IntSize
)

internal data class InsertionTarget(
    val setId: String,
    val index: Int
)

internal enum class SidebarMode(val label: String) {
    Problems("Problems"),
    AskAi("Ask AI"),
    Settings("Settings")
}

internal enum class ProblemFilter(val label: String) {
    All("All"),
    Open("Open"),
    Solved("Solved")
}

internal enum class WorkspaceInputMode {
    Keyboard,
    Sketch
}

internal enum class SupportTab(val label: String) {
    Problem("Problem"),
    Custom("Custom"),
    Results("Results")
}

internal enum class SketchTool {
    Pen,
    Eraser
}

internal enum class EraserSize(val label: String, val radius: Float) {
    Small("S", 16f),
    Medium("M", 28f),
    Large("L", 42f)
}

internal data class SketchStroke(
    val points: List<Offset>,
    val color: Color
)

internal data class ProblemTestCase(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Case",
    val stdin: String = "",
    val expectedOutput: String = "",
    val enabled: Boolean = true
)

internal data class ProblemTestSuite(
    val draft: String = "",
    val cases: List<ProblemTestCase> = emptyList()
)

internal enum class ExecutionTarget {
    Custom,
    LocalSubmission
}

internal enum class ExecutionStatus {
    Idle,
    Running,
    Passed,
    Failed,
    Error
}

internal enum class TestCaseStatus {
    Pending,
    Passed,
    Failed,
    Error,
    Skipped
}

internal data class ProblemTestCaseResult(
    val testCaseId: String,
    val label: String,
    val status: TestCaseStatus,
    val actualOutput: String = "",
    val expectedOutput: String = "",
    val errorOutput: String = "",
    val durationMillis: Long? = null
)

internal data class ProblemExecutionResult(
    val target: ExecutionTarget,
    val status: ExecutionStatus,
    val title: String,
    val summary: String,
    val cases: List<ProblemTestCaseResult> = emptyList(),
    val stdout: String = "",
    val stderr: String = ""
)

internal data class ProblemWorkspaceDocument(
    val problemId: String,
    val draftCode: String,
    val customTestSuite: ProblemTestSuite,
    val sketches: List<SketchStroke>
)

internal enum class ArrowDirection(val glyph: String) {
    Expand(">"),
    Collapse("<")
}

internal fun SidebarMode.shortLabel(): String = when (this) {
    SidebarMode.Problems -> "List"
    SidebarMode.AskAi -> "AI"
    SidebarMode.Settings -> "Set"
}

