package com.example.converter

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
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import com.tom_roush.pdfbox.rendering.PDFRenderer
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

            val resolvedFileSize = if (fileSizeBytes > 0L) fileSizeBytes else tempProbeFile.length()

            PDDocument.load(tempProbeFile, MemoryUsageSetting.setupTempFileOnly()).use { document ->
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

                // Multi-tier title & author resolution: XMP -> PDF Info -> Front Pages (1..10) -> Running Headers -> Outline -> Filename
                val identity = PdfMetadataExtractor.extract(
                    document = document,
                    fileName = fileName,
                    outlineItems = outlineItems,
                    tempDirForExtractor = File(context.cacheDir, "meta_probe")
                )

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

        try {
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
            }

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

            // Normalize PDF bookmark outlines (unwrap Book Title / Part roots & rescue flat 1.1/1.1.1 outlines)
            val normalizedPdfOutlines = PdfStructureExtractor.normalizeOutlineHierarchy(rangeOutlines, totalPages)
            val pdfChapterCount = normalizedPdfOutlines.count { it.level == 0 }

            // Always detect printed TOC pages so TOC rows never create fake chapters,
            // and use printed TOC entries whenever embedded PDF outlines lack >= 2 chapters
            val printedToc = extractor.extractPrintedToc(pagesLines, totalPages)
            val resolvedOutlines = if (pdfChapterCount >= 2) {
                normalizedPdfOutlines
            } else if (printedToc.items.count { it.level == 0 } >= 2) {
                printedToc.items
            } else {
                normalizedPdfOutlines
            }

            // Analyze global document structure (margins, body font size, heading tiers, reading order)
            val structure = extractor.buildDocumentStructure(
                pagesLines = pagesLines,
                pagesImages = pagesImages,
                outlineItems = resolvedOutlines,
                stripHeadersFooters = options.stripHeadersFooters,
                totalPdfPages = totalPages,
                tocPages = printedToc.tocPages
            )

            // Assemble chapters matching PDF structure exactly
            val chapters = assembleChapters(
                blocks = structure.blocks,
                outlineItems = resolvedOutlines,
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
                val normalizedOutlines = PdfStructureExtractor.normalizeOutlineHierarchy(outlineItems, (endPage - startPage + 1))
                val chapterOutlines = normalizedOutlines.filter { it.level == 0 }
                val subOutlinesByPage = normalizedOutlines
                    .filter { it.level >= 1 }
                    .groupBy { it.pageNumber }
                val hasAuthoritativeOutlines = chapterOutlines.size >= 2

                val topOutlineQueueByPage = chapterOutlines
                    .groupBy { it.pageNumber }
                    .mapValues { it.value.toMutableList() }
                    .toMutableMap()

                // Pre-check which (page -> outline title) pairs have an exact heading match later on that page
                val blocksByPage = blocks.groupBy { it.pageNumber }
                fun pageHasDirectHeadingMatch(pageNum: Int, item: ExtractedOutlineItem): Boolean {
                    val normOutline = PdfStructureExtractor.normalizeHeadingComparison(item.title)
                    if (normOutline.isBlank()) return false
                    return blocksByPage[pageNum].orEmpty().any { b ->
                        b.type != PdfBlockType.IMAGE && run {
                            val nb = PdfStructureExtractor.normalizeHeadingComparison(b.text)
                            nb == normOutline || nb.startsWith(normOutline) || normOutline.startsWith(nb)
                        }
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
                            b.pageNumber == sub.pageNumber && b.type != PdfBlockType.IMAGE && run {
                                val nb = PdfStructureExtractor.normalizeHeadingComparison(b.text)
                                nb == normSub || nb.startsWith(normSub) || normSub.startsWith(nb)
                            }
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

                fun flushChapter(endPg: Int, isFrontMatterSplit: Boolean = false) {
                    if (currentChapterBlocks.isEmpty()) return
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
                        chEndPg = endPg
                    )
                    chapters.add(
                        ConvertedChapter(
                            index = chapterIndex,
                            title = resolvedTitle,
                            htmlContent = formatBlocksToHtml(enrichedBlocks),
                            startPage = currentStartPage,
                            endPage = endPg
                        )
                    )
                    chapterIndex++
                    currentChapterBlocks.clear()
                    currentTitle = ""
                }

                for (block in blocks) {
                    val pageQueue = topOutlineQueueByPage[block.pageNumber]
                    var matchedNewTitle: String? = null
                    var consumeHeadingBlock = false

                    if (pageQueue != null && pageQueue.isNotEmpty()) {
                        val normBlock = PdfStructureExtractor.normalizeHeadingComparison(block.text)
                        val matchingIdx = pageQueue.indexOfFirst { outlineItem ->
                            val normOutline = PdfStructureExtractor.normalizeHeadingComparison(outlineItem.title)
                            normOutline.isNotBlank() && block.type != PdfBlockType.IMAGE &&
                                (normBlock == normOutline || normBlock.startsWith(normOutline) || normOutline.startsWith(normBlock))
                        }

                        if (matchingIdx >= 0) {
                            matchedNewTitle = pageQueue.removeAt(matchingIdx).title
                            consumeHeadingBlock = (block.type == PdfBlockType.H1 || block.type == PdfBlockType.H2)
                        } else if (block.pageNumber != lastHandledPage && !pageHasDirectHeadingMatch(block.pageNumber, pageQueue.first())) {
                            if (currentChapterBlocks.isNotEmpty()) {
                                matchedNewTitle = pageQueue.removeAt(0).title
                                consumeHeadingBlock = false
                            } else if (currentTitle.isBlank()) {
                                currentTitle = pageQueue.removeAt(0).title
                            }
                        }
                    }

                    lastHandledPage = block.pageNumber

                    // ONLY split on H1 blocks when we do NOT have authoritative PDF/TOC outlines.
                    // When outlines exist, chapters match the exact outline count and extra headings stay Topics (H2).
                    if (!hasAuthoritativeOutlines && matchedNewTitle == null && block.type == PdfBlockType.H1) {
                        if (currentChapterBlocks.isEmpty() && currentTitle.isBlank()) {
                            currentTitle = block.text
                            consumeHeadingBlock = true
                        } else if (currentChapterBlocks.isNotEmpty()) {
                            val hasContentInCurrent = currentChapterBlocks.any {
                                it.type == PdfBlockType.PARAGRAPH || it.type == PdfBlockType.IMAGE ||
                                    it.type == PdfBlockType.LIST_ITEM || it.type == PdfBlockType.BLOCKQUOTE
                            }
                            if (hasContentInCurrent) {
                                matchedNewTitle = block.text
                                consumeHeadingBlock = true
                            }
                        }
                    }

                    if (matchedNewTitle != null) {
                        val normNew = PdfStructureExtractor.normalizeHeadingComparison(matchedNewTitle)
                        val normCurr = PdfStructureExtractor.normalizeHeadingComparison(currentTitle)
                        if (normNew.isNotBlank() && normNew == normCurr) {
                            // Ignore repeated running header matching the currently active chapter title
                            consumeHeadingBlock = true
                        } else {
                            if (currentChapterBlocks.isNotEmpty()) {
                                flushChapter(
                                    endPg = block.pageNumber.coerceAtLeast(currentStartPage),
                                    isFrontMatterSplit = (currentTitle.isBlank() && chapterIndex == 1)
                                )
                            }
                            currentTitle = matchedNewTitle
                            currentStartPage = block.pageNumber
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

                // Flush final chapter
                if (currentChapterBlocks.isNotEmpty() || chapters.isEmpty()) {
                    flushChapter(endPg = endPage)
                }
            }
        }

        return chapters
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
            try {
                val title = current.title
                val destPage = current.findDestinationPage(document)
                if (destPage != null) {
                    val pageNumber = document.pages.indexOf(destPage) + 1
                    if (pageNumber > 0 && !title.isNullOrBlank()) {
                        results.add(ExtractedOutlineItem(title.trim(), pageNumber, level))
                    }
                }
            } catch (_: Throwable) {}
            if (current.hasChildren() && results.size < maxItems) {
                extractOutlineBookmarks(current, document, results, level + 1, visited, maxItems)
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
