package com.serma.pubify.converter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import com.tom_roush.pdfbox.cos.COSObject
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.DefaultResourceCache
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

data class ConversionOutput(
    val epubFile: File,
    val chapterCount: Int,
    val imageCount: Int,
    val resolvedTitle: String = "",
    val resolvedAuthor: String = ""
)

/**
 * Lightweight PDFBox resource cache that avoids keeping decoded PDImageXObject streams
 * pinned across hundreds of pages, preventing OutOfMemoryError on image-heavy PDFs.
 */
private class LowMemoryResourceCache : DefaultResourceCache() {
    override fun put(indirect: COSObject?, xobject: PDXObject?) {
        if (xobject is PDImageXObject) return
        super.put(indirect, xobject)
    }
}

class PdfToEpubConverter(private val context: Context) {

    private val epubBuilder = EpubBuilder()

    suspend fun extractMetadata(uri: Uri): Result<PdfMetadata> = withContext(Dispatchers.IO) {
        var tempProbeFile: File? = null
        try {
            val fileName = getFileName(uri)
            val fileSizeBytes = getFileSize(uri)

            // Copy to a temporary cache file first: direct ContentResolver streams can fail
            // random-access PDFBox parsing on SAF / Drive / MediaStore providers.
            tempProbeFile = File(context.cacheDir, "meta_probe_${System.currentTimeMillis()}.pdf")
            val copied = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempProbeFile).use { output ->
                    inputStream.copyTo(output)
                }
                true
            } ?: false

            if (!copied || tempProbeFile.length() == 0L) {
                return@withContext Result.failure(Exception("Unable to open input stream for selected PDF"))
            }

            coroutineContext.ensureActive()

            val resolvedFileSize = if (fileSizeBytes > 0L) fileSizeBytes else tempProbeFile.length()

