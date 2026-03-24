package dev.kaixinguo.codeoffthegrid.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class ProblemCatalogManifest(
    val version: Int,
    val collection: ProblemCollectionManifest,
    val sets: List<ProblemSetManifest>
)

internal data class ProblemCollectionManifest(
    val id: String,
    val title: String,
    val totalProblems: Int,
    val fullContentProblems: Int,
    val placeholderProblems: Int,
    val sourceLabel: String,
    val sourceUrl: String
)

internal data class ProblemSetManifest(
    val id: String,
    val title: String,
    val totalProblems: Int,
    val fullContentProblems: Int,
    val placeholderProblems: Int,
    val problems: List<ProblemManifestEntry>
)

internal data class ProblemManifestEntry(
    val id: String,
    val title: String,
    val difficulty: String,
    val contentStatus: String,
    val executionPipeline: String,
    val questionUrl: String,
    val solutionUrl: String,
    val leetcodeUrl: String
)

internal class BundledProblemCatalogManifestLoader(
    private val context: Context
) {
    fun loadManifest(assetName: String = "neetcode150_manifest.json"): ProblemCatalogManifest {
        val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        return ProblemCatalogManifest(
            version = root.optInt("version", 1),
            collection = root.getJSONObject("collection").toCollectionManifest(),
            sets = (root.optJSONArray("sets") ?: JSONArray()).toProblemSetManifests()
        )
    }

    private fun JSONObject.toCollectionManifest(): ProblemCollectionManifest {
        return ProblemCollectionManifest(
            id = getString("id"),
            title = getString("title"),
            totalProblems = getInt("totalProblems"),
            fullContentProblems = getInt("fullContentProblems"),
            placeholderProblems = getInt("placeholderProblems"),
            sourceLabel = optString("sourceLabel"),
            sourceUrl = optString("sourceUrl")
        )
    }

    private fun JSONArray.toProblemSetManifests(): List<ProblemSetManifest> {
        return List(length()) { index ->
            getJSONObject(index).toProblemSetManifest()
        }
    }

    private fun JSONObject.toProblemSetManifest(): ProblemSetManifest {
        return ProblemSetManifest(
            id = getString("id"),
            title = getString("title"),
            totalProblems = getInt("totalProblems"),
            fullContentProblems = getInt("fullContentProblems"),
            placeholderProblems = getInt("placeholderProblems"),
            problems = (optJSONArray("problems") ?: JSONArray()).toProblemManifestEntries()
        )
    }

    private fun JSONArray.toProblemManifestEntries(): List<ProblemManifestEntry> {
        return List(length()) { index ->
            getJSONObject(index).toProblemManifestEntry()
        }
    }

    private fun JSONObject.toProblemManifestEntry(): ProblemManifestEntry {
        return ProblemManifestEntry(
            id = getString("id"),
            title = getString("title"),
            difficulty = optString("difficulty"),
            contentStatus = optString("contentStatus", "unknown"),
            executionPipeline = optString("executionPipeline", "single_method"),
            questionUrl = optString("questionUrl"),
            solutionUrl = optString("solutionUrl"),
            leetcodeUrl = optString("leetcodeUrl")
        )
    }
}
