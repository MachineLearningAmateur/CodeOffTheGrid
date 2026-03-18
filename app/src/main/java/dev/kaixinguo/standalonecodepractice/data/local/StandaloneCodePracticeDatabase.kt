package dev.kaixinguo.standalonecodepractice.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkspaceDocumentEntity::class],
    version = 1,
    exportSchema = false
)
internal abstract class StandaloneCodePracticeDatabase : RoomDatabase() {
    abstract fun workspaceDocumentDao(): WorkspaceDocumentDao

    companion object {
        @Volatile
        private var instance: StandaloneCodePracticeDatabase? = null

        fun getInstance(context: Context): StandaloneCodePracticeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StandaloneCodePracticeDatabase::class.java,
                    "standalone-code-practice.db"
                ).build().also { database ->
                    instance = database
                }
            }
        }
    }
}
