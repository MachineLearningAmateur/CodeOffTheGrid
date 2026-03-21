package dev.kaixinguo.standalonecodepractice.ai

import dev.kaixinguo.standalonecodepractice.data.ProblemShareCodec
import dev.kaixinguo.standalonecodepractice.data.ProblemTestSuiteJsonCodec
import dev.kaixinguo.standalonecodepractice.data.SharedProblemFile
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemExecutionPipeline
import org.json.JSONArray
import org.json.JSONObject

internal class GeneratedProblemDraftJsonParser {
    fun parse(rawResponse: String): SharedProblemFile {
        val payload = extractJsonPayload(rawResponse)
        val root = JSONObject(payload)

        return if (root.has("problem") || root.has("schemaVersion") || root.has("type")) {
            ProblemShareCodec.decodeFromString(payload)
        } else {
            parseProblemObject(root)
        }
    }

    private fun parseProblemObject(problemJson: JSONObject): SharedProblemFile {
        val executionPipeline = problemJson.optString("executionPipeline")
            .takeIf { it.isNotBlank() }
            ?.let(ProblemExecutionPipeline::fromStorage)

        return SharedProblemFile(
            title = problemJson.optString("title").trim(),
            difficulty = problemJson.optString("difficulty").trim(),
            summary = problemJson.optString("summary").trim(),
            statementMarkdown = problemJson.optString("statementMarkdown").trim(),
            exampleInput = normalizeGeneratedExampleInput(problemJson.optString("exampleInput")),
            exampleOutput = problemJson.optString("exampleOutput"),
            starterCode = problemJson.optString("starterCode"),
            hints = problemJson.optJSONArray("hints")
                ?.toStringList()
                .orEmpty()
                .map { it.trim() }
                .filter { it.isNotBlank() },
            submissionTestSuite = ProblemTestSuiteJsonCodec.decodeFromJson(
                problemJson.optJSONObject("submissionTestSuite")
            ),
            executionPipeline = executionPipeline
        )
    }

    private fun extractJsonPayload(rawResponse: String): String {
        val trimmed = rawResponse.trim()
        val unfenced = trimmed
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        if (unfenced.startsWith("{")) {
            return unfenced
        }

        val objectStart = unfenced.indexOf('{').takeIf { it >= 0 }
            ?: error("AI response did not contain JSON")
        val objectEnd = unfenced.lastIndexOf('}').takeIf { it >= 0 }
            ?: error("AI response did not contain complete JSON")
        if (objectEnd < objectStart) {
            error("AI response did not contain complete JSON")
        }
        return unfenced.substring(objectStart, objectEnd + 1)
    }

    private fun normalizeGeneratedExampleInput(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return trimmed

        val withoutInputPrefix = trimmed.replaceFirst(
            Regex("""^(?i)(example\s+)?input\s*:\s*"""),
            ""
        )

        return withoutInputPrefix
            .lines()
            .map { line ->
                val normalizedLine = line.trim()
                val assignmentMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\s*:\s*(.+)$""")
                    .matchEntire(normalizedLine)
                if (assignmentMatch != null) {
                    "${assignmentMatch.groupValues[1]} = ${assignmentMatch.groupValues[2].trim()}"
                } else {
                    normalizedLine
                }
            }
            .joinToString("\n")
            .trim()
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (index in 0 until length()) {
                add(optString(index))
            }
        }
    }
}
