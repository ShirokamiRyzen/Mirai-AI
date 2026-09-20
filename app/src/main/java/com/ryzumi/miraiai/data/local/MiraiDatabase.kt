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
    version = 9,
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

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `characters` ADD COLUMN `live2dPath` TEXT")
                db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `isLive2dMode` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `characters` ADD COLUMN `voiceId` TEXT NOT NULL DEFAULT 'af_heart'")
                db.execSQL("ALTER TABLE `characters` ADD COLUMN `voicePitch` REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE `characters` ADD COLUMN `voiceSpeed` REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `isVoiceMode` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `ttsEngine` TEXT NOT NULL DEFAULT 'local'")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `ttsLocalModel` TEXT NOT NULL DEFAULT 'kokoro-82m'")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `ttsApiEndpoint` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `ttsApiKey` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `ttsApiModel` TEXT NOT NULL DEFAULT 'kokoro'")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `imageGenEngine` TEXT NOT NULL DEFAULT 'local'")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `imageGenApiEndpoint` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `imageGenApiKey` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `imageGenApiModel` TEXT NOT NULL DEFAULT 'dall-e-3'")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `imageGenSize` TEXT NOT NULL DEFAULT '1024x1024'")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `imageGenSteps` INTEGER NOT NULL DEFAULT 20")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `imageGenGuidanceScale` REAL NOT NULL DEFAULT 7.5")
                db.execSQL("ALTER TABLE `inference_configs` ADD COLUMN `imageGenNegativePrompt` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): MiraiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MiraiDatabase::class.java,
                    "mirai_ai_database"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                        MIGRATION_7_8, MIGRATION_8_9
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
