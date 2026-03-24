package dev.kaixinguo.codeoffthegrid.ui.workspace

import androidx.compose.runtime.mutableStateListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemComposerDestinationTest {
    @Test
    fun buildProblemComposerDestinations_includesCustomEmptySets() {
        val folders = listOf(
            ProblemFolderState(
                title = "Seed Folder",
                sets = mutableStateListOf(
                    ProblemSetState(
                        title = "Seed Set",
                        problems = mutableStateListOf()
                    )
                )
            ),
            ProblemFolderState(
                title = "Custom Folder",
                sets = mutableStateListOf(
                    ProblemSetState(
                        title = "Custom Set",
                        problems = mutableStateListOf()
                    )
                )
            )
        )

        val destinations = buildProblemComposerDestinations(folders)

        assertEquals(2, destinations.size)
        assertTrue(destinations.any { it.label == "Seed Folder / Seed Set" })
        assertTrue(destinations.any { it.label == "Custom Folder / Custom Set" })
    }
}
