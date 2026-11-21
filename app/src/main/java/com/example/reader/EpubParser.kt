package com.example.reader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import kotlin.math.roundToInt

class EpubParser {

    suspend fun parse(epubFile: File): Result<ParsedEpubBook> = withContext(Dispatchers.IO) {
        if (!epubFile.exists() || epubFile.length() == 0L) {
            return@withContext Result.failure(Exception("EPUB file does not exist or is empty"))
        }

        try {
            ZipFile(epubFile).use { zip ->
            val entries = zip.entries().toList().associateBy { it.name }

            // 1. Locate root opf file from META-INF/container.xml
            var opfPath = "OEBPS/content.opf"
            val containerEntry = entries["META-INF/container.xml"]
            if (containerEntry != null) {
                val containerXml = zip.getInputStream(containerEntry).bufferedReader().use { it.readText() }
                val rootFileRegex = Regex("""full-path=["']([^"']+)["']""")
                rootFileRegex.find(containerXml)?.groupValues?.get(1)?.let {
                    opfPath = it
                }
            }

            val opfEntry = entries[opfPath] ?: entries.values.firstOrNull { it.name.endsWith(".opf") }
                ?: return@withContext Result.failure(Exception("Cannot find content.opf in EPUB archive"))

            val opfBaseDir = if (opfEntry.name.contains("/")) {
                opfEntry.name.substringBeforeLast("/") + "/"
            } else ""

            val opfContent = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }

            // Extract title (supports dc:title or unprefixed title)
            val titleRegex = Regex("""<(?:dc:)?title\b[^>]*>(.*?)</(?:dc:)?title>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val rawOpfTitle = titleRegex.find(opfContent)?.groupValues?.get(1)
                ?.let { unescapeHtml(stripTags(it)).trim() }
                ?.takeIf { it.isNotBlank() && !com.example.converter.PdfMetadataExtractor.isGenericTitle(it) }

            // Extract author (supports dc:creator or unprefixed creator)
            val authorRegex = Regex("""<(?:dc:)?creator\b[^>]*>(.*?)</(?:dc:)?creator>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val rawOpfAuthors = authorRegex.findAll(opfContent)
                .map { unescapeHtml(stripTags(it.groupValues[1])).trim() }
                .filter { it.isNotBlank() && !com.example.converter.PdfMetadataExtractor.isGenericAuthor(it) }
                .distinct()
                .joinToString(", ")
                .takeIf { it.isNotBlank() }

            var resolvedTitle = rawOpfTitle
            var resolvedAuthor = rawOpfAuthors

            // Manifest items: id -> href
            val manifest = mutableMapOf<String, String>()
            val itemRegex = Regex("""<item\s+[^>]*id=["']([^"']+)["'][^>]*href=["']([^"']+)["'][^>]*>""")
            val itemRegex2 = Regex("""<item\s+[^>]*href=["']([^"']+)["'][^>]*id=["']([^"']+)["'][^>]*>""")

            itemRegex.findAll(opfContent).forEach { match ->
                manifest[match.groupValues[1]] = match.groupValues[2]
            }
            itemRegex2.findAll(opfContent).forEach { match ->
                manifest[match.groupValues[2]] = match.groupValues[1]
            }

            // Parse CSS margins if stylesheet exists
            var epubMargins = EpubMargins()
            val cssEntry = manifest["css"]?.let { entries[opfBaseDir + it] ?: entries[it] }
                ?: entries.values.firstOrNull { it.name.endsWith(".css", ignoreCase = true) }
            if (cssEntry != null) {
                val cssText = zip.getInputStream(cssEntry).bufferedReader().use { it.readText() }
                epubMargins = parseCssMargins(cssText)
            }

            // Load image assets from archive with defensive memory caps to avoid OOM on massive EPUBs
            val bookImages = mutableMapOf<String, ByteArray>()
            val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
            var totalImageBytes = 0L
            val maxTotalImageBytes = 48L * 1024L * 1024L
            for ((entryPath, entry) in entries) {
                val ext = entryPath.substringAfterLast('.', "").lowercase()
                if (ext in imageExtensions && !entry.isDirectory && totalImageBytes < maxTotalImageBytes) {
                    try {
                        if (entry.size > 12L * 1024L * 1024L) continue
                        val bytes = zip.getInputStream(entry).use { it.readBytes() }
                        totalImageBytes += bytes.size
                        bookImages[entryPath] = bytes
                        if (opfBaseDir.isNotEmpty() && entryPath.startsWith(opfBaseDir)) {
                            bookImages[entryPath.removePrefix(opfBaseDir)] = bytes
                        }
                        val justFileName = entryPath.substringAfterLast("/")
                        if (!bookImages.containsKey(justFileName)) {
                            bookImages[justFileName] = bytes
                        }
                    } catch (_: Throwable) {}
                }
            }

            // Spine itemrefs
            val spineIds = mutableListOf<String>()
            val itemrefRegex = Regex("""<itemref\s+[^>]*idref=["']([^"']+)["']""")
            itemrefRegex.findAll(opfContent).forEach { match ->
                spineIds.add(match.groupValues[1])
            }

            // Extract TOC labels & nested Topic/Sub-topic entries from toc.ncx / nav.xhtml
            val tocLabels = mutableMapOf<String, String>()
            val tocSubEntriesByFile = mutableMapOf<String, MutableList<ParsedTocSubEntry>>()
            val ncxEntry = manifest["ncx"]?.let { entries[opfBaseDir + it] ?: entries[it] }
                ?: entries.values.firstOrNull { it.name.endsWith(".ncx") }

            if (ncxEntry != null) {
                val ncxContent = zip.getInputStream(ncxEntry).bufferedReader().use { it.readText() }
                parseNcxHierarchy(ncxContent, tocLabels, tocSubEntriesByFile)
                if (resolvedTitle == null) {
                    val ncxTitle = Regex("""<docTitle[^>]*>.*?<text[^>]*>(.*?)</text>.*?</docTitle>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                        .find(ncxContent)?.groupValues?.get(1)
                        ?.let { unescapeHtml(stripTags(it)).trim() }
                    if (!com.example.converter.PdfMetadataExtractor.isGenericTitle(ncxTitle)) {
                        resolvedTitle = ncxTitle
                    }
                }
                if (resolvedAuthor == null) {
                    val ncxAuthor = Regex("""<docAuthor[^>]*>.*?<text[^>]*>(.*?)</text>.*?</docAuthor>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                        .find(ncxContent)?.groupValues?.get(1)
                        ?.let { unescapeHtml(stripTags(it)).trim() }
                    if (!com.example.converter.PdfMetadataExtractor.isGenericAuthor(ncxAuthor)) {
                        resolvedAuthor = ncxAuthor
                    }
                }
            }
            val navEntry = manifest["nav"]?.let { entries[opfBaseDir + it] ?: entries[it] }
                ?: entries.values.firstOrNull { it.name.endsWith("nav.xhtml", ignoreCase = true) }
            if (navEntry != null) {
                val navContent = zip.getInputStream(navEntry).bufferedReader().use { it.readText() }
                parseNavXhtmlHierarchy(navContent, tocLabels, tocSubEntriesByFile)
            }

            // Parse chapters in order
            val chapters = mutableListOf<ReaderChapter>()
            var chapterIndex = 0

            val hrefsToProcess = if (spineIds.isNotEmpty()) {
                spineIds.mapNotNull { manifest[it] }
            } else {
                entries.keys.filter { it.endsWith(".xhtml") || it.endsWith(".html") }.sorted()
            }

            for (href in hrefsToProcess) {
                val cleanHref = href.substringBefore("#")
                // Skip cover.xhtml in chapter reading stream if it only contains an image
                if (cleanHref.contains("cover", ignoreCase = true)) continue

                val fullPath = if (entries.containsKey(opfBaseDir + cleanHref)) {
                    opfBaseDir + cleanHref
                } else if (entries.containsKey(cleanHref)) {
                    cleanHref
                } else {
                    entries.keys.firstOrNull { it.endsWith(cleanHref) }
                }

                if (fullPath != null) {
                    val entry = entries[fullPath]
                    if (entry != null) {
                        val html = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                        val chapterDir = if (fullPath.contains("/")) fullPath.substringBeforeLast("/") + "/" else ""

                        // Title determination
                        var chapterTitle = tocLabels[cleanHref]
                        if (chapterTitle.isNullOrBlank()) {
                            val h1Regex = Regex("""<h1[^>]*>(.*?)</h1>""", RegexOption.DOT_MATCHES_ALL)
                            chapterTitle = h1Regex.find(html)?.groupValues?.get(1)?.let { unescapeHtml(stripTags(it)) }?.trim()
                        }
                        if (chapterTitle.isNullOrBlank()) {
                            chapterTitle = "Chapter ${chapterIndex + 1}"
                        }

                        val paragraphs = extractParagraphsFromHtml(
                            html = html,
                            chapterDir = chapterDir,
                            opfBaseDir = opfBaseDir,
                            chapterTitle = chapterTitle
                        )

                        if (paragraphs.isNotEmpty()) {
                            val htmlSubs = extractSubHeadings(paragraphs)
                            val tocSubs = tocSubEntriesByFile[cleanHref].orEmpty()
                            val mergedSubs = mergeSubHeadingsWithToc(htmlSubs, tocSubs, paragraphs)
                            chapters.add(
                                ReaderChapter(
                                    id = cleanHref,
                                    index = chapterIndex,
                                    title = chapterTitle,
                                    paragraphs = paragraphs,
                                    subHeadings = mergedSubs
                                )
                            )
                            chapterIndex++
                        }
                    }
                }
            }

            // Check for cover image bytes
            var coverBytes: ByteArray? = null
            val coverHref = manifest["cover-image"] ?: manifest["cover"]
            val coverEntry = coverHref?.let { entries[opfBaseDir + it] ?: entries[it] }
                ?: entries.values.firstOrNull { it.name.contains("cover") && (it.name.endsWith(".jpg") || it.name.endsWith(".png")) }

            if (coverEntry != null) {
                coverBytes = try {
                    zip.getInputStream(coverEntry).use { it.readBytes() }
                } catch (_: Throwable) {
                    null
                }
            }

            // Fallback title & author from filename and front-matter chapters if OPF/NCX were missing or generic
            val (fileTitle, fileAuthor) = com.example.converter.PdfMetadataExtractor.parseFromFileName(epubFile.name)
            if (resolvedTitle == null && fileTitle != null) {
                resolvedTitle = fileTitle
            }
            if (resolvedTitle == null) {
                resolvedTitle = chapters.firstOrNull()?.title?.takeIf {
                    !com.example.converter.PdfMetadataExtractor.isGenericTitle(it)
                }
            }
            if (resolvedAuthor == null && fileAuthor != null) {
                resolvedAuthor = fileAuthor
            }
            if (resolvedAuthor == null && chapters.isNotEmpty()) {
                val frontParagraphs = chapters.take(2).flatMap { it.paragraphs }
                resolvedAuthor = com.example.converter.PdfMetadataExtractor.extractAuthorFromPlainText(frontParagraphs, resolvedTitle)
            }

            val title = resolvedTitle
                ?: com.example.converter.PdfMetadataExtractor.cleanRawFileName(epubFile.name).ifBlank { "Untitled Book" }
            val author = resolvedAuthor ?: "Unknown Author"

            Result.success(
                ParsedEpubBook(
                    title = title,
                    author = author,
                    chapters = chapters,
                    coverBytes = coverBytes,
                    bookImages = bookImages,
                    epubMargins = epubMargins
                )
            )
            }
        } catch (e: Throwable) {
            val ex = if (e is Exception) e else Exception(e.message, e)
            Result.failure(ex)
        }
    }

    private fun parseCssMargins(cssText: String): EpubMargins {
        val bodyMatch = Regex("""body\s*\{([^}]*)\}""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(cssText)?.groupValues?.get(1) ?: return EpubMargins()

        val marginMatch = Regex("""margin\s*:\s*([^;]+);""", RegexOption.IGNORE_CASE)
            .find(bodyMatch)?.groupValues?.get(1)?.trim() ?: return EpubMargins()

        val tokens = marginMatch.split(Regex("""\s+""")).mapNotNull { token ->
            val clean = token.trim().lowercase()
            when {
                clean.endsWith("%") -> clean.removeSuffix("%").toFloatOrNull()?.roundToInt()
                clean.endsWith("px") -> (clean.removeSuffix("px").toFloatOrNull()?.div(8f))?.roundToInt()
                clean.endsWith("pt") -> (clean.removeSuffix("pt").toFloatOrNull()?.div(6f))?.roundToInt()
                clean.endsWith("em") -> (clean.removeSuffix("em").toFloatOrNull()?.times(3f))?.roundToInt()
                else -> clean.toFloatOrNull()?.roundToInt()
            }
        }

        return when (tokens.size) {
            1 -> EpubMargins(tokens[0], tokens[0], tokens[0], tokens[0])
            2 -> EpubMargins(tokens[1], tokens[1], tokens[0], tokens[0])
            3 -> EpubMargins(tokens[1], tokens[1], tokens[0], tokens[2])
            4 -> EpubMargins(tokens[3], tokens[1], tokens[0], tokens[2])
            else -> EpubMargins()
        }
    }

    private fun extractParagraphsFromHtml(
        html: String,
        chapterDir: String,
        opfBaseDir: String,
        chapterTitle: String
    ): List<String> {
        val result = mutableListOf<String>()

        // Match block elements AND standalone/figure images in exact chronological order
        val blockOrImgRegex = Regex(
            """<(figure|p|h[1-6]|blockquote|li)([^>]*)>(.*?)</\1>|<img\b([^>]*)/?>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        val imgTagRegex = Regex("""<img\b([^>]*)/?>""", RegexOption.IGNORE_CASE)
        val srcAttrRegex = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val altAttrRegex = Regex("""alt=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        val widthPctRegex = Regex("""width\s*:\s*(\d+(?:\.\d+)?)%""", RegexOption.IGNORE_CASE)

        val matches = blockOrImgRegex.findAll(html).toList()

        fun resolveImagePath(rawSrc: String): String {
            val cleaned = rawSrc.trim().removePrefix("./")
            if (cleaned.startsWith("../")) {
                val up = cleaned.removePrefix("../")
                return if (opfBaseDir.isNotEmpty()) opfBaseDir + up else up
            }
            return if (chapterDir.isNotEmpty() && !cleaned.startsWith(chapterDir)) {
                chapterDir + cleaned
            } else {
                cleaned
            }
        }

        fun emitImageToken(attrs: String) {
            val src = srcAttrRegex.find(attrs)?.groupValues?.get(1) ?: return
            val alt = unescapeHtml(altAttrRegex.find(attrs)?.groupValues?.get(1) ?: "")
            val widthPct = widthPctRegex.find(attrs)?.groupValues?.get(1)?.toFloatOrNull()?.roundToInt() ?: 100
            val resolvedSrc = resolveImagePath(src)
            result.add("[[IMG:$resolvedSrc|$alt|$widthPct]]")
        }

        if (matches.isNotEmpty()) {
            for (match in matches) {
                val standaloneImgAttrs = match.groupValues[4]
                if (standaloneImgAttrs.isNotEmpty()) {
                    emitImageToken(standaloneImgAttrs)
                    continue
                }

                val tag = match.groupValues[1].lowercase()
                val attrs = match.groupValues[2]
                val inner = match.groupValues[3]

                // Skip decorative chapter-title H1 because Reader already renders chapter header
                if (tag == "h1" && attrs.contains("chapter-title", ignoreCase = true)) {
                    continue
                }

                if (tag == "figure") {
                    imgTagRegex.findAll(inner).forEach { imgMatch ->
                        emitImageToken(imgMatch.groupValues[1])
                    }
                    val captionMatch = Regex("""<figcaption[^>]*>(.*?)</figcaption>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(inner)
                    val caption = captionMatch?.groupValues?.get(1)?.let { unescapeHtml(stripTags(it)).trim() }.orEmpty()
                    if (caption.isNotBlank()) {
                        result.add("[[CENTER]]$caption")
                    }
                    continue
                }

                // If a <p> wraps an <img>, emit any embedded images cleanly
                val embeddedImgs = imgTagRegex.findAll(inner).toList()
                if (embeddedImgs.isNotEmpty()) {
                    embeddedImgs.forEach { imgMatch ->
                        emitImageToken(imgMatch.groupValues[1])
                    }
                }

                val cleaned = unescapeHtml(stripTags(inner)).trim()
                if (cleaned.isNotBlank()) {
                    // Avoid repeating exact chapter title as first element
                    if (result.isEmpty() && tag.startsWith("h") && cleaned.equals(chapterTitle.trim(), ignoreCase = true)) {
                        continue
                    }

                    val isCentered = attrs.contains("centered", ignoreCase = true) ||
                        attrs.contains("text-align: center", ignoreCase = true) ||
                        attrs.contains("text-align:center", ignoreCase = true)

                    val prefix = when (tag) {
                        "h1" -> "# "
                        "h2" -> "## "
                        "h3" -> "### "
                        "h4", "h5", "h6" -> "#### "
                        "blockquote" -> "❝ "
                        "li" -> "• "
                        else -> ""
                    }
                    val centerPrefix = if (isCentered && tag != "blockquote" && tag != "li") "[[CENTER]]" else ""
                    result.add("$centerPrefix$prefix$cleaned")
                }
            }
        } else {
            // Fallback: extract all body text split by lines
            val bodyRegex = Regex(
                """<body[^>]*>(.*?)</body>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
            val bodyContent = bodyRegex.find(html)?.groupValues?.get(1) ?: html
            val textOnly = stripTags(bodyContent)
            textOnly.lines().map { unescapeHtml(it).trim() }.filter { it.isNotBlank() }.forEach {
                result.add(it)
            }
        }
        return result
    }

    private data class ParsedTocSubEntry(
        val title: String,
        val level: Int
    )

    private fun parseNcxHierarchy(
        ncxContent: String,
        tocLabels: MutableMap<String, String>,
        tocSubEntriesByFile: MutableMap<String, MutableList<ParsedTocSubEntry>>
    ) {
        val tokenRegex = Regex("""<(/?)navPoint\b[^>]*>""", RegexOption.IGNORE_CASE)
        val labelRegex = Regex("""<text>(.*?)</text>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val contentRegex = Regex("""<content\s+[^>]*src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

        val tokens = tokenRegex.findAll(ncxContent).toList()
        var depth = 0
        for (i in tokens.indices) {
            val match = tokens[i]
            val isClosing = match.groupValues[1] == "/"
            if (isClosing) {
                depth = (depth - 1).coerceAtLeast(0)
                continue
            }
            depth++
            val sliceEnd = tokens.getOrNull(i + 1)?.range?.first ?: ncxContent.length
            val chunk = ncxContent.substring(match.range.last + 1, sliceEnd)
            val label = labelRegex.find(chunk)?.groupValues?.get(1)?.let { unescapeHtml(stripTags(it)).trim() }.orEmpty()
            val rawSrc = contentRegex.find(chunk)?.groupValues?.get(1)?.trim().orEmpty()
            if (label.isBlank() || rawSrc.isBlank()) continue

            val fileHref = rawSrc.substringBefore("#")
            if (depth == 1 && !rawSrc.contains("#")) {
                if (!tocLabels.containsKey(fileHref)) {
                    tocLabels[fileHref] = label
                }
            } else if (depth >= 2) {
                // depth 2 = Topic (H2), depth 3 = Sub-topic (H3), depth 4+ = Deeper sub-topic (H4)
                val headingLevel = depth.coerceIn(2, 4)
                tocSubEntriesByFile.getOrPut(fileHref) { mutableListOf() }
                    .add(ParsedTocSubEntry(title = label, level = headingLevel))
            }
        }
    }

    private fun parseNavXhtmlHierarchy(
        navContent: String,
        tocLabels: MutableMap<String, String>,
        tocSubEntriesByFile: MutableMap<String, MutableList<ParsedTocSubEntry>>
    ) {
        val navBlock = Regex("""<nav\b[^>]*epub:type=["']toc["'][^>]*>(.*?)</nav>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(navContent)?.groupValues?.get(1) ?: return

        val tokenRegex = Regex(
            """<(/?)ol\b[^>]*>|<a\b[^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        var olDepth = 0
        for (match in tokenRegex.findAll(navBlock)) {
            val full = match.value.lowercase()
            when {
                full.startsWith("<ol") -> olDepth++
                full.startsWith("</ol") -> olDepth = (olDepth - 1).coerceAtLeast(0)
                else -> {
                    val rawHref = match.groupValues[2].trim()
                    val label = unescapeHtml(stripTags(match.groupValues[3])).trim()
                    if (rawHref.isBlank() || label.isBlank()) continue
                    val fileHref = rawHref.substringBefore("#")
                    if (olDepth == 1 && !rawHref.contains("#")) {
                        if (!tocLabels.containsKey(fileHref)) {
                            tocLabels[fileHref] = label
                        }
                    } else if (olDepth >= 2) {
                        // Only populate from nav.xhtml if toc.ncx didn't already supply sub-entries for this file
                        val existing = tocSubEntriesByFile.getOrPut(fileHref) { mutableListOf() }
                        val alreadyHas = existing.any { it.title.equals(label, ignoreCase = true) }
                        if (!alreadyHas) {
                            existing.add(ParsedTocSubEntry(title = label, level = olDepth.coerceIn(2, 4)))
                        }
                    }
                }
            }
        }
    }

    private fun mergeSubHeadingsWithToc(
        htmlSubs: List<ReaderSubHeading>,
        tocSubs: List<ParsedTocSubEntry>,
        paragraphs: List<String>
    ): List<ReaderSubHeading> {
        if (tocSubs.isEmpty()) return htmlSubs
        if (htmlSubs.isEmpty()) {
            return tocSubs.map { toc ->
                val matchedIdx = paragraphs.indexOfFirst { p ->
                    val plain = p.trim().removePrefix("[[CENTER]]").replace(Regex("^#{1,4}\\s+"), "").trim()
                    plain.equals(toc.title, ignoreCase = true) || plain.contains(toc.title, ignoreCase = true)
                }.coerceAtLeast(0)
                ReaderSubHeading(title = toc.title, level = toc.level, paragraphIndex = matchedIdx)
            }
        }
        // Prefer HTML heading paragraph indices and update level from TOC when matched; append any missing TOC items
        val merged = htmlSubs.toMutableList()
        val existingKeys = merged.map { it.title.lowercase().trim() }.toMutableSet()
        for (toc in tocSubs) {
            val key = toc.title.lowercase().trim()
            val idx = merged.indexOfFirst { it.title.lowercase().trim() == key }
            if (idx >= 0) {
                merged[idx] = merged[idx].copy(level = toc.level)
            } else if (existingKeys.add(key)) {
                val matchedParaIdx = paragraphs.indexOfFirst { p ->
                    val plain = p.trim().removePrefix("[[CENTER]]").replace(Regex("^#{1,4}\\s+"), "").trim()
                    plain.equals(toc.title, ignoreCase = true) || plain.contains(toc.title, ignoreCase = true)
                }.coerceAtLeast(0)
                merged.add(ReaderSubHeading(title = toc.title, level = toc.level, paragraphIndex = matchedParaIdx))
            }
        }
        return merged.sortedBy { it.paragraphIndex }
    }

    private fun extractSubHeadings(paragraphs: List<String>): List<ReaderSubHeading> {
        val list = mutableListOf<ReaderSubHeading>()
        paragraphs.forEachIndexed { idx, raw ->
            val cleaned = raw.trim().removePrefix("[[CENTER]]").trim()
            when {
                cleaned.startsWith("## ") -> list.add(ReaderSubHeading(cleaned.removePrefix("## ").trim(), 2, idx))
                cleaned.startsWith("### ") -> list.add(ReaderSubHeading(cleaned.removePrefix("### ").trim(), 3, idx))
                cleaned.startsWith("#### ") -> list.add(ReaderSubHeading(cleaned.removePrefix("#### ").trim(), 4, idx))
            }
        }
        return list
    }

    private fun stripTags(html: String): String {
        return html.replace(Regex("""<[^>]+>"""), " ")
            .replace(Regex("""\s+"""), " ")
    }

    private fun unescapeHtml(text: String): String {
        return text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
    }
}
