package com.serma.pubify.reader

import android.graphics.BitmapFactory
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class PageSlice(
    val paragraphIndex: Int,
    val rawBlock: String,
    val textSlice: String,
    val isContinuation: Boolean = false
)

data class ReaderPage(
    val pageIndexInChapter: Int,
    val includesChapterHeader: Boolean,
    val includesChapterFooter: Boolean,
    val slices: List<PageSlice>
)

/**
 * Splits a [ReaderChapter] into exact screen-fitted pages for Moon+ Reader paginated flip mode,
 * measuring headings, paragraphs, blockquotes, lists, and embedded PDF/EPUB figures against the
 * live viewport width/height and typography settings.
 */
object ReaderPaginator {

    fun paginateChapter(
        chapter: ReaderChapter,
        settings: ReaderSettings,
        composeFontFamily: FontFamily,
        contentWidthPx: Int,
        contentHeightPx: Int,
        density: Density,
        textMeasurer: TextMeasurer,
        bookImages: Map<String, ByteArray>,
        isLastChapter: Boolean = false
    ): List<ReaderPage> {
        if (contentWidthPx <= 40 || contentHeightPx <= 80) {
            return listOf(
                ReaderPage(
                    pageIndexInChapter = 0,
                    includesChapterHeader = true,
                    includesChapterFooter = isLastChapter,
                    slices = chapter.paragraphs.mapIndexedNotNull { idx, raw ->
                        val t = raw.trim()
                        if (t.isBlank()) null else PageSlice(idx, t, t, false)
                    }
                )
            )
        }

        val safeWidth = contentWidthPx.coerceAtLeast(100)
        val safeHeight = contentHeightPx.coerceAtLeast(200)

        fun dpPx(dp: Float): Float = with(density) { dp.dp.toPx() }

        // Measure chapter header height
        val titleResult = textMeasurer.measure(
            text = AnnotatedString(chapter.title.ifBlank { "Chapter ${chapter.index + 1}" }),
            style = TextStyle(
                fontFamily = composeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (settings.fontSizeSp * 1.35f).sp,
                lineHeight = (settings.fontSizeSp * 1.6f).sp
            ),
            constraints = Constraints(maxWidth = safeWidth)
        )
        val headerHeightPx = dpPx(96f) + titleResult.size.height.toFloat()

        val pages = mutableListOf<ReaderPage>()
        var currentSlices = mutableListOf<PageSlice>()
        var currentPageIncludesHeader = true
        var usedHeightPx = headerHeightPx.coerceAtMost(safeHeight * 0.65f)

        fun commitPage(includesFooter: Boolean = false) {
            pages.add(
                ReaderPage(
                    pageIndexInChapter = pages.size,
                    includesChapterHeader = currentPageIncludesHeader,
                    includesChapterFooter = includesFooter,
                    slices = currentSlices.toList()
                )
            )
            currentSlices = mutableListOf()
            currentPageIncludesHeader = false
            usedHeightPx = 0f
        }

        val bodyStyle = TextStyle(
            fontFamily = composeFontFamily,
            fontSize = settings.fontSizeSp.sp,
            lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp
        )

        chapter.paragraphs.forEachIndexed { idx, raw ->
            val block = raw.trim()
            if (block.isEmpty()) return@forEachIndexed

            val normalized = if (block.startsWith("[[CENTER]]")) {
                block.removePrefix("[[CENTER]]").trim()
            } else {
                block
            }

            // Image block
            if (normalized.startsWith("[[IMG:") && normalized.endsWith("]]")) {
                val payload = normalized.removePrefix("[[IMG:").removeSuffix("]]")
                val parts = payload.split("|")
                val imgPath = parts.getOrNull(0).orEmpty()
                val altText = parts.getOrNull(1).orEmpty()
                val widthPct = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(25, 100) ?: 100

                val bytes = bookImages[imgPath] ?: bookImages[imgPath.substringAfterLast("/")]
                val imgHeightPx = estimateImageHeightPx(bytes, safeWidth, safeHeight, widthPct, altText, density)

                if (currentSlices.isNotEmpty() && usedHeightPx + imgHeightPx > safeHeight) {
                    commitPage(false)
                }
                currentSlices.add(PageSlice(idx, block, block, isContinuation = false))
                usedHeightPx += imgHeightPx
                return@forEachIndexed
            }

            // Text / Heading / List / Quote block
            val (displayText, style, extraVerticalPx, maxTextWidth) = when {
                normalized.startsWith("# ") -> BlockMeasureSpec(
                    text = normalized.removePrefix("# ").trim(),
                    style = TextStyle(
                        fontFamily = composeFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (settings.fontSizeSp * 1.42f).sp,
                        lineHeight = (settings.fontSizeSp * 1.65f).sp
                    ),
                    extraPx = dpPx(34f),
                    maxWidth = safeWidth
                )
                normalized.startsWith("## ") -> BlockMeasureSpec(
                    text = normalized.removePrefix("## ").trim(),
                    style = TextStyle(
                        fontFamily = composeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (settings.fontSizeSp * 1.25f).sp,
                        lineHeight = (settings.fontSizeSp * 1.45f).sp
                    ),
                    extraPx = dpPx(30f),
                    maxWidth = safeWidth
                )
                normalized.startsWith("### ") -> BlockMeasureSpec(
                    text = normalized.removePrefix("### ").trim(),
                    style = TextStyle(
                        fontFamily = composeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (settings.fontSizeSp * 1.13f).sp,
                        lineHeight = (settings.fontSizeSp * 1.35f).sp
                    ),
                    extraPx = dpPx(24f),
                    maxWidth = safeWidth
                )
                normalized.startsWith("#### ") -> BlockMeasureSpec(
                    text = normalized.removePrefix("#### ").trim(),
                    style = TextStyle(
                        fontFamily = composeFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (settings.fontSizeSp * 1.05f).sp,
                        lineHeight = (settings.fontSizeSp * 1.3f).sp
                    ),
                    extraPx = dpPx(18f),
                    maxWidth = safeWidth
                )
                normalized.startsWith("❝ ") -> BlockMeasureSpec(
                    text = normalized.removePrefix("❝ ").trim(),
                    style = bodyStyle.copy(fontStyle = FontStyle.Italic),
                    extraPx = dpPx(52f),
                    maxWidth = (safeWidth - dpPx(52f).roundToInt()).coerceAtLeast(80)
                )
                normalized.startsWith("• ") -> BlockMeasureSpec(
                    text = normalized.removePrefix("• ").trim(),
                    style = bodyStyle,
                    extraPx = dpPx(10f),
                    maxWidth = (safeWidth - dpPx(34f).roundToInt()).coerceAtLeast(80)
                )
                else -> BlockMeasureSpec(
                    text = normalized,
                    style = bodyStyle,
                    extraPx = dpPx(16f),
                    maxWidth = safeWidth
                )
            }

            if (displayText.isEmpty()) return@forEachIndexed

            val fullMeasure = textMeasurer.measure(
                text = AnnotatedString(displayText),
                style = style,
                constraints = Constraints(maxWidth = maxTextWidth)
            )
            val totalBlockHeightPx = fullMeasure.size.height.toFloat() + extraVerticalPx

            // Keep headings together with at least a little room if possible
            val isHeading = normalized.startsWith("#")
            if (isHeading && currentSlices.isNotEmpty() && usedHeightPx + totalBlockHeightPx + dpPx(44f) > safeHeight) {
                commitPage(false)
            }

            if (usedHeightPx + totalBlockHeightPx <= safeHeight) {
                currentSlices.add(PageSlice(idx, block, block, isContinuation = false))
                usedHeightPx += totalBlockHeightPx
                return@forEachIndexed
            }

            // Block does not fit completely on current page
            if (isHeading || normalized.startsWith("❝ ") || normalized.startsWith("• ")) {
                if (currentSlices.isNotEmpty()) {
                    commitPage(false)
                }
                currentSlices.add(PageSlice(idx, block, block, isContinuation = false))
                usedHeightPx += totalBlockHeightPx.coerceAtMost(safeHeight.toFloat())
                return@forEachIndexed
            }

            // Normal body paragraph: split cleanly on line boundaries across pages
            val isCentered = block.startsWith("[[CENTER]]")
            var remainingText = displayText
            var isCont = false

            while (remainingText.isNotEmpty()) {
                val measured = textMeasurer.measure(
                    text = AnnotatedString(remainingText),
                    style = style,
                    constraints = Constraints(maxWidth = maxTextWidth)
                )
                val fullRemHeight = measured.size.height.toFloat() + extraVerticalPx
                val availablePx = safeHeight - usedHeightPx

                if (fullRemHeight <= availablePx) {
                    val sliceRaw = if (isCentered && !isCont) "[[CENTER]]$remainingText" else remainingText
                    currentSlices.add(PageSlice(idx, block, sliceRaw, isContinuation = isCont))
                    usedHeightPx += fullRemHeight
                    break
                }

                // Count how many lines fit in remaining space
                val usableTextHeight = availablePx - extraVerticalPx
                var fitLineEnd = -1
                for (lineIdx in 0 until measured.lineCount) {
                    val lineBottom = measured.getLineBottom(lineIdx)
                    if (lineBottom <= usableTextHeight) {
                        fitLineEnd = lineIdx
                    } else {
                        break
                    }
                }

                // If fewer than 2 lines fit and current page already has content, flip to fresh page first
                if (fitLineEnd < 1 && currentSlices.isNotEmpty()) {
                    commitPage(false)
                    continue
                }

                val chosenLastLine = fitLineEnd.coerceAtLeast(0).coerceAtMost(measured.lineCount - 1)
                val splitOffset = measured.getLineEnd(chosenLastLine, visibleEnd = false)
                    .coerceIn(1, remainingText.length)

                val partText = remainingText.substring(0, splitOffset).trimEnd()
                val nextText = remainingText.substring(splitOffset).trimStart()

                if (partText.isNotEmpty()) {
                    val sliceRaw = if (isCentered && !isCont) "[[CENTER]]$partText" else partText
                    currentSlices.add(PageSlice(idx, block, sliceRaw, isContinuation = isCont))
                }

                if (nextText.isNotEmpty()) {
                    commitPage(false)
                    remainingText = nextText
                    isCont = true
                } else {
                    usedHeightPx += (measured.getLineBottom(chosenLastLine) + extraVerticalPx)
                    break
                }
            }
        }

        // Handle book completion footer only on the final chapter
        if (!isLastChapter) {
            if (currentSlices.isNotEmpty() || pages.isEmpty()) {
                commitPage(includesFooter = false)
            }
        } else {
            val footerEstimatePx = dpPx(170f)
            if (currentSlices.isEmpty() && pages.isEmpty()) {
                commitPage(includesFooter = true)
            } else if (usedHeightPx + footerEstimatePx <= safeHeight) {
                commitPage(includesFooter = true)
            } else {
                commitPage(includesFooter = false)
                pages.add(
                    ReaderPage(
                        pageIndexInChapter = pages.size,
                        includesChapterHeader = false,
                        includesChapterFooter = true,
                        slices = emptyList()
                    )
                )
            }
        }

        return pages
    }

