package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CloudBookRecord
import com.example.data.firebase.CloudReadingSession
import com.example.data.firebase.CloudUserProfile
import com.example.data.firebase.FirebaseSyncRepository
import com.example.data.firebase.OperationType
import com.example.data.firebase.logFirestoreError
import com.example.data.model.BookBookmark
import com.example.data.model.BookHighlight
import com.example.data.model.ConvertedBook
import com.example.data.model.ReadingSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

class CloudSyncViewModel(
    private val repository: FirebaseSyncRepository,
    private val currentUserId: String
) : ViewModel() {

    val userProfileState: StateFlow<CloudUiState<CloudUserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<CloudUserProfile?, CloudUiState<CloudUserProfile?>> { profile ->
                CloudUiState.Success(profile)
            }
            .catch { cause ->
                if (cause is CancellationException) throw cause
                logFirestoreError(cause, OperationType.GET, "users/$currentUserId")
                emit(CloudUiState.Error(cause.localizedMessage ?: "Failed to load cloud profile."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    val cloudBooksState: StateFlow<CloudUiState<List<CloudBookRecord>>> =
        repository.observeCloudBooks(currentUserId)
            .map<List<CloudBookRecord>, CloudUiState<List<CloudBookRecord>>> { books ->
                CloudUiState.Success(books)
            }
            .catch { cause ->
                if (cause is CancellationException) throw cause
                logFirestoreError(cause, OperationType.LIST, "users/$currentUserId/books")
                emit(CloudUiState.Error(cause.localizedMessage ?: "Failed to load synced books."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    val cloudSessionsState: StateFlow<CloudUiState<List<CloudReadingSession>>> =
        repository.observeReadingSessions(currentUserId)
            .map<List<CloudReadingSession>, CloudUiState<List<CloudReadingSession>>> { sessions ->
                CloudUiState.Success(sessions)
            }
            .catch { cause ->
                if (cause is CancellationException) throw cause
                logFirestoreError(cause, OperationType.LIST, "users/$currentUserId/reading_sessions")
                emit(CloudUiState.Error(cause.localizedMessage ?: "Failed to load synced reading stats."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncedAtMillis = MutableStateFlow<Long?>(null)
    val lastSyncedAtMillis: StateFlow<Long?> = _lastSyncedAtMillis.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    fun syncProfile(
        displayName: String,
        email: String?,
        photoUrl: String?,
        totalBooksCount: Int
    ) {
        viewModelScope.launch {
            repository.upsertUserProfile(
                displayName = displayName,
                email = email,
                photoUrl = photoUrl,
                totalBooksSynced = totalBooksCount
            ).onFailure { err ->
                _syncError.value = err.localizedMessage ?: "Unable to update cloud profile."
            }
        }
    }

    fun syncBooksSilently(
        displayName: String,
        email: String?,
        photoUrl: String?,
        books: List<ConvertedBook>
    ) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            var hadError = false
            for (book in books) {
                val res = repository.syncBook(book)
                if (res.isFailure) {
                    hadError = true
                    _syncError.value = res.exceptionOrNull()?.localizedMessage ?: "Cloud book sync failed."
                }
            }
            repository.upsertUserProfile(
                displayName = displayName,
                email = email,
                photoUrl = photoUrl,
                totalBooksSynced = books.size
            ).onFailure { err ->
                hadError = true
                _syncError.value = err.localizedMessage ?: "Cloud profile sync failed."
            }
            if (!hadError) {
                _lastSyncedAtMillis.value = System.currentTimeMillis()
            }
            _isSyncing.value = false
        }
    }

    fun syncFullLibraryNow(
        displayName: String,
        email: String?,
        photoUrl: String?,
        books: List<ConvertedBook>,
        sessions: List<ReadingSession>,
        highlights: List<BookHighlight>,
        bookmarks: List<BookBookmark>
    ) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            _syncStatusMessage.value = null

            val result = repository.syncFullLibrary(
                displayName = displayName,
                email = email,
                photoUrl = photoUrl,
                books = books,
                sessions = sessions,
                highlights = highlights,
                bookmarks = bookmarks
            )

            result.onSuccess { syncedCount ->
                _lastSyncedAtMillis.value = System.currentTimeMillis()
                _syncStatusMessage.value = "Synced $syncedCount books & reading progress to Firebase"
            }.onFailure { err ->
                _syncError.value = err.localizedMessage ?: "Sync failed. Please check your internet connection."
            }

            _isSyncing.value = false
        }
    }

    fun deleteCloudBook(book: ConvertedBook) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }

    fun clearSyncFeedback() {
        _syncStatusMessage.value = null
        _syncError.value = null
    }
}
