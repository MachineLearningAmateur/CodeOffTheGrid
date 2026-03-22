package dev.kaixinguo.standalonecodepractice.ui.workspace

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.kaixinguo.standalonecodepractice.ai.AiAssistant
import dev.kaixinguo.standalonecodepractice.ai.AiRuntimeController
import dev.kaixinguo.standalonecodepractice.ai.DefaultAiAssistant
import dev.kaixinguo.standalonecodepractice.ai.DefaultPromptBuilder
import dev.kaixinguo.standalonecodepractice.ai.OnDeviceLlamaCppQwenEngine
import dev.kaixinguo.standalonecodepractice.ai.PromptMode
import dev.kaixinguo.standalonecodepractice.data.SharedProblemFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal interface AskAiSessionController {
    val sessionState: AskAiSessionState
    val aiRuntimeController: AiRuntimeController
    val activeRequestElapsedMillis: Long
    val pendingSideEffect: AskAiPendingSideEffect?

    fun syncContext(composerActive: Boolean)
    fun submitRequest(request: AskAiRequest)
    fun cancelActiveRequest()
    fun clearChat()
    fun completePendingSideEffect(sender: AiChatSender, message: String)
}

internal data class AskAiRequest(
    val mode: PromptMode,
    val problemPrompt: String,
    val code: String?,
    val explicitRequest: String?,
    val userMessage: String,
    val clearPromptDraftAfterSubmit: Boolean,
    val targetProblemId: String? = null
)

internal sealed interface AskAiPendingSideEffect {
    val mode: PromptMode

    data class GeneratedProblem(
        val generatedProblem: SharedProblemFile
    ) : AskAiPendingSideEffect {
        override val mode: PromptMode = PromptMode.CREATE_PROBLEM
    }

    data class GeneratedCustomTests(
        val problemId: String?,
        val generatedSuite: ProblemTestSuite
    ) : AskAiPendingSideEffect {
        override val mode: PromptMode = PromptMode.TEST_CASES
    }
}

internal class AskAiViewModel(
    application: Application
) : AndroidViewModel(application), AskAiSessionController {

    private val runtimeController = OnDeviceLlamaCppQwenEngine(
        context = application,
        sharedPreferences = application.getSharedPreferences(APP_SETTINGS_PREFS, Context.MODE_PRIVATE)
    )

    private val aiAssistant: AiAssistant = DefaultAiAssistant(
        promptBuilder = DefaultPromptBuilder(),
        localQwenEngine = runtimeController
    )

    override val sessionState = AskAiSessionState()
    override val aiRuntimeController: AiRuntimeController = runtimeController

    override var pendingSideEffect by mutableStateOf<AskAiPendingSideEffect?>(null)
        private set

    override var activeRequestElapsedMillis by mutableLongStateOf(0L)
        private set

    private var activeRequestTimerJob: Job? = null

    override fun syncContext(composerActive: Boolean) {
        sessionState.syncContext(composerActive)
    }

    override fun submitRequest(request: AskAiRequest) {
        if (sessionState.isLoading) return

        sessionState.messages = sessionState.messages + AiChatMessage(
            sender = AiChatSender.User,
            mode = request.mode,
            text = request.userMessage
        )
        if (request.clearPromptDraftAfterSubmit) {
            sessionState.promptDraft = ""
        }
        sessionState.isLoading = true
        sessionState.activeRequestMode = request.mode
        pendingSideEffect = null

        val requestJob = viewModelScope.launch {
            startRequestTimer()
            try {
                when (request.mode) {
                    PromptMode.CREATE_PROBLEM -> {
                        val generatedProblem = aiAssistant.createProblem(
                            problem = request.problemPrompt,
                            code = request.code,
                            request = request.explicitRequest
                        )
                        pendingSideEffect = AskAiPendingSideEffect.GeneratedProblem(generatedProblem)
                    }

                    PromptMode.HINT -> {
                        appendAssistantMessage(
                            mode = request.mode,
                            text = aiAssistant.generateHint(
                                problem = request.problemPrompt,
                                code = request.code,
                                request = request.explicitRequest
                            )
                        )
                    }

                    PromptMode.EXPLAIN -> {
                        appendAssistantMessage(
                            mode = request.mode,
                            text = aiAssistant.explainSolution(
                                problem = request.problemPrompt,
                                code = request.code,
                                request = request.explicitRequest
                            )
                        )
                    }

                    PromptMode.REVIEW_CODE -> {
                        appendAssistantMessage(
                            mode = request.mode,
                            text = aiAssistant.reviewCode(
                                problem = request.problemPrompt,
                                code = request.code,
                                request = request.explicitRequest
                            )
                        )
                    }

                    PromptMode.TEST_CASES -> {
                        val generatedSuite = aiAssistant.generateCustomTests(
                            problem = request.problemPrompt,
                            code = null,
                            request = request.explicitRequest
                        )
                        pendingSideEffect = AskAiPendingSideEffect.GeneratedCustomTests(
                            problemId = request.targetProblemId,
                            generatedSuite = generatedSuite
                        )
                    }

                    PromptMode.SOLVE -> {
                        appendAssistantMessage(
                            mode = request.mode,
                            text = aiAssistant.solveProblem(
                                problem = request.problemPrompt,
                                code = request.code,
                                request = request.explicitRequest
                            )
                        )
                    }
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    appendSystemMessage(
                        mode = request.mode,
                        text = "Request canceled."
                    )
                } else {
                    appendSystemMessage(
                        mode = request.mode,
                        text = throwable.message ?: "Unknown AI runtime error"
                    )
                }
            } finally {
                stopRequestTimer()
                sessionState.activeRequestJob = null
                sessionState.activeRequestMode = null
                sessionState.isLoading = false
            }
        }

        sessionState.activeRequestJob = requestJob
    }

    override fun cancelActiveRequest() {
        sessionState.activeRequestJob?.cancel()
    }

    override fun clearChat() {
        if (sessionState.isLoading) return
        sessionState.messages = emptyList()
    }

    override fun completePendingSideEffect(sender: AiChatSender, message: String) {
        val effect = pendingSideEffect ?: return
        pendingSideEffect = null
        val trimmedMessage = message.trim()
        if (trimmedMessage.isBlank()) return
        sessionState.messages = sessionState.messages + AiChatMessage(
            sender = sender,
            mode = effect.mode,
            text = trimmedMessage
        )
    }

    override fun onCleared() {
        stopRequestTimer()
        runtimeController.destroy()
        super.onCleared()
    }

    private fun appendAssistantMessage(mode: PromptMode, text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        sessionState.messages = sessionState.messages + AiChatMessage(
            sender = AiChatSender.Assistant,
            mode = mode,
            text = trimmed
        )
    }

    private fun appendSystemMessage(mode: PromptMode, text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        sessionState.messages = sessionState.messages + AiChatMessage(
            sender = AiChatSender.System,
            mode = mode,
            text = trimmed
        )
    }

    private fun startRequestTimer() {
        stopRequestTimer()
        val startElapsedRealtime = SystemClock.elapsedRealtime()
        activeRequestElapsedMillis = 0L
        activeRequestTimerJob = viewModelScope.launch {
            while (isActive) {
                activeRequestElapsedMillis = SystemClock.elapsedRealtime() - startElapsedRealtime
                delay(1_000L)
            }
        }
    }

    private fun stopRequestTimer() {
        activeRequestTimerJob?.cancel()
        activeRequestTimerJob = null
        activeRequestElapsedMillis = 0L
    }

    private companion object {
        const val APP_SETTINGS_PREFS = "app_settings"
    }
}
