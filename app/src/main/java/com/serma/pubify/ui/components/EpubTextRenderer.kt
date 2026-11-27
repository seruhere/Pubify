package com.serma.pubify.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serma.pubify.R
import com.serma.pubify.data.model.BookHighlight
import com.serma.pubify.reader.ReaderChapter
import com.serma.pubify.reader.ReaderFontFamily
import com.serma.pubify.reader.ReaderPage
import com.serma.pubify.reader.ReaderSettings
import com.serma.pubify.reader.ReaderTextAlign
import com.serma.pubify.ui.theme.GreenSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Plain non-state holder so onGloballyPositioned / onTextLayout updates never trigger
 * redundant per-frame Compose recompositions during smooth scrolling or page swipes.
 */
private class MutableLayoutRef<T>(var value: T? = null)

/**
 * Bounded in-memory bitmap cache for decoded EPUB/PDF figures so scrolling past images
 * never re-decodes JPEG/PNG payloads on the main thread.
 */
private object ReaderImageBitmapCache {
    private val cache = object : LruCache<String, ImageBitmap>(24) {}

    fun get(key: String): ImageBitmap? = synchronized(cache) { cache.get(key) }

    fun put(key: String, bitmap: ImageBitmap) {
        synchronized(cache) {
            cache.put(key, bitmap)
        }
    }
}

/**
 * Virtualized LazyColumn text rendering component for Continuous Scroll mode.
 * Only composes paragraphs currently visible in the viewport, eliminating scroll jank on long chapters.
 */
