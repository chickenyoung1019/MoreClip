package com.chickenyoung.moreclip

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MemoEntity::class], version = 5)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * マイグレーション定義
         * version番号を上げる場合は、ここにMigrationを追加すること
         *
         * 例: version 5 → 6 で「priority」列を追加する場合
         * val MIGRATION_5_6 = object : Migration(5, 6) {
         *     override fun migrate(database: SupportSQLiteDatabase) {
         *         database.execSQL("ALTER TABLE memos ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
         *     }
         * }
         *
         * そして .addMigrations(MIGRATION_5_6) を追加
         */

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "memo_database"
                )
                    // ここにMigrationを追加（現在はversion 5で固定）
                    // .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}