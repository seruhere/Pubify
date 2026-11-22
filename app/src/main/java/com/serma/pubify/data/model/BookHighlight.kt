package com.serma.pubify.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_highlights")
data class BookHighlight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val bookTitle: String = "",
    val chapterIndex: Int,
    val chapterTitle: String = "",
    val paragraphIndex: Int,
    val text: String,
    val colorHex: Long = 0xFFFFEB3B, // Default yellow (0xFFFFEB3B)
    val note: String = "",
    val scope: String = "PASSAGE", // "WORD", "LINE", or "PASSAGE"
    val createdAtMillis: Long = System.currentTimeMillis()
)
