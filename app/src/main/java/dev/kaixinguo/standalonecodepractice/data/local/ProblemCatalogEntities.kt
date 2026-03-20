package dev.kaixinguo.standalonecodepractice.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "problem_folders")
internal data class ProblemFolderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sortIndex: Int
)

@Entity(tableName = "problem_sets")
internal data class ProblemSetEntity(
    @PrimaryKey val id: String,
    val folderId: String,
    val title: String,
    val sortIndex: Int
)

@Entity(tableName = "problems")
internal data class StoredProblemEntity(
    @PrimaryKey val id: String,
    val setId: String,
    val title: String,
    val difficulty: String,
    val active: Boolean,
    val solved: Boolean,
    val summary: String,
    val statementMarkdown: String,
    val exampleInput: String,
    val exampleOutput: String,
    val starterCode: String,
    val customTests: String,
    val hintsJson: String,
    val submissionTestSuiteJson: String,
    val executionPipeline: String,
    val sortIndex: Int
)
