package dev.kaixinguo.codeoffthegrid.data

import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestCase
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestComparisonMode
import dev.kaixinguo.codeoffthegrid.ui.workspace.ProblemTestSuite
import org.json.JSONArray
import org.json.JSONObject

internal object ProblemTestSuiteJsonCodec {
    fun encodeToString(testSuite: ProblemTestSuite): String {
        return encodeToJson(testSuite).toString()
    }

    fun encodeToJson(testSuite: ProblemTestSuite, includeIds: Boolean = true): JSONObject {
        val cases = JSONArray()
        testSuite.cases.forEach { testCase ->
            val caseJson = JSONObject()
                .put("label", testCase.label)
                .put("stdin", testCase.stdin)
                .put("expectedOutput", testCase.expectedOutput)
                .put("enabled", testCase.enabled)
                .put("comparisonMode", testCase.comparisonMode.storageValue)
                .put(
                    "acceptableOutputs",
                    JSONArray().apply {
                        testCase.acceptableOutputs.forEach { put(it) }
                    }
                )
            if (includeIds) {
                caseJson.put("id", testCase.id)
            }
            cases.put(caseJson)
        }
        return JSONObject().put("cases", cases)
    }

    fun decodeFromString(serialized: String): ProblemTestSuite? {
        if (serialized.isBlank()) return null
        return runCatching {
            decodeFromJson(JSONObject(serialized))
        }.getOrNull()
    }

    fun decodeFromJson(json: JSONObject?): ProblemTestSuite? {
        json ?: return null
        val casesJson = json.optJSONArray("cases") ?: JSONArray()
        return ProblemTestSuite(
            draft = json.optString("draft"),
            cases = buildList {
                for (index in 0 until casesJson.length()) {
                    val caseJson = casesJson.optJSONObject(index) ?: continue
                    add(
                        ProblemTestCase(
                            id = caseJson.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                            label = caseJson.optString("label").ifBlank { "Case ${index + 1}" },
                            stdin = caseJson.optString("stdin"),
                            expectedOutput = caseJson.optString("expectedOutput"),
                            enabled = caseJson.optBoolean("enabled", true),
                            comparisonMode = ProblemTestComparisonMode.fromStorage(
                                caseJson.optString("comparisonMode")
                            ),
                            acceptableOutputs = buildList {
                                val acceptableOutputsJson = caseJson.optJSONArray("acceptableOutputs")
                                    ?: JSONArray()
                                for (variantIndex in 0 until acceptableOutputsJson.length()) {
                                    val variant = acceptableOutputsJson.optString(variantIndex)
                                    if (variant.isNotBlank()) add(variant)
                                }
                            }
                        )
                    )
                }
            }
        )
    }
}
