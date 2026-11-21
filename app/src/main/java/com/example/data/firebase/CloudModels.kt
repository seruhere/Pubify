package com.example.data.firebase

import androidx.annotation.Keep
import com.example.data.model.BookBookmark
import com.example.data.model.BookHighlight
import com.example.data.model.ConvertedBook
import com.example.data.model.ReadingSession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    val totalBooksSynced: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("displayName", displayName.trim().ifBlank { "Pubify Reader" }.take(120))
        email?.trim()?.takeIf { it.isNotEmpty() }?.let { put("email", it.take(254)) }
        photoUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { put("photoUrl", it.take(500)) }
        put("totalBooksSynced", totalBooksSynced.coerceIn(0, 100_000))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }

    fun toUpdateMap(): Map<String, Any> = buildMap {
        put("displayName", displayName.trim().ifBlank { "Pubify Reader" }.take(120))
        email?.trim()?.takeIf { it.isNotEmpty() }?.let { put("email", it.take(254)) }
        photoUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { put("photoUrl", it.take(500)) }
        put("totalBooksSynced", totalBooksSynced.coerceIn(0, 100_000))
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class CloudBookRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val author: String = "",
    val originalFileName: String = "",
    val fileSizeBytes: Long = 0L,
    val pageCount: Int = 0,
    val chapterCount: Int = 0,
    val lastReadChapterIndex: Int = 0,
    val favorite: Boolean = false,
    val completed: Boolean = false,
    val convertedAtMillis: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.trim().ifBlank { "Untitled Book" }.take(300),
        "author" to author.trim().ifBlank { "Unknown Author" }.take(200),
        "originalFileName" to originalFileName.trim().ifBlank { "document.pdf" }.take(300),
        "fileSizeBytes" to fileSizeBytes.coerceIn(0L, 2_147_483_648L),
        "pageCount" to pageCount.coerceIn(0, 100_000),
        "chapterCount" to chapterCount.coerceIn(0, 10_000),
        "lastReadChapterIndex" to lastReadChapterIndex.coerceIn(0, 10_000),
        "favorite" to favorite,
        "completed" to completed,
        "convertedAtMillis" to convertedAtMillis.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "title" to title.trim().ifBlank { "Untitled Book" }.take(300),
        "author" to author.trim().ifBlank { "Unknown Author" }.take(200),
        "originalFileName" to originalFileName.trim().ifBlank { "document.pdf" }.take(300),
        "fileSizeBytes" to fileSizeBytes.coerceIn(0L, 2_147_483_648L),
        "pageCount" to pageCount.coerceIn(0, 100_000),
        "chapterCount" to chapterCount.coerceIn(0, 10_000),
        "lastReadChapterIndex" to lastReadChapterIndex.coerceIn(0, 10_000),
        "favorite" to favorite,
        "completed" to completed,
        "convertedAtMillis" to convertedAtMillis.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun docIdFor(book: ConvertedBook): String =
            sanitizeCloudId("book_${book.id}_${book.convertedAtMillis.coerceAtLeast(0L)}")

        fun fromLocal(userId: String, book: ConvertedBook): CloudBookRecord = CloudBookRecord(
            id = docIdFor(book),
            userId = userId,
            title = book.title.trim().ifBlank { "Untitled Book" }.take(300),
            author = book.author.trim().ifBlank { "Unknown Author" }.take(200),
            originalFileName = book.originalFileName.trim().ifBlank { "document.pdf" }.take(300),
            fileSizeBytes = book.fileSizeBytes.coerceIn(0L, 2_147_483_648L),
            pageCount = book.pageCount.coerceIn(0, 100_000),
            chapterCount = book.chapterCount.coerceIn(0, 10_000),
            lastReadChapterIndex = book.lastReadChapterIndex.coerceIn(0, 10_000),
            favorite = book.isFavorite,
            completed = book.isCompleted,
            convertedAtMillis = book.convertedAtMillis.coerceAtLeast(0L)
        )
    }
}

@Keep
@IgnoreExtraProperties
data class CloudReadingSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookKey: String = "",
    val bookTitle: String = "",
    val durationSeconds: Long = 0L,
    val pagesRead: Int = 0,
    val startPage: Int = 0,
    val endPage: Int = 0,
    val timestampMillis: Long = 0L,
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "bookKey" to sanitizeCloudId(bookKey.ifBlank { "book_0" }),
        "bookTitle" to bookTitle.trim().take(300),
        "durationSeconds" to durationSeconds.coerceIn(0L, 86_400L),
        "pagesRead" to pagesRead.coerceIn(0, 10_000),
        "startPage" to startPage.coerceIn(0, 100_000),
        "endPage" to endPage.coerceIn(0, 100_000),
        "timestampMillis" to timestampMillis.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun docIdFor(session: ReadingSession): String =
            sanitizeCloudId("sess_${session.id}_${session.timestampMillis.coerceAtLeast(0L)}")

        fun fromLocal(userId: String, session: ReadingSession, bookKey: String): CloudReadingSession =
            CloudReadingSession(
                id = docIdFor(session),
                userId = userId,
                bookKey = sanitizeCloudId(bookKey.ifBlank { "book_${session.bookId}" }),
                bookTitle = session.bookTitle.trim().take(300),
                durationSeconds = session.durationSeconds.coerceIn(0L, 86_400L),
                pagesRead = session.pagesRead.coerceIn(0, 10_000),
                startPage = session.startPage.coerceIn(0, 100_000),
                endPage = session.endPage.coerceIn(0, 100_000),
                timestampMillis = session.timestampMillis.coerceAtLeast(0L)
            )
    }
}

