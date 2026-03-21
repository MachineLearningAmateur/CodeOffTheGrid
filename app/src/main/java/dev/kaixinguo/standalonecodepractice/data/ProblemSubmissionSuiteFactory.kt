package dev.kaixinguo.standalonecodepractice.data

import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemListItem
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestCase
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestSuite

internal object ProblemSubmissionSuiteFactory {
    fun build(problem: ProblemListItem): ProblemTestSuite {
        return ProblemInputNormalizer.normalizeSubmissionTestSuite(
            problem = problem,
            testSuite = build(
                statementMarkdown = problem.statementMarkdown,
                exampleInput = problem.exampleInput,
                exampleOutput = problem.exampleOutput
            )
        )
    }

    fun build(
        statementMarkdown: String,
        exampleInput: String,
        exampleOutput: String
    ): ProblemTestSuite {
        val parsedCases = extractMarkdownExamples(statementMarkdown)
        val cases = if (parsedCases.isNotEmpty()) {
            parsedCases
        } else {
            listOfNotNull(
                ExampleCase(
                    input = exampleInput.trim(),
                    output = exampleOutput.trim()
                ).takeIf { it.input.isNotBlank() || it.output.isNotBlank() }
            )
        }

        return ProblemTestSuite(
            cases = cases.mapIndexed { index, example ->
                ProblemTestCase(
                    label = "Example ${index + 1}",
                    stdin = example.input,
                    expectedOutput = example.output
                )
            }
        )
    }

    private fun extractMarkdownExamples(statementMarkdown: String): List<ExampleCase> {
        val headingRegex = Regex("""(?im)^\*\*Example(?:\s+\d+)?:\*\*\s*$""")
        val matches = headingRegex.findAll(statementMarkdown).toList()
        if (matches.isEmpty()) return emptyList()

        return matches.mapNotNull { match ->
            val start = match.range.last + 1
            val end = matches
                .firstOrNull { it.range.first > match.range.first }
                ?.range
                ?.first
                ?: statementMarkdown.length
            val segment = statementMarkdown.substring(start, end)
            val exampleBlock = extractExampleBlock(segment)
            val input = extractLabelValue(exampleBlock, "Input")
            val output = extractLabelValue(exampleBlock, "Output")
            ExampleCase(input = input, output = output)
                .takeIf { it.input.isNotBlank() || it.output.isNotBlank() }
        }
    }

    private fun extractExampleBlock(segment: String): String {
        val fenceMatch = Regex("""(?s)```[^\n]*\n(.*?)```""").find(segment)
        return fenceMatch?.groupValues?.get(1) ?: segment
    }

    private fun extractLabelValue(source: String, label: String): String {
        val escapedLabel = Regex.escape(label)
        val labelRegex = Regex("""(?im)^\s*$escapedLabel:\s*""")
        val match = labelRegex.find(source) ?: return ""
        val labels = listOf("Input", "Output", "Explanation", "Constraints", "Note")
        val nextLabelRegex = Regex(
            labels
                .filterNot { it.equals(label, ignoreCase = true) }
                .joinToString(
                    prefix = """(?im)^\s*(?:""",
                    separator = "|",
                    postfix = """):\s*"""
                )
        )
        val start = match.range.last + 1
        val nextLabel = nextLabelRegex.find(source, start)
        val end = nextLabel?.range?.first ?: source.length
        return source.substring(start, end)
            .trim()
            .removeSuffix("```")
            .trim()
    }

    private data class ExampleCase(
        val input: String,
        val output: String
    )
}
