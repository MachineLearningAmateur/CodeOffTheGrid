package dev.kaixinguo.codeoffthegrid.ai

import dev.kaixinguo.codeoffthegrid.data.SharedProblemFile

internal class DefaultAiAssistant(
    private val promptBuilder: PromptBuilder,
    private val localQwenEngine: LocalQwenEngine,
    private val generatedTestSuiteJsonParser: GeneratedTestSuiteJsonParser = GeneratedTestSuiteJsonParser(),
    private val generatedProblemDraftJsonParser: GeneratedProblemDraftJsonParser = GeneratedProblemDraftJsonParser()
) : AiAssistant {
    override suspend fun createProblem(problem: String, code: String?, request: String?): SharedProblemFile {
        return generatedProblemDraftJsonParser.parse(
            generate(
            mode = PromptMode.CREATE_PROBLEM,
            problem = problem,
            code = code,
            request = request
            )
        )
    }

    override suspend fun generateHint(problem: String, code: String?, request: String?): String {
        return generate(
            mode = PromptMode.HINT,
            problem = problem,
            code = code,
            request = request
        )
    }

    override suspend fun explainSolution(problem: String, code: String?, request: String?): String {
        return generate(
            mode = PromptMode.EXPLAIN,
            problem = problem,
            code = code,
            request = request
        )
    }

    override suspend fun reviewCode(problem: String, code: String?, request: String?): String {
        return generate(
            mode = PromptMode.REVIEW_CODE,
            problem = problem,
            code = code,
            request = request
        )
    }

    override suspend fun generateCustomTests(problem: String, code: String?, request: String?) =
        generatedTestSuiteJsonParser.parse(
            generate(
                mode = PromptMode.TEST_CASES,
                problem = problem,
                code = code,
                request = request
            )
        )

    override suspend fun solveProblem(problem: String, code: String?, request: String?): String {
        return generate(
            mode = PromptMode.SOLVE,
            problem = problem,
            code = code,
            request = request
        )
    }

    private suspend fun generate(mode: PromptMode, problem: String, code: String?, request: String?): String {
        return localQwenEngine.generate(
            promptBuilder.build(
                mode = mode,
                problem = problem,
                code = code,
                userRequest = request
            )
        )
    }
}