    fun findPageForParagraph(pages: List<ReaderPage>, paragraphIndex: Int): Int {
        if (pages.isEmpty()) return 0
        val direct = pages.indexOfFirst { page ->
            page.slices.any { it.paragraphIndex == paragraphIndex }
        }
        if (direct >= 0) return direct
        val nearest = pages.indexOfFirst { page ->
            page.slices.any { it.paragraphIndex >= paragraphIndex }
        }
        return if (nearest >= 0) nearest else 0
    }

    private fun estimateImageHeightPx(
        bytes: ByteArray?,
        contentWidthPx: Int,
        contentHeightPx: Int,
        widthPct: Int,
        altText: String,
        density: Density
    ): Float {
        val maxAllowedHeight = (contentHeightPx * 0.62f).coerceAtLeast(160f)
        val extra = with(density) { (if (altText.isNotBlank() && !altText.startsWith("Figure from page")) 48.dp else 28.dp).toPx() }
        if (bytes == null) return with(density) { 180.dp.toPx() } + extra

        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                val targetW = contentWidthPx * (widthPct / 100f).coerceIn(0.35f, 1.0f)
                val aspectH = targetW * (opts.outHeight.toFloat() / opts.outWidth.toFloat())
                aspectH.coerceIn(with(density) { 64.dp.toPx() }, maxAllowedHeight) + extra
            } else {
                with(density) { 180.dp.toPx() } + extra
            }
        } catch (_: Throwable) {
            with(density) { 180.dp.toPx() } + extra
        }
    }

    private data class BlockMeasureSpec(
        val text: String,
        val style: TextStyle,
        val extraPx: Float,
        val maxWidth: Int
    )
}
