package dev.kaixinguo.codeoffthegrid.ui.workspace

import androidx.compose.runtime.mutableStateListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class SeedCatalogSynchronizationTest {
    @Test
    fun synchronizeSeedCatalog_preservesExistingProblemOrderAndCustomSets() {
        val seedFolders = listOf(
            ProblemFolderState(
                id = "folder-neetcode-150",
                title = "NeetCode 150",
                sets = mutableStateListOf(
                    ProblemSetState(
                        id = "set-arrays",
                        title = "Arrays",
                        problems = mutableStateListOf(
                            problem(id = "p1", title = "One"),
                            problem(id = "p2", title = "Two"),
                            problem(id = "p3", title = "Three")
                        )
                    ),
                    ProblemSetState(
                        id = "set-two-pointers",
                        title = "Two Pointers",
                        problems = mutableStateListOf(
                            problem(id = "p4", title = "Four")
                        )
                    )
                )
            )
        )

        val loadedFolders = listOf(
            ProblemFolderState(
                id = "folder-neetcode-150",
                title = "NeetCode 150",
                sets = mutableStateListOf(
                    ProblemSetState(
                        id = "set-arrays",
                        title = "Arrays",
                        problems = mutableStateListOf(
                            problem(id = "p2", title = "Two"),
                            problem(id = "p1", title = "One")
                        )
                    ),
                    ProblemSetState(
                        id = "set-custom-review",
                        title = "Custom Review",
                        problems = mutableStateListOf(
                            problem(id = "p3", title = "Three")
                        )
                    )
                )
            )
        )

        val synchronized = synchronizeSeedCatalog(
            loadedFolders = loadedFolders,
            seedFolders = seedFolders
        )

        val folder = synchronized.single()
        assertEquals(
            listOf("set-arrays", "set-custom-review", "set-two-pointers"),
            folder.sets.map { it.id }
        )
        assertEquals(listOf("p2", "p1"), folder.sets[0].problems.map { it.id })
        assertEquals(listOf("p3"), folder.sets[1].problems.map { it.id })
        assertEquals(listOf("p4"), folder.sets[2].problems.map { it.id })
    }

    private fun problem(id: String, title: String): ProblemListItem {
        return ProblemListItem(
            id = id,
            title = title
        )
    }
}
