package dev.kaixinguo.codeoffthegrid.data

import dev.kaixinguo.codeoffthegrid.data.local.WorkspaceDocumentDao
import dev.kaixinguo.codeoffthegrid.data.local.WorkspaceDocumentEntity
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemListItem
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestSuite
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemWorkspaceDocument
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStroke

internal class WorkspaceDocumentRepository(
    private val workspaceDocumentDao: WorkspaceDocumentDao
) {
    suspend fun loadDocument(problem: ProblemListItem): ProblemWorkspaceDocument {
        val stored = workspaceDocumentDao.getByProblemId(problem.id)
        return if (stored != null) {
            ProblemWorkspaceDocument(
                problemId = stored.problemId,
                draftCode = stored.draftCode,
                customTestSuite = decodeCustomTestSuite(stored.customTests),
                sketches = WorkspaceSketchCodec.decodeFromString(stored.sketchesJson),
                recognizedCodeDraft = stored.recognizedCodeDraft
            )
        } else {
            ProblemWorkspaceDocument(
                problemId = problem.id,
                draftCode = problem.starterCode,
                customTestSuite = ProblemTestSuite(draft = problem.customTests),
                sketches = emptyList(),
                recognizedCodeDraft = ""
            )
        }
    }

    suspend fun saveDocument(
        problemId: String,
        draftCode: String,
        customTestSuite: ProblemTestSuite,
        sketches: List<SketchStroke>,
        recognizedCodeDraft: String
    ) {
        workspaceDocumentDao.upsert(
            WorkspaceDocumentEntity(
                problemId = problemId,
                draftCode = draftCode,
                customTests = encodeCustomTestSuite(customTestSuite),
                sketchesJson = WorkspaceSketchCodec.encodeToString(sketches),
                recognizedCodeDraft = recognizedCodeDraft,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    private fun encodeCustomTestSuite(customTestSuite: ProblemTestSuite): String {
        return ProblemTestSuiteJsonCodec.encodeToString(customTestSuite)
    }

    private fun decodeCustomTestSuite(customTests: String): ProblemTestSuite {
        if (customTests.isBlank()) return ProblemTestSuite()
        return ProblemTestSuiteJsonCodec.decodeFromString(customTests)
            ?: ProblemTestSuite(draft = customTests)
    }
}
