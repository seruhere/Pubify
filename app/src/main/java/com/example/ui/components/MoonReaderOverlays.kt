package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reader.ReaderChapter
import com.example.reader.ReaderReadingMode
import com.example.reader.ReaderSettings
import com.example.reader.ReaderTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Classic Moon+ Reader discrete single-line reading status footer shown when HUD is auto-hidden.
 * Displays chapter title, page counter, percentage progress, live clock, and hairline progress bar.
 */
@Composable
fun MoonReaderStatusFooter(
    chapterTitle: String,
    currentGlobalPage: Int,
    totalBookPages: Int,
    overallProgress: Float,
    theme: ReaderTheme,
    onTapFooter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGlassTheme = theme == ReaderTheme.LIQUID_GLASS
    var timeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            timeString = formatter.format(Date())
            delay(30_000L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        (if (isGlassTheme) Color(0xF0070C15) else theme.backgroundColor).copy(alpha = 0.94f)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTapFooter
            )
            .padding(horizontal = 18.dp, vertical = 5.dp)
            .testTag("reader_subtle_bottom_progress_indicator")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chapterTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = theme.textColor.copy(alpha = 0.56f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )

                Text(
                    text = "p. $currentGlobalPage/$totalBookPages  •  ${(overallProgress * 100).toInt()}%  •  $timeString",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = theme.textColor.copy(alpha = 0.62f),
                    maxLines = 1
                )
            }

            LinearProgressIndicator(
                progress = { overallProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = theme.accentColor.copy(alpha = 0.85f),
                trackColor = theme.textColor.copy(alpha = 0.12f)
            )
        }
    }
}

/**
 * Moon+ Reader floating bottom HUD dock featuring quick Brightness & Theme bar,
 * Chapter Prev/Next navigation with whole-book seek slider, and the 5-button quick dock.
 */
