package com.serma.pubify.converter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.tom_roush.pdfbox.contentstream.PDFStreamEngine
import com.tom_roush.pdfbox.contentstream.operator.Operator
import com.tom_roush.pdfbox.contentstream.operator.OperatorProcessor
import com.tom_roush.pdfbox.contentstream.operator.state.Concatenate
import com.tom_roush.pdfbox.contentstream.operator.state.Restore
import com.tom_roush.pdfbox.contentstream.operator.state.Save
import com.tom_roush.pdfbox.contentstream.operator.state.SetGraphicsStateParameters
import com.tom_roush.pdfbox.contentstream.operator.state.SetMatrix
import com.tom_roush.pdfbox.cos.COSBase
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSNumber
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ExtractedPdfLine(
    val pageNumber: Int,
    val text: String,
    val leftX: Float,
    val rightX: Float,
    val topY: Float,
    val fontSizePt: Float,
    val isBold: Boolean,
    val isItalic: Boolean,
    val pageWidth: Float,
    val pageHeight: Float
)

data class ExtractedPdfImage(
    val pageNumber: Int,
    val leftX: Float,
    val topY: Float,
    val widthPt: Float,
    val heightPt: Float,
    val pageWidth: Float,
    val pageHeight: Float,
    val cosIdentityHash: Int,
    val asset: EpubImageAsset
)

enum class PdfBlockType {
    H1,
    H2,
    H3,
    H4,
    PARAGRAPH,
    BLOCKQUOTE,
    LIST_ITEM,
    IMAGE
}

data class StructuredPdfBlock(
    val pageNumber: Int,
    val type: PdfBlockType,
    val text: String = "",
    val isCentered: Boolean = false,
    val isIndented: Boolean = false,
    val imageAsset: EpubImageAsset? = null,
    val widthFractionOfContent: Float = 1.0f,
    val fontSizePt: Float = 11f
)

data class ExtractedDocumentStructure(
    val blocks: List<StructuredPdfBlock>,
    val images: List<EpubImageAsset>,
    val detectedMargins: PdfMarginInfo,
    val bodyFontSizePt: Float
)

data class PrintedTocResult(
    val items: List<ExtractedOutlineItem> = emptyList(),
    val tocPages: Set<Int> = emptySet()
)

/**
 * Extracts structured lines (with position, font size, bold/italic), visually-accurate rendered
 * figures & images (matching masked/composited images, charts, and vector diagrams in the PDF),
 * margins, and hierarchical headings/sub-headings from a PDF.
 */
