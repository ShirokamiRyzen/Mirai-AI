package com.ryzumi.miraiai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ryzumi.miraiai.data.local.dao.CharacterDao
import com.ryzumi.miraiai.data.local.dao.ChatMessageDao
import com.ryzumi.miraiai.data.local.dao.ChatSessionDao
import com.ryzumi.miraiai.data.local.dao.InferenceConfigDao
import com.ryzumi.miraiai.data.local.dao.UserPersonaDao
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import com.ryzumi.miraiai.data.local.entity.ChatMessageEntity
import com.ryzumi.miraiai.data.local.entity.ChatSessionEntity
import com.ryzumi.miraiai.data.local.entity.InferenceConfigEntity
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity

@Database(
    entities = [
        CharacterEntity::class,
        UserPersonaEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        InferenceConfigEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MiraiDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao
    abstract fun userPersonaDao(): UserPersonaDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun inferenceConfigDao(): InferenceConfigDao

    companion object {
        @Volatile
        private var INSTANCE: MiraiDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inference_configs` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `baseUrl` TEXT NOT NULL, 
                        `apiKey` TEXT NOT NULL, 
                        `generateModelId` TEXT NOT NULL, 
                        `visionModelId` TEXT NOT NULL, 
                        `temperature` REAL NOT NULL, 
                        `topP` REAL NOT NULL, 
                        `repetitionPenalty` REAL NOT NULL, 
                        `maxTokens` INTEGER NOT NULL, 
                        `customHeaders` TEXT NOT NULL, 
                        `availableModelsJson` TEXT NOT NULL, 
                        `isActive` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `configId` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `useLocalGenModel` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `useLocalVisionModel` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `imageGenModelId` TEXT NOT NULL DEFAULT 'none'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `generationSpeedTps` REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `modelName` TEXT")
            }
        }

        fun getInstance(context: Context): MiraiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MiraiDatabase::class.java,
                    "mirai_ai_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