@Composable
fun MoonReaderQuickDock(
    settings: ReaderSettings,
    currentGlobalPage: Int,
    totalBookPages: Int,
    wholeBookProgressFraction: Float,
    showReadingSlider: Boolean,
    showBrightnessQuickBar: Boolean,
    isTtsActive: Boolean,
    isDraggingSlider: Boolean,
    onDraggingSliderChange: (Boolean) -> Unit,
    onJumpFraction: (Float) -> Unit,
    onToggleReadingSlider: () -> Unit,
    onToggleBrightnessBar: () -> Unit,
    onOpenJumpToPage: () -> Unit,
    onOpenToc: () -> Unit,
    onToggleReadingMode: () -> Unit,
    onToggleTts: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onSelectTheme: (ReaderTheme) -> Unit,
    onUserActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGlassTheme = settings.theme == ReaderTheme.LIQUID_GLASS
    val accent = settings.theme.accentColor
    val textColor = settings.theme.textColor
    val dockShape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier.padding(horizontal = 12.dp),
        shape = dockShape,
        color = if (isGlassTheme) Color(0xD90A1628) else settings.theme.backgroundColor.copy(alpha = 0.88f),
        shadowElevation = if (isGlassTheme) 6.dp else 10.dp,
        border = if (isGlassTheme) {
            BorderStroke(1.dp, Color(0x4D00E5FF))
        } else {
            BorderStroke(1.dp, textColor.copy(alpha = 0.16f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Optional Moon+ Quick Brightness & Paper Preset Panel
            AnimatedVisibility(
                visible = showBrightnessQuickBar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = "Screen brightness",
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Slider(
                            value = settings.brightnessLevel.coerceIn(0.2f, 1.0f),
                            onValueChange = {
                                onUserActivity()
                                onBrightnessChange(it.coerceIn(0.2f, 1.0f))
                            },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = textColor.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("moon_brightness_slider")
                        )
                        Text(
                            text = "${(settings.brightnessLevel * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = textColor,
                            modifier = Modifier.width(38.dp)
                        )
                    }

                    // Horizontal Paper & Night Themes Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderTheme.values().forEach { th ->
                            val isSelected = settings.theme == th
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = th.backgroundColor,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accent else textColor.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onUserActivity()
                                        onSelectTheme(th)
                                    }
                                    .testTag("moon_quick_theme_${th.name.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = th.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = th.textColor
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = textColor.copy(alpha = 0.12f), thickness = 0.5.dp)
                }
            }

            // Seek Slider Panel
            AnimatedVisibility(
                visible = showReadingSlider,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                var localSliderValue by remember { mutableFloatStateOf(0f) }
                val safeTotalPages = maxOf(1, totalBookPages)
                val activeSliderVal = (if (isDraggingSlider) localSliderValue else wholeBookProgressFraction).let {
                    if (it.isNaN() || it.isInfinite()) 0f else it.coerceIn(0f, 1f)
                }
                val activeSliderPage = ((activeSliderVal * (safeTotalPages - 1)).toInt() + 1).coerceIn(1, safeTotalPages)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinearScale,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${(activeSliderVal * 100).toInt()}% • Page $activeSliderPage of $safeTotalPages",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }

                        TextButton(
                            onClick = {
                                onUserActivity()
                                onOpenJumpToPage()
                            },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("reader_jump_to_page_bottom_btn")
                        ) {
                            Icon(
                                Icons.Default.FindInPage,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = accent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Jump to Page",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = accent
                            )
                        }
                    }

                    Slider(
                        value = activeSliderVal,
                        onValueChange = { newVal ->
                            onDraggingSliderChange(true)
                            onUserActivity()
                            localSliderValue = if (newVal.isNaN() || newVal.isInfinite()) 0f else newVal.coerceIn(0f, 1f)
                        },
                        onValueChangeFinished = {
                            onDraggingSliderChange(false)
                            onUserActivity()
                            onJumpFraction(localSliderValue)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = if (isGlassTheme) Color(0x3300E5FF) else textColor.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reading_section_slider")
                    )

                    HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 0.5.dp)
                }
            }

            // Clean 4-Button Moon+ Quick Action Bar (Contents / Paged-Scroll / Visual / Listen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoonDockActionButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    label = "Contents",
                    tint = textColor,
                    isActive = false,
                    accentColor = accent,
                    onClick = {
                        onUserActivity()
                        onOpenToc()
                    },
                    modifier = Modifier.weight(1f).testTag("moon_dock_toc_button")
                )

                val isPaged = settings.readingMode == ReaderReadingMode.PAGINATED
                MoonDockActionButton(
                    icon = if (isPaged) Icons.Default.AutoStories else Icons.Default.ViewDay,
                    label = if (isPaged) "Paged" else "Scroll",
                    tint = textColor,
                    isActive = isPaged,
                    accentColor = accent,
                    onClick = {
                        onUserActivity()
                        onToggleReadingMode()
                    },
                    modifier = Modifier.weight(1f).testTag("moon_dock_mode_toggle_button")
                )

                MoonDockActionButton(
                    icon = Icons.Default.Brightness6,
                    label = "Visual",
                    tint = textColor,
                    isActive = showBrightnessQuickBar,
                    accentColor = accent,
                    onClick = {
                        onUserActivity()
                        onToggleBrightnessBar()
                    },
                    modifier = Modifier.weight(1f).testTag("moon_dock_brightness_button")
                )

                MoonDockActionButton(
                    icon = if (isTtsActive) Icons.Default.GraphicEq else Icons.Default.Headphones,
                    label = if (isTtsActive) "Playing" else "Listen",
                    tint = textColor,
                    isActive = isTtsActive,
                    accentColor = accent,
                    onClick = {
                        onUserActivity()
                        onToggleTts()
                    },
                    modifier = Modifier.weight(1f).testTag("moon_dock_tts_button")
                )
            }
        }
    }
}

@Composable
private fun MoonDockActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTint = if (isActive) accentColor else tint.copy(alpha = 0.85f)
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = activeTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            ),
            color = activeTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Hierarchical Table of Contents list displaying PDF/EPUB Chapters, Topics (H2), and
 * Sub-topics (H3/H4) matching the PDF's exact structure so jumping directly to any
 * topic or sub-topic works seamlessly.
 */
