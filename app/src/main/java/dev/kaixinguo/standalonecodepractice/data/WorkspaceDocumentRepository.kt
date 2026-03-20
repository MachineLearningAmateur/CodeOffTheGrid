package dev.kaixinguo.standalonecodepractice.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.kaixinguo.standalonecodepractice.data.local.WorkspaceDocumentDao
import dev.kaixinguo.standalonecodepractice.data.local.WorkspaceDocumentEntity
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemListItem
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestSuite
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemWorkspaceDocument
import dev.kaixinguo.standalonecodepractice.ui.workspace.SketchStroke
import org.json.JSONArray
import org.json.JSONObject

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
                sketches = decodeSketches(stored.sketchesJson)
            )
        } else {
            ProblemWorkspaceDocument(
                problemId = problem.id,
                draftCode = problem.starterCode,
                customTestSuite = ProblemTestSuite(draft = problem.customTests),
                sketches = emptyList()
            )
        }
    }

    suspend fun saveDocument(
        problemId: String,
        draftCode: String,
        customTestSuite: ProblemTestSuite,
        sketches: List<SketchStroke>
    ) {
        workspaceDocumentDao.upsert(
            WorkspaceDocumentEntity(
                problemId = problemId,
                draftCode = draftCode,
                customTests = encodeCustomTestSuite(customTestSuite),
                sketchesJson = encodeSketches(sketches),
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

    private fun encodeSketches(sketches: List<SketchStroke>): String {
        val strokes = JSONArray()
        sketches.forEach { stroke ->
            val points = JSONArray()
            stroke.points.forEach { point ->
                points.put(
                    JSONObject()
                        .put("x", point.x.toDouble())
                        .put("y", point.y.toDouble())
                )
            }
            strokes.put(
                JSONObject()
                    .put("color", stroke.color.toArgb())
                    .put("points", points)
            )
        }
        return strokes.toString()
    }

    private fun decodeSketches(sketchesJson: String): List<SketchStroke> {
        if (sketchesJson.isBlank()) return emptyList()

        val strokes = JSONArray(sketchesJson)
        return buildList {
            for (index in 0 until strokes.length()) {
                val stroke = strokes.getJSONObject(index)
                val pointsJson = stroke.getJSONArray("points")
                val points = buildList {
                    for (pointIndex in 0 until pointsJson.length()) {
                        val point = pointsJson.getJSONObject(pointIndex)
                        add(
                            Offset(
                                x = point.getDouble("x").toFloat(),
                                y = point.getDouble("y").toFloat()
                            )
                        )
                    }
                }
                if (points.isNotEmpty()) {
                    add(
                        SketchStroke(
                            points = points,
                            color = Color(stroke.getInt("color"))
                        )
                    )
                }
            }
        }
    }
}
