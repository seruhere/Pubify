package com.serma.pubify.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.serma.pubify.PdfToEpubApp
import com.serma.pubify.converter.ChapterSplitMode
import com.serma.pubify.converter.ConversionOptions
import com.serma.pubify.converter.ConversionProgress
import com.serma.pubify.converter.ConversionStage
import com.serma.pubify.converter.PdfMetadata
import com.serma.pubify.converter.PdfToEpubConverter
import com.serma.pubify.converter.TypographyPreset
import com.serma.pubify.data.model.BookBookmark
import com.serma.pubify.data.model.BookHighlight
import com.serma.pubify.data.model.ConvertedBook
import com.serma.pubify.data.model.ReadingSession
import com.serma.pubify.data.model.ReadingStatsSummary
import com.serma.pubify.data.model.ReadingStatsCalculator
import com.serma.pubify.reader.EpubParser
import com.serma.pubify.reader.ParsedEpubBook
import com.serma.pubify.reader.ReaderSettings
import com.serma.pubify.reader.ReaderTheme
import com.serma.pubify.reader.ReaderTtsManager
import com.serma.pubify.reader.ThemeMode
import com.serma.pubify.reader.TtsState
import com.serma.pubify.ui.components.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class BookSortOrder {
    DATE_DESC,
    TITLE_ASC,
    PAGES_DESC
}

enum class BookFilter {
    ALL,
    FAVORITES,
    READING,
    COMPLETED
}

data class BatchItem(
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    var status: String = "Pending",
    var progress: Float = 0f,
    var error: String? = null
)

data class CacheClearDelta(
    val beforeBytes: Long,
    val afterBytes: Long,
    val freedBytes: Long,
    val timestampMillis: Long = System.currentTimeMillis()
)

