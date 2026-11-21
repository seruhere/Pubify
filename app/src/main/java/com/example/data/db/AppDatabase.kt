package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BookBookmark
import com.example.data.model.BookHighlight
import com.example.data.model.ConvertedBook
import com.example.data.model.ReadingSession

@Database(entities = [ConvertedBook::class, BookHighlight::class, BookBookmark::class, ReadingSession::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                fun hasColumn(table: String, column: String): Boolean {
                    db.query("PRAGMA table_info($table)").use { cursor ->
                        val nameIdx = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            if (nameIdx >= 0 && cursor.getString(nameIdx).equals(column, ignoreCase = true)) {
                                return true
                            }
                        }
                    }
                    return false
                }
                if (!hasColumn("book_highlights", "scope")) {
                    db.execSQL("ALTER TABLE book_highlights ADD COLUMN scope TEXT NOT NULL DEFAULT 'PASSAGE'")
                }
                if (!hasColumn("converted_books", "deviceSavedPath")) {
                    db.execSQL("ALTER TABLE converted_books ADD COLUMN deviceSavedPath TEXT")
                }
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pdf_epub_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
