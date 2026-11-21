package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookHighlight
import com.example.reader.GeminiReaderHelper
import com.example.reader.ReaderTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import java.net.URLEncoder

enum class HighlightScope(val code: String, val label: String) {
    WORD("WORD", "Word"),
    LINE("LINE", "Line"),
    PASSAGE("PASSAGE", "Passage"),
    CUSTOM("CUSTOM", "Custom");

    companion object {
        fun fromCode(code: String?): HighlightScope {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: WORD
        }
    }
}

private data class ParagraphWordSpan(
    val text: String,
    val startChar: Int,
    val endChar: Int
)

private fun tokenizeParagraphWordSpans(text: String): List<ParagraphWordSpan> {
    if (text.isBlank()) return emptyList()
    return Regex("\\S+").findAll(text).map { m ->
        ParagraphWordSpan(
            text = m.value,
            startChar = m.range.first,
            endChar = m.range.last + 1
        )
    }.toList()
}

private fun findInitialCustomWordRange(
    cleanParagraph: String,
    wordSpans: List<ParagraphWordSpan>,
    targetWord: String,
    existingHighlight: BookHighlight?
): Pair<Int, Int> {
    if (wordSpans.isEmpty()) return 0 to 0
    val lastIdx = wordSpans.lastIndex

    // If editing an existing CUSTOM highlight, restore its exact start..end word span
    if (existingHighlight != null && existingHighlight.scope.equals("CUSTOM", ignoreCase = true)) {
        val snippet = existingHighlight.text.trim()
        val matchStart = if (snippet.isNotBlank()) cleanParagraph.indexOf(snippet, ignoreCase = true) else -1
        if (matchStart >= 0) {
            val matchEnd = (matchStart + snippet.length).coerceAtMost(cleanParagraph.length)
            val startWord = wordSpans.indexOfFirst { it.endChar > matchStart }.coerceIn(0, lastIdx)
            val endWord = wordSpans.indexOfLast { it.startChar < matchEnd }.coerceIn(startWord, lastIdx)
            return startWord to endWord
        }
    }

    // Default to the long-pressed word so the user can smoothly expand left/right to any portion
    val cleanTarget = targetWord.trim()
    val matchedIdx = if (cleanTarget.isNotBlank()) {
        wordSpans.indexOfFirst { span ->
            val stripped = span.text.trim('.', ',', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']', '—', '-', '…')
            stripped.equals(cleanTarget, ignoreCase = true)
        }.takeIf { it >= 0 }
            ?: wordSpans.indexOfFirst { it.text.contains(cleanTarget, ignoreCase = true) }.coerceAtLeast(0)
    } else 0
    val clamped = matchedIdx.coerceIn(0, lastIdx)
    return clamped to clamped
}

data class WordInspectState(
    val word: String,
    val line: String,
    val paragraph: String,
    val paragraphIndex: Int,
    val chapterIndex: Int,
    val chapterTitle: String,
    val existingHighlight: BookHighlight? = null
)

val HIGHLIGHT_COLOR_PRESETS = listOf(
    HighlightColorOption("Sunshine Yellow", 0xFFFFEB3B, "Warm yellow"),
    HighlightColorOption("Cool Aqua", 0xFF80DEEA, "Fresh aqua"),
    HighlightColorOption("Mint Emerald", 0xFFA5D6A7, "Serene mint green"),
    HighlightColorOption("Soft Coral", 0xFFFFAB91, "Warm coral peach"),
    HighlightColorOption("Lavender", 0xFFCE93D8, "Gentle soft purple")
)

data class HighlightColorOption(
    val name: String,
    val colorHex: Long,
    val description: String
)

private enum class ActiveSubDialog {
    NONE,
    NOTE,
    GEMINI
}

