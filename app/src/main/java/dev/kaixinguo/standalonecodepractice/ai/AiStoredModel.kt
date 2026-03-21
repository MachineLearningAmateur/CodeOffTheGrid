package dev.kaixinguo.standalonecodepractice.ai

internal data class AiStoredModel(
    val path: String,
    val name: String,
    val preset: AiModelPreset? = null
)
