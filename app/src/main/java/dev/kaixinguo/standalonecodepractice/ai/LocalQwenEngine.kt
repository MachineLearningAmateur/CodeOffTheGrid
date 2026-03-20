package dev.kaixinguo.standalonecodepractice.ai

internal interface LocalQwenEngine {
    suspend fun generate(prompt: String): String
}