@Composable
fun MoonTocHierarchicalList(
    chapters: List<ReaderChapter>,
    currentChapterIndex: Int,
    onSelectChapter: (Int) -> Unit,
    onSelectSubHeading: (chapterIndex: Int, paragraphIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSubHeadings = remember(chapters) { chapters.sumOf { it.subHeadings.size } }
    val hasAnySubHeadings = totalSubHeadings > 0
    val defaultExpandAll = totalSubHeadings in 1..300

    val expandedChapters = remember(chapters, currentChapterIndex) {
        mutableStateMapOf<Int, Boolean>().apply {
            chapters.forEachIndexed { idx, ch ->
                if (ch.subHeadings.isNotEmpty()) {
                    put(idx, defaultExpandAll || idx == currentChapterIndex)
                }
            }
        }
    }

    val allExpanded = hasAnySubHeadings && chapters.indices.all { idx ->
        chapters[idx].subHeadings.isEmpty() || (expandedChapters[idx] == true)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (hasAnySubHeadings) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalTopics = remember(chapters) { chapters.sumOf { ch -> ch.subHeadings.count { it.level <= 2 } } }
                val totalSubTopics = remember(chapters) { chapters.sumOf { ch -> ch.subHeadings.count { it.level >= 3 } } }
                val countsLabel = buildString {
                    if (totalTopics > 0) append("$totalTopics topics")
                    if (totalTopics > 0 && totalSubTopics > 0) append(" • ")
                    if (totalSubTopics > 0) append("$totalSubTopics sub-topics")
                }
                Text(
                    text = countsLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        val targetState = !allExpanded
                        chapters.forEachIndexed { idx, ch ->
                            if (ch.subHeadings.isNotEmpty()) {
                                expandedChapters[idx] = targetState
                            }
                        }
                    },
                    modifier = Modifier.testTag("toc_toggle_expand_all")
                ) {
                    Text(
                        text = if (allExpanded) "Collapse All" else "Expand All",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(chapters) { index, ch ->
                val isSelected = index == currentChapterIndex
                val isFinished = index < currentChapterIndex
                val hasSubHeadings = ch.subHeadings.isNotEmpty()
                val isExpanded = expandedChapters[index] ?: (defaultExpandAll || isSelected)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectChapter(index) }
                            .testTag("toc_chapter_row_$index"),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ch.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (hasSubHeadings) {
                                        val topicCount = ch.subHeadings.count { it.level <= 2 }
                                        val subTopicCount = ch.subHeadings.count { it.level >= 3 }
                                        val summaryLabel = when {
                                            topicCount > 0 && subTopicCount > 0 -> "$topicCount topics • $subTopicCount sub-topics"
                                            topicCount > 0 -> "$topicCount topics"
                                            else -> "$subTopicCount sub-topics"
                                        }
                                        Text(
                                            text = "$summaryLabel • ~${(ch.wordCount / 200).coerceAtLeast(1)} min",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Current chapter",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (isFinished) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Read",
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (hasSubHeadings) {
                                    IconButton(
                                        onClick = { expandedChapters[index] = !isExpanded },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("toc_expand_chapter_$index")
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "Collapse topics" else "Expand topics",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Indented PDF Topics (H2) & Sub-topics (H3/H4) tree matching the PDF structure
                    if (hasSubHeadings && isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 8.dp, top = 2.dp, bottom = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            ch.subHeadings.forEachIndexed { subIdx, sub ->
                                val isTopic = sub.level <= 2
                                val indentDp = when {
                                    sub.level <= 2 -> 6.dp
                                    sub.level == 3 -> 22.dp
                                    else -> 36.dp
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = indentDp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelectSubHeading(index, sub.paragraphIndex)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("toc_subheading_${index}_$subIdx"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isTopic) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.78f))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.SubdirectoryArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = sub.title,
                                        style = when {
                                            sub.level <= 2 -> MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                            sub.level == 3 -> MaterialTheme.typography.bodySmall
                                            else -> MaterialTheme.typography.labelSmall
                                        },
                                        color = if (isTopic) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
