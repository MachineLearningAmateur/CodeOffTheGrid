package dev.kaixinguo.standalonecodepractice.ai

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
        val hasVisibleLoops = Regex("""(?m)^\s*(for|while)\b""").containsMatchIn(code)
        val visibleReturnLines = Regex("""(?m)^\s*return\b.*$""")
            .findAll(code)
            .map { it.value.trim() }
            .take(3)
            .toList()
        val visibleFunctionNames = Regex("""(?m)^\s*def\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(""")
            .findAll(code)
            .map { it.groupValues[1] }
            .toList()
        val hasVisibleRecursion = visibleFunctionNames.any { functionName ->
            val escapedFunctionName = Regex.escape(functionName)
            Regex("""\b$escapedFunctionName\s*\(""").containsMatchIn(
                code.substringAfter("def $functionName", missingDelimiterValue = "")
            )
        }
        val hasVisibleComprehensions = Regex("""(?s)(\[[^\]]*\bfor\b[^\]]*])|(\{[^\}]*\bfor\b[^\}]*})""")
            .containsMatchIn(code)
        val hasVisibleSorting = Regex("""\bsorted\s*\(|\.sort\s*\(""").containsMatchIn(lowercaseCode)
        val hasVisibleSetOrDictConstruction = Regex("""\b(set|dict|counter|defaultdict)\s*\(""")
            .containsMatchIn(lowercaseCode)
        val hasVisibleHeapOrQueueOps = Regex("""\b(heapq|heappush|heappop|deque)\b""")
            .containsMatchIn(lowercaseCode)

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
}