class PdfStructureExtractor(
    private val tempImageDir: File,
    private val compressionLevel: ImageCompressionLevel,
    private val customImageQuality: Int?
) {

    private var imageCounter = 0

    fun extractPageLines(document: PDDocument, pageNumber: Int): List<ExtractedPdfLine> {
        return try {
            val page = document.getPage(pageNumber - 1)
            val cropBox = page.cropBox ?: page.mediaBox
            val pageWidth = cropBox?.width?.takeIf { it.isFinite() && it > 0f } ?: 595f
            val pageHeight = cropBox?.height?.takeIf { it.isFinite() && it > 0f } ?: 842f

            val stripper = LineExtractingTextStripper(pageNumber, pageWidth, pageHeight).apply {
                sortByPosition = true
                startPage = pageNumber
                endPage = pageNumber
            }
            stripper.getText(document)
            stripper.collectedLines
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun extractPageImages(document: PDDocument, pageNumber: Int): List<ExtractedPdfImage> {
        return extractPageImages(document, renderer = null, pageNumber = pageNumber, pageLineCount = 0)
    }

    /**
     * Extracts page images and vector diagrams so they visually match the PDF identically:
     * 1. Walks the page content stream to record exact bounding boxes for image XObjects,
     *    nested form XObjects, and non-trivial vector graphic figures (charts, diagrams, frames).
     * 2. Clusters overlapping/adjacent graphic elements within a tight gap into unified Figure rects.
     * 3. Renders the PDF page once via PDFRenderer (applying masks, color spaces, blending, clipping,
     *    vector strokes & labels accurately) and crops each Figure rect cleanly.
     * 4. Falls back to raw PDImageXObject bitmaps if PDFRenderer is unavailable or fails.
     */
    fun extractPageImages(
        document: PDDocument,
        renderer: PDFRenderer?,
        pageNumber: Int,
        pageLineCount: Int = 0
    ): List<ExtractedPdfImage> {
        val page = document.getPage(pageNumber - 1)
        val cropBox = page.cropBox ?: page.mediaBox
        val cropLowerX = cropBox?.lowerLeftX ?: 0f
        val cropLowerY = cropBox?.lowerLeftY ?: 0f
        val pageWidth = cropBox?.width?.takeIf { it > 0f } ?: 595f
        val pageHeight = cropBox?.height?.takeIf { it > 0f } ?: 842f

        val rawImagePlacements = mutableListOf<RawGraphicBox>()
        val rawVectorBoxes = mutableListOf<RawGraphicBox>()
        try {
            val engine = PageGraphicStreamEngine(
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                cropLowerX = cropLowerX,
                cropLowerY = cropLowerY,
                outImages = rawImagePlacements,
                outVectors = rawVectorBoxes
            )
            engine.processPage(page)
        } catch (_: Throwable) {
            // Handled by fallback below
        }

        // Resource fallback if stream engine found no image placements
        if (rawImagePlacements.isEmpty()) {
            try {
                collectResourceImagesFallback(page.resources, pageWidth, pageHeight, rawImagePlacements, depth = 0)
            } catch (_: Throwable) {}
        }

        // Filter noisy vector shapes (tiny specks, full-page frames, thin divider lines)
        val filteredVectors = rawVectorBoxes.filter { box ->
            val w = box.rightX - box.leftX
            val h = box.bottomY - box.topY
            val isFullPageFrame = w > pageWidth * 0.90f && h > pageHeight * 0.90f
            val isThinLine = w < 6f || h < 6f
            val isTooSmall = w < 18f && h < 18f
            !isFullPageFrame && !isThinLine && !isTooSmall
        }

        // Filter valid image placements
        val filteredImages = rawImagePlacements.filter { box ->
            val w = box.rightX - box.leftX
            val h = box.bottomY - box.topY
            val isTiny = w < 12f || h < 12f
            val imgObj = box.imageObject
            val smallSource = imgObj != null && (imgObj.width < 12 || imgObj.height < 12)
            // If a massive near-full-page backdrop image sits behind dense text, skip it (unless scanned page with sparse text)
            val isFullPageWatermarkBehindText = pageLineCount >= 6 &&
                w > pageWidth * 0.88f && h > pageHeight * 0.88f
            !isTiny && !smallSource && !isFullPageWatermarkBehindText
        }

        val combinedGraphics = filteredImages + filteredVectors
        if (combinedGraphics.isEmpty()) return emptyList()

        // Cluster overlapping/adjacent graphics (e.g., tiled image strips or multi-part vector diagrams)
        val clusters = clusterGraphicRegions(combinedGraphics, pageWidth, pageHeight)
            .filter { region ->
                val w = region.rightX - region.leftX
                val h = region.bottomY - region.topY
                w >= 16f && h >= 16f
            }
            .sortedBy { it.topY }

        if (clusters.isEmpty()) return emptyList()

        val maxPageDim = maxOf(pageWidth, pageHeight).coerceAtLeast(1f)
        val maxRenderPx = when (compressionLevel) {
            ImageCompressionLevel.LOW -> 1600f
            ImageCompressionLevel.BALANCED -> 1280f
            ImageCompressionLevel.HIGH -> 1000f
            ImageCompressionLevel.MAXIMUM -> 800f
        }
        val desiredScale = when (compressionLevel) {
            ImageCompressionLevel.LOW -> 1.9f
            ImageCompressionLevel.BALANCED -> 1.5f
            ImageCompressionLevel.HIGH -> 1.2f
            ImageCompressionLevel.MAXIMUM -> 0.95f
        }
        val safeScale = minOf(desiredScale, maxRenderPx / maxPageDim).coerceIn(0.65f, 2.0f)

        var renderedPageBitmap: Bitmap? = null
        if (renderer != null) {
            try {
                renderedPageBitmap = renderer.renderImage(pageNumber - 1, safeScale)
            } catch (_: OutOfMemoryError) {
                System.gc()
                renderedPageBitmap = try {
                    renderer.renderImage(pageNumber - 1, (safeScale * 0.55f).coerceAtLeast(0.5f))
                } catch (_: Throwable) {
                    null
                }
            } catch (_: Throwable) {
                renderedPageBitmap = null
            }
        }

        val extracted = mutableListOf<ExtractedPdfImage>()
        try {
            val validRendered = renderedPageBitmap != null && !renderedPageBitmap.isRecycled &&
                renderedPageBitmap.width >= 4 && renderedPageBitmap.height >= 4
            val scaleX = if (validRendered) renderedPageBitmap!!.width.toFloat() / pageWidth else 1f
            val scaleY = if (validRendered) renderedPageBitmap!!.height.toFloat() / pageHeight else 1f

            for (cluster in clusters) {
                try {
                    val widthPt = (cluster.rightX - cluster.leftX).takeIf { it.isFinite() }?.coerceIn(1f, pageWidth) ?: continue
                    val heightPt = (cluster.bottomY - cluster.topY).takeIf { it.isFinite() }?.coerceIn(1f, pageHeight) ?: continue

                    var croppedBitmap: Bitmap? = null
                    if (validRendered && cluster.usedExactStreamCoords) {
                        val bmp = renderedPageBitmap!!
                        val padPt = 2f
                        val cLeft = ((cluster.leftX - padPt).coerceIn(0f, pageWidth) * scaleX)
                            .takeIf { it.isFinite() }?.roundToInt()
                            ?.coerceIn(0, (bmp.width - 2).coerceAtLeast(0)) ?: 0
                        val cTop = ((cluster.topY - padPt).coerceIn(0f, pageHeight) * scaleY)
                            .takeIf { it.isFinite() }?.roundToInt()
                            ?.coerceIn(0, (bmp.height - 2).coerceAtLeast(0)) ?: 0
                        val cRight = ((cluster.rightX + padPt).coerceIn(0f, pageWidth) * scaleX)
                            .takeIf { it.isFinite() }?.roundToInt()
                            ?.coerceIn(cLeft + 1, bmp.width) ?: bmp.width
                        val cBottom = ((cluster.bottomY + padPt).coerceIn(0f, pageHeight) * scaleY)
                            .takeIf { it.isFinite() }?.roundToInt()
                            ?.coerceIn(cTop + 1, bmp.height) ?: bmp.height
                        val cropW = (cRight - cLeft).coerceAtLeast(1)
                        val cropH = (cBottom - cTop).coerceAtLeast(1)

                        if (cropW >= 12 && cropH >= 12 && cLeft + cropW <= bmp.width && cTop + cropH <= bmp.height) {
                            croppedBitmap = Bitmap.createBitmap(bmp, cLeft, cTop, cropW, cropH)
                        }
                    }

                    // If renderer crop failed or looked blank/solid, fall back to direct XObject bitmap when present
                    if (croppedBitmap != null && isSolidColorBitmap(croppedBitmap)) {
                        croppedBitmap.recycle()
                        croppedBitmap = null
                    }
                    var bitmap = croppedBitmap
                    if (bitmap == null && cluster.primaryImageObj != null) {
                        bitmap = try {
                            cluster.primaryImageObj.image
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    if (bitmap == null || bitmap.isRecycled || bitmap.width < 12 || bitmap.height < 12) {
                        bitmap?.takeIf { !it.isRecycled && it !== renderedPageBitmap }?.recycle()
                        continue
                    }

                    if (isSolidColorBitmap(bitmap)) {
                        if (bitmap !== renderedPageBitmap) bitmap.recycle()
                        continue
                    }

                    val asset = saveBitmapAsset(bitmap, pageNumber)
                    if (bitmap !== renderedPageBitmap) {
                        bitmap.recycle()
                    }

                    if (asset != null) {
                        extracted.add(
                            ExtractedPdfImage(
                                pageNumber = pageNumber,
                                leftX = cluster.leftX.coerceIn(0f, pageWidth),
                                topY = cluster.topY.coerceIn(0f, pageHeight),
                                widthPt = widthPt,
                                heightPt = heightPt,
                                pageWidth = pageWidth,
                                pageHeight = pageHeight,
                                cosIdentityHash = cluster.identitySignature,
                                asset = asset
                            )
                        )
                    }
                } catch (_: Throwable) {
                    // Continue extracting remaining images
                }
            }
        } finally {
            if (renderedPageBitmap != null && !renderedPageBitmap.isRecycled) {
                renderedPageBitmap.recycle()
            }
        }

        return extracted
    }

    private fun clusterGraphicRegions(
        boxes: List<RawGraphicBox>,
        pageWidth: Float,
        pageHeight: Float
    ): List<ClusteredGraphicRegion> {
        val remaining = boxes.toMutableList()
        val regions = mutableListOf<ClusteredGraphicRegion>()
        val mergeGapPt = 16f

        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            var left = seed.leftX
            var top = seed.topY
            var right = seed.rightX
            var bottom = seed.bottomY
            var primaryImg = seed.imageObject
            var exactCoords = seed.usedExactStreamCoords
            var hasImage = seed.imageObject != null
            var sigHash = seed.identitySignature

            var mergedAny = true
            while (mergedAny) {
                mergedAny = false
                val iter = remaining.iterator()
                while (iter.hasNext()) {
                    val candidate = iter.next()
                    val overlapsOrClose =
                        candidate.leftX <= right + mergeGapPt &&
                        candidate.rightX >= left - mergeGapPt &&
                        candidate.topY <= bottom + mergeGapPt &&
                        candidate.bottomY >= top - mergeGapPt

                    if (overlapsOrClose) {
                        val candLeft = min(left, candidate.leftX)
                        val candTop = min(top, candidate.topY)
                        val candRight = max(right, candidate.rightX)
                        val candBottom = max(bottom, candidate.bottomY)
                        val mergedW = candRight - candLeft
                        val mergedH = candBottom - candTop

                        // Guard against collapsing two separate figures across the whole page
                        if (mergedW <= pageWidth * 0.94f && mergedH <= pageHeight * 0.85f) {
                            left = candLeft
                            top = candTop
                            right = candRight
                            bottom = candBottom
                            if (primaryImg == null && candidate.imageObject != null) {
                                primaryImg = candidate.imageObject
                            }
                            hasImage = hasImage || (candidate.imageObject != null)
                            exactCoords = exactCoords && candidate.usedExactStreamCoords
                            sigHash = 31 * sigHash + candidate.identitySignature
                            iter.remove()
                            mergedAny = true
                        }
                    }
                }
            }

            // Skip vector-only boxes that contain neither an image nor substantial graphic area (e.g. small table borders without figures)
            val area = (right - left) * (bottom - top)
            if (hasImage || area >= (pageWidth * pageHeight * 0.015f)) {
                regions.add(
                    ClusteredGraphicRegion(
                        leftX = left.coerceIn(0f, pageWidth),
                        topY = top.coerceIn(0f, pageHeight),
                        rightX = right.coerceIn(0f, pageWidth),
                        bottomY = bottom.coerceIn(0f, pageHeight),
                        primaryImageObj = primaryImg,
                        identitySignature = sigHash,
                        usedExactStreamCoords = exactCoords
                    )
                )
            }
        }
        return regions
    }

    private fun collectResourceImagesFallback(
        resources: PDResources?,
        pageWidth: Float,
        pageHeight: Float,
        out: MutableList<RawGraphicBox>,
        depth: Int
    ) {
        if (resources == null || depth > 3) return
        for (name in resources.xObjectNames) {
            val xobj = try {
                resources.getXObject(name)
            } catch (_: Throwable) {
                null
            } ?: continue
            when (xobj) {
                is PDImageXObject -> {
                    val left = pageWidth * 0.1f
                    val top = (pageHeight * 0.2f + (out.size * 20f)).coerceAtMost(pageHeight * 0.7f)
                    val w = minOf(pageWidth * 0.8f, xobj.width.toFloat())
                    val h = minOf(pageHeight * 0.6f, xobj.height.toFloat())
                    out.add(
                        RawGraphicBox(
                            leftX = left,
                            topY = top,
                            rightX = left + w,
                            bottomY = top + h,
                            imageObject = xobj,
                            identitySignature = System.identityHashCode(xobj.cosObject),
                            usedExactStreamCoords = false
                        )
                    )
                }
                is PDFormXObject -> {
                    collectResourceImagesFallback(xobj.resources, pageWidth, pageHeight, out, depth + 1)
                }
            }
        }
    }

    private fun isSolidColorBitmap(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        val sampleCols = 7
        val sampleRows = 7
        val firstPixel = bitmap.getPixel(w / 2, h / 2)
        val r0 = Color.red(firstPixel)
        val g0 = Color.green(firstPixel)
        val b0 = Color.blue(firstPixel)
        val a0 = Color.alpha(firstPixel)

        for (ry in 1 until sampleRows) {
            val y = (h * ry) / sampleRows
            for (rx in 1 until sampleCols) {
                val x = (w * rx) / sampleCols
                val px = bitmap.getPixel(x, y)
                if (abs(Color.red(px) - r0) > 8 ||
                    abs(Color.green(px) - g0) > 8 ||
                    abs(Color.blue(px) - b0) > 8 ||
                    abs(Color.alpha(px) - a0) > 8
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun saveBitmapAsset(bitmap: Bitmap, pageNumber: Int): EpubImageAsset? {
        return try {
            tempImageDir.mkdirs()
            imageCounter++
            val maxDim = compressionLevel.maxDimension
            val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat()
                val newW = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
                val newH = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            } else {
                bitmap
            }

            val hasMeaningfulAlpha = hasTransparentPixels(scaled)
            val usePng = hasMeaningfulAlpha
            val ext = if (usePng) "png" else "jpg"
            val mediaType = if (usePng) "image/png" else "image/jpeg"
            val fileName = "img_p${pageNumber}_${imageCounter}.$ext"
            val outFile = File(tempImageDir, fileName)
            val quality = (customImageQuality ?: compressionLevel.qualityPercent).coerceIn(20, 100)

            val bitmapToSave = if (!usePng && scaled.hasAlpha()) {
                val opaque = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(opaque)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(scaled, 0f, 0f, null)
                opaque
            } else {
                scaled
            }

            FileOutputStream(outFile).use { fos ->
                if (usePng) {
                    bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, fos)
                } else {
                    bitmapToSave.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                }
            }

            if (bitmapToSave !== scaled && bitmapToSave !== bitmap) {
                bitmapToSave.recycle()
            }
            if (scaled !== bitmap) {
                scaled.recycle()
            }

            EpubImageAsset(
                id = "img_p${pageNumber}_${imageCounter}",
                fileName = fileName,
                file = outFile,
                mediaType = mediaType
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun hasTransparentPixels(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha()) return false
        val w = bitmap.width
        val h = bitmap.height
        val points = listOf(
            0 to 0,
            w - 1 to 0,
            0 to h - 1,
            w - 1 to h - 1,
            w / 2 to 0,
            0 to h / 2
        )
        return points.any { (x, y) ->
            Color.alpha(bitmap.getPixel(x.coerceIn(0, w - 1), y.coerceIn(0, h - 1))) < 245
        }
    }

    /**
     * Parses printed "Table of Contents" / "Contents" pages and resolves exact PDF page numbers
     * plus hierarchical Chapter (level 0), Topic (level 1), and Sub-topic (level 2) structure.
     */
    fun extractPrintedToc(
        pagesLines: Map<Int, List<ExtractedPdfLine>>,
        totalPages: Int
    ): PrintedTocResult {
        if (pagesLines.size < 3) return PrintedTocResult()
        val sortedPages = pagesLines.keys.sorted()
        val frontSearchPages = sortedPages.take(25)

        var tocStartPage = -1
        for (pg in frontSearchPages) {
            val lines = pagesLines[pg].orEmpty()
            val hasTocHeader = lines.any { line ->
                val t = line.text.trim()
                t.equals("Contents", ignoreCase = true) ||
                    t.equals("Table of Contents", ignoreCase = true) ||
                    t.equals("Brief Contents", ignoreCase = true) ||
                    t.equals("Detailed Contents", ignoreCase = true)
            }
            if (hasTocHeader) {
                tocStartPage = pg
                break
            }
        }
        if (tocStartPage < 0) return PrintedTocResult()

        data class RawTocEntry(
            val title: String,
            val printedPage: Int,
            val leftX: Float,
            val fontSizePt: Float,
            val isBold: Boolean
        )

        val tocCandidates = mutableListOf<RawTocEntry>()
        val detectedTocPages = mutableSetOf<Int>()
        var tocEndPage = tocStartPage
        val tocDottedOrWideRegex = Regex("""^(.+?)\s*(?:\.{2,}|·{2,}|_{2,}|\s{2,}|\t)\s*(\d{1,4})\s*$""")
        val simpleTrailingNumRegex = Regex("""^((?:Chapter|Part|Unit|Module|Lesson|Appendix|Section|\d+(?:\.\d+){0,3})[\s:.].+?)\s+(\d{1,4})\s*$""", RegexOption.IGNORE_CASE)

        for (pg in sortedPages.filter { it in tocStartPage..(tocStartPage + 12) }) {
            val lines = pagesLines[pg].orEmpty().sortedBy { it.topY }
            var foundOnPage = 0
            for (line in lines) {
                val t = line.text.trim()
                val m = tocDottedOrWideRegex.find(t) ?: simpleTrailingNumRegex.find(t)
                if (m != null) {
                    val entryTitle = m.groupValues[1].trim().trimEnd('.', '·', '_', ' ')
                    val printedPage = m.groupValues[2].toIntOrNull() ?: continue
                    if (entryTitle.length in 2..110 && printedPage in 1..totalPages) {
                        tocCandidates.add(
                            RawTocEntry(
                                title = entryTitle,
                                printedPage = printedPage,
                                leftX = line.leftX,
                                fontSizePt = line.fontSizePt,
                                isBold = line.isBold
                            )
                        )
                        foundOnPage++
                    }
                }
            }
            if (foundOnPage > 0) {
                detectedTocPages.add(pg)
                tocEndPage = pg
            } else if (pg > tocStartPage) {
                break
            }
        }

        if (tocCandidates.size < 2) {
            return PrintedTocResult(emptyList(), detectedTocPages)
        }

        // Resolve real PDF pages by matching titles on pages after tocEndPage, and estimating page-number offset
        val bodyFontSize = computeBodyFontSize(pagesLines.values.flatten())
        val searchPages = sortedPages.filter { it > tocEndPage }
        val resolvedOffsets = mutableListOf<Int>()
        val directMatches = mutableMapOf<Int, Int>()

        for ((idx, entry) in tocCandidates.withIndex()) {
            val normTarget = normalizeHeadingComparison(entry.title)
            if (normTarget.length < 3) continue

            val matchedPage = searchPages.firstOrNull { pg ->
                pagesLines[pg].orEmpty().any { line ->
                    val normLine = normalizeHeadingComparison(line.text)
                    val isProminent = line.fontSizePt >= bodyFontSize * 1.08f || line.isBold ||
                        isExplicitChapterStart(line.text)
                    isProminent && normLine.isNotBlank() &&
                        (normLine == normTarget || normLine.contains(normTarget) || normTarget.contains(normLine))
                }
            }
            if (matchedPage != null) {
                directMatches[idx] = matchedPage
                resolvedOffsets.add(matchedPage - entry.printedPage)
            }
        }

        if (resolvedOffsets.isEmpty()) {
            return PrintedTocResult(emptyList(), detectedTocPages)
        }
        val medianOffset = resolvedOffsets.sorted()[resolvedOffsets.size / 2]

        // Infer indentation tiers among TOC entries for non-numbered books
        val minLeftX = tocCandidates.minOf { it.leftX }
        val hasExplicitInToc = tocCandidates.any { isExplicitChapterStart(it.title) }

        val minContentPage = tocEndPage + 1
        val maxContentPage = sortedPages.lastOrNull() ?: return PrintedTocResult(emptyList(), detectedTocPages)
        if (minContentPage > maxContentPage) {
            return PrintedTocResult(emptyList(), detectedTocPages)
        }

        val rawItems = mutableListOf<ExtractedOutlineItem>()
        for ((idx, entry) in tocCandidates.withIndex()) {
            val pdfPage = (directMatches[idx] ?: (entry.printedPage + medianOffset))
                .coerceIn(minContentPage, maxContentPage)

            val level = when {
                isExplicitChapterStart(entry.title) -> 0
                SUBTOPIC_NUMBERED_PATTERN.matcher(entry.title).matches() -> 2
                TOPIC_NUMBERED_PATTERN.matcher(entry.title).matches() -> 1
                TOP_LEVEL_NUMBERED_PATTERN.matcher(entry.title).matches() && !hasExplicitInToc -> 0
                entry.leftX >= minLeftX + 26f -> 2
                entry.leftX >= minLeftX + 10f -> 1
                hasExplicitInToc -> 1
                else -> 0
            }
            rawItems.add(ExtractedOutlineItem(title = entry.title, pageNumber = pdfPage, level = level))
        }

        val normalized = normalizeOutlineHierarchy(
            outlineItems = rawItems.distinctBy { "${it.pageNumber}:${normalizeHeadingComparison(it.title)}" },
            totalPdfPages = totalPages
        )
        return PrintedTocResult(items = normalized, tocPages = detectedTocPages)
    }

    /**
     * Analyzes extracted lines & images across all pages to compute accurate margins,
     * filter running headers/footers, classify headings (H1-H4), sub-headings, lists,
     * blockquotes, merge 2-line chapter titles, and interleave images in top-down reading order.
     */
    fun buildDocumentStructure(
        pagesLines: Map<Int, List<ExtractedPdfLine>>,
        pagesImages: Map<Int, List<ExtractedPdfImage>>,
        outlineItems: List<ExtractedOutlineItem>,
        stripHeadersFooters: Boolean,
        totalPdfPages: Int,
        tocPages: Set<Int> = emptySet()
    ): ExtractedDocumentStructure {
        val allRawLines = pagesLines.values.flatten().filter { it.text.isNotBlank() }
        val preliminaryBodyFontSize = computeBodyFontSize(allRawLines)

        // 1. Identify repeating header/footer patterns (both whole-book & chapter-scoped) & repeated watermark logos
        val filteredLinesByPage = filterRunningHeadersFooters(
            pagesLines = pagesLines,
            stripHeadersFooters = stripHeadersFooters,
            totalPdfPages = totalPdfPages,
            bodyFontSize = preliminaryBodyFontSize
        )
        val filteredImagesByPage = filterRepeatedWatermarkImages(
            pagesImages = pagesImages,
            pagesLines = filteredLinesByPage,
            stripHeadersFooters = stripHeadersFooters
        )

        val contentLines = filteredLinesByPage.values.flatten().filter { it.text.isNotBlank() }

        // 2. Compute dominant body font size (mode weighted by character count)
        val bodyFontSize = computeBodyFontSize(contentLines.ifEmpty { allRawLines })

        // 3. Detect PDF margins from content lines & images
        val detectedMargins = detectMargins(filteredLinesByPage, filteredImagesByPage)

        // 4. Compute body geometry baselines (left margin, right edge, normal line gap)
        val bodyLines = contentLines.filter { abs(it.fontSizePt - bodyFontSize) <= 1.2f }
        val refLines = bodyLines.ifEmpty { contentLines }

        val bodyLeftMarginX = if (refLines.isNotEmpty()) {
            percentile(refLines.map { it.leftX }, 0.12f)
        } else 54f

        val bodyMaxRightX = if (refLines.isNotEmpty()) {
            percentile(refLines.map { it.rightX }, 0.88f)
        } else 540f

        val medianLineGap = computeMedianLineGap(filteredLinesByPage, bodyFontSize)

        // 5. Normalize outlines so Chapter = level 0, Topic = level 1, Sub-topic = level 2+
        val normalizedOutlines = normalizeOutlineHierarchy(outlineItems, totalPdfPages)
        val hasAuthoritativeOutlines = normalizedOutlines.count { it.level == 0 } >= 2
        val outlineByPage = normalizedOutlines.groupBy { it.pageNumber }

        // 6. Document-wide heading calibration to prevent 150 topic headings from becoming 150 fake chapters
        val nonTocLines = filteredLinesByPage
            .filterKeys { it !in tocPages }
            .values
            .flatten()
            .filter { it.text.isNotBlank() }

        val explicitChapterCount = nonTocLines.count { line ->
            val t = line.text.trim()
            val ratio = line.fontSizePt / bodyFontSize.coerceAtLeast(6f)
            t.length <= 95 && !isTocEntryLine(t) && isExplicitChapterStart(t) &&
                (ratio >= 1.04f || line.isBold)
        }
        val hasExplicitChapters = explicitChapterCount >= 2

        val calibratedH1MinFontPt = if (!hasAuthoritativeOutlines && !hasExplicitChapters) {
            calibrateChapterFontSizeThreshold(
                nonTocLines = nonTocLines,
                bodyFontSize = bodyFontSize,
                totalPdfPages = totalPdfPages
            )
        } else {
            Float.POSITIVE_INFINITY
        }

        val keptAssets = mutableListOf<EpubImageAsset>()
        val structuredBlocks = mutableListOf<StructuredPdfBlock>()

        val sortedPages = (filteredLinesByPage.keys + filteredImagesByPage.keys).toSortedSet()

        for (pageNum in sortedPages) {
            val pageLines = filteredLinesByPage[pageNum].orEmpty().sortedBy { it.topY }
            val pageImages = filteredImagesByPage[pageNum].orEmpty().sortedBy { it.topY }
            val pageOutlines = outlineByPage[pageNum].orEmpty()
            val isTocPage = pageNum in tocPages

            val pageWidth = pageLines.firstOrNull()?.pageWidth
                ?: pageImages.firstOrNull()?.pageWidth ?: 595f
            val contentWidth = (bodyMaxRightX - bodyLeftMarginX).coerceAtLeast(pageWidth * 0.5f)

            val elements = buildList {
                pageLines.forEach { add(PageElement.LineElem(it, it.topY)) }
                pageImages.forEach { add(PageElement.ImgElem(it, it.topY)) }
            }.sortedBy { it.sortY }

            val pendingParagraphLines = mutableListOf<ExtractedPdfLine>()
            var prevLine: ExtractedPdfLine? = null

            fun flushParagraph() {
                if (pendingParagraphLines.isEmpty()) return
                val mergedText = joinLinesSmart(pendingParagraphLines)
                if (mergedText.isNotBlank()) {
                    val firstLine = pendingParagraphLines.first()
                    val minLeft = pendingParagraphLines.minOf { it.leftX }
                    val maxRight = pendingParagraphLines.maxOf { it.rightX }
                    val avgSize = pendingParagraphLines.map { it.fontSizePt }.average().toFloat()

                    val isIndentedFirst = firstLine.leftX > bodyLeftMarginX + bodyFontSize * 0.9f
                    val isBlockquote = pendingParagraphLines.size >= 2 &&
                        minLeft > bodyLeftMarginX + bodyFontSize * 1.4f &&
                        maxRight < bodyMaxRightX - bodyFontSize * 1.0f

                    val pageCenter = pageWidth / 2f
                    val isCentered = pendingParagraphLines.size <= 3 &&
                        pendingParagraphLines.all { line ->
                            val cx = (line.leftX + line.rightX) / 2f
                            abs(cx - pageCenter) < pageWidth * 0.08f &&
                                (line.rightX - line.leftX) < contentWidth * 0.82f
                        }

                    val blockType = when {
                        isListItem(mergedText) -> PdfBlockType.LIST_ITEM
                        isBlockquote -> PdfBlockType.BLOCKQUOTE
                        else -> PdfBlockType.PARAGRAPH
                    }

                    structuredBlocks.add(
                        StructuredPdfBlock(
                            pageNumber = pageNum,
                            type = blockType,
                            text = mergedText,
                            isCentered = isCentered,
                            isIndented = isIndentedFirst && blockType == PdfBlockType.PARAGRAPH && !isCentered,
                            fontSizePt = avgSize
                        )
                    )
                }
                pendingParagraphLines.clear()
            }

            for (elem in elements) {
                when (elem) {
                    is PageElement.ImgElem -> {
                        flushParagraph()
                        prevLine = null
                        keptAssets.add(elem.image.asset)
                        val widthFrac = (elem.image.widthPt / contentWidth).coerceIn(0.25f, 1.0f)
                        structuredBlocks.add(
                            StructuredPdfBlock(
                                pageNumber = pageNum,
                                type = PdfBlockType.IMAGE,
                                text = "Figure from page $pageNum",
                                isCentered = true,
                                imageAsset = elem.image.asset,
                                widthFractionOfContent = widthFrac
                            )
                        )
                    }
                    is PageElement.LineElem -> {
                        val line = elem.line
                        val trimmed = line.text.trim()
                        if (trimmed.isEmpty()) continue

                        val gapAbove = prevLine?.let { line.topY - it.topY } ?: (medianLineGap * 1.6f)
                        val headingLevel = classifyHeadingLevel(
                            line = line,
                            bodyFontSize = bodyFontSize,
                            gapAbove = gapAbove,
                            medianLineGap = medianLineGap,
                            pageOutlines = pageOutlines,
                            hasAuthoritativeOutlines = hasAuthoritativeOutlines,
                            hasExplicitChapters = hasExplicitChapters,
                            calibratedH1MinFontPt = calibratedH1MinFontPt,
                            isTocPage = isTocPage
                        )

                        // Handle 2-line chapter header ("CHAPTER 1" followed by "The Journey Begins" at larger/bold font)
                        val lastBlock = structuredBlocks.lastOrNull()
                        val canMergeTwoLineChapterTitle = prevLine != null &&
                            pendingParagraphLines.isEmpty() &&
                            lastBlock != null &&
                            lastBlock.pageNumber == pageNum &&
                            lastBlock.type == PdfBlockType.H1 &&
                            isBareChapterOrPartLabel(lastBlock.text) &&
                            !isExplicitChapterStart(trimmed) &&
                            !isListItem(trimmed) &&
                            trimmed.length <= 95 &&
                            !endsWithSentencePunctuation(trimmed) &&
                            gapAbove <= maxOf(medianLineGap * 3.4f, line.fontSizePt * 2.8f) &&
                            (headingLevel != null || line.isBold || line.fontSizePt >= bodyFontSize * 1.15f)

                        if (canMergeTwoLineChapterTitle) {
                            val prefix = lastBlock!!.text.trimEnd(':', '-', ' ')
                            structuredBlocks[structuredBlocks.lastIndex] = lastBlock.copy(
                                text = "$prefix: $trimmed",
                                fontSizePt = maxOf(lastBlock.fontSizePt, line.fontSizePt)
                            )
                            prevLine = line
                            continue
                        }

                        if (headingLevel != null) {
                            val canMergeWithPrevHeading = prevLine != null &&
                                pendingParagraphLines.isEmpty() &&
                                lastBlock != null &&
                                lastBlock.pageNumber == pageNum &&
                                lastBlock.type == headingLevel &&
                                abs(line.fontSizePt - lastBlock.fontSizePt) <= 1.0f &&
                                gapAbove <= maxOf(medianLineGap * 1.75f, line.fontSizePt * 1.9f) &&
                                lastBlock.text.length + trimmed.length < 130 &&
                                !isExplicitChapterStart(trimmed)

                            if (canMergeWithPrevHeading) {
                                structuredBlocks[structuredBlocks.lastIndex] = lastBlock!!.copy(
                                    text = "${lastBlock.text} $trimmed"
                                )
                            } else {
                                flushParagraph()
                                val lineCenter = (line.leftX + line.rightX) / 2f
                                val isCentered = abs(lineCenter - (pageWidth / 2f)) < pageWidth * 0.09f &&
                                    (line.rightX - line.leftX) < contentWidth * 0.88f

                                structuredBlocks.add(
                                    StructuredPdfBlock(
                                        pageNumber = pageNum,
                                        type = headingLevel,
                                        text = trimmed,
                                        isCentered = isCentered,
                                        fontSizePt = line.fontSizePt
                                    )
                                )
                            }
                            prevLine = line
                            continue
                        }

                        val startsNewParagraph = if (pendingParagraphLines.isEmpty() || prevLine == null) {
                            false
                        } else {
                            val p = prevLine!!
                            val largeVerticalGap = gapAbove > medianLineGap * 1.38f
                            val indentJump = line.leftX > bodyLeftMarginX + bodyFontSize * 0.9f &&
                                p.leftX <= bodyLeftMarginX + bodyFontSize * 0.6f
                            val prevEndedShortAndTerminal = p.rightX < bodyMaxRightX - bodyFontSize * 4.0f &&
                                endsWithSentencePunctuation(p.text.trim())
                            val startsList = isListItem(trimmed)
                            val alignmentJump = abs(((line.leftX + line.rightX) / 2f) - ((p.leftX + p.rightX) / 2f)) > pageWidth * 0.15f &&
                                p.rightX < bodyMaxRightX - bodyFontSize * 3.0f

                            largeVerticalGap || indentJump || prevEndedShortAndTerminal || startsList || alignmentJump
                        }

                        if (startsNewParagraph) {
                            flushParagraph()
                        }
                        pendingParagraphLines.add(line)
                        prevLine = line
                    }
                }
            }
            flushParagraph()
        }

        return ExtractedDocumentStructure(
            blocks = structuredBlocks,
            images = keptAssets,
            detectedMargins = detectedMargins,
            bodyFontSizePt = bodyFontSize
        )
    }

    private fun filterRunningHeadersFooters(
        pagesLines: Map<Int, List<ExtractedPdfLine>>,
        stripHeadersFooters: Boolean,
        totalPdfPages: Int,
        bodyFontSize: Float = 11f
    ): Map<Int, List<ExtractedPdfLine>> {
        if (!stripHeadersFooters) return pagesLines

        val pageNumPattern = Regex("^\\s*(?:Page\\s+)?\\d+(?:\\s*(?:of|/)\\s*\\d+)?\\s*$", RegexOption.IGNORE_CASE)
        val normalizedHeaderCounts = mutableMapOf<String, Int>()
        val normalizedFooterCounts = mutableMapOf<String, Int>()
        val smallHeaderCounts = mutableMapOf<String, Int>()
        val smallFooterCounts = mutableMapOf<String, Int>()

        for ((_, lines) in pagesLines) {
            if (lines.isEmpty()) continue
            val sorted = lines.sortedBy { it.topY }
            val pageHeight = sorted.first().pageHeight
            val seenHeaderOnPage = mutableSetOf<String>()
            val seenFooterOnPage = mutableSetOf<String>()

            for (line in sorted) {
                val trimmed = line.text.trim()
                if (trimmed.isEmpty()) continue
                val norm = trimmed.replace(Regex("\\d+"), "#").lowercase()
                val isSmallOrBody = line.fontSizePt <= bodyFontSize * 1.12f
                if (line.topY < pageHeight * 0.11f && trimmed.length <= 95 && seenHeaderOnPage.add(norm)) {
                    normalizedHeaderCounts[norm] = (normalizedHeaderCounts[norm] ?: 0) + 1
                    if (isSmallOrBody) {
                        smallHeaderCounts[norm] = (smallHeaderCounts[norm] ?: 0) + 1
                    }
                } else if (line.topY > pageHeight * 0.89f && trimmed.length <= 95 && seenFooterOnPage.add(norm)) {
                    normalizedFooterCounts[norm] = (normalizedFooterCounts[norm] ?: 0) + 1
                    if (isSmallOrBody) {
                        smallFooterCounts[norm] = (smallFooterCounts[norm] ?: 0) + 1
                    }
                }
            }
        }

        val pageCount = pagesLines.size
        val minBookRepeats = maxOf(3, (pageCount * 0.35f).roundToInt())
        // Chapter-scoped running headers (e.g. "Chapter 1: Foundations" repeated across 10-20 pages of a 200-page book)
        val minChapterHeaderRepeats = if (pageCount >= 6) 3 else Int.MAX_VALUE

        return pagesLines.mapValues { (pageNum, lines) ->
            val sorted = lines.sortedBy { it.topY }
            sorted.filterIndexed { idx, line ->
                val trimmed = line.text.trim()
                if (trimmed.isEmpty()) return@filterIndexed false
                val isTopZone = line.topY < line.pageHeight * 0.11f || idx == 0
                val isBottomZone = line.topY > line.pageHeight * 0.89f || idx == sorted.lastIndex

                if (isTopZone || isBottomZone) {
                    if (pageNumPattern.matches(trimmed)) return@filterIndexed false
                    if (trimmed == pageNum.toString() || trimmed == "$pageNum / $totalPdfPages") return@filterIndexed false
                }

                val norm = trimmed.replace(Regex("\\d+"), "#").lowercase()
                val isSmallOrBody = line.fontSizePt <= bodyFontSize * 1.12f
                if (line.topY < line.pageHeight * 0.11f) {
                    if ((normalizedHeaderCounts[norm] ?: 0) >= minBookRepeats) return@filterIndexed false
                    if (isSmallOrBody && (smallHeaderCounts[norm] ?: 0) >= minChapterHeaderRepeats) return@filterIndexed false
                }
                if (line.topY > line.pageHeight * 0.89f) {
                    if ((normalizedFooterCounts[norm] ?: 0) >= minBookRepeats) return@filterIndexed false
                    if (isSmallOrBody && (smallFooterCounts[norm] ?: 0) >= minChapterHeaderRepeats) return@filterIndexed false
                }
                true
            }
        }
    }

    private fun filterRepeatedWatermarkImages(
        pagesImages: Map<Int, List<ExtractedPdfImage>>,
        pagesLines: Map<Int, List<ExtractedPdfLine>>,
        stripHeadersFooters: Boolean
    ): Map<Int, List<ExtractedPdfImage>> {
        if (!stripHeadersFooters || pagesImages.size < 3) return pagesImages

        val hashPageCount = mutableMapOf<Int, MutableSet<Int>>()
        for ((pageNum, imgs) in pagesImages) {
            for (img in imgs) {
                val topFrac = img.topY / img.pageHeight
                if (topFrac < 0.12f || topFrac > 0.85f || (img.widthPt < img.pageWidth * 0.2f && img.heightPt < img.pageHeight * 0.12f)) {
                    hashPageCount.getOrPut(img.cosIdentityHash) { mutableSetOf() }.add(pageNum)
                }
            }
        }

        val minRepeats = maxOf(3, (pagesLines.size * 0.45f).roundToInt())
        val repeatedWatermarks = hashPageCount.filter { it.value.size >= minRepeats }.keys

        return pagesImages.mapValues { (_, imgs) ->
            imgs.filter { img ->
                if (img.cosIdentityHash in repeatedWatermarks) {
                    img.asset.file.delete()
                    false
                } else {
                    true
                }
            }
        }
    }

    private fun computeBodyFontSize(lines: List<ExtractedPdfLine>): Float {
        if (lines.isEmpty()) return 11f
        val bucketWeights = mutableMapOf<Float, Int>()
        for (line in lines) {
            val pt = line.fontSizePt.takeIf { it.isFinite() } ?: continue
            val bucket = (pt * 2f).roundToInt() / 2f
            if (bucket in 6f..36f) {
                bucketWeights[bucket] = (bucketWeights[bucket] ?: 0) + line.text.trim().length
            }
        }
        return bucketWeights.maxByOrNull { it.value }?.key ?: 11f
    }

    private fun detectMargins(
        pagesLines: Map<Int, List<ExtractedPdfLine>>,
        pagesImages: Map<Int, List<ExtractedPdfImage>>
    ): PdfMarginInfo {
        val leftPercents = mutableListOf<Float>()
        val rightPercents = mutableListOf<Float>()
        val topPercents = mutableListOf<Float>()
        val bottomPercents = mutableListOf<Float>()

        for ((pageNum, lines) in pagesLines) {
            val nonEmpty = lines.filter {
                it.text.isNotBlank() && it.leftX.isFinite() && it.rightX.isFinite() &&
                    it.topY.isFinite() && it.fontSizePt.isFinite() &&
                    it.pageWidth.isFinite() && it.pageWidth > 0f &&
                    it.pageHeight.isFinite() && it.pageHeight > 0f
            }
            if (nonEmpty.isNotEmpty()) {
                val pw = nonEmpty.first().pageWidth
                val ph = nonEmpty.first().pageHeight
                for (l in nonEmpty) {
                    leftPercents.add(((l.leftX / pw) * 100f).coerceIn(0f, 45f))
                    rightPercents.add((((pw - l.rightX) / pw) * 100f).coerceIn(0f, 45f))
                }
                val minTop = nonEmpty.minOf { it.topY }
                val maxBottom = nonEmpty.maxOf { it.topY + it.fontSizePt }
                topPercents.add(((minTop / ph) * 100f).coerceIn(0f, 40f))
                bottomPercents.add((((ph - maxBottom) / ph) * 100f).coerceIn(0f, 40f))
            } else {
                val imgs = pagesImages[pageNum].orEmpty().filter {
                    it.leftX.isFinite() && it.topY.isFinite() && it.widthPt.isFinite() && it.heightPt.isFinite() &&
                        it.pageWidth > 0f && it.pageHeight > 0f
                }
                if (imgs.isNotEmpty()) {
                    val pw = imgs.first().pageWidth
                    val ph = imgs.first().pageHeight
                    leftPercents.add(((imgs.minOf { it.leftX } / pw) * 100f).coerceIn(0f, 45f))
                    rightPercents.add((((pw - imgs.maxOf { it.leftX + it.widthPt }) / pw) * 100f).coerceIn(0f, 45f))
                    topPercents.add(((imgs.minOf { it.topY } / ph) * 100f).coerceIn(0f, 40f))
                    bottomPercents.add((((ph - imgs.maxOf { it.topY + it.heightPt }) / ph) * 100f).coerceIn(0f, 40f))
                }
            }
        }

        if (leftPercents.isEmpty()) return PdfMarginInfo()

        val left = percentile(leftPercents, 0.12f).takeIf { it.isFinite() }?.roundToInt()?.coerceIn(2, 18) ?: 5
        val right = percentile(rightPercents, 0.12f).takeIf { it.isFinite() }?.roundToInt()?.coerceIn(2, 18) ?: 5
        val top = percentile(topPercents, 0.15f).takeIf { it.isFinite() }?.roundToInt()?.coerceIn(2, 15) ?: 4
        val bottom = percentile(bottomPercents, 0.15f).takeIf { it.isFinite() }?.roundToInt()?.coerceIn(2, 15) ?: 4
        return PdfMarginInfo(left, right, top, bottom)
    }

    private fun computeMedianLineGap(
        pagesLines: Map<Int, List<ExtractedPdfLine>>,
        bodyFontSize: Float
    ): Float {
        val gaps = mutableListOf<Float>()
        for ((_, lines) in pagesLines) {
            val sorted = lines.sortedBy { it.topY }
            for (i in 1 until sorted.size) {
                val prev = sorted[i - 1]
                val curr = sorted[i]
                if (abs(prev.fontSizePt - bodyFontSize) <= 1.2f && abs(curr.fontSizePt - bodyFontSize) <= 1.2f) {
                    val gap = curr.topY - prev.topY
                    if (gap in (bodyFontSize * 0.8f)..(bodyFontSize * 2.2f)) {
                        gaps.add(gap)
                    }
                }
            }
        }
        return if (gaps.isNotEmpty()) percentile(gaps, 0.5f) else bodyFontSize * 1.28f
    }

    private fun calibrateChapterFontSizeThreshold(
        nonTocLines: List<ExtractedPdfLine>,
        bodyFontSize: Float,
        totalPdfPages: Int
    ): Float {
        val minLargePt = bodyFontSize * 1.45f
        val candidates = nonTocLines.filter { line ->
            val t = line.text.trim()
            t.length in 2..95 &&
                !isTocEntryLine(t) &&
                !endsWithSentenceComma(t) &&
                line.fontSizePt >= minLargePt &&
                line.topY <= line.pageHeight * 0.55f
        }
        if (candidates.size < 2) return Float.POSITIVE_INFINITY

        // Group candidate lines by rounded 0.5pt font size from largest downward
        val sizeBuckets = candidates
            .groupBy { (it.fontSizePt * 2f).roundToInt() / 2f }
            .toList()
            .sortedByDescending { it.first }

        // A real book does not have 1 chapter on every single page (prevents 150-chapter explosion)
        val maxReasonableChapters = if (totalPdfPages <= 6) {
            totalPdfPages
        } else {
            maxOf(12, minOf(48, (totalPdfPages / 2.2f).roundToInt()))
        }

        var cumulative = 0
        var chosenThreshold = Float.POSITIVE_INFINITY
        for ((sizePt, bucketLines) in sizeBuckets) {
            if (cumulative + bucketLines.size <= maxReasonableChapters) {
                cumulative += bucketLines.size
                chosenThreshold = sizePt
                if (cumulative >= 2) {
                    // Stop at the topmost chapter tier so smaller tiers remain H2 Topics / H3 Sub-topics
                    break
                }
            } else if (cumulative == 0) {
                // Even the largest font tier appears too often -> treat all as topics, not 150 chapters
                return Float.POSITIVE_INFINITY
            } else {
                break
            }
        }
        return if (cumulative >= 2) chosenThreshold - 0.35f else Float.POSITIVE_INFINITY
    }

    private fun classifyHeadingLevel(
        line: ExtractedPdfLine,
        bodyFontSize: Float,
        gapAbove: Float,
        medianLineGap: Float,
        pageOutlines: List<ExtractedOutlineItem>,
        hasAuthoritativeOutlines: Boolean,
        hasExplicitChapters: Boolean,
        calibratedH1MinFontPt: Float,
        isTocPage: Boolean
    ): PdfBlockType? {
        val text = line.text.trim()
        if (text.length > 130 || text.isEmpty()) return null
        if (isListItem(text) && line.fontSizePt <= bodyFontSize + 0.8f && !line.isBold) return null

        // Never classify printed TOC rows as chapter breaks
        if (isTocPage || isTocEntryLine(text)) {
            if (text.equals("Table of Contents", ignoreCase = true) ||
                text.equals("Contents", ignoreCase = true) ||
                text.equals("Brief Contents", ignoreCase = true)
            ) {
                return PdfBlockType.H2
            }
            return null
        }

        // 1. Exact outline match (authoritative Chapter = 0 -> H1, Topic = 1 -> H2, Sub-topic = 2 -> H3, deeper -> H4)
        val normText = normalizeHeadingComparison(text)
        val matchedOutline = pageOutlines.firstOrNull { item ->
            val normOutline = normalizeHeadingComparison(item.title)
            normOutline.isNotBlank() && (normText == normOutline || normText.startsWith(normOutline) || normOutline.startsWith(normText))
        }
        if (matchedOutline != null) {
            return when {
                matchedOutline.level <= 0 -> PdfBlockType.H1
                matchedOutline.level == 1 -> PdfBlockType.H2
                matchedOutline.level == 2 -> PdfBlockType.H3
                else -> PdfBlockType.H4
            }
        }

        val sizeRatio = line.fontSizePt / bodyFontSize.coerceAtLeast(6f)
        val hasExtraGap = gapAbove >= medianLineGap * 1.25f

        // 2. Explicit Chapter Headings ("Chapter 1", "Unit 2", "Prologue", etc.)
        // Only classify as H1 if we do NOT already have an authoritative PDF outline controlling chapters,
        // and NEVER promote a body-sized running header in the top 9% margin to H1.
        val isLikelySmallRunningHeader = line.topY < line.pageHeight * 0.09f && sizeRatio <= 1.12f
        if (!hasAuthoritativeOutlines && !isLikelySmallRunningHeader &&
            isExplicitChapterStart(text) && text.length <= 95 &&
            (sizeRatio >= 1.10f || (line.isBold && sizeRatio >= 0.98f))
        ) {
            return PdfBlockType.H1
        }

        // 3. Calibrated display-size Chapter Heading (only when neither outlines nor explicit "Chapter N" markers exist)
        if (!hasAuthoritativeOutlines && !hasExplicitChapters &&
            line.fontSizePt >= calibratedH1MinFontPt &&
            line.topY <= line.pageHeight * 0.55f &&
            text.length <= 95 &&
            !endsWithSentenceComma(text)
        ) {
            return PdfBlockType.H1
        }

        // 4. Numbered Topics & Sub-topics (e.g. "1.1 Foundations" -> H2 Topic, "1.1.2 Details" -> H3 Sub-topic)
        val endsLikeSentence = text.endsWith(".") || text.endsWith(",") || text.endsWith(";")
        if (text.length <= 105 && !endsLikeSentence) {
            if (SUBTOPIC_NUMBERED_PATTERN.matcher(text).matches() && (line.isBold || sizeRatio >= 1.04f || hasExtraGap)) {
                return if (text.substringBefore(' ').count { it == '.' } >= 3) PdfBlockType.H4 else PdfBlockType.H3
            }
            if (TOPIC_NUMBERED_PATTERN.matcher(text).matches() && (line.isBold || sizeRatio >= 1.06f || hasExtraGap)) {
                return if (sizeRatio >= 1.20f) PdfBlockType.H2 else PdfBlockType.H3
            }
        }

        // 5. Visual hierarchy for Topics (H2) and Sub-topics (H3 / H4) — NEVER promotes to H1
        if (sizeRatio >= 1.24f && !endsWithSentenceComma(text)) {
            return PdfBlockType.H2
        }
        if (sizeRatio >= 1.10f && !endsWithSentencePunctuationOrComma(text)) {
            return PdfBlockType.H3
        }
        if (line.isBold && text.length <= 85 && !endsLikeSentence && hasExtraGap) {
            return if (sizeRatio >= 1.05f) PdfBlockType.H3 else PdfBlockType.H4
        }

        return null
    }

    private fun isExplicitChapterStart(text: String): Boolean {
        val trimmed = text.trim()
        if (isTocEntryLine(trimmed)) return false
        return CHAPTER_PATTERN.matcher(trimmed).matches()
    }

    private fun isBareChapterOrPartLabel(text: String): Boolean {
        return BARE_CHAPTER_LABEL_PATTERN.matcher(text.trim()).matches()
    }

    private fun isTocEntryLine(text: String): Boolean {
        val t = text.trim()
        return TOC_LEADER_LINE_PATTERN.matcher(t).matches()
    }

    private fun isListItem(text: String): Boolean {
        val t = text.trim()
        return t.startsWith("• ") || t.startsWith("- ") || t.startsWith("* ") ||
            t.startsWith("▪ ") || t.startsWith("◦ ")
    }

    private fun endsWithSentencePunctuation(text: String): Boolean {
        if (text.isEmpty()) return false
        val last = text.last()
        return last == '.' || last == '!' || last == '?' || last == '”' || last == '"' || last == ':'
    }

    private fun endsWithSentenceComma(text: String): Boolean {
        if (text.isEmpty()) return false
        val last = text.last()
        return last == ',' || last == ';'
    }

    private fun endsWithSentencePunctuationOrComma(text: String): Boolean {
        if (text.isEmpty()) return false
        val last = text.last()
        return last == '.' || last == ',' || last == ';'
    }

    private fun joinLinesSmart(lines: List<ExtractedPdfLine>): String {
        val sb = StringBuilder()
        for (line in lines) {
            val t = line.text.trim()
            if (t.isEmpty()) continue
            if (sb.isEmpty()) {
                sb.append(t)
            } else {
                val lastChar = sb.last()
                if (lastChar == '-' && sb.length > 2 && sb[sb.length - 2].isLetter() && t.first().isLetter()) {
                    sb.deleteCharAt(sb.length - 1)
                    sb.append(t)
                } else {
                    sb.append(' ').append(t)
                }
            }
        }
        return sb.toString().trim()
    }

    private fun percentile(values: List<Float>, p: Float): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val idx = (p.coerceIn(0f, 1f) * (sorted.size - 1)).roundToInt().coerceIn(0, sorted.lastIndex)
        return sorted[idx]
    }

    private sealed interface PageElement {
        val sortY: Float
        data class LineElem(val line: ExtractedPdfLine, override val sortY: Float) : PageElement
        data class ImgElem(val image: ExtractedPdfImage, override val sortY: Float) : PageElement
    }

    companion object {
        private const val CHAPTER_NUM_TOKEN =
            "(?:[0-9]{1,3}|[IVXLCDM]{1,6}|One|Two|Three|Four|Five|Six|Seven|Eight|Nine|Ten|Eleven|Twelve|Thirteen|Fourteen|Fifteen|Sixteen|Seventeen|Eighteen|Nineteen|Twenty|Twenty[\\-\\s](?:One|Two|Three|Four|Five|Six|Seven|Eight|Nine)|Thirty)"

        // Matches true top-level chapters ("Chapter 1", "Chapter 1: Foundations", "Unit 2", "Prologue", etc.)
        // Intentionally excludes "Section 1.1", "Part of the...", "Book reviews...", and decimal sub-numbers "1.1"
        private val CHAPTER_PATTERN = java.util.regex.Pattern.compile(
            "^(?:Chapter|Unit|Module|Lesson|Appendix)\\s+$CHAPTER_NUM_TOKEN(?!\\.\\d)(?:\\s*[:.\\-–—]\\s*.*|\\s+[A-Z0-9].*)?$" +
                "|^(?:Part|Book)\\s+$CHAPTER_NUM_TOKEN(?!\\.\\d)(?:\\s*[:.\\-–—]\\s*.*)?$" +
                "|^(?:Prologue|Epilogue|Introduction|Preface|Foreword|Conclusion|Afterword|Bibliography|References|Glossary)$",
            java.util.regex.Pattern.CASE_INSENSITIVE
        )

        private val BARE_CHAPTER_LABEL_PATTERN = java.util.regex.Pattern.compile(
            "^(?:Chapter|Part|Book|Unit|Module|Lesson|Appendix)\\s+$CHAPTER_NUM_TOKEN[:.\\-–—\\s]*$",
            java.util.regex.Pattern.CASE_INSENSITIVE
        )

        // Matches 1-level numbered topics like "1.1 Overview" or "Section 2.3 Forces"
        private val TOPIC_NUMBERED_PATTERN = java.util.regex.Pattern.compile(
            "^(?:Section\\s+)?\\d{1,3}\\.\\d{1,3}(?!\\.\\d)\\.?\\s+\\S.*",
            java.util.regex.Pattern.CASE_INSENSITIVE
        )

        // Matches deeper numbered sub-topics like "1.1.1 Atomic Structure" or "2.3.4.1 Edge Cases"
        private val SUBTOPIC_NUMBERED_PATTERN = java.util.regex.Pattern.compile(
            "^(?:Section\\s+)?\\d{1,3}(?:\\.\\d{1,3}){2,4}\\.?\\s+\\S.*",
            java.util.regex.Pattern.CASE_INSENSITIVE
        )

        // Matches top-level numbered chapter headings like "1. Mechanics" or "2 Thermodynamics"
        private val TOP_LEVEL_NUMBERED_PATTERN = java.util.regex.Pattern.compile(
            "^\\d{1,2}\\.?\\s+[A-Z]\\S.*"
        )

        // Matches printed TOC rows ending in dotted leaders or wide-spaced trailing page numbers
        private val TOC_LEADER_LINE_PATTERN = java.util.regex.Pattern.compile(
            "^.+?(?:\\.{3,}|·{3,}|_{3,}|\\s{3,})\\s*(?:[ivxlcdm]+|\\d{1,4})\\s*$",
            java.util.regex.Pattern.CASE_INSENSITIVE
        )

        fun normalizeHeadingComparison(text: String): String {
            return text.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        /**
         * Determines which outline nesting depth holds real chapters:
         * handles single-root Book Title bookmarks at level 0 wrapping chapters at level 1,
         * or Part wrappers at level 0 wrapping explicit "Chapter ..." nodes at level 1.
         */
        fun resolveChapterOutlineLevel(outlineItems: List<ExtractedOutlineItem>): Int {
            if (outlineItems.isEmpty()) return 0
            val byLevel = outlineItems.groupBy { it.level }
            val sortedLevels = byLevel.keys.sorted()
            val minLevel = sortedLevels.first()

            val minCount = byLevel[minLevel]?.size ?: 0
            val nextLevel = sortedLevels.getOrNull(1)
            if (minCount <= 1 && nextLevel != null && (byLevel[nextLevel]?.size ?: 0) >= 2) {
                return nextLevel
            }
            if (nextLevel != null && minCount <= 4) {
                val minHasChapters = byLevel[minLevel].orEmpty().any {
                    CHAPTER_PATTERN.matcher(it.title.trim()).matches() &&
                        !it.title.trim().startsWith("Part", ignoreCase = true)
                }
                val nextChapterCount = byLevel[nextLevel].orEmpty().count {
                    CHAPTER_PATTERN.matcher(it.title.trim()).matches()
                }
                if (!minHasChapters && nextChapterCount >= 2) {
                    return nextLevel
                }
            }
            return minLevel
        }

        private val PER_PAGE_BOOKMARK_PATTERN = java.util.regex.Pattern.compile(
            "^(?:page|p\\.?|pg\\.?|slide|sheet|folio)?\\s*\\d{1,4}$",
            java.util.regex.Pattern.CASE_INSENSITIVE
        )

        /**
         * Normalizes a PDF outline tree so that:
         * - Level 0 = Chapters (unwrapping single Book Title / Part wrappers)
         * - Level 1 = Topics (H2)
         * - Level 2 = Sub-topics (H3)
         * - Level 3+ = Deeper Sub-topics (H4)
         * Also strips noisy per-page bookmarks ("Page 1", "Page 2", ...) and rescues flat PDF
         * outlines where chapters, topics (1.1 or unnumbered), and sub-topics (1.1.1) were all
         * stored at level 0 so a 208-page PDF never explodes into 208 chapters.
         */
        fun normalizeOutlineHierarchy(
            outlineItems: List<ExtractedOutlineItem>,
            totalPdfPages: Int = 0
        ): List<ExtractedOutlineItem> {
            if (outlineItems.isEmpty()) return emptyList()

            // 1. Strip per-page bookmarks ("Page 1", "2", "Slide 14", etc.)
            val meaningful = outlineItems.filterNot {
                PER_PAGE_BOOKMARK_PATTERN.matcher(it.title.trim()).matches()
            }
            if (meaningful.isEmpty()) return emptyList()

            val chapterLevel = resolveChapterOutlineLevel(meaningful)

            // 2. Drop wrapper root nodes above chapterLevel (e.g. single Book Title root)
            val shifted = meaningful
                .filter { it.level >= chapterLevel }
                .map { it.copy(level = (it.level - chapterLevel).coerceAtLeast(0)) }

            if (shifted.isEmpty()) return emptyList()

            // 3. Rescue flat outlines mixing real chapters with topics/sub-topics all at level 0
            val level0 = shifted.filter { it.level == 0 }
            val hasNestedAlready = shifted.any { it.level > 0 }
            val explicitChapterInL0 = level0.count { CHAPTER_PATTERN.matcher(it.title.trim()).matches() }
            val topNumInL0 = level0.count { TOP_LEVEL_NUMBERED_PATTERN.matcher(it.title.trim()).matches() }
            val topicNumInL0 = level0.count { TOPIC_NUMBERED_PATTERN.matcher(it.title.trim()).matches() }
            val subTopicNumInL0 = level0.count { SUBTOPIC_NUMBERED_PATTERN.matcher(it.title.trim()).matches() }

            val needsFlatRescue = (explicitChapterInL0 >= 2 && level0.size > explicitChapterInL0 && (!hasNestedAlready || topicNumInL0 >= 2 || subTopicNumInL0 >= 2)) ||
                (topNumInL0 >= 2 && (topicNumInL0 >= 2 || subTopicNumInL0 >= 2))

            val normalized = if (needsFlatRescue) {
                shifted.map { item ->
                    if (item.level != 0) {
                        item
                    } else {
                        val t = item.title.trim()
                        val inferredLevel = when {
                            SUBTOPIC_NUMBERED_PATTERN.matcher(t).matches() -> 2
                            TOPIC_NUMBERED_PATTERN.matcher(t).matches() -> 1
                            CHAPTER_PATTERN.matcher(t).matches() -> 0
                            explicitChapterInL0 == 0 && TOP_LEVEL_NUMBERED_PATTERN.matcher(t).matches() -> 0
                            explicitChapterInL0 >= 2 -> 1
                            !hasNestedAlready -> 1
                            else -> 0
                        }
                        item.copy(level = inferredLevel)
                    }
                }
            } else {
                shifted
            }

            // 4. Sanity-guard against bloated noisy outlines (e.g. 208 per-page/per-heading bookmarks in a 208-page PDF)
            val finalLevel0 = normalized.filter { it.level == 0 }
            if (totalPdfPages >= 25 && finalLevel0.size >= 40) {
                val pagesPerChapter = totalPdfPages.toFloat() / finalLevel0.size.toFloat()
                val explicitRatio = finalLevel0.count {
                    val t = it.title.trim()
                    CHAPTER_PATTERN.matcher(t).matches() || TOP_LEVEL_NUMBERED_PATTERN.matcher(t).matches()
                }.toFloat() / finalLevel0.size.toFloat()
                if (pagesPerChapter < 1.6f && explicitRatio < 0.45f) {
                    return emptyList()
                }
            }

            return normalized
        }
    }

    private class LineExtractingTextStripper(
        private val pageNumber: Int,
        private val pageWidth: Float,
        private val pageHeight: Float
    ) : PDFTextStripper() {

        val collectedLines = mutableListOf<ExtractedPdfLine>()
        private val lineTextBuilder = StringBuilder()
        private val linePositions = mutableListOf<TextPosition>()

        override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
            if (textPositions.isEmpty()) return

            if (linePositions.isNotEmpty()) {
                val prevY = linePositions.last().yDirAdj
                val nextY = textPositions.first().yDirAdj
                val refSize = maxOf(linePositions.last().fontSizeInPt, 8f)
                if (abs(nextY - prevY) > refSize * 0.65f) {
                    flushCurrentLine()
                }
            }

            if (lineTextBuilder.isNotEmpty() && !lineTextBuilder.endsWith(" ") && !text.startsWith(" ")) {
                val prevEnd = linePositions.lastOrNull()?.endX ?: 0f
                val nextStart = textPositions.first().xDirAdj
                val approxSpace = maxOf(textPositions.first().widthOfSpace, textPositions.first().fontSizeInPt * 0.22f)
                if (nextStart - prevEnd > approxSpace * 0.55f) {
                    lineTextBuilder.append(' ')
                }
            }

            lineTextBuilder.append(text)
            linePositions.addAll(textPositions)
        }

        override fun writeLineSeparator() {
            flushCurrentLine()
        }

        override fun endPage(page: PDPage) {
            flushCurrentLine()
            super.endPage(page)
        }

        private fun flushCurrentLine() {
            val rawText = lineTextBuilder.toString().trim()
            if (rawText.isNotEmpty() && linePositions.isNotEmpty()) {
                val nonSpacePositions = linePositions.filter { it.unicode?.isNotBlank() == true }
                    .ifEmpty { linePositions }

                val leftX = nonSpacePositions.mapNotNull { it.xDirAdj.takeIf { v -> v.isFinite() } }.minOrNull() ?: 0f
                val rightX = nonSpacePositions.mapNotNull { it.endX.takeIf { v -> v.isFinite() } }.maxOrNull() ?: pageWidth
                val topY = nonSpacePositions.mapNotNull { it.yDirAdj.takeIf { v -> v.isFinite() } }.minOrNull() ?: 0f

                val sizeBuckets = mutableMapOf<Float, Int>()
                var boldCount = 0
                var italicCount = 0
                for (tp in nonSpacePositions) {
                    val fontPt = tp.fontSizeInPt.takeIf { it.isFinite() } ?: -1f
                    val hDir = tp.heightDir.takeIf { it.isFinite() } ?: -1f
                    val effectivePt = when {
                        fontPt in 3f..144f && hDir in 3f..144f && (hDir * 1.25f) > (fontPt * 1.30f) ->
                            (hDir * 1.25f).coerceAtMost(144f)
                        fontPt in 3f..144f -> fontPt
                        hDir in 2f..144f -> hDir * 1.18f
                        else -> 11f
                    }
                    val bucket = (effectivePt * 2f).roundToInt() / 2f
                    sizeBuckets[bucket] = (sizeBuckets[bucket] ?: 0) + 1

                    val fontName = tp.font?.name?.lowercase() ?: ""
                    if (fontName.contains("bold") || fontName.contains("black") ||
                        fontName.contains("heavy") || fontName.contains("semibold") ||
                        fontName.contains("demi")
                    ) {
                        boldCount++
                    }
                    if (fontName.contains("italic") || fontName.contains("oblique")) {
                        italicCount++
                    }
                }

                val dominantSize = sizeBuckets.maxByOrNull { it.value }?.key ?: 11f
                val totalChars = nonSpacePositions.size.coerceAtLeast(1)
                val isBold = (boldCount.toFloat() / totalChars) >= 0.6f
                val isItalic = (italicCount.toFloat() / totalChars) >= 0.6f

                collectedLines.add(
                    ExtractedPdfLine(
                        pageNumber = pageNumber,
                        text = rawText,
                        leftX = leftX,
                        rightX = rightX,
                        topY = topY,
                        fontSizePt = dominantSize,
                        isBold = isBold,
                        isItalic = isItalic,
                        pageWidth = pageWidth,
                        pageHeight = pageHeight
                    )
                )
            }
            lineTextBuilder.clear()
            linePositions.clear()
        }
    }

    private data class RawGraphicBox(
        val leftX: Float,
        val topY: Float,
        val rightX: Float,
        val bottomY: Float,
        val imageObject: PDImageXObject? = null,
        val identitySignature: Int = 0,
        val usedExactStreamCoords: Boolean = true
    )

    private data class ClusteredGraphicRegion(
        val leftX: Float,
        val topY: Float,
        val rightX: Float,
        val bottomY: Float,
        val primaryImageObj: PDImageXObject?,
        val identitySignature: Int,
        val usedExactStreamCoords: Boolean
    )

    /**
     * Walks PDF content stream operators to capture both raster XObject images (including rotated/
     * mirrored/nested FormXObjects) AND painted vector paths (diagrams, charts, vector art).
     */
    private class PageGraphicStreamEngine(
        private val pageWidth: Float,
        private val pageHeight: Float,
        private val cropLowerX: Float,
        private val cropLowerY: Float,
        private val outImages: MutableList<RawGraphicBox>,
        private val outVectors: MutableList<RawGraphicBox>
    ) : PDFStreamEngine() {

        private var pathMinX = Float.POSITIVE_INFINITY
        private var pathMaxX = Float.NEGATIVE_INFINITY
        private var pathMinY = Float.POSITIVE_INFINITY
        private var pathMaxY = Float.NEGATIVE_INFINITY
        private var hasPathPoints = false

        init {
            addOperator(Concatenate())
            addOperator(Save())
            addOperator(Restore())
            addOperator(SetGraphicsStateParameters())
            addOperator(SetMatrix())
            addOperator(DrawObjectCoordProcessor())

            // Vector path construction operators
            addOperator(PathPointProcessor("m", intArrayOf(0, 1)))
            addOperator(PathPointProcessor("l", intArrayOf(0, 1)))
            addOperator(PathPointProcessor("c", intArrayOf(0, 1, 2, 3, 4, 5)))
            addOperator(PathPointProcessor("v", intArrayOf(0, 1, 2, 3)))
            addOperator(PathPointProcessor("y", intArrayOf(0, 1, 2, 3)))
            addOperator(RectPathProcessor())

            // Painting / path ending operators
            listOf("S", "s", "f", "F", "f*", "B", "b", "B*", "b*").forEach { op ->
                addOperator(PaintPathProcessor(op))
            }
            addOperator(EndPathNoPaintProcessor("n"))
        }

        private fun recordUserSpacePoint(ux: Float, uy: Float) {
            val ctm = graphicsState?.currentTransformationMatrix ?: return
            val tx = ctm.scalingFactorX * ux + ctm.shearX * uy + ctm.translateX
            val ty = ctm.shearY * ux + ctm.scalingFactorY * uy + ctm.translateY
            val pageX = tx - cropLowerX
            val pageTopY = pageHeight - (ty - cropLowerY)

            if (pageX.isFinite() && pageTopY.isFinite()) {
                pathMinX = min(pathMinX, pageX)
                pathMaxX = max(pathMaxX, pageX)
                pathMinY = min(pathMinY, pageTopY)
                pathMaxY = max(pathMaxY, pageTopY)
                hasPathPoints = true
            }
        }

        private fun resetPath() {
            pathMinX = Float.POSITIVE_INFINITY
            pathMaxX = Float.NEGATIVE_INFINITY
            pathMinY = Float.POSITIVE_INFINITY
            pathMaxY = Float.NEGATIVE_INFINITY
            hasPathPoints = false
        }

        private inner class PathPointProcessor(
            private val opName: String,
            private val pairIndices: IntArray
        ) : OperatorProcessor() {
            override fun getName(): String = opName
            override fun process(operator: Operator, operands: MutableList<COSBase>) {
                var i = 0
                while (i + 1 < pairIndices.size) {
                    val xi = pairIndices[i]
                    val yi = pairIndices[i + 1]
                    if (xi < operands.size && yi < operands.size) {
                        val xNum = operands[xi] as? COSNumber
                        val yNum = operands[yi] as? COSNumber
                        if (xNum != null && yNum != null) {
                            recordUserSpacePoint(xNum.floatValue(), yNum.floatValue())
                        }
                    }
                    i += 2
                }
            }
        }

        private inner class RectPathProcessor : OperatorProcessor() {
            override fun getName(): String = "re"
            override fun process(operator: Operator, operands: MutableList<COSBase>) {
                if (operands.size < 4) return
                val x = (operands[0] as? COSNumber)?.floatValue() ?: return
                val y = (operands[1] as? COSNumber)?.floatValue() ?: return
                val w = (operands[2] as? COSNumber)?.floatValue() ?: return
                val h = (operands[3] as? COSNumber)?.floatValue() ?: return
                recordUserSpacePoint(x, y)
                recordUserSpacePoint(x + w, y)
                recordUserSpacePoint(x, y + h)
                recordUserSpacePoint(x + w, y + h)
            }
        }

        private inner class PaintPathProcessor(private val opName: String) : OperatorProcessor() {
            override fun getName(): String = opName
            override fun process(operator: Operator, operands: MutableList<COSBase>) {
                if (hasPathPoints) {
                    val left = pathMinX.coerceIn(0f, pageWidth)
                    val top = pathMinY.coerceIn(0f, pageHeight)
                    val right = pathMaxX.coerceIn(0f, pageWidth)
                    val bottom = pathMaxY.coerceIn(0f, pageHeight)
                    if (right > left && bottom > top) {
                        outVectors.add(
                            RawGraphicBox(
                                leftX = left,
                                topY = top,
                                rightX = right,
                                bottomY = bottom,
                                imageObject = null,
                                identitySignature = 0,
                                usedExactStreamCoords = true
                            )
                        )
                    }
                }
                resetPath()
            }
        }

        private inner class EndPathNoPaintProcessor(private val opName: String) : OperatorProcessor() {
            override fun getName(): String = opName
            override fun process(operator: Operator, operands: MutableList<COSBase>) {
                resetPath()
            }
        }

        private inner class DrawObjectCoordProcessor : OperatorProcessor() {
            override fun getName(): String = "Do"

            override fun process(operator: Operator, operands: MutableList<COSBase>) {
                if (operands.isEmpty()) return
                val objectName = operands[0] as? COSName ?: return
                val xobject = context.resources?.getXObject(objectName) ?: return

                when (xobject) {
                    is PDImageXObject -> {
                        val ctm = context.graphicsState.currentTransformationMatrix
                        // Transform all 4 corners of the image unit square [0,0]..[1,1]
                        val corners = arrayOf(
                            0f to 0f,
                            1f to 0f,
                            0f to 1f,
                            1f to 1f
                        )
                        var minX = Float.POSITIVE_INFINITY
                        var maxX = Float.NEGATIVE_INFINITY
                        var minTopY = Float.POSITIVE_INFINITY
                        var maxBottomY = Float.NEGATIVE_INFINITY

                        for ((ux, uy) in corners) {
                            val tx = ctm.scalingFactorX * ux + ctm.shearX * uy + ctm.translateX
                            val ty = ctm.shearY * ux + ctm.scalingFactorY * uy + ctm.translateY
                            val px = tx - cropLowerX
                            val py = pageHeight - (ty - cropLowerY)
                            minX = min(minX, px)
                            maxX = max(maxX, px)
                            minTopY = min(minTopY, py)
                            maxBottomY = max(maxBottomY, py)
                        }

                        if (minX.isFinite() && maxX > minX && maxBottomY > minTopY) {
                            outImages.add(
                                RawGraphicBox(
                                    leftX = minX.coerceIn(0f, pageWidth),
                                    topY = minTopY.coerceIn(0f, pageHeight),
                                    rightX = maxX.coerceIn(0f, pageWidth),
                                    bottomY = maxBottomY.coerceIn(0f, pageHeight),
                                    imageObject = xobject,
                                    identitySignature = System.identityHashCode(xobject.cosObject),
                                    usedExactStreamCoords = true
                                )
                            )
                        }
                    }
                    is PDFormXObject -> {
                        showForm(xobject)
                    }
                }
            }
        }
    }
}

