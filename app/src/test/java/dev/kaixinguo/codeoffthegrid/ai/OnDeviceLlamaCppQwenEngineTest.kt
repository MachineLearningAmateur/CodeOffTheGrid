package dev.kaixinguo.codeoffthegrid.ai

import com.arm.aichat.UnsupportedArchitectureException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class OnDeviceLlamaCppQwenEngineTest {
    @Test
    fun resolveDownloadFailureDetail_reportsDnsFailuresClearly() {
        val detail = resolveDownloadFailureDetail(
            UnknownHostException("Unable to resolve host huggingface.co")
        )

        assertEquals(
            "This device cannot resolve huggingface.co right now. Check the emulator or device internet connection and DNS, then retry.",
            detail
        )
    }

    @Test
    fun buildDownloadFailureRetryDetail_usesImprovedGrammar() {
        val detail = buildDownloadFailureRetryDetail(
            UnknownHostException("Unable to resolve host huggingface.co")
        )

        assertEquals(
            "This device cannot resolve huggingface.co right now. Check the emulator or device internet connection and DNS, then retry. Select a model to try again.",
            detail
        )
    }

    @Test
    fun resolvePostDownloadLoadFailureDetail_explainsX86EmulatorLimitation() {
        val detail = resolvePostDownloadLoadFailureDetail(
            throwable = UnsupportedArchitectureException(),
            supportedAbis = listOf("x86_64"),
            fingerprint = "google/sdk_gtablet_x86_64/emu64xa:15/AE3A.240806.046.T1/13135149:user/dev-keys",
            deviceModel = "Pixel Tablet"
        )

        assertTrue(detail.contains("The model downloaded successfully"))
        assertTrue(detail.contains("x86_64 emulator"))
        assertTrue(detail.contains("arm64"))
    }

    @Test
    fun resolveStoredModelLoadFailureDetail_explainsArchitectureFailure() {
        val detail = resolveStoredModelLoadFailureDetail(
            throwable = UnsupportedArchitectureException(),
            supportedAbis = listOf("arm64-v8a"),
            fingerprint = "google/panther/panther:15/AP4A.250205.002/1234567:user/release-keys",
            deviceModel = "Pixel 7"
        )

        assertEquals(
            "The on-device llama.cpp backend could not load the selected model.",
            detail
        )
    }

    @Test
    fun resolvePostDownloadLoadFailureDetail_preservesGenericMessages() {
        val detail = resolvePostDownloadLoadFailureDetail(
            throwable = IllegalStateException("Backend initialization timed out."),
            supportedAbis = listOf("arm64-v8a"),
            fingerprint = "google/panther/panther:15/AP4A.250205.002/1234567:user/release-keys",
            deviceModel = "Pixel 7"
        )

        assertEquals(
            "The model downloaded successfully, but Backend initialization timed out.",
            detail
        )
    }

    @Test
    fun isProbablyX86Emulator_requiresEmulatorSignal() {
        assertFalse(
            isProbablyX86Emulator(
                supportedAbis = listOf("x86_64"),
                fingerprint = "google/panther/panther:15/AP4A.250205.002/1234567:user/release-keys",
                deviceModel = "Pixel 7"
            )
        )
    }
}
