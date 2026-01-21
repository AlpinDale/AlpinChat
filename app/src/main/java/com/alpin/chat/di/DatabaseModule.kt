package com.alpin.chat.di

import android.content.Context
import androidx.room.Room
import com.alpin.chat.data.local.db.ChatDatabase
import com.alpin.chat.data.local.db.ConversationDao
import com.alpin.chat.data.local.db.MessageDao
import com.alpin.chat.data.local.db.ModelConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChatDatabase(
        @ApplicationContext context: Context
    ): ChatDatabase = Room.databaseBuilder(
        context,
        ChatDatabase::class.java,
        "chat_database"
    )
        .addMigrations(ChatDatabase.MIGRATION_1_2, ChatDatabase.MIGRATION_2_3, ChatDatabase.MIGRATION_3_4)
        .build()

    @Provides
    @Singleton
    fun provideConversationDao(database: ChatDatabase): ConversationDao =
        database.conversationDao()

    @Provides
    @Singleton
    fun provideMessageDao(database: ChatDatabase): MessageDao =
        database.messageDao()

    @Provides
    @Singleton
    fun provideModelConfigDao(database: ChatDatabase): ModelConfigDao =
        database.modelConfigDao()
}
