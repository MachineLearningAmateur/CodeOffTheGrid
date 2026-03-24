package dev.kaixinguo.codeoffthegrid.ai

import dev.kaixinguo.codeoffthegrid.data.SharedProblemFile
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestSuite

internal interface AiAssistant {
    suspend fun createProblem(problem: String, code: String?, request: String? = null): SharedProblemFile
    suspend fun generateHint(problem: String, code: String?, request: String? = null): String
    suspend fun explainSolution(problem: String, code: String?, request: String? = null): String
    suspend fun reviewCode(problem: String, code: String?, request: String? = null): String
    suspend fun generateCustomTests(problem: String, code: String?, request: String? = null): ProblemTestSuite
    suspend fun solveProblem(problem: String, code: String?, request: String? = null): String
}
