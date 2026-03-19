package dev.kaixinguo.standalonecodepractice.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
internal interface ProblemCatalogDao {
    @Query("SELECT * FROM problem_folders ORDER BY sortIndex ASC")
    suspend fun getFolders(): List<ProblemFolderEntity>

    @Query("SELECT * FROM problem_sets ORDER BY sortIndex ASC")
    suspend fun getSets(): List<ProblemSetEntity>

    @Query("SELECT * FROM problems ORDER BY sortIndex ASC")
    suspend fun getProblems(): List<StoredProblemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<ProblemFolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<ProblemSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblems(problems: List<StoredProblemEntity>)

    @Query("DELETE FROM problems")
    suspend fun clearProblems()

    @Query("DELETE FROM problem_sets")
    suspend fun clearSets()

    @Query("DELETE FROM problem_folders")
    suspend fun clearFolders()

    @Transaction
    suspend fun replaceCatalog(
        folders: List<ProblemFolderEntity>,
        sets: List<ProblemSetEntity>,
        problems: List<StoredProblemEntity>
    ) {
        clearProblems()
        clearSets()
        clearFolders()
        if (folders.isNotEmpty()) insertFolders(folders)
        if (sets.isNotEmpty()) insertSets(sets)
        if (problems.isNotEmpty()) insertProblems(problems)
    }
}
