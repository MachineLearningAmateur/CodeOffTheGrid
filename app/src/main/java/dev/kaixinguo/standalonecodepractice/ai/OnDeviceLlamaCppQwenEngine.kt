package dev.kaixinguo.standalonecodepractice.ai

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.UnsupportedArchitectureException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.net.URL
import java.util.Locale

internal class OnDeviceLlamaCppQwenEngine(
    context: Context,
    private val sharedPreferences: SharedPreferences,
    private val inferenceEngineFactory: (Context) -> InferenceEngine = { appContext ->
        AiChat.getInferenceEngine(appContext)
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalQwenEngine, AiRuntimeController {
    private val appContext = context.applicationContext
    private val operationMutex = Mutex()
    private val _runtimeState = MutableStateFlow(initialRuntimeState())
    override val runtimeState: StateFlow<AiRuntimeState> = _runtimeState.asStateFlow()
    private val runtimeScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private var inferenceEngine: InferenceEngine? = null
    private var modelLoaded = false

    init {
        if (_runtimeState.value.currentModelPath != null) {
            runtimeScope.launch {
                runCatching {
                    operationMutex.withLock {
                        loadConfiguredModelLocked()
                    }
                }
            }
        }
    }

    override suspend fun generate(prompt: String): String = operationMutex.withLock {
        require(prompt.isNotBlank()) { "AI prompt cannot be empty." }
        ensureModelLoadedLocked()

        val currentModel = currentConfiguredModel()
        val installedModels = installedModels()
        _runtimeState.value = AiRuntimeState(
            phase = AiRuntimePhase.Generating,
            preset = currentModel?.preset,
            currentModelPath = currentModel?.path,
            modelName = currentModel?.name,
            installedModels = installedModels,
            detail = "Generating response on-device."
        )

        try {
            val engine = getOrCreateInferenceEngine()
            engine.resetConversation()

            val response = StringBuilder()
            engine.sendUserPrompt(
                message = prompt,
                predictLength = DEFAULT_PREDICT_LENGTH
            ).collect { token ->
                response.append(token)
            }

            val content = response.toString().trim()
            check(content.isNotBlank()) { "The on-device model returned an empty response." }
            _runtimeState.value = readyState(currentModel = currentModel, installedModels = installedModels)
            content
        } catch (throwable: Throwable) {
            modelLoaded = false
            _runtimeState.value = errorState(
                currentModel = currentModel,
                installedModels = installedModels,
                detail = throwable.message ?: "On-device generation failed."
            )
            throw throwable
        }
    }

    override suspend fun importModel(uri: Uri) = operationMutex.withLock {
        val displayName = queryDisplayName(uri)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_MODEL_FILENAME
        require(displayName.endsWith(MODEL_EXTENSION, ignoreCase = true)) {
            "Select a .gguf model file."
        }

        _runtimeState.value = AiRuntimeState(
            phase = AiRuntimePhase.Importing,
            modelName = displayName,
            detail = "Copying $displayName into app storage."
        )

        try {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val copiedModel = withContext(ioDispatcher) {
                copyModelToAppStorage(uri = uri, displayName = displayName)
            }
            unloadIfNeeded()
            val importedModel = AiStoredModel(
                path = copiedModel.absolutePath,
                name = copiedModel.name,
                preset = null
            )
            val updatedModels = upsertInstalledModel(importedModel)
            persistConfiguredModel(importedModel)
            _runtimeState.value = configuredState(
                currentModel = importedModel,
                installedModels = updatedModels,
                detail = "Model imported. Loading into memory."
            )
            loadConfiguredModelLocked()
        } catch (throwable: Throwable) {
            modelLoaded = false
            _runtimeState.value = errorState(
                currentModel = AiStoredModel(path = "", name = displayName, preset = null),
                installedModels = installedModels(),
                detail = throwable.message ?: "Model import failed."
            )
            throw throwable
        }
    }

    override suspend fun downloadPresetModel(preset: AiModelPreset) = operationMutex.withLock {
        _runtimeState.value = AiRuntimeState(
            phase = AiRuntimePhase.Downloading,
            preset = preset,
            modelName = preset.filename,
            detail = "Starting download for ${preset.shortLabel}.",
            progress = 0f
        )

        try {
            val downloadedModel = withContext(ioDispatcher) {
                downloadPresetIntoAppStorage(preset)
            }
            unloadIfNeeded()
            val storedModel = AiStoredModel(
                path = downloadedModel.absolutePath,
                name = downloadedModel.name,
                preset = preset
            )
            val updatedModels = upsertInstalledModel(storedModel)
            persistConfiguredModel(storedModel)
            _runtimeState.value = configuredState(
                currentModel = storedModel,
                installedModels = updatedModels,
                detail = "${preset.shortLabel} downloaded. Loading into memory."
            )
            try {
                loadConfiguredModelLocked()
            } catch (throwable: Throwable) {
                modelLoaded = false
                _runtimeState.value = errorState(
                    currentModel = storedModel,
                    installedModels = updatedModels,
                    detail = resolvePostDownloadLoadFailureDetail(
                        throwable = throwable,
                        supportedAbis = Build.SUPPORTED_ABIS.toList(),
                        fingerprint = Build.FINGERPRINT.orEmpty(),
                        deviceModel = Build.MODEL.orEmpty()
                    )
                )
                throw throwable
            }
        } catch (throwable: Throwable) {
            if (currentConfiguredModelPath() != null) {
                throw throwable
            }
            modelLoaded = false
            _runtimeState.value = unconfiguredState(
                detail = buildDownloadFailureRetryDetail(throwable)
            )
            throw throwable
        }
    }

    override suspend fun loadConfiguredModel() = operationMutex.withLock {
        loadConfiguredModelLocked()
    }

    override suspend fun selectStoredModel(path: String) = operationMutex.withLock {
        val models = installedModels()
        val selectedModel = models.firstOrNull { it.path == path }
            ?: throw IllegalArgumentException("Selected model is no longer available.")

        if (currentConfiguredModelPath() != selectedModel.path) {
            unloadIfNeeded()
            persistConfiguredModel(selectedModel)
        }
        _runtimeState.value = configuredState(
            currentModel = selectedModel,
            installedModels = models,
            detail = "${selectedModel.name} selected. Loading into memory."
        )
        loadConfiguredModelLocked()
    }

    override suspend fun unloadModel() = operationMutex.withLock {
        unloadIfNeeded()
        val currentModel = currentConfiguredModel()
        val models = installedModels()
        _runtimeState.value = if (currentModel == null) {
            unconfiguredState(installedModels = models)
        } else {
            configuredState(currentModel = currentModel, installedModels = models)
        }
    }

    override suspend fun removeConfiguredModel() = operationMutex.withLock {
        val existingModel = currentConfiguredModel()
        val modelsBeforeRemoval = installedModels()
        unloadIfNeeded()
        val remainingModels = modelsBeforeRemoval.filterNot { it.path == existingModel?.path }
        persistInstalledModels(remainingModels)

        existingModel?.path?.let { modelPath ->
            val modelFile = File(modelPath)
            if (modelFile.exists() && modelFile.parentFile == File(appContext.filesDir, MODEL_DIRECTORY_NAME)) {
                modelFile.delete()
            }
        }

        val replacementModel = remainingModels.firstOrNull()
        if (replacementModel == null) {
            clearConfiguredModel()
        } else {
            persistConfiguredModel(replacementModel)
        }

        _runtimeState.value = if (replacementModel == null) {
            unconfiguredState(
                installedModels = emptyList(),
                detail = if (existingModel?.preset != null) {
                    "${existingModel.preset.shortLabel} removed from the device."
                } else {
                    "Custom model removed from the device."
                }
            )
        } else {
            configuredState(
                currentModel = replacementModel,
                installedModels = remainingModels,
                detail = if (existingModel?.preset != null) {
                    "${existingModel.preset.shortLabel} removed. ${replacementModel.name} is stored on-device."
                } else {
                    "Custom model removed. ${replacementModel.name} is stored on-device."
                }
            )
        }
    }

    override fun destroy() {
        runtimeScope.cancel()
        runCatching {
            inferenceEngine?.destroy()
        }
        inferenceEngine = null
        modelLoaded = false
        _runtimeState.value = initialRuntimeState()
    }

    private suspend fun loadConfiguredModelLocked() {
        val installedModels = installedModels()
        val currentModel = currentConfiguredModel()
            ?: throw IllegalStateException("Import or download a model from Settings first.")
        val modelFile = resolveConfiguredModelFile()
            ?: throw IllegalStateException("Import or download a model from Settings first.")
        if (modelLoaded) {
            _runtimeState.value = readyState(currentModel = currentModel, installedModels = installedModels)
            return
        }

        _runtimeState.value = AiRuntimeState(
            phase = AiRuntimePhase.Loading,
            preset = currentModel.preset,
            currentModelPath = currentModel.path,
            modelName = currentModel.name,
            installedModels = installedModels,
            detail = "Loading ${currentModel.name} into on-device memory."
        )

        try {
            val engine = getOrCreateInferenceEngine()
            awaitEngineInitialization(engine)
            when (engine.state.value) {
                is InferenceEngine.State.Error -> engine.cleanUp()
                is InferenceEngine.State.ModelReady -> {
                    if (!modelLoaded) {
                        engine.cleanUp()
                    }
                }
                else -> Unit
            }
            engine.loadModel(modelFile.absolutePath)
            modelLoaded = true
            _runtimeState.value = readyState(currentModel = currentModel, installedModels = installedModels)
        } catch (throwable: Throwable) {
            modelLoaded = false
            _runtimeState.value = errorState(
                currentModel = currentModel,
                installedModels = installedModels,
                detail = resolveStoredModelLoadFailureDetail(
                    throwable = throwable,
                    supportedAbis = Build.SUPPORTED_ABIS.toList(),
                    fingerprint = Build.FINGERPRINT.orEmpty(),
                    deviceModel = Build.MODEL.orEmpty()
                )
            )
            throw throwable
        }
    }

    private suspend fun ensureModelLoadedLocked() {
        if (!modelLoaded) {
            loadConfiguredModelLocked()
        }
    }

    private suspend fun unloadIfNeeded() {
        val engine = inferenceEngine ?: return
        if (!modelLoaded) {
            return
        }

        val currentModel = currentConfiguredModel()
        val installedModels = installedModels()

        _runtimeState.value = AiRuntimeState(
            phase = AiRuntimePhase.Unloading,
            preset = currentModel?.preset,
            currentModelPath = currentModel?.path,
            modelName = currentModel?.name,
            installedModels = installedModels,
            detail = "Unloading model from memory."
        )
        engine.cleanUp()
        modelLoaded = false
    }

    private suspend fun getOrCreateInferenceEngine(): InferenceEngine {
        val existing = inferenceEngine
        if (existing != null) {
            return existing
        }

        return withContext(ioDispatcher) {
            inferenceEngineFactory(appContext)
        }.also { created ->
            inferenceEngine = created
        }
    }

    private suspend fun awaitEngineInitialization(engine: InferenceEngine) {
        val state = engine.state.first { currentState ->
            currentState !is InferenceEngine.State.Uninitialized &&
                currentState !is InferenceEngine.State.Initializing
        }
        if (state is InferenceEngine.State.Error) {
            throw state.exception
        }
    }

    private fun resolveConfiguredModelFile(): File? {
        val modelPath = reconcileStoredModels().second?.path ?: return null
        val modelFile = File(modelPath)
        if (!modelFile.exists() || !modelFile.isFile) {
            return null
        }
        return modelFile
    }

    private fun initialRuntimeState(): AiRuntimeState {
        val (installedModels, currentModel) = reconcileStoredModels()
        return if (currentModel == null) {
            unconfiguredState(installedModels = installedModels)
        } else {
            configuredState(
                currentModel = currentModel,
                installedModels = installedModels,
                detail = "Model is stored on-device and ready to load."
            )
        }
    }

    private fun readyState(currentModel: AiStoredModel?, installedModels: List<AiStoredModel>): AiRuntimeState {
        return AiRuntimeState(
            phase = AiRuntimePhase.Ready,
            preset = currentModel?.preset,
            currentModelPath = currentModel?.path,
            modelName = currentModel?.name,
            installedModels = installedModels,
            detail = "Model is loaded and ready."
        )
    }

    private fun configuredState(
        currentModel: AiStoredModel?,
        installedModels: List<AiStoredModel>,
        detail: String = "Model is stored on-device."
    ): AiRuntimeState {
        return AiRuntimeState(
            phase = AiRuntimePhase.Configured,
            preset = currentModel?.preset,
            currentModelPath = currentModel?.path,
            modelName = currentModel?.name,
            installedModels = installedModels,
            detail = detail
        )
    }

    private fun errorState(
        currentModel: AiStoredModel?,
        installedModels: List<AiStoredModel>,
        detail: String
    ): AiRuntimeState {
        return AiRuntimeState(
            phase = AiRuntimePhase.Error,
            preset = currentModel?.preset,
            currentModelPath = currentModel?.path,
            modelName = currentModel?.name,
            installedModels = installedModels,
            detail = detail
        )
    }

    private fun unconfiguredState(
        installedModels: List<AiStoredModel> = installedModels(),
        detail: String = "Import or download a model from Settings to enable Ask AI."
    ): AiRuntimeState {
        return AiRuntimeState(
            phase = AiRuntimePhase.Unconfigured,
            installedModels = installedModels,
            detail = detail
        )
    }

    private fun persistConfiguredModel(model: AiStoredModel) {
        sharedPreferences.edit()
            .putString(KEY_MODEL_PATH, model.path)
            .putString(KEY_MODEL_NAME, model.name)
            .putString(KEY_MODEL_PRESET, model.preset?.storageKey)
            .apply()
    }

    private fun clearConfiguredModel() {
        sharedPreferences.edit()
            .remove(KEY_MODEL_PATH)
            .remove(KEY_MODEL_NAME)
            .remove(KEY_MODEL_PRESET)
            .apply()
    }

    private fun currentConfiguredModelPath(): String? {
        return sharedPreferences.getString(KEY_MODEL_PATH, null)
    }

    private fun currentConfiguredModel(): AiStoredModel? {
        val (installedModels, currentModel) = reconcileStoredModels()
        if (currentModel != null) {
            return currentModel
        }
        val legacyPath = currentConfiguredModelPath() ?: return null
        val legacyName = sharedPreferences.getString(KEY_MODEL_NAME, null) ?: File(legacyPath).name
        val legacyPreset = AiModelPreset.fromStorageKey(sharedPreferences.getString(KEY_MODEL_PRESET, null))
        return installedModels.firstOrNull { it.path == legacyPath }
            ?: AiStoredModel(path = legacyPath, name = legacyName, preset = legacyPreset)
    }

    private fun installedModels(): List<AiStoredModel> {
        val migratedModels = readInstalledModels().ifEmpty {
            val legacyPath = currentConfiguredModelPath()
            if (legacyPath == null) {
                emptyList()
            } else {
                listOf(
                    AiStoredModel(
                        path = legacyPath,
                        name = sharedPreferences.getString(KEY_MODEL_NAME, null) ?: File(legacyPath).name,
                        preset = AiModelPreset.fromStorageKey(sharedPreferences.getString(KEY_MODEL_PRESET, null))
                    )
                ).also(::persistInstalledModels)
            }
        }
        return reconcileStoredModelOrder(migratedModels)
    }

    private fun readInstalledModels(): List<AiStoredModel> {
        val raw = sharedPreferences.getString(KEY_INSTALLED_MODELS, null)
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val path = item.optString("path").trim()
                if (path.isBlank()) continue
                val name = item.optString("name").trim().ifBlank { File(path).name }
                add(
                    AiStoredModel(
                        path = path,
                        name = name,
                        preset = AiModelPreset.fromStorageKey(item.optString("preset").trim().ifBlank { null })
                    )
                )
            }
        }
    }

    private fun persistInstalledModels(models: List<AiStoredModel>) {
        sharedPreferences.edit()
            .putString(
                KEY_INSTALLED_MODELS,
                JSONArray().apply {
                    models.forEach { model ->
                        put(
                            JSONObject()
                                .put("path", model.path)
                                .put("name", model.name)
                                .put("preset", model.preset?.storageKey ?: "")
                        )
                    }
                }.toString()
            )
            .apply()
    }

    private fun upsertInstalledModel(model: AiStoredModel): List<AiStoredModel> {
        val updatedModels = reconcileStoredModelOrder(
            listOf(model) + installedModels().filterNot { existing -> existing.path == model.path }
        )
        persistInstalledModels(updatedModels)
        return updatedModels
    }

    private fun reconcileStoredModels(): Pair<List<AiStoredModel>, AiStoredModel?> {
        val existingModels = reconcileStoredModelOrder(
            installedModels().filter { storedModel ->
                val file = File(storedModel.path)
                file.exists() && file.isFile
            }
        )
        if (existingModels != readInstalledModels()) {
            persistInstalledModels(existingModels)
        }

        val selectedPath = currentConfiguredModelPath()
        val currentModel = existingModels.firstOrNull { it.path == selectedPath } ?: existingModels.firstOrNull()
        if (currentModel == null) {
            clearConfiguredModel()
        } else if (currentModel.path != selectedPath) {
            persistConfiguredModel(currentModel)
        }
        return existingModels to currentModel
    }

    private fun reconcileStoredModelOrder(models: List<AiStoredModel>): List<AiStoredModel> {
        return models
            .filter { it.path.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { it.path }
    }

    private fun copyModelToAppStorage(uri: Uri, displayName: String): File {
        val modelDirectory = File(appContext.filesDir, MODEL_DIRECTORY_NAME).apply {
            mkdirs()
        }
        val destinationFile = File(modelDirectory, sanitizeFileName(displayName))
        appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
            destinationFile.outputStream().buffered().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: error("Unable to read the selected model file.")
        return destinationFile
    }

    private fun downloadPresetIntoAppStorage(preset: AiModelPreset): File {
        val modelDirectory = File(appContext.filesDir, MODEL_DIRECTORY_NAME).apply {
            mkdirs()
        }
        val destinationFile = File(modelDirectory, preset.filename)
        val tempFile = File(modelDirectory, "${preset.filename}.part")
        val connection = openDownloadConnection(preset)

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Download failed with HTTP $responseCode.")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
            connection.inputStream.buffered().use { input ->
                tempFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    var downloadedBytes = 0L

                    while (true) {
                        val readCount = input.read(buffer)
                        if (readCount < 0) {
                            break
                        }
                        if (readCount == 0) {
                            continue
                        }

                        output.write(buffer, 0, readCount)
                        downloadedBytes += readCount
                        val currentModel = currentConfiguredModel()
                        _runtimeState.value = AiRuntimeState(
                            phase = AiRuntimePhase.Downloading,
                            preset = preset,
                            currentModelPath = currentModel?.path,
                            modelName = preset.filename,
                            installedModels = installedModels(),
                            detail = formatDownloadProgress(
                                preset = preset,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes
                            ),
                            progress = totalBytes?.let {
                                (downloadedBytes.toDouble() / it.toDouble()).toFloat()
                            }
                        )
                    }
                }
            }

            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            check(tempFile.renameTo(destinationFile)) {
                "Failed to finalize the downloaded model file."
            }
            destinationFile
        } finally {
            connection.disconnect()
            if (tempFile.exists() && !destinationFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun openDownloadConnection(preset: AiModelPreset): HttpURLConnection {
        return (URL(preset.downloadUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = DOWNLOAD_CONNECT_TIMEOUT_MILLIS
            readTimeout = DOWNLOAD_READ_TIMEOUT_MILLIS
            requestMethod = "GET"
            doInput = true
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "StandaloneCodePractice/1.0")
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.lastPathSegment?.substringAfterLast('/')
        }

        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    private fun sanitizeFileName(fileName: String): String {
        val cleaned = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (cleaned.endsWith(MODEL_EXTENSION, ignoreCase = true)) {
            cleaned
        } else {
            "$cleaned$MODEL_EXTENSION"
        }
    }

    private fun formatDownloadProgress(
        preset: AiModelPreset,
        downloadedBytes: Long,
        totalBytes: Long?
    ): String {
        val downloadedMb = downloadedBytes / (1024f * 1024f)
        return if (totalBytes != null && totalBytes > 0L) {
            val totalGb = totalBytes / (1024f * 1024f * 1024f)
            val percent = ((downloadedBytes.toDouble() / totalBytes.toDouble()) * 100.0).toInt()
            String.format(
                Locale.US,
                "Downloading %s: %.0f MB / %.2f GB (%d%%).",
                preset.shortLabel,
                downloadedMb,
                totalGb,
                percent
            )
        } else {
            String.format(
                Locale.US,
                "Downloading %s: %.0f MB.",
                preset.shortLabel,
                downloadedMb
            )
        }
    }

    private companion object {
        const val KEY_MODEL_PATH = "ai_model_path"
        const val KEY_MODEL_NAME = "ai_model_name"
        const val KEY_MODEL_PRESET = "ai_model_preset"
        const val KEY_INSTALLED_MODELS = "ai_installed_models"
        const val MODEL_DIRECTORY_NAME = "ai-models"
        const val MODEL_EXTENSION = ".gguf"
        const val DEFAULT_MODEL_FILENAME = "model.gguf"
        const val DEFAULT_PREDICT_LENGTH = 768
        const val DOWNLOAD_BUFFER_SIZE = 1024 * 64
        const val DOWNLOAD_CONNECT_TIMEOUT_MILLIS = 15_000
        const val DOWNLOAD_READ_TIMEOUT_MILLIS = 300_000
    }
}

internal fun resolveDownloadFailureDetail(throwable: Throwable): String {
    val causeChain = generateSequence(throwable) { it.cause }.toList()
    if (causeChain.any { it is UnknownHostException }) {
        return "This device cannot resolve huggingface.co right now. Check the emulator or device internet connection and DNS, then retry."
    }

    return throwable.message ?: "Model download failed."
}

internal fun buildDownloadFailureRetryDetail(throwable: Throwable): String {
    return "${resolveDownloadFailureDetail(throwable)} Select a model to try again."
}

internal fun resolveStoredModelLoadFailureDetail(
    throwable: Throwable,
    supportedAbis: List<String>,
    fingerprint: String,
    deviceModel: String
): String {
    val reason = resolveModelLoadFailureReason(
        throwable = throwable,
        supportedAbis = supportedAbis,
        fingerprint = fingerprint,
        deviceModel = deviceModel
    )
    return reason?.replaceFirstChar { firstChar ->
        if (firstChar.isLowerCase()) {
            firstChar.titlecase(Locale.US)
        } else {
            firstChar.toString()
        }
    } ?: (throwable.message ?: "Model load failed.")
}

internal fun resolvePostDownloadLoadFailureDetail(
    throwable: Throwable,
    supportedAbis: List<String>,
    fingerprint: String,
    deviceModel: String
): String {
    val reason = resolveModelLoadFailureReason(
        throwable = throwable,
        supportedAbis = supportedAbis,
        fingerprint = fingerprint,
        deviceModel = deviceModel
    )
    if (reason != null) {
        return "The model downloaded successfully, but $reason"
    }

    val message = throwable.message
        ?.takeIf { it.isNotBlank() }
        ?: "loading it into memory failed."
    return "The model downloaded successfully, but $message"
}

private fun resolveModelLoadFailureReason(
    throwable: Throwable,
    supportedAbis: List<String>,
    fingerprint: String,
    deviceModel: String
): String? {
    val causeChain = generateSequence(throwable) { it.cause }.toList()
    if (causeChain.any { it is UnsupportedArchitectureException }) {
        return if (isProbablyX86Emulator(supportedAbis, fingerprint, deviceModel)) {
            "this x86_64 emulator could not load the llama.cpp backend. Try a physical arm64 Android device or an arm64 emulator image."
        } else {
            "this device could not load the llama.cpp backend for the selected model."
        }
    }

    return null
}

internal fun isProbablyX86Emulator(
    supportedAbis: List<String>,
    fingerprint: String,
    deviceModel: String
): Boolean {
    val hasX86Abi = supportedAbis.any { abi ->
        abi.contains("x86", ignoreCase = true)
    }
    val fingerprintLower = fingerprint.lowercase(Locale.US)
    val modelLower = deviceModel.lowercase(Locale.US)
    val looksLikeEmulator = listOf(
        "generic",
        "emulator",
        "sdk_gphone",
        "sdk_gtablet",
        "ranchu"
    ).any { marker ->
        fingerprintLower.contains(marker) || modelLower.contains(marker)
    }
    return hasX86Abi && looksLikeEmulator
}
