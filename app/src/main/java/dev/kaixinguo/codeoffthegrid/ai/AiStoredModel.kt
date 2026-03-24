package dev.kaixinguo.codeoffthegrid.ai

internal data class AiStoredModel(
    val path: String,
    val name: String,
    val preset: AiModelPreset? = null
)