@Composable
fun EpubLazyTextRenderer(
    chapter: ReaderChapter,
    listState: LazyListState,
    settings: ReaderSettings,
    bookTitle: String,
    totalChapters: Int,
    contentPadding: PaddingValues,
    searchQuery: String = "",
    activeTtsParagraphIndex: Int? = null,
    highlights: List<BookHighlight> = emptyList(),
    bookImages: Map<String, ByteArray> = emptyMap(),
    previousChapterTitle: String? = null,
    nextChapterTitle: String? = null,
    onPreviousChapter: (() -> Unit)? = null,
    onNextChapter: (() -> Unit)? = null,
    onWordLongPress: ((word: String, line: String, fullParagraph: String, paragraphIndex: Int, existingHighlight: BookHighlight?) -> Unit)? = null,
    onTapAtWindowX: ((windowX: Float) -> Unit)? = null,
    onNavigateToLibrary: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val composeFontFamily = rememberReaderFontFamily(settings.fontFamily)
    val textAlign = rememberReaderTextAlign(settings.textAlign)
    val firstBodyParaIndex = rememberFirstBodyParagraphIndex(chapter.paragraphs)
    val highlightsByParagraph = remember(highlights, chapter.index) {
        highlights.filter { it.chapterIndex == chapter.index }.groupBy { it.paragraphIndex }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxSize()
            .testTag("epub_text_renderer")
    ) {
        item(key = "chapter_header_${chapter.index}") {
            if (!previousChapterTitle.isNullOrBlank() && onPreviousChapter != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPreviousChapter() }
                        .testTag("scroll_previous_chapter_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = settings.theme.accentColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, settings.theme.accentColor.copy(alpha = 0.22f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous chapter",
                            tint = settings.theme.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PREVIOUS CHAPTER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = settings.theme.accentColor
                            )
                            Text(
                                text = previousChapterTitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = settings.theme.textColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            ChapterHeaderSection(
                chapter = chapter,
                settings = settings,
                totalChapters = totalChapters,
                composeFontFamily = composeFontFamily,
                onTapAtWindowX = onTapAtWindowX
            )
        }

        itemsIndexed(
            items = chapter.paragraphs,
            key = { idx, _ -> "para_${chapter.index}_$idx" }
        ) { index, rawBlock ->
            val block = rawBlock.trim()
            if (block.isNotBlank()) {
                val paraHighlights = highlightsByParagraph[index].orEmpty()
                ParagraphBlockItem(
                    chapterIndex = chapter.index,
                    paragraphIndex = index,
                    displayBlock = block,
                    fullBlock = block,
                    isFirstBodyParagraph = (index == firstBodyParaIndex),
                    settings = settings,
                    composeFontFamily = composeFontFamily,
                    textAlign = textAlign,
                    searchQuery = searchQuery,
                    isSpeakingThis = (activeTtsParagraphIndex == index),
                    paragraphHighlights = paraHighlights,
                    bookImages = bookImages,
                    onWordLongPress = onWordLongPress,
                    onTapAtWindowX = onTapAtWindowX
                )
            }
        }

        if (chapter.index >= totalChapters - 1) {
            item(key = "book_completed_footer_${chapter.index}") {
                Spacer(modifier = Modifier.height(28.dp))
                BookCompletedFooterCard(
                    settings = settings,
                    bookTitle = bookTitle,
                    onNavigateToLibrary = onNavigateToLibrary
                )
            }
        } else if (!nextChapterTitle.isNullOrBlank() && onNextChapter != null) {
            item(key = "next_chapter_footer_${chapter.index}") {
                Spacer(modifier = Modifier.height(28.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onNextChapter() }
                        .testTag("scroll_next_chapter_button"),
                    shape = RoundedCornerShape(18.dp),
                    color = settings.theme.accentColor.copy(alpha = 0.11f),
                    border = BorderStroke(1.dp, settings.theme.accentColor.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "END OF CHAPTER • KEEP SCROLLING OR TAP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.7.sp
                                ),
                                color = settings.theme.accentColor
                            )
                            Text(
                                text = nextChapterTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = settings.theme.textColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = settings.theme.accentColor,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next chapter",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "chapter_bottom_spacer_${chapter.index}") {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * Dedicated high-craft text rendering component for reading EPUB chapters in Continuous Scroll mode.
 */
@Composable
fun EpubTextRenderer(
    chapter: ReaderChapter,
    settings: ReaderSettings,
    bookTitle: String,
    totalChapters: Int,
    searchQuery: String = "",
    activeTtsParagraphIndex: Int? = null,
    highlights: List<BookHighlight> = emptyList(),
    bookImages: Map<String, ByteArray> = emptyMap(),
    onPlayFromParagraph: ((paragraphIndex: Int) -> Unit)? = null,
    onWordLongPress: ((word: String, line: String, fullParagraph: String, paragraphIndex: Int, existingHighlight: BookHighlight?) -> Unit)? = null,
    onTapAtWindowX: ((windowX: Float) -> Unit)? = null,
    onNavigateToLibrary: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val composeFontFamily = rememberReaderFontFamily(settings.fontFamily)
    val textAlign = rememberReaderTextAlign(settings.textAlign)
    val firstBodyParaIndex = rememberFirstBodyParagraphIndex(chapter.paragraphs)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("epub_text_renderer")
    ) {
        ChapterHeaderSection(
            chapter = chapter,
            settings = settings,
            totalChapters = totalChapters,
            composeFontFamily = composeFontFamily,
            onTapAtWindowX = onTapAtWindowX
        )

        chapter.paragraphs.forEachIndexed { index, rawBlock ->
            val block = rawBlock.trim()
            if (block.isNotBlank()) {
                val paraHighlights = highlights.filter {
                    it.chapterIndex == chapter.index && it.paragraphIndex == index
                }
                ParagraphBlockItem(
                    chapterIndex = chapter.index,
                    paragraphIndex = index,
                    displayBlock = block,
                    fullBlock = block,
                    isFirstBodyParagraph = (index == firstBodyParaIndex),
                    settings = settings,
                    composeFontFamily = composeFontFamily,
                    textAlign = textAlign,
                    searchQuery = searchQuery,
                    isSpeakingThis = (activeTtsParagraphIndex == index),
                    paragraphHighlights = paraHighlights,
                    bookImages = bookImages,
                    onWordLongPress = onWordLongPress,
                    onTapAtWindowX = onTapAtWindowX
                )
            }
        }

        if (chapter.index >= totalChapters - 1) {
            Spacer(modifier = Modifier.height(28.dp))

            BookCompletedFooterCard(
                settings = settings,
                bookTitle = bookTitle,
                onNavigateToLibrary = onNavigateToLibrary
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

/**
 * Moon+ Reader Paginated Page Surface rendering a single screen-sized [ReaderPage] slice
 * inside a HorizontalPager, preserving drop caps, embedded PDF/EPUB figures,
 * Word/Line/Passage highlights, TTS status, live tap coordinates, and long-press word inspection.
 */
@Composable
fun PaginatedPageRenderer(
    chapter: ReaderChapter,
    page: ReaderPage,
    settings: ReaderSettings,
    bookTitle: String,
    totalChapters: Int,
    searchQuery: String = "",
    activeTtsParagraphIndex: Int? = null,
    highlights: List<BookHighlight> = emptyList(),
    bookImages: Map<String, ByteArray> = emptyMap(),
    onWordLongPress: ((word: String, line: String, fullParagraph: String, paragraphIndex: Int, existingHighlight: BookHighlight?) -> Unit)? = null,
    onTapAtWindowX: ((windowX: Float) -> Unit)? = null,
    onNavigateToLibrary: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val composeFontFamily = rememberReaderFontFamily(settings.fontFamily)
    val textAlign = rememberReaderTextAlign(settings.textAlign)
    val firstBodyParaIndex = rememberFirstBodyParagraphIndex(chapter.paragraphs)
    val highlightsByParagraph = remember(highlights, chapter.index) {
        highlights.filter { it.chapterIndex == chapter.index }.groupBy { it.paragraphIndex }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("epub_paginated_page_${page.pageIndexInChapter}")
    ) {
        if (page.includesChapterHeader) {
            ChapterHeaderSection(
                chapter = chapter,
                settings = settings,
                totalChapters = totalChapters,
                composeFontFamily = composeFontFamily,
                onTapAtWindowX = onTapAtWindowX
            )
        }

        page.slices.forEach { slice ->
            val paraHighlights = highlightsByParagraph[slice.paragraphIndex].orEmpty()
            ParagraphBlockItem(
                chapterIndex = chapter.index,
                paragraphIndex = slice.paragraphIndex,
                displayBlock = slice.textSlice,
                fullBlock = slice.rawBlock,
                isFirstBodyParagraph = (!slice.isContinuation && slice.paragraphIndex == firstBodyParaIndex),
                settings = settings,
                composeFontFamily = composeFontFamily,
                textAlign = textAlign,
                searchQuery = searchQuery,
                isSpeakingThis = (activeTtsParagraphIndex == slice.paragraphIndex),
                paragraphHighlights = paraHighlights,
                bookImages = bookImages,
                onWordLongPress = onWordLongPress,
                onTapAtWindowX = onTapAtWindowX
            )
        }

        if (page.includesChapterFooter && chapter.index >= totalChapters - 1) {
            Spacer(modifier = Modifier.height(14.dp))
            BookCompletedFooterCard(
                settings = settings,
                bookTitle = bookTitle,
                onNavigateToLibrary = onNavigateToLibrary
            )
        }
    }
}

@Composable
private fun rememberReaderFontFamily(fontFamily: ReaderFontFamily): FontFamily {
    return remember(fontFamily) {
        when (fontFamily) {
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

@Composable
private fun rememberReaderTextAlign(align: ReaderTextAlign): TextAlign {
    return remember(align) {
        when (align) {
            ReaderTextAlign.JUSTIFY -> TextAlign.Justify
            ReaderTextAlign.LEFT -> TextAlign.Start
            ReaderTextAlign.CENTER -> TextAlign.Center
            ReaderTextAlign.RIGHT -> TextAlign.End
        }
    }
}

@Composable
private fun rememberFirstBodyParagraphIndex(paragraphs: List<String>): Int {
    return remember(paragraphs) {
        paragraphs.indexOfFirst { p ->
            val t = p.trim()
            t.isNotBlank() &&
                !t.startsWith("[[IMG:") &&
                !t.startsWith("[[CENTER]]") &&
                !t.startsWith("#") &&
                !t.startsWith("❝ ") &&
                !t.startsWith("• ")
        }
    }
}

@Composable
private fun ChapterHeaderSection(
    chapter: ReaderChapter,
    settings: ReaderSettings,
    totalChapters: Int,
    composeFontFamily: FontFamily,
    onTapAtWindowX: ((windowX: Float) -> Unit)?
) {
    val theme = settings.theme
    val textColor = theme.textColor
    val accentColor = theme.accentColor
    val estMinutes = (chapter.wordCount / 200).coerceAtLeast(1)
    val headerCoordsRef = remember { MutableLayoutRef<LayoutCoordinates>() }
    val latestOnTap by rememberUpdatedState(onTapAtWindowX)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                headerCoordsRef.value = coords
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val wx = headerCoordsRef.value?.takeIf { it.isAttached }?.localToWindow(offset)?.x ?: offset.x
                        latestOnTap?.invoke(wx)
                    }
                )
            }
            .padding(bottom = 20.dp, top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = accentColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
        ) {
            Text(
                text = "CHAPTER ${chapter.index + 1} OF $totalChapters",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = accentColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = chapter.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = composeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (settings.fontSizeSp * 1.35f).sp,
                lineHeight = (settings.fontSizeSp * 1.6f).sp
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "~$estMinutes min read • ${chapter.wordCount} words",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.65f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = accentColor.copy(alpha = 0.3f))
            Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = accentColor.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun ParagraphBlockItem(
    chapterIndex: Int,
    paragraphIndex: Int,
    displayBlock: String,
    fullBlock: String,
    isFirstBodyParagraph: Boolean,
    settings: ReaderSettings,
    composeFontFamily: FontFamily,
    textAlign: TextAlign,
    searchQuery: String,
    isSpeakingThis: Boolean,
    paragraphHighlights: List<BookHighlight>,
    bookImages: Map<String, ByteArray>,
    onWordLongPress: ((word: String, line: String, fullParagraph: String, paragraphIndex: Int, existingHighlight: BookHighlight?) -> Unit)?,
    onTapAtWindowX: ((windowX: Float) -> Unit)?
) {
    val theme = settings.theme
    val textColor = theme.textColor
    val accentColor = theme.accentColor
    val searchHighlightBgColor = accentColor.copy(alpha = 0.35f)
    val searchHighlightTextColor = if (theme.isDark) Color.White else Color.Black

    // Only tint the entire block container when the user explicitly chose "Passage" scope
    val passageHighlight = paragraphHighlights.firstOrNull { it.scope.equals("PASSAGE", ignoreCase = true) }
    val inlineHighlights = paragraphHighlights.filter { !it.scope.equals("PASSAGE", ignoreCase = true) }
    val isPassageHighlighted = passageHighlight != null
    val passageHighlightColor = if (isPassageHighlighted) Color(passageHighlight!!.colorHex) else Color.Transparent

    val textLayoutResultRef = remember { MutableLayoutRef<TextLayoutResult>() }
    val blockCoordsRef = remember { MutableLayoutRef<LayoutCoordinates>() }

    val latestOnTap by rememberUpdatedState(onTapAtWindowX)
    val latestOnWordLongPress by rememberUpdatedState(onWordLongPress)
    val latestHighlights by rememberUpdatedState(paragraphHighlights)

    val isCenteredBlock = displayBlock.startsWith("[[CENTER]]")
    val normalizedBlock = if (isCenteredBlock) displayBlock.removePrefix("[[CENTER]]").trim() else displayBlock
    val effectiveAlign = if (isCenteredBlock) TextAlign.Center else textAlign

    val visiblePlainText = remember(normalizedBlock) {
        when {
            normalizedBlock.startsWith("#### ") -> normalizedBlock.removePrefix("#### ").trim()
            normalizedBlock.startsWith("### ") -> normalizedBlock.removePrefix("### ").trim()
            normalizedBlock.startsWith("## ") -> normalizedBlock.removePrefix("## ").trim()
            normalizedBlock.startsWith("# ") -> normalizedBlock.removePrefix("# ").trim()
            normalizedBlock.startsWith("❝ ") -> normalizedBlock.removePrefix("❝ ").trim()
            normalizedBlock.startsWith("• ") -> normalizedBlock.removePrefix("• ").trim()
            else -> normalizedBlock
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                blockCoordsRef.value = coords
            }
            .padding(vertical = if (isSpeakingThis || isPassageHighlighted) 4.dp else 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                when {
                    isSpeakingThis -> {
                        Modifier
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    }
                    isPassageHighlighted -> {
                        Modifier
                            .background(passageHighlightColor.copy(alpha = if (theme.isDark) 0.32f else 0.42f))
                            .border(1.5.dp, passageHighlightColor.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    }
                    else -> Modifier
                }
            )
            .pointerInput(displayBlock, fullBlock) {
                detectTapGestures(
                    onTap = { offset ->
                        val wx = blockCoordsRef.value?.takeIf { it.isAttached }?.localToWindow(offset)?.x ?: offset.x
                        latestOnTap?.invoke(wx)
                    },
                    onLongPress = { offset ->
                        val layoutRes = textLayoutResultRef.value
                        val charOffset = layoutRes?.getOffsetForPosition(offset) ?: 0
                        val word = extractWordAtOffset(visiblePlainText, charOffset)
                        val effectiveWord = word.ifBlank {
                            visiblePlainText.split(Regex("\\s+")).firstOrNull { it.isNotBlank() } ?: "Reading"
                        }
                        val line = extractLineAtOffset(visiblePlainText, charOffset, layoutRes)
                        val matchingHl = findMatchingHighlight(
                            highlights = latestHighlights,
                            word = effectiveWord,
                            line = line
                        )
                        latestOnWordLongPress?.invoke(effectiveWord, line, fullBlock, paragraphIndex, matchingHl)
                    }
                )
            }
            .testTag("paragraph_block_$paragraphIndex")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isPassageHighlighted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(passageHighlightColor)
                    )
                    Text(
                        text = "Highlighted Passage",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }

            if (isSpeakingThis) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speaking paragraph",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Reading Aloud • Para ${paragraphIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }

            when {
                // Embedded PDF/EPUB Image
                normalizedBlock.startsWith("[[IMG:") && normalizedBlock.endsWith("]]") -> {
                    val payload = normalizedBlock.removePrefix("[[IMG:").removeSuffix("]]")
                    val parts = payload.split("|")
                    val imgPath = parts.getOrNull(0).orEmpty()
                    val altText = parts.getOrNull(1).orEmpty()
                    val widthPct = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(25, 100) ?: 100

                    val imgBytes = bookImages[imgPath]
                        ?: bookImages[imgPath.substringAfterLast("/")]
                    val cacheKey = "${imgPath}_${imgBytes?.size ?: 0}"
                    val imageBitmap by produceState<ImageBitmap?>(
                        initialValue = ReaderImageBitmapCache.get(cacheKey),
                        key1 = cacheKey,
                        key2 = imgBytes
                    ) {
                        if (value == null && imgBytes != null) {
                            value = withContext(Dispatchers.Default) {
                                try {
                                    BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)?.asImageBitmap()?.also {
                                        ReaderImageBitmapCache.put(cacheKey, it)
                                    }
                                } catch (_: Throwable) {
                                    null
                                }
                            }
                        }
                    }

                    if (imageBitmap != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = imageBitmap!!,
                                contentDescription = altText.ifBlank { "Book figure" },
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth((widthPct / 100f).coerceIn(0.35f, 1.0f))
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.dp,
                                        color = textColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .testTag("reader_embedded_image_$paragraphIndex")
                            )
                            if (altText.isNotBlank() && !altText.startsWith("Figure from page")) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = altText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                    color = textColor.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // H1 Heading
                normalizedBlock.startsWith("# ") -> {
                    val styled = remember(visiblePlainText, inlineHighlights, searchQuery, theme.isDark) {
                        buildStyledParagraphText(
                            text = visiblePlainText,
                            inlineHighlights = inlineHighlights,
                            searchQuery = searchQuery,
                            searchHighlightBgColor = searchHighlightBgColor,
                            searchHighlightTextColor = searchHighlightTextColor,
                            isDarkTheme = theme.isDark
                        )
                    }
                    Text(
                        text = styled,
                        onTextLayout = { textLayoutResultRef.value = it },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = composeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (settings.fontSizeSp * 1.42f).sp,
                            lineHeight = (settings.fontSizeSp * 1.65f).sp
                        ),
                        color = accentColor,
                        textAlign = if (isCenteredBlock) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, bottom = 8.dp)
                    )
                }

                // H2 Topic Heading
                normalizedBlock.startsWith("## ") -> {
                    val styled = remember(visiblePlainText, inlineHighlights, searchQuery, theme.isDark) {
                        buildStyledParagraphText(
                            text = visiblePlainText,
                            inlineHighlights = inlineHighlights,
                            searchQuery = searchQuery,
                            searchHighlightBgColor = searchHighlightBgColor,
                            searchHighlightTextColor = searchHighlightTextColor,
                            isDarkTheme = theme.isDark
                        )
                    }
                    Text(
                        text = styled,
                        onTextLayout = { textLayoutResultRef.value = it },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = composeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (settings.fontSizeSp * 1.25f).sp,
                            lineHeight = (settings.fontSizeSp * 1.45f).sp
                        ),
                        color = accentColor,
                        textAlign = if (isCenteredBlock) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 6.dp)
                    )
                }

                // H3 Sub-topic Heading
                normalizedBlock.startsWith("### ") -> {
                    val styled = remember(visiblePlainText, inlineHighlights, searchQuery, theme.isDark) {
                        buildStyledParagraphText(
                            text = visiblePlainText,
                            inlineHighlights = inlineHighlights,
                            searchQuery = searchQuery,
                            searchHighlightBgColor = searchHighlightBgColor,
                            searchHighlightTextColor = searchHighlightTextColor,
                            isDarkTheme = theme.isDark
                        )
                    }
                    Text(
                        text = styled,
                        onTextLayout = { textLayoutResultRef.value = it },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = composeFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (settings.fontSizeSp * 1.13f).sp,
                            lineHeight = (settings.fontSizeSp * 1.35f).sp
                        ),
                        color = accentColor.copy(alpha = 0.92f),
                        textAlign = if (isCenteredBlock) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 5.dp)
                    )
                }

                // H4 Sub-topic Heading
                normalizedBlock.startsWith("#### ") -> {
                    val styled = remember(visiblePlainText, inlineHighlights, searchQuery, theme.isDark) {
                        buildStyledParagraphText(
                            text = visiblePlainText,
                            inlineHighlights = inlineHighlights,
                            searchQuery = searchQuery,
                            searchHighlightBgColor = searchHighlightBgColor,
                            searchHighlightTextColor = searchHighlightTextColor,
                            isDarkTheme = theme.isDark
                        )
                    }
                    Text(
                        text = styled,
                        onTextLayout = { textLayoutResultRef.value = it },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = composeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (settings.fontSizeSp * 1.05f).sp,
                            lineHeight = (settings.fontSizeSp * 1.3f).sp
                        ),
                        color = textColor.copy(alpha = 0.92f),
                        textAlign = if (isCenteredBlock) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                // Blockquote
                normalizedBlock.startsWith("❝ ") -> {
                    val styled = remember(visiblePlainText, inlineHighlights, searchQuery, theme.isDark) {
                        buildStyledParagraphText(
                            text = visiblePlainText,
                            inlineHighlights = inlineHighlights,
                            searchQuery = searchQuery,
                            searchHighlightBgColor = searchHighlightBgColor,
                            searchHighlightTextColor = searchHighlightTextColor,
                            isDarkTheme = theme.isDark
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(accentColor, shape = RoundedCornerShape(2.dp))
                            )
                            Column {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = styled,
                                    onTextLayout = { textLayoutResultRef.value = it },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = composeFontFamily,
                                        fontSize = settings.fontSizeSp.sp,
                                        fontStyle = FontStyle.Italic,
                                        lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp
                                    ),
                                    color = textColor.copy(alpha = 0.9f),
                                    textAlign = effectiveAlign
                                )
                            }
                        }
                    }
                }

                // Bullet point
                normalizedBlock.startsWith("• ") -> {
                    val styled = remember(visiblePlainText, inlineHighlights, searchQuery, theme.isDark) {
                        buildStyledParagraphText(
                            text = visiblePlainText,
                            inlineHighlights = inlineHighlights,
                            searchQuery = searchQuery,
                            searchHighlightBgColor = searchHighlightBgColor,
                            searchHighlightTextColor = searchHighlightTextColor,
                            isDarkTheme = theme.isDark
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = (settings.fontSizeSp * 1.1f).sp
                            ),
                            color = accentColor
                        )
                        Text(
                            text = styled,
                            onTextLayout = { textLayoutResultRef.value = it },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = composeFontFamily,
                                fontSize = settings.fontSizeSp.sp,
                                lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp
                            ),
                            color = textColor,
                            textAlign = effectiveAlign,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Normal body paragraph (supports drop-cap + inline Word/Line/Custom highlights + search highlights)
                else -> {
                    val useDropCap = isFirstBodyParagraph && visiblePlainText.length > 20 &&
                        !isPassageHighlighted && !isCenteredBlock
                    val styledText = remember(
                        visiblePlainText,
                        inlineHighlights,
                        searchQuery,
                        theme.isDark,
                        useDropCap,
                        settings.fontSizeSp,
                        composeFontFamily
                    ) {
                        buildStyledParagraphText(
                            text = visiblePlainText,
                            inlineHighlights = inlineHighlights,
                            searchQuery = searchQuery,
                            searchHighlightBgColor = searchHighlightBgColor,
                            searchHighlightTextColor = searchHighlightTextColor,
                            isDarkTheme = theme.isDark,
                            dropCapStyle = if (useDropCap) {
                                SpanStyle(
                                    fontFamily = composeFontFamily,
                                    fontSize = (settings.fontSizeSp * 1.55f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            } else null
                        )
                    }

                    Text(
                        text = styledText,
                        onTextLayout = { textLayoutResultRef.value = it },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = composeFontFamily,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp
                        ),
                        color = textColor,
                        textAlign = effectiveAlign,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isSpeakingThis || isPassageHighlighted) 4.dp else 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookCompletedFooterCard(
    settings: ReaderSettings,
    bookTitle: String,
    onNavigateToLibrary: (() -> Unit)?
) {
    val theme = settings.theme
    val textColor = theme.textColor
    val accentColor = theme.accentColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("book_completed_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GreenSuccess.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GreenSuccess,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = "Book Completed!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
            Text(
                text = "You've finished '$bookTitle'.",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            if (onNavigateToLibrary != null) {
                Button(
                    onClick = onNavigateToLibrary,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Back to My Books", color = Color.White)
                }
            }
        }
    }
}

private fun findMatchingHighlight(
    highlights: List<BookHighlight>,
    word: String,
    line: String
): BookHighlight? {
    if (highlights.isEmpty()) return null
    // Prefer exact Word match, then Custom portion match, then Line match, then Passage highlight
    return highlights.firstOrNull {
        it.scope.equals("WORD", ignoreCase = true) && it.text.equals(word, ignoreCase = true)
    } ?: highlights.firstOrNull {
        it.scope.equals("CUSTOM", ignoreCase = true) &&
            (it.text.equals(word, ignoreCase = true) || it.text.contains(word, ignoreCase = true))
    } ?: highlights.firstOrNull {
        it.scope.equals("LINE", ignoreCase = true) &&
            (it.text.equals(line, ignoreCase = true) || it.text.contains(word, ignoreCase = true))
    } ?: highlights.firstOrNull {
        it.scope.equals("PASSAGE", ignoreCase = true)
    }
}

private fun extractWordAtOffset(text: String, charOffset: Int): String {
    if (text.isEmpty()) return ""
    val safeOffset = charOffset.coerceIn(0, text.length - 1)

    var probeOffset = safeOffset
    if (!text[probeOffset].isLetterOrDigit()) {
        if (probeOffset > 0 && text[probeOffset - 1].isLetterOrDigit()) {
            probeOffset--
        } else if (probeOffset < text.length - 1 && text[probeOffset + 1].isLetterOrDigit()) {
            probeOffset++
        }
    }

    var start = probeOffset
    while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '\'')) {
        start--
    }
    var end = probeOffset
    while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '\'')) {
        end++
    }
    val candidate = text.substring(start, end).trim().trim('.', ',', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']', '—', '-', '…')
    return candidate.ifBlank {
        text.split(Regex("\\s+")).firstOrNull { it.isNotBlank() }?.trim('.', ',', ';', ':', '!', '?', '"', '\'') ?: "Reading"
    }
}

private fun extractLineAtOffset(
    text: String,
    charOffset: Int,
    layoutResult: TextLayoutResult?
): String {
    if (text.isBlank()) return ""
    val safeOffset = charOffset.coerceIn(0, text.length - 1)

    if (layoutResult != null && layoutResult.lineCount > 0) {
        try {
            val lineIdx = layoutResult.getLineForOffset(safeOffset).coerceIn(0, layoutResult.lineCount - 1)
            val lineStart = layoutResult.getLineStart(lineIdx).coerceIn(0, text.length)
            val lineEnd = layoutResult.getLineEnd(lineIdx, visibleEnd = true).coerceIn(lineStart, text.length)
            val visualLine = text.substring(lineStart, lineEnd).trim()
            if (visualLine.isNotBlank()) {
                return visualLine
            }
        } catch (_: Throwable) {
            // Fall back to sentence extraction below
        }
    }

    // Fallback: extract containing sentence
    var start = safeOffset
    while (start > 0 && text[start - 1] !in charArrayOf('.', '!', '?', '\n')) {
        start--
    }
    var end = safeOffset
    while (end < text.length && text[end] !in charArrayOf('.', '!', '?', '\n')) {
        end++
    }
    if (end < text.length && text[end] in charArrayOf('.', '!', '?')) {
        end++
    }
    return text.substring(start, end).trim().ifBlank { text.trim() }
}

private fun buildStyledParagraphText(
    text: String,
    inlineHighlights: List<BookHighlight>,
    searchQuery: String,
    searchHighlightBgColor: Color,
    searchHighlightTextColor: Color,
    isDarkTheme: Boolean,
    dropCapStyle: SpanStyle? = null
): AnnotatedString {
    if (inlineHighlights.isEmpty() && searchQuery.isBlank() && dropCapStyle == null) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        append(text)

        if (dropCapStyle != null && text.isNotEmpty()) {
            addStyle(dropCapStyle, 0, 1)
        }

        // Apply Word & Line inline highlights cleanly without tinting the whole passage
        for (hl in inlineHighlights) {
            val snippet = hl.text.trim()
            if (snippet.isBlank()) continue
            val bgColor = Color(hl.colorHex).copy(alpha = if (isDarkTheme) 0.45f else 0.58f)
            val fgColor = if (isDarkTheme) Color.White else Color.Black
            val spanStyle = SpanStyle(
                background = bgColor,
                color = fgColor,
                fontWeight = if (hl.scope.equals("WORD", ignoreCase = true)) FontWeight.SemiBold else null
            )

            if (hl.scope.equals("WORD", ignoreCase = true)) {
                val wordRegex = Regex("\\b${Regex.escape(snippet)}\\b", RegexOption.IGNORE_CASE)
                val matches = wordRegex.findAll(text).toList()
                if (matches.isNotEmpty()) {
                    for (m in matches) {
                        addStyle(spanStyle, m.range.first, (m.range.last + 1).coerceAtMost(text.length))
                    }
                } else {
                    val idx = text.indexOf(snippet, ignoreCase = true)
                    if (idx >= 0) {
                        addStyle(spanStyle, idx, (idx + snippet.length).coerceAtMost(text.length))
                    }
                }
            } else {
                // LINE highlight
                var searchFrom = 0
                while (searchFrom < text.length) {
                    val idx = text.indexOf(snippet, searchFrom, ignoreCase = true)
                    if (idx < 0) break
                    val end = (idx + snippet.length).coerceAtMost(text.length)
                    addStyle(spanStyle, idx, end)
                    searchFrom = end
                }
            }
        }

        // Apply active in-book search query highlights on top
        if (searchQuery.isNotBlank()) {
            var startIndex = 0
            while (startIndex < text.length) {
                val matchIndex = text.indexOf(searchQuery, startIndex, ignoreCase = true)
                if (matchIndex == -1) break
                val endIndex = (matchIndex + searchQuery.length).coerceAtMost(text.length)
                addStyle(
                    SpanStyle(
                        background = searchHighlightBgColor,
                        color = searchHighlightTextColor,
                        fontWeight = FontWeight.Bold
                    ),
                    matchIndex,
                    endIndex
                )
                startIndex = endIndex
            }
        }
    }
}
