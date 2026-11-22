package com.serma.pubify

import com.serma.pubify.converter.ConversionOptions
import com.serma.pubify.converter.EpubFontFamily
import com.serma.pubify.converter.EpubMargin
import com.serma.pubify.converter.ImageCompressionLevel
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun conversionOptions_defaultSettings_areAccurate() {
    val options = ConversionOptions()
    assertEquals(EpubFontFamily.SANS_SERIF, options.fontFamily)
    assertEquals(16, options.fontSizePt)
    assertEquals(EpubMargin.MATCH_PDF, options.margin)
    assertEquals(ImageCompressionLevel.BALANCED, options.imageCompression)
    assertNull(options.customMarginPercent)
    assertNull(options.customImageQuality)
    assertFalse(options.isCustomized)
    assertEquals(6, options.effectiveHorizontalMargin)
    assertEquals(5, options.effectiveVerticalMargin)
    assertEquals(80, options.effectiveImageQuality)
  }

  @Test
  fun conversionOptions_customSettings_affectEffectiveValues() {
    val options = ConversionOptions(
      fontFamily = EpubFontFamily.SERIF,
      fontSizePt = 18,
      margin = EpubMargin.COMPACT,
      customMarginPercent = 7,
      imageCompression = ImageCompressionLevel.LOW,
      customImageQuality = 95
    )
    assertTrue(options.isCustomized)
    assertEquals(7, options.effectiveHorizontalMargin)
    assertEquals(6, options.effectiveVerticalMargin)
    assertEquals(95, options.effectiveImageQuality)
  }

  @Test
  fun imageCompressionLevels_haveValidParameters() {
    ImageCompressionLevel.values().forEach { level ->
      assertTrue(level.qualityPercent in 20..100)
      assertTrue(level.maxDimension >= 400)
      assertTrue(level.displayName.isNotBlank())
    }
  }

  @Test
  fun epubFontFamilies_haveValidCss() {
    EpubFontFamily.values().forEach { family ->
      assertTrue(family.fontCss.contains("font-family"))
      assertTrue(family.displayName.isNotBlank())
    }
  }

  @Test
  fun resolveChapterOutlineLevel_promotesNestedChaptersUnderBookRootOrParts() {
    val flat = listOf(
      com.serma.pubify.converter.ExtractedOutlineItem("Chapter 1: Intro", 1, 0),
      com.serma.pubify.converter.ExtractedOutlineItem("Chapter 2: Core", 5, 0)
    )
    assertEquals(0, com.serma.pubify.converter.PdfStructureExtractor.resolveChapterOutlineLevel(flat))

    val singleRoot = listOf(
      com.serma.pubify.converter.ExtractedOutlineItem("Complete Book Title", 1, 0),
      com.serma.pubify.converter.ExtractedOutlineItem("Chapter 1: Intro", 2, 1),
      com.serma.pubify.converter.ExtractedOutlineItem("1.1 Setup", 3, 2),
      com.serma.pubify.converter.ExtractedOutlineItem("Chapter 2: Core", 8, 1)
    )
    assertEquals(1, com.serma.pubify.converter.PdfStructureExtractor.resolveChapterOutlineLevel(singleRoot))

    val partsWrappingChapters = listOf(
      com.serma.pubify.converter.ExtractedOutlineItem("Beginnings", 1, 0),
      com.serma.pubify.converter.ExtractedOutlineItem("Chapter 1: Dawn", 1, 1),
      com.serma.pubify.converter.ExtractedOutlineItem("Chapter 2: Noon", 5, 1),
      com.serma.pubify.converter.ExtractedOutlineItem("Endings", 10, 0),
      com.serma.pubify.converter.ExtractedOutlineItem("Chapter 3: Dusk", 10, 1)
    )
    assertEquals(1, com.serma.pubify.converter.PdfStructureExtractor.resolveChapterOutlineLevel(partsWrappingChapters))
  }

  @Test
  fun readerSettings_defaultToMoonReaderPaginatedMode() {
    val defaults = com.serma.pubify.reader.ReaderSettings()
    assertEquals(com.serma.pubify.reader.ReaderReadingMode.PAGINATED, defaults.readingMode)
    assertEquals(1.0f, defaults.brightnessLevel, 0.001f)
    assertEquals("Paper", com.serma.pubify.reader.ReaderTheme.WARM_AMBER.displayName)
    assertEquals("Dusk", com.serma.pubify.reader.ReaderTheme.DUSK_SLATE.displayName)
  }

  @Test
  fun normalizeOutlineHierarchy_rescuesFlatOutlineWithChaptersTopicsAndSubTopics() {
    val flatOutlines = mutableListOf<com.serma.pubify.converter.ExtractedOutlineItem>()
    var page = 1
    for (ch in 1..4) {
      flatOutlines.add(com.serma.pubify.converter.ExtractedOutlineItem("Chapter $ch: Core Unit $ch", page, 0))
      for (topic in 1..5) {
        page += 2
        flatOutlines.add(com.serma.pubify.converter.ExtractedOutlineItem("$ch.$topic Topic Alpha $topic", page, 0))
        for (sub in 1..4) {
          page += 1
          flatOutlines.add(com.serma.pubify.converter.ExtractedOutlineItem("$ch.$topic.$sub Sub-Topic Detail $sub", page, 0))
        }
      }
      page += 2
    }
    // Total flat items = 4 + 20 + 80 = 104 items all at level 0
    assertEquals(104, flatOutlines.size)

    val normalized = com.serma.pubify.converter.PdfStructureExtractor.normalizeOutlineHierarchy(flatOutlines)
    val chapters = normalized.filter { it.level == 0 }
    val topics = normalized.filter { it.level == 1 }
    val subTopics = normalized.filter { it.level == 2 }

    assertEquals("Exact 4 chapters must be extracted from flat 104-item outline", 4, chapters.size)
    assertEquals("All 20 topics should be classified at level 1", 20, topics.size)
    assertEquals("All 80 sub-topics should be classified at level 2", 80, subTopics.size)
  }

  @Test
  fun normalizeOutlineHierarchy_stripsPerPageBookmarksAndRescues208EntryFlatOutline() {
    // 1. Per-page bookmarks ("Page 1" .. "Page 208") must never become 208 chapters
    val perPageBookmarks = (1..208).map { pg ->
      com.serma.pubify.converter.ExtractedOutlineItem("Page $pg", pg, 0)
    }
    val stripped = com.serma.pubify.converter.PdfStructureExtractor.normalizeOutlineHierarchy(perPageBookmarks, totalPdfPages = 208)
    assertTrue("Per-page bookmarks should be discarded instead of creating 208 chapters", stripped.isEmpty())

    // 2. Flat 208-entry outline with 10 real chapters + 198 unnumbered topic titles all at level 0
    val mixedFlat = mutableListOf<com.serma.pubify.converter.ExtractedOutlineItem>()
    var pg = 1
    for (ch in 1..10) {
      mixedFlat.add(com.serma.pubify.converter.ExtractedOutlineItem("Chapter $ch: Theme $ch", pg, 0))
      for (t in 1..19) {
        pg++
        mixedFlat.add(com.serma.pubify.converter.ExtractedOutlineItem("Unnumbered Topic $t of $ch", pg, 0))
      }
      pg++
    }
    val rescued = com.serma.pubify.converter.PdfStructureExtractor.normalizeOutlineHierarchy(mixedFlat, totalPdfPages = 208)
    assertEquals("Exact 10 chapters must remain at level 0", 10, rescued.count { it.level == 0 })
    assertEquals("All 190 unnumbered topics must be demoted to level 1", 190, rescued.count { it.level == 1 })
  }
}
