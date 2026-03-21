package dev.kaixinguo.standalonecodepractice.data

import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemFolderState
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemListItem
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemSetState
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipInputStream

internal class GitHubMarkdownImportService {
    suspend fun importRepo(repoUrl: String): ProblemFolderState = withContext(Dispatchers.IO) {
        val normalizedRepo = normalizeRepo(repoUrl)
        val repoInfo = fetchRepoInfo(normalizedRepo)
        val repoArchive = downloadRepoArchive(normalizedRepo, repoInfo.defaultBranch)
        val problemsBySet = buildProblemsBySet(repoArchive, repoInfo.defaultBranch)
        if (problemsBySet.isEmpty()) {
            throw IllegalArgumentException("No markdown problems were found in that repository.")
        }

        val folderId = "github-${slugify(normalizedRepo.owner)}-${slugify(normalizedRepo.repo)}"
        val orderedSets = problemsBySet.entries
            .sortedBy { it.key.orderKey }
        val firstSetTitle = orderedSets.first().key.rawTitle
        val sortedSets = orderedSets
            .map { entry ->
                ProblemSetState(
                    id = "$folderId-set-${slugify(entry.key.rawTitle)}",
                    title = entry.key.displayTitle,
                    problems = mutableStateListOf<ProblemListItem>().apply {
                        addAll(
                            entry.value
                                .sortedBy { it.orderKey }
                                .mapIndexed { index, problem ->
                                    ProblemListItem(
                                        id = "$folderId-problem-${slugify(problem.relativePath)}",
                                        title = problem.title,
                                        difficulty = problem.difficulty,
                                        active = false,
                                        solved = false,
                                        summary = problem.summary,
                                        statementMarkdown = problem.statementMarkdown,
                                        exampleInput = problem.exampleInput,
                                        exampleOutput = problem.exampleOutput,
                                        starterCode = "",
                                        customTests = "",
                                        hints = emptyList(),
                                        submissionTestSuite = ProblemSubmissionSuiteFactory.build(
                                            statementMarkdown = problem.statementMarkdown,
                                            exampleInput = problem.exampleInput,
                                            exampleOutput = problem.exampleOutput
                                        ),
                                        executionPipeline = ProblemExecutionPipelineResolver.infer(
                                            title = problem.title,
                                            starterCode = ""
                                        )
                                    ).let { item ->
                                        if (index == 0 && entry.key.rawTitle == firstSetTitle) {
                                            item.copy(active = true)
                                        } else {
                                            item
                                        }
                                    }
                                }
                        )
                    }
                )
            }

        ProblemFolderState(
            id = folderId,
            title = repoInfo.displayName,
            sets = mutableStateListOf<ProblemSetState>().apply {
                addAll(sortedSets)
            }
        )
    }

    private fun fetchRepoInfo(repo: GitHubRepo): RepoInfo {
        val response = readTextFromUrl("https://api.github.com/repos/${repo.owner}/${repo.repo}")
        return RepoInfo(
            defaultBranch = extractJsonString(response, "default_branch").ifBlank { "main" },
            displayName = extractJsonString(response, "name").ifBlank { repo.repo }.replace('-', ' ')
        )
    }

    private fun buildProblemsBySet(
        repoArchive: RepoArchive,
        defaultBranch: String
    ): Map<SetTitle, List<ImportedProblem>> {
        val readmeProblems = repoArchive.readmeMarkdown
            ?.let { parseReadmeProblemIndex(it, repoArchive.markdownProblemsByPath, defaultBranch) }
            .orEmpty()

        return if (readmeProblems.isNotEmpty()) {
            readmeProblems
        } else {
            repoArchive.markdownProblemsByPath.values
                .groupBy(
                    keySelector = {
                        SetTitle(
                            rawTitle = it.setSegment,
                            displayTitle = cleanSetTitle(it.setSegment),
                            orderKey = it.setSegment
                        )
                    },
                    valueTransform = { markdownProblem ->
                        val statementMarkdown = extractStatementMarkdown(markdownProblem.content)
                        ImportedProblem(
                            relativePath = markdownProblem.relativePath,
                            orderKey = markdownProblem.fileName,
                            title = extractTitle(markdownProblem.fileName, markdownProblem.content),
                            difficulty = extractDifficulty(markdownProblem.content),
                            summary = extractSummary(statementMarkdown),
                            statementMarkdown = statementMarkdown,
                            exampleInput = extractInlineField(markdownProblem.content, "Input"),
                            exampleOutput = extractInlineField(markdownProblem.content, "Output")
                        )
                    }
                )
        }
    }

