package com.example.data.repository

import com.example.data.db.BookDao
import com.example.data.model.ConvertedBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class BookRepository(private val bookDao: BookDao) {
    val allBooks: Flow<List<ConvertedBook>> = bookDao.getAllBooks()

    fun getBookById(id: Long): Flow<ConvertedBook?> = bookDao.getBookById(id)

    fun searchBooks(query: String): Flow<List<ConvertedBook>> = bookDao.searchBooks(query)

    suspend fun saveBook(book: ConvertedBook): Long = withContext(Dispatchers.IO) {
        bookDao.insertBook(book)
    }

    suspend fun deleteBook(book: ConvertedBook) = withContext(Dispatchers.IO) {
        // Delete generated EPUB file
        try {
            val epubFile = File(book.epubFilePath)
            if (epubFile.exists()) {
                epubFile.delete()
            }
        } catch (_: Exception) {}

        // Delete generated cover file if exists
        book.coverImagePath?.let { path ->
            try {
                val coverFile = File(path)
                if (coverFile.exists()) {
                    coverFile.delete()
                }
            } catch (_: Exception) {}
        }

        bookDao.deleteBook(book)
    }

    suspend fun updateReadingProgress(bookId: Long, chapterIndex: Int, offset: Int) = withContext(Dispatchers.IO) {
        bookDao.updateReadingProgress(bookId, chapterIndex, offset)
    }

    suspend fun toggleFavorite(bookId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        bookDao.toggleFavorite(bookId, isFavorite)
    }

    suspend fun updateBook(book: ConvertedBook) = withContext(Dispatchers.IO) {
        bookDao.updateBook(book)
    }

    fun getHighlightsForBook(bookId: Long): Flow<List<com.example.data.model.BookHighlight>> =
        bookDao.getHighlightsForBook(bookId)

    fun getAllHighlights(): Flow<List<com.example.data.model.BookHighlight>> =
        bookDao.getAllHighlights()

    suspend fun saveHighlight(highlight: com.example.data.model.BookHighlight): Long = withContext(Dispatchers.IO) {
        bookDao.insertHighlight(highlight)
    }

    suspend fun deleteHighlight(highlight: com.example.data.model.BookHighlight) = withContext(Dispatchers.IO) {
        bookDao.deleteHighlight(highlight)
    }

    suspend fun deleteHighlightById(id: Long) = withContext(Dispatchers.IO) {
        bookDao.deleteHighlightById(id)
    }

    fun getBookmarksForBook(bookId: Long): Flow<List<com.example.data.model.BookBookmark>> =
        bookDao.getBookmarksForBook(bookId)

    fun getAllBookmarks(): Flow<List<com.example.data.model.BookBookmark>> =
        bookDao.getAllBookmarks()

    suspend fun saveBookmark(bookmark: com.example.data.model.BookBookmark): Long = withContext(Dispatchers.IO) {
        bookDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: com.example.data.model.BookBookmark) = withContext(Dispatchers.IO) {
        bookDao.deleteBookmark(bookmark)
    }

    suspend fun deleteBookmarkById(id: Long) = withContext(Dispatchers.IO) {
        bookDao.deleteBookmarkById(id)
    }

    suspend fun deleteBookmarkByPage(bookId: Long, pageNumber: Int) = withContext(Dispatchers.IO) {
        bookDao.deleteBookmarkByPage(bookId, pageNumber)
    }

    suspend fun toggleBookCompleted(bookId: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        bookDao.toggleBookCompleted(bookId, isCompleted)
    }

    val allReadingSessions: Flow<List<com.example.data.model.ReadingSession>> =
        bookDao.getAllReadingSessions()

    fun getReadingSessionsForBook(bookId: Long): Flow<List<com.example.data.model.ReadingSession>> =
        bookDao.getReadingSessionsForBook(bookId)

    suspend fun saveReadingSession(session: com.example.data.model.ReadingSession): Long = withContext(Dispatchers.IO) {
        bookDao.insertReadingSession(session)
    }

    suspend fun deleteReadingSession(session: com.example.data.model.ReadingSession) = withContext(Dispatchers.IO) {
        bookDao.deleteReadingSession(session)
    }

    suspend fun deleteReadingSessionById(id: Long) = withContext(Dispatchers.IO) {
        bookDao.deleteReadingSessionById(id)
    }

    suspend fun clearAllReadingSessions() = withContext(Dispatchers.IO) {
        bookDao.clearAllReadingSessions()
    }
}
