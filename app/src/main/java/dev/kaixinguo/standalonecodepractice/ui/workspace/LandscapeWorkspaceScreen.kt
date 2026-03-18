package dev.kaixinguo.standalonecodepractice.ui.workspace

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.kaixinguo.standalonecodepractice.ui.theme.AppBackground
import dev.kaixinguo.standalonecodepractice.ui.theme.StandaloneLeetCodeTheme

@Composable
fun LandscapeWorkspaceScreen(modifier: Modifier = Modifier) {
    val folders = remember {
        mutableStateListOf<ProblemFolderState>().apply {
            addAll(sampleFolders())
        }
    }
    val initialSelectedSetId = remember(folders) { folders.firstOrNull()?.sets?.firstOrNull()?.id.orEmpty() }
    var selectedProblemTitle by remember { mutableStateOf("Tree Traversal") }
    var selectedProblemSetId by remember { mutableStateOf(initialSelectedSetId) }
    var sidebarMode by remember { mutableStateOf(SidebarMode.Problems) }
    var sidebarCollapsed by remember { mutableStateOf(false) }
    var workspaceInputMode by remember { mutableStateOf(WorkspaceInputMode.Keyboard) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarCollapsed) 84.dp else 288.dp,
        animationSpec = spring(),
        label = "sidebarWidth"
    )

    val allSets = folders.flatMap { it.sets }
    val selectedProblemSet = allSets
        .firstOrNull { it.id == selectedProblemSetId }
        ?: allSets.firstOrNull()
    val selectedProblem = selectedProblemSet?.problems
        ?.firstOrNull { it.title == selectedProblemTitle }
        ?: selectedProblemSet?.problems?.firstOrNull()
        ?: allSets.flatMap { it.problems }.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A202D), AppBackground)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SidebarPane(
                folders = folders,
                selectedProblemSetId = selectedProblemSet?.id.orEmpty(),
                selectedProblemTitle = selectedProblem?.title.orEmpty(),
                onProblemSetSelected = { setId ->
                    selectedProblemSetId = setId
                    val set = folders.flatMap { it.sets }.firstOrNull { it.id == setId }
                    if (set != null) {
                        val activeProblem = set.problems.firstOrNull { it.active } ?: set.problems.firstOrNull()
                        if (activeProblem != null) {
                            selectedProblemTitle = activeProblem.title
                        }
                    }
                },
                onProblemSelected = { setId, problemTitle ->
                    selectedProblemSetId = setId
                    selectedProblemTitle = problemTitle
                    val set = folders.flatMap { it.sets }.firstOrNull { it.id == setId }
                    if (set != null) {
                        val updated = set.problems.map { it.copy(active = it.title == problemTitle) }
                        set.problems.clear()
                        set.problems.addAll(updated)
                    }
                },
                onDeleteProblem = { setId, problem ->
                    val set = folders.flatMap { it.sets }.firstOrNull { it.id == setId } ?: return@SidebarPane
                    val removingSelected = selectedProblemSetId == setId && selectedProblemTitle == problem.title
                    set.problems.removeAll { it.title == problem.title }
                    if (removingSelected) {
                        val fallback = set.problems.firstOrNull()
                            ?: folders.flatMap { it.sets }.firstOrNull { it.problems.isNotEmpty() }?.problems?.firstOrNull()
                        if (fallback != null) {
                            selectedProblemSetId = allSets
                                .first { candidate -> candidate.problems.any { it.title == fallback.title } }.id
                            selectedProblemTitle = fallback.title
                        } else {
                            selectedProblemSetId = ""
                            selectedProblemTitle = ""
                        }
                    }
                },
                onCreateFolder = { folderTitle ->
                    folders.add(
                        ProblemFolderState(
                            title = folderTitle,
                            sets = mutableStateListOf()
                        )
                    )
                },
                onCreateSet = { folderTitle ->
                    val folder = folders.firstOrNull { it.title == folderTitle } ?: return@SidebarPane
                    val existingTitles = folder.sets.map { it.title }.toSet()
                    var index = 1
                    var candidate = "New Set"
                    while (candidate in existingTitles) {
                        index += 1
                        candidate = "New Set $index"
                    }
                    folder.sets.add(
                        ProblemSetState(
                            title = candidate,
                            problems = mutableStateListOf()
                        )
                    )
                    if (selectedProblemSet == null) {
                        selectedProblemSetId = folder.sets.last().id
                        selectedProblemTitle = ""
                    }
                },
                onDeleteSet = { setId ->
                    val folder = folders.firstOrNull { folderState ->
                        folderState.sets.any { it.id == setId }
                    } ?: return@SidebarPane
                    val deletingSelected = selectedProblemSetId == setId
                    folder.sets.removeAll { it.id == setId }
                    if (deletingSelected) {
                        val fallbackSet = folders.flatMap { it.sets }.firstOrNull()
                        val fallbackProblem = fallbackSet?.problems?.firstOrNull()
                        selectedProblemSetId = fallbackSet?.id.orEmpty()
                        selectedProblemTitle = fallbackProblem?.title.orEmpty()
                    }
                },
                onDeleteFolder = { folderTitle ->
                    val folderIndex = folders.indexOfFirst { it.title == folderTitle }
                    if (folderIndex == -1) return@SidebarPane
                    val deletingFolder = folders[folderIndex]
                    val deletingSelected = deletingFolder.sets.any { it.id == selectedProblemSetId }
                    folders.removeAt(folderIndex)
                    if (deletingSelected) {
                        val fallbackSet = folders.flatMap { it.sets }.firstOrNull()
                        val fallbackProblem = fallbackSet?.problems?.firstOrNull()
                        selectedProblemSetId = fallbackSet?.id.orEmpty()
                        selectedProblemTitle = fallbackProblem?.title.orEmpty()
                    }
                },
                onMoveProblem = { sourceSetId, targetSetId, problem, targetIndex ->
                    val sourceSet = folders.flatMap { it.sets }.firstOrNull { it.id == sourceSetId } ?: return@SidebarPane
                    val targetSet = folders.flatMap { it.sets }.firstOrNull { it.id == targetSetId } ?: return@SidebarPane
                    val sourceIndex = sourceSet.problems.indexOfFirst { it.title == problem.title }
                    if (sourceIndex == -1) return@SidebarPane
                    if (sourceSetId == targetSetId && (targetIndex == sourceIndex || targetIndex == sourceIndex + 1)) {
                        return@SidebarPane
                    }
                    sourceSet.problems.removeAll { it.title == problem.title }
                    val adjustedIndex = if (sourceSetId == targetSetId && sourceIndex < targetIndex) {
                        targetIndex - 1
                    } else {
                        targetIndex
                    }.coerceIn(0, targetSet.problems.size)
                    targetSet.problems.add(adjustedIndex, problem.copy(active = false))

                    if (selectedProblemTitle == problem.title && selectedProblemSetId == sourceSetId) {
                        selectedProblemSetId = targetSetId
                        selectedProblemTitle = problem.title
                    }
                    folders.flatMap { it.sets }.forEach { set ->
                        val updated = set.problems.map { item ->
                            item.copy(active = set.id == selectedProblemSetId && item.title == selectedProblemTitle)
                        }
                        set.problems.clear()
                        set.problems.addAll(updated)
                    }
                },
                selectedMode = sidebarMode,
                onModeSelected = { sidebarMode = it },
                collapsed = sidebarCollapsed,
                onToggleCollapsed = { sidebarCollapsed = !sidebarCollapsed },
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
            )
            if (selectedProblem != null) {
                WorkspacePane(
                    problem = selectedProblem,
                    inputMode = workspaceInputMode,
                    onInputModeChange = { workspaceInputMode = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                ProblemPane(
                    problem = selectedProblem,
                    modifier = Modifier
                        .width(356.dp)
                        .fillMaxHeight()
                )
            }
        }
    }
}

