package com.serma.pubify.converter

import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.util.Locale

data class ResolvedBookIdentity(
    val title: String,
    val author: String
)

/**
 * Multi-tier Book Title & Author extractor for PDFs:
 * 1. Embedded XMP XML metadata (elements & attributes) + PDF DocumentInformation (standard & custom keys)
 * 2. Cover / Title / Copyright page visual typography & text analysis (pages 1..10, checking author ABOVE & BELOW title)
 * 2B. Running headers across body pages (even/odd page author & title headers when cover is an image)
 * 3. Single-root PDF outline title fallback
 * 4. Smart filename parsing ("Title - Author.pdf", "Title by Author.pdf", "Title (Author).pdf") with SAF URI decoding
 * 4B. First prominent readable line fallback on early pages (so real PDFs rarely fall back to "Untitled Book")
 * 5. Clean human-readable fallbacks
 */
object PdfMetadataExtractor {

    private val junkTitles = setOf(
        "untitled", "untitled book", "selected document", "document", "microsoft word",
        "powerpoint presentation", "presentation", "slide 1", "ebook", "book", "pdf",
        "noname", "no title", "new document", "unknown", "my document", "scanned document",
        "scan", "temp", "tmp", "download", "file", "output", "export", "print", "latex",
        "indesign", "cover", "title", "page 1", "draft", "final", "copy", "imported book"
    )

    private val junkAuthors = setOf(
        "unknown", "unknown author", "author", "admin", "administrator", "user",
        "owner", "windows user", "pc", "hp", "dell", "lenovo", "asus", "acer",
        "apple", "mac", "macbook", "default", "anonymous", "none", "n/a", "na",
        "calibre", "adobe", "microsoft", "libreoffice", "openoffice", "google",
        "pdf", "latex", "tex", "pandoc", "scanner", "scan", "test", "guest",
        "staff", "editor", "publisher", "system", "root", "me", "student", "creator"
    )

