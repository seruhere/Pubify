package com.serma.pubify

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.serma.pubify.ui.viewmodel.ConverterViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pubify", appName)
  }

  @Test
  fun `test pdf to epub conversion`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val converter = com.serma.pubify.converter.PdfToEpubConverter(app)

    // Create a simple 1-page PDF using PDFBox
    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
    val page = com.tom_roush.pdfbox.pdmodel.PDPage()
    doc.addPage(page)
    val font = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA
    val contentStream = com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page)
    contentStream.beginText()
    contentStream.setFont(font, 12f)
    contentStream.newLineAtOffset(100f, 700f)
    contentStream.showText("Chapter 1: The Beginning")
    contentStream.newLineAtOffset(0f, -20f)
    contentStream.showText("This is test body text for conversion verification.")
    contentStream.endText()
    contentStream.close()

    val pdfFile = java.io.File(app.cacheDir, "sample_test.pdf")
    doc.save(pdfFile)
    doc.close()

    val options = com.serma.pubify.converter.ConversionOptions(
      title = "Sample Book",
      author = "Test Author",
      extractCover = true
    )

    val progressUpdates = mutableListOf<com.serma.pubify.converter.ConversionProgress>()
    val result = converter.convert(android.net.Uri.fromFile(pdfFile), options) { progress ->
      progressUpdates.add(progress)
    }

    assertTrue("Conversion should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
    val epub = result.getOrNull()
    assertTrue(epub != null && epub.exists() && epub.length() > 0)
  }

  @Test
  fun `test bookmark insertion retrieval and deletion`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<PdfToEpubApp>()
    val repository = app.repository

    val bookmark1 = com.serma.pubify.data.model.BookBookmark(
      bookId = 101L,
      bookTitle = "Sample Book",
      chapterIndex = 0,
      chapterTitle = "Chapter 1: The Beginning",
      pageNumber = 5,
      totalBookPages = 42,
      scrollOffsetFraction = 0.35f,
      snippet = "This is test body text for conversion verification."
    )

    val id = repository.saveBookmark(bookmark1)
    assertTrue("Inserted bookmark id should be > 0", id > 0)

    val bookmarks = repository.getBookmarksForBook(101L).first()
    assertEquals(1, bookmarks.size)
    assertEquals(5, bookmarks[0].pageNumber)
    assertEquals(42, bookmarks[0].totalBookPages)
    assertEquals("Chapter 1: The Beginning", bookmarks[0].chapterTitle)

    repository.deleteBookmarkById(id)
    val remaining = repository.getBookmarksForBook(101L).first()
    assertEquals(0, remaining.size)
  }

  @Test
  fun `test pdf to epub preserves images margins headings subheadings and chapters`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val converter = com.serma.pubify.converter.PdfToEpubConverter(app)
    val parser = com.serma.pubify.reader.EpubParser()

    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
    val boldFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    val regularFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA

    // Create a colorful 64x64 bitmap to embed inside Page 1
    val bmp = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
    for (x in 0 until 64) {
      for (y in 0 until 64) {
        bmp.setPixel(x, y, if ((x + y) % 2 == 0) android.graphics.Color.BLUE else android.graphics.Color.RED)
      }
    }
    val pdImage = com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromImage(doc, bmp)
    bmp.recycle()

    // Page 1: ~12% left margin (x = 72pt out of 612pt), Chapter 1, Section Heading, Sub-heading, Paragraph, Image
    val page1 = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(page1)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page1).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 20f)
      cs.newLineAtOffset(72f, 720f)
      cs.showText("Chapter 1: Foundations")
      cs.endText()

      cs.beginText()
      cs.setFont(boldFont, 15f)
      cs.newLineAtOffset(72f, 675f)
      cs.showText("Major Section Overview")
      cs.endText()

      cs.beginText()
      cs.setFont(boldFont, 12.5f)
      cs.newLineAtOffset(72f, 645f)
      cs.showText("1.1 Key Concepts")
      cs.endText()

      cs.beginText()
      cs.setFont(regularFont, 11f)
      cs.newLineAtOffset(72f, 620f)
      cs.showText("This is the opening paragraph explaining the core ideas in clear detail.")
      cs.newLineAtOffset(0f, -15f)
      cs.showText("Additional body text ensures 11pt is detected as the dominant font size.")
      cs.endText()

      cs.drawImage(pdImage, 120f, 380f, 200f, 160f)
    }

    // Page 2: Chapter 2
    val page2 = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(page2)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page2).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 20f)
      cs.newLineAtOffset(72f, 720f)
      cs.showText("Chapter 2: Advanced Topics")
      cs.endText()

      cs.beginText()
      cs.setFont(regularFont, 11f)
      cs.newLineAtOffset(72f, 680f)
      cs.showText("Here begins the second chapter with its own body paragraphs and layout.")
      cs.newLineAtOffset(0f, -15f)
      cs.showText("Consistent margins and hierarchy keep the reader experience faithful.")
      cs.endText()
    }

    val pdfFile = java.io.File(app.cacheDir, "structured_layout_test.pdf")
    doc.save(pdfFile)
    doc.close()

    val options = com.serma.pubify.converter.ConversionOptions(
      title = "Structured PDF",
      author = "Test Author",
      splitMode = com.serma.pubify.converter.ChapterSplitMode.AUTO_DETECT,
      margin = com.serma.pubify.converter.EpubMargin.MATCH_PDF,
      extractCover = true
    )

    val result = converter.convert(android.net.Uri.fromFile(pdfFile), options) {}
    assertTrue("Conversion should succeed", result.isSuccess)
    val epubFile = result.getOrNull()!!

    // Parse resulting EPUB and assert chapters, headings, sub-headings, images, and margins
    val parsedResult = parser.parse(epubFile)
    assertTrue("EPUB parsing should succeed", parsedResult.isSuccess)
    val parsedBook = parsedResult.getOrNull()!!

    assertEquals("Should match 2 chapters from PDF", 2, parsedBook.chapters.size)
    assertTrue("Chapter 1 title should match", parsedBook.chapters[0].title.contains("Chapter 1"))
    assertTrue("Chapter 2 title should match", parsedBook.chapters[1].title.contains("Chapter 2"))

    val ch1Paras = parsedBook.chapters[0].paragraphs
    assertTrue("Chapter 1 should preserve section heading (##)", ch1Paras.any { it.startsWith("## ") && it.contains("Major Section Overview") })
    assertTrue("Chapter 1 should preserve sub-heading (###)", ch1Paras.any { it.startsWith("### ") && it.contains("1.1 Key Concepts") })
    assertTrue("Chapter 1 should include embedded image token", ch1Paras.any { it.startsWith("[[IMG:") })
    assertTrue("Parsed EPUB should contain extracted image bytes", parsedBook.bookImages.isNotEmpty())
    assertTrue("Left margin should reflect ~12% PDF margin (>= 9%)", parsedBook.epubMargins.leftPercent >= 9)
  }

  @Test
  fun `test reading session persistence and stats calculation`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<PdfToEpubApp>()
    val repository = app.repository
    repository.clearAllReadingSessions()

    val now = System.currentTimeMillis()
    repository.saveReadingSession(
      com.serma.pubify.data.model.ReadingSession(
        bookId = 1L,
        bookTitle = "Book A",
        durationSeconds = 1800L,
        pagesRead = 20,
        timestampMillis = now
      )
    )
    repository.saveReadingSession(
      com.serma.pubify.data.model.ReadingSession(
        bookId = 2L,
        bookTitle = "Book B",
        durationSeconds = 1200L,
        pagesRead = 10,
        timestampMillis = now
      )
    )

    val sessions = repository.allReadingSessions.first()
    val books = listOf(
      com.serma.pubify.data.model.ConvertedBook(
        id = 1L,
        title = "Book A",
        author = "Author",
        originalFileName = "a.pdf",
        epubFilePath = "/tmp/a.epub",
        chapterCount = 3,
        lastReadChapterIndex = 2,
        isCompleted = true
      ),
      com.serma.pubify.data.model.ConvertedBook(
        id = 2L,
        title = "Book B",
        author = "Author",
        originalFileName = "b.pdf",
        epubFilePath = "/tmp/b.epub",
        chapterCount = 4,
        lastReadChapterIndex = 1,
        isCompleted = false
      )
    )

    val summary = com.serma.pubify.data.model.ReadingStatsCalculator.calculate(books, sessions)
    assertEquals(3000L, summary.totalReadingTimeSeconds)
    assertEquals(15f, summary.averagePagesPerSession, 0.01f)
    assertEquals(1, summary.completedBooksCount)
    assertEquals(1, summary.inProgressBooksCount)
    assertEquals("50m", summary.formattedTotalTime)
  }

  @Test
  fun `test nested pdf outlines populate chapters and subheadings in epub parser`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val converter = com.serma.pubify.converter.PdfToEpubConverter(app)
    val parser = com.serma.pubify.reader.EpubParser()

    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
    val boldFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    val regularFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA

    fun addPageWithText(heading: String, sub: String?, body: String): com.tom_roush.pdfbox.pdmodel.PDPage {
      val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
      doc.addPage(page)
      com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
        cs.beginText()
        cs.setFont(boldFont, 18f)
        cs.newLineAtOffset(72f, 720f)
        cs.showText(heading)
        cs.endText()
        if (sub != null) {
          cs.beginText()
          cs.setFont(boldFont, 13.5f)
          cs.newLineAtOffset(72f, 680f)
          cs.showText(sub)
          cs.endText()
        }
        cs.beginText()
        cs.setFont(regularFont, 11f)
        cs.newLineAtOffset(72f, 645f)
        cs.showText(body)
        cs.endText()
      }
      return page
    }

    val p1 = addPageWithText("Chapter 1: Architecture", "1.1 Overview", "System components and clean boundaries make books readable.")
    val p2 = addPageWithText("Chapter 2: Implementation", "2.1 Pipelines", "Pipelines transform PDF layouts accurately into reflowable EPUB.")

    // Root bookmark wrapping level-1 chapters and level-2 subsections
    val outline = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline()
    doc.documentCatalog.documentOutline = outline

    val bookRoot = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "Engineering Handbook"
      setDestination(p1)
    }
    val ch1Node = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "Chapter 1: Architecture"
      setDestination(p1)
    }
    val ch1Sub = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "1.1 Overview"
      setDestination(p1)
    }
    val ch2Node = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "Chapter 2: Implementation"
      setDestination(p2)
    }
    val ch2Sub = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "2.1 Pipelines"
      setDestination(p2)
    }
    ch1Node.addLast(ch1Sub)
    ch2Node.addLast(ch2Sub)
    bookRoot.addLast(ch1Node)
    bookRoot.addLast(ch2Node)
    outline.addLast(bookRoot)

    val pdfFile = java.io.File(app.cacheDir, "nested_outline_test.pdf")
    doc.save(pdfFile)
    doc.close()

    val result = converter.convert(
      android.net.Uri.fromFile(pdfFile),
      com.serma.pubify.converter.ConversionOptions(
        title = "Engineering Handbook",
        author = "Tester",
        splitMode = com.serma.pubify.converter.ChapterSplitMode.AUTO_DETECT
      )
    ) {}
    assertTrue("Conversion should succeed", result.isSuccess)
    val parsedBook = parser.parse(result.getOrNull()!!).getOrThrow()

    assertEquals("Should unwrap book root and yield 2 real chapters", 2, parsedBook.chapters.size)
    assertTrue("Chapter 1 title preserved", parsedBook.chapters[0].title.contains("Chapter 1"))
    assertTrue("Chapter 2 title preserved", parsedBook.chapters[1].title.contains("Chapter 2"))
    assertTrue("Chapter 1 subheadings extracted", parsedBook.chapters[0].subHeadings.any { it.title.contains("1.1 Overview") })
    assertTrue("Chapter 2 subheadings extracted", parsedBook.chapters[1].subHeadings.any { it.title.contains("2.1 Pipelines") })
  }

  @Test
  fun `test two-line chapter headers front matter and vector figures match pdf`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val converter = com.serma.pubify.converter.PdfToEpubConverter(app)
    val parser = com.serma.pubify.reader.EpubParser()

    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
    val boldFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    val regularFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA

    // Page 1: Front Matter (copyright / dedication)
    val frontPage = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(frontPage)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, frontPage).use { cs ->
      cs.beginText()
      cs.setFont(regularFont, 11f)
      cs.newLineAtOffset(72f, 700f)
      cs.showText("Copyright 2025 Test Press. Dedicated to curious readers everywhere.")
      cs.endText()
    }

    // Page 2: Two-line Chapter 1 + Vector Chart/Diagram + Raster Bitmap
    val page2 = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(page2)

    val bmp = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
    for (x in 0 until 48) {
      for (y in 0 until 48) {
        bmp.setPixel(x, y, if (x < 24) android.graphics.Color.GREEN else android.graphics.Color.MAGENTA)
      }
    }
    val pdImg = com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromImage(doc, bmp)
    bmp.recycle()

    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page2).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 13f)
      cs.newLineAtOffset(72f, 725f)
      cs.showText("CHAPTER 1")
      cs.endText()

      cs.beginText()
      cs.setFont(boldFont, 22f)
      cs.newLineAtOffset(72f, 695f)
      cs.showText("The Beginning")
      cs.endText()

      cs.beginText()
      cs.setFont(regularFont, 11f)
      cs.newLineAtOffset(72f, 650f)
      cs.showText("Every great journey begins with a clear first chapter and faithful diagrams.")
      cs.endText()

      // Vector figure: multi-bar diagram
      cs.setNonStrokingColor(30, 100, 200)
      cs.addRect(100f, 450f, 40f, 120f)
      cs.fill()
      cs.setNonStrokingColor(220, 80, 50)
      cs.addRect(155f, 450f, 40f, 90f)
      cs.fill()
      cs.setStrokingColor(20, 20, 20)
      cs.setLineWidth(2f)
      cs.moveTo(90f, 445f)
      cs.lineTo(220f, 445f)
      cs.stroke()

      // Raster image below vector diagram (separated vertically so both stay distinct)
      cs.drawImage(pdImg, 100f, 180f, 140f, 120f)
    }

    // Page 3: Two-line Chapter 2
    val page3 = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(page3)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page3).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 13f)
      cs.newLineAtOffset(72f, 725f)
      cs.showText("CHAPTER 2")
      cs.endText()

      cs.beginText()
      cs.setFont(boldFont, 22f)
      cs.newLineAtOffset(72f, 695f)
      cs.showText("The Continuation")
      cs.endText()

      cs.beginText()
      cs.setFont(regularFont, 11f)
      cs.newLineAtOffset(72f, 650f)
      cs.showText("The second chapter continues cleanly without numbering drift.")
      cs.endText()
    }

    val pdfFile = java.io.File(app.cacheDir, "two_line_vector_test.pdf")
    doc.save(pdfFile)
    doc.close()

    val result = converter.convert(
      android.net.Uri.fromFile(pdfFile),
      com.serma.pubify.converter.ConversionOptions(
        title = "Two Line Test",
        author = "Tester",
        splitMode = com.serma.pubify.converter.ChapterSplitMode.AUTO_DETECT,
        extractCover = false
      )
    ) {}
    assertTrue("Conversion should succeed", result.isSuccess)
    val parsedBook = parser.parse(result.getOrNull()!!).getOrThrow()

    assertEquals("Front Matter + Chapter 1 + Chapter 2 = 3 chapters", 3, parsedBook.chapters.size)
    assertEquals("Front Matter", parsedBook.chapters[0].title)
    assertEquals("CHAPTER 1: The Beginning", parsedBook.chapters[1].title)
    assertEquals("CHAPTER 2: The Continuation", parsedBook.chapters[2].title)

    val ch1Images = parsedBook.chapters[1].paragraphs.count { it.startsWith("[[IMG:") }
    assertTrue("Expected both vector figure and raster bitmap in Chapter 1 (found $ch1Images)", ch1Images >= 2)
    assertTrue("Book images map should contain at least 2 extracted visual assets", parsedBook.bookImages.size >= 2)
  }

  @Test
  fun `test exact chapter matching ignores printed toc and nests topics and subtopics without inflating chapter count`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val converter = com.serma.pubify.converter.PdfToEpubConverter(app)
    val parser = com.serma.pubify.reader.EpubParser()

    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
    val boldFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    val regularFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA

    // Page 1: Printed Table of Contents (must NOT generate fake chapters)
    val tocPage = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(tocPage)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, tocPage).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 18f)
      cs.newLineAtOffset(72f, 720f)
      cs.showText("Table of Contents")
      cs.endText()

      val tocEntries = listOf(
        Triple(72f, "Chapter 1: Foundations ....................... 2", true),
        Triple(92f, "1.1 Core Principles .......................... 2", false),
        Triple(112f, "1.1.1 First Law ............................. 2", false),
        Triple(92f, "1.2 Design Patterns .......................... 3", false),
        Triple(72f, "Chapter 2: Systems ........................... 4", true),
        Triple(92f, "2.1 Architecture ............................. 4", false),
        Triple(112f, "2.1.1 Layers ................................ 4", false),
        Triple(72f, "Chapter 3: Practice .......................... 5", true)
      )
      var y = 685f
      for ((x, label, isBold) in tocEntries) {
        cs.beginText()
        cs.setFont(if (isBold) boldFont else regularFont, 11f)
        cs.newLineAtOffset(x, y)
        cs.showText(label)
        cs.endText()
        y -= 18f
      }
    }

    // Page 2: Chapter 1 with large-font Topic 1.1, Sub-topic 1.1.1, and deceptive "Section 1.1" / "Part of the..." lines
    val page2 = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(page2)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page2).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 20f)
      cs.newLineAtOffset(72f, 720f)
      cs.showText("Chapter 1: Foundations")
      cs.endText()

      cs.beginText()
      cs.setFont(boldFont, 16f)
      cs.newLineAtOffset(72f, 680f)
      cs.showText("1.1 Core Principles")
      cs.endText()

      cs.beginText()
      cs.setFont(boldFont, 13f)
      cs.newLineAtOffset(72f, 650f)
      cs.showText("1.1.1 First Law")
      cs.endText()

      cs.beginText()
      cs.setFont(regularFont, 11f)
      cs.newLineAtOffset(72f, 620f)
      cs.showText("Part of the strength of a clean document is keeping chapters accurately grouped.")
      cs.newLineAtOffset(0f, -16f)
      cs.showText("Book design relies on keeping topics and sub-topics inside their parent chapter.")
      cs.endText()
    }

    // Page 3: Continuation of Chapter 1 with large-font Topic 1.2 + "Section 1.2 Overview" (must stay inside Chapter 1!)
    val page3 = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(page3)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page3).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 16f)
      cs.newLineAtOffset(72f, 720f)
      cs.showText("1.2 Design Patterns")
      cs.endText()

      cs.beginText()
      cs.setFont(boldFont, 15f)
      cs.newLineAtOffset(72f, 685f)
      cs.showText("Section 1.2 Overview")
      cs.endText()

      cs.beginText()
      cs.setFont(regularFont, 11f)
      cs.newLineAtOffset(72f, 650f)
      cs.showText("Even large-font section headings belong inside Chapter 1 as topics, not new chapters.")
      cs.endText()
    }

    // Page 4: Chapter 2
    val page4 = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(page4)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page4).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 20f)
      cs.newLineAtOffset(72f, 720f)
      cs.showText("Chapter 2: Systems")
      cs.endText()

      cs.beginText()
      cs.setFont(boldFont, 16f)
      cs.newLineAtOffset(72f, 680f)
      cs.showText("2.1 Architecture")
      cs.endText()

      cs.beginText()
      cs.setFont(boldFont, 13f)
      cs.newLineAtOffset(72f, 650f)
      cs.showText("2.1.1 Layers")
      cs.endText()

      cs.beginText()
      cs.setFont(regularFont, 11f)
      cs.newLineAtOffset(72f, 620f)
      cs.showText("Layered architecture cleanly isolates parsing, pagination, and rendering.")
      cs.endText()
    }

    // Page 5: Chapter 3
    val page5 = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(page5)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page5).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 20f)
      cs.newLineAtOffset(72f, 720f)
      cs.showText("Chapter 3: Practice")
      cs.endText()

      cs.beginText()
      cs.setFont(regularFont, 11f)
      cs.newLineAtOffset(72f, 680f)
      cs.showText("Final chapter content verifying exact chapter count without inflation.")
      cs.endText()
    }

    val pdfFile = java.io.File(app.cacheDir, "exact_chapters_topics_test.pdf")
    doc.save(pdfFile)
    doc.close()

    val result = converter.convert(
      android.net.Uri.fromFile(pdfFile),
      com.serma.pubify.converter.ConversionOptions(
        title = "Exact Structure Book",
        author = "Tester",
        splitMode = com.serma.pubify.converter.ChapterSplitMode.AUTO_DETECT,
        extractCover = false
      )
    ) {}
    assertTrue("Conversion should succeed", result.isSuccess)
    val parsedBook = parser.parse(result.getOrNull()!!).getOrThrow()

    // Expect Front Matter (TOC page) + exact 3 chapters (or exact 3 chapters if TOC skipped) — never inflated to 7+!
    val realChapters = parsedBook.chapters.filter { it.title.startsWith("Chapter ") }
    assertEquals("Must extract exact 3 chapters (found: ${parsedBook.chapters.map { it.title }})", 3, realChapters.size)
    assertTrue("Total chapters should be <= 4 (found ${parsedBook.chapters.size}: ${parsedBook.chapters.map { it.title }})", parsedBook.chapters.size <= 4)

    val ch1 = realChapters[0]
    assertTrue("Chapter 1 should nest Topic 1.1", ch1.subHeadings.any { it.title.contains("1.1 Core Principles") && it.level == 2 })
    assertTrue("Chapter 1 should nest Sub-topic 1.1.1", ch1.subHeadings.any { it.title.contains("1.1.1 First Law") && it.level >= 3 })
    assertTrue("Chapter 1 should nest Topic 1.2 from page 3", ch1.subHeadings.any { it.title.contains("1.2 Design Patterns") })

    val ch2 = realChapters[1]
    assertTrue("Chapter 2 should nest Topic 2.1", ch2.subHeadings.any { it.title.contains("2.1 Architecture") })
    assertTrue("Chapter 2 should nest Sub-topic 2.1.1", ch2.subHeadings.any { it.title.contains("2.1.1 Layers") })
  }

  @Test
  fun `test chapter-scoped running headers on every page do not inflate chapter count`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val converter = com.serma.pubify.converter.PdfToEpubConverter(app)
    val parser = com.serma.pubify.reader.EpubParser()

    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
    val boldFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    val regularFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA

    // 12 pages: Pages 1-6 have Chapter 1 running header at top margin; Pages 7-12 have Chapter 2 running header at top margin
    for (pg in 1..12) {
      val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
      doc.addPage(page)
      val runningHeader = if (pg <= 6) "Chapter 1: Foundations" else "Chapter 2: Applications"
      com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
        // Small running header near very top (y=760 -> topY=32pt out of 792pt = 4%)
        cs.beginText()
        cs.setFont(regularFont, 9.5f)
        cs.newLineAtOffset(72f, 760f)
        cs.showText(runningHeader)
        cs.endText()

        // Actual chapter title ONLY on page 1 and page 7
        if (pg == 1 || pg == 7) {
          cs.beginText()
          cs.setFont(boldFont, 20f)
          cs.newLineAtOffset(72f, 690f)
          cs.showText(runningHeader)
          cs.endText()
        }

        // Body text on every page
        cs.beginText()
        cs.setFont(regularFont, 11f)
        cs.newLineAtOffset(72f, 640f)
        cs.showText("Body text on page $pg explaining core details clearly without splitting into fake chapters.")
        cs.newLineAtOffset(0f, -16f)
        cs.showText("Second line of body text on page $pg so 11pt stays the dominant body font size.")
        cs.endText()
      }
    }

    val pdfFile = java.io.File(app.cacheDir, "running_header_chapters_test.pdf")
    doc.save(pdfFile)
    doc.close()

    val result = converter.convert(
      android.net.Uri.fromFile(pdfFile),
      com.serma.pubify.converter.ConversionOptions(
        title = "Running Header Book",
        author = "Tester",
        splitMode = com.serma.pubify.converter.ChapterSplitMode.AUTO_DETECT,
        extractCover = false
      )
    ) {}
    assertTrue("Conversion should succeed", result.isSuccess)
    val parsedBook = parser.parse(result.getOrNull()!!).getOrThrow()

    assertEquals(
      "Must produce exact 2 chapters despite running headers on all 12 pages (found: ${parsedBook.chapters.map { it.title }})",
      2,
      parsedBook.chapters.size
    )
    assertEquals("Chapter 1: Foundations", parsedBook.chapters[0].title)
    assertEquals("Chapter 2: Applications", parsedBook.chapters[1].title)
  }

  @Test
  fun `test word line and passage highlights persist independently on same paragraph`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<PdfToEpubApp>()
    val repository = app.repository

    val wordHl = com.serma.pubify.data.model.BookHighlight(
      bookId = 505L,
      bookTitle = "Scope Test Book",
      chapterIndex = 0,
      chapterTitle = "Chapter 1",
      paragraphIndex = 2,
      text = "resilient",
      colorHex = 0xFFFFEB3B,
      scope = "WORD"
    )
    val lineHl = com.serma.pubify.data.model.BookHighlight(
      bookId = 505L,
      bookTitle = "Scope Test Book",
      chapterIndex = 0,
      chapterTitle = "Chapter 1",
      paragraphIndex = 2,
      text = "A resilient reader lets you highlight one line.",
      colorHex = 0xFF80DEEA,
      scope = "LINE"
    )
    val passageHl = com.serma.pubify.data.model.BookHighlight(
      bookId = 505L,
      bookTitle = "Scope Test Book",
      chapterIndex = 0,
      chapterTitle = "Chapter 1",
      paragraphIndex = 3,
      text = "Full passage text highlighted completely.",
      colorHex = 0xFFA5D6A7,
      scope = "PASSAGE"
    )

    val customHl = com.serma.pubify.data.model.BookHighlight(
      bookId = 505L,
      bookTitle = "Scope Test Book",
      chapterIndex = 0,
      chapterTitle = "Chapter 1",
      paragraphIndex = 2,
      text = "resilient reader lets you",
      colorHex = 0xFFFFCC80,
      scope = "CUSTOM"
    )

    repository.saveHighlight(wordHl)
    repository.saveHighlight(lineHl)
    repository.saveHighlight(passageHl)
    repository.saveHighlight(customHl)

    val saved = repository.getHighlightsForBook(505L).first()
    assertEquals(4, saved.size)
    assertTrue(saved.any { it.scope == "WORD" && it.text == "resilient" })
    assertTrue(saved.any { it.scope == "LINE" && it.text.contains("highlight one line") })
    assertTrue(saved.any { it.scope == "PASSAGE" && it.text.contains("Full passage") })
    assertTrue(saved.any { it.scope == "CUSTOM" && it.text == "resilient reader lets you" })
  }

  @Test
  fun `test epub device storage creates app named folder and saves epub inside it`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val expectedFolderName = app.getString(R.string.app_name)
    assertEquals("Pubify", expectedFolderName)
    assertEquals(expectedFolderName, com.serma.pubify.converter.EpubDeviceStorage.getAppFolderName(app))

    val folderReady = com.serma.pubify.converter.EpubDeviceStorage.ensureAppFolderExists(app)
    assertTrue(
      "Expected app-named folder creation to succeed",
      folderReady != null && folderReady.exists() && folderReady.name == expectedFolderName
    )

    val sourceFile = java.io.File(app.cacheDir, "sample_device_save.epub")
    sourceFile.writeBytes("PK\u0003\u0004dummy-epub-content".toByteArray())

    val saved = com.serma.pubify.converter.EpubDeviceStorage.saveEpubToDeviceDownloads(
      context = app,
      sourceEpubFile = sourceFile,
      bookTitle = "My Converted Book"
    )
    assertTrue("Expected EPUB to save to device storage: ${saved.errorMessage}", saved.savedSuccessfully)
    assertEquals(expectedFolderName, saved.folderName)
    assertTrue(
      "Display path should include app-named folder '$expectedFolderName' (got: ${saved.displayPath})",
      saved.displayPath?.contains(expectedFolderName) == true
    )
    assertTrue("Display path should end with .epub", saved.displayPath?.endsWith(".epub") == true)
  }

  @Test
  fun `test convertWithSummary preserves nested chapter topic and subtopic outline hierarchy`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val converter = com.serma.pubify.converter.PdfToEpubConverter(app)
    val parser = com.serma.pubify.reader.EpubParser()

    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
    val boldFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    val regularFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA

    fun addPage(title: String, body: String): com.tom_roush.pdfbox.pdmodel.PDPage {
      val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
      doc.addPage(page)
      com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
        cs.beginText()
        cs.setFont(boldFont, 18f)
        cs.newLineAtOffset(72f, 720f)
        cs.showText(title)
        cs.endText()

        cs.beginText()
        cs.setFont(regularFont, 11f)
        cs.newLineAtOffset(72f, 680f)
        cs.showText(body)
        cs.endText()
      }
      return page
    }

    val p1 = addPage("Chapter 1: Networking", "Overview of modern network design and layered protocols.")
    val p2 = addPage("Transport Layer Details", "Reliable streams and congestion control mechanisms are discussed here.")
    val p3 = addPage("Chapter 2: Storage", "Durable storage engines organize pages cleanly on disk.")

    // Outline with Chapter -> Topic -> Sub-topic (including topics whose exact title differs slightly from body headings)
    val outline = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline()
    doc.documentCatalog.documentOutline = outline

    val ch1 = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "Chapter 1: Networking"
      setDestination(p1)
    }
    val ch1Topic1 = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "1.1 Physical & Link Layers"
      setDestination(p1)
    }
    val ch1Topic2 = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "1.2 Transport Layer"
      setDestination(p2)
    }
    val ch1SubTopic = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "1.2.1 Congestion Control"
      setDestination(p2)
    }
    ch1Topic2.addLast(ch1SubTopic)
    ch1.addLast(ch1Topic1)
    ch1.addLast(ch1Topic2)

    val ch2 = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "Chapter 2: Storage"
      setDestination(p3)
    }
    val ch2Topic1 = com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem().apply {
      title = "2.1 B-Trees"
      setDestination(p3)
    }
    ch2.addLast(ch2Topic1)

    outline.addLast(ch1)
    outline.addLast(ch2)

    val pdfFile = java.io.File(app.cacheDir, "deep_outline_hierarchy_test.pdf")
    doc.save(pdfFile)
    doc.close()

    val summaryResult = converter.convertWithSummary(
      android.net.Uri.fromFile(pdfFile),
      com.serma.pubify.converter.ConversionOptions(
        title = "Systems Guide",
        author = "Tester",
        splitMode = com.serma.pubify.converter.ChapterSplitMode.AUTO_DETECT,
        extractCover = false
      )
    ) {}

    assertTrue("convertWithSummary should succeed: ${summaryResult.exceptionOrNull()?.message}", summaryResult.isSuccess)
    val output = summaryResult.getOrThrow()
    assertEquals("convertWithSummary should report exact 2 chapters", 2, output.chapterCount)

    val parsedBook = parser.parse(output.epubFile).getOrThrow()
    assertEquals("Parsed EPUB should contain exact 2 chapters", 2, parsedBook.chapters.size)

    val parsedCh1 = parsedBook.chapters[0]
    assertTrue(
      "Chapter 1 should include Topic '1.1 Physical & Link Layers' at level 2 (found: ${parsedCh1.subHeadings})",
      parsedCh1.subHeadings.any { it.title.contains("1.1 Physical") && it.level == 2 }
    )
    assertTrue(
      "Chapter 1 should include Topic '1.2 Transport Layer' at level 2 (found: ${parsedCh1.subHeadings})",
      parsedCh1.subHeadings.any { it.title.contains("1.2 Transport Layer") && it.level == 2 }
    )
    assertTrue(
      "Chapter 1 should include Sub-topic '1.2.1 Congestion Control' at level 3 (found: ${parsedCh1.subHeadings})",
      parsedCh1.subHeadings.any { it.title.contains("1.2.1 Congestion Control") && it.level == 3 }
    )

    val parsedCh2 = parsedBook.chapters[1]
    assertTrue(
      "Chapter 2 should include Topic '2.1 B-Trees' at level 2 (found: ${parsedCh2.subHeadings})",
      parsedCh2.subHeadings.any { it.title.contains("2.1 B-Trees") && it.level == 2 }
    )
  }

  @Test
  fun `test metadata extraction captures title and author from title page when pdf info is empty`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val converter = com.serma.pubify.converter.PdfToEpubConverter(app)
    val parser = com.serma.pubify.reader.EpubParser()

    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
    val boldFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    val regularFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA

    // Page 1: Title page with large prominent title & "By <Author>" line (NO PDDocumentInformation set!)
    val coverPage = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(coverPage)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, coverPage).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 28f)
      cs.newLineAtOffset(120f, 600f)
      cs.showText("Deep Work Mastery")
      cs.endText()

      cs.beginText()
      cs.setFont(regularFont, 15f)
      cs.newLineAtOffset(160f, 540f)
      cs.showText("By Cal Newport")
      cs.endText()
    }

    // Page 2: Body chapter
    val bodyPage = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(bodyPage)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, bodyPage).use { cs ->
      cs.beginText()
      cs.setFont(boldFont, 18f)
      cs.newLineAtOffset(72f, 700f)
      cs.showText("Chapter 1: Focus")
      cs.endText()

      cs.beginText()
      cs.setFont(regularFont, 11f)
      cs.newLineAtOffset(72f, 660f)
      cs.showText("Deep uninterrupted concentration produces rare and valuable work.")
      cs.endText()
    }

    val pdfFile = java.io.File(app.cacheDir, "scan_0042.pdf")
    doc.save(pdfFile)
    doc.close()

    val meta = converter.extractMetadata(android.net.Uri.fromFile(pdfFile)).getOrThrow()
    assertEquals("Deep Work Mastery", meta.title)
    assertEquals("Cal Newport", meta.author)

    // Also verify convertWithSummary with blank title/author resolves and embeds them in the EPUB
    val summary = converter.convertWithSummary(
      android.net.Uri.fromFile(pdfFile),
      com.serma.pubify.converter.ConversionOptions(
        title = "",
        author = "",
        extractCover = false
      )
    ) {}.getOrThrow()

    assertEquals("Deep Work Mastery", summary.resolvedTitle)
    assertEquals("Cal Newport", summary.resolvedAuthor)

    val parsed = parser.parse(summary.epubFile).getOrThrow()
    assertEquals("Deep Work Mastery", parsed.title)
    assertEquals("Cal Newport", parsed.author)
  }

  @Test
  fun `test metadata extraction ignores junk pdf info and parses structured filename`() {
    val parsed = com.serma.pubify.converter.PdfMetadataExtractor.parseFromFileName(
      "Atomic Habits - James Clear (z-lib.org).pdf"
    )
    assertEquals("Atomic Habits", parsed.first)
    assertEquals("James Clear", parsed.second)

    val parsedBy = com.serma.pubify.converter.PdfMetadataExtractor.parseFromFileName(
      "The_Pragmatic_Programmer_by_Andrew_Hunt.pdf"
    )
    assertEquals("The Pragmatic Programmer", parsedBy.first)
    assertEquals("Andrew Hunt", parsedBy.second)

    assertTrue(com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle("Microsoft Word - draft.docx"))
    assertTrue(com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle("Untitled"))
    assertTrue(com.serma.pubify.converter.PdfMetadataExtractor.isGenericAuthor("Admin"))
    assertTrue(com.serma.pubify.converter.PdfMetadataExtractor.isGenericAuthor("Unknown Author"))
  }

  @Test
  fun `test metadata extraction captures title containing Library and author above title on page 4 plus copyright without symbol`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val converter = com.serma.pubify.converter.PdfToEpubConverter(app)

    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
    val boldFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    val regularFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA

    // Pages 1..3: Blank/image-only cover, blank leaf, blank half-title (0 text)
    repeat(3) {
      doc.addPage(com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER))
    }

    // Page 4: Real title page with ALL-CAPS Author ABOVE ALL-CAPS Title containing "LIBRARY"
    val titlePage = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(titlePage)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, titlePage).use { cs ->
      cs.beginText()
      cs.setFont(regularFont, 14f)
      cs.newLineAtOffset(240f, 660f)
      cs.showText("MATT HAIG")
      cs.endText()

      cs.beginText()
      cs.setFont(boldFont, 28f)
      cs.newLineAtOffset(120f, 590f)
      cs.showText("THE MIDNIGHT LIBRARY")
      cs.endText()
    }

    // Page 5: Copyright page without © symbol
    val copyrightPage = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
    doc.addPage(copyrightPage)
    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, copyrightPage).use { cs ->
      cs.beginText()
      cs.setFont(regularFont, 10f)
      cs.newLineAtOffset(72f, 500f)
      cs.showText("Copyright 2020, Matt Haig")
      cs.endText()
      cs.beginText()
      cs.setFont(regularFont, 10f)
      cs.newLineAtOffset(72f, 480f)
      cs.showText("All rights reserved. ISBN 978-0-525-55947-4")
      cs.endText()
    }

    val pdfFile = java.io.File(app.cacheDir, "msf_100042.pdf")
    doc.save(pdfFile)
    doc.close()

    val meta = converter.extractMetadata(android.net.Uri.fromFile(pdfFile)).getOrThrow()
    assertEquals("The Midnight Library", meta.title)
    assertEquals("Matt Haig", meta.author)
  }

  @Test
  fun `test metadata extraction recovers title and author from running headers when cover has no text`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val converter = com.serma.pubify.converter.PdfToEpubConverter(app)

    val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
    val regularFont = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA

    // Page 1: Raster cover (0 text)
    doc.addPage(com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER))

    // Pages 2..7: Body pages with classic even/odd running headers (Even = Author, Odd = Book Title)
    for (p in 2..7) {
      val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.LETTER)
      doc.addPage(page)
      com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
        cs.beginText()
        cs.setFont(regularFont, 9f)
        cs.newLineAtOffset(220f, 755f)
        cs.showText(if (p % 2 == 0) "JAMES CLEAR" else "ATOMIC HABITS")
        cs.endText()

        // Dense body prose so page isn't treated as a sparse title page
        var y = 690f
        repeat(24) { idx ->
          cs.beginText()
          cs.setFont(regularFont, 11f)
          cs.newLineAtOffset(72f, y)
          cs.showText("Small habits make a big difference over time when practiced consistently on day ${p * 10 + idx}.")
          cs.endText()
          y -= 20f
        }
      }
    }

    val pdfFile = java.io.File(app.cacheDir, "document.pdf")
    doc.save(pdfFile)
    doc.close()

    val meta = converter.extractMetadata(android.net.Uri.fromFile(pdfFile)).getOrThrow()
    assertEquals("Atomic Habits", meta.title)
    assertEquals("James Clear", meta.author)
  }

  @Test
  fun `test metadata extraction handles SAF URI prefixes and encoded filenames`() {
    val parsedSaf = com.serma.pubify.converter.PdfMetadataExtractor.parseFromFileName(
      "primary%3ADownload%2FAtomic%20Habits%20-%20James%20Clear.pdf"
    )
    assertEquals("Atomic Habits", parsedSaf.first)
    assertEquals("James Clear", parsedSaf.second)

    assertTrue(com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle("msf:100042"))
    assertTrue(com.serma.pubify.converter.PdfMetadataExtractor.isGenericTitle("1000012345"))
    assertEquals(
      "Dr. Jekyll and Mr. Hyde",
      com.serma.pubify.converter.PdfMetadataExtractor.cleanRawFileName("Dr. Jekyll and Mr. Hyde.pdf")
    )
  }
}