class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as PdfToEpubApp).repository
    private val converter = PdfToEpubConverter(application)
    private val epubParser = EpubParser()

    private val prefs = application.getSharedPreferences("reader_app_prefs", Context.MODE_PRIVATE)

    // Dark Mode / Theme Mode state (defaults to Dark theme)
    private val _themeMode = MutableStateFlow(
        when (prefs.getString("app_theme_mode", "DARK")) {
            "LIGHT" -> ThemeMode.LIGHT
            "LIQUID_GLASS" -> ThemeMode.LIQUID_GLASS
            "SYSTEM" -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    private val _isGuestMode = MutableStateFlow(prefs.getBoolean("is_guest_mode", false))
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    fun setGuestMode(skip: Boolean) {
        _isGuestMode.value = skip
        prefs.edit().putBoolean("is_guest_mode", skip).apply()
    }

    fun toggleDarkMode() {
        val next = when (_themeMode.value) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.LIQUID_GLASS
            ThemeMode.LIQUID_GLASS -> ThemeMode.DARK
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setThemeMode(next)
    }

    // Hardware volume key event bus for page turning
    private val _volumeKeyEvent = kotlinx.coroutines.flow.MutableSharedFlow<Int>(extraBufferCapacity = 2)
    val volumeKeyEvent: kotlinx.coroutines.flow.SharedFlow<Int> = _volumeKeyEvent

    fun onVolumeKeyPressed(keyCode: Int): Boolean {
        val currentSettings = _readerSettings.value
        val isReaderActive = _activeReaderBook.value != null
        val volumeEnabled = currentSettings.pageTurnMode == com.serma.pubify.reader.PageTurnMode.VOLUME_KEYS ||
                currentSettings.pageTurnMode == com.serma.pubify.reader.PageTurnMode.TAP_AND_VOLUME
        if (isReaderActive && volumeEnabled && (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN)) {
            _volumeKeyEvent.tryEmit(keyCode)
            return true
        }
        return false
    }

    // Cache management state
    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    private val _cacheOperationMessage = MutableStateFlow<String?>(null)
    val cacheOperationMessage: StateFlow<String?> = _cacheOperationMessage.asStateFlow()

    private val _lastCacheDelta = MutableStateFlow<CacheClearDelta?>(null)
    val lastCacheDelta: StateFlow<CacheClearDelta?> = _lastCacheDelta.asStateFlow()

    // Text-to-Speech Engine
    val ttsManager = ReaderTtsManager(application, viewModelScope) { finishedIndex ->
        onTtsChapterFinished(finishedIndex)
    }
    val ttsState: StateFlow<TtsState> = ttsManager.state

    init {
        refreshCacheSize()
        healExistingBooksMetadata()
    }

    /**
     * Automatically repairs any previously converted books in My Books that were saved
     * with generic "Untitled Book" / "Unknown Author" metadata by inspecting their EPUB
     * and original filename on a background IO thread.
     */
    private fun healExistingBooksMetadata() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val books = repository.allBooks.first()
                for (book in books) {
                    val needsTitle = com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle(book.title)
                    val needsAuthor = com.serma.pubify.converter.PdfMetadataExtractor.isGenericAuthor(book.author)
                    if (!needsTitle && !needsAuthor) continue

                    val (fileTitle, fileAuthor) = com.serma.pubify.converter.PdfMetadataExtractor.parseFromFileName(book.originalFileName)
                    var newTitle = if (needsTitle) fileTitle else null
                    var newAuthor = if (needsAuthor) fileAuthor else null

                    if ((needsTitle && newTitle == null) || (needsAuthor && newAuthor == null)) {
                        val file = File(book.epubFilePath)
                        if (file.exists()) {
                            epubParser.parse(file).onSuccess { parsed ->
                                if (needsTitle && newTitle == null && !com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle(parsed.title)) {
                                    newTitle = parsed.title
                                }
                                if (needsAuthor && newAuthor == null && !com.serma.pubify.converter.PdfMetadataExtractor.isGenericAuthor(parsed.author)) {
                                    newAuthor = parsed.author
                                }
                            }
                        }
                    }

                    val updated = book.copy(
                        title = newTitle ?: book.title,
                        author = newAuthor ?: book.author
                    )
                    if (updated != book) {
                        repository.updateBook(updated)
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    // Single conversion state
    private val _selectedPdfUri = MutableStateFlow<Uri?>(null)
    val selectedPdfUri: StateFlow<Uri?> = _selectedPdfUri.asStateFlow()

    private val _pdfMetadata = MutableStateFlow<PdfMetadata?>(null)
    val pdfMetadata: StateFlow<PdfMetadata?> = _pdfMetadata.asStateFlow()

    private val _isLoadingMetadata = MutableStateFlow(false)
    val isLoadingMetadata: StateFlow<Boolean> = _isLoadingMetadata.asStateFlow()

    private val _conversionOptions = MutableStateFlow(
        ConversionOptions(
            title = "",
            author = "",
            splitMode = ChapterSplitMode.AUTO_DETECT,
            pagesPerChapter = 10,
            extractCover = prefs.getBoolean("default_extract_cover", true),
            typography = TypographyPreset.CLEAN_MODERN,
            stripHeadersFooters = prefs.getBoolean("default_strip_headers_footers", true),
            margin = runCatching {
                com.serma.pubify.converter.EpubMargin.valueOf(
                    prefs.getString("default_margin", ConversionOptions.DEFAULT_MARGIN.name)
                        ?: ConversionOptions.DEFAULT_MARGIN.name
                )
            }.getOrDefault(ConversionOptions.DEFAULT_MARGIN),
            imageCompression = runCatching {
                com.serma.pubify.converter.ImageCompressionLevel.valueOf(
                    prefs.getString("default_image_compression", ConversionOptions.DEFAULT_IMAGE_COMPRESSION.name)
                        ?: ConversionOptions.DEFAULT_IMAGE_COMPRESSION.name
                )
            }.getOrDefault(ConversionOptions.DEFAULT_IMAGE_COMPRESSION)
        )
    )
    val conversionOptions: StateFlow<ConversionOptions> = _conversionOptions.asStateFlow()

    private val _conversionProgress = MutableStateFlow(ConversionProgress())
    val conversionProgress: StateFlow<ConversionProgress> = _conversionProgress.asStateFlow()

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    private val _lastConvertedBook = MutableStateFlow<ConvertedBook?>(null)
    val lastConvertedBook: StateFlow<ConvertedBook?> = _lastConvertedBook.asStateFlow()

    private var conversionJob: Job? = null
    private var batchJob: Job? = null
    private var metadataJob: Job? = null
    private var scrollPersistJob: Job? = null
    private var userEditedTitle: Boolean = false
    private var userEditedAuthor: Boolean = false

    // Library state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(BookSortOrder.DATE_DESC)
    val sortOrder: StateFlow<BookSortOrder> = _sortOrder.asStateFlow()

    private val _bookFilter = MutableStateFlow(BookFilter.ALL)
    val bookFilter: StateFlow<BookFilter> = _bookFilter.asStateFlow()

    val libraryBooks: StateFlow<List<ConvertedBook>> = combine(
        repository.allBooks,
        _searchQuery,
        _sortOrder,
        _bookFilter
    ) { books, query, sort, filter ->
        var filtered = if (query.isBlank()) {
            books
        } else {
            books.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true) ||
                it.originalFileName.contains(query, ignoreCase = true)
            }
        }

        filtered = when (filter) {
            BookFilter.ALL -> filtered
            BookFilter.FAVORITES -> filtered.filter { it.isFavorite }
            BookFilter.READING -> filtered.filter {
                val isCompleted = it.isCompleted || (it.chapterCount > 1 && it.lastReadChapterIndex >= it.chapterCount - 1)
                !isCompleted
            }
            BookFilter.COMPLETED -> filtered.filter {
                it.isCompleted || (it.chapterCount > 1 && it.lastReadChapterIndex >= it.chapterCount - 1)
            }
        }

        when (sort) {
            BookSortOrder.DATE_DESC -> filtered.sortedByDescending { it.convertedAtMillis }
            BookSortOrder.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            BookSortOrder.PAGES_DESC -> filtered.sortedByDescending { it.pageCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Batch conversion state
    private val _batchQueue = MutableStateFlow<List<BatchItem>>(emptyList())
    val batchQueue: StateFlow<List<BatchItem>> = _batchQueue.asStateFlow()

    private val _isBatchRunning = MutableStateFlow(false)
    val isBatchRunning: StateFlow<Boolean> = _isBatchRunning.asStateFlow()

    private val _batchCurrentIndex = MutableStateFlow(0)
    val batchCurrentIndex: StateFlow<Int> = _batchCurrentIndex.asStateFlow()

    // Reader state
    private val _activeReaderBook = MutableStateFlow<ParsedEpubBook?>(null)
    val activeReaderBook: StateFlow<ParsedEpubBook?> = _activeReaderBook.asStateFlow()

    private val _activeBookRecord = MutableStateFlow<ConvertedBook?>(null)
    val activeBookRecord: StateFlow<ConvertedBook?> = _activeBookRecord.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _readerSettings = MutableStateFlow(
        ReaderSettings(
            textAlign = runCatching {
                com.serma.pubify.reader.ReaderTextAlign.valueOf(
                    prefs.getString("reader_text_align", com.serma.pubify.reader.ReaderTextAlign.JUSTIFY.name)
                        ?: com.serma.pubify.reader.ReaderTextAlign.JUSTIFY.name
                )
            }.getOrDefault(com.serma.pubify.reader.ReaderTextAlign.JUSTIFY),
            pageTurnMode = runCatching {
                com.serma.pubify.reader.PageTurnMode.valueOf(
                    prefs.getString("reader_page_turn_mode", com.serma.pubify.reader.PageTurnMode.TAP_AND_VOLUME.name)
                        ?: com.serma.pubify.reader.PageTurnMode.TAP_AND_VOLUME.name
                )
            }.getOrDefault(com.serma.pubify.reader.PageTurnMode.TAP_AND_VOLUME),
            readingMode = runCatching {
                com.serma.pubify.reader.ReaderReadingMode.valueOf(
                    prefs.getString("reader_reading_mode", com.serma.pubify.reader.ReaderReadingMode.PAGINATED.name)
                        ?: com.serma.pubify.reader.ReaderReadingMode.PAGINATED.name
                )
            }.getOrDefault(com.serma.pubify.reader.ReaderReadingMode.PAGINATED),
            showTapZonesOverlay = prefs.getBoolean("reader_show_tap_zones", false)
        )
    )
    val readerSettings: StateFlow<ReaderSettings> = _readerSettings.asStateFlow()

    private val _isLoadingReader = MutableStateFlow(false)
    val isLoadingReader: StateFlow<Boolean> = _isLoadingReader.asStateFlow()

    private val _bookHighlights = MutableStateFlow<List<BookHighlight>>(emptyList())
    val bookHighlights: StateFlow<List<BookHighlight>> = _bookHighlights.asStateFlow()

    private val _bookBookmarks = MutableStateFlow<List<BookBookmark>>(emptyList())
    val bookBookmarks: StateFlow<List<BookBookmark>> = _bookBookmarks.asStateFlow()

    val allBooks: StateFlow<List<ConvertedBook>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHighlights: StateFlow<List<BookHighlight>> = repository.getAllHighlights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookBookmark>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingSessions: StateFlow<List<ReadingSession>> = repository.allReadingSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingStatsSummary: StateFlow<ReadingStatsSummary> = combine(
        repository.allBooks,
        readingSessions
    ) { books, sessions ->
        ReadingStatsCalculator.calculate(books, sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingStatsSummary())

    private val _isReaderBarsVisible = MutableStateFlow(true)
    val isReaderBarsVisible: StateFlow<Boolean> = _isReaderBarsVisible.asStateFlow()

    fun setReaderBarsVisible(visible: Boolean) {
        _isReaderBarsVisible.value = visible
    }

    fun toggleReaderBarsVisible() {
        _isReaderBarsVisible.value = !_isReaderBarsVisible.value
    }

    fun onPdfSelected(uri: Uri) {
        metadataJob?.cancel()
        userEditedTitle = false
        userEditedAuthor = false
        _selectedPdfUri.value = uri
        _pdfMetadata.value = null
        _isLoadingMetadata.value = true

        // Immediately resolve a clean filename-based title/author preview if non-generic
        val quickFileName = resolveDisplayName(uri)
        val (quickTitle, quickAuthor) = com.serma.pubify.converter.PdfMetadataExtractor.parseFromFileName(quickFileName)
        val previewTitle = quickTitle?.takeUnless { com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle(it) } ?: ""
        val previewAuthor = quickAuthor?.takeUnless { com.serma.pubify.converter.PdfMetadataExtractor.isGenericAuthor(it) } ?: ""
        _conversionOptions.value = _conversionOptions.value.copy(
            title = previewTitle,
            author = previewAuthor,
            pageStart = 1,
            pageEnd = 1
        )

        metadataJob = viewModelScope.launch {
            val result = converter.extractMetadata(uri)
            if (_selectedPdfUri.value != uri) return@launch
            _isLoadingMetadata.value = false
            result.onSuccess { metadata ->
                _pdfMetadata.value = metadata
                val current = _conversionOptions.value
                val finalTitle = if (userEditedTitle && current.title.isNotBlank()) current.title else metadata.title
                val finalAuthor = if (userEditedAuthor && current.author.isNotBlank()) current.author else metadata.author
                _conversionOptions.value = current.copy(
                    title = finalTitle,
                    author = finalAuthor,
                    pageStart = 1,
                    pageEnd = metadata.pageCount.coerceAtLeast(1)
                )
            }.onFailure {
                val fallbackTitle = previewTitle.ifBlank {
                    com.serma.pubify.converter.PdfMetadataExtractor.cleanRawFileName(quickFileName)
                        .takeUnless { com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle(it) }
                        ?: ""
                }
                val fallbackAuthor = previewAuthor
                _pdfMetadata.value = PdfMetadata(
                    fileName = quickFileName,
                    title = fallbackTitle.ifBlank { "Untitled Book" },
                    author = fallbackAuthor.ifBlank { "Unknown Author" },
                    subject = "",
                    creator = "",
                    pageCount = 1,
                    fileSizeBytes = 0L
                )
                val current = _conversionOptions.value
                _conversionOptions.value = current.copy(
                    title = if (userEditedTitle && current.title.isNotBlank()) current.title else fallbackTitle,
                    author = if (userEditedAuthor && current.author.isNotBlank()) current.author else fallbackAuthor,
                    pageStart = 1,
                    pageEnd = 1
                )
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        val context = getApplication<Application>()
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { return it }
                    }
                }
            }
        } catch (_: Throwable) {}
        val decoded = try {
            Uri.decode(uri.lastPathSegment ?: uri.path ?: "")
        } catch (_: Throwable) {
            uri.lastPathSegment ?: uri.path ?: ""
        }
        return decoded.substringAfterLast('/').substringAfterLast(':').takeIf { it.isNotBlank() } ?: "document.pdf"
    }

    fun updateConversionOptions(updater: (ConversionOptions) -> ConversionOptions) {
        val prev = _conversionOptions.value
        val next = updater(prev)
        if (next.title != prev.title) userEditedTitle = true
        if (next.author != prev.author) userEditedAuthor = true
        _conversionOptions.value = next
        if (next.extractCover != prev.extractCover ||
            next.stripHeadersFooters != prev.stripHeadersFooters ||
            next.margin != prev.margin ||
            next.imageCompression != prev.imageCompression
        ) {
            prefs.edit()
                .putBoolean("default_extract_cover", next.extractCover)
                .putBoolean("default_strip_headers_footers", next.stripHeadersFooters)
                .putString("default_margin", next.margin.name)
                .putString("default_image_compression", next.imageCompression.name)
                .apply()
        }
    }

    fun resetConversionSettingsToDefaults() {
        _conversionOptions.value = _conversionOptions.value.copy(
            fontFamily = ConversionOptions.DEFAULT_FONT_FAMILY,
            fontSizePt = ConversionOptions.DEFAULT_FONT_SIZE_PT,
            margin = ConversionOptions.DEFAULT_MARGIN,
            customMarginPercent = null,
            imageCompression = ConversionOptions.DEFAULT_IMAGE_COMPRESSION,
            customImageQuality = null
        )
    }

    fun startConversion() {
        val uri = _selectedPdfUri.value ?: return

        _isConverting.value = true
        _conversionProgress.value = ConversionProgress(stage = ConversionStage.INITIALIZING)

        conversionJob = viewModelScope.launch {
            try {
                // If background metadata probe is still running, let it finish briefly so we capture its results
                if (metadataJob?.isActive == true) {
                    kotlinx.coroutines.withTimeoutOrNull(3000L) {
                        metadataJob?.join()
                    }
                }

                val currentOptions = _conversionOptions.value
                // If user didn't manually type a title/author and metadata probe missed/hadn't finished,
                // leave blank so convertWithSummary runs full-document extraction across all extracted pages.
                val effectiveOptions = currentOptions.copy(
                    title = if (userEditedTitle && currentOptions.title.isNotBlank()) {
                        currentOptions.title
                    } else if (com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle(currentOptions.title)) {
                        ""
                    } else {
                        currentOptions.title
                    },
                    author = if (userEditedAuthor && currentOptions.author.isNotBlank()) {
                        currentOptions.author
                    } else if (com.serma.pubify.converter.PdfMetadataExtractor.isGenericAuthor(currentOptions.author)) {
                        ""
                    } else {
                        currentOptions.author
                    }
                )

                val result = converter.convertWithSummary(uri, effectiveOptions) { progress ->
                    _conversionProgress.value = progress
                }

                result.onSuccess { output ->
                    val epubFile = output.epubFile
                    val resolvedTitle = output.resolvedTitle.ifBlank { currentOptions.title.ifBlank { "Untitled Book" } }
                    val resolvedAuthor = output.resolvedAuthor.ifBlank { currentOptions.author.ifBlank { "Unknown Author" } }
                    _conversionOptions.value = _conversionOptions.value.copy(
                        title = resolvedTitle,
                        author = resolvedAuthor
                    )
                    // Automatically save a copy to the device's app-named folder (Downloads/PDF to EPUB Converter/)
                    val appContext = getApplication<Application>()
                    val savedDeviceResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        com.serma.pubify.converter.EpubDeviceStorage.saveEpubToDeviceDownloads(
                            context = appContext,
                            sourceEpubFile = epubFile,
                            bookTitle = resolvedTitle
                        )
                    }

                    // Check if cover file exists in filesDir/covers/
                    val metadata = _pdfMetadata.value
                    val coversDir = File(appContext.filesDir, "covers")
                    val coverFile = coversDir.listFiles()?.maxByOrNull { it.lastModified() }

                    val convertedBook = ConvertedBook(
                        title = resolvedTitle,
                        author = resolvedAuthor,
                        originalFileName = metadata?.fileName ?: resolveDisplayName(uri),
                        epubFilePath = epubFile.absolutePath,
                        coverImagePath = coverFile?.absolutePath,
                        fileSizeBytes = epubFile.length(),
                        pageCount = metadata?.pageCount ?: 1,
                        chapterCount = output.chapterCount.coerceAtLeast(1),
                        convertedAtMillis = System.currentTimeMillis(),
                        deviceSavedPath = savedDeviceResult.displayPath
                    )

                    val bookId = repository.saveBook(convertedBook)
                    _lastConvertedBook.value = convertedBook.copy(id = bookId)
                    _isConverting.value = false
                }.onFailure { err ->
                    _isConverting.value = false
                    _cacheOperationMessage.value = "Conversion failed: ${err.localizedMessage ?: "Unknown error"}"
                }
            } catch (t: Throwable) {
                _isConverting.value = false
                _cacheOperationMessage.value = "Conversion failed: ${t.localizedMessage ?: "Unexpected error"}"
            }
        }
    }

    fun cancelConversion() {
        conversionJob?.cancel()
        _isConverting.value = false
        _conversionProgress.value = ConversionProgress(
            stage = ConversionStage.IDLE,
            message = "Conversion cancelled by user"
        )
    }

    fun resetConversion() {
        _selectedPdfUri.value = null
        _pdfMetadata.value = null
        _lastConvertedBook.value = null
        _conversionProgress.value = ConversionProgress()
        _isConverting.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: BookSortOrder) {
        _sortOrder.value = order
    }

    fun deleteBook(book: ConvertedBook) {
        viewModelScope.launch {
            if (_activeBookRecord.value?.id == book.id) {
                _activeReaderBook.value = null
                _activeBookRecord.value = null
            }
            repository.deleteBook(book)
        }
    }

    fun toggleFavorite(book: ConvertedBook) {
        viewModelScope.launch {
            repository.toggleFavorite(book.id, !book.isFavorite)
        }
    }

    fun setBookFilter(filter: BookFilter) {
        _bookFilter.value = filter
    }

    fun updateBookMetadata(book: ConvertedBook, newTitle: String, newAuthor: String) {
        viewModelScope.launch {
            val updated = book.copy(
                title = newTitle.trim().ifBlank { book.title },
                author = newAuthor.trim().ifBlank { book.author }
            )
            repository.updateBook(updated)
            if (_activeBookRecord.value?.id == book.id) {
                _activeBookRecord.value = updated
            }
        }
    }

    fun resetReadingProgress(book: ConvertedBook) {
        viewModelScope.launch {
            repository.updateReadingProgress(book.id, 0, 0)
            repository.toggleBookCompleted(book.id, false)
            if (_activeBookRecord.value?.id == book.id) {
                _currentChapterIndex.value = 0
                _activeBookRecord.value = _activeBookRecord.value?.copy(
                    lastReadChapterIndex = 0,
                    lastReadScrollOffset = 0,
                    isCompleted = false
                )
            }
        }
    }

    fun updateReadingProgress(book: ConvertedBook, chapterIndex: Int) {
        val total = book.chapterCount.coerceAtLeast(1)
        val validChapter = chapterIndex.coerceIn(0, total - 1)
        viewModelScope.launch {
            repository.updateReadingProgress(book.id, validChapter, 0)
            if (_activeBookRecord.value?.id == book.id) {
                _currentChapterIndex.value = validChapter
            }
        }
    }

    // Batch Conversion
    fun addBatchFiles(uris: List<Uri>) {
        val context = getApplication<Application>()
        val items = uris.map { uri ->
            var name = "document.pdf"
            var size = 0L
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) cursor.getString(nameIndex)?.let { name = it }
                        if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                    }
                }
            } catch (e: Exception) {
                name = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            }
            BatchItem(uri = uri, fileName = name, fileSizeBytes = size)
        }
        _batchQueue.value = _batchQueue.value + items
    }

    fun removeBatchItem(item: BatchItem) {
        if (_isBatchRunning.value) return
        _batchQueue.value = _batchQueue.value.filter { it != item }
    }

    fun removeBatchItemAt(index: Int) {
        if (_isBatchRunning.value) return
        val current = _batchQueue.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _batchQueue.value = current
        }
    }

    fun clearBatch() {
        cancelBatchConversion()
        _batchQueue.value = emptyList()
        _isBatchRunning.value = false
    }

    fun cancelBatchConversion() {
        batchJob?.cancel()
        batchJob = null
        _isBatchRunning.value = false
        val current = _batchQueue.value
        current.forEach { item ->
            if (item.status == "Converting") {
                item.status = "Cancelled"
            }
        }
        _batchQueue.value = ArrayList(current)
    }

    fun startBatchConversion() {
        val queue = _batchQueue.value.ifEmpty { return }
        if (_isBatchRunning.value) return
        _isBatchRunning.value = true

        batchJob = viewModelScope.launch {
            try {
                queue.forEachIndexed { index, item ->
                    // Skip items that are already successfully converted
                    if (item.status == "Success") return@forEachIndexed

                    _batchCurrentIndex.value = index
                    item.status = "Converting"
                    item.progress = 0f
                    item.error = null
                    _batchQueue.value = ArrayList(_batchQueue.value)

                    try {
                        val metadataResult = converter.extractMetadata(item.uri)
                        val meta = metadataResult.getOrNull()
                        val options = ConversionOptions(
                            title = meta?.title?.takeUnless { com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle(it) } ?: "",
                            author = meta?.author?.takeUnless { com.serma.pubify.converter.PdfMetadataExtractor.isGenericAuthor(it) } ?: "",
                            extractCover = true
                        )

                        val convertResult = converter.convertWithSummary(item.uri, options) { progress ->
                            item.progress = progress.percentage
                            _batchQueue.value = ArrayList(_batchQueue.value)
                        }

                        convertResult.onSuccess { output ->
                            val file = output.epubFile
                            val resolvedTitle = output.resolvedTitle.ifBlank { options.title.ifBlank { "Untitled Book" } }
                            val resolvedAuthor = output.resolvedAuthor.ifBlank { options.author.ifBlank { "Unknown Author" } }
                            val appContext = getApplication<Application>()
                            val savedDeviceResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                com.serma.pubify.converter.EpubDeviceStorage.saveEpubToDeviceDownloads(
                                    context = appContext,
                                    sourceEpubFile = file,
                                    bookTitle = resolvedTitle
                                )
                            }
                            item.status = "Success"
                            item.progress = 1.0f
                            val coversDir = File(appContext.filesDir, "covers")
                            val coverFile = coversDir.listFiles()?.maxByOrNull { it.lastModified() }
                            val book = ConvertedBook(
                                title = resolvedTitle,
                                author = resolvedAuthor,
                                originalFileName = item.fileName,
                                epubFilePath = file.absolutePath,
                                coverImagePath = coverFile?.absolutePath,
                                fileSizeBytes = file.length(),
                                pageCount = meta?.pageCount ?: 1,
                                chapterCount = output.chapterCount.coerceAtLeast(1),
                                deviceSavedPath = savedDeviceResult.displayPath
                            )
                            repository.saveBook(book)
                        }.onFailure { err ->
                            item.status = "Failed"
                            item.error = err.localizedMessage ?: "Conversion failed"
                        }
                    } catch (t: Throwable) {
                        if (t is kotlinx.coroutines.CancellationException) throw t
                        item.status = "Failed"
                        item.error = t.localizedMessage ?: "Unexpected error"
                    }
                    _batchQueue.value = ArrayList(_batchQueue.value)
                }
            } finally {
                _isBatchRunning.value = false
            }
        }
    }

    private var highlightsJob: Job? = null

    private fun loadHighlightsForBook(bookId: Long) {
        highlightsJob?.cancel()
        highlightsJob = viewModelScope.launch {
            repository.getHighlightsForBook(bookId).collect { list ->
                _bookHighlights.value = list
            }
        }
    }

    fun addHighlight(
        chapterIndex: Int,
        chapterTitle: String,
        paragraphIndex: Int,
        text: String,
        colorHex: Long = 0xFFFFEB3B,
        note: String = "",
        scope: String = "WORD"
    ) {
        val bookRecord = _activeBookRecord.value
        val bookId = bookRecord?.id ?: 0L
        val bookTitle = bookRecord?.title ?: _activeReaderBook.value?.title ?: "Book"
        viewModelScope.launch {
            val highlight = BookHighlight(
                bookId = bookId,
                bookTitle = bookTitle,
                chapterIndex = chapterIndex,
                chapterTitle = chapterTitle,
                paragraphIndex = paragraphIndex,
                text = text,
                colorHex = colorHex,
                note = note,
                scope = scope
            )
            val newId = repository.saveHighlight(highlight)
            if (bookId == 0L) {
                _bookHighlights.value = _bookHighlights.value + highlight.copy(id = newId)
            }
        }
    }

    fun removeHighlight(highlightId: Long) {
        viewModelScope.launch {
            repository.deleteHighlightById(highlightId)
            _bookHighlights.value = _bookHighlights.value.filter { it.id != highlightId }
        }
    }

    fun updateHighlightColor(
        highlight: BookHighlight,
        newColorHex: Long,
        newScope: String = highlight.scope,
        newText: String = highlight.text
    ) {
        viewModelScope.launch {
            val updated = highlight.copy(
                colorHex = newColorHex,
                scope = newScope,
                text = newText
            )
            repository.saveHighlight(updated)
            _bookHighlights.value = _bookHighlights.value.map { if (it.id == highlight.id) updated else it }
        }
    }

    fun saveHighlightNote(
        chapterIndex: Int,
        chapterTitle: String,
        paragraphIndex: Int,
        text: String,
        note: String,
        existingHighlight: BookHighlight? = null,
        scope: String = "WORD"
    ) {
        viewModelScope.launch {
            if (existingHighlight != null) {
                val updated = existingHighlight.copy(note = note)
                repository.saveHighlight(updated)
                _bookHighlights.value = _bookHighlights.value.map { if (it.id == existingHighlight.id) updated else it }
            } else {
                val bookRecord = _activeBookRecord.value
                val bookId = bookRecord?.id ?: 0L
                val bookTitle = bookRecord?.title ?: _activeReaderBook.value?.title ?: "Book"
                val highlight = BookHighlight(
                    bookId = bookId,
                    bookTitle = bookTitle,
                    chapterIndex = chapterIndex,
                    chapterTitle = chapterTitle,
                    paragraphIndex = paragraphIndex,
                    text = text,
                    note = note,
                    scope = scope
                )
                val newId = repository.saveHighlight(highlight)
                if (bookId == 0L) {
                    _bookHighlights.value = _bookHighlights.value + highlight.copy(id = newId)
                }
            }
        }
    }

    private var bookmarksJob: Job? = null

    private fun loadBookmarksForBook(bookId: Long) {
        bookmarksJob?.cancel()
        bookmarksJob = viewModelScope.launch {
            repository.getBookmarksForBook(bookId).collect { list ->
                _bookBookmarks.value = list
            }
        }
    }

    fun toggleBookmark(
        chapterIndex: Int,
        chapterTitle: String,
        pageNumber: Int,
        totalBookPages: Int,
        scrollOffsetFraction: Float,
        snippet: String = ""
    ) {
        val bookRecord = _activeBookRecord.value
        val bookId = bookRecord?.id ?: 0L
        val bookTitle = bookRecord?.title ?: _activeReaderBook.value?.title ?: "Book"
        val existing = _bookBookmarks.value.find { it.pageNumber == pageNumber }

        viewModelScope.launch {
            if (existing != null) {
                repository.deleteBookmarkById(existing.id)
                _bookBookmarks.value = _bookBookmarks.value.filter { it.id != existing.id }
            } else {
                val bookmark = BookBookmark(
                    bookId = bookId,
                    bookTitle = bookTitle,
                    chapterIndex = chapterIndex,
                    chapterTitle = chapterTitle,
                    pageNumber = pageNumber,
                    totalBookPages = totalBookPages,
                    scrollOffsetFraction = scrollOffsetFraction,
                    snippet = snippet
                )
                val newId = repository.saveBookmark(bookmark)
                if (bookId == 0L) {
                    _bookBookmarks.value = _bookBookmarks.value + bookmark.copy(id = newId)
                }
            }
        }
    }

    fun removeBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            repository.deleteBookmarkById(bookmarkId)
            _bookBookmarks.value = _bookBookmarks.value.filter { it.id != bookmarkId }
        }
    }

    fun toggleBookCompleted(bookId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleBookCompleted(bookId, isCompleted)
            if (_activeBookRecord.value?.id == bookId) {
                _activeBookRecord.value = _activeBookRecord.value?.copy(isCompleted = isCompleted)
            }
        }
    }

    fun toggleBookCompleted(book: ConvertedBook, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleBookCompleted(book.id, isCompleted)
            // If unmarking a book that was auto-completed by reaching its last chapter,
            // step its chapter index back so filters and stats reflect In Progress accurately.
            if (!isCompleted && book.chapterCount > 1 && book.lastReadChapterIndex >= book.chapterCount - 1) {
                val steppedBackChapter = (book.chapterCount - 2).coerceAtLeast(0)
                repository.updateReadingProgress(book.id, steppedBackChapter, 0)
                if (_activeBookRecord.value?.id == book.id) {
                    _currentChapterIndex.value = steppedBackChapter
                }
            }
            if (_activeBookRecord.value?.id == book.id) {
                _activeBookRecord.value = _activeBookRecord.value?.copy(isCompleted = isCompleted)
            }
        }
    }

    fun logReadingSession(
        bookId: Long,
        bookTitle: String,
        durationSeconds: Long,
        pagesRead: Int,
        startPage: Int = 0,
        endPage: Int = 0
    ) {
        if (durationSeconds < 2 && pagesRead <= 0) return
        viewModelScope.launch {
            val session = ReadingSession(
                bookId = bookId,
                bookTitle = bookTitle.ifBlank { "Reading Session" },
                durationSeconds = durationSeconds,
                pagesRead = maxOf(0, pagesRead),
                startPage = startPage,
                endPage = endPage,
                timestampMillis = System.currentTimeMillis()
            )
            repository.saveReadingSession(session)
        }
    }

    fun deleteReadingSession(id: Long) {
        viewModelScope.launch {
            repository.deleteReadingSessionById(id)
        }
    }

    fun clearAllReadingSessions() {
        viewModelScope.launch {
            repository.clearAllReadingSessions()
        }
    }

    fun resetReadingStats(alsoResetBookProgress: Boolean = false) {
        viewModelScope.launch {
            repository.clearAllReadingSessions()
            if (alsoResetBookProgress) {
                val books = repository.allBooks.first()
                for (book in books) {
                    if (book.isCompleted || book.lastReadChapterIndex > 0 || book.lastReadScrollOffset > 0) {
                        repository.toggleBookCompleted(book.id, false)
                        repository.updateReadingProgress(book.id, 0, 0)
                    }
                }
                _activeBookRecord.value = _activeBookRecord.value?.copy(
                    isCompleted = false,
                    lastReadChapterIndex = 0,
                    lastReadScrollOffset = 0
                )
            }
        }
    }

    fun seedDemoReadingSessions() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L
            val sampleSessions = listOf(
                ReadingSession(
                    bookId = 1L,
                    bookTitle = "The Art of Reading",
                    durationSeconds = 1800L, // 30 mins
                    pagesRead = 18,
                    timestampMillis = now - 4 * dayMillis
                ),
                ReadingSession(
                    bookId = 1L,
                    bookTitle = "The Art of Reading",
                    durationSeconds = 2400L, // 40 mins
                    pagesRead = 24,
                    timestampMillis = now - 3 * dayMillis
                ),
                ReadingSession(
                    bookId = 2L,
                    bookTitle = "Classic Literature",
                    durationSeconds = 1500L, // 25 mins
                    pagesRead = 15,
                    timestampMillis = now - 2 * dayMillis
                ),
                ReadingSession(
                    bookId = 2L,
                    bookTitle = "Classic Literature",
                    durationSeconds = 2700L, // 45 mins
                    pagesRead = 30,
                    timestampMillis = now - 1 * dayMillis
                ),
                ReadingSession(
                    bookId = 1L,
                    bookTitle = "The Art of Reading",
                    durationSeconds = 1200L, // 20 mins
                    pagesRead = 14,
                    timestampMillis = now
                )
            )
            for (s in sampleSessions) {
                repository.saveReadingSession(s)
            }
        }
    }

    // Reader functions
    fun openBookInReader(book: ConvertedBook) {
        _isLoadingReader.value = true
        _activeBookRecord.value = book
        _currentChapterIndex.value = book.lastReadChapterIndex
        loadHighlightsForBook(book.id)
        loadBookmarksForBook(book.id)

        viewModelScope.launch {
            val file = File(book.epubFilePath)
            if (!file.exists()) {
                _isLoadingReader.value = false
                _activeReaderBook.value = null
                _cacheOperationMessage.value = "Error: Book file is missing. It may have been cleared from the cache."
                return@launch
            }

            val result = epubParser.parse(file)
            _isLoadingReader.value = false
            result.onSuccess { parsed ->
                val (fileTitle, fileAuthor) = com.serma.pubify.converter.PdfMetadataExtractor.parseFromFileName(book.originalFileName)
                val healedTitle = if (com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle(book.title)) {
                    parsed.title.takeUnless { com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle(it) }
                        ?: fileTitle
                        ?: book.title
                } else book.title
                val healedAuthor = if (com.serma.pubify.converter.PdfMetadataExtractor.isGenericAuthor(book.author)) {
                    parsed.author.takeUnless { com.serma.pubify.converter.PdfMetadataExtractor.isGenericAuthor(it) }
                        ?: fileAuthor
                        ?: book.author
                } else book.author

                val healedBook = book.copy(
                    title = healedTitle,
                    author = healedAuthor,
                    chapterCount = parsed.chapters.size.coerceAtLeast(1)
                )
                val healedParsed = parsed.copy(title = healedTitle, author = healedAuthor)
                _activeReaderBook.value = healedParsed
                if (healedBook != book) {
                    _activeBookRecord.value = healedBook
                    repository.updateBook(healedBook)
                }
            }.onFailure { err ->
                _activeReaderBook.value = null
                _cacheOperationMessage.value = "Error opening book: ${err.localizedMessage}"
            }
        }
    }

    fun openEpubFromUri(uri: Uri) {
        _isLoadingReader.value = true
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver
                val destDir = File(context.filesDir, "epubs").apply { mkdirs() }

                var displayName = "Imported Book.epub"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        cursor.getString(nameIndex)?.let { displayName = it }
                    }
                }

                val destFile = File(destDir, "${System.currentTimeMillis()}_$displayName")
                contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val parseResult = epubParser.parse(destFile)
                _isLoadingReader.value = false
                parseResult.onSuccess { parsed ->
                    _activeReaderBook.value = parsed
                    _currentChapterIndex.value = 0

                    val coverPath = if (parsed.coverBytes != null) {
                        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
                        val cFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                        cFile.writeBytes(parsed.coverBytes)
                        cFile.absolutePath
                    } else null

                    val (fileTitle, fileAuthor) = com.serma.pubify.converter.PdfMetadataExtractor.parseFromFileName(displayName)
                    val resolvedTitle = parsed.title.takeUnless { com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle(it) }
                        ?: fileTitle
                        ?: com.serma.pubify.converter.PdfMetadataExtractor.cleanRawFileName(displayName).ifBlank { "Untitled Book" }
                    val resolvedAuthor = parsed.author.takeUnless { com.serma.pubify.converter.PdfMetadataExtractor.isGenericAuthor(it) }
                        ?: fileAuthor
                        ?: "Unknown Author"

                    val bookRecord = ConvertedBook(
                        title = resolvedTitle,
                        author = resolvedAuthor,
                        originalFileName = displayName,
                        epubFilePath = destFile.absolutePath,
                        coverImagePath = coverPath,
                        fileSizeBytes = destFile.length(),
                        pageCount = parsed.chapters.size,
                        chapterCount = parsed.chapters.size,
                        lastReadChapterIndex = 0
                    )
                    val id = repository.saveBook(bookRecord)
                    _activeBookRecord.value = bookRecord.copy(id = id)
                    loadHighlightsForBook(id)
                    loadBookmarksForBook(id)
                }.onFailure { err ->
                    _activeReaderBook.value = null
                }
            } catch (e: Exception) {
                _isLoadingReader.value = false
                _activeReaderBook.value = null
            }
        }
    }

    fun setChapter(index: Int) {
        val book = _activeReaderBook.value ?: return
        val validIndex = index.coerceIn(0, (book.chapters.size - 1).coerceAtLeast(0))
        val changed = _currentChapterIndex.value != validIndex
        _currentChapterIndex.value = validIndex

        _activeBookRecord.value?.let { record ->
            viewModelScope.launch {
                repository.updateReadingProgress(record.id, validIndex, 0)
            }
        }

        if (changed && ttsManager.state.value.isPlaying) {
            val newCh = book.chapters.getOrNull(validIndex)
            if (newCh != null) {
                ttsManager.startChapter(
                    chapterIndex = validIndex,
                    chapterTitle = newCh.title,
                    paragraphs = newCh.paragraphs,
                    startParagraphIndex = 0
                )
            } else {
                ttsManager.stop()
            }
        }
    }

    private fun onTtsChapterFinished(finishedIndex: Int) {
        val book = _activeReaderBook.value ?: return
        if (finishedIndex < book.chapters.size - 1) {
            val nextIdx = finishedIndex + 1
            setChapter(nextIdx)
            val nextChapter = book.chapters.getOrNull(nextIdx)
            if (nextChapter != null) {
                ttsManager.startChapter(
                    chapterIndex = nextIdx,
                    chapterTitle = nextChapter.title,
                    paragraphs = nextChapter.paragraphs,
                    startParagraphIndex = 0
                )
            }
        } else {
            ttsManager.stop()
        }
    }

    fun toggleTtsPlayPause() {
        val book = _activeReaderBook.value ?: return
        val chapter = book.chapters.getOrNull(_currentChapterIndex.value) ?: return
        ttsManager.togglePlayPause(
            currentChapterIndex = _currentChapterIndex.value,
            currentChapterTitle = chapter.title,
            paragraphs = chapter.paragraphs
        )
    }

    fun startTtsAtParagraph(chapterIndex: Int, chapterTitle: String, paragraphs: List<String>, paragraphIndex: Int) {
        ttsManager.startChapter(
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            paragraphs = paragraphs,
            startParagraphIndex = paragraphIndex
        )
    }

    fun pauseTts() = ttsManager.pause()
    fun resumeTts() = ttsManager.resume()
    fun stopTts() = ttsManager.stop()
    fun skipTtsNext() = ttsManager.skipNext()
    fun skipTtsPrevious() = ttsManager.skipPrevious()
    fun setTtsSpeechRate(rate: Float) = ttsManager.setSpeechRate(rate)
    fun setTtsPitch(pitch: Float) = ttsManager.setPitch(pitch)
    fun setTtsVisible(visible: Boolean) = ttsManager.setVisible(visible)
    fun closeTts() = ttsManager.closePlayer()

    fun updateReadingScrollOffset(offset: Int) {
        val record = _activeBookRecord.value ?: return
        val chapterIdx = _currentChapterIndex.value
        scrollPersistJob?.cancel()
        scrollPersistJob = viewModelScope.launch {
            kotlinx.coroutines.delay(650L)
            repository.updateReadingProgress(record.id, chapterIdx, offset)
        }
    }

    fun nextChapter() {
        val book = _activeReaderBook.value ?: return
        if (_currentChapterIndex.value < book.chapters.size - 1) {
            setChapter(_currentChapterIndex.value + 1)
        }
    }

    fun previousChapter() {
        if (_currentChapterIndex.value > 0) {
            setChapter(_currentChapterIndex.value - 1)
        }
    }

    fun toggleReaderDarkTheme() {
        _readerSettings.value = _readerSettings.value.copy(
            theme = if (_readerSettings.value.theme.isDark) ReaderTheme.LIGHT else ReaderTheme.DARK
        )
    }

    fun updateReaderSettings(updater: (ReaderSettings) -> ReaderSettings) {
        val prev = _readerSettings.value
        val next = updater(prev)
        _readerSettings.value = next
        if (next.readingMode != prev.readingMode ||
            next.pageTurnMode != prev.pageTurnMode ||
            next.textAlign != prev.textAlign ||
            next.showTapZonesOverlay != prev.showTapZonesOverlay
        ) {
            prefs.edit()
                .putString("reader_reading_mode", next.readingMode.name)
                .putString("reader_page_turn_mode", next.pageTurnMode.name)
                .putString("reader_text_align", next.textAlign.name)
                .putBoolean("reader_show_tap_zones", next.showTapZonesOverlay)
                .apply()
        }
    }

    fun closeReader() {
        ttsManager.stop()
        highlightsJob?.cancel()
        _bookHighlights.value = emptyList()
        _activeReaderBook.value = null
        _activeBookRecord.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.release()
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _cacheSizeBytes.value = calculateTotalCacheBytes()
        }
    }

    private fun calculateTotalCacheBytes(): Long {
        val context = getApplication<Application>()
        var total = 0L
        total += getFolderSize(context.cacheDir)
        total += getFolderSize(context.codeCacheDir)
        try {
            context.externalCacheDir?.let { total += getFolderSize(it) }
        } catch (_: Exception) {}
        return total
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (!file.isDirectory) return file.length()
        var size = 0L
        val children = file.listFiles() ?: return 0L
        for (child in children) {
            size += if (child.isDirectory) getFolderSize(child) else child.length()
        }
        return size
    }

    fun clearAppCache() {
        if (_isClearingCache.value) return
        _isClearingCache.value = true
        _cacheOperationMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val initialBytes = calculateTotalCacheBytes()

                // 1. Clear Coil image loader caches
                try {
                    coil.Coil.imageLoader(context).apply {
                        diskCache?.clear()
                        memoryCache?.clear()
                    }
                } catch (_: Exception) {}

                // 2. Delete cacheDir contents recursively
                deleteDirChildrenRecursively(context.cacheDir)

                // 3. Delete codeCacheDir contents recursively
                deleteDirChildrenRecursively(context.codeCacheDir)

                // 4. Delete externalCacheDir contents recursively
                try {
                    deleteDirChildrenRecursively(context.externalCacheDir)
                } catch (_: Exception) {}

                val remainingBytes = calculateTotalCacheBytes()
                _cacheSizeBytes.value = remainingBytes
                val freedBytes = maxOf(0L, initialBytes - remainingBytes)
                _lastCacheDelta.value = CacheClearDelta(
                    beforeBytes = initialBytes,
                    afterBytes = remainingBytes,
                    freedBytes = freedBytes
                )

                _isClearingCache.value = false
                _cacheOperationMessage.value = if (freedBytes > 0L) {
                    "Cache cleared: ${formatBytes(initialBytes)} → ${formatBytes(remainingBytes)}"
                } else {
                    "Cache is already clean (0 B)"
                }
            } catch (e: Exception) {
                _isClearingCache.value = false
                _cacheOperationMessage.value = "Error clearing cache: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    private fun deleteDirChildrenRecursively(dir: File?) {
        if (dir == null || !dir.exists()) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            try {
                if (child.isDirectory) {
                    child.deleteRecursively()
                } else {
                    child.delete()
                }
            } catch (_: Exception) {}
        }
    }
}
