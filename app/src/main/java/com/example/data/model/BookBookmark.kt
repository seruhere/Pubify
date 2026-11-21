package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_bookmarks")
data class BookBookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val bookTitle: String = "",
    val chapterIndex: Int,
    val chapterTitle: String = "",
    val pageNumber: Int,
    val totalBookPages: Int,
    val scrollOffsetFraction: Float = 0f,
    val snippet: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
