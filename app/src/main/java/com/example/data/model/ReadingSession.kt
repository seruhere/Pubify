package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_sessions")
data class ReadingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long = 0,
    val bookTitle: String = "",
    val durationSeconds: Long = 0L,
    val pagesRead: Int = 0,
    val startPage: Int = 0,
    val endPage: Int = 0,
    val timestampMillis: Long = System.currentTimeMillis()
)
