package com.serma.pubify.converter

import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubBuilder {

    fun buildEpub(
        outputFile: File,
        title: String,
        author: String,
        chapters: List<ConvertedChapter>,
        coverImageFile: File?,
        typography: TypographyPreset = TypographyPreset.CLEAN_MODERN,
        fontFamily: EpubFontFamily = EpubFontFamily.SANS_SERIF,
        fontSizePt: Int = ConversionOptions.DEFAULT_FONT_SIZE_PT,
        marginHorizontalPercent: Int = 5,
        marginVerticalPercent: Int = 4,
        marginLeftPercent: Int = marginHorizontalPercent,
        marginRightPercent: Int = marginHorizontalPercent,
        marginTopPercent: Int = marginVerticalPercent,
        marginBottomPercent: Int = marginVerticalPercent,
        images: List<EpubImageAsset> = emptyList()
    ): Boolean {
        val zipOut = ZipOutputStream(FileOutputStream(outputFile))
        try {
            val bookId = "urn:uuid:" + UUID.randomUUID().toString()

            // 1. Write uncompressed mimetype entry at offset 0
            val mimetypeBytes = "application/epub+zip".toByteArray(Charsets.US_ASCII)
            val mimetypeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimetypeBytes.size.toLong()
                compressedSize = mimetypeBytes.size.toLong()
                val crc = CRC32()
                crc.update(mimetypeBytes)
                setCrc(crc.value)
            }
            zipOut.putNextEntry(mimetypeEntry)
            zipOut.write(mimetypeBytes)
            zipOut.closeEntry()

            // Subsequent files are deflated (standard compression)
            zipOut.setMethod(ZipOutputStream.DEFLATED)

            // 2. Write META-INF/container.xml
            zipOut.putNextEntry(ZipEntry("META-INF/container.xml"))
            val containerXml = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".trimIndent()
            zipOut.write(containerXml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 3. Write OEBPS/styles.css
            zipOut.putNextEntry(ZipEntry("OEBPS/styles.css"))
            val selectedFontCss = fontFamily.fontCss
            val css = """
@charset "utf-8";
body {
    $selectedFontCss
    font-size: ${fontSizePt}pt;
    line-height: 1.65;
    margin: ${marginTopPercent}% ${marginRightPercent}% ${marginBottomPercent}% ${marginLeftPercent}%;
    color: #212121;
    background-color: #FFFFFF;
}
h1, h2, h3, h4, h5, h6 {
    font-weight: 700;
    line-height: 1.3;
    margin-top: 1.5em;
    margin-bottom: 0.55em;
    page-break-after: avoid;
    color: #1A237E;
}
h1.chapter-title {
    font-size: 1.85em;
    text-align: center;
    margin-top: 1.8em;
    margin-bottom: 1.1em;
    border-bottom: 2px solid #E0E0E0;
    padding-bottom: 0.5em;
}
h2 {
    font-size: 1.42em;
    margin-top: 1.5em;
    margin-bottom: 0.55em;
}
h3 {
    font-size: 1.20em;
    margin-top: 1.3em;
    margin-bottom: 0.45em;
    color: #283593;
}
h4 {
    font-size: 1.06em;
    font-weight: 600;
    margin-top: 1.1em;
    margin-bottom: 0.4em;
    color: #3949AB;
}
.centered {
    text-align: center !important;
    text-indent: 0 !important;
}
p {
    margin: 0 0 0.9em 0;
    text-align: justify;
    text-justify: inter-word;
}
p.indented {
    text-indent: 1.4em;
}
p.flush, p.first-after-heading {
    text-indent: 0;
}
ul.epub-list {
    margin: 0.6em 0 1em 1.4em;
    padding-left: 0.8em;
}
ul.epub-list li {
    margin-bottom: 0.45em;
}
blockquote {
    margin: 1em 2em;
    padding-left: 1em;
    border-left: 3px solid #7986CB;
    font-style: italic;
    color: #424242;
}
.epub-figure {
    margin: 1.25em auto;
    text-align: center;
    page-break-inside: avoid;
}
.epub-figure img {
    max-width: 100%;
    height: auto;
    display: block;
    margin: 0 auto;
    border-radius: 4px;
}
.cover-container {
    text-align: center;
    padding: 0;
    margin: 0;
    height: 100vh;
}
.cover-image {
    max-width: 100%;
    max-height: 100%;
    height: auto;
    width: auto;
    object-fit: contain;
    display: block;
    margin: 0 auto;
}
.page-break {
    page-break-before: always;
}
""".trimIndent()
            zipOut.write(css.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 4. If cover image exists, write it to OEBPS/cover.jpg and OEBPS/cover.xhtml
            val hasCover = coverImageFile != null && coverImageFile.exists() && coverImageFile.length() > 0
            if (hasCover) {
                zipOut.putNextEntry(ZipEntry("OEBPS/cover.jpg"))
                coverImageFile.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()

                zipOut.putNextEntry(ZipEntry("OEBPS/cover.xhtml"))
                val coverXhtml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">
<head>
  <title>Cover</title>
  <link rel="stylesheet" type="text/css" href="styles.css" />
  <style>
    body { margin: 0; padding: 0; text-align: center; background-color: #000000; }
    img { max-width: 100%; max-height: 100vh; height: auto; width: auto; object-fit: contain; }
  </style>
</head>
<body>
  <div class="cover-container">
    <img src="cover.jpg" alt="${escapeXml(title)}" class="cover-image" />
  </div>
</body>
</html>""".trimIndent()
                zipOut.write(coverXhtml.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }

            // 5. Write each chapter XHTML file
            chapters.forEachIndexed { i, chapter ->
                val fileName = "OEBPS/chapter_${i + 1}.xhtml"
                zipOut.putNextEntry(ZipEntry(fileName))

                val safeTitle = escapeXml(chapter.title.ifBlank { "Chapter ${i + 1}" })
                val chapterXhtml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">
<head>
  <meta charset="utf-8" />
  <title>$safeTitle</title>
  <link rel="stylesheet" type="text/css" href="styles.css" />
</head>
<body>
  <section epub:type="chapter">
    <h1 class="chapter-title">$safeTitle</h1>
    <div class="chapter-content">
${chapter.htmlContent}
    </div>
  </section>
</body>
</html>""".trimIndent()
                zipOut.write(chapterXhtml.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }

            val chapterSubEntries = chapters.map { ch ->
                extractSubTocEntries(ch.htmlContent)
            }
            val hasAnySubEntries = chapterSubEntries.any { it.isNotEmpty() }
            val hasDeepSubTopics = chapterSubEntries.any { list -> list.any { it.level >= 3 } }

            // 6. Write EPUB 3 Navigation Document OEBPS/nav.xhtml with nested Chapter -> Topic -> Sub-topic tree
            zipOut.putNextEntry(ZipEntry("OEBPS/nav.xhtml"))
            val navXhtml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">
<head>
  <meta charset="utf-8" />
  <title>Table of Contents</title>
  <link rel="stylesheet" type="text/css" href="styles.css" />
</head>
<body>
  <nav epub:type="toc" id="toc">
    <h1>Table of Contents</h1>
    <ol>
""")
                if (hasCover) {
                    append("""      <li><a href="cover.xhtml">Cover</a></li>""" + "\n")
                }
                chapters.forEachIndexed { index, chapter ->
                    val safeChapterTitle = escapeXml(chapter.title.ifBlank { "Chapter ${index + 1}" })
                    val subs = chapterSubEntries.getOrElse(index) { emptyList() }
                    val chHref = "chapter_${index + 1}.xhtml"
                    if (subs.isEmpty()) {
                        append("""      <li><a href="$chHref">$safeChapterTitle</a></li>""" + "\n")
                    } else {
                        append("""      <li><a href="$chHref">$safeChapterTitle</a>""" + "\n")
                        append("""        <ol>""" + "\n")
                        var iSub = 0
                        while (iSub < subs.size) {
                            val entry = subs[iSub]
                            if (entry.level <= 2) {
                                // Collect consecutive sub-topics (level >= 3) under this topic
                                val children = mutableListOf<SubTocEntry>()
                                var j = iSub + 1
                                while (j < subs.size && subs[j].level >= 3) {
                                    children.add(subs[j])
                                    j++
                                }
                                if (children.isEmpty()) {
                                    append("""          <li><a href="$chHref#${entry.id}">${escapeXml(entry.label)}</a></li>""" + "\n")
                                } else {
                                    append("""          <li><a href="$chHref#${entry.id}">${escapeXml(entry.label)}</a>""" + "\n")
                                    append("""            <ol>""" + "\n")
                                    children.forEach { child ->
                                        append("""              <li><a href="$chHref#${child.id}">${escapeXml(child.label)}</a></li>""" + "\n")
                                    }
                                    append("""            </ol>""" + "\n")
                                    append("""          </li>""" + "\n")
                                }
                                iSub = j
                            } else {
                                append("""          <li><a href="$chHref#${entry.id}">${escapeXml(entry.label)}</a></li>""" + "\n")
                                iSub++
                            }
                        }
                        append("""        </ol>""" + "\n")
                        append("""      </li>""" + "\n")
                    }
                }
                append("""    </ol>
  </nav>
</body>
</html>""")
            }
            zipOut.write(navXhtml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 7. Write EPUB 2 NCX file OEBPS/toc.ncx with nested Chapter -> Topic -> Sub-topic navPoints
            zipOut.putNextEntry(ZipEntry("OEBPS/toc.ncx"))
            val ncxDepth = when {
                hasDeepSubTopics -> 3
                hasAnySubEntries -> 2
                else -> 1
            }
            val ncxXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="$bookId"/>
    <meta name="dtb:depth" content="$ncxDepth"/>
    <meta name="dtb:totalPageCount" content="0"/>
    <meta name="dtb:maxPageNumber" content="0"/>
  </head>
  <docTitle>
    <text>${escapeXml(title)}</text>
  </docTitle>
  <docAuthor>
    <text>${escapeXml(author)}</text>
  </docAuthor>
  <navMap>
""")
                var playOrder = 1
                if (hasCover) {
                    append("""    <navPoint id="navPoint-$playOrder" playOrder="$playOrder">
      <navLabel><text>Cover</text></navLabel>
      <content src="cover.xhtml"/>
    </navPoint>
""")
                    playOrder++
                }
                chapters.forEachIndexed { index, chapter ->
                    val safeChapterTitle = escapeXml(chapter.title.ifBlank { "Chapter ${index + 1}" })
                    val subs = chapterSubEntries.getOrElse(index) { emptyList() }
                    val chHref = "chapter_${index + 1}.xhtml"
                    append("""    <navPoint id="navPoint-$playOrder" playOrder="$playOrder">
      <navLabel><text>$safeChapterTitle</text></navLabel>
      <content src="$chHref"/>
""")
                    playOrder++
                    var iSub = 0
                    while (iSub < subs.size) {
                        val entry = subs[iSub]
                        if (entry.level <= 2) {
                            val children = mutableListOf<SubTocEntry>()
                            var j = iSub + 1
                            while (j < subs.size && subs[j].level >= 3) {
                                children.add(subs[j])
                                j++
                            }
                            append("""      <navPoint id="navPoint-$playOrder" playOrder="$playOrder">
        <navLabel><text>${escapeXml(entry.label)}</text></navLabel>
        <content src="$chHref#${entry.id}"/>
""")
                            playOrder++
                            children.forEach { child ->
                                append("""        <navPoint id="navPoint-$playOrder" playOrder="$playOrder">
          <navLabel><text>${escapeXml(child.label)}</text></navLabel>
          <content src="$chHref#${child.id}"/>
        </navPoint>
""")
                                playOrder++
                            }
                            append("""      </navPoint>
""")
                            iSub = j
                        } else {
                            append("""      <navPoint id="navPoint-$playOrder" playOrder="$playOrder">
        <navLabel><text>${escapeXml(entry.label)}</text></navLabel>
        <content src="$chHref#${entry.id}"/>
      </navPoint>
""")
                            playOrder++
                            iSub++
                        }
                    }
                    append("""    </navPoint>
""")
                }
                append("""  </navMap>
</ncx>""")
            }
            zipOut.write(ncxXml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 7b. Write all extracted embedded images to OEBPS/images/
            val uniqueImages = images.distinctBy { it.fileName }.filter { it.file.exists() && it.file.length() > 0 }
            uniqueImages.forEach { img ->
                zipOut.putNextEntry(ZipEntry("OEBPS/images/${img.fileName}"))
                img.file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }

            // 8. Write OEBPS/content.opf
            zipOut.putNextEntry(ZipEntry("OEBPS/content.opf"))
            val opfXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="3.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
    <dc:identifier id="BookId">$bookId</dc:identifier>
    <dc:title>${escapeXml(title)}</dc:title>
    <dc:creator id="creator">${escapeXml(author)}</dc:creator>
    <meta refines="#creator" property="role" scheme="marc:relators">aut</meta>
    <dc:language>en</dc:language>
    <meta property="dcterms:modified">${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())}</meta>
""")
                if (hasCover) {
                    append("""    <meta name="cover" content="cover-image" />""" + "\n")
                }
                append("""  </metadata>
  <manifest>
    <item id="css" href="styles.css" media-type="text/css"/>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
""")
                if (hasCover) {
                    append("""    <item id="cover-image" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>""" + "\n")
                    append("""    <item id="cover-page" href="cover.xhtml" media-type="application/xhtml+xml"/>""" + "\n")
                }
                uniqueImages.forEach { img ->
                    append("""    <item id="${escapeXml(img.id)}" href="images/${escapeXml(img.fileName)}" media-type="${escapeXml(img.mediaType)}"/>""" + "\n")
                }
                chapters.forEachIndexed { index, _ ->
                    append("""    <item id="chapter_${index + 1}" href="chapter_${index + 1}.xhtml" media-type="application/xhtml+xml"/>""" + "\n")
                }
                append("""  </manifest>
  <spine toc="ncx">
""")
                if (hasCover) {
                    append("""    <itemref idref="cover-page" />""" + "\n")
                }
                chapters.forEachIndexed { index, _ ->
                    append("""    <itemref idref="chapter_${index + 1}" />""" + "\n")
                }
                append("""  </spine>
  <guide>
""")
                if (hasCover) {
                    append("""    <reference type="cover" title="Cover" href="cover.xhtml"/>""" + "\n")
                }
                if (chapters.isNotEmpty()) {
                    append("""    <reference type="text" title="Beginning" href="chapter_1.xhtml"/>""" + "\n")
                }
                append("""  </guide>
</package>""")
            }
            zipOut.write(opfXml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.finish()
            return true
        } finally {
            zipOut.close()
        }
    }

    private data class SubTocEntry(
        val id: String,
        val label: String,
        val level: Int
    )

    private fun extractSubTocEntries(htmlContent: String): List<SubTocEntry> {
        val regex = Regex("""<h([234])\b[^>]*id=["']([^"']+)["'][^>]*>(.*?)</h\1>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        return regex.findAll(htmlContent).mapNotNull { match ->
            val level = match.groupValues[1].toIntOrNull() ?: 2
            val id = match.groupValues[2].trim()
            val rawLabel = match.groupValues[3]
                .replace(Regex("""<[^>]+>"""), " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace(Regex("""\s+"""), " ")
                .trim()
            if (id.isNotBlank() && rawLabel.isNotBlank()) SubTocEntry(id, rawLabel, level) else null
        }.toList()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
