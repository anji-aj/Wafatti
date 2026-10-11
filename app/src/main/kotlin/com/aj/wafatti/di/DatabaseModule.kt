package com.aj.wafatti.di

import android.content.Context
import androidx.room.Room
import com.aj.wafatti.data.local.database.WafattiDatabase
import com.aj.wafatti.data.local.database.dao.ConversationDao
import com.aj.wafatti.data.local.database.dao.MessageDao
import com.aj.wafatti.data.local.database.dao.UserDao
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
    fun provideDatabase(@ApplicationContext context: Context): WafattiDatabase {
        return Room.databaseBuilder(
            context,
            WafattiDatabase::class.java,
            "wafatti_database"
        ).addMigrations(WafattiDatabase.MIGRATION_1_2)
         .fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    fun provideUserDao(database: WafattiDatabase): UserDao = database.userDao()

    @Provides
    fun provideConversationDao(database: WafattiDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideMessageDao(database: WafattiDatabase): MessageDao = database.messageDao()
}
