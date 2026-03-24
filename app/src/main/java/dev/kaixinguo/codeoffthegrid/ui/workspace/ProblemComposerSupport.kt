package dev.kaixinguo.codeoffthegrid.ui.workspace

import dev.kaixinguo.codeoffthegrid.data.ProblemExecutionPipelineResolver
import dev.kaixinguo.codeoffthegrid.data.ProblemInputNormalizer
import dev.kaixinguo.codeoffthegrid.data.ProblemShareCodec
import dev.kaixinguo.codeoffthegrid.data.ProblemStarterCodeFactory
import dev.kaixinguo.codeoffthegrid.data.ProblemSubmissionSuiteFactory
import dev.kaixinguo.codeoffthegrid.data.ProblemTestSuiteJsonCodec
import dev.kaixinguo.codeoffthegrid.data.SharedProblemFile
import org.json.JSONArray

internal fun ProblemComposerSession.isDirty(): Boolean {
    return draft != ProblemComposerDraft() || !importedFileName.isNullOrBlank()
}

internal fun ProblemComposerDraft.toPreviewProblem(): ProblemListItem {
    val pipeline = resolvedExecutionPipeline()
    val effectiveStarterCode = effectiveStarterCode()
    val effectiveSummary = effectiveSummary()
    val normalizedExampleInput = normalizedExampleInput(
        effectiveStarterCode = effectiveStarterCode,
        pipeline = pipeline
    )
    val explicitSubmissionSuite = parsedSubmissionTestSuite()
    val submissionSuite = ProblemInputNormalizer.normalizeSubmissionTestSuite(
        testSuite = explicitSubmissionSuite ?: autoSubmissionTestSuite(
            normalizedExampleInput = normalizedExampleInput
        ),
        starterCode = effectiveStarterCode,
        executionPipeline = pipeline
    )

    return ProblemListItem(
        id = "composer-preview",
        title = title.trim().ifBlank { "Untitled Problem" },
        difficulty = difficulty.trim(),
        active = false,
        solved = false,
        summary = effectiveSummary,
        statementMarkdown = statementMarkdown.trim(),
        exampleInput = normalizedExampleInput,
        exampleOutput = exampleOutput.trim(),
        starterCode = effectiveStarterCode,
        customTests = "",
        hints = hints(),
        submissionTestSuite = submissionSuite,
        executionPipeline = pipeline
    )
}

internal fun ProblemComposerDraft.toSavedProblem(): ProblemListItem {
    val previewProblem = toPreviewProblem()
    return previewProblem.copy(
        id = java.util.UUID.randomUUID().toString(),
        customTests = "",
        active = false,
        solved = false
    )
}

internal fun ProblemComposerDraft.validationError(availableSetIds: Set<String>): String? {
    if (destinationSetId !in availableSetIds) {
        return "Choose a destination set before saving."
    }
    if (title.isBlank()) {
        return "Enter a problem title before saving."
    }
    if (statementMarkdown.isBlank()) {
        return "Add a statement before saving."
    }
    return submissionTestSuiteValidationError()
}

internal fun ProblemComposerDraft.submissionTestSuiteValidationError(): String? {
    if (submissionTestSuiteJson.isBlank()) return null
    return if (parsedSubmissionTestSuite() == null) {
        "Advanced submission suite JSON is invalid."
    } else {
        null
    }
}

internal fun ProblemComposerDraft.parsedSubmissionTestSuite(): ProblemTestSuite? {
    if (submissionTestSuiteJson.isBlank()) return null
    return ProblemTestSuiteJsonCodec.decodeFromString(submissionTestSuiteJson)
}

internal fun ProblemComposerDraft.displaySubmissionTestSuiteJson(): String {
    return if (submissionTestSuiteJson.isBlank()) {
        ProblemTestSuiteJsonCodec.encodeToJson(
            autoSubmissionTestSuite(),
            includeIds = false
        ).toString(2)
    } else {
        submissionTestSuiteJson
    }
}

internal fun ProblemComposerDraft.resolvedExecutionPipeline(): ProblemExecutionPipeline {
    return executionPipelineOverride
        .takeIf { it.isNotBlank() }
        ?.let(ProblemExecutionPipeline::fromStorage)
        ?: ProblemExecutionPipelineResolver.infer(
            title = title.trim(),
            starterCode = effectiveStarterCode()
        )
}

