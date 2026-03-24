package dev.kaixinguo.codeoffthegrid.data

import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemExecutionPipeline
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestSuite
import org.json.JSONObject

internal data class SharedProblemFile(
    val title: String,
    val difficulty: String,
    val summary: String,
    val statementMarkdown: String,
    val exampleInput: String,
    val exampleOutput: String,
    val starterCode: String,
    val hints: List<String>,
    val submissionTestSuite: ProblemTestSuite?,
    val executionPipeline: ProblemExecutionPipeline?
)

internal object ProblemShareCodec {
    private const val CURRENT_SCHEMA_VERSION = 1
    private const val FILE_TYPE = "standalone_code_practice_problem"

    fun decodeFromString(serialized: String): SharedProblemFile {
        val root = JSONObject(serialized)
        val schemaVersion = root.optInt("schemaVersion", -1)
        require(schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Unsupported problem file version: $schemaVersion"
        }
        val type = root.optString("type")
        require(type == FILE_TYPE) {
            "Unsupported problem file type: $type"
        }
        val problemJson = root.optJSONObject("problem")
            ?: throw IllegalArgumentException("Problem payload was not found.")

        val executionPipeline = problemJson.optString("executionPipeline")
            .takeIf { it.isNotBlank() }
            ?.let(ProblemExecutionPipeline::fromStorage)

        return SharedProblemFile(
            title = problemJson.optString("title"),
            difficulty = problemJson.optString("difficulty"),
            summary = problemJson.optString("summary"),
            statementMarkdown = problemJson.optString("statementMarkdown"),
            exampleInput = problemJson.optString("exampleInput"),
            exampleOutput = problemJson.optString("exampleOutput"),
            starterCode = problemJson.optString("starterCode"),
            hints = buildList {
                val hintsJson = problemJson.optJSONArray("hints")
                if (hintsJson != null) {
                    for (index in 0 until hintsJson.length()) {
                        val hint = hintsJson.optString(index).trim()
                        if (hint.isNotBlank()) add(hint)
                    }
                }
            },
            submissionTestSuite = ProblemTestSuiteJsonCodec.decodeFromJson(
                problemJson.optJSONObject("submissionTestSuite")
            ),
            executionPipeline = executionPipeline
        )
    }

    fun encodeToString(problem: SharedProblemFile): String {
        return JSONObject()
            .put("schemaVersion", CURRENT_SCHEMA_VERSION)
            .put("type", FILE_TYPE)
            .put(
                "problem",
                JSONObject()
                    .put("title", problem.title)
                    .put("difficulty", problem.difficulty)
                    .put("summary", problem.summary)
                    .put("statementMarkdown", problem.statementMarkdown)
                    .put("exampleInput", problem.exampleInput)
                    .put("exampleOutput", problem.exampleOutput)
                    .put("starterCode", problem.starterCode)
                    .put(
                        "hints",
                        org.json.JSONArray().apply {
                            problem.hints.forEach { put(it) }
                        }
                    )
                    .apply {
                        problem.submissionTestSuite?.let {
                            put("submissionTestSuite", ProblemTestSuiteJsonCodec.encodeToJson(it))
                        }
                        problem.executionPipeline?.let {
                            put("executionPipeline", it.storageValue)
                        }
                    }
            )
            .toString(2)
    }
}
