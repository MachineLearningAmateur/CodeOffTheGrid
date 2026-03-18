package dev.kaixinguo.standalonecodepractice.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_documents")
internal data class WorkspaceDocumentEntity(
    @PrimaryKey val problemId: String,
    val draftCode: String,
    val customTests: String,
    val sketchesJson: String,
    val updatedAtEpochMillis: Long
)
