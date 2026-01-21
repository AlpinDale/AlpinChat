package com.alpin.chat.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alpin.chat.data.local.entity.ConversationEntity
import com.alpin.chat.data.local.entity.MessageAlternativesConverter
import com.alpin.chat.data.local.entity.MessageEntity
import com.alpin.chat.data.local.entity.ModelConfigEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ModelConfigEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(MessageAlternativesConverter::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun modelConfigDao(): ModelConfigDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add supportsThinking column to model_configs table
                db.execSQL("ALTER TABLE model_configs ADD COLUMN supportsThinking INTEGER NOT NULL DEFAULT 0")
                // Add thinkingContent column to messages table
                db.execSQL("ALTER TABLE messages ADD COLUMN thinkingContent TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add alternatives (JSON string) and currentAlternativeIndex columns to messages table
                db.execSQL("ALTER TABLE messages ADD COLUMN alternatives TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN currentAlternativeIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add contextLength column to model_configs table (default 32768)
                db.execSQL("ALTER TABLE model_configs ADD COLUMN contextLength INTEGER NOT NULL DEFAULT 32768")
            }
        }
    }
}