internal fun ProblemComposerDraft.hints(): List<String> {
    return parseComposerHintsText(hintsText)
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

internal fun ProblemComposerDraft.withImportedProblem(sharedProblem: SharedProblemFile): ProblemComposerDraft {
    val importedStatement = sharedProblem.statementMarkdown.ifBlank { sharedProblem.summary }
    return copy(
        title = sharedProblem.title,
        difficulty = sharedProblem.difficulty.ifBlank { "Easy" },
        summary = sharedProblem.summary,
        statementMarkdown = importedStatement,
        exampleInput = sharedProblem.exampleInput,
        exampleOutput = sharedProblem.exampleOutput,
        starterCode = sharedProblem.starterCode,
        starterCodeMode = if (sharedProblem.starterCode.isBlank()) {
            ProblemStarterCodeMode.Auto
        } else {
            ProblemStarterCodeMode.Manual
        },
        hintsText = sharedProblem.hints.joinToString("\n"),
        submissionTestSuiteJson = sharedProblem.submissionTestSuite
            ?.let { ProblemTestSuiteJsonCodec.encodeToJson(it, includeIds = false).toString(2) }
            .orEmpty(),
        executionPipelineOverride = sharedProblem.executionPipeline?.storageValue.orEmpty()
    )
}

internal fun ProblemComposerDraft.withGeneratedProblem(sharedProblem: SharedProblemFile): ProblemComposerDraft {
    val generatedStatement = sharedProblem.statementMarkdown.ifBlank { sharedProblem.summary }
    val generatedHints = sharedProblem.hints
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return copy(
        title = sharedProblem.title.takeIf { it.isNotBlank() } ?: title,
        difficulty = sharedProblem.difficulty.takeIf { it.isNotBlank() } ?: difficulty,
        summary = sharedProblem.summary.takeIf { it.isNotBlank() } ?: summary,
        statementMarkdown = generatedStatement.takeIf { it.isNotBlank() } ?: statementMarkdown,
        exampleInput = sharedProblem.exampleInput.takeIf { it.isNotBlank() } ?: exampleInput,
        exampleOutput = sharedProblem.exampleOutput.takeIf { it.isNotBlank() } ?: exampleOutput,
        starterCode = sharedProblem.starterCode.takeIf { it.isNotBlank() } ?: starterCode,
        starterCodeMode = when {
            sharedProblem.starterCode.isNotBlank() -> ProblemStarterCodeMode.Manual
            starterCode.isNotBlank() -> starterCodeMode
            else -> ProblemStarterCodeMode.Auto
        },
        hintsText = generatedHints.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: hintsText,
        submissionTestSuiteJson = sharedProblem.submissionTestSuite
            ?.let { ProblemTestSuiteJsonCodec.encodeToJson(it, includeIds = false).toString(2) }
            ?: submissionTestSuiteJson,
        executionPipelineOverride = sharedProblem.executionPipeline?.storageValue ?: executionPipelineOverride
    )
}

internal fun ProblemComposerDraft.toSharedProblemFile(): SharedProblemFile {
    return SharedProblemFile(
        title = title.trim(),
        difficulty = difficulty.trim(),
        summary = effectiveSummary(),
        statementMarkdown = statementMarkdown.trim(),
        exampleInput = exampleInput.trim(),
        exampleOutput = exampleOutput.trim(),
        starterCode = effectiveStarterCode(),
        hints = hints(),
        submissionTestSuite = parsedSubmissionTestSuite(),
        executionPipeline = executionPipelineOverride
            .takeIf { it.isNotBlank() }
            ?.let(ProblemExecutionPipeline::fromStorage)
    )
}

internal fun ProblemComposerDraft.toSharedProblemJson(): String {
    return ProblemShareCodec.encodeToString(toSharedProblemFile())
}

internal fun ProblemComposerDraft.effectiveStarterCode(): String {
    return if (starterCodeMode == ProblemStarterCodeMode.Manual && starterCode.isNotBlank()) {
        starterCode
    } else {
        ProblemStarterCodeFactory.build(
            title = title.trim(),
            statementMarkdown = statementMarkdown.trim(),
            exampleInput = exampleInput.trim(),
            exampleOutput = exampleOutput.trim(),
            executionPipeline = executionPipelineOverride
                .takeIf { it.isNotBlank() }
                ?.let(ProblemExecutionPipeline::fromStorage)
                ?: ProblemExecutionPipelineResolver.infer(
                    title = title.trim(),
                    starterCode = starterCode
                )
        )
    }
}

private fun ProblemComposerDraft.normalizedExampleInput(
    effectiveStarterCode: String = effectiveStarterCode(),
    pipeline: ProblemExecutionPipeline = resolvedExecutionPipeline()
): String {
    return ProblemInputNormalizer.normalizeExampleInput(
        rawInput = exampleInput,
        starterCode = effectiveStarterCode,
        executionPipeline = pipeline
    )
}

private fun ProblemComposerDraft.autoSubmissionTestSuite(
    normalizedExampleInput: String = normalizedExampleInput()
): ProblemTestSuite {
    return ProblemSubmissionSuiteFactory.build(
        statementMarkdown = statementMarkdown.trim(),
        exampleInput = normalizedExampleInput,
        exampleOutput = exampleOutput.trim()
    )
}

internal fun ProblemComposerDraft.effectiveSummary(): String {
    val explicitSummary = summary.trim()
    if (explicitSummary.isNotBlank()) return explicitSummary

    val paragraph = statementMarkdown
        .lineSequence()
        .map { it.trim() }
        .dropWhile { it.isBlank() }
        .takeWhile { it.isNotBlank() }
        .joinToString(" ")
        .replace("**", "")
        .replace("`", "")
        .replace(Regex("""\s+"""), " ")
        .trim()

    return when {
        paragraph.isBlank() -> title.trim()
        paragraph.length <= 220 -> paragraph
        else -> paragraph.take(217).trimEnd() + "..."
    }
}

internal fun parseComposerHintsText(
    raw: String,
    preserveBlankEntries: Boolean = false
): List<String> {
    if (raw.isEmpty()) return emptyList()

    val trimmed = raw.trim()
    if (trimmed.startsWith("[")) {
        runCatching {
            val json = JSONArray(trimmed)
            return List(json.length()) { index -> json.optString(index) }
                .let { hints ->
                    if (preserveBlankEntries) hints else hints.filter { it.isNotBlank() }
                }
        }
    }

    val legacyHints = raw
        .replace("\r\n", "\n")
        .split("\n")
        .map { it.trimEnd() }

    return if (preserveBlankEntries) {
        legacyHints.filterIndexed { index, hint ->
            hint.isNotBlank() || index < legacyHints.lastIndex
        }
    } else {
        legacyHints.filter { it.isNotBlank() }
    }
}

internal fun serializeComposerHints(hints: List<String>): String {
    if (hints.isEmpty()) return ""
    return JSONArray().apply {
        hints.forEach { put(it) }
    }.toString()
}
