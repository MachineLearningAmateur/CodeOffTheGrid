package dev.kaixinguo.codeoffthegrid.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchPoint
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStroke
import dev.kaixinguo.codeoffthegrid.ui.workspace.SketchStrokeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSketchCodecTest {
    @Test
    fun decodeFromString_acceptsLegacySketchPayloadWithoutKindOrTimestamps() {
        val decoded = WorkspaceSketchCodec.decodeFromString(
            """
            [
              {
                "color": -1,
                "points": [
                  { "x": 10.0, "y": 20.0 },
                  { "x": 30.0, "y": 40.0 }
                ]
              }
            ]
            """.trimIndent()
        )

        assertEquals(1, decoded.size)
        assertEquals(SketchStrokeKind.Diagram, decoded.single().kind)
        assertEquals(2, decoded.single().points.size)
        assertTrue(decoded.single().points[1].timestampMillis > decoded.single().points[0].timestampMillis)
    }

    @Test
    fun encodeToString_roundTripsCodeStrokeKindAndTimestamps() {
        val sketches = listOf(
            SketchStroke(
                points = listOf(
                    SketchPoint(Offset(12f, 24f), 100L),
                    SketchPoint(Offset(18f, 28f), 120L)
                ),
                color = Color(0xFFE58BA5),
                kind = SketchStrokeKind.Code
            )
        )

        val decoded = WorkspaceSketchCodec.decodeFromString(
            WorkspaceSketchCodec.encodeToString(sketches)
        )

        assertEquals(sketches, decoded)
    }
}
