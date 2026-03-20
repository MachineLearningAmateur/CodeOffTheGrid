package dev.kaixinguo.standalonecodepractice.ai

internal enum class AiModelPreset(
    val storageKey: String,
    val label: String,
    val shortLabel: String,
    val sizeLabel: String,
    val description: String,
    val repoId: String,
    val filename: String
) {
    QwenCoder15BQ4(
        storageKey = "qwen_coder_1_5b_q4_k_m",
        label = "Qwen2.5 Coder 1.5B Q4_K_M",
        shortLabel = "1.5B Q4",
        sizeLabel = "1.12 GB",
        description = "Smaller and lighter. Better fit for weaker devices.",
        repoId = "Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF",
        filename = "qwen2.5-coder-1.5b-instruct-q4_k_m.gguf"
    ),
    QwenCoder3BQ4(
        storageKey = "qwen_coder_3b_q4_k_m",
        label = "Qwen2.5 Coder 3B Q4_K_M",
        shortLabel = "3B Q4",
        sizeLabel = "2.1 GB",
        description = "Stronger answers, but heavier on storage and RAM.",
        repoId = "Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
        filename = "qwen2.5-coder-3b-instruct-q4_k_m.gguf"
    );

    val downloadUrl: String
        get() = "https://huggingface.co/$repoId/resolve/main/$filename?download=true"

    companion object {
        fun fromStorageKey(value: String?): AiModelPreset? {
            return entries.firstOrNull { it.storageKey == value }
        }
    }
}
