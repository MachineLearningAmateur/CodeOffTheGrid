package dev.kaixinguo.codeoffthegrid.ai

internal class DefaultPromptBuilder : PromptBuilder {
    override fun build(mode: PromptMode, problem: String, code: String?, userRequest: String?): String {
        val normalizedProblem = problem.trim().ifBlank { "No problem statement provided." }
        val normalizedCode = code?.trim()?.takeIf { it.isNotBlank() } ?: "No code provided."
        val normalizedRequest = userRequest?.trim()?.takeIf { it.isNotBlank() } ?: defaultRequestForMode(mode)
        var prompt = PromptTemplates.forMode(mode)
            .replace("{problem}", normalizedProblem)
            .replace("{code}", normalizedCode)
            .replace("{request}", normalizedRequest)

        if (mode == PromptMode.REVIEW_CODE) {
            prompt = prompt.replace(
                "{literal_draft_facts}",
                buildReviewLiteralDraftFacts(normalizedCode)
            )
        }

        return prompt
    }

    private fun defaultRequestForMode(mode: PromptMode): String {
        return when (mode) {
            PromptMode.CREATE_PROBLEM -> "Help me draft this problem and fill in the missing pieces."
            PromptMode.HINT -> "Give me a hint."
            PromptMode.EXPLAIN -> "Explain the solution in plain beginner-friendly English."
            PromptMode.REVIEW_CODE -> "Review my current code and explain its runtime and space complexity."
            PromptMode.TEST_CASES -> "Generate importable custom unit tests."
            PromptMode.SOLVE -> "Provide the solution."
        }
    }

    private fun buildReviewLiteralDraftFacts(code: String): String {
        if (code == "No code provided.") {
            return "- No code provided."
        }

        val lowercaseCode = code.lowercase()
        val trimmedLines = code.lineSequence()
            .map { it.trim() }
            .toList()
        val hasVisibleLoops = trimmedLines.any { line ->
            line.startsWith("for ") || line.startsWith("while ")
        }
        val visibleReturnLines = trimmedLines.asSequence()
            .filter { line -> line.startsWith("return") }
            .take(3)
            .toList()
        val visibleFunctionNames = trimmedLines.asSequence()
            .mapNotNull(::extractFunctionName)
            .toList()
        val hasVisibleRecursion = visibleFunctionNames.any { functionName ->
            code.substringAfter("def $functionName", missingDelimiterValue = "")
                .contains("$functionName(")
        }
        val hasVisibleComprehensions = trimmedLines.any { line ->
            !line.startsWith("for ") &&
                (line.contains('[') || line.contains('{')) &&
                line.contains(" for ")
        }
        val hasVisibleSorting = lowercaseCode.contains("sorted(") || lowercaseCode.contains(".sort(")
        val hasVisibleSetOrDictConstruction = listOf("set(", "dict(", "counter(", "defaultdict(")
            .any { token -> lowercaseCode.contains(token) }
        val hasVisibleHeapOrQueueOps = listOf("heapq", "heappush", "heappop", "deque")
            .any { token -> lowercaseCode.contains(token) }

        return buildString {
            appendLine("- Visible loops: ${yesNo(hasVisibleLoops)}")
            appendLine("- Visible recursion: ${yesNo(hasVisibleRecursion)}")
            appendLine("- Visible comprehensions: ${yesNo(hasVisibleComprehensions)}")
            appendLine("- Visible sorting calls: ${yesNo(hasVisibleSorting)}")
            appendLine("- Visible set or dict construction: ${yesNo(hasVisibleSetOrDictConstruction)}")
            appendLine("- Visible heap or queue operations: ${yesNo(hasVisibleHeapOrQueueOps)}")
            appendLine("- Visible direct return lines:")
            if (visibleReturnLines.isEmpty()) {
                appendLine("  none")
            } else {
                visibleReturnLines.forEach { returnLine ->
                    appendLine("  $returnLine")
                }
            }
            appendLine("- If no complexity-driving operations are visible, keep the review at O(1) time and O(1) auxiliary space.")
        }.trim()
    }

    private fun yesNo(value: Boolean): String {
        return if (value) "yes" else "no"
    }

    private fun extractFunctionName(line: String): String? {
        if (!line.startsWith("def ")) return null
        val signature = line.removePrefix("def ").substringBefore('(').trim()
        return signature.takeIf { candidate ->
            candidate.isNotBlank() &&
                (candidate.first().isLetter() || candidate.first() == '_') &&
                candidate.all { it.isLetterOrDigit() || it == '_' }
        }
    }
}
