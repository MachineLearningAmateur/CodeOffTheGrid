package dev.kaixinguo.standalonecodepractice.ai

internal enum class AiRuntimePhase {
    Unconfigured,
    Importing,
    Downloading,
    Configured,
    Loading,
    Ready,
    Generating,
    Unloading,
    Error
}

internal data class AiRuntimeState(
    val phase: AiRuntimePhase = AiRuntimePhase.Unconfigured,
    val preset: AiModelPreset? = null,
    val modelName: String? = null,
    val detail: String? = null,
    val progress: Float? = null
)
