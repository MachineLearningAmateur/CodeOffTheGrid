package dev.kaixinguo.codeoffthegrid.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

internal class HostLlamaCppQwenEngine(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val connectTimeoutMillis: Int = 5_000,
    private val readTimeoutMillis: Int = 180_000
) : LocalQwenEngine {
    @Volatile
    private var cachedModelId: String? = null

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val chatAttempt = runCatching { generateFromChatCompletions(prompt) }
        if (chatAttempt.isSuccess) {
            return@withContext chatAttempt.getOrThrow()
        }

        val completionAttempt = runCatching { generateFromCompletion(prompt) }
        if (completionAttempt.isSuccess) {
            return@withContext completionAttempt.getOrThrow()
        }

        val chatMessage = chatAttempt.exceptionOrNull()?.message.orEmpty()
        val completionMessage = completionAttempt.exceptionOrNull()?.message.orEmpty()
        throw IllegalStateException(
            buildString {
                append("Unable to reach the local Qwen runtime at ")
                append(baseUrl)
                append(". Start llama-server on the host machine and retry.")
                if (chatMessage.isNotBlank()) {
                    append(" Chat API: ")
                    append(chatMessage)
                    append(".")
                }
                if (completionMessage.isNotBlank()) {
                    append(" Completion API: ")
                    append(completionMessage)
                    append(".")
                }
            },
            completionAttempt.exceptionOrNull() ?: chatAttempt.exceptionOrNull()
        )
    }

    private fun generateFromChatCompletions(prompt: String): String {
        val modelId = cachedModelId ?: discoverModelId()
        val requestBody = JSONObject()
            .put("model", modelId)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
                )
            )
            .put("stream", false)
            .put("temperature", 0.2)
            .put("max_tokens", 700)

        val response = requestJson(
            path = "/v1/chat/completions",
            method = "POST",
            requestBody = requestBody.toString()
        )
        val content = response.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            .orEmpty()
            .trim()
        if (content.isBlank()) {
            error("Chat completion response did not include content")
        }
        return content
    }

    private fun generateFromCompletion(prompt: String): String {
        val requestBody = JSONObject()
            .put("prompt", prompt)
            .put("n_predict", 700)
            .put("temperature", 0.2)
            .put("cache_prompt", true)

        val response = requestJson(
            path = "/completion",
            method = "POST",
            requestBody = requestBody.toString()
        )
        val content = response.optString("content").trim()
        if (content.isNotBlank()) {
            return content
        }

        val legacyChoiceContent = response.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()
            .trim()
        if (legacyChoiceContent.isBlank()) {
            error("Completion response did not include content")
        }
        return legacyChoiceContent
    }

    private fun discoverModelId(): String {
        val response = requestJson(
            path = "/v1/models",
            method = "GET"
        )
        val modelId = response.optJSONArray("data")
            ?.optJSONObject(0)
            ?.optString("id")
            .orEmpty()
            .ifBlank { FALLBACK_MODEL_ID }
        cachedModelId = modelId
        return modelId
    }

    private fun requestJson(
        path: String,
        method: String,
        requestBody: String? = null
    ): JSONObject {
        val connection = openConnection(path = path, method = method)
        return try {
            if (requestBody != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(requestBody)
                }
            }

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection, responseCode)
            if (responseCode !in 200..299) {
                error(
                    "HTTP $responseCode${responseText.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()}"
                )
            }
            if (responseText.isBlank()) {
                error("Empty response")
            }
            JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(path: String, method: String): HttpURLConnection {
        return (URL("${baseUrl.trimEnd('/')}/${path.trimStart('/')}").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            doInput = true
            useCaches = false
            setRequestProperty("Accept", "application/json")
            if (method != "GET") {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
    }

    private fun readResponseText(connection: HttpURLConnection, responseCode: Int): String {
        val responseStream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        } ?: return ""

        return BufferedReader(InputStreamReader(responseStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    internal companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"

        fun defaultEndpointLabel(): String {
            return DEFAULT_BASE_URL.removePrefix("http://")
        }

        private const val FALLBACK_MODEL_ID = "local-model"
    }
}
