package dev.kaixinguo.standalonecodepractice.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkspaceDocumentEntity::class,
        ProblemFolderEntity::class,
        ProblemSetEntity::class,
        StoredProblemEntity::class
    ],
    version = 5,
    exportSchema = false
)
internal abstract class OffTheCodeGridDatabase : RoomDatabase() {
    abstract fun workspaceDocumentDao(): WorkspaceDocumentDao
    abstract fun problemCatalogDao(): ProblemCatalogDao

    companion object {
        @Volatile
        private var instance: OffTheCodeGridDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `problem_folders` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `sortIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `problem_sets` (
                        `id` TEXT NOT NULL,
                        `folderId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `sortIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `problems` (
                        `id` TEXT NOT NULL,
                        `setId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `active` INTEGER NOT NULL,
                        `solved` INTEGER NOT NULL,
                        `summary` TEXT NOT NULL,
                        `exampleInput` TEXT NOT NULL,
                        `exampleOutput` TEXT NOT NULL,
                        `starterCode` TEXT NOT NULL,
                        `customTests` TEXT NOT NULL,
                        `hintsJson` TEXT NOT NULL,
                        `executionPipeline` TEXT NOT NULL DEFAULT 'single_method',
                        `sortIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE `problems`
                    ADD COLUMN `statementMarkdown` TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE `problems`
                    ADD COLUMN `executionPipeline` TEXT NOT NULL DEFAULT 'single_method'
                    """.trimIndent()
                )
            }
        }

        private val migration4To5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE `problems`
                    ADD COLUMN `submissionTestSuiteJson` TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): OffTheCodeGridDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OffTheCodeGridDatabase::class.java,
                    "standalone-code-practice.db"
                )
                    .addMigrations(migration1To2, migration2To3, migration3To4, migration4To5)
                    .build()
                    .also { database ->
                    instance = database
                }
            }
        }
    }
}