/**
 * Clean, uncluttered reading action dropdown menu for long-pressed words and passages.
 * Contains 5 focused options:
 * 1. Copy
 * 2. Highlight (with Word / Line / Passage scope + 5 color swatches)
 * 3. Note
 * 4. Dictionary (opens Google Search directly for definition)
 * 5. Ask Gemini
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingWordInspectSheet(
    target: WordInspectState,
    theme: ReaderTheme,
    onDismiss: () -> Unit,
    onHighlight: (colorHex: Long, scope: String, targetText: String) -> Unit,
    onRemoveHighlight: (highlightId: Long) -> Unit,
    onSaveNote: ((note: String) -> Unit)? = null,
    onSearchInBook: ((query: String) -> Unit)? = null,
    onReadAloud: ((paragraphIndex: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedScope by remember(target) {
        mutableStateOf(
            if (target.existingHighlight != null) {
                HighlightScope.fromCode(target.existingHighlight.scope)
            } else {
                HighlightScope.WORD
            }
        )
    }
    var activeDialog by remember { mutableStateOf(ActiveSubDialog.NONE) }
    val isHighlighted = target.existingHighlight != null

    val cleanParagraph = remember(target.paragraph) {
        target.paragraph
            .removePrefix("[[CENTER]]")
            .replace(Regex("^#{1,4}\\s+"), "")
            .removePrefix("❝ ")
            .removePrefix("• ")
            .trim()
    }
    val effectiveLine = remember(target.line, cleanParagraph) {
        target.line.trim().ifBlank { cleanParagraph }
    }
    val wordSpans = remember(cleanParagraph) {
        tokenizeParagraphWordSpans(cleanParagraph)
    }
    val initialCustomRange = remember(cleanParagraph, target.word, target.existingHighlight) {
        findInitialCustomWordRange(cleanParagraph, wordSpans, target.word, target.existingHighlight)
    }
    var customStartIdx by remember(target) { mutableIntStateOf(initialCustomRange.first) }
    var customEndIdx by remember(target) { mutableIntStateOf(initialCustomRange.second) }

    val customSelectedText = remember(cleanParagraph, wordSpans, customStartIdx, customEndIdx) {
        if (wordSpans.isEmpty()) {
            cleanParagraph
        } else {
            val sIdx = customStartIdx.coerceIn(0, wordSpans.lastIndex)
            val eIdx = customEndIdx.coerceIn(sIdx, wordSpans.lastIndex)
            cleanParagraph.substring(wordSpans[sIdx].startChar, wordSpans[eIdx].endChar).trim()
        }
    }

    val selectedTextForScope = when (selectedScope) {
        HighlightScope.WORD -> target.word.trim().ifBlank { effectiveLine }
        HighlightScope.LINE -> effectiveLine
        HighlightScope.PASSAGE -> cleanParagraph
        HighlightScope.CUSTOM -> customSelectedText.ifBlank { target.word.trim().ifBlank { cleanParagraph } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (theme.isGlass) Color(0xF80B1528) else if (theme.isDark) Color(0xFF1E212B) else Color(0xFFFAFAFA),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .testTag("reading_clean_dropdown_menu")
        ) {
            // Header: Selected Word / Snippet + Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = theme.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "“${target.word}”",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = theme.accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (isHighlighted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(target.existingHighlight!!.colorHex).copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, Color(target.existingHighlight.colorHex))
                        ) {
                            Text(
                                text = HighlightScope.fromCode(target.existingHighlight.scope).label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = theme.textColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("clean_dropdown_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = theme.textColor.copy(alpha = 0.7f)
                    )
                }
            }

            HorizontalDivider(
                color = theme.textColor.copy(alpha = 0.1f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // THE 5 CLEAN DROPDOWN OPTIONS: Copy, Highlight, Note, Dictionary (Google Search), Ask Gemini
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Copy Option
                DropdownOptionRow(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy",
                    subtitle = "Copy selected word to clipboard",
                    theme = theme,
                    testTag = "long_press_action_copy",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Copied Word", target.word))
                        Toast.makeText(context, "'${target.word}' copied", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                // 2. Highlight Option (Word / Line / Passage / Custom + Color Swatches)
                DropdownOptionRow(
                    icon = Icons.Default.Highlight,
                    title = if (isHighlighted) "Highlight (Edit)" else "Highlight",
                    subtitle = "Choose Word, Line, Passage, or Custom portion",
                    theme = theme,
                    testTag = "long_press_action_highlight",
                    trailingBadge = if (isHighlighted) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(target.existingHighlight!!.colorHex))
                            )
                        }
                    } else null,
                    onClick = {
                        showColorPicker = !showColorPicker
                    }
                )

                // Inline Scope Picker (Word / Line / Passage) + Color Swatches
                AnimatedVisibility(
                    visible = showColorPicker,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (theme.isDark) Color(0x28FFFFFF) else Color(0x0D000000)
                        ),
                        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Highlight Scope",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = theme.textColor
                                )

                                if (isHighlighted) {
                                    TextButton(
                                        onClick = {
                                            target.existingHighlight?.let { onRemoveHighlight(it.id) }
                                            onDismiss()
                                            Toast.makeText(context, "Highlight removed", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.testTag("highlight_remove_btn")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            // 4-way Scope Selector: Word | Line | Passage | Custom
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HighlightScope.entries.forEach { scope ->
                                    val isSelected = selectedScope == scope
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedScope = scope },
                                        label = {
                                            Text(
                                                text = scope.label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = theme.accentColor.copy(alpha = 0.22f),
                                            selectedLabelColor = theme.accentColor,
                                            labelColor = theme.textColor.copy(alpha = 0.8f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("highlight_scope_${scope.code.lowercase()}")
                                    )
                                }
                            }

                            if (selectedScope == HighlightScope.CUSTOM && wordSpans.isNotEmpty()) {
                                val lastWordIdx = wordSpans.lastIndex
                                val safeStart = customStartIdx.coerceIn(0, lastWordIdx)
                                val safeEnd = customEndIdx.coerceIn(safeStart, lastWordIdx)
                                val previewHlBg = Color(target.existingHighlight?.colorHex ?: 0xFFFFEB3B)
                                    .copy(alpha = if (theme.isDark) 0.50f else 0.62f)
                                val previewHlFg = if (theme.isDark) Color.White else Color.Black
                                var previewLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_highlight_picker"),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tap words or drag handles to select portion",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = theme.textColor.copy(alpha = 0.72f)
                                        )
                                        Text(
                                            text = "${safeEnd - safeStart + 1} word${if (safeEnd == safeStart) "" else "s"}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = theme.accentColor
                                        )
                                    }

                                    // Interactive live passage preview: tap any word to expand/adjust selection
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = theme.textColor.copy(alpha = 0.06f),
                                        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.28f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 124.dp)
                                    ) {
                                        val annotatedPreview = remember(cleanParagraph, wordSpans, safeStart, safeEnd, previewHlBg, previewHlFg) {
                                            buildAnnotatedString {
                                                append(cleanParagraph)
                                                val charStart = wordSpans[safeStart].startChar.coerceIn(0, cleanParagraph.length)
                                                val charEnd = wordSpans[safeEnd].endChar.coerceIn(charStart, cleanParagraph.length)
                                                addStyle(
                                                    SpanStyle(
                                                        background = previewHlBg,
                                                        color = previewHlFg,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    start = charStart,
                                                    end = charEnd
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .verticalScroll(rememberScrollState())
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = annotatedPreview,
                                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                                color = theme.textColor.copy(alpha = 0.85f),
                                                onTextLayout = { previewLayout = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .pointerInput(wordSpans, safeStart, safeEnd) {
                                                        detectTapGestures { tapOffset ->
                                                            val charPos = previewLayout?.getOffsetForPosition(tapOffset) ?: return@detectTapGestures
                                                            val tappedWordIdx = wordSpans.indexOfFirst { charPos in it.startChar..it.endChar }
                                                                .takeIf { it >= 0 }
                                                                ?: wordSpans.indices.minByOrNull { idx ->
                                                                    abs(((wordSpans[idx].startChar + wordSpans[idx].endChar) / 2) - charPos)
                                                                } ?: return@detectTapGestures
                                                            when {
                                                                tappedWordIdx < safeStart -> customStartIdx = tappedWordIdx
                                                                tappedWordIdx > safeEnd -> customEndIdx = tappedWordIdx
                                                                else -> {
                                                                    val distToStart = tappedWordIdx - safeStart
                                                                    val distToEnd = safeEnd - tappedWordIdx
                                                                    if (distToStart <= distToEnd) {
                                                                        customStartIdx = tappedWordIdx
                                                                    } else {
                                                                        customEndIdx = tappedWordIdx
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                            )
                                        }
                                    }

                                    // Smooth RangeSlider for Start & End word boundaries
                                    if (lastWordIdx > 0) {
                                        RangeSlider(
                                            value = safeStart.toFloat()..safeEnd.toFloat(),
                                            onValueChange = { range ->
                                                val newStart = range.start.roundToInt().coerceIn(0, lastWordIdx)
                                                val newEnd = range.endInclusive.roundToInt().coerceIn(newStart, lastWordIdx)
                                                customStartIdx = newStart
                                                customEndIdx = newEnd
                                            },
                                            valueRange = 0f..lastWordIdx.toFloat(),
                                            colors = SliderDefaults.colors(
                                                thumbColor = theme.accentColor,
                                                activeTrackColor = theme.accentColor,
                                                inactiveTrackColor = theme.textColor.copy(alpha = 0.16f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("custom_highlight_range_slider")
                                        )
                                    }

                                    // Precision 1-word Start & End nudge controls
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "Start",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = theme.textColor.copy(alpha = 0.78f)
                                            )
                                            IconButton(
                                                onClick = { customStartIdx = (safeStart - 1).coerceAtLeast(0) },
                                                enabled = safeStart > 0,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("custom_start_prev_btn")
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Expand start earlier", modifier = Modifier.size(16.dp), tint = theme.accentColor)
                                            }
                                            IconButton(
                                                onClick = { customStartIdx = (safeStart + 1).coerceAtMost(safeEnd) },
                                                enabled = safeStart < safeEnd,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("custom_start_next_btn")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Trim start later", modifier = Modifier.size(16.dp), tint = theme.accentColor)
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "End",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = theme.textColor.copy(alpha = 0.78f)
                                            )
                                            IconButton(
                                                onClick = { customEndIdx = (safeEnd - 1).coerceAtLeast(safeStart) },
                                                enabled = safeEnd > safeStart,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("custom_end_prev_btn")
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Trim end earlier", modifier = Modifier.size(16.dp), tint = theme.accentColor)
                                            }
                                            IconButton(
                                                onClick = { customEndIdx = (safeEnd + 1).coerceAtMost(lastWordIdx) },
                                                enabled = safeEnd < lastWordIdx,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("custom_end_next_btn")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Expand end later", modifier = Modifier.size(16.dp), tint = theme.accentColor)
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Live preview of exact snippet that will be highlighted (Word / Line / Passage)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = theme.textColor.copy(alpha = 0.06f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "“$selectedTextForScope”",
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        color = theme.textColor.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Select Color",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = theme.textColor.copy(alpha = 0.85f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                HIGHLIGHT_COLOR_PRESETS.forEach { preset ->
                                    val isCurrent = isHighlighted &&
                                        target.existingHighlight?.colorHex == preset.colorHex &&
                                        HighlightScope.fromCode(target.existingHighlight.scope) == selectedScope
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                onHighlight(preset.colorHex, selectedScope.code, selectedTextForScope)
                                                onDismiss()
                                                Toast.makeText(
                                                    context,
                                                    "Highlighted ${selectedScope.label.lowercase()} in ${preset.name}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .testTag("swatch_${preset.name.lowercase().replace(" ", "_")}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(preset.colorHex))
                                                .border(
                                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                                    color = if (isCurrent) theme.accentColor else Color.Black.copy(alpha = 0.2f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = preset.name.substringBefore(" "),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = theme.textColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Note Option
                DropdownOptionRow(
                    icon = Icons.Default.EditNote,
                    title = "Note",
                    subtitle = if (!target.existingHighlight?.note.isNullOrBlank()) "View or edit attached note" else "Add personal thought or bookmark",
                    theme = theme,
                    testTag = "long_press_action_note",
                    trailingBadge = if (!target.existingHighlight?.note.isNullOrBlank()) {
                        {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = theme.accentColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "1 note",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = theme.accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    } else null,
                    onClick = {
                        activeDialog = ActiveSubDialog.NOTE
                    }
                )

                // 4. Dictionary Option -> Opens Google Search directly for word definition
                DropdownOptionRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Dictionary",
                    subtitle = "Look up meaning on Google Search",
                    theme = theme,
                    testTag = "long_press_action_dictionary",
                    onClick = {
                        openGoogleDictionarySearch(context, target.word)
                        onDismiss()
                    }
                )

                // 5. Ask Gemini Option
                DropdownOptionRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Ask Gemini",
                    subtitle = "Context explanation & literary analysis",
                    theme = theme,
                    testTag = "long_press_action_gemini",
                    accentTint = true,
                    onClick = {
                        activeDialog = ActiveSubDialog.GEMINI
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // SUB-DIALOG 1: Note Dialog
    if (activeDialog == ActiveSubDialog.NOTE) {
        var noteInput by remember { mutableStateOf(target.existingHighlight?.note ?: "") }
        AlertDialog(
            onDismissRequest = { activeDialog = ActiveSubDialog.NONE },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = theme.accentColor)
                    Text("Note on “${target.word}”", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "From ${target.chapterTitle}: “${cleanParagraph.take(120)}...”",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("Write your thoughts or annotation here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("note_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveNote?.invoke(noteInput.trim())
                        Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        activeDialog = ActiveSubDialog.NONE
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                    modifier = Modifier.testTag("save_note_confirm_btn")
                ) {
                    Text("Save Note", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDialog = ActiveSubDialog.NONE }) {
                    Text("Cancel")
                }
            }
        )
    }

    // SUB-DIALOG 2: Ask Gemini AI Dialog
    if (activeDialog == ActiveSubDialog.GEMINI) {
        var userQuestion by remember { mutableStateOf("") }
        var isGeminiLoading by remember { mutableStateOf(false) }
        var geminiAnswer by remember { mutableStateOf<String?>(null) }

        fun queryGemini(prompt: String) {
            isGeminiLoading = true
            geminiAnswer = null
            coroutineScope.launch {
                val result = GeminiReaderHelper.askGemini(
                    word = target.word,
                    passage = cleanParagraph,
                    chapterTitle = target.chapterTitle,
                    userPrompt = prompt
                )
                isGeminiLoading = false
                geminiAnswer = result.getOrElse { "Could not generate explanation. Please try again." }
            }
        }

        AlertDialog(
            onDismissRequest = { activeDialog = ActiveSubDialog.NONE },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = theme.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text("Ask Gemini", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Word: “${target.word}”",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.accentColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = theme.accentColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    queryGemini("Explain '${target.word}' in the context of this passage.")
                                }
                        ) {
                            Text(
                                text = "Explain Context",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = theme.accentColor,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = theme.accentColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    queryGemini("Analyze the literary theme, mood, and symbolism connected with '${target.word}'.")
                                }
                        ) {
                            Text(
                                text = "Literary Theme",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = theme.accentColor,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = userQuestion,
                            onValueChange = { userQuestion = it },
                            placeholder = { Text("Ask anything about this word...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (userQuestion.isNotBlank()) {
                                    queryGemini(userQuestion.trim())
                                }
                            },
                            enabled = userQuestion.isNotBlank() && !isGeminiLoading
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = theme.accentColor)
                        }
                    }

                    if (isGeminiLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = theme.accentColor)
                            Text("Gemini is thinking...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (geminiAnswer != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = theme.accentColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = geminiAnswer!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Gemini Insight", geminiAnswer))
                                            Toast.makeText(context, "Gemini insight copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDialog = ActiveSubDialog.NONE }) {
                    Text("Done")
                }
            }
        )
    }
}

private fun openGoogleDictionarySearch(context: Context, word: String) {
    val cleanWord = word.trim().ifBlank { return }
    try {
        val encoded = URLEncoder.encode("define $cleanWord", "UTF-8")
        val uri = Uri.parse("https://www.google.com/search?q=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
        Toast.makeText(context, "Unable to open Google Search on this device", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun DropdownOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    theme: ReaderTheme,
    testTag: String,
    accentTint: Boolean = false,
    trailingBadge: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (accentTint) theme.accentColor.copy(alpha = 0.2f) else theme.textColor.copy(alpha = 0.08f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (accentTint) theme.accentColor else theme.textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = if (accentTint) theme.accentColor else theme.textColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.textColor.copy(alpha = 0.6f)
                    )
                }
            }

            if (trailingBadge != null) {
                trailingBadge()
            }
        }
    }
}
