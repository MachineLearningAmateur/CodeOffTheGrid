package dev.kaixinguo.codeoffthegrid

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.kaixinguo.codeoffthegrid.data.LocalPythonExecutionService
import dev.kaixinguo.codeoffthegrid.ui.workspace.ExecutionStatus
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemExecutionPipeline
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestComparisonMode
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestCase
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestSuite
import dev.kaixinguo.codeoffthegrid.ui.workspace.TestCaseStatus
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
            testSuite = suite,
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

    @Test
    fun runCustomSuite_supportsBareStdlibNamesAndPublicSolutionMethods() = runBlocking {
        val groupSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Bare defaultdict",
                    stdin = """{"strs":["eat","tea","tan","ate","nat","bat"]}""",
                    expectedOutput = """[["eat","tea","ate"],["tan","nat"],["bat"]]""",
                    comparisonMode = ProblemTestComparisonMode.UnorderedNestedCollections
                )
            )
        )
        val groupCode = """
            class Solution:
                def groupAnagrams(self, strs):
                    groups = defaultdict(list)
                    for value in strs:
                        groups["".join(sorted(value))].append(value)
                    return list(groups.values())
        """.trimIndent()

        assertPassed(service.runCustomSuite(groupSuite, groupCode))

        val searchSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Use public search method",
                    stdin = """{"nums":[-1,0,3,5,9,12],"target":9}""",
                    expectedOutput = "4"
                )
            )
        )
        val searchCode = """
            class Solution:
                def binary_search(self, left, right, nums, target):
                    if left > right:
                        return -1
                    middle = left + (right - left) // 2
                    if nums[middle] == target:
                        return middle
                    if nums[middle] < target:
                        return self.binary_search(middle + 1, right, nums, target)
                    return self.binary_search(left, middle - 1, nums, target)

                def search(self, nums, target):
                    return self.binary_search(0, len(nums) - 1, nums, target)
        """.trimIndent()

        assertPassed(service.runCustomSuite(searchSuite, searchCode))
    }

    @Test
    fun runCustomSuite_supportsSeededExampleAssignmentSyntax() = runBlocking {
        val suite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Inline assignments",
                    stdin = """nums = [1, 2, 3, 3]""",
                    expectedOutput = "true"
                ),
                ProblemTestCase(
                    label = "Multiple assignments",
                    stdin = """nums = [3,4,5,6], target = 7""",
                    expectedOutput = "[0,1]"
                ),
                ProblemTestCase(
                    label = "Multiline assignment",
                    stdin = """
                        board =
                        [["5","3",".",".","7",".",".",".","."],
                         ["6",".",".","1","9","5",".",".","."],
                         [".","9","8",".",".",".",".","6","."],
                         ["8",".",".",".","6",".",".",".","3"],
                         ["4",".",".","8",".","3",".",".","1"],
                         ["7",".",".",".","2",".",".",".","6"],
                         [".","6",".",".",".",".","2","8","."],
                         [".",".",".","4","1","9",".",".","5"],
                         [".",".",".",".","8",".",".","7","9"]]
                    """.trimIndent(),
                    expectedOutput = "true"
                )
            )
        )
        val code = """
            class Solution:
                def containsDuplicate(self, nums):
                    return len(nums) != len(set(nums))

            def two_sum_impl(nums, target):
                lookup = {}
                for index, value in enumerate(nums):
                    needed = target - value
                    if needed in lookup:
                        return [lookup[needed], index]
                    lookup[value] = index

            class SolutionTwoSum:
                def twoSum(self, nums, target):
                    return two_sum_impl(nums, target)

            class SolutionValidSudoku:
                def isValidSudoku(self, board):
                    seen_rows = [set() for _ in range(9)]
                    seen_cols = [set() for _ in range(9)]
                    seen_boxes = [set() for _ in range(9)]
                    for row in range(9):
                        for col in range(9):
                            value = board[row][col]
                            if value == ".":
                                continue
                            box = (row // 3) * 3 + (col // 3)
                            if value in seen_rows[row] or value in seen_cols[col] or value in seen_boxes[box]:
                                return False
                            seen_rows[row].add(value)
                            seen_cols[col].add(value)
                            seen_boxes[box].add(value)
                    return True
        """.trimIndent()

        val containsDuplicateResult = service.runCustomSuite(
            suite.copy(cases = listOf(suite.cases[0])),
            """
                class Solution:
                    def containsDuplicate(self, nums):
                        return len(nums) != len(set(nums))
            """.trimIndent()
        )
        assertPassed(containsDuplicateResult)

        val twoSumResult = service.runCustomSuite(
            suite.copy(cases = listOf(suite.cases[1])),
            """
                class Solution:
                    def twoSum(self, nums, target):
                        lookup = {}
                        for index, value in enumerate(nums):
                            needed = target - value
                            if needed in lookup:
                                return [lookup[needed], index]
                            lookup[value] = index
            """.trimIndent()
        )
        assertPassed(twoSumResult)

        val sudokuResult = service.runCustomSuite(
            suite.copy(cases = listOf(suite.cases[2])),
            """
                class Solution:
                    def isValidSudoku(self, board):
                        rows = [set() for _ in range(9)]
                        cols = [set() for _ in range(9)]
                        boxes = [set() for _ in range(9)]
                        for row in range(9):
                            for col in range(9):
                                value = board[row][col]
                                if value == ".":
                                    continue
                                box = (row // 3) * 3 + (col // 3)
                                if value in rows[row] or value in cols[col] or value in boxes[box]:
                                    return False
                                rows[row].add(value)
                                cols[col].add(value)
                                boxes[box].add(value)
                        return True
            """.trimIndent()
        )
        assertPassed(sudokuResult)
    }

    @Test
    fun runCustomSuite_supportsEncodeDecodeRoundTrip() = runBlocking {
        val suite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Round trip",
                    stdin = """dummy_input = ["Hello","World"]""",
                    expectedOutput = """["Hello","World"]"""
                )
            )
        )
        val code = """
            class Solution:
                def encode(self, strs):
                    return "".join(f"{len(item)}#{item}" for item in strs)

                def decode(self, s):
                    result = []
                    index = 0
                    while index < len(s):
                        separator = s.index("#", index)
                        length = int(s[index:separator])
                        start = separator + 1
                        result.append(s[start:start + length])
                        index = start + length
                    return result
        """.trimIndent()

        val result = service.runCustomSuite(
            executionPipeline = ProblemExecutionPipeline.EncodeDecodeRoundTrip,
            testSuite = suite,
            draftCode = code
        )
        assertPassed(result)
        assertEquals(
            """["Hello","World"]""",
            result.cases.single().actualOutput.replace(" ", "")
        )
    }

    @Test
    fun runCustomSuite_supportsDesignProblemOperationSequences() = runBlocking {
        val suite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "MinStack",
                    stdin = """["MinStack", "push", 1, "push", 2, "getMin", "top", "pop", "getMin"]""",
                    expectedOutput = ""
                )
            )
        )
        val code = """
            class MinStack:
                def __init__(self):
                    self.values = []

                def push(self, val):
                    self.values.append(val)

                def pop(self):
                    self.values.pop()

                def top(self):
                    return self.values[-1]

                def getMin(self):
                    return min(self.values)
        """.trimIndent()

        val result = service.runCustomSuite(
            executionPipeline = ProblemExecutionPipeline.OperationSequence,
            testSuite = suite,
            draftCode = code
        )
        assertPassed(result)

        val twitterSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Bare defaultdict design problem",
                    stdin = """["Twitter","postTweet",1,5,"follow",1,2,"postTweet",2,6,"getNewsFeed",1]""",
                    expectedOutput = """[null,null,null,null,[6,5]]"""
                )
            )
        )
        val twitterCode = """
            class Twitter:
                def __init__(self):
                    self.time = 0
                    self.followMap = defaultdict(set)
                    self.tweetMap = defaultdict(list)

                def postTweet(self, userId, tweetId):
                    self.tweetMap[userId].append((self.time, tweetId))
                    self.time += 1

                def getNewsFeed(self, userId):
                    feed = self.tweetMap[userId][:]
                    for followeeId in self.followMap[userId]:
                        feed.extend(self.tweetMap[followeeId])
                    feed.sort(key=lambda item: -item[0])
                    return [tweetId for _, tweetId in feed[:10]]

                def follow(self, followerId, followeeId):
                    if followerId != followeeId:
                        self.followMap[followerId].add(followeeId)

                def unfollow(self, followerId, followeeId):
                    self.followMap[followerId].discard(followeeId)
        """.trimIndent()

        assertPassed(
            service.runCustomSuite(
                executionPipeline = ProblemExecutionPipeline.OperationSequence,
                testSuite = twitterSuite,
                draftCode = twitterCode
            )
        )
    }

    @Test
    fun runLocalSubmission_reportsBuiltInSubmissionSuiteResults() = runBlocking {
        val suite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Example 1",
                    stdin = """nums = [2,7,11,15], target = 9""",
                    expectedOutput = "[0,1]"
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

        val result = service.runLocalSubmission(
            testSuite = suite,
            draftCode = code,
            executionPipeline = ProblemExecutionPipeline.SingleMethod
        )

        assertPassed(result)
        assertTrue(result.summary.contains("built-in submission cases"))
    }

    @Test
    fun runCustomSuite_supportsUnorderedAndVariantComparators() = runBlocking {
        val unorderedSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Subsets",
                    stdin = "nums = [1, 2]",
                    expectedOutput = "[[],[1],[2],[1,2]]",
                    comparisonMode = ProblemTestComparisonMode.UnorderedNestedCollections
                )
            )
        )
        val unorderedCode = """
            class Solution:
                def subsets(self, nums):
                    return [[1, 2], [2], [1], []]
        """.trimIndent()
        assertPassed(service.runCustomSuite(unorderedSuite, unorderedCode))

        val oneOfSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Longest palindrome",
                    stdin = "s = \"babad\"",
                    expectedOutput = "bab",
                    comparisonMode = ProblemTestComparisonMode.OneOf,
                    acceptableOutputs = listOf("aba")
                )
            )
        )
        val oneOfCode = """
            class Solution:
                def longestPalindrome(self, s):
                    return "aba"
        """.trimIndent()
        assertPassed(service.runCustomSuite(oneOfSuite, oneOfCode))
    }

    @Test
    fun runCustomSuite_supportsTreeNodeAndGraphOrderingValidators() = runBlocking {
        val lcaSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "LCA",
                    stdin = "root = [2, 1, 3], p = 1, q = 3",
                    expectedOutput = "2",
                    comparisonMode = ProblemTestComparisonMode.TreeNodeValue
                )
            )
        )
        val lcaCode = """
            class Solution:
                def lowestCommonAncestor(self, root, p, q):
                    return root
        """.trimIndent()
        assertPassed(service.runCustomSuite(lcaSuite, lcaCode))

        val topoSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Topo",
                    stdin = "numCourses = 4, prerequisites = [[1, 0], [2, 0], [3, 1], [3, 2]]",
                    expectedOutput = "[0,1,2,3]",
                    comparisonMode = ProblemTestComparisonMode.TopologicalOrder
                )
            )
        )
        val topoCode = """
            class Solution:
                def findOrder(self, numCourses, prerequisites):
                    return [0, 2, 1, 3]
        """.trimIndent()
        assertPassed(service.runCustomSuite(topoSuite, topoCode))

        val alienSuite = ProblemTestSuite(
            cases = listOf(
                ProblemTestCase(
                    label = "Alien order",
                    stdin = """words = ["ab","adc"]""",
                    expectedOutput = "abdc",
                    comparisonMode = ProblemTestComparisonMode.AlienDictionaryOrder
                )
            )
        )
        val alienCode = """
            class Solution:
                def foreignDictionary(self, words):
                    return "acbd"
        """.trimIndent()
        assertPassed(service.runCustomSuite(alienSuite, alienCode))
    }

    private fun assertPassed(result: dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemExecutionResult) {
        if (result.status != ExecutionStatus.Passed) {
            fail(
                "Unexpected status=${result.status}, summary=${result.summary}, stdout=${result.stdout}, stderr=${result.stderr}, " +
                    "cases=${result.cases}"
            )
        }
        assertEquals(ExecutionStatus.Passed, result.status)
    }
}
