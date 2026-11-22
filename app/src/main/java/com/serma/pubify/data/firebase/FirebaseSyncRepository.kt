package com.serma.pubify.data.firebase

import android.content.Context
import com.serma.pubify.R
import com.serma.pubify.data.model.BookBookmark
import com.serma.pubify.data.model.BookHighlight
import com.serma.pubify.data.model.ConvertedBook
import com.serma.pubify.data.model.ReadingSession
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to access Pubify cloud sync.")

    private fun userDocRef(userId: String) =
        db.collection("users").document(sanitizeCloudId(userId))

    private fun booksCollection(userId: String) =
        userDocRef(userId).collection("books")

    private fun sessionsCollection(userId: String) =
        userDocRef(userId).collection("reading_sessions")

    private fun highlightsCollection(userId: String) =
        userDocRef(userId).collection("highlights")

    private fun bookmarksCollection(userId: String) =
        userDocRef(userId).collection("bookmarks")

    fun observeUserProfile(userId: String): Flow<CloudUserProfile?> {
        val safeUid = sanitizeCloudId(userId)
        return userDocRef(safeUid)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        CloudUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
    }

    fun observeCloudBooks(userId: String): Flow<List<CloudBookRecord>> {
        val safeUid = sanitizeCloudId(userId)
        return booksCollection(safeUid)
            .orderBy("convertedAtMillis", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        CloudBookRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
    }

    fun observeReadingSessions(userId: String): Flow<List<CloudReadingSession>> {
        val safeUid = sanitizeCloudId(userId)
        return sessionsCollection(safeUid)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        CloudReadingSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
    }

    suspend fun upsertUserProfile(
        displayName: String,
        email: String?,
        photoUrl: String?,
        totalBooksSynced: Int
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid"
        return try {
            val docRef = userDocRef(uid)
            val profile = CloudUserProfile(
                id = uid,
                userId = uid,
                displayName = displayName.trim().ifBlank { "Pubify Reader" }.take(120),
                email = email?.trim()?.takeIf { it.isNotEmpty() }?.take(254),
                photoUrl = photoUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
                totalBooksSynced = totalBooksSynced.coerceIn(0, 100_000)
            )
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(profile.toUpdateMap()).await()
            } else {
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncBook(book: ConvertedBook): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = CloudBookRecord.docIdFor(book)
        val path = "users/$uid/books/$docId"
        return try {
            val record = CloudBookRecord.fromLocal(uid, book)
            val docRef = booksCollection(uid).document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(record.toUpdateMap()).await()
            } else {
                docRef.set(record.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBook(book: ConvertedBook): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = CloudBookRecord.docIdFor(book)
        val path = "users/$uid/books/$docId"
        return try {
            booksCollection(uid).document(docId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun syncReadingSession(session: ReadingSession, bookKey: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = CloudReadingSession.docIdFor(session)
        val path = "users/$uid/reading_sessions/$docId"
        return try {
            val docRef = sessionsCollection(uid).document(docId)
            if (!docRef.get().await().exists()) {
                val record = CloudReadingSession.fromLocal(uid, session, bookKey)
                docRef.set(record.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncHighlight(highlight: BookHighlight, bookKey: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = CloudHighlight.docIdFor(highlight)
        val path = "users/$uid/highlights/$docId"
        return try {
            val record = CloudHighlight.fromLocal(uid, highlight, bookKey)
            val docRef = highlightsCollection(uid).document(docId)
            if (docRef.get().await().exists()) {
                docRef.update(record.toUpdateMap()).await()
            } else {
                docRef.set(record.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncBookmark(bookmark: BookBookmark, bookKey: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = CloudBookmark.docIdFor(bookmark)
        val path = "users/$uid/bookmarks/$docId"
        return try {
            val docRef = bookmarksCollection(uid).document(docId)
            if (!docRef.get().await().exists()) {
                val record = CloudBookmark.fromLocal(uid, bookmark, bookKey)
                docRef.set(record.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncFullLibrary(
        displayName: String,
        email: String?,
        photoUrl: String?,
        books: List<ConvertedBook>,
        sessions: List<ReadingSession>,
        highlights: List<BookHighlight>,
        bookmarks: List<BookBookmark>
    ): Result<Int> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid"
        return try {
            val bookKeyById = books.associate { it.id to CloudBookRecord.docIdFor(it) }

            for (book in books) {
                syncBook(book).getOrThrow()
            }
            for (session in sessions) {
                val key = bookKeyById[session.bookId] ?: sanitizeCloudId("book_${session.bookId}")
                syncReadingSession(session, key).getOrThrow()
            }
            for (highlight in highlights) {
                val key = bookKeyById[highlight.bookId] ?: sanitizeCloudId("book_${highlight.bookId}")
                syncHighlight(highlight, key).getOrThrow()
            }
            for (bookmark in bookmarks) {
                val key = bookKeyById[bookmark.bookId] ?: sanitizeCloudId("book_${bookmark.bookId}")
                syncBookmark(bookmark, key).getOrThrow()
            }

            upsertUserProfile(
                displayName = displayName,
                email = email,
                photoUrl = photoUrl,
                totalBooksSynced = books.size
            ).getOrThrow()

            Result.success(books.size)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
