package dev.kaixinguo.standalonecodepractice.ai

import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestSuite

internal interface AiAssistant {
    suspend fun createProblem(problem: String, code: String?, request: String? = null): String
    suspend fun generateHint(problem: String, code: String?, request: String? = null): String
    suspend fun explainSolution(problem: String, code: String?, request: String? = null): String
    suspend fun reviewCode(problem: String, code: String?, request: String? = null): String
    suspend fun generateCustomTests(problem: String, code: String?, request: String? = null): ProblemTestSuite
    suspend fun solveProblem(problem: String, code: String?, request: String? = null): String
}
