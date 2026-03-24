package dev.kaixinguo.codeoffthegrid.ai

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

internal interface AiRuntimeController {
    val runtimeState: StateFlow<AiRuntimeState>

    suspend fun importModel(uri: Uri)

    suspend fun downloadPresetModel(preset: AiModelPreset)

    suspend fun selectStoredModel(path: String)

    suspend fun loadConfiguredModel()

    suspend fun unloadModel()

    suspend fun removeConfiguredModel()

    fun destroy()
}
