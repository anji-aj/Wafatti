package com.aj.wafatti.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aj.wafatti.data.local.database.dao.ConversationDao
import com.aj.wafatti.data.local.database.dao.MessageDao
import com.aj.wafatti.data.local.database.dao.UserDao
import com.aj.wafatti.data.local.database.entity.ConversationEntity
import com.aj.wafatti.data.local.database.entity.MessageEntity
import com.aj.wafatti.data.local.database.entity.UserEntity

@Database(
    entities = [UserEntity::class, ConversationEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WafattiDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `savedName` TEXT DEFAULT NULL")
            }
        }
    }
}
