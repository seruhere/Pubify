package com.serma.pubify.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "converted_books")
data class ConvertedBook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val originalFileName: String,
    val epubFilePath: String,
    val coverImagePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val pageCount: Int = 0,
    val chapterCount: Int = 0,
    val convertedAtMillis: Long = System.currentTimeMillis(),
    val lastReadChapterIndex: Int = 0,
    val lastReadScrollOffset: Int = 0,
    val isFavorite: Boolean = false,
    val isCompleted: Boolean = false,
    val deviceSavedPath: String? = null
)
