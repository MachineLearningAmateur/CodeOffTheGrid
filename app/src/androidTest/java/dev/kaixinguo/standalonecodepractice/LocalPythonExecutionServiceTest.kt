package dev.kaixinguo.standalonecodepractice

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.kaixinguo.standalonecodepractice.data.LocalPythonExecutionService
import dev.kaixinguo.standalonecodepractice.ui.workspace.ExecutionStatus
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestCase
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestSuite
import dev.kaixinguo.standalonecodepractice.ui.workspace.TestCaseStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalPythonExecutionServiceTest {
    private val service by lazy {
        LocalPythonExecutionService(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun runCustomSuite_executesPythonAgainstStructuredCases() = runBlocking {
        val suite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Pair exists",
                    stdin = """{"nums":[2,7,11,15],"target":9}""",
                    expectedOutput = "[0,1]"
                ),
                ProblemTestCase(
                    label = "Expression mode",
                    stdin = """expr: Solution().twoSum([3, 2, 4], 6)""",
                    expectedOutput = "[1,2]"
                )
            )
        )
        val code = """
            class Solution:
                def twoSum(self, nums, target):
                    lookup = {}
                    for index, value in enumerate(nums):
                        needed = target - value
                        if needed in lookup:
                            return [lookup[needed], index]
                        lookup[value] = index
        """.trimIndent()

        val result = service.runCustomSuite(
            customTestSuite = suite,
            draftCode = code
        )

        assertPassed(result)
        assertEquals(2, result.cases.count { it.status == TestCaseStatus.Passed })
        assertTrue(result.summary.contains("2 enabled custom cases passed"))
    }

    @Test
    fun runCustomSuite_supportsLinkedListsTreesAndIntervals() = runBlocking {
        val suite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Reverse linked list",
                    stdin = """{"head":[1,2,3,4]}""",
                    expectedOutput = "[4,3,2,1]"
                )
            )
        )
        val linkedListCode = """
            class Solution:
                def reverseList(self, head):
                    prev = None
                    current = head
                    while current:
                        nxt = current.next
                        current.next = prev
                        prev = current
                        current = nxt
                    return prev
        """.trimIndent()

        val linkedListResult = service.runCustomSuite(suite, linkedListCode)
        assertPassed(linkedListResult)

        val treeSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Tree depth",
                    stdin = """{"root":[3,9,20,null,null,15,7]}""",
                    expectedOutput = "3"
                )
            )
        )
        val treeCode = """
            class Solution:
                def maxDepth(self, root):
                    if not root:
                        return 0
                    return 1 + max(self.maxDepth(root.left), self.maxDepth(root.right))
        """.trimIndent()

        val treeResult = service.runCustomSuite(treeSuite, treeCode)
        assertPassed(treeResult)

        val intervalSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Meeting rooms",
                    stdin = """{"intervals":[[0,30],[5,10],[15,20]]}""",
                    expectedOutput = "false"
                )
            )
        )
        val intervalCode = """
            class Solution:
                def canAttendMeetings(self, intervals):
                    ordered = sorted(intervals, key=lambda item: item.start)
                    for index in range(1, len(ordered)):
                        if ordered[index].start < ordered[index - 1].end:
                            return False
                    return True
        """.trimIndent()

        val intervalResult = service.runCustomSuite(intervalSuite, intervalCode)
        assertPassed(intervalResult)
    }

    @Test
    fun runCustomSuite_supportsGraphsRandomPointersAndImports() = runBlocking {
        val graphSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Clone graph",
                    stdin = """{"node":[[2],[1,3],[2]]}""",
                    expectedOutput = "[[2],[1,3],[2]]"
                )
            )
        )
        val graphCode = """
            class Solution:
                def cloneGraph(self, node):
                    if not node:
                        return None
                    clones = {node: Node(node.val)}
                    queue = [node]
                    while queue:
                        current = queue.pop(0)
                        for neighbor in current.neighbors:
                            if neighbor not in clones:
                                clones[neighbor] = Node(neighbor.val)
                                queue.append(neighbor)
                            clones[current].neighbors.append(clones[neighbor])
                    return clones[node]
        """.trimIndent()

        val graphResult = service.runCustomSuite(graphSuite, graphCode)
        assertPassed(graphResult)

        val randomListSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Copy random list",
                    stdin = """{"head":[[3,null],[7,0],[4,1]]}""",
                    expectedOutput = "[[3,null],[7,0],[4,1]]"
                )
            )
        )
        val randomListCode = """
            class Solution:
                def copyRandomList(self, head):
                    if not head:
                        return None
                    mapping = {}
                    current = head
                    while current:
                        mapping[current] = Node(current.val)
                        current = current.next
                    current = head
                    while current:
                        mapping[current].next = mapping.get(current.next)
                        mapping[current].random = mapping.get(current.random)
                        current = current.next
                    return mapping[head]
        """.trimIndent()

        val randomListResult = service.runCustomSuite(randomListSuite, randomListCode)
        assertPassed(randomListResult)

        val importSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Standard imports",
                    stdin = """{"nums":[1,2,2,3,3,3]}""",
                    expectedOutput = "{\"1\":1,\"2\":2,\"3\":3}"
                )
            )
        )
        val importCode = """
            from collections import defaultdict

            class Solution:
                def frequencyMap(self, nums):
                    counts = defaultdict(int)
                    for value in nums:
                        counts[value] += 1
                    return {str(key): counts[key] for key in sorted(counts)}
        """.trimIndent()

        val importResult = service.runCustomSuite(importSuite, importCode)
        assertPassed(importResult)
    }

    private fun assertPassed(result: dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemExecutionResult) {
        if (result.status != ExecutionStatus.Passed) {
            fail(
                "Unexpected status=${result.status}, summary=${result.summary}, stdout=${result.stdout}, stderr=${result.stderr}, " +
                    "cases=${result.cases}"
            )
        }
        assertEquals(ExecutionStatus.Passed, result.status)
    }
}
