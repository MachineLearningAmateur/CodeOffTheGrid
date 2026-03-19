package dev.kaixinguo.standalonecodepractice.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemFolderState
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemListItem
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemSetState
import org.json.JSONArray
import org.json.JSONObject

internal class BundledProblemCatalogLoader(
    private val context: Context
) {
    fun loadCatalog(assetName: String = "neetcode150_catalog.json"): List<ProblemFolderState> {
        val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val folders = root.optJSONArray("folders") ?: JSONArray()
        return List(folders.length()) { index ->
            folders.getJSONObject(index).toFolderState()
        }
    }

    private fun JSONObject.toFolderState(): ProblemFolderState {
        val setsJson = optJSONArray("sets") ?: JSONArray()
        return ProblemFolderState(
            id = getString("id"),
            title = getString("title"),
            sets = mutableStateListOf<ProblemSetState>().apply {
                repeat(setsJson.length()) { index ->
                    add(setsJson.getJSONObject(index).toSetState())
                }
            }
        )
    }

    private fun JSONObject.toSetState(): ProblemSetState {
        val problemsJson = optJSONArray("problems") ?: JSONArray()
        return ProblemSetState(
            id = getString("id"),
            title = getString("title"),
            problems = mutableStateListOf<ProblemListItem>().apply {
                repeat(problemsJson.length()) { index ->
                    add(problemsJson.getJSONObject(index).toProblemItem())
                }
            }
        )
    }

    private fun JSONObject.toProblemItem(): ProblemListItem {
        val hintsJson = optJSONArray("hints") ?: JSONArray()
        return ProblemListItem(
            id = getString("id"),
            title = getString("title"),
            difficulty = optString("difficulty"),
            active = optBoolean("active", false),
            solved = optBoolean("solved", false),
            summary = optString("summary"),
            statementMarkdown = optString("statementMarkdown"),
            exampleInput = optString("exampleInput"),
            exampleOutput = optString("exampleOutput"),
            starterCode = optString("starterCode"),
            customTests = optString("customTests"),
            hints = List(hintsJson.length()) { index -> hintsJson.optString(index) }
                .filter { it.isNotBlank() }
        )
    }
}
