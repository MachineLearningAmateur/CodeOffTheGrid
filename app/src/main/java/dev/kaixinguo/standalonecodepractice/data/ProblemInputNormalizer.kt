package dev.kaixinguo.standalonecodepractice.data

import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemExecutionPipeline
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemListItem
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestSuite

internal object ProblemInputNormalizer {
    fun normalizeExampleInput(problem: ProblemListItem): String {
        return normalizeExampleInput(
            rawInput = problem.exampleInput,
            starterCode = problem.starterCode,
            executionPipeline = problem.executionPipeline
        )
    }

    fun normalizeExampleInput(
        rawInput: String,
        starterCode: String,
        executionPipeline: ProblemExecutionPipeline
    ): String {
        val trimmedInput = rawInput.trim()
        if (trimmedInput.isBlank()) return trimmedInput

        val parameterNames = when (executionPipeline) {
            ProblemExecutionPipeline.SingleMethod -> extractStarterParameterNames(
                starterCode = starterCode,
                preferredMethod = null
            )
            ProblemExecutionPipeline.EncodeDecodeRoundTrip -> extractStarterParameterNames(
                starterCode = starterCode,
                preferredMethod = "encode"
            )
            ProblemExecutionPipeline.SerializeDeserializeRoundTrip -> extractStarterParameterNames(
                starterCode = starterCode,
                preferredMethod = "serialize"
            )
            ProblemExecutionPipeline.OperationSequence -> emptyList()
        }

        if (parameterNames.isEmpty()) return trimmedInput

        val assignments = extractExampleAssignments(trimmedInput)
        if (assignments.isEmpty()) return trimmedInput
        if (assignments.map { it.name } == parameterNames.take(assignments.size)) return trimmedInput
        if (assignments.size != parameterNames.size) return trimmedInput

        return assignments.mapIndexed { index, assignment ->
            "${parameterNames[index]} = ${assignment.value}"
        }.joinToString("\n")
    }

    fun normalizeSubmissionTestSuite(
        problem: ProblemListItem,
        testSuite: ProblemTestSuite
    ): ProblemTestSuite {
        return normalizeSubmissionTestSuite(
            testSuite = testSuite,
            starterCode = problem.starterCode,
            executionPipeline = problem.executionPipeline
        )
    }

    fun normalizeSubmissionTestSuite(
        testSuite: ProblemTestSuite,
        starterCode: String,
        executionPipeline: ProblemExecutionPipeline
    ): ProblemTestSuite {
        return testSuite.copy(
            cases = testSuite.cases.map { testCase ->
                testCase.copy(
                    stdin = normalizeExampleInput(
                        rawInput = testCase.stdin,
                        starterCode = starterCode,
                        executionPipeline = executionPipeline
                    )
                )
            }
        )
    }

    private fun extractStarterParameterNames(
        starterCode: String,
        preferredMethod: String?
    ): List<String> {
        val lines = starterCode.lines()
        if (lines.isEmpty()) return emptyList()

        val methodRegex = Regex("""^\s*def\s+(\w+)\s*\(([^)]*)\)""")
        val methods = mutableListOf<Pair<String, String>>()
        var inSolutionClass = false

        lines.forEach { line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("class ")) {
                inSolutionClass = trimmed.startsWith("class Solution")
            }
            val match = methodRegex.find(line) ?: return@forEach
            if (line.startsWith("    ") || !line.startsWith(" ")) {
                if (inSolutionClass || !line.startsWith(" ")) {
                    methods += match.groupValues[1] to match.groupValues[2]
                }
            }
        }

        val target = when {
            preferredMethod != null -> methods.firstOrNull { it.first == preferredMethod }
            else -> methods.firstOrNull()
        } ?: return emptyList()

        return target.second
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "self" }
            .map { parameter ->
                parameter.substringBefore(':').substringBefore('=').trim()
            }
            .filter { it.isNotBlank() }
    }

    private fun extractExampleAssignments(rawInput: String): List<ExampleAssignment> {
        val matches = Regex("""(?m)([A-Za-z_][A-Za-z0-9_]*)\s*=""").findAll(rawInput).toList()
        if (matches.isEmpty()) return emptyList()

        return matches.mapIndexedNotNull { index, match ->
            val name = match.groupValues[1]
            val valueStart = match.range.last + 1
            val valueEnd = matches.getOrNull(index + 1)?.range?.first ?: rawInput.length
            val rawValue = rawInput.substring(valueStart, valueEnd)
                .trim()
                .removeSuffix(",")
                .trim()
            if (rawValue.isBlank()) {
                null
            } else {
                ExampleAssignment(name = name, value = rawValue)
            }
        }
    }

    private data class ExampleAssignment(
        val name: String,
        val value: String
    )
}
