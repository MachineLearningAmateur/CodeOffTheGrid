package dev.kaixinguo.codeoffthegrid.ai

internal interface LocalQwenEngine {
    suspend fun generate(prompt: String): String
}
