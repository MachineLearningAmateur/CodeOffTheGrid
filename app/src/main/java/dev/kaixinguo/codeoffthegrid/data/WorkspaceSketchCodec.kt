package dev.kaixinguo.codeoffthegrid.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchPoint
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStroke
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStrokeKind
import org.json.JSONArray
import org.json.JSONObject

internal object WorkspaceSketchCodec {
    fun encodeToString(sketches: List<SketchStroke>): String {
        val strokes = JSONArray()
        sketches.forEach { stroke ->
            val points = JSONArray()
            stroke.points.forEach { point ->
                points.put(
                    JSONObject()
                        .put("x", point.offset.x.toDouble())
                        .put("y", point.offset.y.toDouble())
                        .put("t", point.timestampMillis)
                )
            }
            strokes.put(
                JSONObject()
                    .put("color", stroke.color.toArgb())
                    .put("kind", stroke.kind.storageValue)
                    .put("points", points)
            )
        }
        return strokes.toString()
    }

    fun decodeFromString(sketchesJson: String): List<SketchStroke> {
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
                            SketchPoint(
                                offset = Offset(
                                    x = point.getDouble("x").toFloat(),
                                    y = point.getDouble("y").toFloat()
                                ),
                                timestampMillis = point.optLong(
                                    "t",
                                    ((index + 1L) * 1_000L) + pointIndex
                                )
                            )
                        )
                    }
                }
                if (points.isNotEmpty()) {
                    add(
                        SketchStroke(
                            points = points,
                            color = Color(stroke.getInt("color")),
                            kind = sketchStrokeKindFromStorage(
                                stroke.optString("kind", SketchStrokeKind.Diagram.storageValue)
                            )
                        )
                    )
                }
            }
        }
    }

    private val SketchStrokeKind.storageValue: String
        get() = when (this) {
            SketchStrokeKind.Diagram -> "diagram"
            SketchStrokeKind.Code -> "code"
        }

    private fun sketchStrokeKindFromStorage(value: String): SketchStrokeKind {
        return when (value.lowercase()) {
            "code" -> SketchStrokeKind.Code
            else -> SketchStrokeKind.Diagram
        }
    }
}
