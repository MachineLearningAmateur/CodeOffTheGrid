package dev.kaixinguo.codeoffthegrid.data

import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemExecutionPipeline

internal object ProblemExecutionPipelineResolver {
    private val operationSequenceClassNames = setOf(
        "MinStack",
        "TimeMap",
        "LRUCache",
        "KthLargest",
        "Twitter",
        "MedianFinder",
        "PrefixTree",
        "Trie",
        "WordDictionary",
        "CountSquares"
    )

    fun infer(title: String, starterCode: String): ProblemExecutionPipeline {
        val normalizedTitle = title.trim().lowercase()
        if (normalizedTitle == "encode and decode strings") {
            return ProblemExecutionPipeline.EncodeDecodeRoundTrip
        }
        if (normalizedTitle == "serialize and deserialize binary tree") {
            return ProblemExecutionPipeline.SerializeDeserializeRoundTrip
        }

        val firstClassName = Regex("""(?m)^\s*class\s+(\w+)""")
            .find(starterCode)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()

        if (firstClassName in operationSequenceClassNames) {
            return ProblemExecutionPipeline.OperationSequence
        }

        return ProblemExecutionPipeline.SingleMethod
    }
}
