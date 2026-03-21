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
        if (_runtimeState.value.phase == AiRuntimePhase.Configured) {
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

        val modelName = currentConfiguredModelName()
        val preset = currentConfiguredModelPreset()
        _runtimeState.value = AiRuntimeState(
            phase = AiRuntimePhase.Generating,
            preset = preset,
            modelName = modelName,
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
            _runtimeState.value = readyState(modelName = modelName, preset = preset)
            content
        } catch (throwable: Throwable) {
            modelLoaded = false
            _runtimeState.value = errorState(
                modelName = modelName,
                preset = preset,
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
            val previousModelPath = currentConfiguredModelPath()

            unloadIfNeeded()
            persistConfiguredModel(
                path = copiedModel.absolutePath,
                name = copiedModel.name,
                preset = null
            )
            deleteManagedModelIfReplaced(
                previousModelPath = previousModelPath,
                newModelPath = copiedModel.absolutePath
            )
            _runtimeState.value = configuredState(
                modelName = copiedModel.name,
                preset = null,
                detail = "Model imported. Loading into memory."
            )
            loadConfiguredModelLocked()
        } catch (throwable: Throwable) {
            modelLoaded = false
            _runtimeState.value = errorState(
                modelName = displayName,
                preset = null,
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
            val previousModelPath = currentConfiguredModelPath()

            unloadIfNeeded()
            persistConfiguredModel(
                path = downloadedModel.absolutePath,
                name = downloadedModel.name,
                preset = preset
            )
            deleteManagedModelIfReplaced(
                previousModelPath = previousModelPath,
                newModelPath = downloadedModel.absolutePath
            )
            _runtimeState.value = configuredState(
                modelName = downloadedModel.name,
                preset = preset,
                detail = "${preset.shortLabel} downloaded. Loading into memory."
            )
            try {
                loadConfiguredModelLocked()
            } catch (throwable: Throwable) {
                modelLoaded = false
                _runtimeState.value = errorState(
                    modelName = downloadedModel.name,
                    preset = preset,
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
                detail = "${resolveDownloadFailureDetail(throwable)} Try again to select a model."
            )
            throw throwable
        }
    }

    override suspend fun loadConfiguredModel() = operationMutex.withLock {
        loadConfiguredModelLocked()
    }

    override suspend fun unloadModel() = operationMutex.withLock {
        unloadIfNeeded()
        val modelName = currentConfiguredModelName()
        val preset = currentConfiguredModelPreset()
        _runtimeState.value = if (modelName == null) {
            unconfiguredState()
        } else {
            configuredState(modelName = modelName, preset = preset)
        }
    }

    override suspend fun removeConfiguredModel() = operationMutex.withLock {
        val existingModelPath = currentConfiguredModelPath()
        val existingPreset = currentConfiguredModelPreset()
        unloadIfNeeded()
        clearConfiguredModel()

        existingModelPath?.let { modelPath ->
            val modelFile = File(modelPath)
            if (modelFile.exists() && modelFile.parentFile == File(appContext.filesDir, MODEL_DIRECTORY_NAME)) {
                modelFile.delete()
            }
        }

        _runtimeState.value = AiRuntimeState(
            phase = AiRuntimePhase.Unconfigured,
            preset = null,
            detail = if (existingPreset != null) {
                "${existingPreset.shortLabel} removed from the device."
            } else {
                "Model removed from the device."
            }
        )
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
        val modelFile = resolveConfiguredModelFile()
            ?: throw IllegalStateException("Download a model from Settings first.")
        val modelName = modelFile.name
        val preset = currentConfiguredModelPreset()
        if (modelLoaded) {
            _runtimeState.value = readyState(modelName = modelName, preset = preset)
            return
        }

        _runtimeState.value = AiRuntimeState(
            phase = AiRuntimePhase.Loading,
            preset = preset,
            modelName = modelName,
            detail = "Loading $modelName into on-device memory."
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
            _runtimeState.value = readyState(modelName = modelName, preset = preset)
        } catch (throwable: Throwable) {
            modelLoaded = false
            _runtimeState.value = errorState(
                modelName = modelName,
                preset = preset,
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

        _runtimeState.value = AiRuntimeState(
            phase = AiRuntimePhase.Unloading,
            preset = currentConfiguredModelPreset(),
            modelName = currentConfiguredModelName(),
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
        val modelPath = currentConfiguredModelPath() ?: return null
        val modelFile = File(modelPath)
        if (!modelFile.exists() || !modelFile.isFile) {
            clearConfiguredModel()
            return null
        }
        return modelFile
    }

    private fun initialRuntimeState(): AiRuntimeState {
        val modelFile = resolveConfiguredModelFile()
        val preset = currentConfiguredModelPreset()
        return if (modelFile == null) {
            unconfiguredState()
        } else {
            configuredState(
                modelName = modelFile.name,
                preset = preset,
                detail = "Model is stored on-device and ready to load."
            )
        }
    }

    private fun readyState(modelName: String?, preset: AiModelPreset?): AiRuntimeState {
        return AiRuntimeState(
            phase = AiRuntimePhase.Ready,
            preset = preset,
            modelName = modelName,
            detail = "Model is loaded and ready."
        )
    }

    private fun configuredState(
        modelName: String?,
        preset: AiModelPreset?,
        detail: String = "Model is stored on-device."
    ): AiRuntimeState {
        return AiRuntimeState(
            phase = AiRuntimePhase.Configured,
            preset = preset,
            modelName = modelName,
            detail = detail
        )
    }

    private fun errorState(
        modelName: String?,
        preset: AiModelPreset?,
        detail: String
    ): AiRuntimeState {
        return AiRuntimeState(
            phase = AiRuntimePhase.Error,
            preset = preset,
            modelName = modelName,
            detail = detail
        )
    }

    private fun unconfiguredState(
        detail: String = "Download a model from Settings to enable Ask AI."
    ): AiRuntimeState {
        return AiRuntimeState(
            phase = AiRuntimePhase.Unconfigured,
            detail = detail
        )
    }

    private fun persistConfiguredModel(path: String, name: String, preset: AiModelPreset?) {
        sharedPreferences.edit()
            .putString(KEY_MODEL_PATH, path)
            .putString(KEY_MODEL_NAME, name)
            .putString(KEY_MODEL_PRESET, preset?.storageKey)
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

    private fun currentConfiguredModelName(): String? {
        return sharedPreferences.getString(KEY_MODEL_NAME, null)
    }

    private fun currentConfiguredModelPreset(): AiModelPreset? {
        return AiModelPreset.fromStorageKey(sharedPreferences.getString(KEY_MODEL_PRESET, null))
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
                        _runtimeState.value = AiRuntimeState(
                            phase = AiRuntimePhase.Downloading,
                            preset = preset,
                            modelName = preset.filename,
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

    private fun deleteManagedModelIfReplaced(previousModelPath: String?, newModelPath: String) {
        if (previousModelPath == null || previousModelPath == newModelPath) {
            return
        }
        val previousFile = File(previousModelPath)
        val modelDirectory = File(appContext.filesDir, MODEL_DIRECTORY_NAME)
        if (previousFile.exists() && previousFile.parentFile == modelDirectory) {
            previousFile.delete()
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