private fun sampleFolders(): List<ProblemFolderState> {
    return listOf(
        ProblemFolderState(
            title = "Interview Prep",
            sets = mutableStateListOf(
                ProblemSetState(
                    title = "Trees",
                    problems = mutableStateListOf(
                        ProblemListItem(
                            title = "Tree Traversal",
                            active = true,
                            solved = true,
                            summary = "Given a binary tree, return the nodes in pre-order, in-order, and post-order traversal.",
                            exampleInput = "root = [1, 2, 3, null, 4]",
                            exampleOutput = "Pre-order: [1, 2, 4, 3]\nIn-order: [2, 4, 1, 3]\nPost-order: [4, 2, 3, 1]",
                            starterCode = """
                                def preorder(root):
                                    if root is None:
                                        return []

                                    result = [root.val]
                                    result += preorder(root.left)
                                    result += preorder(root.right)
                                    return result
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "input": [1, 2, 3, None, 4],
                                    "expected": [1, 2, 4, 3]
                                }

                                case_2 = {
                                    "input": [],
                                    "expected": []
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "Remember the order of traversal methods.",
                                "Think about recursion or using a stack.",
                                "Pre-order visits root first. In-order centers the root. Post-order delays the root until both sides are explored."
                            )
                        ),
                        ProblemListItem(
                            title = "Path Sum",
                            solved = true,
                            summary = "Determine whether the tree has any root-to-leaf path whose values sum to a target.",
                            exampleInput = "root = [5,4,8,11,null,13,4,7,2], targetSum = 22",
                            exampleOutput = "True",
                            starterCode = """
                                def has_path_sum(root, target_sum):
                                    if root is None:
                                        return False
                                    if root.left is None and root.right is None:
                                        return root.val == target_sum
                                    remaining = target_sum - root.val
                                    return (
                                        has_path_sum(root.left, remaining) or
                                        has_path_sum(root.right, remaining)
                                    )
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "input": [5,4,8,11,None,13,4,7,2],
                                    "target": 22,
                                    "expected": True
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "Subtract the current node from the remaining target as you descend.",
                                "The base case matters most at leaf nodes.",
                                "This is simpler if you think in terms of remaining sum rather than accumulated sum."
                            )
                        ),
                        ProblemListItem(
                            title = "Lowest Common Ancestor",
                            summary = "Find the lowest node in a binary tree that has both targets in its subtree.",
                            exampleInput = "root = [3,5,1,6,2,0,8,null,null,7,4], p = 5, q = 1",
                            exampleOutput = "3",
                            starterCode = """
                                def lowest_common_ancestor(root, p, q):
                                    if root is None or root == p or root == q:
                                        return root
                                    left = lowest_common_ancestor(root.left, p, q)
                                    right = lowest_common_ancestor(root.right, p, q)
                                    if left and right:
                                        return root
                                    return left or right
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "input": [3,5,1,6,2,0,8,None,None,7,4],
                                    "p": 5,
                                    "q": 1,
                                    "expected": 3
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "A node can be the answer if one target is found on each side.",
                                "Return matches upward so parents can detect the split.",
                                "Short-circuit if the current node already equals one of the targets."
                            )
                        )
                    )
                ),
                ProblemSetState(
                    title = "Arrays",
                    problems = mutableStateListOf(
                        ProblemListItem(
                            title = "Two Sum",
                            solved = true,
                            summary = "Given an array of integers and a target, return the indices of the two numbers that add up to the target.",
                            exampleInput = "nums = [2, 7, 11, 15], target = 9",
                            exampleOutput = "[0, 1]",
                            starterCode = """
                                def two_sum(nums, target):
                                    seen = {}
                                    for index, value in enumerate(nums):
                                        complement = target - value
                                        if complement in seen:
                                            return [seen[complement], index]
                                        seen[value] = index
                                    return []
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "nums": [2, 7, 11, 15],
                                    "target": 9,
                                    "expected": [0, 1]
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "A hash map can trade space for time.",
                                "For each number, ask whether its complement has already been seen.",
                                "Store indices as you scan so you return the first valid pair immediately."
                            )
                        ),
                        ProblemListItem(
                            title = "Product Except Self",
                            summary = "Build an output array where each position contains the product of all other elements.",
                            exampleInput = "nums = [1, 2, 3, 4]",
                            exampleOutput = "[24, 12, 8, 6]",
                            starterCode = """
                                def product_except_self(nums):
                                    result = [1] * len(nums)
                                    prefix = 1
                                    for index in range(len(nums)):
                                        result[index] = prefix
                                        prefix *= nums[index]
                                    suffix = 1
                                    for index in range(len(nums) - 1, -1, -1):
                                        result[index] *= suffix
                                        suffix *= nums[index]
                                    return result
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "nums": [1, 2, 3, 4],
                                    "expected": [24, 12, 8, 6]
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "Think in terms of prefix and suffix products.",
                                "You can avoid division entirely.",
                                "A second pass from the right can fold in the suffix product."
                            )
                        )
                    )
                )
            )
        ),
        ProblemFolderState(
            title = "Graph Study",
            sets = mutableStateListOf(
                ProblemSetState(
                    title = "Graphs",
                    problems = mutableStateListOf(
                        ProblemListItem(
                            title = "Number of Islands",
                            summary = "Count the number of connected land masses in a 2D grid using graph traversal.",
                            exampleInput = "grid = [[1,1,0],[0,1,0],[1,0,1]]",
                            exampleOutput = "3",
                            starterCode = """
                                def num_islands(grid):
                                    rows = len(grid)
                                    cols = len(grid[0]) if rows else 0
                                    visited = set()

                                    def dfs(row, col):
                                        if (
                                            row < 0 or row >= rows or
                                            col < 0 or col >= cols or
                                            grid[row][col] == 0 or
                                            (row, col) in visited
                                        ):
                                            return
                                        visited.add((row, col))
                                        dfs(row + 1, col)
                                        dfs(row - 1, col)
                                        dfs(row, col + 1)
                                        dfs(row, col - 1)

                                    islands = 0
                                    for row in range(rows):
                                        for col in range(cols):
                                            if grid[row][col] == 1 and (row, col) not in visited:
                                                islands += 1
                                                dfs(row, col)
                                    return islands
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "grid": [[1,1,0],[0,1,0],[1,0,1]],
                                    "expected": 3
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "Treat the grid as a graph where adjacent land cells are connected.",
                                "Use DFS or BFS to mark an entire island once you find unvisited land.",
                                "The answer increments only when you start a traversal from a fresh land cell."
                            )
                        ),
                        ProblemListItem(
                            title = "Course Schedule",
                            summary = "Determine whether prerequisite dependencies contain a cycle.",
                            exampleInput = "numCourses = 2, prerequisites = [[1,0]]",
                            exampleOutput = "True",
                            starterCode = """
                                def can_finish(num_courses, prerequisites):
                                    graph = {course: [] for course in range(num_courses)}
                                    for course, prereq in prerequisites:
                                        graph[course].append(prereq)

                                    visiting = set()
                                    visited = set()

                                    def dfs(course):
                                        if course in visiting:
                                            return False
                                        if course in visited:
                                            return True
                                        visiting.add(course)
                                        for prereq in graph[course]:
                                            if not dfs(prereq):
                                                return False
                                        visiting.remove(course)
                                        visited.add(course)
                                        return True

                                    return all(dfs(course) for course in range(num_courses))
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "num_courses": 2,
                                    "prerequisites": [[1,0]],
                                    "expected": True
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "This is a cycle-detection problem on a directed graph.",
                                "DFS with a visiting set is a standard pattern.",
                                "Topological sort is another valid lens."
                            )
                        ),
                        ProblemListItem(
                            title = "Clone Graph",
                            solved = true,
                            summary = "Deep-copy an undirected graph starting from a given node.",
                            exampleInput = "adjList = [[2,4],[1,3],[2,4],[1,3]]",
                            exampleOutput = "A structurally identical deep copy",
                            starterCode = """
                                def clone_graph(node):
                                    if node is None:
                                        return None
                                    clones = {}

                                    def dfs(current):
                                        if current in clones:
                                            return clones[current]
                                        copy = Node(current.val)
                                        clones[current] = copy
                                        for neighbor in current.neighbors:
                                            copy.neighbors.append(dfs(neighbor))
                                        return copy

                                    return dfs(node)
                            """.trimIndent(),
                            customTests = """
                                case_1 = {
                                    "graph": [[2,4],[1,3],[2,4],[1,3]],
                                    "expected": "deep copy"
                                }
                            """.trimIndent(),
                            hints = listOf(
                                "You need a mapping from original nodes to cloned nodes.",
                                "DFS or BFS both work.",
                                "Be careful not to recreate the same node more than once."
                            )
                        )
                    )
                )
            )
        )
    )
}

@Preview(widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun LandscapeWorkspacePreview() {
    StandaloneLeetCodeTheme {
        LandscapeWorkspaceScreen()
    }
}
