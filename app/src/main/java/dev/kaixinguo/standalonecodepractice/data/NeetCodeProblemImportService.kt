package dev.kaixinguo.standalonecodepractice.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

internal data class ImportedNeetCodeProblem(
    val title: String,
    val difficulty: String,
    val summary: String,
    val statementMarkdown: String,
    val exampleInput: String,
    val exampleOutput: String,
    val starterCode: String,
    val hints: List<String>,
    val questionUrl: String,
    val executionPipeline: String
)

internal class NeetCodeProblemImportService {
    suspend fun importProblemByTitle(title: String): ImportedNeetCodeProblem = withContext(Dispatchers.IO) {
        val solutionSlug = slugify(title)
        val solutionUrl = "https://neetcode.io/solutions/$solutionSlug"
        val solutionHtml = readTextFromUrl(solutionUrl)
        val problemMarker = "/problems/"
        val problemPathStart = solutionHtml.indexOf(problemMarker)
        val problemPath = if (problemPathStart >= 0) {
            val slugStart = problemPathStart + problemMarker.length
            val slugEnd = solutionHtml.indexOf('/', slugStart)
            if (slugEnd > slugStart) solutionHtml.substring(slugStart, slugEnd) else ""
        } else {
            ""
        }.ifBlank {
            throw IllegalArgumentException("Unable to resolve the NeetCode problem path for $title.")
        }
        val questionUrl = "https://neetcode.io/problems/$problemPath/question?list=neetcode150"
        importProblemFromQuestionUrl(questionUrl)
    }

    suspend fun importProblemFromQuestionUrl(questionUrl: String): ImportedNeetCodeProblem = withContext(Dispatchers.IO) {
        val html = readTextFromUrl(questionUrl)
        parseProblemPage(html, questionUrl)
    }

    private fun parseProblemPage(
        html: String,
        questionUrl: String
    ): ImportedNeetCodeProblem {
        val stateJson = extractStateJson(html)
        val problemObject = extractFirstRootObject(stateJson)
        val title = extractJsonString(problemObject, "name")
            .ifBlank { extractHtmlTitle(html) }
        val difficulty = extractJsonString(problemObject, "difficulty")
        val description = extractJsonString(problemObject, "description")
        val statementMarkdown = extractStatementMarkdown(description)
        val starterCodeJson = extractJsonObject(problemObject, "starterCode")
        val starterCode = extractJsonString(starterCodeJson, "python")
        val hints = extractHints(description)

        return ImportedNeetCodeProblem(
            title = title,
            difficulty = difficulty,
            summary = extractSummary(statementMarkdown),
            statementMarkdown = statementMarkdown,
            exampleInput = extractExampleInput(statementMarkdown),
            exampleOutput = extractExampleOutput(statementMarkdown),
            starterCode = starterCode,
            hints = hints,
            questionUrl = questionUrl,
            executionPipeline = ProblemExecutionPipelineResolver.infer(
                title = title,
                starterCode = starterCode
            ).storageValue
        )
    }

    private fun extractStateJson(html: String): String {
        val startMarker = """<script id="ng-state" type="application/json">"""
        val startIndex = html.indexOf(startMarker)
        if (startIndex == -1) {
            throw IllegalArgumentException("NeetCode page state was not found.")
        }
        val contentStart = startIndex + startMarker.length
        val endIndex = html.indexOf("</script>", contentStart)
        if (endIndex == -1) {
            throw IllegalArgumentException("NeetCode page state was not found.")
        }
        return html.substring(contentStart, endIndex).trim()
    }

    private fun extractFirstRootObject(stateJson: String): String {
        val rootStart = stateJson.indexOf('{')
        if (rootStart == -1) {
            throw IllegalArgumentException("NeetCode problem payload was not found.")
        }
        var index = rootStart + 1
        while (index < stateJson.length && stateJson[index].isWhitespace()) {
            index += 1
        }
        if (index >= stateJson.length || stateJson[index] != '"') {
            throw IllegalArgumentException("NeetCode problem payload was not found.")
        }
        val colonIndex = stateJson.indexOf(':', index + 1)
        if (colonIndex == -1) {
            throw IllegalArgumentException("NeetCode problem payload was not found.")
        }
        var objectStart = colonIndex + 1
        while (objectStart < stateJson.length && stateJson[objectStart].isWhitespace()) {
            objectStart += 1
        }
        if (objectStart >= stateJson.length || stateJson[objectStart] != '{') {
            throw IllegalArgumentException("NeetCode problem payload was not found.")
        }
        val objectEnd = findMatchingDelimiter(
            source = stateJson,
            startIndex = objectStart,
            openDelimiter = '{',
            closeDelimiter = '}'
        )
        return stateJson.substring(objectStart, objectEnd + 1)
    }

    private fun extractHtmlTitle(html: String): String {
        val startTag = "<title>"
        val endTag = "</title>"
        val startIndex = html.indexOf(startTag)
        if (startIndex == -1) return ""
        val contentStart = startIndex + startTag.length
        val endIndex = html.indexOf(endTag, contentStart)
        if (endIndex == -1) return ""
        return html.substring(contentStart, endIndex)
            .substringBefore(" - NeetCode")
            .trim()
    }

