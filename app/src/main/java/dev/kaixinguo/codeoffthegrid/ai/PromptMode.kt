package dev.kaixinguo.codeoffthegrid.ai

internal enum class PromptMode(
    val label: String,
    val description: String,
    val actionLabel: String
) {
    CREATE_PROBLEM(
        label = "Create Problem",
        description = "Help draft or refine the problem composer fields from the current draft.",
        actionLabel = "Generate Problem Draft"
    ),
    HINT(
        label = "Hint",
        description = "Small next-step guidance without the full answer.",
        actionLabel = "Ask for Hint"
    ),
    EXPLAIN(
        label = "Explain",
        description = "Walk through the idea, algorithm, and complexity.",
        actionLabel = "Explain Solution"
    ),
    REVIEW_CODE(
        label = "Review Code",
        description = "Review the current draft and explain correctness, runtime, and space complexity.",
        actionLabel = "Review Code"
    ),
    TEST_CASES(
        label = "Test Cases",
        description = "Generate structured custom tests and import them into the Custom tab.",
        actionLabel = "Generate Tests"
    ),
    SOLVE(
        label = "Solve",
        description = "Give the full approach and clean code when you're stuck.",
        actionLabel = "Show Solution"
    )
}
