package com.serma.pubify.reader

import androidx.compose.ui.graphics.Color

enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode"),
    LIQUID_GLASS("Liquid Glass")
}

enum class ReaderTheme(
    val displayName: String,
    val backgroundColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val isDark: Boolean = false,
    val isGlass: Boolean = false
) {
    LIGHT(
        displayName = "Light",
        backgroundColor = Color(0xFFFFFFFF),
        textColor = Color(0xFF1E2022),
        accentColor = Color(0xFF1976D2),
        isDark = false
    ),
    SEPIA(
        displayName = "Sepia",
        backgroundColor = Color(0xFFF9F3E5),
        textColor = Color(0xFF382B1D),
        accentColor = Color(0xFF8D6E63),
        isDark = false
    ),
    MINT(
        displayName = "Mint",
        backgroundColor = Color(0xFFE8F5E9),
        textColor = Color(0xFF1B5E20),
        accentColor = Color(0xFF2E7D32),
        isDark = false
    ),
    WARM_AMBER(
        displayName = "Paper",
        backgroundColor = Color(0xFFF3E9D2),
        textColor = Color(0xFF2B2118),
        accentColor = Color(0xFF9E5A2B),
        isDark = false
    ),
    DUSK_SLATE(
        displayName = "Dusk",
        backgroundColor = Color(0xFF222733),
        textColor = Color(0xFFD5DCE8),
        accentColor = Color(0xFF7CB7FF),
        isDark = true
    ),
    DARK(
        displayName = "Charcoal",
        backgroundColor = Color(0xFF1E212B),
        textColor = Color(0xFFE2E4EE),
        accentColor = Color(0xFF80CBC4),
        isDark = true
    ),
    OLED_BLACK(
        displayName = "OLED",
        backgroundColor = Color(0xFF000000),
        textColor = Color(0xFFCCCCCC),
        accentColor = Color(0xFF64B5F6),
        isDark = true
    ),
    LIQUID_GLASS(
        displayName = "Liquid Glass",
        backgroundColor = Color(0xFF0A101D),
        textColor = Color(0xFFE6F0FA),
        accentColor = Color(0xFF00E5FF),
        isDark = true,
        isGlass = true
    )
}

enum class ReaderReadingMode(
    val displayName: String,
    val description: String
) {
    PAGINATED(
        displayName = "Paged Flip",
        description = "Moon+ book-like horizontal page turns synced to screen height"
    ),
    SCROLL(
        displayName = "Continuous Scroll",
        description = "Smooth vertical scrolling through each chapter"
    )
}

enum class PageTurnMode(
    val displayName: String,
    val description: String
) {
    TAP_EDGES(
        displayName = "Tap Edges",
        description = "Tap left side for previous, right side for next page"
    ),
    VOLUME_KEYS(
        displayName = "Volume Keys",
        description = "Press hardware Volume Up/Down buttons to turn pages"
    ),
    TAP_AND_VOLUME(
        displayName = "Tap & Volume",
        description = "Turn pages with both screen edge taps and volume keys"
    ),
    SCROLL_ONLY(
        displayName = "Scroll Only",
        description = "Standard smooth touch scrolling; taps toggle reader menu"
    )
}

enum class ReaderFontFamily(
    val displayName: String,
    val description: String = "",
    val category: String = "Serif"
) {
    LITERATA("Literata", "Google Play Books e-reader serif", "Book Serif"),
    EB_GARAMOND("EB Garamond", "Classic Renaissance book typography", "Classic"),
    LORA("Lora", "Contemporary editorial reading serif", "Editorial"),
    INTER("Inter", "Ultra-clean modern sans-serif", "Modern Sans"),
    SERIF("Serif", "Default system serif typeface", "System"),
    SANS_SERIF("Sans-Serif", "Clean neutral sans-serif", "System"),
    MONOSPACE("Monospace", "Fixed-width typewriter font", "Clean Mono")
}

enum class ReaderTextAlign(
    val displayName: String,
    val description: String = ""
) {
    LEFT("Left", "Ragged right margin, natural eye flow"),
    JUSTIFY("Justify", "Even margins on both sides, traditional book format"),
    CENTER("Center", "Centered text for poetry & dramatic reading"),
    RIGHT("Right", "Aligned to right margin")
}

data class ReaderSettings(
    val fontSizeSp: Float = 18f,
    val lineSpacingMultiplier: Float = 1.65f,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.LITERATA,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val textAlign: ReaderTextAlign = ReaderTextAlign.JUSTIFY,
    val pageTurnMode: PageTurnMode = PageTurnMode.TAP_AND_VOLUME,
    val readingMode: ReaderReadingMode = ReaderReadingMode.PAGINATED,
    val brightnessLevel: Float = 1.0f,
    val showTapZonesOverlay: Boolean = false
)

data class EpubMargins(
    val leftPercent: Int = 5,
    val rightPercent: Int = 5,
    val topPercent: Int = 4,
    val bottomPercent: Int = 4
)

data class ReaderSubHeading(
    val title: String,
    val level: Int,
    val paragraphIndex: Int
)

data class ReaderChapter(
    val id: String,
    val index: Int,
    val title: String,
    val paragraphs: List<String>,
    val subHeadings: List<ReaderSubHeading> = emptyList(),
    val wordCount: Int = paragraphs
        .filterNot { it.trimStart().startsWith("[[IMG:") }
        .sumOf { p ->
            p.removePrefix("[[CENTER]]")
                .replace(Regex("^#{1,4}\\s+"), "")
                .split(Regex("\\s+"))
                .count { it.isNotBlank() }
        }
)

data class ParsedEpubBook(
    val title: String,
    val author: String,
    val chapters: List<ReaderChapter>,
    val coverBytes: ByteArray? = null,
    val bookImages: Map<String, ByteArray> = emptyMap(),
    val epubMargins: EpubMargins = EpubMargins()
) {
    val totalWords: Int
        get() = chapters.sumOf { it.wordCount }

    val estimatedReadTimeMinutes: Int
        get() = (totalWords / 200).coerceAtLeast(1)
}
