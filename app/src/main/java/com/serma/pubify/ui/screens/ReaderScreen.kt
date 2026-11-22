package com.serma.pubify.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import com.serma.pubify.R
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.serma.pubify.data.model.ConvertedBook
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.ripple
import com.serma.pubify.reader.PageTurnMode
import com.serma.pubify.reader.ReaderChapter
import com.serma.pubify.data.model.BookBookmark
import com.serma.pubify.data.model.BookHighlight
import com.serma.pubify.reader.ReaderFontFamily
import com.serma.pubify.reader.ReaderPage
import com.serma.pubify.reader.ReaderPaginator
import com.serma.pubify.reader.ReaderReadingMode
import com.serma.pubify.reader.ReaderTextAlign
import com.serma.pubify.reader.ReaderTheme
import com.serma.pubify.ui.components.EpubLazyTextRenderer
import com.serma.pubify.ui.components.EpubTextRenderer
import com.serma.pubify.ui.components.MoonReaderQuickDock
import com.serma.pubify.ui.components.MoonReaderStatusFooter
import com.serma.pubify.ui.components.MoonTocHierarchicalList
import com.serma.pubify.ui.components.PaginatedPageRenderer
import com.serma.pubify.ui.components.ReadingWordInspectSheet
import com.serma.pubify.ui.components.CrystalBlue
import com.serma.pubify.ui.components.GlassCard
import com.serma.pubify.ui.components.LocalGlassPalette
import com.serma.pubify.ui.components.LuxuryGold
import com.serma.pubify.ui.components.TtsReaderControlsBar
import com.serma.pubify.ui.components.WordInspectState
import com.serma.pubify.ui.components.formatBytes
import com.serma.pubify.ui.components.luxuryBlueColor
import com.serma.pubify.ui.components.luxuryGoldBlueBorderBrush
import com.serma.pubify.ui.components.luxuryGoldColor
import com.serma.pubify.ui.theme.GreenSuccess
import com.serma.pubify.ui.viewmodel.ConverterViewModel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Highlight
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private class CoordsRef(var coords: LayoutCoordinates? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ConverterViewModel,
    onNavigateToLibrary: () -> Unit,
    onShareEpub: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeBook by viewModel.activeReaderBook.collectAsStateWithLifecycle()
    val activeBookRecord by viewModel.activeBookRecord.collectAsStateWithLifecycle()
    val currentChapterIndex by viewModel.currentChapterIndex.collectAsStateWithLifecycle()
    val settings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingReader.collectAsStateWithLifecycle()
    val libraryBooks by viewModel.libraryBooks.collectAsStateWithLifecycle()
    val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()
    val highlights by viewModel.bookHighlights.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookBookmarks.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.pauseTts()
        }
    }

    var showTocSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSwitchBookSheet by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var chapterSearchQuery by remember { mutableStateOf("") }
    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var showReadingSlider by remember { mutableStateOf(true) }
    var showBrightnessQuickBar by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var pendingScrollFraction by remember { mutableStateOf<Float?>(null) }
    var pendingParagraphJumpIndex by remember { mutableStateOf<Int?>(null) }
    var currentPaginatedPageIdx by remember { mutableIntStateOf(0) }
    var totalPaginatedPagesCount by remember { mutableIntStateOf(1) }
    val pagerJumpFlow = remember { kotlinx.coroutines.flow.MutableSharedFlow<Int>(extraBufferCapacity = 8) }
    var tocActiveTab by remember { mutableStateOf(0) }
    var inspectTarget by remember { mutableStateOf<WordInspectState?>(null) }

    // Master visibility for all reading menus (top bar & bottom bar)
    // Starts true when reader is opened and auto-hides after 5 seconds of inactivity
    val areBarsVisible by viewModel.isReaderBarsVisible.collectAsStateWithLifecycle()
    var userActivityTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-hide reading menus (top bar & bottom bar) after 5 seconds of inactivity
    LaunchedEffect(userActivityTime, areBarsVisible, isDraggingSlider, showTocSheet, showSettingsSheet, showSwitchBookSheet, showJumpToPageDialog, inspectTarget) {
        val anyModalOpen = showTocSheet || showSettingsSheet || showSwitchBookSheet || showJumpToPageDialog || inspectTarget != null
        if (areBarsVisible && !isDraggingSlider && !anyModalOpen) {
            delay(5000L)
            viewModel.setReaderBarsVisible(false)
        }
    }

    // Intercept system back press in Reading mode so it navigates back to the application menu
    BackHandler(enabled = true) {
        when {
            inspectTarget != null -> inspectTarget = null
            showTocSheet -> showTocSheet = false
            showSettingsSheet -> showSettingsSheet = false
            showSwitchBookSheet -> showSwitchBookSheet = false
            showJumpToPageDialog -> showJumpToPageDialog = false
            isSearchVisible -> {
                isSearchVisible = false
                chapterSearchQuery = ""
            }
            ttsState.isVisible -> viewModel.setTtsVisible(false)
            !areBarsVisible -> {
                viewModel.setReaderBarsVisible(true)
                userActivityTime = System.currentTimeMillis()
            }
            else -> onNavigateToLibrary()
        }
    }

    val scrollListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // File picker to open ANY EPUB file directly
    val epubPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.openEpubFromUri(it) }
    }

    // Reset scroll / page when chapter changes, or jump cleanly to pending target paragraph / fraction
    LaunchedEffect(currentChapterIndex) {
        currentPaginatedPageIdx = 0
        if (settings.readingMode == ReaderReadingMode.PAGINATED) {
            // Handled smoothly inside the Pager effect once chapterPages are measured
            return@LaunchedEffect
        }
        val fraction = pendingScrollFraction
        val paraTarget = pendingParagraphJumpIndex
        pendingScrollFraction = null
        pendingParagraphJumpIndex = null
        try {
            val totalParagraphs = (activeBook?.chapters?.getOrNull(currentChapterIndex)?.paragraphs?.size ?: 0)
            val targetItem = when {
                paraTarget != null -> (paraTarget + 1).coerceIn(0, totalParagraphs)
                fraction != null && fraction > 0f -> (fraction * totalParagraphs).roundToInt().coerceIn(0, totalParagraphs)
                else -> 0
            }
            scrollListState.scrollToItem(targetItem)
        } catch (_: Throwable) {
            // Non-fatal if cancelled or interrupted
        }
    }

    // Observe scroll item index via snapshotFlow (never triggers per-pixel recomposition or DB spam)
    LaunchedEffect(scrollListState) {
        snapshotFlow { scrollListState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { itemIdx ->
                if (itemIdx >= 0) {
                    viewModel.updateReadingScrollOffset(itemIdx)
                }
            }
    }

    // Loading State
    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("reader_loading_spinner")
                )
                Text(
                    text = "Opening & Rendering EPUB...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        return
    }

    // Empty / Shelf State: When no book is currently loaded
    if (activeBook == null || activeBook?.chapters.isNullOrEmpty()) {
        ReaderShelfView(
            convertedBooks = libraryBooks,
            onOpenBook = { book -> viewModel.openBookInReader(book) },
            onPickEpubFile = {
                epubPickerLauncher.launch(
                    arrayOf("application/epub+zip", "application/octet-stream", "*/*")
                )
            },
            onNavigateToLibrary = onNavigateToLibrary,
            modifier = modifier
        )
        return
    }

    val book = activeBook!!
    val chapters = book.chapters
    val currentChapter = chapters.getOrNull(currentChapterIndex) ?: chapters.first()

    // Auto-scroll when TTS reads aloud across paragraphs
    LaunchedEffect(ttsState.paragraphIndex, ttsState.isPlaying) {
        if (ttsState.isPlaying && ttsState.chapterIndex == currentChapterIndex && currentChapter.paragraphs.isNotEmpty() && !scrollListState.isScrollInProgress) {
            val targetIdx = (ttsState.paragraphIndex + 1).coerceIn(0, currentChapter.paragraphs.size)
            try {
                scrollListState.animateScrollToItem(targetIdx)
            } catch (_: Throwable) {}
        }
    }

    // Estimated Page calculations (approx. 250 words per page)
    val pagesPerChapter = remember(chapters) {
        chapters.map { ch -> maxOf(1, (ch.wordCount / 250).coerceAtLeast(1)) }
    }
    val totalBookPages = remember(pagesPerChapter) {
        maxOf(1, pagesPerChapter.sum())
    }

    // Reading Progress Calculations (supports both Paginated Flip and Smooth Virtualized Scroll modes)
    val isPaginatedMode = settings.readingMode == ReaderReadingMode.PAGINATED
    val chapterScrollProgress by remember(isPaginatedMode, currentPaginatedPageIdx, totalPaginatedPagesCount, currentChapter.paragraphs.size) {
        derivedStateOf {
            if (isPaginatedMode) {
                if (totalPaginatedPagesCount > 1) {
                    (currentPaginatedPageIdx.toFloat() / (totalPaginatedPagesCount - 1).toFloat()).coerceIn(0f, 1f)
                } else 0f
            } else {
                val layoutInfo = scrollListState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems <= 1) {
                    0f
                } else if (!scrollListState.canScrollForward && scrollListState.firstVisibleItemIndex > 0) {
                    1f
                } else {
                    val visibleCount = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                    val maxFirstIdx = (totalItems - visibleCount).coerceAtLeast(1)
                    (scrollListState.firstVisibleItemIndex.toFloat() / maxFirstIdx.toFloat()).coerceIn(0f, 1f)
                }
            }
        }
    }

    // Continuous and accurate whole-book progress fraction (0f..1f)
    val wholeBookProgressFraction by remember(currentChapterIndex, pagesPerChapter, totalBookPages) {
        derivedStateOf {
            val safeTotal = maxOf(1, totalBookPages)
            val prevPages = pagesPerChapter.take(currentChapterIndex).sum()
            val currentChPages = maxOf(1, pagesPerChapter.getOrElse(currentChapterIndex) { 1 })
            val progressInCh = chapterScrollProgress.let { if (it.isNaN() || it.isInfinite()) 0f else it.coerceIn(0f, 1f) }
            val continuousPage = prevPages + (progressInCh * currentChPages)
            (continuousPage / safeTotal.toFloat()).let {
                if (it.isNaN() || it.isInfinite()) 0f else it.coerceIn(0f, 1f)
            }
        }
    }

    val overallBookProgress by remember {
        derivedStateOf { wholeBookProgressFraction }
    }

    val estimatedPagesInCurrentChapter = remember(currentChapter.wordCount, isPaginatedMode, totalPaginatedPagesCount) {
        if (isPaginatedMode) maxOf(1, totalPaginatedPagesCount)
        else maxOf(1, (currentChapter.wordCount / 250).coerceAtLeast(1))
    }
    val currentChapterPage = remember(chapterScrollProgress, estimatedPagesInCurrentChapter, isPaginatedMode, currentPaginatedPageIdx) {
        if (isPaginatedMode) {
            (currentPaginatedPageIdx + 1).coerceIn(1, estimatedPagesInCurrentChapter)
        } else {
            val safeEstPages = maxOf(1, estimatedPagesInCurrentChapter)
            val progressInCh = chapterScrollProgress.let { if (it.isNaN() || it.isInfinite()) 0f else it.coerceIn(0f, 1f) }
            ((progressInCh * (safeEstPages - 1)).toInt() + 1).coerceIn(1, safeEstPages)
        }
    }
    val currentGlobalPage = remember(currentChapterIndex, currentChapterPage, pagesPerChapter, totalBookPages) {
        val safeTotal = maxOf(1, totalBookPages)
        val prevPages = pagesPerChapter.take(currentChapterIndex).sum()
        if (isPaginatedMode) {
            val estChPages = maxOf(1, pagesPerChapter.getOrElse(currentChapterIndex) { 1 })
            val mappedOffset = (chapterScrollProgress * (estChPages - 1)).roundToInt() + 1
            (prevPages + mappedOffset).coerceIn(1, safeTotal)
        } else {
            (prevPages + currentChapterPage).coerceIn(1, safeTotal)
        }
    }

    val latestGlobalPage by androidx.compose.runtime.rememberUpdatedState(currentGlobalPage)
    DisposableEffect(activeBookRecord?.id, book.title) {
        val sessionStartTimeMs = System.currentTimeMillis()
        val sessionStartPage = currentGlobalPage
        val bookId = activeBookRecord?.id ?: 0L
        val bookTitle = activeBookRecord?.title ?: book.title
        onDispose {
            val elapsedSeconds = (System.currentTimeMillis() - sessionStartTimeMs) / 1000L
            val endPage = latestGlobalPage
            val pagesDelta = kotlin.math.abs(endPage - sessionStartPage)
            val pagesRead = if (pagesDelta > 0) pagesDelta else if (elapsedSeconds >= 30L) 1 else 0
            if (elapsedSeconds >= 5L) {
                viewModel.logReadingSession(
                    bookId = bookId,
                    bookTitle = bookTitle,
                    durationSeconds = elapsedSeconds,
                    pagesRead = pagesRead,
                    startPage = sessionStartPage,
                    endPage = endPage
                )
            }
        }
    }

    val isCurrentPageBookmarked = remember(bookmarks, currentGlobalPage) {
        bookmarks.any { it.pageNumber == currentGlobalPage }
    }

    val toggleCurrentPageBookmark = remember(currentChapterIndex, currentChapter, currentGlobalPage, totalBookPages, chapterScrollProgress) {
        {
            val firstPara = currentChapter.paragraphs
                .firstOrNull { it.isNotBlank() && !it.trimStart().startsWith("[[IMG:") }
                ?.removePrefix("[[CENTER]]")
                ?.replace(Regex("^#{1,4}\\s+"), "")
                ?.take(120) ?: ""
            viewModel.toggleBookmark(
                chapterIndex = currentChapterIndex,
                chapterTitle = currentChapter.title.ifBlank { "Chapter ${currentChapterIndex + 1}" },
                pageNumber = currentGlobalPage,
                totalBookPages = totalBookPages,
                scrollOffsetFraction = chapterScrollProgress,
                snippet = firstPara
            )
        }
    }

    val jumpToWholeBookFraction: (Float) -> Unit = remember(pagesPerChapter, totalBookPages, currentChapterIndex, chapters.size, isPaginatedMode, totalPaginatedPagesCount) {
        { targetFraction: Float ->
            try {
                val safeFraction = if (targetFraction.isNaN() || targetFraction.isInfinite()) 0f else targetFraction.coerceIn(0f, 1f)
                val safeTotalPages = maxOf(1, totalBookPages)
                val targetContinuousPage = safeFraction * safeTotalPages
                var accumulatedPages = 0
                var targetChapterIdx = 0
                var fractionInTargetChapter = 0f

                for ((idx, chPages) in pagesPerChapter.withIndex()) {
                    val nextAccumulated = accumulatedPages + chPages
                    if (targetContinuousPage <= nextAccumulated || idx == pagesPerChapter.size - 1) {
                        targetChapterIdx = idx
                        val safeChPages = maxOf(1, chPages)
                        val pageOffsetInChapter = (targetContinuousPage - accumulatedPages).coerceIn(0f, safeChPages.toFloat())
                        fractionInTargetChapter = (pageOffsetInChapter / safeChPages.toFloat()).coerceIn(0f, 1f)
                        break
                    }
                    accumulatedPages = nextAccumulated
                }

                val validTargetChapter = targetChapterIdx.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
                if (validTargetChapter != currentChapterIndex) {
                    pendingScrollFraction = fractionInTargetChapter
                    viewModel.setChapter(validTargetChapter)
                } else if (isPaginatedMode) {
                    val lastIdx = (totalPaginatedPagesCount - 1).coerceAtLeast(0)
                    val targetIdx = (fractionInTargetChapter * lastIdx).roundToInt().coerceIn(0, lastIdx)
                    currentPaginatedPageIdx = targetIdx
                    pagerJumpFlow.tryEmit(targetIdx)
                } else {
                    coroutineScope.launch {
                        try {
                            val maxItem = currentChapter.paragraphs.size.coerceAtLeast(0)
                            val targetItem = (fractionInTargetChapter * maxItem).roundToInt().coerceIn(0, maxItem)
                            scrollListState.animateScrollToItem(targetItem)
                        } catch (_: Throwable) {
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    // Direct bridge to active HorizontalPager so tap/scroll/volume turns always read live pagerState.currentPage
    val paginatedTurnBridge = remember {
        object {
            var turnBy: ((Int) -> Unit)? = null
        }
    }

    // Page turning actions (used by screen taps, scroll gestures, and volume buttons)
    fun turnPageNext() {
        if (isPaginatedMode) {
            val directTurn = paginatedTurnBridge.turnBy
            if (directTurn != null) {
                directTurn(+1)
                return
            }
            if (currentPaginatedPageIdx < totalPaginatedPagesCount - 1) {
                val nextPage = currentPaginatedPageIdx + 1
                currentPaginatedPageIdx = nextPage
                pagerJumpFlow.tryEmit(nextPage)
            } else if (currentChapterIndex < chapters.size - 1) {
                pendingScrollFraction = 0f
                viewModel.nextChapter()
            }
            return
        }
        coroutineScope.launch {
            try {
                val viewportHeight = scrollListState.layoutInfo.viewportSize.height.takeIf { it > 0 } ?: 800
                val scrollDelta = viewportHeight * 0.88f
                if (scrollListState.canScrollForward) {
                    scrollListState.animateScrollBy(scrollDelta)
                } else if (currentChapterIndex < chapters.size - 1) {
                    pendingScrollFraction = 0f
                    viewModel.nextChapter()
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun turnPagePrevious() {
        if (isPaginatedMode) {
            val directTurn = paginatedTurnBridge.turnBy
            if (directTurn != null) {
                directTurn(-1)
                return
            }
            if (currentPaginatedPageIdx > 0) {
                val prevPage = currentPaginatedPageIdx - 1
                currentPaginatedPageIdx = prevPage
                pagerJumpFlow.tryEmit(prevPage)
            } else if (currentChapterIndex > 0) {
                pendingScrollFraction = 1f
                viewModel.previousChapter()
            }
            return
        }
        coroutineScope.launch {
            try {
                val viewportHeight = scrollListState.layoutInfo.viewportSize.height.takeIf { it > 0 } ?: 800
                val scrollDelta = viewportHeight * 0.88f
                if (scrollListState.canScrollBackward) {
                    scrollListState.animateScrollBy(-scrollDelta)
                } else if (currentChapterIndex > 0) {
                    pendingScrollFraction = 1f
                    viewModel.previousChapter()
                }
            } catch (_: Throwable) {
            }
        }
    }

    // Listen to hardware volume button clicks using latest closures
    val latestTurnPageNext by androidx.compose.runtime.rememberUpdatedState(::turnPageNext)
    val latestTurnPagePrevious by androidx.compose.runtime.rememberUpdatedState(::turnPagePrevious)
    LaunchedEffect(Unit) {
        viewModel.volumeKeyEvent.collect { keyCode ->
            if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
                latestTurnPageNext()
            } else if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
                latestTurnPagePrevious()
            }
        }
    }

    // Jump to Page / Section Dialog
    if (showJumpToPageDialog) {
        JumpToPageDialog(
            currentGlobalPage = currentGlobalPage,
            totalBookPages = totalBookPages,
            currentChapterIndex = currentChapterIndex,
            totalChapters = chapters.size,
            currentChapterTitle = currentChapter.title,
            currentSectionProgress = chapterScrollProgress,
            pagesPerChapter = pagesPerChapter,
            chapters = chapters,
            theme = settings.theme,
            onDismiss = { showJumpToPageDialog = false },
            onJumpToBookPage = { targetPage ->
                showJumpToPageDialog = false
                val clampedPage = targetPage.coerceIn(1, totalBookPages)
                var accumulated = 0
                var targetCh = 0
                var targetFraction = 0f
                for ((idx, pCount) in pagesPerChapter.withIndex()) {
                    if (clampedPage <= accumulated + pCount || idx == pagesPerChapter.size - 1) {
                        targetCh = idx
                        val pInCh = (clampedPage - accumulated).coerceIn(1, pCount)
                        targetFraction = if (pCount > 1) {
                            (pInCh - 1).toFloat() / (pCount - 1).toFloat()
                        } else 0f
                        break
                    }
                    accumulated += pCount
                }
                val validTargetCh = targetCh.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
                if (validTargetCh != currentChapterIndex) {
                    pendingScrollFraction = targetFraction
                    viewModel.setChapter(validTargetCh)
                } else if (isPaginatedMode) {
                    val lastIdx = (totalPaginatedPagesCount - 1).coerceAtLeast(0)
                    val targetIdx = (targetFraction * lastIdx).roundToInt().coerceIn(0, lastIdx)
                    currentPaginatedPageIdx = targetIdx
                    pagerJumpFlow.tryEmit(targetIdx)
                } else {
                    coroutineScope.launch {
                        try {
                            val maxItem = currentChapter.paragraphs.size.coerceAtLeast(0)
                            scrollListState.animateScrollToItem((targetFraction * maxItem).roundToInt().coerceIn(0, maxItem))
                        } catch (_: Throwable) {}
                    }
                }
            },
            onJumpToChapterSection = { targetCh, fraction ->
                showJumpToPageDialog = false
                val validTargetCh = targetCh.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
                if (validTargetCh != currentChapterIndex) {
                    pendingScrollFraction = fraction
                    viewModel.setChapter(validTargetCh)
                } else if (isPaginatedMode) {
                    val lastIdx = (totalPaginatedPagesCount - 1).coerceAtLeast(0)
                    val targetIdx = (fraction * lastIdx).roundToInt().coerceIn(0, lastIdx)
                    currentPaginatedPageIdx = targetIdx
                    pagerJumpFlow.tryEmit(targetIdx)
                } else {
                    coroutineScope.launch {
                        try {
                            val maxItem = currentChapter.paragraphs.size.coerceAtLeast(0)
                            scrollListState.animateScrollToItem((fraction * maxItem).roundToInt().coerceIn(0, maxItem))
                        } catch (_: Throwable) {}
                    }
                }
            }
        )
    }

    // Table of Contents Sheet
    if (showTocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Contents",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        val totalTopicsCount = remember(chapters) { chapters.sumOf { it.subHeadings.size } }
                        Text(
                            text = buildString {
                                append("${chapters.size} Chapters")
                                if (totalTopicsCount > 0) append(" • $totalTopicsCount Topics")
                                append(" • ${(overallBookProgress * 100).toInt()}%")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Ch. ${currentChapterIndex + 1}/${chapters.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar inside TOC sheet
                LinearProgressIndicator(
                    progress = { overallBookProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher: Contents vs Bookmarks vs Highlights
                TabRow(
                    selectedTabIndex = tocActiveTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = tocActiveTab == 0,
                        onClick = { tocActiveTab = 0 },
                        text = { Text("Contents") },
                        modifier = Modifier.testTag("toc_tab_chapters")
                    )
                    Tab(
                        selected = tocActiveTab == 1,
                        onClick = { tocActiveTab = 1 },
                        text = { Text("Bookmarks (${bookmarks.size})") },
                        modifier = Modifier.testTag("toc_tab_bookmarks")
                    )
                    Tab(
                        selected = tocActiveTab == 2,
                        onClick = { tocActiveTab = 2 },
                        text = { Text("Highlights (${highlights.size})") },
                        modifier = Modifier.testTag("toc_tab_highlights")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (tocActiveTab) {
                    0 -> {
                        MoonTocHierarchicalList(
                            chapters = chapters,
                            currentChapterIndex = currentChapterIndex,
                            onSelectChapter = { index ->
                                pendingParagraphJumpIndex = null
                                pendingScrollFraction = 0f
                                viewModel.setChapter(index)
                                showTocSheet = false
                            },
                            onSelectSubHeading = { chIdx, paraIdx ->
                                if (chIdx != currentChapterIndex) {
                                    pendingParagraphJumpIndex = paraIdx
                                    viewModel.setChapter(chIdx)
                                } else {
                                    pendingParagraphJumpIndex = paraIdx
                                }
                                showTocSheet = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        )
                    }
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Quick Bookmark current page action card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        toggleCurrentPageBookmark()
                                    }
                                    .testTag("toc_bookmark_current_page_button"),
                                color = if (isCurrentPageBookmarked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isCurrentPageBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = if (isCurrentPageBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text(
                                                text = if (isCurrentPageBookmarked) "Page $currentGlobalPage is Bookmarked" else "Bookmark Current Page ($currentGlobalPage)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isCurrentPageBookmarked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isCurrentPageBookmarked) "Tap to remove bookmark" else "Tap to bookmark for quick return",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isCurrentPageBookmarked) "Remove" else "+ Add",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrentPageBookmarked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (bookmarks.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(42.dp)
                                        )
                                        Text(
                                            text = "No Bookmarks Yet",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap the bookmark icon in the reading toolbar or the button above to bookmark any page.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(bookmarks) { bm ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setChapter(bm.chapterIndex)
                                                    pendingScrollFraction = bm.scrollOffsetFraction
                                                    showTocSheet = false
                                                }
                                                .testTag("bookmark_item_${bm.pageNumber}"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Bookmark,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                text = "p. ${bm.pageNumber}",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = bm.chapterTitle.ifBlank { "Chapter ${bm.chapterIndex + 1}" },
                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (bm.snippet.isNotBlank()) {
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = bm.snippet,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Page ${bm.pageNumber} of ${bm.totalBookPages.coerceAtLeast(1)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { viewModel.removeBookmark(bm.id) },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .testTag("delete_bookmark_${bm.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete bookmark",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        if (highlights.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Highlight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "No Highlights Yet",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Long-press any word or line while reading to highlight passages.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(380.dp)
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(highlights) { hl ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setChapter(hl.chapterIndex)
                                                showTocSheet = false
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(hl.colorHex).copy(alpha = 0.18f)
                                        ),
                                        border = BorderStroke(1.dp, Color(hl.colorHex).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(hl.colorHex))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = hl.chapterTitle.ifBlank { "Chapter ${hl.chapterIndex + 1}" },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "“${hl.text}”",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                    ),
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeHighlight(hl.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove highlight",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Switch Book Bottom Sheet
    if (showSwitchBookSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSwitchBookSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Switch Converted Book",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Select any converted EPUB to read immediately",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    items(libraryBooks) { b ->
                        val isCurrent = b.id == activeBookRecord?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.openBookInReader(b)
                                    showSwitchBookSheet = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = b.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${b.author} • ${b.chapterCount} chapters",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isCurrent) {
                                    Text(
                                        text = "Reading",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val resolveReaderFont: (ReaderFontFamily) -> FontFamily = remember {
        { fam ->
            when (fam) {
                ReaderFontFamily.LITERATA -> FontFamily(Font(R.font.literata))
                ReaderFontFamily.EB_GARAMOND -> FontFamily(Font(R.font.eb_garamond))
                ReaderFontFamily.LORA -> FontFamily(Font(R.font.lora))
                ReaderFontFamily.INTER -> FontFamily(Font(R.font.inter))
                ReaderFontFamily.SERIF -> FontFamily.Serif
                ReaderFontFamily.SANS_SERIF -> FontFamily.SansSerif
                ReaderFontFamily.MONOSPACE -> FontFamily.Monospace
            }
        }
    }

    val resolveTextAlign: (ReaderTextAlign) -> TextAlign = remember {
        { align ->
            when (align) {
                ReaderTextAlign.JUSTIFY -> TextAlign.Justify
                ReaderTextAlign.LEFT -> TextAlign.Start
                ReaderTextAlign.CENTER -> TextAlign.Center
                ReaderTextAlign.RIGHT -> TextAlign.End
            }
        }
    }

    // Appearance & Typography Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Reader Settings",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Fonts, line spacing, text alignment & themes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSettingsSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close settings")
                    }
                }

                // 1. LIVE TYPOGRAPHY PREVIEW CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reader_settings_live_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (settings.theme == ReaderTheme.LIQUID_GLASS) Color(0xFF0A101D) else settings.theme.backgroundColor
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (settings.theme == ReaderTheme.LIQUID_GLASS) Color(0xFF00E5FF).copy(alpha = 0.5f) else settings.theme.accentColor.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE PREVIEW",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = if (settings.theme == ReaderTheme.LIQUID_GLASS) Color(0xFF00E5FF) else settings.theme.accentColor
                            )
                            Text(
                                text = "${settings.fontFamily.displayName} • ${String.format(java.util.Locale.US, "%.2f", settings.lineSpacingMultiplier)}x • ${settings.textAlign.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = settings.theme.textColor.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Chapter 1: The Art of Reading",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = resolveReaderFont(settings.fontFamily),
                                fontWeight = FontWeight.Bold,
                                fontSize = (settings.fontSizeSp * 1.12f).sp
                            ),
                            color = if (settings.theme == ReaderTheme.LIQUID_GLASS) Color(0xFF00E5FF) else settings.theme.accentColor,
                            textAlign = resolveTextAlign(settings.textAlign),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Reading is an intimate encounter with thoughts woven through language. When typography breathes with comfortable line spacing and balanced alignment, every sentence flows effortlessly.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = resolveReaderFont(settings.fontFamily),
                                fontSize = settings.fontSizeSp.sp,
                                lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp
                            ),
                            color = settings.theme.textColor,
                            textAlign = resolveTextAlign(settings.textAlign),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 2. FONT FAMILY SELECTOR
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Typeface & Font",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = settings.fontFamily.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderFontFamily.values().forEach { fam ->
                            val isSelected = settings.fontFamily == fam
                            val fontTypeface = resolveReaderFont(fam)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.updateReaderSettings { it.copy(fontFamily = fam) }
                                    }
                                    .testTag("reader_font_${fam.name.lowercase()}"),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "Aa",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontFamily = fontTypeface,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = fam.displayName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = fontTypeface,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(100.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                ) {
                                                    Text(
                                                        text = fam.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (fam.description.isNotBlank()) {
                                                Text(
                                                    text = fam.description,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected font",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 3. LINE SPACING ADJUSTER
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatLineSpacing,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Line Spacing",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.2f", settings.lineSpacingMultiplier)}x",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            1.35f to "Compact",
                            1.65f to "Normal",
                            1.95f to "Relaxed",
                            2.25f to "Spacious"
                        ).forEach { (preset, label) ->
                            val isSelected = kotlin.math.abs(settings.lineSpacingMultiplier - preset) < 0.08f
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.updateReaderSettings { it.copy(lineSpacingMultiplier = preset) }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("line_spacing_${label.lowercase()}")
                            )
                        }
                    }

                    // Stepper slider row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val next = (settings.lineSpacingMultiplier - 0.1f).coerceIn(1.2f, 2.4f)
                                viewModel.updateReaderSettings { it.copy(lineSpacingMultiplier = next) }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .testTag("line_spacing_decrease_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease line spacing",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Slider(
                            value = settings.lineSpacingMultiplier,
                            onValueChange = { mult ->
                                viewModel.updateReaderSettings { it.copy(lineSpacingMultiplier = mult) }
                            },
                            valueRange = 1.2f..2.4f,
                            steps = 11,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("line_spacing_slider")
                        )

                        IconButton(
                            onClick = {
                                val next = (settings.lineSpacingMultiplier + 0.1f).coerceIn(1.2f, 2.4f)
                                viewModel.updateReaderSettings { it.copy(lineSpacingMultiplier = next) }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .testTag("line_spacing_increase_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase line spacing",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 4. TEXT ALIGNMENT
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Text Alignment",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = settings.textAlign.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(ReaderTextAlign.LEFT, "Left", Icons.AutoMirrored.Filled.FormatAlignLeft),
                            Triple(ReaderTextAlign.JUSTIFY, "Justify", Icons.Default.FormatAlignJustify),
                            Triple(ReaderTextAlign.CENTER, "Center", Icons.Default.FormatAlignCenter),
                            Triple(ReaderTextAlign.RIGHT, "Right", Icons.AutoMirrored.Filled.FormatAlignRight)
                        ).forEach { (align, label, icon) ->
                            val isSelected = settings.textAlign == align
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.updateReaderSettings { it.copy(textAlign = align) }
                                    }
                                    .testTag("text_align_${align.name.lowercase()}"),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 5. FONT SIZE STEPPER & SLIDER
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Font Size",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${settings.fontSizeSp.toInt()} sp",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val next = (settings.fontSizeSp - 1f).coerceIn(13f, 28f)
                                viewModel.updateReaderSettings { it.copy(fontSizeSp = next) }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .testTag("font_size_decrease_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease font size",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Slider(
                            value = settings.fontSizeSp,
                            onValueChange = { size ->
                                viewModel.updateReaderSettings { it.copy(fontSizeSp = size) }
                            },
                            valueRange = 13f..28f,
                            steps = 14,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_font_size_slider")
                        )

                        IconButton(
                            onClick = {
                                val next = (settings.fontSizeSp + 1f).coerceIn(13f, 28f)
                                viewModel.updateReaderSettings { it.copy(fontSizeSp = next) }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .testTag("font_size_increase_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase font size",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 6. READING MODE & BRIGHTNESS
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Reading Mode (Moon+ Engine)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReaderReadingMode.values().forEach { rMode ->
                            val selected = settings.readingMode == rMode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.updateReaderSettings { it.copy(readingMode = rMode) }
                                    }
                                    .testTag("reading_mode_${rMode.name.lowercase()}"),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (rMode == ReaderReadingMode.PAGINATED) Icons.Default.AutoStories else Icons.Default.ViewDay,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = rMode.displayName,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = "Screen brightness",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Slider(
                            value = settings.brightnessLevel.coerceIn(0.2f, 1.0f),
                            onValueChange = { b ->
                                viewModel.updateReaderSettings { it.copy(brightnessLevel = b.coerceIn(0.2f, 1.0f)) }
                            },
                            valueRange = 0.2f..1.0f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_brightness_slider")
                        )
                        Text(
                            text = "${(settings.brightnessLevel * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 7. PAGE THEME & BACKGROUNDS
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paper & Night Themes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    ReaderTheme.values().toList().chunked(4).forEach { themeRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themeRow.forEach { th ->
                                val isSelected = settings.theme == th
                                val isGlass = th == ReaderTheme.LIQUID_GLASS
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.updateReaderSettings { it.copy(theme = th) }
                                        }
                                        .testTag("reader_theme_${th.name.lowercase()}"),
                                    color = th.backgroundColor,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            if (isGlass) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
                                        } else {
                                            if (isGlass) Color(0x6000E5FF) else Color.Gray.copy(alpha = 0.4f)
                                        }
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                        Text(
                                            text = th.displayName,
                                            color = if (isGlass) Color(0xFF00E5FF) else th.textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Page Turn Controls Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page Turning Method",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = settings.pageTurnMode.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageTurnMode.values().forEach { mode ->
                            val isSelected = settings.pageTurnMode == mode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.updateReaderSettings { it.copy(pageTurnMode = mode) }
                                    }
                                    .testTag("page_turn_mode_${mode.name.lowercase()}"),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            PageTurnMode.TAP_EDGES -> Icons.Default.TouchApp
                                            PageTurnMode.VOLUME_KEYS -> Icons.Default.VolumeUp
                                            PageTurnMode.TAP_AND_VOLUME -> Icons.Default.Swipe
                                            PageTurnMode.SCROLL_ONLY -> Icons.Default.MenuBook
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.updateReaderSettings { it.copy(pageTurnMode = mode) } }
                                    )
                                }
                            }
                        }
                    }

                    // Toggle for tap zone hints
                    if (settings.pageTurnMode == PageTurnMode.TAP_EDGES || settings.pageTurnMode == PageTurnMode.TAP_AND_VOLUME) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.updateReaderSettings { it.copy(showTapZonesOverlay = !it.showTapZonesOverlay) }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Show Edge Tap Hints",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Subtle arrows on screen margins indicating page turn zones",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.showTapZonesOverlay,
                                    onCheckedChange = { checked ->
                                        viewModel.updateReaderSettings { it.copy(showTapZonesOverlay = checked) }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // MAIN DEDICATED READER SCREEN — Root Box keeps reading viewport 100% stable when HUD bars animate
    val isGlassTheme = settings.theme == ReaderTheme.LIQUID_GLASS
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isGlassTheme) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF070C15),
                                Color(0xFF0A1322),
                                Color(0xFF0D192C),
                                Color(0xFF070C15)
                            )
                        )
                    )
                } else {
                    Modifier.background(settings.theme.backgroundColor)
                }
            )
            .testTag("reader_screen_main")
    ) {
        // Floating Top App Bar Overlay (Auto-hides smoothly without resizing reading canvas)
        AnimatedVisibility(
            visible = areBarsVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(3f)
        ) {
            Surface(
                color = if (isGlassTheme) Color(0xDD0A1526) else settings.theme.backgroundColor.copy(alpha = 0.94f),
                shadowElevation = if (isGlassTheme) 0.dp else 4.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isGlassTheme) Color(0x3800E5FF) else settings.theme.textColor.copy(alpha = 0.08f)
                )
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateToLibrary,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to shelf",
                            tint = settings.theme.textColor
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = settings.theme.textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Ch. ${currentChapterIndex + 1}/${chapters.size} • ${(overallBookProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = settings.theme.textColor.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 1. Quick Dark / Light Mode Toggle
                        IconButton(
                            onClick = { viewModel.toggleReaderDarkTheme() },
                            modifier = Modifier.testTag("reader_quick_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (settings.theme.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (settings.theme.isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = settings.theme.textColor
                            )
                        }

                        // 2. Table of Contents
                        IconButton(
                            onClick = { showTocSheet = true },
                            modifier = Modifier.testTag("reader_toc_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = "Table of contents",
                                tint = settings.theme.textColor
                            )
                        }

                        // 2. Text-to-Speech / Listen
                        IconButton(
                            onClick = {
                                if (!ttsState.isVisible) {
                                    viewModel.setTtsVisible(true)
                                }
                                viewModel.toggleTtsPlayPause()
                            },
                            modifier = Modifier.testTag("reader_tts_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (ttsState.isPlaying) Icons.Default.GraphicEq else Icons.Default.Headphones,
                                contentDescription = if (ttsState.isPlaying) "Pause Reading Aloud" else "Read Aloud with Text-to-Speech",
                                tint = if (ttsState.isPlaying || ttsState.isVisible) settings.theme.accentColor else settings.theme.textColor
                            )
                        }

                        // 3. Quick Bookmark Button
                        IconButton(
                            onClick = {
                                userActivityTime = System.currentTimeMillis()
                                toggleCurrentPageBookmark()
                            },
                            modifier = Modifier.testTag("reader_bookmark_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isCurrentPageBookmarked) "Remove Bookmark (Page $currentGlobalPage)" else "Bookmark Page $currentGlobalPage",
                                tint = if (isCurrentPageBookmarked) (if (isGlassTheme) Color(0xFF00E5FF) else settings.theme.accentColor) else settings.theme.textColor
                            )
                        }

                        // 4. Appearance Settings
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.testTag("reader_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Reader typography and settings",
                                tint = settings.theme.textColor
                            )
                        }

                        // 4. Overflow Menu
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("reader_more_options_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = settings.theme.textColor
                                )
                            }

                            val menuIsDark = isGlassTheme || settings.theme.isDark
                            val menuContainerColor = if (isGlassTheme) {
                                Color(0xF80D1B2A)
                            } else if (settings.theme.isDark) {
                                Color(0xFF1E212B)
                            } else {
                                Color(0xFFFFFFFF)
                            }
                            val menuTextColor = if (menuIsDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            val menuIconColor = if (isGlassTheme) Color(0xFF00E5FF) else if (settings.theme.isDark) Color(0xFF80CBC4) else Color(0xFF1E3A8A)
                            val menuBorderColor = if (isGlassTheme) Color(0x4000E5FF) else if (settings.theme.isDark) Color(0x33FFFFFF) else Color(0x22000000)

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                containerColor = menuContainerColor,
                                border = BorderStroke(1.dp, menuBorderColor),
                                shape = RoundedCornerShape(14.dp),
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("reader_overflow_dropdown_menu")
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isCurrentPageBookmarked) "Remove Bookmark (p. $currentGlobalPage)" else "Bookmark Page $currentGlobalPage",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = menuTextColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = if (isCurrentPageBookmarked) MaterialTheme.colorScheme.primary else menuIconColor
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = menuTextColor,
                                        leadingIconColor = menuIconColor
                                    ),
                                    onClick = {
                                        showOverflowMenu = false
                                        userActivityTime = System.currentTimeMillis()
                                        toggleCurrentPageBookmark()
                                    },
                                    modifier = Modifier.testTag("reader_overflow_bookmark_button")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Reader Settings (Fonts & Layout)",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = menuTextColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = menuIconColor
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = menuTextColor,
                                        leadingIconColor = menuIconColor
                                    ),
                                    onClick = {
                                        showOverflowMenu = false
                                        showSettingsSheet = true
                                    },
                                    modifier = Modifier.testTag("reader_overflow_settings_button")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isSearchVisible) "Close Search" else "Search in Chapter",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = menuTextColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                            contentDescription = null,
                                            tint = menuIconColor
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = menuTextColor,
                                        leadingIconColor = menuIconColor
                                    ),
                                    onClick = {
                                        showOverflowMenu = false
                                        isSearchVisible = !isSearchVisible
                                        if (!isSearchVisible) chapterSearchQuery = ""
                                    },
                                    modifier = Modifier.testTag("reader_search_button")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Jump to Page",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = menuTextColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.FindInPage,
                                            contentDescription = null,
                                            tint = menuIconColor
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = menuTextColor,
                                        leadingIconColor = menuIconColor
                                    ),
                                    onClick = {
                                        showOverflowMenu = false
                                        showJumpToPageDialog = true
                                    },
                                    modifier = Modifier.testTag("reader_jump_to_page_button")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (showReadingSlider) "Hide Reading Slider" else "Show Reading Slider",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = menuTextColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.LinearScale,
                                            contentDescription = null,
                                            tint = menuIconColor
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = menuTextColor,
                                        leadingIconColor = menuIconColor
                                    ),
                                    onClick = {
                                        showOverflowMenu = false
                                        showReadingSlider = !showReadingSlider
                                        if (showReadingSlider) {
                                            userActivityTime = System.currentTimeMillis()
                                        }
                                    },
                                    modifier = Modifier.testTag("reader_toggle_slider_button")
                                )

                                if (libraryBooks.size > 1) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Switch Book",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = menuTextColor
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.SwapHoriz,
                                                contentDescription = null,
                                                tint = menuIconColor
                                            )
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = menuTextColor,
                                            leadingIconColor = menuIconColor
                                        ),
                                        onClick = {
                                            showOverflowMenu = false
                                            showSwitchBookSheet = true
                                        },
                                        modifier = Modifier.testTag("reader_switch_book_button")
                                    )
                                }

                                if (activeBookRecord != null && onShareEpub != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Share EPUB",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = menuTextColor
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = null,
                                                tint = menuIconColor
                                            )
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = menuTextColor,
                                            leadingIconColor = menuIconColor
                                        ),
                                        onClick = {
                                            showOverflowMenu = false
                                            activeBookRecord?.let { book ->
                                                onShareEpub(book.epubFilePath, book.title)
                                            }
                                        },
                                        modifier = Modifier.testTag("reader_share_button")
                                    )
                                }

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (settings.theme.isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = menuTextColor
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (settings.theme.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = null,
                                            tint = menuIconColor
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = menuTextColor,
                                        leadingIconColor = menuIconColor
                                    ),
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleReaderDarkTheme()
                                    },
                                    modifier = Modifier.testTag("reader_dark_mode_toggle")
                                )
                            }
                        }
                    }
                }

                // Inline Chapter Search Bar
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val matchCount = remember(currentChapter.paragraphs, chapterSearchQuery) {
                        if (chapterSearchQuery.isBlank()) 0
                        else {
                            currentChapter.paragraphs.sumOf { paragraph ->
                                var count = 0
                                var idx = 0
                                while (idx < paragraph.length) {
                                    val found = paragraph.indexOf(chapterSearchQuery, idx, ignoreCase = true)
                                    if (found == -1) break
                                    count++
                                    idx = found + chapterSearchQuery.length
                                }
                                count
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(settings.theme.backgroundColor)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chapterSearchQuery,
                            onValueChange = { chapterSearchQuery = it },
                            placeholder = {
                                Text(
                                    "Search in chapter...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = settings.theme.textColor.copy(alpha = 0.5f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = settings.theme.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (chapterSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { chapterSearchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = settings.theme.textColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = settings.theme.textColor.copy(alpha = 0.05f),
                                unfocusedContainerColor = settings.theme.textColor.copy(alpha = 0.03f),
                                focusedTextColor = settings.theme.textColor,
                                unfocusedTextColor = settings.theme.textColor,
                                focusedIndicatorColor = settings.theme.accentColor,
                                unfocusedIndicatorColor = settings.theme.textColor.copy(alpha = 0.2f)
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_search_input")
                        )

                        if (chapterSearchQuery.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (matchCount > 0) settings.theme.accentColor.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = if (matchCount > 0) "$matchCount found" else "No match",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (matchCount > 0) settings.theme.accentColor else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Dedicated Reading Progress Bar (overall book reading progress) - Tap toggles reading slider
                LinearProgressIndicator(
                    progress = { overallBookProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .clickable {
                            showReadingSlider = !showReadingSlider
                            if (showReadingSlider) {
                                userActivityTime = System.currentTimeMillis()
                            }
                        }
                        .testTag("reader_pinned_progress_bar"),
                    color = settings.theme.accentColor,
                    trackColor = settings.theme.textColor.copy(alpha = 0.12f)
                )
            }
        }
    }

        // Dedicated Moon+ Reading Surface (Fixed full-screen bounds — zero re-pagination jitter when HUD toggles)
        val marginStartDp = (book.epubMargins.leftPercent * 2.8f).coerceIn(12f, 44f).dp
        val marginEndDp = (book.epubMargins.rightPercent * 2.8f).coerceIn(12f, 44f).dp
        val marginTopDp = (book.epubMargins.topPercent * 2.2f).coerceIn(10f, 32f).dp
        val marginBottomDp = (book.epubMargins.bottomPercent * 2.2f).coerceIn(14f, 36f).dp

        val readerContainerCoordsRef = remember { CoordsRef() }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .onGloballyPositioned { coords ->
                    readerContainerCoordsRef.coords = coords
                }
        ) {
            val density = LocalDensity.current
            val textMeasurer = rememberTextMeasurer()
            val viewportWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val verticalPageTurnThresholdPx = with(density) { 56.dp.toPx() }
            val chapterOverscrollThresholdPx = with(density) { 88.dp.toPx() }

            // Unified coordinate-based Moon+ Reader tap handler using LIVE window bounds:
            // Left 33% -> Previous page | Center 34% -> Toggle HUD | Right 33% -> Next page
            val handleReaderTapAtWindowX: (Float) -> Unit = { windowX ->
                val liveCoords = readerContainerCoordsRef.coords?.takeIf { it.isAttached }
                val containerLeftX = liveCoords?.positionInWindow()?.x ?: 0f
                val containerWidth = (liveCoords?.size?.width?.toFloat() ?: viewportWidthPx).coerceAtLeast(1f)
                val tapFraction = ((windowX - containerLeftX) / containerWidth).coerceIn(0f, 1f)
                if (settings.pageTurnMode != PageTurnMode.SCROLL_ONLY) {
                    when {
                        tapFraction < 0.33f -> turnPagePrevious()
                        tapFraction > 0.67f -> turnPageNext()
                        else -> {
                            val nextVisible = !areBarsVisible
                            viewModel.setReaderBarsVisible(nextVisible)
                            if (nextVisible) {
                                userActivityTime = System.currentTimeMillis()
                            }
                        }
                    }
                } else {
                    val nextVisible = !areBarsVisible
                    viewModel.setReaderBarsVisible(nextVisible)
                    if (nextVisible) {
                        userActivityTime = System.currentTimeMillis()
                    }
                }
            }
            val latestTapHandler by androidx.compose.runtime.rememberUpdatedState(handleReaderTapAtWindowX)
            val stableTapAtWindowX: (Float) -> Unit = remember {
                { windowX: Float -> latestTapHandler(windowX) }
            }

            if (isPaginatedMode) {
                val contentWidthPx = with(density) {
                    (maxWidth - marginStartDp - marginEndDp).toPx().roundToInt().coerceAtLeast(120)
                }
                val contentHeightPx = with(density) {
                    (maxHeight - marginTopDp - marginBottomDp - 30.dp).toPx().roundToInt().coerceAtLeast(220)
                }
                val composeFontFamily = remember(settings.fontFamily) {
                    resolveReaderFont(settings.fontFamily)
                }
                val isLastChapter = currentChapterIndex >= chapters.size - 1

                val chapterPages = remember(
                    currentChapter.id,
                    currentChapter.paragraphs,
                    isLastChapter,
                    settings.fontSizeSp,
                    settings.lineSpacingMultiplier,
                    settings.fontFamily,
                    settings.textAlign,
                    contentWidthPx,
                    contentHeightPx
                ) {
                    ReaderPaginator.paginateChapter(
                        chapter = currentChapter,
                        settings = settings,
                        composeFontFamily = composeFontFamily,
                        contentWidthPx = contentWidthPx,
                        contentHeightPx = contentHeightPx,
                        density = density,
                        textMeasurer = textMeasurer,
                        bookImages = book.bookImages,
                        isLastChapter = isLastChapter
                    )
                }

                val safePageCount = chapterPages.size.coerceAtLeast(1)
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { safePageCount }
                )

                // Register live pager turn callback so taps, vertical scrolls & volume keys always read live pagerState.currentPage
                paginatedTurnBridge.turnBy = { delta ->
                    val current = pagerState.currentPage
                    val count = pagerState.pageCount.coerceAtLeast(1)
                    if (delta > 0) {
                        if (current < count - 1) {
                            val nextPage = current + 1
                            currentPaginatedPageIdx = nextPage
                            coroutineScope.launch {
                                try {
                                    pagerState.animateScrollToPage(nextPage)
                                } catch (_: Throwable) {}
                            }
                        } else if (currentChapterIndex < chapters.size - 1) {
                            pendingScrollFraction = 0f
                            viewModel.nextChapter()
                        }
                    } else if (delta < 0) {
                        if (current > 0) {
                            val prevPage = current - 1
                            currentPaginatedPageIdx = prevPage
                            coroutineScope.launch {
                                try {
                                    pagerState.animateScrollToPage(prevPage)
                                } catch (_: Throwable) {}
                            }
                        } else if (currentChapterIndex > 0) {
                            pendingScrollFraction = 1f
                            viewModel.previousChapter()
                        }
                    }
                }

                LaunchedEffect(safePageCount) {
                    totalPaginatedPagesCount = safePageCount
                }

                LaunchedEffect(pagerState.currentPage) {
                    currentPaginatedPageIdx = pagerState.currentPage
                    viewModel.updateReadingScrollOffset(pagerState.currentPage)
                }

                // Handle chapter transitions, subheading jumps, or requested scroll fractions
                LaunchedEffect(currentChapterIndex, safePageCount, pendingScrollFraction, pendingParagraphJumpIndex) {
                    val targetPara = pendingParagraphJumpIndex
                    val targetFrac = pendingScrollFraction
                    if (targetPara != null) {
                        pendingParagraphJumpIndex = null
                        pendingScrollFraction = null
                        val pageIdx = ReaderPaginator.findPageForParagraph(chapterPages, targetPara)
                        val clamped = pageIdx.coerceIn(0, safePageCount - 1)
                        currentPaginatedPageIdx = clamped
                        pagerState.scrollToPage(clamped)
                    } else if (targetFrac != null) {
                        pendingScrollFraction = null
                        val targetIdx = (targetFrac * (safePageCount - 1)).roundToInt().coerceIn(0, safePageCount - 1)
                        currentPaginatedPageIdx = targetIdx
                        pagerState.scrollToPage(targetIdx)
                    } else if (pagerState.currentPage >= safePageCount) {
                        val last = safePageCount - 1
                        currentPaginatedPageIdx = last
                        pagerState.scrollToPage(last)
                    }
                }

                // Collect slider / jump-to-page requests without ever self-cancelling animateScrollToPage
                LaunchedEffect(pagerState) {
                    pagerJumpFlow.collect { target ->
                        val maxIdx = (pagerState.pageCount - 1).coerceAtLeast(0)
                        val clamped = target.coerceIn(0, maxIdx)
                        pagerState.animateScrollToPage(clamped)
                    }
                }

                // Follow TTS paragraph in Paginated Mode
                LaunchedEffect(ttsState.paragraphIndex, ttsState.isPlaying) {
                    if (ttsState.isPlaying && ttsState.chapterIndex == currentChapterIndex) {
                        val pageIdx = ReaderPaginator.findPageForParagraph(chapterPages, ttsState.paragraphIndex)
                        if (pageIdx != pagerState.currentPage && pageIdx in 0 until safePageCount) {
                            pagerState.animateScrollToPage(pageIdx)
                        }
                    }
                }

                // Allow horizontal overscroll at chapter boundaries to smoothly advance to next/previous chapter
                val paginatedOverscrollConnection = remember(pagerState, currentChapterIndex, chapters.size, chapterOverscrollThresholdPx) {
                    object : NestedScrollConnection {
                        private var overscrollAccumX = 0f
                        private var lastSwitchTimeMs = 0L

                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (source == NestedScrollSource.UserInput && available.x != 0f) {
                                val now = System.currentTimeMillis()
                                if (now - lastSwitchTimeMs < 450L) return Offset.Zero
                                if (available.x < 0f && pagerState.currentPage >= pagerState.pageCount - 1 && currentChapterIndex < chapters.size - 1) {
                                    overscrollAccumX += -available.x
                                    if (overscrollAccumX >= chapterOverscrollThresholdPx) {
                                        overscrollAccumX = 0f
                                        lastSwitchTimeMs = now
                                        pendingScrollFraction = 0f
                                        viewModel.nextChapter()
                                    }
                                } else if (available.x > 0f && pagerState.currentPage == 0 && currentChapterIndex > 0) {
                                    overscrollAccumX += available.x
                                    if (overscrollAccumX >= chapterOverscrollThresholdPx) {
                                        overscrollAccumX = 0f
                                        lastSwitchTimeMs = now
                                        pendingScrollFraction = 1f
                                        viewModel.previousChapter()
                                    }
                                }
                            } else if (consumed.x != 0f) {
                                overscrollAccumX = 0f
                            }
                            return Offset.Zero
                        }

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            overscrollAccumX = 0f
                            return Velocity.Zero
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(paginatedOverscrollConnection)
                        // Strictly vertical swipe page-turn that NEVER fights HorizontalPager horizontal swipes
                        .pointerInput(verticalPageTurnThresholdPx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var totalDx = 0f
                                var totalDy = 0f
                                var prevPos = down.position
                                var lockedHorizontal = false
                                var turnedInGesture = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val delta = change.position - prevPos
                                    prevPos = change.position
                                    totalDx += delta.x
                                    totalDy += delta.y

                                    if (!lockedHorizontal && abs(totalDx) > 18f && abs(totalDx) > abs(totalDy) * 1.1f) {
                                        lockedHorizontal = true
                                    }
                                    if (!lockedHorizontal && !turnedInGesture &&
                                        abs(totalDy) >= verticalPageTurnThresholdPx * 1.45f &&
                                        abs(totalDy) > abs(totalDx) * 1.5f
                                    ) {
                                        turnedInGesture = true
                                        if (totalDy < 0f) {
                                            latestTurnPageNext()
                                        } else {
                                            latestTurnPagePrevious()
                                        }
                                    }
                                }

                                if (!lockedHorizontal && !turnedInGesture &&
                                    abs(totalDy) >= verticalPageTurnThresholdPx &&
                                    abs(totalDy) > abs(totalDx) * 1.5f
                                ) {
                                    if (totalDy < 0f) {
                                        latestTurnPageNext()
                                    } else {
                                        latestTurnPagePrevious()
                                    }
                                }
                            }
                        }
                        .testTag("reader_paginated_pager")
                ) { pageIdx ->
                    val pageModel = chapterPages.getOrNull(pageIdx) ?: return@HorizontalPager
                    val pageCoordsRef = remember { CoordsRef() }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coords ->
                                pageCoordsRef.coords = coords
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val wx = pageCoordsRef.coords?.takeIf { it.isAttached }?.localToWindow(offset)?.x ?: offset.x
                                        stableTapAtWindowX(wx)
                                    }
                                )
                            }
                            .padding(
                                start = marginStartDp,
                                end = marginEndDp,
                                top = marginTopDp + 8.dp,
                                bottom = marginBottomDp + 28.dp
                            )
                    ) {
                        PaginatedPageRenderer(
                            chapter = currentChapter,
                            page = pageModel,
                            settings = settings,
                            bookTitle = book.title,
                            totalChapters = chapters.size,
                            searchQuery = chapterSearchQuery,
                            activeTtsParagraphIndex = if ((ttsState.isPlaying || ttsState.isPaused) && ttsState.chapterIndex == currentChapterIndex) ttsState.paragraphIndex else null,
                            highlights = highlights,
                            bookImages = book.bookImages,
                            onWordLongPress = { word, line, paragraph, paraIdx, existingHl ->
                                inspectTarget = WordInspectState(
                                    word = word,
                                    line = line,
                                    paragraph = paragraph,
                                    paragraphIndex = paraIdx,
                                    chapterIndex = currentChapterIndex,
                                    chapterTitle = currentChapter.title,
                                    existingHighlight = existingHl
                                )
                            },
                            onTapAtWindowX = stableTapAtWindowX,
                            onNavigateToLibrary = onNavigateToLibrary
                        )
                    }
                }
            } else {
                paginatedTurnBridge.turnBy = null

                // Smooth Virtualized Continuous Scroll Mode (LazyColumn — zero lag on long chapters)
                LaunchedEffect(pendingParagraphJumpIndex) {
                    val targetPara = pendingParagraphJumpIndex
                    if (targetPara != null) {
                        pendingParagraphJumpIndex = null
                        val clamped = (targetPara + 1).coerceIn(0, currentChapter.paragraphs.size)
                        scrollListState.animateScrollToItem(clamped)
                    }
                }

                // Allow vertical overscroll past bottom/top of a chapter to switch chapters seamlessly
                val scrollOverscrollConnection = remember(scrollListState, currentChapterIndex, chapters.size, chapterOverscrollThresholdPx) {
                    object : NestedScrollConnection {
                        private var overscrollAccumY = 0f
                        private var lastSwitchTimeMs = 0L

                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (source == NestedScrollSource.UserInput && available.y != 0f) {
                                val now = System.currentTimeMillis()
                                if (now - lastSwitchTimeMs < 500L) return Offset.Zero
                                val atBottom = !scrollListState.canScrollForward
                                val atTop = !scrollListState.canScrollBackward
                                if (available.y < 0f && atBottom && currentChapterIndex < chapters.size - 1) {
                                    overscrollAccumY += -available.y
                                    if (overscrollAccumY >= chapterOverscrollThresholdPx) {
                                        overscrollAccumY = 0f
                                        lastSwitchTimeMs = now
                                        pendingScrollFraction = 0f
                                        viewModel.nextChapter()
                                    }
                                } else if (available.y > 0f && atTop && currentChapterIndex > 0) {
                                    overscrollAccumY += available.y
                                    if (overscrollAccumY >= chapterOverscrollThresholdPx) {
                                        overscrollAccumY = 0f
                                        lastSwitchTimeMs = now
                                        pendingScrollFraction = 1f
                                        viewModel.previousChapter()
                                    }
                                }
                            } else if (consumed.y != 0f) {
                                overscrollAccumY = 0f
                            }
                            return Offset.Zero
                        }

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            overscrollAccumY = 0f
                            return Velocity.Zero
                        }
                    }
                }

                val scrollCoordsRef = remember { CoordsRef() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollOverscrollConnection)
                        .onGloballyPositioned { coords ->
                            scrollCoordsRef.coords = coords
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val wx = scrollCoordsRef.coords?.takeIf { it.isAttached }?.localToWindow(offset)?.x ?: offset.x
                                    stableTapAtWindowX(wx)
                                }
                            )
                        }
                        .testTag("reader_text_scroll_container")
                ) {
                    EpubLazyTextRenderer(
                        chapter = currentChapter,
                        listState = scrollListState,
                        settings = settings,
                        bookTitle = book.title,
                        totalChapters = chapters.size,
                        contentPadding = PaddingValues(
                            start = marginStartDp,
                            end = marginEndDp,
                            top = marginTopDp + 8.dp,
                            bottom = marginBottomDp + 32.dp
                        ),
                        searchQuery = chapterSearchQuery,
                        activeTtsParagraphIndex = if ((ttsState.isPlaying || ttsState.isPaused) && ttsState.chapterIndex == currentChapterIndex) ttsState.paragraphIndex else null,
                        highlights = highlights,
                        bookImages = book.bookImages,
                        onWordLongPress = { word, line, paragraph, paraIdx, existingHl ->
                            inspectTarget = WordInspectState(
                                word = word,
                                line = line,
                                paragraph = paragraph,
                                paragraphIndex = paraIdx,
                                chapterIndex = currentChapterIndex,
                                chapterTitle = currentChapter.title,
                                existingHighlight = existingHl
                            )
                        },
                        onTapAtWindowX = stableTapAtWindowX,
                        onNavigateToLibrary = onNavigateToLibrary
                    )
                }
            }

            // Optional Non-Blocking Visual Hint Badges when Tap-Zone Hints are enabled in settings
            if (settings.pageTurnMode != PageTurnMode.SCROLL_ONLY && settings.showTapZonesOverlay) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .size(30.dp),
                    shape = CircleShape,
                    color = settings.theme.accentColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, settings.theme.accentColor.copy(alpha = 0.45f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Previous Page Zone",
                            tint = settings.theme.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .size(30.dp),
                    shape = CircleShape,
                    color = settings.theme.accentColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, settings.theme.accentColor.copy(alpha = 0.45f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Next Page Zone",
                            tint = settings.theme.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Moon+ Screen Brightness Dimming Scrim (non-touch-intercepting)
            if (settings.brightnessLevel < 0.99f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = ((1f - settings.brightnessLevel) * 0.72f).coerceIn(0f, 0.65f)
                            )
                        )
                )
            }

            // Moon+ Discrete Single-Line Status Footer (visible when HUD bars are hidden)
            androidx.compose.animation.AnimatedVisibility(
                visible = !areBarsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                MoonReaderStatusFooter(
                    chapterTitle = currentChapter.title,
                    currentGlobalPage = currentGlobalPage,
                    totalBookPages = totalBookPages,
                    overallProgress = overallBookProgress,
                    theme = settings.theme,
                    onTapFooter = {
                        viewModel.setReaderBarsVisible(true)
                        userActivityTime = System.currentTimeMillis()
                    }
                )
            }
        }

        // Floating Bottom HUD Stack (TTS Controls + Moon+ Quick Dock) — never resizes the reading viewport
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(3f)
                .navigationBarsPadding()
                .padding(bottom = if (areBarsVisible) 88.dp else 12.dp)
        ) {
            // Docked Text-to-Speech Controls Bar
            AnimatedVisibility(
                visible = ttsState.isVisible || ttsState.isPlaying || ttsState.isPaused,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                TtsReaderControlsBar(
                    ttsState = ttsState,
                    readerTheme = settings.theme,
                    onTogglePlayPause = { viewModel.toggleTtsPlayPause() },
                    onSkipNext = { viewModel.skipTtsNext() },
                    onSkipPrevious = { viewModel.skipTtsPrevious() },
                    onStop = { viewModel.stopTts() },
                    onClose = { viewModel.closeTts() },
                    onSetSpeechRate = { viewModel.setTtsSpeechRate(it) }
                )
            }

            // Moon+ Bottom Reader Control Dock (Auto-hides smoothly with areBarsVisible)
            AnimatedVisibility(
                visible = areBarsVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                MoonReaderQuickDock(
                    settings = settings,
                    currentGlobalPage = currentGlobalPage,
                    totalBookPages = totalBookPages,
                    wholeBookProgressFraction = wholeBookProgressFraction,
                    showReadingSlider = showReadingSlider,
                    showBrightnessQuickBar = showBrightnessQuickBar,
                    isTtsActive = ttsState.isPlaying || ttsState.isVisible,
                    isDraggingSlider = isDraggingSlider,
                    onDraggingSliderChange = { isDraggingSlider = it },
                    onJumpFraction = jumpToWholeBookFraction,
                    onToggleReadingSlider = { showReadingSlider = !showReadingSlider },
                    onToggleBrightnessBar = { showBrightnessQuickBar = !showBrightnessQuickBar },
                    onOpenJumpToPage = { showJumpToPageDialog = true },
                    onOpenToc = { showTocSheet = true },
                    onToggleReadingMode = {
                        val nextMode = if (settings.readingMode == ReaderReadingMode.PAGINATED) {
                            ReaderReadingMode.SCROLL
                        } else {
                            ReaderReadingMode.PAGINATED
                        }
                        viewModel.updateReaderSettings { it.copy(readingMode = nextMode) }
                    },
                    onToggleTts = {
                        if (!ttsState.isVisible) viewModel.setTtsVisible(true)
                        viewModel.toggleTtsPlayPause()
                    },
                    onBrightnessChange = { b ->
                        viewModel.updateReaderSettings { it.copy(brightnessLevel = b) }
                    },
                    onSelectTheme = { th ->
                        viewModel.updateReaderSettings { it.copy(theme = th) }
                    },
                    onUserActivity = { userActivityTime = System.currentTimeMillis() }
                )
            }
        }
    }

    // Long-press Word & Reading Options Sheet (Copy, Word/Line/Passage Highlight, Note, Google Dictionary, Ask Gemini)
    inspectTarget?.let { target ->
        ReadingWordInspectSheet(
            target = target,
            theme = settings.theme,
            onDismiss = { inspectTarget = null },
            onHighlight = { colorHex, scope, targetText ->
                val existing = target.existingHighlight
                if (existing != null && existing.scope.equals(scope, ignoreCase = true) && existing.text.equals(targetText, ignoreCase = true)) {
                    viewModel.updateHighlightColor(
                        highlight = existing,
                        newColorHex = colorHex,
                        newScope = scope,
                        newText = targetText
                    )
                } else {
                    viewModel.addHighlight(
                        chapterIndex = target.chapterIndex,
                        chapterTitle = target.chapterTitle,
                        paragraphIndex = target.paragraphIndex,
                        text = targetText,
                        colorHex = colorHex,
                        scope = scope
                    )
                }
            },
            onSaveNote = { noteText ->
                viewModel.saveHighlightNote(
                    chapterIndex = target.chapterIndex,
                    chapterTitle = target.chapterTitle,
                    paragraphIndex = target.paragraphIndex,
                    text = target.word.ifBlank { target.paragraph },
                    note = noteText,
                    existingHighlight = target.existingHighlight,
                    scope = target.existingHighlight?.scope ?: "WORD"
                )
            },
            onRemoveHighlight = { highlightId ->
                viewModel.removeHighlight(highlightId)
            },
            onSearchInBook = { query ->
                chapterSearchQuery = query
                isSearchVisible = true
                viewModel.setReaderBarsVisible(true)
                userActivityTime = System.currentTimeMillis()
            },
            onReadAloud = { paraIdx ->
                viewModel.startTtsAtParagraph(
                    chapterIndex = target.chapterIndex,
                    chapterTitle = target.chapterTitle,
                    paragraphs = currentChapter.paragraphs,
                    paragraphIndex = paraIdx
                )
            }
        )
    }
}