            PDDocument.load(tempProbeFile, MemoryUsageSetting.setupTempFileOnly()).use { document ->
                coroutineContext.ensureActive()
                document.resourceCache = LowMemoryResourceCache()
                val info = document.documentInformation
                val pageCount = document.numberOfPages
                val subject = info?.subject ?: ""
                val creator = info?.creator ?: ""

                // Extract outline/bookmarks (capped at 400 in preview so metadata loads fast even on huge textbooks)
                val outlineItems = mutableListOf<ExtractedOutlineItem>()
                try {
                    val outline = document.documentCatalog?.documentOutline
                    if (outline != null) {
                        extractOutlineBookmarks(outline, document, outlineItems, level = 0, maxItems = 400)
                    }
                } catch (_: Throwable) {}

                coroutineContext.ensureActive()

                // Multi-tier title & author resolution: XMP -> PDF Info -> Front Pages (1..10) -> Running Headers -> Outline -> Filename
                val identity = PdfMetadataExtractor.extract(
                    document = document,
                    fileName = fileName,
                    outlineItems = outlineItems,
                    tempDirForExtractor = File(context.cacheDir, "meta_probe")
                )

                coroutineContext.ensureActive()

                Result.success(
                    PdfMetadata(
                        fileName = fileName,
                        title = identity.title,
                        author = identity.author,
                        subject = subject,
                        creator = creator,
                        pageCount = pageCount,
                        fileSizeBytes = resolvedFileSize,
                        outlineItems = outlineItems
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val ex = if (e is Exception) e else Exception(e.message, e)
            Result.failure(ex)
        } finally {
            try { tempProbeFile?.delete() } catch (_: Throwable) {}
        }
    }

    suspend fun convert(
        uri: Uri,
        options: ConversionOptions,
        onProgress: (ConversionProgress) -> Unit
    ): Result<File> = convertWithSummary(uri, options, onProgress).map { it.epubFile }

    suspend fun convertWithSummary(
        uri: Uri,
        options: ConversionOptions,
        onProgress: (ConversionProgress) -> Unit
    ): Result<ConversionOutput> = withContext(Dispatchers.IO) {
        var tempPdfFile: File? = null
        val tempImagesDir = File(context.cacheDir, "epub_extract_imgs_${System.currentTimeMillis()}")
        var document: PDDocument? = null
        var createdOutputEpub: File? = null
        var completedSuccessfully = false

        try {
            coroutineContext.ensureActive()
            onProgress(
                ConversionProgress(
                    stage = ConversionStage.INITIALIZING,
                    message = "Opening document and preparing converter...",
                    percentage = 0.05f
                )
            )

            // Cache PDF to temp file for robust access and PdfRenderer cover extraction
            tempPdfFile = File(context.cacheDir, "input_convert_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempPdfFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Failed to read PDF file"))

            coroutineContext.ensureActive()

            document = PDDocument.load(tempPdfFile, MemoryUsageSetting.setupTempFileOnly())
            document.resourceCache = LowMemoryResourceCache()
            val totalPages = document.numberOfPages
            if (totalPages == 0) {
                return@withContext Result.failure(Exception("The PDF has 0 pages"))
            }

            val startPage = options.pageStart.coerceIn(1, totalPages)
            val endPage = if (options.pageEnd in startPage..totalPages) options.pageEnd else totalPages
            val pagesToConvert = (endPage - startPage) + 1

            onProgress(
                ConversionProgress(
                    stage = ConversionStage.ANALYZING_STRUCTURE,
                    currentPage = 0,
                    totalPages = pagesToConvert,
                    message = "Analyzing PDF outline, margins, headings & chapters...",
                    percentage = 0.10f
                )
            )

            // Extract full hierarchical outline bookmarks
            val allOutlineItems = mutableListOf<ExtractedOutlineItem>()
            try {
                val outline = document.documentCatalog?.documentOutline
                if (outline != null) {
                    extractOutlineBookmarks(outline, document, allOutlineItems, level = 0)
                }
            } catch (_: Exception) {}
            val rangeOutlines = allOutlineItems.filter { it.pageNumber in startPage..endPage }

            // Cover Image Generation
            var coverFile: File? = null
            if (options.extractCover) {
                onProgress(
                    ConversionProgress(
                        stage = ConversionStage.RENDERING_COVER,
                        currentPage = 0,
                        totalPages = pagesToConvert,
                        message = "Rendering book cover art (${options.imageCompression.displayName})...",
                        percentage = 0.15f
                    )
                )
                coverFile = renderCoverImage(
                    document,
                    startPage - 1,
                    options.imageCompression,
                    options.customImageQuality
                )
            }

            coroutineContext.ensureActive()

            // Extract lines (with font size, style, coordinates) and visually-accurate rendered images per page
            val extractor = PdfStructureExtractor(
                tempImageDir = tempImagesDir,
                compressionLevel = options.imageCompression,
                customImageQuality = options.customImageQuality
            )
            val pagesLines = linkedMapOf<Int, List<ExtractedPdfLine>>()
            val pagesImages = linkedMapOf<Int, List<ExtractedPdfImage>>()
            val pageRenderer = try {
                PDFRenderer(document)
            } catch (_: Throwable) {
                null
            }

            for (page in startPage..endPage) {
                coroutineContext.ensureActive()

                val currentProgressPct = 0.18f + (0.57f * ((page - startPage + 1).toFloat() / pagesToConvert))
                onProgress(
                    ConversionProgress(
                        stage = ConversionStage.EXTRACTING_PAGES,
                        currentPage = page - startPage + 1,
                        totalPages = pagesToConvert,
                        currentChapterTitle = "Page $page of $endPage",
                        percentage = currentProgressPct,
                        message = "Extracting text, headings & images (page $page of $endPage)..."
                    )
                )

                val lines = try {
                    extractor.extractPageLines(document, page)
                } catch (_: Throwable) {
                    emptyList()
                }
                coroutineContext.ensureActive()
                pagesLines[page] = lines
                pagesImages[page] = try {
                    extractor.extractPageImages(
                        document = document,
                        renderer = pageRenderer,
                        pageNumber = page,
                        pageLineCount = lines.size
                    )
                } catch (_: Throwable) {
                    emptyList()
                }
                coroutineContext.ensureActive()
            }

            coroutineContext.ensureActive()

            // Resolve final Book Title & Author before closing document if options left them blank/generic
            val sourceFileName = getFileName(uri)
            val needsAutoTitle = PdfMetadataExtractor.isGenericTitle(options.title)
            val needsAutoAuthor = PdfMetadataExtractor.isGenericAuthor(options.author)
            val autoIdentity = if (needsAutoTitle || needsAutoAuthor) {
                try {
                    PdfMetadataExtractor.extract(
                        document = document,
                        fileName = sourceFileName,
                        outlineItems = allOutlineItems,
                        preExtractedPagesLines = pagesLines,
                        tempDirForExtractor = tempImagesDir
                    )
                } catch (_: Throwable) {
                    null
                }
            } else {
                null
            }
            val finalBookTitle = if (!needsAutoTitle) {
                options.title.trim()
            } else {
                autoIdentity?.title ?: options.title.trim().ifBlank { "Untitled Book" }
            }
            val finalBookAuthor = if (!needsAutoAuthor) {
                options.author.trim()
            } else {
                autoIdentity?.author ?: options.author.trim().ifBlank { "Unknown Author" }
            }

            document.close()
            document = null

            onProgress(
                ConversionProgress(
                    stage = ConversionStage.ANALYZING_STRUCTURE,
                    currentPage = pagesToConvert,
                    totalPages = pagesToConvert,
                    percentage = 0.80f,
                    message = "Matching PDF chapters, sub-headings & layout margins..."
                )
            )

            // Normalize PDF bookmark outlines (preserving all sibling chapters)
            val normalizedPdfOutlines = PdfStructureExtractor.normalizeOutlineHierarchy(rangeOutlines, totalPages)

            // Always detect printed TOC pages so TOC rows never create fake chapters,
            // and reconcile embedded PDF bookmarks with the Printed TOC so missing chapters are recovered!
            val printedToc = extractor.extractPrintedToc(pagesLines, totalPages)
            val reconciledOutlines = reconcileOutlinesWithPrintedToc(
                pdfOutlines = normalizedPdfOutlines,
                printedTocOutlines = printedToc.items
            )

            // Analyze global document structure (margins, body font size, heading tiers, reading order)
            val structure = extractor.buildDocumentStructure(
                pagesLines = pagesLines,
                pagesImages = pagesImages,
                outlineItems = reconciledOutlines,
                stripHeadersFooters = options.stripHeadersFooters,
                totalPdfPages = totalPages,
                tocPages = printedToc.tocPages
            )

            // Final pass: recover any unmistakable explicit in-text chapters ("Chapter 7: ...")
            // that were missing from BOTH the PDF bookmark tree and the Printed TOC
            val finalOutlines = reconcileMissingBodyChapters(
                baseOutlines = reconciledOutlines,
                blocks = structure.blocks,
                tocPages = printedToc.tocPages
            )

            // Assemble chapters matching the full PDF Contents accurately
            val chapters = assembleChapters(
                blocks = structure.blocks,
                outlineItems = finalOutlines,
                options = options,
                startPage = startPage,
                endPage = endPage
            )

            // Compute final margins: match PDF margins when MATCH_PDF is selected
            val usePdfMargins = options.margin == EpubMargin.MATCH_PDF && options.customMarginPercent == null
            val marginLeft = if (usePdfMargins) structure.detectedMargins.leftPercent else options.effectiveHorizontalMargin
            val marginRight = if (usePdfMargins) structure.detectedMargins.rightPercent else options.effectiveHorizontalMargin
            val marginTop = if (usePdfMargins) structure.detectedMargins.topPercent else options.effectiveVerticalMargin
            val marginBottom = if (usePdfMargins) structure.detectedMargins.bottomPercent else options.effectiveVerticalMargin

            // Packaging EPUB
            coroutineContext.ensureActive()
            onProgress(
                ConversionProgress(
                    stage = ConversionStage.PACKAGING_EPUB,
                    currentPage = pagesToConvert,
                    totalPages = pagesToConvert,
                    percentage = 0.90f,
                    message = "Packaging EPUB with ${structure.images.size} images & ${chapters.size} chapters..."
                )
            )

            val epubDir = File(context.filesDir, "epubs").apply { mkdirs() }
            val sanitizedTitle = finalBookTitle.replace(Regex("[^a-zA-Z0-9_\\-\\.]"), "_").take(40).ifBlank { "Converted_Book" }
            val outputEpub = File(epubDir, "${sanitizedTitle}_${System.currentTimeMillis()}.epub")
            createdOutputEpub = outputEpub

            coroutineContext.ensureActive()

            epubBuilder.buildEpub(
                outputFile = outputEpub,
                title = finalBookTitle,
                author = finalBookAuthor,
                chapters = chapters,
                coverImageFile = coverFile,
                typography = options.typography,
                fontFamily = options.fontFamily,
                fontSizePt = options.fontSizePt,
                marginHorizontalPercent = options.effectiveHorizontalMargin,
                marginVerticalPercent = options.effectiveVerticalMargin,
                marginLeftPercent = marginLeft,
                marginRightPercent = marginRight,
                marginTopPercent = marginTop,
                marginBottomPercent = marginBottom,
                images = structure.images
            )

            coroutineContext.ensureActive()
            completedSuccessfully = true

            onProgress(
                ConversionProgress(
                    stage = ConversionStage.COMPLETED,
                    currentPage = pagesToConvert,
                    totalPages = pagesToConvert,
                    percentage = 1.0f,
                    message = "Conversion complete! Preserved ${chapters.size} chapters and ${structure.images.size} images."
                )
            )

            Result.success(
                ConversionOutput(
                    epubFile = outputEpub,
                    chapterCount = chapters.size.coerceAtLeast(1),
                    imageCount = structure.images.size,
                    resolvedTitle = finalBookTitle,
                    resolvedAuthor = finalBookAuthor
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val ex = if (e is Exception) e else Exception(e.message, e)
            onProgress(
                ConversionProgress(
                    stage = ConversionStage.ERROR,
                    percentage = 0f,
                    message = "Conversion failed: ${e.localizedMessage ?: e.javaClass.simpleName}",
                    errorMessage = e.localizedMessage ?: e.javaClass.simpleName
                )
            )
            Result.failure(ex)
        } finally {
            try {
                document?.close()
            } catch (_: Throwable) {}
            if (!completedSuccessfully) {
                try { createdOutputEpub?.delete() } catch (_: Throwable) {}
            }
            tempPdfFile?.delete()
            tempImagesDir.deleteRecursively()
        }
    }

    private fun assembleChapters(
        blocks: List<StructuredPdfBlock>,
        outlineItems: List<ExtractedOutlineItem>,
        options: ConversionOptions,
        startPage: Int,
        endPage: Int
    ): List<ConvertedChapter> {
        if (blocks.isEmpty()) {
            return listOf(
                ConvertedChapter(
                    index = 1,
                    title = options.title.ifBlank { "Chapter 1" },
                    htmlContent = "      <p class=\"flush\">No readable content was found in the selected page range.</p>\n",
                    startPage = startPage,
                    endPage = endPage
                )
            )
        }

        val chapters = mutableListOf<ConvertedChapter>()

        when (options.splitMode) {
            ChapterSplitMode.SINGLE_FILE -> {
                chapters.add(
                    ConvertedChapter(
                        index = 1,
                        title = options.title.ifBlank { "Complete Document" },
                        htmlContent = formatBlocksToHtml(blocks),
                        startPage = startPage,
                        endPage = endPage
                    )
                )
            }
            ChapterSplitMode.PER_PAGE -> {
                val byPage = blocks.groupBy { it.pageNumber }
                var idx = 1
                for (page in startPage..endPage) {
                    val pageBlocks = byPage[page].orEmpty()
                    if (pageBlocks.isNotEmpty() || idx == 1) {
                        val firstHeading = pageBlocks.firstOrNull {
                            it.type == PdfBlockType.H1 || it.type == PdfBlockType.H2
                        }?.text
                        val title = firstHeading ?: "Page $page"
                        val trimmedBlocks = if (firstHeading != null && pageBlocks.firstOrNull()?.text == firstHeading) {
                            pageBlocks.drop(1)
                        } else {
                            pageBlocks
                        }
                        chapters.add(
                            ConvertedChapter(
                                index = idx,
                                title = title,
                                htmlContent = formatBlocksToHtml(trimmedBlocks),
                                startPage = page,
                                endPage = page
                            )
                        )
                        idx++
                    }
                }
            }
            ChapterSplitMode.PAGE_INTERVAL -> {
                val step = options.pagesPerChapter.coerceAtLeast(1)
                var idx = 1
                var chStart = startPage
                while (chStart <= endPage) {
                    val chEnd = minOf(endPage, chStart + step - 1)
                    val chBlocks = blocks.filter { it.pageNumber in chStart..chEnd }
                    if (chBlocks.isNotEmpty() || idx == 1) {
                        val firstHeading = chBlocks.firstOrNull { it.type == PdfBlockType.H1 }?.text
                        val title = firstHeading ?: "Chapter $idx"
                        val trimmedBlocks = if (firstHeading != null && chBlocks.firstOrNull()?.text == firstHeading) {
                            chBlocks.drop(1)
                        } else {
                            chBlocks
                        }
                        chapters.add(
                            ConvertedChapter(
                                index = idx,
                                title = title,
                                htmlContent = formatBlocksToHtml(trimmedBlocks),
                                startPage = chStart,
                                endPage = chEnd
                            )
                        )
                        idx++
                    }
                    chStart = chEnd + 1
                }
            }
            ChapterSplitMode.AUTO_DETECT -> {
                // outlineItems are already normalized and reconciled (PDF Bookmarks + Printed TOC + explicit body chapters)
                val chapterOutlines = outlineItems
                    .filter { it.level == 0 }
                    .sortedBy { it.pageNumber }
                val subOutlinesByPage = outlineItems
                    .filter { it.level >= 1 }
                    .groupBy { it.pageNumber }
                val hasAuthoritativeOutlines = chapterOutlines.size >= 2

                // Chronological FIFO queue of ALL level-0 chapters so even chapters starting on
                // image-only/empty/skipped pages are NEVER skipped or merged into preceding chapters!
                val pendingOutlines = ArrayDeque<ExtractedOutlineItem>(chapterOutlines)

                // Pre-check which (page -> outline title) pairs have a direct heading match on that page
                val blocksByPage = blocks.groupBy { it.pageNumber }
                fun pageHasDirectHeadingMatch(pageNum: Int, item: ExtractedOutlineItem): Boolean {
                    return blocksByPage[pageNum].orEmpty().any { b ->
                        b.type != PdfBlockType.IMAGE &&
                            PdfStructureExtractor.titlesMatchFlexible(b.text, item.title)
                    }
                }

                val currentChapterBlocks = mutableListOf<StructuredPdfBlock>()
                var currentTitle = ""
                var currentStartPage = blocks.first().pageNumber
                var chapterIndex = 1
                var numberedChapterCounter = 1
                var lastHandledPage = -1

                fun injectMissingOutlineSubHeadings(
                    chBlocks: List<StructuredPdfBlock>,
                    chStartPg: Int,
                    chEndPg: Int
                ): List<StructuredPdfBlock> {
                    val relevantSubs = (chStartPg..chEndPg)
                        .flatMap { pg -> subOutlinesByPage[pg].orEmpty() }
                    if (relevantSubs.isEmpty()) return chBlocks

                    val mutable = chBlocks.toMutableList()
                    for (sub in relevantSubs) {
                        val normSub = PdfStructureExtractor.normalizeHeadingComparison(sub.title)
                        if (normSub.isBlank()) continue
                        val desiredType = when {
                            sub.level <= 1 -> PdfBlockType.H2
                            sub.level == 2 -> PdfBlockType.H3
                            else -> PdfBlockType.H4
                        }
                        val existingIdx = mutable.indexOfFirst { b ->
                            b.pageNumber == sub.pageNumber &&
                                b.type != PdfBlockType.IMAGE &&
                                PdfStructureExtractor.titlesMatchFlexible(b.text, sub.title)
                        }
                        if (existingIdx >= 0) {
                            val existing = mutable[existingIdx]
                            if (existing.type == PdfBlockType.H1 || existing.type == PdfBlockType.H2 ||
                                existing.type == PdfBlockType.H3 || existing.type == PdfBlockType.H4
                            ) {
                                mutable[existingIdx] = existing.copy(type = desiredType)
                            } else if (existing.type == PdfBlockType.PARAGRAPH && existing.text.length <= 140) {
                                mutable[existingIdx] = existing.copy(type = desiredType)
                            } else {
                                mutable.add(
                                    existingIdx,
                                    StructuredPdfBlock(
                                        pageNumber = sub.pageNumber,
                                        type = desiredType,
                                        text = sub.title
                                    )
                                )
                            }
                        } else {
                            // Insert the PDF Topic/Sub-topic cleanly at the start of its PDF page inside this chapter
                            val insertAt = mutable.indexOfFirst { it.pageNumber >= sub.pageNumber }
                                .let { if (it < 0) mutable.size else it }
                            mutable.add(
                                insertAt,
                                StructuredPdfBlock(
                                    pageNumber = sub.pageNumber,
                                    type = desiredType,
                                    text = sub.title
                                )
                            )
                        }
                    }
                    return mutable
                }

                fun flushChapter(endPg: Int, isFrontMatterSplit: Boolean = false, allowPlaceholderIfEmpty: Boolean = false) {
                    if (currentChapterBlocks.isEmpty()) {
                        if (!allowPlaceholderIfEmpty || currentTitle.isBlank()) return
                        currentChapterBlocks.add(
                            StructuredPdfBlock(
                                pageNumber = currentStartPage,
                                type = PdfBlockType.PARAGRAPH,
                                text = currentTitle,
                                isCentered = true
                            )
                        )
                    }
                    val resolvedTitle = when {
                        currentTitle.isNotBlank() -> currentTitle
                        isFrontMatterSplit -> {
                            val subTitle = currentChapterBlocks.firstOrNull {
                                it.type == PdfBlockType.H2 || it.type == PdfBlockType.H3
                            }?.text
                            if (!subTitle.isNullOrBlank() && subTitle.length <= 60) subTitle else "Front Matter"
                        }
                        chapterIndex == 1 && !hasAuthoritativeOutlines -> {
                            options.title.ifBlank { "Chapter 1" }
                        }
                        else -> "Chapter ${numberedChapterCounter++}"
                    }
                    val enrichedBlocks = injectMissingOutlineSubHeadings(
                        chBlocks = currentChapterBlocks,
                        chStartPg = currentStartPage,
                        chEndPg = endPg.coerceAtLeast(currentStartPage)
                    )
                    chapters.add(
                        ConvertedChapter(
                            index = chapterIndex,
                            title = resolvedTitle,
                            htmlContent = formatBlocksToHtml(enrichedBlocks),
                            startPage = currentStartPage,
                            endPage = endPg.coerceAtLeast(currentStartPage)
                        )
                    )
                    chapterIndex++
                    currentChapterBlocks.clear()
                    currentTitle = ""
                }

                fun openNewChapterFromOutline(outline: ExtractedOutlineItem, splitPage: Int) {
                    val normNew = PdfStructureExtractor.normalizeHeadingComparison(outline.title)
                    val normCurr = PdfStructureExtractor.normalizeHeadingComparison(currentTitle)
                    if (normNew.isNotBlank() && normNew == normCurr && currentChapterBlocks.isEmpty()) {
                        return
                    }
                    if (currentChapterBlocks.isNotEmpty()) {
                        flushChapter(
                            endPg = (splitPage - 1).coerceAtLeast(currentStartPage),
                            isFrontMatterSplit = (currentTitle.isBlank() && chapterIndex == 1)
                        )
                    } else if (currentTitle.isNotBlank() && normNew != normCurr) {
                        // Previous outline sat on a page with no extracted text/images before this chapter began
                        flushChapter(
                            endPg = currentStartPage,
                            isFrontMatterSplit = false,
                            allowPlaceholderIfEmpty = true
                        )
                    }
                    currentTitle = outline.title
                    currentStartPage = outline.pageNumber
                }

                for (block in blocks) {
                    var matchedNewOutline: ExtractedOutlineItem? = null
                    var consumeHeadingBlock = false

                    // 1. Drain ANY pending chapters that started on earlier pages (e.g., blank or image-only divider pages)
                    while (pendingOutlines.isNotEmpty() && pendingOutlines.first().pageNumber < block.pageNumber) {
                        val earlierOutline = pendingOutlines.removeFirst()
                        openNewChapterFromOutline(earlierOutline, splitPage = earlierOutline.pageNumber)
                    }

                    // 2. Check pending chapters scheduled on THIS page
                    if (pendingOutlines.isNotEmpty() && pendingOutlines.first().pageNumber == block.pageNumber) {
                        // Collect indices of all pending outlines on this exact page
                        var samePageCount = 0
                        while (samePageCount < pendingOutlines.size &&
                            pendingOutlines[samePageCount].pageNumber == block.pageNumber
                        ) {
                            samePageCount++
                        }

                        val matchingPos = if (block.type != PdfBlockType.IMAGE) {
                            (0 until samePageCount).firstOrNull { pos ->
                                PdfStructureExtractor.titlesMatchFlexible(block.text, pendingOutlines[pos].title)
                            } ?: -1
                        } else -1

                        if (matchingPos >= 0) {
                            // Open any earlier outlines on the same page that preceded this matched heading
                            repeat(matchingPos) {
                                val prior = pendingOutlines.removeFirst()
                                openNewChapterFromOutline(prior, splitPage = block.pageNumber)
                            }
                            matchedNewOutline = pendingOutlines.removeFirst()
                            consumeHeadingBlock = (
                                block.type == PdfBlockType.H1 ||
                                    block.type == PdfBlockType.H2 ||
                                    block.type == PdfBlockType.H3 ||
                                    (block.type == PdfBlockType.PARAGRAPH && block.text.length <= 130)
                                )
                        } else if (block.pageNumber != lastHandledPage &&
                            !pageHasDirectHeadingMatch(block.pageNumber, pendingOutlines.first())
                        ) {
                            matchedNewOutline = pendingOutlines.removeFirst()
                            consumeHeadingBlock = false
                        }
                    }

                    lastHandledPage = block.pageNumber

                    if (matchedNewOutline != null) {
                        val normNew = PdfStructureExtractor.normalizeHeadingComparison(matchedNewOutline.title)
                        val normCurr = PdfStructureExtractor.normalizeHeadingComparison(currentTitle)
                        if (normNew.isNotBlank() && normNew == normCurr && currentChapterBlocks.isEmpty()) {
                            // Repeated running header matching already-open empty chapter
                        } else {
                            openNewChapterFromOutline(matchedNewOutline, splitPage = block.pageNumber)
                        }
                    } else if (!hasAuthoritativeOutlines && block.type == PdfBlockType.H1) {
                        // ONLY split purely on H1 blocks when we do NOT have authoritative outlines/TOC
                        val normNew = PdfStructureExtractor.normalizeHeadingComparison(block.text)
                        val normCurr = PdfStructureExtractor.normalizeHeadingComparison(currentTitle)
                        if (normNew.isNotBlank() && normNew == normCurr) {
                            consumeHeadingBlock = true
                        } else if (currentChapterBlocks.isEmpty() && currentTitle.isBlank()) {
                            currentTitle = block.text
                            currentStartPage = block.pageNumber
                            consumeHeadingBlock = true
                        } else if (currentChapterBlocks.isNotEmpty()) {
                            val hasContentInCurrent = currentChapterBlocks.any {
                                it.type == PdfBlockType.PARAGRAPH || it.type == PdfBlockType.IMAGE ||
                                    it.type == PdfBlockType.LIST_ITEM || it.type == PdfBlockType.BLOCKQUOTE
                            }
                            if (hasContentInCurrent) {
                                flushChapter(
                                    endPg = block.pageNumber.coerceAtLeast(currentStartPage),
                                    isFrontMatterSplit = (currentTitle.isBlank() && chapterIndex == 1)
                                )
                                currentTitle = block.text
                                currentStartPage = block.pageNumber
                                consumeHeadingBlock = true
                            }
                        }
                    }

                    if (!consumeHeadingBlock) {
                        // Demote stray H1 inside an already-open chapter to H2 Topic so TOC shows it as a Topic
                        val normalizedBlock = if (block.type == PdfBlockType.H1) {
                            block.copy(type = PdfBlockType.H2)
                        } else {
                            block
                        }
                        currentChapterBlocks.add(normalizedBlock)
                    }
                }

                // Flush active chapter
                if (currentChapterBlocks.isNotEmpty() || chapters.isEmpty()) {
                    flushChapter(endPg = endPage)
                }

                // If any trailing outlines existed on final pages with no extracted blocks (e.g., end-matter art), preserve them cleanly
                while (pendingOutlines.isNotEmpty()) {
                    val trailing = pendingOutlines.removeFirst()
                    val alreadyPresent = chapters.any {
                        PdfStructureExtractor.titlesMatchFlexible(it.title, trailing.title)
                    }
                    if (!alreadyPresent) {
                        currentTitle = trailing.title
                        currentStartPage = trailing.pageNumber
                        flushChapter(endPg = trailing.pageNumber, allowPlaceholderIfEmpty = true)
                    }
                }
            }
        }

        return chapters
    }

    /**
     * Reconciles embedded PDF bookmark outlines with parsed Printed TOC entries so that
     * incomplete PDF bookmarks never cause missing chapters in the final EPUB Contents.
     */
    private fun reconcileOutlinesWithPrintedToc(
        pdfOutlines: List<ExtractedOutlineItem>,
        printedTocOutlines: List<ExtractedOutlineItem>
    ): List<ExtractedOutlineItem> {
        val pdfChapters = pdfOutlines.filter { it.level == 0 }
        val tocChapters = printedTocOutlines.filter { it.level == 0 }

        if (pdfChapters.size < 2 && tocChapters.size >= 2) return printedTocOutlines
        if (tocChapters.size < 2) return pdfOutlines

        // Choose the richer source as primary and merge any missing level-0 chapters from the other
        val useTocAsPrimary = tocChapters.size > pdfChapters.size + 1 ||
            (tocChapters.size > pdfChapters.size && pdfChapters.size <= 5)

        val primary = if (useTocAsPrimary) printedTocOutlines else pdfOutlines
        val secondary = if (useTocAsPrimary) pdfOutlines else printedTocOutlines

        val merged = primary.toMutableList()
        val primaryChapters = primary.filter { it.level == 0 }

        for (candidate in secondary) {
            if (candidate.level != 0) continue
            // Skip generic "Cover" / "Contents" / "Title Page" duplicates when merging secondary
            val normCand = PdfStructureExtractor.normalizeHeadingComparison(candidate.title)
            if (normCand.isBlank()) continue
            val isDuplicate = primaryChapters.any { existing ->
                PdfStructureExtractor.titlesMatchFlexible(existing.title, candidate.title) ||
                    (existing.pageNumber == candidate.pageNumber &&
                        PdfStructureExtractor.stripChapterNumberPrefix(existing.title) ==
                        PdfStructureExtractor.stripChapterNumberPrefix(candidate.title))
            }
            if (!isDuplicate) {
                merged.add(candidate)
            }
        }

        // Preserve hierarchical ordering while keeping all chapters in strict page order
        return merged.sortedWith(compareBy<ExtractedOutlineItem> { it.pageNumber }.thenBy { it.level })
    }

    /**
     * Scans structured body blocks for unmistakable explicit chapter headings ("Chapter 8",
     * "Part III", "Appendix A", "Epilogue", etc.) that were absent from both PDF bookmarks
     * and the Printed TOC, adding them so no chapter gets silently swallowed into the previous one.
     */
    private fun reconcileMissingBodyChapters(
        baseOutlines: List<ExtractedOutlineItem>,
        blocks: List<StructuredPdfBlock>,
        tocPages: Set<Int>
    ): List<ExtractedOutlineItem> {
        val existingChapters = baseOutlines.filter { it.level == 0 }
        val numberedChapterRegex = Regex("^\\d{1,2}\\.?\\s+[A-Z]\\S.*")
        val bookUsesNumberedChapters = existingChapters.count {
            numberedChapterRegex.matches(it.title.trim())
        } >= 2
        val merged = baseOutlines.toMutableList()
        val addedChapters = mutableListOf<ExtractedOutlineItem>()

        for (block in blocks) {
            if (block.pageNumber in tocPages) continue
            if (block.type != PdfBlockType.H1 && block.type != PdfBlockType.H2) continue
            val text = block.text.trim()
            if (text.length !in 3..120) continue
            val isExplicit = PdfStructureExtractor.isExplicitChapterHeading(text)
            val isNumberedH1 = bookUsesNumberedChapters &&
                block.type == PdfBlockType.H1 &&
                numberedChapterRegex.matches(text)
            if (!isExplicit && !isNumberedH1) continue

            // Avoid splitting on generic repeated section titles like "Notes" or "References" if already well-covered
            val allKnown = existingChapters + addedChapters
            val alreadyCovered = allKnown.any { existing ->
                PdfStructureExtractor.titlesMatchFlexible(existing.title, text) ||
                    (kotlin.math.abs(existing.pageNumber - block.pageNumber) <= 1 &&
                        (existing.pageNumber == block.pageNumber ||
                            PdfStructureExtractor.stripChapterNumberPrefix(existing.title) ==
                            PdfStructureExtractor.stripChapterNumberPrefix(text)))
            }
            if (!alreadyCovered) {
                val item = ExtractedOutlineItem(
                    title = text,
                    pageNumber = block.pageNumber,
                    level = 0
                )
                merged.add(item)
                addedChapters.add(item)
            }
        }

        return if (addedChapters.isEmpty()) {
            baseOutlines
        } else {
            merged.sortedWith(compareBy<ExtractedOutlineItem> { it.pageNumber }.thenBy { it.level })
        }
    }

    private fun formatBlocksToHtml(blocks: List<StructuredPdfBlock>): String {
        val html = StringBuilder()
        var inList = false
        var afterHeading = true
        var secIndex = 1

        fun closeListIfNeeded() {
            if (inList) {
                html.append("      </ul>\n")
                inList = false
            }
        }

        for (block in blocks) {
            when (block.type) {
                PdfBlockType.H1 -> {
                    closeListIfNeeded()
                    val secId = "sec_${secIndex++}"
                    val cls = if (block.isCentered) " class=\"pdf-h1 centered\"" else " class=\"pdf-h1\""
                    html.append("      <h2 id=\"$secId\"$cls>").append(escapeHtml(block.text)).append("</h2>\n")
                    afterHeading = true
                }
                PdfBlockType.H2 -> {
                    closeListIfNeeded()
                    val secId = "sec_${secIndex++}"
                    val cls = if (block.isCentered) " class=\"centered\"" else ""
                    html.append("      <h2 id=\"$secId\"$cls>").append(escapeHtml(block.text)).append("</h2>\n")
                    afterHeading = true
                }
                PdfBlockType.H3 -> {
                    closeListIfNeeded()
                    val secId = "sec_${secIndex++}"
                    val cls = if (block.isCentered) " class=\"centered\"" else ""
                    html.append("      <h3 id=\"$secId\"$cls>").append(escapeHtml(block.text)).append("</h3>\n")
                    afterHeading = true
                }
                PdfBlockType.H4 -> {
                    closeListIfNeeded()
                    val secId = "sec_${secIndex++}"
                    val cls = if (block.isCentered) " class=\"centered\"" else ""
                    html.append("      <h4 id=\"$secId\"$cls>").append(escapeHtml(block.text)).append("</h4>\n")
                    afterHeading = true
                }
                PdfBlockType.LIST_ITEM -> {
                    if (!inList) {
                        html.append("      <ul class=\"epub-list\">\n")
                        inList = true
                    }
                    val cleanedItem = block.text.replaceFirst(Regex("^[•\\-*▪◦]\\s*"), "").trim()
                    html.append("        <li>").append(escapeHtml(cleanedItem)).append("</li>\n")
                    afterHeading = false
                }
                PdfBlockType.BLOCKQUOTE -> {
                    closeListIfNeeded()
                    html.append("      <blockquote>").append(escapeHtml(block.text)).append("</blockquote>\n")
                    afterHeading = false
                }
                PdfBlockType.IMAGE -> {
                    closeListIfNeeded()
                    val asset = block.imageAsset
                    if (asset != null) {
                        val safeFrac = block.widthFractionOfContent.takeIf { it.isFinite() } ?: 1.0f
                        val widthPct = (safeFrac * 100f).roundToInt().coerceIn(25, 100)
                        html.append("      <figure class=\"epub-figure\">\n")
                        html.append("        <img src=\"images/${escapeHtml(asset.fileName)}\" alt=\"${escapeHtml(block.text)}\" style=\"width: ${widthPct}%; max-width: 100%; height: auto;\" />\n")
                        html.append("      </figure>\n")
                    }
                    afterHeading = false
                }
                PdfBlockType.PARAGRAPH -> {
                    closeListIfNeeded()
                    val cls = when {
                        block.isCentered -> " class=\"centered\""
                        block.isIndented && !afterHeading -> " class=\"indented\""
                        else -> " class=\"flush\""
                    }
                    html.append("      <p$cls>").append(escapeHtml(block.text)).append("</p>\n")
                    afterHeading = false
                }
            }
        }
        closeListIfNeeded()
        return html.toString()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun renderCoverImage(
        document: PDDocument,
        pageIndex: Int,
        compressionLevel: ImageCompressionLevel = ImageCompressionLevel.BALANCED,
        customQuality: Int? = null
    ): File? {
        return try {
            val totalPages = document.numberOfPages
            if (totalPages == 0) return null

            val targetIndex = pageIndex.coerceIn(0, totalPages - 1)
            val renderer = PDFRenderer(document)

            // Render cover page
            val sourceBitmap = renderer.renderImage(targetIndex, 1.0f) ?: return null

            // Ensure opaque white background for clean JPEG compression
            val finalBitmap = if (sourceBitmap.hasAlpha()) {
                val rgbBitmap = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(rgbBitmap)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(sourceBitmap, 0f, 0f, null)
                sourceBitmap.recycle()
                rgbBitmap
            } else {
                sourceBitmap
            }

            val quality = (customQuality ?: compressionLevel.qualityPercent).coerceIn(20, 100)
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
            FileOutputStream(coverFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            finalBitmap.recycle()
            coverFile
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Resolves the 1-based PDF page number for a bookmark outline item across all PDF destination formats:
     * direct PDPage references, PDActionGoTo actions, PDNamedDestination catalog lookups, and
     * 0-based page-index PDPageDestination objects.
     */
    private fun resolveOutlinePageNumber(item: PDOutlineItem, document: PDDocument): Int {
        // 1. Standard PDFBox helper
        try {
            val destPage = item.findDestinationPage(document)
            if (destPage != null) {
                val idx = document.pages.indexOf(destPage)
                if (idx >= 0) return idx + 1
            }
        } catch (_: Throwable) {}

        // 2. Inspect explicit Destination or GoTo Action
        val destinations = mutableListOf<PDDestination>()
        try {
            item.destination?.let { destinations.add(it) }
        } catch (_: Throwable) {}
        try {
            val action = item.action
            if (action is PDActionGoTo) {
                action.destination?.let { destinations.add(it) }
            }
        } catch (_: Throwable) {}

        for (rawDest in destinations) {
            try {
                val resolvedDest: PDDestination? = if (rawDest is PDNamedDestination) {
                    document.documentCatalog.findNamedDestinationPage(rawDest)
                } else {
                    rawDest
                }
                if (resolvedDest is PDPageDestination) {
                    resolvedDest.page?.let { page ->
                        val idx = document.pages.indexOf(page)
                        if (idx >= 0) return idx + 1
                    }
                    val retrievedZeroBased = resolvedDest.retrievePageNumber()
                    if (retrievedZeroBased in 0 until document.numberOfPages) {
                        return retrievedZeroBased + 1
                    }
                    val rawZeroBased = resolvedDest.pageNumber
                    if (rawZeroBased in 0 until document.numberOfPages) {
                        return rawZeroBased + 1
                    }
                }
            } catch (_: Throwable) {}
        }
        return -1
    }

    private fun extractOutlineBookmarks(
        node: PDOutlineNode,
        document: PDDocument,
        results: MutableList<ExtractedOutlineItem>,
        level: Int = 0,
        visited: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap()),
        maxItems: Int = 4000
    ) {
        if (level > 12 || results.size >= maxItems) return
        var current: PDOutlineItem? = node.firstChild
        while (current != null && results.size < maxItems) {
            if (!visited.add(current)) break
            val title = try {
                current.title?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
            } catch (_: Throwable) {
                ""
            }
            val directPage = resolveOutlinePageNumber(current, document)

            if (directPage > 0 && title.isNotBlank()) {
                results.add(ExtractedOutlineItem(title, directPage, level))
                if (current.hasChildren() && results.size < maxItems) {
                    extractOutlineBookmarks(current, document, results, level + 1, visited, maxItems)
                }
            } else if (current.hasChildren() && results.size < maxItems) {
                // Container bookmark (e.g., Chapter or Part folder with no direct destination of its own):
                // extract children first and inherit the first child's page number so it's never dropped!
                val insertIndex = results.size
                extractOutlineBookmarks(current, document, results, level + 1, visited, maxItems)
                if (title.isNotBlank() && results.size > insertIndex) {
                    val inheritedPage = results[insertIndex].pageNumber
                    if (inheritedPage > 0) {
                        results.add(insertIndex, ExtractedOutlineItem(title, inheritedPage, level))
                    }
                }
            }
            current = current.nextSibling
        }
    }

    private fun getFileName(uri: Uri): String {
        var name: String? = null
        try {
            if (uri.scheme == "content") {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            name = it.getString(index)
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        if (name.isNullOrBlank()) {
            val rawPath = try {
                Uri.decode(uri.lastPathSegment ?: uri.path ?: "")
            } catch (_: Throwable) {
                uri.lastPathSegment ?: uri.path ?: ""
            }
            name = rawPath.substringAfterLast('/').substringAfterLast(':')
        }
        return name?.takeIf { it.isNotBlank() } ?: "document.pdf"
    }

    private fun getFileSize(uri: Uri): Long {
        var size: Long = 0L
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (index >= 0) {
                        size = it.getLong(index)
                    }
                }
            }
        } catch (_: Exception) {}
        return size
    }
}
