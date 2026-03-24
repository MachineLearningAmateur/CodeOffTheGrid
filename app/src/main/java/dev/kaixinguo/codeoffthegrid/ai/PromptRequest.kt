package dev.kaixinguo.codeoffthegrid.ai

internal data class PromptRequest(
    val mode: PromptMode,
    val problem: String,
    val code: String?,
    val userRequest: String?
)