    private val nonTitlePageNoiseRegex = Regex(
        """^(isbn[\s:-]*\d|issn|doi:|https?://|www\.|all rights reserved|copyright|©|\(c\)|printed in|published by|first\s+(?:published|printing|edition)|second edition|third edition|\d+(st|nd|rd|th)\s+edition|table of contents|contents|dedication|preface|foreword|acknowledgments?|introduction|chapter\s+\d+|page\s+\d+|\d+)$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Strict imprint/publisher line filter for TITLE rejection — intentionally does NOT reject
     * common book-title words like "book", "house", "library", "foundation", "company",
     * "group", "university", "edition", "media", or "international".
     */
    private val strictPublisherLineRegex = Regex(
        """^(?:published\s+by\b.*|(?:penguin|random\s+house|harpercollins|simon\s*&\s*schuster|macmillan|hachette|o'reilly\s+media|oreilly\s+media|pearson(?:\s+education)?|mcgraw[\s\-]*hill|springer(?:\s+nature)?|wiley|elsevier|packt\s+publishing|apress|manning\s+publications|oxford\s+university\s+press|cambridge\s+university\s+press|mit\s+press|princeton\s+university\s+press|harvard\s+university\s+press|vintage\s+books|anchor\s+books|bantam\s+books|crown\s+publishing|avery\s+publishing|portfolio\s+penguin|riverhead\s+books|little,\s*brown).*|.*\b(?:publishing\s+group|publishing\s+house|university\s+press|inc\.|ltd\.|llc|corp\.|publications?\s+inc\.?)\s*)$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Broader publisher/organization filter used ONLY when validating human AUTHOR candidates.
     */
    private val authorPublisherRejectRegex = Regex(
        """\b(press|publishing|publishers?|publications?|inc\.?|ltd\.?|llc|corp\.?|corporation|company|co\.|university|institute|association|foundation|group|media|edition|editions|international|library|springer|wiley|elsevier|pearson|mcgraw|o'reilly|oreilly|penguin|harpercollins|random\s+house|simon\s*&\s*schuster|macmillan|hachette|oxford|cambridge|packt|apress|manning|books\s+ltd|books\s+inc)\b""",
        RegexOption.IGNORE_CASE
    )

    private val explicitByAuthorRegex = Regex(
        """^(?:written\s+by|authored\s+by|edited\s+by|compiled\s+by|translated\s+by|prepared\s+by|authors?\s*[:\-]|by\s*[:\-]|by\s+)(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val copyrightAuthorPatterns = listOf(
        // "Copyright © 2018 by James Clear", "Text copyright © 2020, Matt Haig", "Copyright (c) 2021 Jane Doe"
        Regex(
            """(?:text\s+)?(?:copyright\s*)?(?:©|\(c\))\s*(?:\d{4}(?:\s*[-–,]\s*\d{4})*\s*,?\s*)?(?:by\s+)?([\p{Lu}][\p{L}.'\-]+(?:\s+(?:and|&|de|da|del|van|von|la|le|di|al|el|[\p{Lu}][\p{L}.'\-]+)){1,6})""",
            RegexOption.IGNORE_CASE
        ),
        // "Copyright 2020 by Matt Haig" or "Copyright 2020, Matt Haig" (no © symbol)
        Regex(
            """(?:text\s+)?copyright\s+\d{4}(?:\s*[-–,]\s*\d{4})*\s*,?\s*(?:by\s+)?([\p{Lu}][\p{L}.'\-]+(?:\s+(?:and|&|de|da|del|van|von|la|le|di|al|el|[\p{Lu}][\p{L}.'\-]+)){1,6})""",
            RegexOption.IGNORE_CASE
        ),
        // "Copyright by James Clear" (no year, no ©)
        Regex(
            """(?:text\s+)?copyright\s+by\s+([\p{Lu}][\p{L}.'\-]+(?:\s+(?:and|&|de|da|del|van|von|la|le|di|al|el|[\p{Lu}][\p{L}.'\-]+)){1,6})""",
            RegexOption.IGNORE_CASE
        ),
        // "© James Clear 2018" or "Copyright © James Clear, 2018"
        Regex(
            """(?:copyright\s*)?(?:©|\(c\))\s*([\p{Lu}][\p{L}.'\-]+(?:\s+(?:and|&|de|da|del|van|von|la|le|di|al|el|[\p{Lu}][\p{L}.'\-]+)){1,5})\s*,?\s*\d{4}""",
            RegexOption.IGNORE_CASE
        )
    )

    private val filenameNoiseTagRegex = Regex(
        """\s*[\(\[](?:z-lib\.org|z-library|libgen|pdfdrive|oceanofpdf|annas[\s\-]*archive|epdf|dokumen\.pub|vdoc\.pub|ebook|pdf|epub|scan|ocr|hq|retail|fixed|edition|\d{4})[^\)\]]*[\)\]]""",
        RegexOption.IGNORE_CASE
    )

    private val knownDocExtensionRegex = Regex(
        """\.(?:pdf|epub|mobi|azw3?|docx?|pptx?|txt|rtf|odt|indd|tex)$""",
        RegexOption.IGNORE_CASE
    )

    private val safPrefixRegex = Regex(
        """^(?:primary|home|raw|msf|document|downloads|image|audio|video|[0-9a-fA-F]{4}-[0-9a-fA-F]{4}):""",
        RegexOption.IGNORE_CASE
    )

    private val genericFileIdRegex = Regex(
        """^(?:msf|doc|document|file|pdf|download|img|scan|tmp|temp|book|ebook)[\s_\-:#]*\d+$|^\d{4,}$|^[a-f0-9]{12,}$""",
        RegexOption.IGNORE_CASE
    )

    private val preserveUpperAcronyms = setOf(
        "AI", "ML", "UI", "UX", "API", "APIS", "SQL", "NOSQL", "HTML", "CSS", "XML",
        "JSON", "HTTP", "HTTPS", "PDF", "EPUB", "USA", "UK", "EU", "UN", "DNA", "RNA",
        "CEO", "CTO", "CFO", "MBA", "PHD", "STEM", "IOT", "AWS", "GCP", "JVM", "SDK",
        "OS", "PC", "CPU", "GPU", "RAM", "DIY", "FAQ", "IQ", "EQ", "II", "III", "IV", "VI", "VII", "VIII", "IX", "XI", "XII"
    )

    private val minorTitleWords = setOf(
        "a", "an", "and", "as", "at", "but", "by", "for", "from", "in", "into",
        "nor", "of", "on", "or", "over", "per", "the", "to", "up", "via", "with"
    )

    /**
     * Resolves title and author directly from an open PDDocument (plus optional pre-extracted lines
     * so conversion doesn't need to re-strip text).
     */
    fun extract(
        document: PDDocument,
        fileName: String,
        outlineItems: List<ExtractedOutlineItem> = emptyList(),
        preExtractedPagesLines: Map<Int, List<ExtractedPdfLine>>? = null,
        tempDirForExtractor: File? = null
    ): ResolvedBookIdentity {
        // Tier 1A: XMP XML metadata (modern PDFs from InDesign, Acrobat, LaTeX, Word, Calibre)
        val (xmpTitle, xmpAuthor) = extractFromXmp(document)

        // Tier 1B: Legacy PDDocumentInformation (standard + custom keys)
        val info = try { document.documentInformation } catch (_: Throwable) { null }
        val infoTitle = sanitizeTitleCandidate(info?.title)
            ?: sanitizeTitleCandidate(try { info?.getCustomMetadataValue("Title") } catch (_: Throwable) { null })
        val infoAuthor = sanitizeAuthorCandidate(info?.author)
            ?: sanitizeAuthorCandidate(try { info?.getCustomMetadataValue("Author") } catch (_: Throwable) { null })
            ?: sanitizeAuthorCandidate(try { info?.getCustomMetadataValue("Authors") } catch (_: Throwable) { null })

        var bestTitle = xmpTitle ?: infoTitle
        var bestAuthor = xmpAuthor ?: infoAuthor

        val totalPages = try { document.numberOfPages } catch (_: Throwable) { 0 }
        val frontScanLimit = minOf(10, totalPages)
        var frontPagesLines: Map<Int, List<ExtractedPdfLine>> = emptyMap()

        // Tier 2A: Cover / Title / Copyright pages visual & text extraction (pages 1..10)
        if (bestTitle == null || bestAuthor == null) {
            frontPagesLines = if (preExtractedPagesLines != null) {
                (1..frontScanLimit)
                    .mapNotNull { pg -> preExtractedPagesLines[pg]?.let { pg to it } }
                    .toMap()
            } else {
                extractPagesLines(document, 1, frontScanLimit, tempDirForExtractor)
            }

            if (frontPagesLines.isNotEmpty()) {
                val (pageTitle, pageAuthor) = extractFromFrontPages(frontPagesLines)
                if (bestTitle == null && pageTitle != null) {
                    bestTitle = pageTitle
                }
                if (bestAuthor == null && pageAuthor != null) {
                    bestAuthor = pageAuthor
                }
            }
        }

        // Tier 2B: Running headers across body pages (recovers Author/Title when cover is a flat raster image)
        if ((bestTitle == null || bestAuthor == null) && totalPages >= 4) {
            val headerEndPage = minOf(18, totalPages)
            val bodyPagesForHeaders = if (preExtractedPagesLines != null) {
                (2..headerEndPage)
                    .mapNotNull { pg -> preExtractedPagesLines[pg]?.let { pg to it } }
                    .toMap()
            } else {
                val combined = linkedMapOf<Int, List<ExtractedPdfLine>>()
                frontPagesLines.forEach { (pg, lines) -> if (pg >= 2) combined[pg] = lines }
                val nextStart = (frontScanLimit + 1).coerceAtLeast(2)
                if (nextStart <= headerEndPage) {
                    combined.putAll(extractPagesLines(document, nextStart, headerEndPage, tempDirForExtractor))
                }
                combined
            }

            if (bodyPagesForHeaders.size >= 2) {
                val (headerTitle, headerAuthor) = extractFromRunningHeaders(bodyPagesForHeaders, bestTitle, bestAuthor)
                if (bestTitle == null && headerTitle != null) {
                    bestTitle = headerTitle
                }
                if (bestAuthor == null && headerAuthor != null) {
                    bestAuthor = headerAuthor
                }
            }
        }

        // Tier 3: Single-root PDF outline title fallback
        if (bestTitle == null && outlineItems.isNotEmpty()) {
            val topLevel = outlineItems.filter { it.level == 0 }
            if (topLevel.size == 1 && outlineItems.size > 1) {
                bestTitle = sanitizeTitleCandidate(topLevel.first().title)
            }
        }

        // Tier 4: Smart filename parsing ("Title - Author.pdf", "Title by Author.pdf", etc.)
        val (fileTitle, fileAuthor) = parseFromFileName(fileName)
        if (bestTitle == null && fileTitle != null) {
            bestTitle = fileTitle
        }
        if (bestAuthor == null && fileAuthor != null) {
            bestAuthor = fileAuthor
        }

        // Tier 4B: Last-resort first prominent heading/readable line from early pages (avoid "Untitled Book" whenever PDF has text)
        if (bestTitle == null && frontPagesLines.isNotEmpty()) {
            bestTitle = extractFallbackHeadingFromPages(frontPagesLines)
        }

        // Tier 5: Clean safe fallbacks
        val rawCleanFile = cleanRawFileName(fileName).takeIf { !isGenericTitle(it) }
        val finalTitle = bestTitle
            ?: rawCleanFile
            ?: "Untitled Book"

        val finalAuthor = bestAuthor ?: "Unknown Author"

        return ResolvedBookIdentity(
            title = finalTitle,
            author = finalAuthor
        )
    }

    /**
     * Parses a PDF/EPUB filename like "Atomic_Habits_-_James_Clear_(z-lib.org).pdf" or
     * "Deep Work by Cal Newport.pdf" into (Title?, Author?).
     */
    fun parseFromFileName(fileName: String): Pair<String?, String?> {
        val base = cleanRawFileName(fileName)
        if (base.isBlank() || genericFileIdRegex.matches(base)) return null to null

        // Pattern 1: "Title by Author"
        val byMatch = Regex("""^(.{3,95}?)\s+by\s+([\p{L}][\p{L}.'\-]+(?:\s+(?:and|&|[\p{L}][\p{L}.'\-]+)){0,5})$""", RegexOption.IGNORE_CASE)
            .find(base)
        if (byMatch != null) {
            val t = sanitizeTitleCandidate(byMatch.groupValues[1])
            val a = sanitizeAuthorCandidate(byMatch.groupValues[2])
            if (t != null && a != null) return t to a
        }

        // Pattern 2: "Title (Author)" where parenthetical looks like a person's name
        val parenMatch = Regex("""^(.{3,95}?)\s*\(([\p{L}][\p{L}.'\-]+(?:\s+(?:and|&|[\p{L}][\p{L}.'\-]+)){1,5})\)$""")
            .find(base)
        if (parenMatch != null) {
            val t = sanitizeTitleCandidate(parenMatch.groupValues[1])
            val candidateAuthor = parenMatch.groupValues[2].trim()
            val a = if (looksLikeHumanName(candidateAuthor)) sanitizeAuthorCandidate(candidateAuthor) else null
            if (t != null && a != null) return t to a
        }

        // Pattern 3: "Part A - Part B" or "Part A — Part B"
        val dashParts = base.split(Regex("""\s+[\-–—]\s+""")).map { it.trim() }.filter { it.isNotBlank() }
        if (dashParts.size == 2) {
            val left = dashParts[0]
            val right = dashParts[1]
            val rightIsName = looksLikeHumanName(right)
            val leftIsName = looksLikeHumanName(left)
            if (rightIsName && !leftIsName) {
                val t = sanitizeTitleCandidate(left)
                val a = sanitizeAuthorCandidate(right)
                if (t != null && a != null) return t to a
            } else if (leftIsName && !rightIsName) {
                val t = sanitizeTitleCandidate(right)
                val a = sanitizeAuthorCandidate(left)
                if (t != null && a != null) return t to a
            } else if (rightIsName && left.length >= right.length) {
                val t = sanitizeTitleCandidate(left)
                val a = sanitizeAuthorCandidate(right)
                if (t != null && a != null) return t to a
            }
        }

        val cleanedTitle = sanitizeTitleCandidate(base)
        return cleanedTitle to null
    }

    fun cleanRawFileName(fileName: String): String {
        var name = try {
            Uri.decode(fileName) ?: fileName
        } catch (_: Throwable) {
            fileName
        }
        name = safPrefixRegex.replace(name.trim(), "")
        name = name.substringAfterLast('/').substringAfterLast('\\')
        name = safPrefixRegex.replace(name, "")
        // Only strip known document extensions — preserves dots inside titles like "Dr. Jekyll"
        name = knownDocExtensionRegex.replace(name, "")
        name = filenameNoiseTagRegex.replace(name, " ")
        name = name.replace('_', ' ')
        // Replace dots used as word separators (when no spaces exist)
        if (!name.contains(' ') && name.count { it == '.' } >= 2) {
            name = name.replace('.', ' ')
        }
        // Strip leading timestamp prefixes like "1732001234567_" or "1732001234567-"
        name = name.replace(Regex("""^\d{10,14}\s*[-_ ]+\s*"""), "")
        name = name.replace(Regex("""\s+"""), " ").trim(' ', '-', '_', '.', '–', '—')
        return name
    }

    fun isGenericTitle(title: String?): Boolean {
        if (title.isNullOrBlank()) return true
        val trimmed = title.trim()
        val norm = trimmed.lowercase(Locale.ROOT)
        if (norm in junkTitles) return true
        if (genericFileIdRegex.matches(trimmed)) return true
        val cleaned = cleanRawFileName(trimmed)
        if (cleaned.isBlank() || cleaned.lowercase(Locale.ROOT) in junkTitles || genericFileIdRegex.matches(cleaned)) {
            return true
        }
        return sanitizeTitleCandidate(trimmed) == null
    }

    fun isGenericAuthor(author: String?): Boolean {
        if (author.isNullOrBlank()) return true
        val norm = author.trim().lowercase(Locale.ROOT)
        if (norm in junkAuthors) return true
        return sanitizeAuthorCandidate(author) == null
    }

    /**
     * Also usable by EpubParser to rescue an author from front-matter plain text paragraphs
     * when OPF <dc:creator> is missing or "Unknown Author".
     */
    fun extractAuthorFromPlainText(paragraphs: List<String>, knownTitle: String? = null): String? {
        val sample = paragraphs.take(40).map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotBlank() }
        for (i in sample.indices) {
            val line = sample[i]
            if (line.length > 180) continue
            if (Regex("""^(?:by|written\s+by|authored\s+by|edited\s+by|author\s*:?)$""", RegexOption.IGNORE_CASE).matches(line)) {
                val next = sample.getOrNull(i + 1)
                val cand = sanitizeAuthorCandidate(next)
                if (cand != null && !authorPublisherRejectRegex.containsMatchIn(cand) && !cand.equals(knownTitle, ignoreCase = true)) {
                    return cand
                }
            }
            val byMatch = explicitByAuthorRegex.find(line)
            if (byMatch != null) {
                val cand = sanitizeAuthorCandidate(byMatch.groupValues[1])
                if (cand != null && !authorPublisherRejectRegex.containsMatchIn(cand) && !cand.equals(knownTitle, ignoreCase = true)) {
                    return cand
                }
            }
        }
        for (line in sample) {
            for (pattern in copyrightAuthorPatterns) {
                val m = pattern.find(line) ?: continue
                val rawCand = m.groupValues[1].trim()
                if (!authorPublisherRejectRegex.containsMatchIn(rawCand) && looksLikeHumanName(rawCand)) {
                    val auth = sanitizeAuthorCandidate(rawCand)
                    if (auth != null && !auth.equals(knownTitle, ignoreCase = true)) return auth
                }
            }
        }
        return null
    }

    private fun extractFromXmp(document: PDDocument): Pair<String?, String?> {
        return try {
            val metadata = document.documentCatalog?.metadata ?: return null to null
            val xmpXml = metadata.exportXMPMetadata()?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: return null to null

            // <dc:title><rdf:Alt><rdf:li ...>Book Title</rdf:li></rdf:Alt></dc:title>
            val dcTitleBlock = Regex("""<(?:dc:)?title\b[^>]*>(.*?)</(?:dc:)?title>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                .find(xmpXml)?.groupValues?.get(1)
            val rawXmpTitle = dcTitleBlock?.let { block ->
                Regex("""<rdf:li[^>]*>(.*?)</rdf:li>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                    .find(block)?.groupValues?.get(1) ?: block.replace(Regex("<[^>]+>"), " ")
            }?.let { unescapeXml(it) }
                // XML attribute syntax: dc:title="Book Title" or pdf:Title="Book Title"
                ?: Regex("""\b(?:dc:title|pdf:Title)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    .find(xmpXml)?.groupValues?.get(1)?.let { unescapeXml(it) }

            // <dc:creator><rdf:Seq><rdf:li>Author Name</rdf:li></rdf:Seq></dc:creator>
            val dcCreatorBlock = Regex("""<(?:dc:)?creator\b[^>]*>(.*?)</(?:dc:)?creator>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                .find(xmpXml)?.groupValues?.get(1)
            val rawXmpCreators = dcCreatorBlock?.let { block ->
                val listItems = Regex("""<rdf:li[^>]*>(.*?)</rdf:li>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                    .findAll(block)
                    .map { unescapeXml(it.groupValues[1]).trim() }
                    .filter { it.isNotBlank() }
                    .toList()
                if (listItems.isNotEmpty()) listItems.joinToString(", ")
                else unescapeXml(block.replace(Regex("<[^>]+>"), " ")).trim()
            }
                ?: Regex("""<pdf:Author\b[^>]*>(.*?)</pdf:Author>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                    .find(xmpXml)?.groupValues?.get(1)?.let { unescapeXml(it.replace(Regex("<[^>]+>"), " ")) }
                ?: Regex("""\b(?:dc:creator|pdf:Author)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    .find(xmpXml)?.groupValues?.get(1)?.let { unescapeXml(it) }

            sanitizeTitleCandidate(rawXmpTitle) to sanitizeAuthorCandidate(rawXmpCreators)
        } catch (_: Throwable) {
            null to null
        }
    }

    private fun extractPagesLines(
        document: PDDocument,
        startPage: Int,
        endPage: Int,
        tempDir: File?
    ): Map<Int, List<ExtractedPdfLine>> {
        val result = linkedMapOf<Int, List<ExtractedPdfLine>>()
        val safeStart = startPage.coerceAtLeast(1)
        val safeEnd = minOf(endPage, document.numberOfPages)
        if (safeStart > safeEnd) return result

        val dir = tempDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp", "meta_probe")
        val extractor = PdfStructureExtractor(
            tempImageDir = dir,
            compressionLevel = ImageCompressionLevel.BALANCED,
            customImageQuality = null
        )
        for (page in safeStart..safeEnd) {
            try {
                val lines = extractor.extractPageLines(document, page)
                if (lines.isNotEmpty()) {
                    result[page] = lines
                }
            } catch (_: Throwable) {
                // Skip unreadable page
            }
        }
        return result
    }

    private fun extractFromFrontPages(
        frontPages: Map<Int, List<ExtractedPdfLine>>
    ): Pair<String?, String?> {
        var detectedTitle: String? = null
        var detectedAuthor: String? = null

        val orderedPages = frontPages.entries.sortedBy { it.key }

        for ((_, rawLines) in orderedPages) {
            val cleanLines = rawLines
                .map { it.copy(text = it.text.replace(Regex("\\s+"), " ").trim()) }
                .filter { it.text.isNotBlank() && it.text.length <= 160 }

            if (cleanLines.isEmpty()) continue

            // Skip TOC pages completely when hunting for book title/author
            val isTocPage = cleanLines.take(6).any {
                it.text.equals("table of contents", ignoreCase = true) ||
                    it.text.equals("contents", ignoreCase = true)
            }
            if (isTocPage) continue

            // Identify dense prose chapter pages (e.g. page 6+ with >26 lines and starting with Chapter/Prologue)
            val isBodyChapterPage = cleanLines.size > 28 && cleanLines.take(4).any {
                Regex("""^(?:chapter|prologue|introduction|part)\b""", RegexOption.IGNORE_CASE).containsMatchIn(it.text)
            }

            // Pure copyright / colophon page: skip for title, but great source for author!
            val isCopyrightPage = cleanLines.count {
                it.text.contains("copyright", ignoreCase = true) ||
                    it.text.contains("isbn", ignoreCase = true) ||
                    it.text.contains("all rights reserved", ignoreCase = true) ||
                    it.text.contains("library of congress", ignoreCase = true)
            } >= 2

            // 1) Explicit "By <Author>" / "By: <Author>" / "Author: <Name>" / standalone "BY" + next line
            if (detectedAuthor == null && !isBodyChapterPage) {
                for (i in cleanLines.indices) {
                    val text = cleanLines[i].text

                    // Skip "Foreword by ..." / "Introduction by ..." unless nothing else exists
                    if (Regex("""^(?:foreword|preface|introduction|afterword|cover\s+design|illustrated)\s+by\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                        continue
                    }

                    // Standalone "By" or "Written by" followed by the author's name on the next line
                    if (Regex("""^(?:by|written\s+by|authored\s+by|edited\s+by|authors?\s*:?)$""", RegexOption.IGNORE_CASE).matches(text)) {
                        val nextLine = cleanLines.getOrNull(i + 1)?.text
                        val candidate = sanitizeAuthorCandidate(nextLine)
                        if (candidate != null && !authorPublisherRejectRegex.containsMatchIn(candidate)) {
                            detectedAuthor = candidate
                            break
                        }
                    }

                    val match = explicitByAuthorRegex.find(text)
                    if (match != null) {
                        val candidate = sanitizeAuthorCandidate(match.groupValues[1])
                        if (candidate != null && !authorPublisherRejectRegex.containsMatchIn(candidate)) {
                            detectedAuthor = candidate
                            break
                        }
                    }
                }
            }

            // 2) Title detection on non-copyright, non-body front pages (pick largest prominent font block)
            if (detectedTitle == null && !isCopyrightPage && !isBodyChapterPage) {
                val eligibleLines = cleanLines.filter { line ->
                    val t = line.text
                    t.length in 2..110 &&
                        !nonTitlePageNoiseRegex.matches(t) &&
                        !explicitByAuthorRegex.matches(t) &&
                        !t.equals("by", ignoreCase = true) &&
                        !strictPublisherLineRegex.matches(t) &&
                        line.topY <= line.pageHeight * 0.88f
                }

                if (eligibleLines.isNotEmpty()) {
                    val maxFont = eligibleLines.maxOf { it.fontSizePt }
                    val minFont = cleanLines.minOf { it.fontSizePt }
                    val isSparseTitlePage = cleanLines.size <= 20
                    // Require either clearly prominent typography OR sparse cover/title page
                    val isTitlePageCandidate = (maxFont >= minFont * 1.20f) || isSparseTitlePage

                    if (isTitlePageCandidate) {
                        val titleThreshold = maxFont * 0.85f
                        // Find all lines at the title font size; skip a single top human-name line ONLY if there is
                        // another equal/larger non-name title block right below it (e.g. big author name on cover)
                        var firstTitleIdx = eligibleLines.indexOfFirst { it.fontSizePt >= titleThreshold }
                        if (firstTitleIdx >= 0 && looksLikeHumanName(eligibleLines[firstTitleIdx].text)) {
                            val altIdx = eligibleLines.indices.firstOrNull { idx ->
                                idx > firstTitleIdx &&
                                    eligibleLines[idx].fontSizePt >= titleThreshold &&
                                    !looksLikeHumanName(eligibleLines[idx].text)
                            }
                            if (altIdx != null) {
                                if (detectedAuthor == null) {
                                    detectedAuthor = sanitizeAuthorCandidate(eligibleLines[firstTitleIdx].text)
                                }
                                firstTitleIdx = altIdx
                            }
                        }

                        if (firstTitleIdx >= 0) {
                            val firstTitleLine = eligibleLines[firstTitleIdx]
                            val titleParts = mutableListOf(firstTitleLine.text)
                            var prevLine = firstTitleLine
                            for (j in (firstTitleIdx + 1) until eligibleLines.size) {
                                val next = eligibleLines[j]
                                val verticalGap = next.topY - prevLine.topY
                                val maxLineGap = maxOf(prevLine.fontSizePt * 2.5f, 48f)
                                if (next.fontSizePt >= titleThreshold &&
                                    verticalGap in -4f..maxLineGap &&
                                    titleParts.size < 4
                                ) {
                                    titleParts.add(next.text)
                                    prevLine = next
                                } else {
                                    break
                                }
                            }
                            val combined = titleParts.joinToString(" ").replace(Regex("\\s+"), " ").trim()
                            val sanitized = sanitizeTitleCandidate(combined)
                            if (sanitized != null) {
                                detectedTitle = sanitized
                            }

                            // 3) Check for human-name author line BOTH ABOVE and BELOW the title on this title page
                            if (detectedAuthor == null) {
                                val titleTopY = firstTitleLine.topY
                                val titleBottomY = prevLine.topY
                                val candidateAuthorLines = cleanLines.filter { line ->
                                    val outsideTitleBlock = (line.topY > titleBottomY + 2f) || (line.topY < titleTopY - 2f)
                                    outsideTitleBlock &&
                                        !titleParts.any { it.equals(line.text, ignoreCase = true) }
                                }
                                // Prefer lines below title first, then lines above title
                                val orderedCandidates = candidateAuthorLines.filter { it.topY > titleBottomY + 2f } +
                                    candidateAuthorLines.filter { it.topY < titleTopY - 2f }

                                for (cand in orderedCandidates) {
                                    val t = cand.text
                                    if (nonTitlePageNoiseRegex.matches(t) || authorPublisherRejectRegex.containsMatchIn(t)) continue
                                    if (looksLikeHumanName(t)) {
                                        val auth = sanitizeAuthorCandidate(t)
                                        if (auth != null && !auth.equals(detectedTitle, ignoreCase = true)) {
                                            detectedAuthor = auth
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4) Copyright line author fallback (supports ©, (c), and plain "Copyright 2020 Matt Haig")
            if (detectedAuthor == null) {
                for (line in cleanLines) {
                    var matchedAuthor: String? = null
                    for (pattern in copyrightAuthorPatterns) {
                        val m = pattern.find(line.text) ?: continue
                        val rawCandidate = m.groupValues[1].trim()
                        if (!authorPublisherRejectRegex.containsMatchIn(rawCandidate) && looksLikeHumanName(rawCandidate)) {
                            val auth = sanitizeAuthorCandidate(rawCandidate)
                            if (auth != null && !auth.equals(detectedTitle, ignoreCase = true)) {
                                matchedAuthor = auth
                                break
                            }
                        }
                    }
                    if (matchedAuthor != null) {
                        detectedAuthor = matchedAuthor
                        break
                    }
                }
            }

            if (detectedTitle != null && detectedAuthor != null) break
        }

        return detectedTitle to detectedAuthor
    }

    /**
     * Tier 2B: Recovers Author & Book Title from classic repeated running headers at the top
     * of body pages (e.g., even pages = "JAMES CLEAR", odd pages = "ATOMIC HABITS").
     */
    private fun extractFromRunningHeaders(
        pages: Map<Int, List<ExtractedPdfLine>>,
        existingTitle: String?,
        existingAuthor: String?
    ): Pair<String?, String?> {
        val evenHeaderCounts = mutableMapOf<String, Int>()
        val oddHeaderCounts = mutableMapOf<String, Int>()
        val allHeaderCounts = mutableMapOf<String, Int>()

        for ((pageNum, lines) in pages) {
            if (lines.isEmpty()) continue
            val topLine = lines
                .filter { it.topY <= it.pageHeight * 0.14f }
                .minByOrNull { it.topY }
                ?: continue

            // Strip leading/trailing page numbers like "14  ATOMIC HABITS" or "JAMES CLEAR  15"
            val cleaned = topLine.text
                .replace(Regex("""^\d{1,4}\s+"""), "")
                .replace(Regex("""\s+\d{1,4}$"""), "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            if (cleaned.length !in 3..90) continue
            if (nonTitlePageNoiseRegex.matches(cleaned)) continue
            if (Regex("""^(?:chapter|part|section|unit|module)\b""", RegexOption.IGNORE_CASE).containsMatchIn(cleaned)) continue

            val key = cleaned
            allHeaderCounts[key] = (allHeaderCounts[key] ?: 0) + 1
            if (pageNum % 2 == 0) {
                evenHeaderCounts[key] = (evenHeaderCounts[key] ?: 0) + 1
            } else {
                oddHeaderCounts[key] = (oddHeaderCounts[key] ?: 0) + 1
            }
        }

        var title = existingTitle
        var author = existingAuthor

        val topEven = evenHeaderCounts.entries.filter { it.value >= 2 }.maxByOrNull { it.value }?.key
        val topOdd = oddHeaderCounts.entries.filter { it.value >= 2 }.maxByOrNull { it.value }?.key

        val distinctHeaders = listOfNotNull(topEven, topOdd)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .ifEmpty {
                allHeaderCounts.entries
                    .filter { it.value >= 2 }
                    .sortedByDescending { it.value }
                    .take(2)
                    .map { it.key }
            }

        for (header in distinctHeaders) {
            // Check if a single running header contains "Title — Author" or "Title - Author"
            val (splitTitle, splitAuthor) = parseFromFileName(header)
            if (splitTitle != null && splitAuthor != null) {
                if (title == null) title = splitTitle
                if (author == null) author = splitAuthor
                continue
            }

            if (looksLikeHumanName(header) && !authorPublisherRejectRegex.containsMatchIn(header)) {
                if (author == null) {
                    val auth = sanitizeAuthorCandidate(header)
                    if (auth != null && !auth.equals(title, ignoreCase = true)) {
                        author = auth
                    }
                }
            } else {
                if (title == null && !strictPublisherLineRegex.matches(header)) {
                    val candTitle = sanitizeTitleCandidate(header)
                    if (candTitle != null && !candTitle.equals(author, ignoreCase = true)) {
                        title = candTitle
                    }
                }
            }
        }

        return title to author
    }

    /**
     * Tier 4B: Last-resort extraction of the most prominent readable heading from pages 1..5
     * when XMP, Info, title-page heuristics, running headers, outline, and filename all failed.
     */
    private fun extractFallbackHeadingFromPages(pages: Map<Int, List<ExtractedPdfLine>>): String? {
        val earlyPages = pages.entries.sortedBy { it.key }.take(5)
        for ((_, rawLines) in earlyPages) {
            val candidates = rawLines
                .map { it.copy(text = it.text.replace(Regex("\\s+"), " ").trim()) }
                .filter { line ->
                    val t = line.text
                    t.length in 3..110 &&
                        !nonTitlePageNoiseRegex.matches(t) &&
                        !strictPublisherLineRegex.matches(t) &&
                        !explicitByAuthorRegex.matches(t) &&
                        !looksLikeHumanName(t)
                }
            if (candidates.isEmpty()) continue
            val bestLine = candidates.maxByOrNull { it.fontSizePt } ?: continue
            val sanitized = sanitizeTitleCandidate(bestLine.text)
            if (sanitized != null) return sanitized
        }
        return null
    }

    private fun sanitizeTitleCandidate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var s = raw
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '_', ':', ';', '|', '"', '\'')

        // Strip common office/editor export prefixes (e.g., "Microsoft Word - Book Title.docx")
        s = s.replace(Regex("""^(?:Microsoft\s+(?:Word|PowerPoint|Excel)\s*[-–:]\s*|PowerPoint\s+Presentation\s*[-–:]\s*)""", RegexOption.IGNORE_CASE), "")
        s = knownDocExtensionRegex.replace(s, "")
        s = filenameNoiseTagRegex.replace(s, " ").replace(Regex("\\s+"), " ").trim()

        if (s.length < 2 || s.length > 160) return null
        val lower = s.lowercase(Locale.ROOT)
        if (lower in junkTitles) return null
        if (genericFileIdRegex.matches(s)) return null
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://")) return null
        // Reject raw file paths like C:\Users\... or /Users/...
        if (Regex("""^[A-Za-z]:\\|^/(?:Users|home|var|tmp|storage)/""").containsMatchIn(s)) return null
        // Reject purely numeric or hex-hash titles
        if (Regex("""^[\d\s\-_.]+$""").matches(s)) return null
        if (Regex("""^[a-f0-9]{16,}$""", RegexOption.IGNORE_CASE).matches(s)) return null
        // Reject single-line "Chapter 1" or "Table of Contents"
        if (Regex("""^(?:chapter|part|section)\s+\d+$""", RegexOption.IGNORE_CASE).matches(s)) return null

        return normalizeAllCapsTitle(s)
    }

    private fun sanitizeAuthorCandidate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var s = raw
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '_', ':', ';', '|', ',', '.', '"', '\'')

        // Strip leading "by " or "Author: "
        s = s.replace(Regex("""^(?:written\s+by|authored\s+by|edited\s+by|compiled\s+by|authors?\s*[:\-]|by\s*[:\-]|by)\s+""", RegexOption.IGNORE_CASE), "").trim()
        // Strip trailing credentials/dates if parenthetical (e.g., "James Clear (2018)")
        s = s.replace(Regex("""\s*\(\d{4}.*?\)$"""), "").trim()
        // Strip trailing "All rights reserved" if copyright line bled over
        s = s.replace(Regex("""[.,;]?\s*all\s+rights\s+reserved.*$""", RegexOption.IGNORE_CASE), "").trim(' ', ',', '.', ';')

        if (s.length < 2 || s.length > 100) return null
        val lower = s.lowercase(Locale.ROOT)
        if (lower in junkAuthors) return null
        if (lower.contains("@") || lower.startsWith("http") || lower.startsWith("www.")) return null
        if (Regex("""\d{3,}""").containsMatchIn(s)) return null
        if (s.count { it.isLetter() } < 2) return null
        val wordCount = s.split(Regex("\\s+")).size
        if (wordCount > 12) return null

        return normalizeAllCapsName(s)
    }

    private fun normalizeAllCapsTitle(text: String): String {
        val letters = text.filter { it.isLetter() }
        if (letters.length < 5 || letters.any { it.isLowerCase() }) return text
        val words = text.split(Regex("\\s+"))
        return words.mapIndexed { idx, word ->
            val core = word.trim('.', ',', ':', ';', '!', '?', '"', '\'', '(', ')')
            val upperCore = core.uppercase(Locale.ROOT)
            when {
                upperCore in preserveUpperAcronyms -> word
                idx > 0 && idx < words.lastIndex && core.lowercase(Locale.ROOT) in minorTitleWords ->
                    word.lowercase(Locale.ROOT)
                else -> capitalizeWord(word)
            }
        }.joinToString(" ")
    }

    private fun normalizeAllCapsName(text: String): String {
        val letters = text.filter { it.isLetter() }
        if (letters.length < 4 || letters.any { it.isLowerCase() }) return text
        val lowerParticles = setOf("and", "de", "da", "del", "van", "von", "la", "le", "di", "bin", "al", "el")
        val words = text.split(Regex("\\s+"))
        return words.mapIndexed { idx, word ->
            val core = word.trim('.', ',', '\'')
            val upperCore = core.uppercase(Locale.ROOT)
            when {
                upperCore in setOf("II", "III", "IV") -> word
                core.length == 1 && word.endsWith(".") -> word // Keep initials like "J."
                idx > 0 && core.lowercase(Locale.ROOT) in lowerParticles -> word.lowercase(Locale.ROOT)
                else -> capitalizeWord(word)
            }
        }.joinToString(" ")
    }

    private fun capitalizeWord(word: String): String {
        if (word.isEmpty()) return word
        // Handle hyphenated names/words like "SAINT-EXUPÉRY" -> "Saint-Exupéry"
        if (word.contains('-')) {
            return word.split('-').joinToString("-") { part -> capitalizeSingleToken(part) }
        }
        return capitalizeSingleToken(word)
    }

    private fun capitalizeSingleToken(token: String): String {
        val lower = token.lowercase(Locale.ROOT)
        val firstLetterIdx = lower.indexOfFirst { it.isLetter() }
        if (firstLetterIdx < 0) return token
        return lower.substring(0, firstLetterIdx) +
            lower[firstLetterIdx].titlecase(Locale.ROOT) +
            lower.substring(firstLetterIdx + 1)
    }

    /**
     * Checks whether a short string looks like one or more human author names
     * (e.g., "James Clear", "MATT HAIG", "J. K. Rowling", "Robert C. Martin",
     * "Dr. Maya Angelou", "Andrew Hunt and David Thomas", "Gamma, Helm, Johnson, Vlissides").
     */
    private fun looksLikeHumanName(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length !in 4..95) return false
        if (trimmed.any { it.isDigit() }) return false
        if (trimmed.contains(':') || trimmed.contains('?') || trimmed.contains('!') || trimmed.contains('/')) return false
        if (authorPublisherRejectRegex.containsMatchIn(trimmed)) return false

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size !in 2..10) return false

        val connectorWords = setOf(
            "and", "&", "with", "de", "da", "del", "van", "von", "la", "le", "di", "bin",
            "al", "el", "jr", "sr", "ii", "iii", "iv", "dr", "prof", "mr", "ms", "mrs", "phd", "md"
        )
        val stopWords = setOf(
            "the", "a", "an", "in", "on", "of", "for", "to", "from", "into", "about",
            "guide", "handbook", "manual", "introduction", "complete", "modern",
            "practical", "essential", "advanced", "principles", "foundations",
            "edition", "volume", "chapter", "part", "book", "books", "series", "revised",
            "updated", "study", "notes", "summary", "workbook", "textbook", "report",
            "analysis", "review", "theory", "design", "systems", "programming", "science",
            "atomic", "habits", "midnight", "library", "story", "stories", "novel",
            "history", "world", "life", "mind", "art", "power", "secret", "secrets",
            "how", "why", "what", "when", "where", "your", "our", "my", "this", "that",
            "new", "york", "times", "bestseller", "bestselling", "copyright", "reserved",
            "contents", "table", "index", "appendix", "preface", "foreword", "prologue", "epilogue"
        )

        var capitalizedNameWords = 0
        for (w in words) {
            val cleanToken = w.trim('.', ',', '\'', '"')
            val cleanLower = cleanToken.lowercase(Locale.ROOT)
            if (cleanLower in stopWords) return false
            if (cleanLower in connectorWords) continue
            val firstChar = cleanToken.firstOrNull() ?: return false
            if (!firstChar.isLetter() || !firstChar.isUpperCase()) return false
            // Allow initials like "J." / "J.K." or capitalized/ALL-CAPS name words (including Unicode letters & hyphens)
            if (!Regex("""^[\p{Lu}](?:\.|[\p{L}'\-\.]+)$""").matches(w.trimEnd(','))) return false
            capitalizedNameWords++
        }
        return capitalizedNameWords >= 2
    }

    private fun unescapeXml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .trim()
    }
}
