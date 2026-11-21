package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ConvertedBook
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM converted_books ORDER BY convertedAtMillis DESC")
    fun getAllBooks(): Flow<List<ConvertedBook>>

    @Query("SELECT * FROM converted_books WHERE id = :id")
    fun getBookById(id: Long): Flow<ConvertedBook?>

    @Query("SELECT * FROM converted_books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY convertedAtMillis DESC")
    fun searchBooks(query: String): Flow<List<ConvertedBook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: ConvertedBook): Long

    @Update
    suspend fun updateBook(book: ConvertedBook)

    @Delete
    suspend fun deleteBook(book: ConvertedBook)

    @Query("DELETE FROM converted_books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("UPDATE converted_books SET lastReadChapterIndex = :chapterIndex, lastReadScrollOffset = :offset WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: Long, chapterIndex: Int, offset: Int)

    @Query("UPDATE converted_books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun toggleFavorite(bookId: Long, isFavorite: Boolean)

    @Query("UPDATE converted_books SET isCompleted = :isCompleted WHERE id = :bookId")
    suspend fun toggleBookCompleted(bookId: Long, isCompleted: Boolean)

    @Query("SELECT * FROM book_highlights WHERE bookId = :bookId ORDER BY chapterIndex ASC, paragraphIndex ASC, createdAtMillis ASC")
    fun getHighlightsForBook(bookId: Long): Flow<List<com.example.data.model.BookHighlight>>

    @Query("SELECT * FROM book_highlights ORDER BY createdAtMillis DESC")
    fun getAllHighlights(): Flow<List<com.example.data.model.BookHighlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: com.example.data.model.BookHighlight): Long

    @Delete
    suspend fun deleteHighlight(highlight: com.example.data.model.BookHighlight)

    @Query("DELETE FROM book_highlights WHERE id = :id")
    suspend fun deleteHighlightById(id: Long)

    @Query("SELECT * FROM book_bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC, createdAtMillis ASC")
    fun getBookmarksForBook(bookId: Long): Flow<List<com.example.data.model.BookBookmark>>

    @Query("SELECT * FROM book_bookmarks ORDER BY createdAtMillis DESC")
    fun getAllBookmarks(): Flow<List<com.example.data.model.BookBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: com.example.data.model.BookBookmark): Long

    @Delete
    suspend fun deleteBookmark(bookmark: com.example.data.model.BookBookmark)

    @Query("DELETE FROM book_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM book_bookmarks WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteBookmarkByPage(bookId: Long, pageNumber: Int)

    @Query("SELECT * FROM reading_sessions ORDER BY timestampMillis DESC")
    fun getAllReadingSessions(): Flow<List<com.example.data.model.ReadingSession>>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY timestampMillis DESC")
    fun getReadingSessionsForBook(bookId: Long): Flow<List<com.example.data.model.ReadingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingSession(session: com.example.data.model.ReadingSession): Long

    @Delete
    suspend fun deleteReadingSession(session: com.example.data.model.ReadingSession)

    @Query("DELETE FROM reading_sessions WHERE id = :id")
    suspend fun deleteReadingSessionById(id: Long)

    @Query("DELETE FROM reading_sessions")
    suspend fun clearAllReadingSessions()
}