    private fun extractJsonString(json: String, key: String): String {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return ""
        val colonIndex = json.indexOf(':', keyIndex + key.length + 2)
        if (colonIndex == -1) return ""
        var index = colonIndex + 1
        while (index < json.length && json[index].isWhitespace()) {
            index += 1
        }
        if (index >= json.length || json[index] != '"') return ""
        index += 1

        val builder = StringBuilder()
        while (index < json.length) {
            val character = json[index]
            if (character == '\\') {
                if (index + 1 >= json.length) break
                val escaped = json[index + 1]
                when (escaped) {
                    '"', '\\', '/' -> builder.append(escaped)
                    'n' -> builder.append('\n')
                    'r' -> builder.append('\r')
                    't' -> builder.append('\t')
                    'b' -> builder.append('\b')
                    'f' -> builder.append('\u000C')
                    'u' -> {
                        if (index + 5 < json.length) {
                            val hex = json.substring(index + 2, index + 6)
                            hex.toIntOrNull(16)?.let { codePoint ->
                                builder.append(codePoint.toChar())
                            }
                            index += 4
                        }
                    }
                    else -> builder.append(escaped)
                }
                index += 2
                continue
            }
            if (character == '"') {
                return builder.toString()
            }
            builder.append(character)
            index += 1
        }
        return builder.toString()
    }

    private fun extractJsonObject(json: String, key: String): String {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return ""
        val colonIndex = json.indexOf(':', keyIndex + key.length + 2)
        if (colonIndex == -1) return ""
        var objectStart = colonIndex + 1
        while (objectStart < json.length && json[objectStart].isWhitespace()) {
            objectStart += 1
        }
        if (objectStart >= json.length || json[objectStart] != '{') return ""
        val objectEnd = findMatchingDelimiter(
            source = json,
            startIndex = objectStart,
            openDelimiter = '{',
            closeDelimiter = '}'
        )
        return json.substring(objectStart, objectEnd + 1)
    }

    private fun extractStatementMarkdown(description: String): String {
        val normalized = description
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
        val cutoff = normalized.indexOf("<details")
        val statement = if (cutoff >= 0) {
            normalized.substring(0, cutoff)
        } else {
            normalized
        }
        return statement
            .lines()
            .map { it.trimEnd() }
            .joinToString("\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun extractSummary(statementMarkdown: String): String {
        val paragraph = mutableListOf<String>()
        for (rawLine in statementMarkdown.lines()) {
            val line = rawLine.trim()
            if (line.isBlank()) {
                if (paragraph.isNotEmpty()) break
                continue
            }
            if (
                line.startsWith("```") ||
                line.startsWith("**Example", ignoreCase = true) ||
                line.startsWith("**Constraints", ignoreCase = true) ||
                line.startsWith("* ")
            ) {
                if (paragraph.isNotEmpty()) break
                continue
            }
            paragraph += line
        }
        val text = paragraph.joinToString(" ")
            .replace("`", "")
            .replace("**", "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return if (text.length <= 360) text else text.take(357).trimEnd() + "..."
    }

    private fun extractExampleInput(statementMarkdown: String): String {
        return Regex(
            """(?s)Input:\s*(.+?)\s*(?:\n\s*Output:|\r\n\s*Output:)"""
        ).find(statementMarkdown)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
    }

    private fun extractExampleOutput(statementMarkdown: String): String {
        return Regex(
            """(?s)Output:\s*(.+?)\s*(?:\n\s*Explanation:|\n\s*\*\*Example|\n\s*\*\*Constraints|```|$)"""
        ).find(statementMarkdown)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
    }

    private fun extractHints(description: String): List<String> {
        val normalized = description
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
        return Regex(
            """(?is)<summary>\s*Hint\s*\d+\s*</summary>\s*<p>\s*(.*?)\s*</p>"""
        ).findAll(normalized)
            .map { match -> cleanHtmlText(match.groupValues[1]) }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun cleanHtmlText(htmlFragment: String): String {
        return htmlFragment
            .replace(Regex("""</?code>""", RegexOption.IGNORE_CASE), "`")
            .replace(Regex("""</?[^>]+>"""), "")
            .replace("**", "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun findMatchingDelimiter(
        source: String,
        startIndex: Int,
        openDelimiter: Char,
        closeDelimiter: Char
    ): Int {
        var depth = 0
        var inString = false
        var isEscaped = false

        for (index in startIndex until source.length) {
            val character = source[index]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (character == '\\') {
                isEscaped = true
                continue
            }
            if (character == '"') {
                inString = !inString
                continue
            }
            if (inString) continue
            if (character == openDelimiter) {
                depth += 1
            } else if (character == closeDelimiter) {
                depth -= 1
                if (depth == 0) {
                    return index
                }
            }
        }

        throw IllegalArgumentException("Failed to parse NeetCode JSON payload.")
    }

    private fun slugify(value: String): String {
        return value
            .lowercase(Locale.US)
            .replace(Regex("""[^a-z0-9]+"""), "-")
            .trim('-')
    }

    private fun readTextFromUrl(url: String): String {
        val connection = openConnection(url)
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun openConnection(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "text/html,application/json")
            setRequestProperty("User-Agent", "CodeOffTheGrid")
            if (responseCode !in 200..299) {
                val message = errorStream?.bufferedReader()?.use { it.readText() }
                    ?.takeIf { it.isNotBlank() }
                    ?: responseMessage
                throw IllegalArgumentException("NeetCode request failed: $message")
            }
        }
    }
}
