package dev.kaixinguo.standalonecodepractice.ai

import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestCase
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestComparisonMode
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestSuite
import org.json.JSONArray
import org.json.JSONObject

internal class GeneratedTestSuiteJsonParser {
    fun parse(rawResponse: String): ProblemTestSuite {
        val payload = extractJsonPayload(rawResponse)
        val casesArray = when {
            payload.startsWith("{") -> JSONObject(payload).optJSONArray("cases")
            payload.startsWith("[") -> JSONArray(payload)
            else -> null
        } ?: error("AI response did not contain a cases array")

        val parsedCases = buildList {
            for (index in 0 until casesArray.length()) {
                val caseObject = casesArray.optJSONObject(index) ?: continue
                val sanitizedCase = sanitizeCaseFields(
                    stdin = caseObject.optString("stdin").trim(),
                    expectedOutput = caseObject.optString("expectedOutput").trim()
                )
                val stdin = sanitizedCase.stdin
                if (stdin.isBlank()) continue
                val rawLabel = caseObject.optString("label").trim()
                val comparisonModeValue = caseObject.optString("comparisonMode").trim()
                val acceptableOutputs = caseObject.optJSONArray("acceptableOutputs")
                    ?.toStringList()
                    .orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                add(
                    ProblemTestCase(
                        label = rawLabel.ifBlank { "Case ${index + 1}" },
                        stdin = stdin,
                        expectedOutput = sanitizedCase.expectedOutput,
                        enabled = caseObject.optBoolean("enabled", true),
                        comparisonMode = if (comparisonModeValue.isBlank()) {
                            ProblemTestComparisonMode.Exact
                        } else {
                            ProblemTestComparisonMode.fromStorage(comparisonModeValue)
                        },
                        acceptableOutputs = acceptableOutputs
                    )
                )
            }
        }.ifEmpty {
            error("AI response did not include any valid test cases")
        }

        return ProblemTestSuite(
            cases = parsedCases.mapIndexed { index, testCase ->
                testCase.copy(label = "Case ${index + 1}")
            }
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
        if (unfenced.startsWith("{") || unfenced.startsWith("[")) {
            return unfenced
        }

        val objectStart = unfenced.indexOf('{').takeIf { it >= 0 }
        val arrayStart = unfenced.indexOf('[').takeIf { it >= 0 }
        val start = listOfNotNull(objectStart, arrayStart).minOrNull()
            ?: error("AI response did not contain JSON")
        val objectEnd = unfenced.lastIndexOf('}').takeIf { it >= 0 }
        val arrayEnd = unfenced.lastIndexOf(']').takeIf { it >= 0 }
        val end = listOfNotNull(objectEnd, arrayEnd).maxOrNull()
            ?: error("AI response did not contain complete JSON")
        if (end < start) {
            error("AI response did not contain complete JSON")
        }
        return unfenced.substring(start, end + 1)
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (index in 0 until length()) {
                add(optString(index))
            }
        }
    }

    private fun sanitizeCaseFields(
        stdin: String,
        expectedOutput: String
    ): SanitizedGeneratedCase {
        val trimmedInput = stdin.trim()
        if (trimmedInput.isBlank()) {
            return SanitizedGeneratedCase(stdin = "", expectedOutput = expectedOutput)
        }

        val assignments = extractAssignments(trimmedInput)
        val trailingOutputAssignment = assignments.lastOrNull()
            ?.takeIf { assignment -> assignment.name in OUTPUT_ASSIGNMENT_NAMES }

        if (trailingOutputAssignment == null) {
            return SanitizedGeneratedCase(stdin = trimmedInput, expectedOutput = expectedOutput)
        }

        val sanitizedInput = trimmedInput.substring(0, trailingOutputAssignment.range.first).trim()
        if (sanitizedInput.isBlank()) {
            return SanitizedGeneratedCase(stdin = trimmedInput, expectedOutput = expectedOutput)
        }

        return SanitizedGeneratedCase(
            stdin = sanitizedInput,
            expectedOutput = expectedOutput.ifBlank { trailingOutputAssignment.value }
        )
    }

    private fun extractAssignments(rawInput: String): List<ExtractedAssignment> {
        val matches = Regex("""(?m)([A-Za-z_][A-Za-z0-9_]*)\s*=""").findAll(rawInput).toList()
        if (matches.isEmpty()) return emptyList()

        return matches.mapIndexedNotNull { index, match ->
            val name = match.groupValues[1].trim().lowercase()
            val valueStart = match.range.last + 1
            val valueEnd = matches.getOrNull(index + 1)?.range?.first ?: rawInput.length
            val value = rawInput.substring(valueStart, valueEnd)
                .trim()
                .removeSuffix(",")
                .trim()
            if (value.isBlank()) {
                null
            } else {
                ExtractedAssignment(
                    name = name,
                    value = value,
                    range = match.range.first until valueEnd
                )
            }
        }
    }

    private data class SanitizedGeneratedCase(
        val stdin: String,
        val expectedOutput: String
    )

    private data class ExtractedAssignment(
        val name: String,
        val value: String,
        val range: IntRange
    )

    private companion object {
        val OUTPUT_ASSIGNMENT_NAMES = setOf(
            "output",
            "expected",
            "expected_output",
            "expectedoutput",
            "answer",
            "result"
        )
    }
}
