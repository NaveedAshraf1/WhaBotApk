package com.example.whabotpro.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.whabotpro.data.model.*

@Database(
    entities = [
        BusinessInfo::class,
        KbItem::class,
        Category::class,
        Order::class,
        InboxMessage::class,
        Contact::class,
        Rule::class,
        LogEntry::class,
        AppSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessInfoDao(): BusinessInfoDao
    abstract fun kbItemDao(): KbItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun orderDao(): OrderDao
    abstract fun inboxDao(): InboxDao
    abstract fun contactDao(): ContactDao
    abstract fun ruleDao(): RuleDao
    abstract fun logDao(): LogDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whabotpro.db"
                ).fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
