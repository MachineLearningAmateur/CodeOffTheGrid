package dev.kaixinguo.standalonecodepractice.data

import androidx.compose.runtime.mutableStateListOf
import dev.kaixinguo.standalonecodepractice.data.local.ProblemCatalogDao
import dev.kaixinguo.standalonecodepractice.data.local.ProblemFolderEntity
import dev.kaixinguo.standalonecodepractice.data.local.ProblemSetEntity
import dev.kaixinguo.standalonecodepractice.data.local.StoredProblemEntity
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemFolderState
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemExecutionPipeline
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemListItem
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemSetState
import org.json.JSONArray

internal class ProblemCatalogRepository(
    private val problemCatalogDao: ProblemCatalogDao
) {
    suspend fun loadCatalogOrSeed(seedFolders: List<ProblemFolderState>): List<ProblemFolderState> {
        val loaded = loadCatalog()
        if (loaded.isNotEmpty()) return loaded

        persistCatalog(seedFolders)
        return loadCatalog()
    }

    suspend fun loadCatalog(): List<ProblemFolderState> {
        val folders = problemCatalogDao.getFolders()
        if (folders.isEmpty()) return emptyList()

        val setsByFolderId = problemCatalogDao.getSets().groupBy { it.folderId }
        val problemsBySetId = problemCatalogDao.getProblems().groupBy { it.setId }

        return folders.map { folder ->
            ProblemFolderState(
                id = folder.id,
                title = folder.title,
                sets = mutableStateListOf<ProblemSetState>().apply {
                    addAll(
                        setsByFolderId[folder.id].orEmpty().map { set ->
                            ProblemSetState(
                                id = set.id,
                                title = set.title,
                                problems = mutableStateListOf<ProblemListItem>().apply {
                                    addAll(
                                        problemsBySetId[set.id].orEmpty().map { problem ->
                                            problem.toModel()
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
    }

    suspend fun persistCatalog(folders: List<ProblemFolderState>) {
        val folderEntities = mutableListOf<ProblemFolderEntity>()
        val setEntities = mutableListOf<ProblemSetEntity>()
        val problemEntities = mutableListOf<StoredProblemEntity>()

        folders.forEachIndexed { folderIndex, folder ->
            folderEntities += ProblemFolderEntity(
                id = folder.id,
                title = folder.title,
                sortIndex = folderIndex
            )
            folder.sets.forEachIndexed { setIndex, set ->
                setEntities += ProblemSetEntity(
                    id = set.id,
                    folderId = folder.id,
                    title = set.title,
                    sortIndex = setIndex
                )
                set.problems.forEachIndexed { problemIndex, problem ->
                    problemEntities += problem.toEntity(
                        setId = set.id,
                        sortIndex = problemIndex
                    )
                }
            }
        }

        problemCatalogDao.replaceCatalog(
            folders = folderEntities,
            sets = setEntities,
            problems = problemEntities
        )
    }

    private fun StoredProblemEntity.toModel(): ProblemListItem {
        val pipeline = ProblemExecutionPipeline.fromStorage(executionPipeline)
        val normalizedExampleInput = ProblemInputNormalizer.normalizeExampleInput(
            rawInput = exampleInput,
            starterCode = starterCode,
            executionPipeline = pipeline
        )
        val decodedSubmissionSuite = ProblemTestSuiteJsonCodec.decodeFromString(submissionTestSuiteJson)
        return ProblemListItem(
            id = id,
            title = title,
            difficulty = difficulty,
            active = active,
            solved = solved,
            summary = summary,
            statementMarkdown = statementMarkdown,
            exampleInput = normalizedExampleInput,
            exampleOutput = exampleOutput,
            starterCode = starterCode,
            customTests = customTests,
            hints = decodeHints(hintsJson),
            submissionTestSuite = ProblemInputNormalizer.normalizeSubmissionTestSuite(
                testSuite = decodedSubmissionSuite
                    ?: ProblemSubmissionSuiteFactory.build(
                        statementMarkdown = statementMarkdown,
                        exampleInput = normalizedExampleInput,
                        exampleOutput = exampleOutput
                    ),
                starterCode = starterCode,
                executionPipeline = pipeline
            ),
            executionPipeline = pipeline
        )
    }

    private fun ProblemListItem.toEntity(setId: String, sortIndex: Int): StoredProblemEntity {
        return StoredProblemEntity(
            id = id,
            setId = setId,
            title = title,
            difficulty = difficulty,
            active = active,
            solved = solved,
            summary = summary,
            statementMarkdown = statementMarkdown,
            exampleInput = exampleInput,
            exampleOutput = exampleOutput,
            starterCode = starterCode,
            customTests = customTests,
            hintsJson = encodeHints(hints),
            submissionTestSuiteJson = ProblemTestSuiteJsonCodec.encodeToString(
                submissionTestSuite.takeIf { it.cases.isNotEmpty() }
                    ?: ProblemSubmissionSuiteFactory.build(this)
            ),
            executionPipeline = executionPipeline.storageValue,
            sortIndex = sortIndex
        )
    }

    private fun encodeHints(hints: List<String>): String {
        return JSONArray().apply {
            hints.forEach { put(it) }
        }.toString()
    }

    private fun decodeHints(hintsJson: String): List<String> {
        if (hintsJson.isBlank()) return emptyList()
        val hints = JSONArray(hintsJson)
        return List(hints.length()) { index -> hints.optString(index) }
            .filter { it.isNotBlank() }
    }
}
