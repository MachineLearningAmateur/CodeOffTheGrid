package dev.kaixinguo.standalonecodepractice.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface WorkspaceDocumentDao {
    @Query("SELECT * FROM workspace_documents WHERE problemId = :problemId")
    suspend fun getByProblemId(problemId: String): WorkspaceDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: WorkspaceDocumentEntity)
}
