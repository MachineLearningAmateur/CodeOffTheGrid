package dev.kaixinguo.codeoffthegrid

import dev.kaixinguo.codeoffthegrid.data.ProblemInputNormalizer
import dev.kaixinguo.codeoffthegrid.data.ProblemSubmissionSuiteFactory
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemExecutionPipeline
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemListItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ProblemInputNormalizerTest {
    @Test
    fun normalizeExampleInput_renamesRoundTripDummyInputToStarterParam() {
        val problem = ProblemListItem(
            title = "Encode and Decode Strings",
            exampleInput = """dummy_input = ["Hello","World"]""",
            starterCode = """
                class Solution:
                    def encode(self, strs: List[str]) -> str:
                        return ""

                    def decode(self, s: str) -> List[str]:
                        return []
            """.trimIndent(),
            executionPipeline = ProblemExecutionPipeline.EncodeDecodeRoundTrip
        )

        assertEquals(
            """strs = ["Hello","World"]""",
            ProblemInputNormalizer.normalizeExampleInput(problem)
        )
    }

    @Test
    fun normalizeExampleInput_renamesSingleMethodAliasesToStarterParams() {
        assertEquals(
            "heights = [1,7,2,5,4,7,3,6]",
            ProblemInputNormalizer.normalizeExampleInput(
                rawInput = "height = [1,7,2,5,4,7,3,6]",
                starterCode = """
                    class Solution:
                        def maxArea(self, heights: List[int]) -> int:
                            return 0
                """.trimIndent(),
                executionPipeline = ProblemExecutionPipeline.SingleMethod
            )
        )
    }

    @Test
    fun build_problemSubmissionSuite_normalizesMarkdownExamples() {
        val problem = ProblemListItem(
            title = "Encode and Decode Strings",
            statementMarkdown = """
                Implement the encode and decode methods.

                **Example 1:**

                ```java
                Input: dummy_input = [""]

                Output: [""]
                ```
            """.trimIndent(),
            starterCode = """
                class Solution:
                    def encode(self, strs: List[str]) -> str:
                        return ""

                    def decode(self, s: str) -> List[str]:
                        return []
            """.trimIndent(),
            executionPipeline = ProblemExecutionPipeline.EncodeDecodeRoundTrip
        )

        val suite = ProblemSubmissionSuiteFactory.build(problem)

        assertEquals("""strs = [""]""", suite.cases.single().stdin)
    }
}