@Keep
@IgnoreExtraProperties
data class CloudHighlight(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookKey: String = "",
    val bookTitle: String = "",
    val chapterIndex: Int = 0,
    val chapterTitle: String = "",
    val paragraphIndex: Int = 0,
    val text: String = "",
    val colorHex: Long = 0xFFFFEB3BL,
    val note: String = "",
    val scope: String = "PASSAGE",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "bookKey" to sanitizeCloudId(bookKey.ifBlank { "book_0" }),
        "bookTitle" to bookTitle.trim().take(300),
        "chapterIndex" to chapterIndex.coerceIn(0, 10_000),
        "chapterTitle" to chapterTitle.trim().take(300),
        "paragraphIndex" to paragraphIndex.coerceIn(0, 100_000),
        "text" to text.trim().ifBlank { "Highlight" }.take(5_000),
        "colorHex" to colorHex,
        "note" to note.trim().take(2_000),
        "scope" to normalizeHighlightScope(scope),
        "createdAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "note" to note.trim().take(2_000),
        "colorHex" to colorHex
    )

    companion object {
        private val VALID_SCOPES = setOf("WORD", "LINE", "PASSAGE", "CUSTOM")

        fun normalizeHighlightScope(raw: String): String {
            val upper = raw.trim().uppercase()
            return if (upper in VALID_SCOPES) upper else "PASSAGE"
        }

        fun docIdFor(highlight: BookHighlight): String =
            sanitizeCloudId("hl_${highlight.id}_${highlight.createdAtMillis.coerceAtLeast(0L)}")

        fun fromLocal(userId: String, highlight: BookHighlight, bookKey: String): CloudHighlight =
            CloudHighlight(
                id = docIdFor(highlight),
                userId = userId,
                bookKey = sanitizeCloudId(bookKey.ifBlank { "book_${highlight.bookId}" }),
                bookTitle = highlight.bookTitle.trim().take(300),
                chapterIndex = highlight.chapterIndex.coerceIn(0, 10_000),
                chapterTitle = highlight.chapterTitle.trim().take(300),
                paragraphIndex = highlight.paragraphIndex.coerceIn(0, 100_000),
                text = highlight.text.trim().ifBlank { "Highlight" }.take(5_000),
                colorHex = highlight.colorHex,
                note = highlight.note.trim().take(2_000),
                scope = normalizeHighlightScope(highlight.scope)
            )
    }
}

@Keep
@IgnoreExtraProperties
data class CloudBookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookKey: String = "",
    val bookTitle: String = "",
    val chapterIndex: Int = 0,
    val chapterTitle: String = "",
    val pageNumber: Int = 0,
    val totalBookPages: Int = 0,
    val scrollOffsetFraction: Double = 0.0,
    val snippet: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "bookKey" to sanitizeCloudId(bookKey.ifBlank { "book_0" }),
        "bookTitle" to bookTitle.trim().take(300),
        "chapterIndex" to chapterIndex.coerceIn(0, 10_000),
        "chapterTitle" to chapterTitle.trim().take(300),
        "pageNumber" to pageNumber.coerceIn(0, 100_000),
        "totalBookPages" to totalBookPages.coerceIn(0, 100_000),
        "scrollOffsetFraction" to scrollOffsetFraction.coerceIn(0.0, 1.0),
        "snippet" to snippet.trim().take(500),
        "createdAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun docIdFor(bookmark: BookBookmark): String =
            sanitizeCloudId("bm_${bookmark.id}_${bookmark.createdAtMillis.coerceAtLeast(0L)}")

        fun fromLocal(userId: String, bookmark: BookBookmark, bookKey: String): CloudBookmark =
            CloudBookmark(
                id = docIdFor(bookmark),
                userId = userId,
                bookKey = sanitizeCloudId(bookKey.ifBlank { "book_${bookmark.bookId}" }),
                bookTitle = bookmark.bookTitle.trim().take(300),
                chapterIndex = bookmark.chapterIndex.coerceIn(0, 10_000),
                chapterTitle = bookmark.chapterTitle.trim().take(300),
                pageNumber = bookmark.pageNumber.coerceIn(0, 100_000),
                totalBookPages = bookmark.totalBookPages.coerceIn(0, 100_000),
                scrollOffsetFraction = bookmark.scrollOffsetFraction.toDouble().coerceIn(0.0, 1.0),
                snippet = bookmark.snippet.trim().take(500)
            )
    }
}

private val SAFE_ID_REGEX = Regex("[^a-zA-Z0-9_\\-]")

/**
 * Guarantees every Firestore document ID matches `isValidId(id)` in firestore.rules:
 * `^[a-zA-Z0-9_\-]+$` with length 1..128.
 */
fun sanitizeCloudId(raw: String): String {
    val cleaned = raw.replace(SAFE_ID_REGEX, "_").take(128)
    return cleaned.ifEmpty { "doc_0" }
}