/**
 * Dedicated Reader Shelf View shown when no book is currently loaded,
 * allowing users to pick any EPUB file or choose from their converted EPUB books.
 */
@Composable
fun ReaderShelfView(
    convertedBooks: List<ConvertedBook>,
    onOpenBook: (ConvertedBook) -> Unit,
    onPickEpubFile: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()
    val isDark = LocalGlassPalette.current.isDark

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero Card to Open EPUB file — matches Reading Statistics container theme & color composition
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_epub_file_card"),
            shape = RoundedCornerShape(22.dp),
            elevated = true,
            accentTint = gold,
            borderWidth = 1.25.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        gold.copy(alpha = if (isDark) 0.22f else 0.16f),
                                        blue.copy(alpha = if (isDark) 0.20f else 0.14f)
                                    )
                                )
                            )
                            .border(1.1.dp, luxuryGoldBlueBorderBrush(0.75f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "READER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.6.sp,
                                fontSize = 10.sp
                            ),
                            color = gold
                        )
                        Text(
                            text = "Open EPUB File",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Read any converted EPUB or external eBook",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Embedded Pill Action Button matching Reading Statistics Reset pill
                Surface(
                    onClick = onPickEpubFile,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0x33152946) else blue.copy(alpha = 0.08f),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                gold.copy(alpha = 0.72f),
                                blue.copy(alpha = 0.65f)
                            )
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pick_epub_file_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select EPUB from Device",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Converted Books Shelf Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Converted Books (${convertedBooks.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (convertedBooks.isNotEmpty()) {
                    TextButton(onClick = onNavigateToLibrary) {
                        Text(
                            text = "View All",
                            color = gold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (convertedBooks.isEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    elevated = true,
                    accentTint = gold,
                    borderWidth = 1.25.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            gold.copy(alpha = if (isDark) 0.22f else 0.16f),
                                            blue.copy(alpha = if (isDark) 0.20f else 0.14f)
                                        )
                                    )
                                )
                                .border(1.1.dp, luxuryGoldBlueBorderBrush(0.75f), RoundedCornerShape(15.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = gold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "No Converted Books Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Convert any PDF in the 'Convert' tab or open an existing EPUB above to start reading.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                convertedBooks.forEach { book ->
                    val isBookCompleted = book.isCompleted || (book.chapterCount > 1 && book.lastReadChapterIndex >= book.chapterCount - 1)
                    val totalCh = book.chapterCount.coerceAtLeast(1)
                    val readCh = if (isBookCompleted) totalCh else (book.lastReadChapterIndex + 1).coerceAtMost(totalCh)
                    val progressFloat = if (isBookCompleted) 1f else (readCh.toFloat() / totalCh.toFloat()).coerceIn(0f, 1f)

                    GlassCard(
                        onClick = { onOpenBook(book) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shelf_book_${book.id}"),
                        shape = RoundedCornerShape(22.dp),
                        elevated = true,
                        accentTint = if (isBookCompleted) GreenSuccess else gold,
                        borderWidth = 1.25.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cover Thumbnail matching Reading Statistics gold-blue framed badge
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                gold.copy(alpha = if (isDark) 0.18f else 0.14f),
                                                blue.copy(alpha = if (isDark) 0.18f else 0.12f)
                                            )
                                        )
                                    )
                                    .border(1.1.dp, luxuryGoldBlueBorderBrush(0.75f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (book.coverImagePath != null && File(book.coverImagePath).exists()) {
                                    AsyncImage(
                                        model = File(book.coverImagePath),
                                        contentDescription = "Cover of ${book.title}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        tint = gold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Book Info matching Reading Statistics typography composition
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = book.author,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                LinearProgressIndicator(
                                    progress = { progressFloat },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .testTag("shelf_book_progress_${book.id}"),
                                    color = if (isBookCompleted) GreenSuccess else gold,
                                    trackColor = blue.copy(alpha = 0.18f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Ch. $readCh of $totalCh",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isBookCompleted) "Completed" else "${(progressFloat * 100).toInt()}% read",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isBookCompleted) GreenSuccess else gold
                                    )
                                }
                            }

                            // Embedded Read Pill matching Reading Statistics Reset pill
                            Surface(
                                onClick = { onOpenBook(book) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0x33152946) else blue.copy(alpha = 0.08f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            gold.copy(alpha = 0.72f),
                                            blue.copy(alpha = 0.65f)
                                        )
                                    )
                                ),
                                modifier = Modifier.testTag("shelf_read_now_btn_${book.id}")
                            ) {
                                Text(
                                    text = "Read",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Jump to Page and Section Dialog allowing immediate jump
 * by estimated book page number or by chapter and in-page section percentage.
 */
@Composable
fun JumpToPageDialog(
    currentGlobalPage: Int,
    totalBookPages: Int,
    currentChapterIndex: Int,
    totalChapters: Int,
    currentChapterTitle: String,
    currentSectionProgress: Float,
    pagesPerChapter: List<Int>,
    chapters: List<ReaderChapter>,
    theme: ReaderTheme,
    onDismiss: () -> Unit,
    onJumpToBookPage: (targetPage: Int) -> Unit,
    onJumpToChapterSection: (chapterIndex: Int, sectionFraction: Float) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var inputPageText by remember { mutableStateOf(currentGlobalPage.toString()) }
    var sliderPage by remember { mutableFloatStateOf(currentGlobalPage.toFloat()) }

    var selectedChapter by remember { mutableStateOf(currentChapterIndex) }
    var selectedSectionFraction by remember { mutableFloatStateOf(currentSectionProgress) }

    val targetPage = (inputPageText.toIntOrNull() ?: sliderPage.toInt()).coerceIn(1, totalBookPages)

    // Destination chapter calculation for preview
    val destinationChapterPreview = remember(targetPage, pagesPerChapter, chapters) {
        var acc = 0
        var foundCh = chapters.firstOrNull()
        for ((idx, pCount) in pagesPerChapter.withIndex()) {
            if (targetPage <= acc + pCount || idx == pagesPerChapter.size - 1) {
                foundCh = chapters.getOrNull(idx)
                break
            }
            acc += pCount
        }
        foundCh
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.FindInPage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Jump to Page / Section",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("By Book Page", style = MaterialTheme.typography.labelMedium) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("By Chapter & Section", style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                if (selectedTab == 0) {
                    // Mode 1: Jump by Book Page
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Page:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Total: $totalBookPages pages",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Page Number Input with stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val newP = (targetPage - 1).coerceAtLeast(1)
                                    inputPageText = newP.toString()
                                    sliderPage = newP.toFloat()
                                },
                                modifier = Modifier.size(42.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("-1")
                            }

                            OutlinedTextField(
                                value = inputPageText,
                                onValueChange = { str ->
                                    val filtered = str.filter { it.isDigit() }
                                    inputPageText = filtered
                                    filtered.toIntOrNull()?.let {
                                        sliderPage = it.coerceIn(1, totalBookPages).toFloat()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("jump_page_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                ),
                                prefix = { Text("Page ") },
                                suffix = { Text("/ $totalBookPages") }
                            )

                            OutlinedButton(
                                onClick = {
                                    val newP = (targetPage + 1).coerceAtMost(totalBookPages)
                                    inputPageText = newP.toString()
                                    sliderPage = newP.toFloat()
                                },
                                modifier = Modifier.size(42.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+1")
                            }
                        }

                        // Quick Steppers (-10, +10)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val newP = (targetPage - 10).coerceAtLeast(1)
                                    inputPageText = newP.toString()
                                    sliderPage = newP.toFloat()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-10 Pages")
                            }

                            OutlinedButton(
                                onClick = {
                                    val newP = (targetPage + 10).coerceAtMost(totalBookPages)
                                    inputPageText = newP.toString()
                                    sliderPage = newP.toFloat()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+10 Pages")
                            }
                        }

                        // Page Scrubbing Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Scrub Page: $targetPage of $totalBookPages",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val safeSliderMax = maxOf(2f, totalBookPages.toFloat())
                            Slider(
                                value = sliderPage.coerceIn(1f, safeSliderMax),
                                onValueChange = { v ->
                                    sliderPage = v
                                    inputPageText = v.toInt().toString()
                                },
                                valueRange = 1f..safeSliderMax,
                                enabled = totalBookPages > 1,
                                steps = if (totalBookPages > 2) (totalBookPages - 2).coerceAtMost(50) else 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("jump_page_slider")
                            )
                        }

                        // Quick Jump Shortcuts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(
                                1 to "Start (1)",
                                (totalBookPages * 0.25).toInt().coerceAtLeast(1) to "25%",
                                (totalBookPages * 0.50).toInt().coerceAtLeast(1) to "50%",
                                (totalBookPages * 0.75).toInt().coerceAtLeast(1) to "75%",
                                totalBookPages to "End ($totalBookPages)"
                            ).forEach { (p, label) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (targetPage == p) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            inputPageText = p.toString()
                                            sliderPage = p.toFloat()
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (targetPage == p) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        ),
                                        color = if (targetPage == p) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Destination Chapter Preview
                        destinationChapterPreview?.let { ch ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Lands in Chapter ${ch.index + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = ch.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Mode 2: Jump by Chapter & Section
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Chapter Selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Chapter: ${selectedChapter + 1} of $totalChapters",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = chapters.getOrNull(selectedChapter)?.title ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                                )
                            }

                            val safeChapterMax = maxOf(1f, (totalChapters - 1).toFloat())
                            Slider(
                                value = selectedChapter.toFloat().coerceIn(0f, safeChapterMax),
                                onValueChange = { selectedChapter = it.toInt().coerceIn(0, (totalChapters - 1).coerceAtLeast(0)) },
                                valueRange = 0f..safeChapterMax,
                                enabled = totalChapters > 1,
                                steps = if (totalChapters > 2) (totalChapters - 2).coerceAtMost(50) else 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("jump_chapter_slider")
                            )
                        }

                        HorizontalDivider()

                        // In-Chapter Section Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Section of Page/Chapter:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${(selectedSectionFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Slider(
                                value = selectedSectionFraction,
                                onValueChange = { selectedSectionFraction = it },
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("jump_section_slider")
                            )

                            // Quick section chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    0.0f to "0% Start",
                                    0.25f to "25%",
                                    0.50f to "50% Mid",
                                    0.75f to "75%",
                                    1.0f to "100% End"
                                ).forEach { (f, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (kotlin.math.abs(selectedSectionFraction - f) < 0.1f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedSectionFraction = f }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (kotlin.math.abs(selectedSectionFraction - f) < 0.1f) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            ),
                                            color = if (kotlin.math.abs(selectedSectionFraction - f) < 0.1f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        onJumpToBookPage(targetPage)
                    } else {
                        onJumpToChapterSection(selectedChapter, selectedSectionFraction)
                    }
                },
                modifier = Modifier.testTag("jump_page_confirm_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (selectedTab == 0) "Jump to Page $targetPage" else "Jump to Section")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