    private fun downloadRepoArchive(
        repo: GitHubRepo,
        defaultBranch: String
    ): RepoArchive {
        val connection = openConnection("https://codeload.github.com/${repo.owner}/${repo.repo}/zip/refs/heads/$defaultBranch")
        val markdownProblemsByPath = linkedMapOf<String, MarkdownProblem>()
        var readmeMarkdown: String? = null

        connection.inputStream.use { rawInput ->
            ZipInputStream(BufferedInputStream(rawInput)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory || !entry.name.endsWith(".md", ignoreCase = true)) {
                        zip.closeEntry()
                        continue
                    }

                    val segments = entry.name.split('/').filter { it.isNotBlank() }
                    if (segments.size == 2 && segments[1].equals("README.md", ignoreCase = true)) {
                        readmeMarkdown = zip.readEntryText()
                        zip.closeEntry()
                        continue
                    }

                    if (segments.size < 3) {
                        zip.closeEntry()
                        continue
                    }

                    val setSegment = segments[1]
                    val fileName = segments.last()
                    if (setSegment.startsWith(".") || fileName.equals("README.md", ignoreCase = true)) {
                        zip.closeEntry()
                        continue
                    }

                    val content = zip.readEntryText()
                    val relativePath = segments.drop(1).joinToString("/")
                    markdownProblemsByPath[relativePath] = MarkdownProblem(
                        relativePath = relativePath,
                        setSegment = setSegment,
                        fileName = fileName,
                        content = content
                    )
                    zip.closeEntry()
                }
            }
        }

        return RepoArchive(
            readmeMarkdown = readmeMarkdown,
            markdownProblemsByPath = markdownProblemsByPath
        )
    }

    private fun parseReadmeProblemIndex(
        readmeMarkdown: String,
        markdownProblemsByPath: Map<String, MarkdownProblem>,
        defaultBranch: String
    ): Map<SetTitle, List<ImportedProblem>> {
        val lines = readmeMarkdown.lines()
        val sectionRegex = Regex("""^##\s+(\d+\.\s+.*?)(?:\s+\[Resources].*)?$""")
        val problemRegex = Regex("""^\s*(\d+)\.\s+(.*?)(?:\s+\[Solution]\((.*?)\))?\s*$""")
        val problemsBySet = linkedMapOf<SetTitle, MutableList<ImportedProblem>>()
        var currentSet: SetTitle? = null

        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            val sectionMatch = sectionRegex.find(line)
            if (sectionMatch != null) {
                val rawTitle = sectionMatch.groupValues[1].trim()
                currentSet = if (shouldSkipReadmeSet(rawTitle)) {
                    null
                } else {
                    SetTitle(
                        rawTitle = rawTitle,
                        displayTitle = cleanReadmeSetTitle(rawTitle),
                        orderKey = extractReadmeOrderKey(rawTitle)
                    )
                }
                return@forEach
            }

            val activeSet = currentSet ?: return@forEach
            val problemMatch = problemRegex.find(line) ?: return@forEach
            val orderPrefix = problemMatch.groupValues[1]
            val title = problemMatch.groupValues[2].trim()
            val solutionUrl = problemMatch.groupValues.getOrNull(3).orEmpty().trim()
            val relativePath = extractRelativePathFromGitHubUrl(solutionUrl, defaultBranch)
            val markdownProblem = relativePath?.let(markdownProblemsByPath::get)
            problemsBySet.getOrPut(activeSet) { mutableListOf() }.add(
                importedProblemFromReadmeEntry(
                    title = title,
                    orderPrefix = orderPrefix,
                    relativePath = relativePath,
                    markdownProblem = markdownProblem
                )
            )
        }

        return problemsBySet
    }

    private fun importedProblemFromReadmeEntry(
        title: String,
        orderPrefix: String,
        relativePath: String?,
        markdownProblem: MarkdownProblem?
    ): ImportedProblem {
        if (markdownProblem != null) {
            val statementMarkdown = extractStatementMarkdown(markdownProblem.content)
            return ImportedProblem(
                relativePath = markdownProblem.relativePath,
                orderKey = "$orderPrefix-${markdownProblem.fileName}",
                title = title,
                difficulty = extractDifficulty(markdownProblem.content),
                summary = extractSummary(statementMarkdown),
                statementMarkdown = statementMarkdown,
                exampleInput = extractInlineField(markdownProblem.content, "Input"),
                exampleOutput = extractInlineField(markdownProblem.content, "Output")
            )
        }

        val syntheticPath = relativePath
            ?.takeIf { it.isNotBlank() }
            ?: "readme-only/${slugify(title)}.md"
        return ImportedProblem(
            relativePath = syntheticPath,
            orderKey = "$orderPrefix-$syntheticPath",
            title = title,
            difficulty = "",
            summary = "Imported from the repository index. No markdown file was linked for this problem.",
            statementMarkdown = "Imported from the repository index. No markdown file was linked for this problem.",
            exampleInput = "",
            exampleOutput = ""
        )
    }

    private fun normalizeRepo(repoUrl: String): GitHubRepo {
        val trimmed = repoUrl.trim().removeSuffix("/")
        val match = Regex("""https?://github\.com/([^/]+)/([^/]+?)(?:\.git)?(?:/.*)?$""")
            .find(trimmed)
            ?: throw IllegalArgumentException("Enter a valid GitHub repository URL.")
        return GitHubRepo(
            owner = match.groupValues[1],
            repo = match.groupValues[2]
        )
    }

    private fun extractTitle(fileName: String, markdown: String): String {
        val heading = markdown.lineSequence()
            .firstOrNull { it.trim().startsWith("# ") }
            ?.removePrefix("# ")
            ?.trim()
            .orEmpty()
        if (heading.isNotBlank()) return cleanProblemTitle(heading)
        return cleanProblemTitle(fileName.removeSuffix(".md"))
    }

    private fun extractDifficulty(markdown: String): String {
        val match = Regex("""(?im)\b(Easy|Medium|Hard)\b""").find(markdown)
        return match?.value.orEmpty()
    }

    private fun extractSummary(statementMarkdown: String): String {
        val lines = statementMarkdown.lines()
        val paragraph = mutableListOf<String>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) {
                if (paragraph.isNotEmpty()) break
                continue
            }
            if (
                line.startsWith("```") ||
                line.startsWith("Example", ignoreCase = true) ||
                line.startsWith("Constraints", ignoreCase = true) ||
                line.startsWith("- ")
            ) {
                if (paragraph.isNotEmpty()) break
                continue
            }
            paragraph += line
        }

        val text = paragraph.joinToString(" ")
            .replace(Regex("""\[(.+?)]\(.+?\)"""), "$1")
            .replace("`", "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        return if (text.length <= 360) text else text.take(357).trimEnd() + "..."
    }

    private fun extractStatementMarkdown(markdown: String): String {
        val lines = markdown.lines()
        if (lines.isEmpty()) return ""

        var index = 0
        while (index < lines.size && lines[index].trim().isBlank()) {
            index += 1
        }
        if (index < lines.size && lines[index].trim().startsWith("#")) {
            index += 1
        }
        while (index < lines.size && lines[index].trim().isBlank()) {
            index += 1
        }
        if (index < lines.size && lines[index].trim().matches(Regex("""(?i)^(Easy|Medium|Hard)$"""))) {
            index += 1
        }

        val bodyLines = mutableListOf<String>()
        var lastNonBlank = ""

        while (index < lines.size) {
            val rawLine = lines[index]
            val trimmed = rawLine.trim()

            if (trimmed.startsWith("```")) {
                val isExampleFence = lastNonBlank.startsWith("Example", ignoreCase = true) ||
                    lastNonBlank.startsWith("Input", ignoreCase = true) ||
                    lastNonBlank.startsWith("Output", ignoreCase = true)

                if (!isExampleFence) break

                bodyLines += "```"
                index += 1
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    bodyLines += lines[index].trimEnd()
                    index += 1
                }
                if (index < lines.size) {
                    bodyLines += "```"
                }
                lastNonBlank = "```"
                index += 1
                continue
            }

            bodyLines += rawLine.trimEnd()
            if (trimmed.isNotBlank()) {
                lastNonBlank = trimmed
            }
            index += 1
        }

        return bodyLines
            .dropWhile { it.trim().isBlank() }
            .dropLastWhile { it.trim().isBlank() }
            .joinToString("\n")
    }

    private fun extractInlineField(markdown: String, label: String): String {
        val escapedLabel = Regex.escape(label)
        val regex = Regex("""(?im)^\s*$escapedLabel\s*:\s*(.+)$""")
        return regex.find(markdown)?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractJsonString(json: String, key: String): String {
        val escapedKey = Regex.escape(key)
        val match = Regex(""""$escapedKey"\s*:\s*"((?:\\.|[^"])*)"""").find(json) ?: return ""
        return match.groupValues[1]
            .replace("""\"""", "\"")
            .replace("""\/""", "/")
            .replace("""\\n""", "\n")
            .replace("""\\r""", "\r")
            .replace("""\\t""", "\t")
            .replace("""\\\\""", "\\")
    }

    private fun cleanSetTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("""^\d+\.\s*"""), "")
            .trim()
    }

    private fun cleanReadmeSetTitle(rawTitle: String): String {
        return cleanSetTitle(rawTitle)
            .replace(Regex("""\s+\(.*\)$"""), "")
            .trim()
    }

    private fun shouldSkipReadmeSet(rawTitle: String): Boolean {
        return rawTitle.contains("Not from", ignoreCase = true)
    }

    private fun extractReadmeOrderKey(rawTitle: String): String {
        val prefix = Regex("""^(\d+)""").find(rawTitle)?.groupValues?.get(1)?.padStart(3, '0')
        return prefix ?: rawTitle
    }

    private fun extractRelativePathFromGitHubUrl(url: String, defaultBranch: String): String? {
        if (url.isBlank()) return null
        val marker = "/blob/$defaultBranch/"
        val index = url.indexOf(marker)
        if (index == -1) return null
        return URLDecoder.decode(
            url.substring(index + marker.length),
            StandardCharsets.UTF_8.name()
        )
    }

    private fun cleanProblemTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("""^\d+[\s.\-_:]+"""), "")
            .replace('_', ' ')
            .trim()
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
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "StandaloneCodePractice")
            if (responseCode !in 200..299) {
                val message = errorStream?.bufferedReader()?.use { it.readText() }
                    ?.takeIf { it.isNotBlank() }
                    ?: responseMessage
                throw IllegalArgumentException("GitHub request failed: $message")
            }
        }
    }

    private fun ZipInputStream.readEntryText(): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = read(buffer)
            if (count <= 0) break
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private data class GitHubRepo(
        val owner: String,
        val repo: String
    )

    private data class RepoInfo(
        val defaultBranch: String,
        val displayName: String
    )

    private data class RepoArchive(
        val readmeMarkdown: String?,
        val markdownProblemsByPath: Map<String, MarkdownProblem>
    )

    private data class SetTitle(
        val rawTitle: String,
        val displayTitle: String,
        val orderKey: String
    )

    private data class MarkdownProblem(
        val relativePath: String,
        val setSegment: String,
        val fileName: String,
        val content: String
    )

    private data class ImportedProblem(
        val relativePath: String,
        val orderKey: String,
        val title: String,
        val difficulty: String,
        val summary: String,
        val statementMarkdown: String,
        val exampleInput: String,
        val exampleOutput: String
    )
}
