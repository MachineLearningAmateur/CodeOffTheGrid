package dev.kaixinguo.standalonecodepractice.ai

internal class DefaultAiAssistant(
    private val promptBuilder: PromptBuilder,
    private val localQwenEngine: LocalQwenEngine,
    private val generatedTestSuiteJsonParser: GeneratedTestSuiteJsonParser = GeneratedTestSuiteJsonParser()
) : AiAssistant {
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
