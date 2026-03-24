package dev.kaixinguo.codeoffthegrid.data

import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemExecutionPipeline
import java.util.Locale

internal object ProblemStarterCodeFactory {
    fun build(
        title: String,
        statementMarkdown: String,
        exampleInput: String,
        exampleOutput: String,
        executionPipeline: ProblemExecutionPipeline
    ): String {
        return when (executionPipeline) {
            ProblemExecutionPipeline.SingleMethod -> buildSingleMethodStarter(
                title = title,
                statementMarkdown = statementMarkdown,
                exampleInput = exampleInput,
                exampleOutput = exampleOutput
            )
            ProblemExecutionPipeline.EncodeDecodeRoundTrip -> buildEncodeDecodeStarter()
            ProblemExecutionPipeline.SerializeDeserializeRoundTrip -> buildSerializeDeserializeStarter()
            ProblemExecutionPipeline.OperationSequence -> buildOperationSequenceStarter(title)
        }
    }

    private fun buildSingleMethodStarter(
        title: String,
        statementMarkdown: String,
        exampleInput: String,
        exampleOutput: String
    ): String {
        val assignments = parseExampleAssignments(exampleInput)
        val helperTypes = helperTypesFor(
            title = title,
            statementMarkdown = statementMarkdown,
            assignments = assignments
        )
        val parameters = assignments.map { assignment ->
            val typeHint = inferParameterType(
                parameterName = assignment.name,
                value = assignment.value,
                helperTypes = helperTypes
            )
            "${assignment.name}: $typeHint"
        }.ifEmpty {
            listOf("input_data: Any")
        }
        val returnType = inferReturnType(
            value = exampleOutput.trim(),
            helperTypes = helperTypes
        )

        return buildString {
            append(helperTypeComments(helperTypes))
            append("class Solution:\n")
            append("    def ")
            append(methodNameFromTitle(title))
            append("(self, ")
            append(parameters.joinToString(", "))
            append(") -> ")
            append(returnType)
            append(":\n")
            append("        return\n")
        }
    }

    private fun buildEncodeDecodeStarter(): String {
        return """
            class Solution:
                def encode(self, strs: List[str]) -> str:
                    return ""

                def decode(self, s: str) -> List[str]:
                    return []
        """.trimIndent() + "\n"
    }

    private fun buildSerializeDeserializeStarter(): String {
        return """
            # Definition for a binary tree node.
            # class TreeNode:
            #     def __init__(self, val=0, left=None, right=None):
            #         self.val = val
            #         self.left = left
            #         self.right = right

            class Codec:
                def serialize(self, root: Optional[TreeNode]) -> str:
                    return ""

                def deserialize(self, data: str) -> Optional[TreeNode]:
                    return None
        """.trimIndent() + "\n"
    }

    private fun buildOperationSequenceStarter(title: String): String {
        return """
            class ${classNameFromTitle(title)}:
                def __init__(self):
                    return
        """.trimIndent() + "\n"
    }

    private fun helperTypesFor(
        title: String,
        statementMarkdown: String,
        assignments: List<ParsedAssignment>
    ): Set<HelperType> {
        val text = "$title\n$statementMarkdown".lowercase(Locale.US)
        val helperTypes = linkedSetOf<HelperType>()

        if ("linked list" in text) {
            helperTypes += HelperType.ListNode
        }
        if ("binary tree" in text || "tree node" in text) {
            helperTypes += HelperType.TreeNode
        }
        if ("graph" in text) {
            helperTypes += HelperType.Node
        }
        if ("interval" in text) {
            helperTypes += HelperType.Interval
        }

        assignments.forEach { assignment ->
            when (assignment.name.lowercase(Locale.US)) {
                "head", "list1", "list2", "l1", "l2", "lists" -> helperTypes += HelperType.ListNode
                "root", "subroot", "p", "q" -> helperTypes += HelperType.TreeNode
                "node" -> helperTypes += HelperType.Node
                "intervals" -> helperTypes += HelperType.Interval
            }
        }

        return helperTypes
    }

    private fun inferParameterType(
        parameterName: String,
        value: String,
        helperTypes: Set<HelperType>
    ): String {
        val normalizedName = parameterName.lowercase(Locale.US)

        if (HelperType.ListNode in helperTypes) {
            when (normalizedName) {
                "head", "list1", "list2", "l1", "l2" -> return "Optional[ListNode]"
                "lists" -> return "List[Optional[ListNode]]"
            }
        }
        if (HelperType.TreeNode in helperTypes && normalizedName in setOf("root", "subroot", "p", "q")) {
            return if (normalizedName == "p" || normalizedName == "q") "TreeNode" else "Optional[TreeNode]"
        }
        if (HelperType.Node in helperTypes && normalizedName == "node") {
            return "Optional[Node]"
        }
        if (HelperType.Interval in helperTypes && normalizedName == "intervals") {
            return "List[Interval]"
        }

        return inferTypeHint(value)
    }

    private fun inferReturnType(
        value: String,
        helperTypes: Set<HelperType>
    ): String {
        return when {
            HelperType.ListNode in helperTypes && value.startsWith("[") -> "Optional[ListNode]"
            HelperType.TreeNode in helperTypes && (value.startsWith("[") || value == "null" || value == "None") ->
                "Optional[TreeNode]"
            HelperType.Node in helperTypes && value.startsWith("[") -> "Optional[Node]"
            HelperType.Interval in helperTypes && value.startsWith("[[") -> "List[Interval]"
            else -> inferTypeHint(value)
        }
    }

