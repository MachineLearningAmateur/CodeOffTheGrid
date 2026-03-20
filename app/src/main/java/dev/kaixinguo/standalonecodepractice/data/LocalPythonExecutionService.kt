package dev.kaixinguo.standalonecodepractice.data

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dev.kaixinguo.standalonecodepractice.ui.workspace.ExecutionStatus
import dev.kaixinguo.standalonecodepractice.ui.workspace.ExecutionTarget
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemExecutionResult
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemExecutionPipeline
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestCase
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestCaseResult
import dev.kaixinguo.standalonecodepractice.ui.workspace.ProblemTestSuite
import dev.kaixinguo.standalonecodepractice.ui.workspace.TestCaseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class LocalPythonExecutionService(
    context: Context
) {
    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context.applicationContext))
        }
    }

    suspend fun runCustomSuite(
        testSuite: ProblemTestSuite,
        draftCode: String,
        executionPipeline: ProblemExecutionPipeline = ProblemExecutionPipeline.SingleMethod
    ): ProblemExecutionResult = execute(
        target = ExecutionTarget.Custom,
        title = "Custom Run",
        draftCode = draftCode,
        executionPipeline = executionPipeline,
        testSuite = testSuite
    )

    suspend fun runLocalSubmission(
        testSuite: ProblemTestSuite,
        draftCode: String,
        executionPipeline: ProblemExecutionPipeline = ProblemExecutionPipeline.SingleMethod
    ): ProblemExecutionResult = execute(
        target = ExecutionTarget.LocalSubmission,
        title = "Local Submission",
        draftCode = draftCode,
        executionPipeline = executionPipeline,
        testSuite = testSuite
    )

    private suspend fun execute(
        target: ExecutionTarget,
        title: String,
        draftCode: String,
        executionPipeline: ProblemExecutionPipeline,
        testSuite: ProblemTestSuite
    ): ProblemExecutionResult = withContext(Dispatchers.Default) {
        runCatching {
            val python = Python.getInstance()
            val module = python.getModule("standalone_runner")
            val payload = module.callAttr(
                "run_suite",
                draftCode,
                encodeSuite(testSuite, executionPipeline)
            ).toString()
            decodeResult(
                target = target,
                title = title,
                payload = payload
            )
        }.getOrElse { error ->
            ProblemExecutionResult(
                target = target,
                status = ExecutionStatus.Error,
                title = "$title Error",
                summary = error.message ?: "Python runtime failed before execution started.",
                stderr = error.stackTraceToString()
            )
        }
    }

    private fun encodeSuite(
        testSuite: ProblemTestSuite,
        executionPipeline: ProblemExecutionPipeline
    ): String {
        val cases = JSONArray()
        testSuite.cases.forEach { testCase ->
            cases.put(
                JSONObject()
                    .put("id", testCase.id)
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
            )
        }
        return JSONObject()
            .put("draft", testSuite.draft)
            .put("executionPipeline", executionPipeline.storageValue)
            .put("cases", cases)
            .toString()
    }

    private fun decodeResult(
        target: ExecutionTarget,
        title: String,
        payload: String
    ): ProblemExecutionResult {
        val json = JSONObject(payload)
        val casesJson = json.optJSONArray("cases") ?: JSONArray()
        val normalizedSummary = when (target) {
            ExecutionTarget.Custom -> json.optString("summary")
            ExecutionTarget.LocalSubmission -> json.optString("summary")
                .replace("enabled custom cases", "enabled built-in submission cases")
                .replace("local checks", "local submission")
        }
        return ProblemExecutionResult(
            target = target,
            status = when (json.optString("status").lowercase()) {
                "passed" -> ExecutionStatus.Passed
                "failed" -> ExecutionStatus.Failed
                "error" -> ExecutionStatus.Error
                "running" -> ExecutionStatus.Running
                else -> ExecutionStatus.Idle
            },
            title = when (val summaryTitle = json.optString("status").lowercase()) {
                "passed" -> "$title Passed"
                "failed" -> "$title Failed"
                "error" -> "$title Error"
                else -> title
            },
            summary = normalizedSummary,
            cases = List(casesJson.length()) { index ->
                casesJson.getJSONObject(index).toCaseResult()
            },
            stdout = json.optString("stdout"),
            stderr = json.optString("stderr")
        )
    }

    private fun JSONObject.toCaseResult(): ProblemTestCaseResult {
        return ProblemTestCaseResult(
            testCaseId = optString("testCaseId"),
            label = optString("label"),
            status = when (optString("status").lowercase()) {
                "passed" -> TestCaseStatus.Passed
                "failed" -> TestCaseStatus.Failed
                "error" -> TestCaseStatus.Error
                "skipped" -> TestCaseStatus.Skipped
                else -> TestCaseStatus.Pending
            },
            input = optString("input"),
            actualOutput = optString("actualOutput"),
            expectedOutput = optString("expectedOutput"),
            errorOutput = optString("errorOutput"),
            durationMillis = if (has("durationMillis") && !isNull("durationMillis")) {
                optLong("durationMillis")
            } else {
                null
            }
        )
    }
}
