package dev.kaixinguo.standalonecodepractice.ai

internal interface PromptBuilder {
    fun build(mode: PromptMode, problem: String, code: String?, userRequest: String?): String

    fun build(request: PromptRequest): String {
        return build(
            mode = request.mode,
            problem = request.problem,
            code = request.code,
            userRequest = request.userRequest
        )
    }
}