    private fun inferTypeHint(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return "Any"
        if (trimmed == "true" || trimmed == "false" || trimmed == "True" || trimmed == "False") return "bool"
        if (trimmed == "null" || trimmed == "None") return "Any"
        if (Regex("""^-?\d+$""").matches(trimmed)) return "int"
        if (Regex("""^-?\d+\.\d+$""").matches(trimmed)) return "float"
        if (
            (trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))
        ) {
            return "str"
        }
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val items = splitTopLevel(trimmed.removePrefix("[").removeSuffix("]"))
            if (items.isEmpty()) return "List[Any]"
            val itemTypes = items.map(::inferTypeHint).distinct()
            return if (itemTypes.size == 1) {
                "List[${itemTypes.single()}]"
            } else {
                "List[Any]"
            }
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return if (":" in trimmed) "Dict[str, Any]" else "Any"
        }
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            val items = splitTopLevel(trimmed.removePrefix("(").removeSuffix(")"))
            return if (items.isEmpty()) "Tuple[Any, ...]" else "Tuple[${items.map(::inferTypeHint).joinToString(", ")}]"
        }
        return "Any"
    }

    private fun helperTypeComments(helperTypes: Set<HelperType>): String {
        if (helperTypes.isEmpty()) return ""

        val blocks = helperTypes.map { helperType ->
            when (helperType) {
                HelperType.ListNode -> """
                    # Definition for singly-linked list.
                    # class ListNode:
                    #     def __init__(self, val=0, next=None):
                    #         self.val = val
                    #         self.next = next
                """.trimIndent()

                HelperType.TreeNode -> """
                    # Definition for a binary tree node.
                    # class TreeNode:
                    #     def __init__(self, val=0, left=None, right=None):
                    #         self.val = val
                    #         self.left = left
                    #         self.right = right
                """.trimIndent()

                HelperType.Node -> """
                    # Definition for a Node.
                    # class Node:
                    #     def __init__(self, val=0, neighbors=None):
                    #         self.val = val
                    #         self.neighbors = neighbors if neighbors is not None else []
                """.trimIndent()

                HelperType.Interval -> """
                    # Definition for Interval.
                    # class Interval:
                    #     def __init__(self, start=0, end=0):
                    #         self.start = start
                    #         self.end = end
                """.trimIndent()
            }
        }

        return blocks.joinToString(separator = "\n\n", postfix = "\n\n")
    }

    private fun methodNameFromTitle(title: String): String {
        val words = title.trim()
            .split(Regex("""[^A-Za-z0-9]+"""))
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return "solve"

        val first = words.first().lowercase(Locale.US)
        val rest = words.drop(1).joinToString("") { word ->
            word.lowercase(Locale.US).replaceFirstChar(Char::titlecase)
        }
        val candidate = (first + rest).replace(Regex("""[^A-Za-z0-9_]"""), "")
        return if (candidate.firstOrNull()?.isLetter() == true || candidate.firstOrNull() == '_') {
            candidate
        } else {
            "solve${candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }}"
        }
    }

    private fun classNameFromTitle(title: String): String {
        val words = title.trim()
            .split(Regex("""[^A-Za-z0-9]+"""))
            .filter { it.isNotBlank() }
        val candidate = words.joinToString("") { word ->
            word.lowercase(Locale.US).replaceFirstChar(Char::titlecase)
        }.ifBlank { "CustomProblem" }
        return if (candidate.firstOrNull()?.isLetter() == true) candidate else "CustomProblem"
    }

    private fun parseExampleAssignments(rawInput: String): List<ParsedAssignment> {
        val text = rawInput.trim()
        if (text.isBlank() || "=" !in text) return emptyList()

        return splitTopLevel(text)
            .mapNotNull { segment ->
                val separatorIndex = segment.indexOf('=')
                if (separatorIndex <= 0) return@mapNotNull null
                val name = segment.substring(0, separatorIndex).trim()
                val value = segment.substring(separatorIndex + 1).trim()
                if (!name.matches(Regex("""[A-Za-z_][A-Za-z0-9_]*""")) || value.isBlank()) {
                    null
                } else {
                    ParsedAssignment(name = name, value = value)
                }
            }
    }

    private fun splitTopLevel(rawText: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        var quoteChar: Char? = null
        var escaping = false

        rawText.forEach { character ->
            if (quoteChar != null) {
                current.append(character)
                when {
                    escaping -> escaping = false
                    character == '\\' -> escaping = true
                    character == quoteChar -> quoteChar = null
                }
                return@forEach
            }

            when (character) {
                '\'', '"' -> {
                    quoteChar = character
                    current.append(character)
                }
                '[', '{', '(' -> {
                    depth += 1
                    current.append(character)
                }
                ']', '}', ')' -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    current.append(character)
                }
                ',', '\n' -> {
                    if (depth == 0) {
                        val segment = current.toString().trim()
                        if (segment.isNotBlank()) parts += segment
                        current.clear()
                    } else {
                        current.append(character)
                    }
                }
                else -> current.append(character)
            }
        }

        val tail = current.toString().trim()
        if (tail.isNotBlank()) parts += tail
        return parts
    }

    private data class ParsedAssignment(
        val name: String,
        val value: String
    )

    private enum class HelperType {
        ListNode,
        TreeNode,
        Node,
        Interval
    }
}
