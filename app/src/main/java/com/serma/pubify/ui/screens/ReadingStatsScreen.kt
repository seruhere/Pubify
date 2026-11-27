package com.serma.pubify.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serma.pubify.data.model.ConvertedBook
import com.serma.pubify.data.model.DailyReadingStat
import com.serma.pubify.data.model.ReadingSession
import com.serma.pubify.data.model.ReadingStatsCalculator
import com.serma.pubify.data.model.ReadingStatsSummary
import com.serma.pubify.reader.ThemeMode
import com.serma.pubify.ui.components.GlassCard
import com.serma.pubify.ui.components.LocalGlassPalette
import com.serma.pubify.ui.components.LuxuryGold
import com.serma.pubify.ui.components.LuxuryGoldDeep
import com.serma.pubify.ui.components.CrystalBlue
import com.serma.pubify.ui.components.iosTapBounce
import com.serma.pubify.ui.components.luxuryBlueColor
import com.serma.pubify.ui.components.luxuryFilterChipColors
import com.serma.pubify.ui.components.luxuryGoldBlueBorderBrush
import com.serma.pubify.ui.components.luxuryGoldColor
import com.serma.pubify.ui.viewmodel.ConverterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReadingStatsScreen(
    viewModel: ConverterViewModel,
    onOpenReader: (ConvertedBook) -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stats by viewModel.readingStatsSummary.collectAsStateWithLifecycle()
    val sessions by viewModel.readingSessions.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isGlass = themeMode == ThemeMode.LIQUID_GLASS
    val context = LocalContext.current

    var selectedActivityMetric by remember { mutableIntStateOf(0) } // 0: Minutes, 1: Pages
    var showResetStatsDialog by remember { mutableStateOf(false) }
    var alsoResetBookProgress by remember { mutableStateOf(false) }

    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            containerColor = LocalGlassPalette.current.dialogSurface,
            icon = {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Reset Reading Stats?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This will clear your reading time, streaks, daily activity charts, and session history. Your books in My Books will not be deleted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { alsoResetBookProgress = !alsoResetBookProgress }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = alsoResetBookProgress,
                            onCheckedChange = { alsoResetBookProgress = it },
                            modifier = Modifier.testTag("reset_stats_include_books_checkbox")
                        )
                        Text(
                            text = "Also reset book completion & reading progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetReadingStats(alsoResetBookProgress)
                        showResetStatsDialog = false
                        alsoResetBookProgress = false
                        Toast.makeText(context, "Reading statistics reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reset_stats_button")
                ) {
                    Text("Reset Stats")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reading_stats_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Luxury Gold & Blue Header Bar with embedded Reset Stats container
        item {
            StatsHeaderSection(
                isGlass = isGlass,
                onResetClick = { showResetStatsDialog = true }
            )
        }

        // 2. Overview KPI Grid
        item {
            StatsKpiGrid(
                stats = stats,
                isGlass = isGlass,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 3. Daily Reading Activity Chart (Mon-Sun Bar Chart)
        item {
            DailyActivityChartCard(
                weeklyStats = stats.weeklyDayStats,
                selectedMetric = selectedActivityMetric,
                onMetricChange = { selectedActivityMetric = it },
                isGlass = isGlass,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 4. Reading Velocity & Pace Trend (Line & Area Chart)
        item {
            ReadingVelocityChartCard(
                weeklyStats = stats.weeklyDayStats,
                stats = stats,
                isGlass = isGlass,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 5. Library Completion Donut / Ring Chart
        item {
            LibraryCompletionCard(
                stats = stats,
                isGlass = isGlass,
                onNavigateToLibrary = onNavigateToLibrary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 6. Recent Reading Sessions Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = luxuryGoldColor(),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Reading Sessions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = CircleShape,
                        color = luxuryBlueColor().copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, luxuryGoldColor().copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "${sessions.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = luxuryBlueColor(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 7. Recent Reading Sessions List or Empty State
        if (sessions.isEmpty()) {
            item {
                EmptySessionsCard(
                    isGlass = isGlass,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(
                items = sessions.sortedByDescending { it.timestampMillis }.take(10),
                key = { it.id }
            ) { session ->
                ReadingSessionItemCard(
                    session = session,
                    isGlass = isGlass,
                    onDelete = {
                        viewModel.deleteReadingSession(session.id)
                        Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun StatsHeaderSection(
    isGlass: Boolean,
    onResetClick: () -> Unit
) {
    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()
    val isDark = LocalGlassPalette.current.isDark

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        elevated = true,
        accentTint = gold,
        borderWidth = 1.25.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    gold.copy(alpha = if (isDark) 0.22f else 0.16f),
                                    blue.copy(alpha = if (isDark) 0.20f else 0.14f)
                                )
                            )
                        )
                        .border(1.1.dp, luxuryGoldBlueBorderBrush(0.75f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ANALYTICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            fontSize = 10.sp
                        ),
                        color = gold
                    )
                    Text(
                        text = "Reading Statistics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Habits, velocity & books",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Compact Reset Stats pill container neatly embedded inside the header card
            Surface(
                onClick = onResetClick,
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x33152946) else blue.copy(alpha = 0.08f),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            gold.copy(alpha = 0.72f),
                            blue.copy(alpha = 0.65f)
                        )
                    )
                ),
                modifier = Modifier.testTag("reset_stats_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Stats",
                        tint = gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsKpiGrid(
    stats: ReadingStatsSummary,
    isGlass: Boolean,
    modifier: Modifier = Modifier
) {
    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Total Time",
                value = stats.formattedTotalTime,
                subtitle = "${stats.totalSessionsCount} reading sessions",
                icon = Icons.Default.Timer,
                accentColor = blue,
                isGlass = isGlass,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_total_reading_time")
            )
            KpiMetricCard(
                title = "Avg / Session",
                value = if (stats.averagePagesPerSession > 0) {
                    String.format(Locale.US, "%.1f pgs", stats.averagePagesPerSession)
                } else "0 pgs",
                subtitle = if (stats.averageMinutesPerSession > 0) {
                    "~${stats.averageMinutesPerSession.toInt()} mins per read"
                } else "No data yet",
                icon = Icons.Default.AutoStories,
                accentColor = gold,
                isGlass = isGlass,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_avg_pages_per_session")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Completed",
                value = "${stats.completedBooksCount} books",
                subtitle = "${stats.inProgressBooksCount} currently reading",
                icon = Icons.Default.CheckCircle,
                accentColor = if (isGlass) Color(0xFF00E676) else Color(0xFF10B981),
                isGlass = isGlass,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_books_completed")
            )
            KpiMetricCard(
                title = "Reading Streak",
                value = "${stats.currentStreakDays} days",
                subtitle = if (stats.currentStreakDays > 0) "Keep up the momentum!" else "Read today to start!",
                icon = Icons.Default.LocalFireDepartment,
                accentColor = gold,
                isGlass = isGlass,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_reading_streak")
            )
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isGlass: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        accentTint = accentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (isGlass) 0.2f else 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DailyActivityChartCard(
    weeklyStats: List<DailyReadingStat>,
    selectedMetric: Int, // 0: Minutes, 1: Pages
    onMetricChange: (Int) -> Unit,
    isGlass: Boolean,
    modifier: Modifier = Modifier
) {
    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_activity_bar_chart_card"),
        shape = RoundedCornerShape(22.dp),
        accentTint = blue
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with metric toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Daily Reading Activity",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Last 7 days reading volume",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Metric Toggle Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedMetric == 0,
                        onClick = { onMetricChange(0) },
                        label = { Text("Minutes", style = MaterialTheme.typography.labelSmall) },
                        colors = luxuryFilterChipColors(useGoldAccent = true),
                        modifier = Modifier.iosTapBounce(pressedScale = 0.94f)
                    )
                    FilterChip(
                        selected = selectedMetric == 1,
                        onClick = { onMetricChange(1) },
                        label = { Text("Pages", style = MaterialTheme.typography.labelSmall) },
                        colors = luxuryFilterChipColors(useGoldAccent = false),
                        modifier = Modifier.iosTapBounce(pressedScale = 0.94f)
                    )
                }
            }

            val maxVal = weeklyStats.maxOfOrNull {
                if (selectedMetric == 0) it.minutesRead else it.pagesRead
            }?.coerceAtLeast(1) ?: 1
            val scaleMax = if (selectedMetric == 0) maxOf(45, ((maxVal + 14) / 15) * 15) else maxOf(30, ((maxVal + 9) / 10) * 10)

            // Bar Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Background subtle dashed gridlines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val steps = 3
                    val stepHeight = size.height / steps
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    val gridColor = if (isGlass) Color(0x22FFFFFF) else Color(0x18000000)

                    for (i in 1..steps) {
                        val y = size.height - (i * stepHeight)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = pathEffect
                        )
                    }
                }

                // Columns of bars
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyStats.forEach { dayStat ->
                        val value = if (selectedMetric == 0) dayStat.minutesRead else dayStat.pagesRead
                        val fraction = (value.toFloat() / scaleMax.toFloat()).coerceIn(0.04f, 1f)

                        DailyBarColumn(
                            dayStat = dayStat,
                            value = value,
                            fraction = fraction,
                            unit = if (selectedMetric == 0) "m" else "p",
                            isGlass = isGlass,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBarColumn(
    dayStat: DailyReadingStat,
    value: Int,
    fraction: Float,
    unit: String,
    isGlass: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Value label above bar
        Text(
            text = if (value > 0) "$value$unit" else "-",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (dayStat.isToday) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (dayStat.isToday) {
                if (isGlass) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
            } else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Bar Container with proportional height
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(130.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Track background pill
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isGlass) Color(0x18FFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            )

            // Luxury Champagne Gold -> Crystal Blue gradient bar
            val goldBar = luxuryGoldColor()
            val blueBar = luxuryBlueColor()
            val barBrush = Brush.verticalGradient(
                listOf(goldBar, blueBar)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(10.dp))
                    .background(barBrush)
                    .then(
                        if (dayStat.isToday) {
                            Modifier.border(1.5.dp, LuxuryGold.copy(alpha = 0.95f), RoundedCornerShape(10.dp))
                        } else Modifier
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day label
        Text(
            text = dayStat.dayLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (dayStat.isToday) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (dayStat.isToday) luxuryGoldColor() else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadingVelocityChartCard(
    weeklyStats: List<DailyReadingStat>,
    stats: ReadingStatsSummary,
    isGlass: Boolean,
    modifier: Modifier = Modifier
) {
    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reading_velocity_trend_card"),
        shape = RoundedCornerShape(22.dp),
        accentTint = gold
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = blue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Reading Velocity Trend",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Pages read momentum over time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = gold.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, gold.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "${stats.totalPagesRead} total pgs",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = gold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Canvas Smooth Line & Gradient Area Chart
            val values = weeklyStats.map { it.pagesRead.toFloat() }
            val maxPages = values.maxOrNull()?.coerceAtLeast(10f) ?: 10f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (values.size < 2) return@Canvas
                    val width = size.width
                    val height = size.height - 16f
                    val topPadding = 12f
                    val chartHeight = height - topPadding
                    val stepX = width / (values.size - 1).coerceAtLeast(1)

                    val points = values.mapIndexed { index, v ->
                        val x = index * stepX
                        val normalized = (v / maxPages).coerceIn(0f, 1f)
                        val y = topPadding + (chartHeight * (1f - normalized))
                        Offset(x, y)
                    }

                    // Build smooth cubic bezier path
                    val strokePath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        }
                    }

                    // Build area fill path
                    val areaPath = Path().apply {
                        addPath(strokePath)
                        lineTo(points.last().x, size.height)
                        lineTo(points.first().x, size.height)
                        close()
                    }

                    val gradientBrush = Brush.verticalGradient(
                        colors = listOf(
                            blue.copy(alpha = 0.40f),
                            gold.copy(alpha = 0.04f)
                        )
                    )

                    // Draw area fill
                    drawPath(path = areaPath, brush = gradientBrush)

                    // Draw gold-to-blue luxury line stroke
                    drawPath(
                        path = strokePath,
                        brush = Brush.horizontalGradient(listOf(blue, gold)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw glowing point circles
                    points.forEachIndexed { idx, point ->
                        val isToday = (idx == points.size - 1)
                        drawCircle(
                            color = if (isToday) LuxuryGold else blue,
                            radius = if (isToday) 5.dp.toPx() else 3.5.dp.toPx(),
                            center = point
                        )
                        if (isToday) {
                            drawCircle(
                                color = LuxuryGold.copy(alpha = 0.5f),
                                radius = 9.dp.toPx(),
                                center = point,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Labels under velocity chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyStats.forEach {
                    Text(
                        text = it.dayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (it.isToday) gold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryCompletionCard(
    stats: ReadingStatsSummary,
    isGlass: Boolean,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("library_completion_donut_card"),
        shape = RoundedCornerShape(22.dp),
        accentTint = blue
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Library Completion",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Progress across converted books",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onNavigateToLibrary) {
                    Text(
                        text = "View Books",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = gold
                    )
                }
            }

            val totalBooks = stats.totalBooksCount
            val completed = stats.completedBooksCount
            val inProgress = stats.inProgressBooksCount
            val unread = stats.unreadBooksCount
            val completionPercent = if (totalBooks > 0) ((completed.toFloat() / totalBooks.toFloat()) * 100f).toInt() else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Chart Canvas
                val inProgressArcColor = blue
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        val arcSize = Size(size.width, size.height)

                        if (totalBooks == 0) {
                            // Empty placeholder ring
                            drawArc(
                                color = if (isGlass) Color(0x22FFFFFF) else Color(0x20000000),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                size = arcSize
                            )
                            return@Canvas
                        }

                        val completedAngle = (completed.toFloat() / totalBooks.toFloat()) * 360f
                        val inProgressAngle = (inProgress.toFloat() / totalBooks.toFloat()) * 360f
                        val unreadAngle = 360f - completedAngle - inProgressAngle

                        var currentAngle = -90f

                        // Completed Arc
                        if (completedAngle > 0f) {
                            drawArc(
                                color = if (isGlass) Color(0xFF00E676) else Color(0xFF10B981),
                                startAngle = currentAngle,
                                sweepAngle = completedAngle.coerceAtMost(359.9f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                size = arcSize
                            )
                            currentAngle += completedAngle
                        }

                        // In Progress Arc
                        if (inProgressAngle > 0f) {
                            drawArc(
                                color = inProgressArcColor,
                                startAngle = currentAngle,
                                sweepAngle = inProgressAngle.coerceAtMost(359.9f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                size = arcSize
                            )
                            currentAngle += inProgressAngle
                        }

                        // Unread Arc
                        if (unreadAngle > 0f) {
                            drawArc(
                                color = if (isGlass) Color(0x30FFFFFF) else Color(0x30000000),
                                startAngle = currentAngle,
                                sweepAngle = unreadAngle.coerceAtMost(359.9f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                size = arcSize
                            )
                        }
                    }

                    // Center percentage text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$completionPercent%",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DonutLegendRow(
                        label = "Completed",
                        count = completed,
                        color = if (isGlass) Color(0xFF00E676) else Color(0xFF10B981)
                    )
                    DonutLegendRow(
                        label = "Reading",
                        count = inProgress,
                        color = blue
                    )
                    DonutLegendRow(
                        label = "Unread",
                        count = unread,
                        color = if (isGlass) Color(0x70FFFFFF) else Color(0x60000000)
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutLegendRow(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "$count books",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptySessionsCard(
    isGlass: Boolean,
    modifier: Modifier = Modifier
) {
    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        accentTint = gold
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                gold.copy(alpha = 0.22f),
                                blue.copy(alpha = 0.22f)
                            )
                        )
                    )
                    .border(1.1.dp, luxuryGoldBlueBorderBrush(0.75f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = gold,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "No Reading Sessions Yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Your reading time, pace, and progress are automatically recorded as you read books in the Reader.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReadingSessionItemCard(
    session: ReadingSession,
    isGlass: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(session.timestampMillis) {
        dateFormatter.format(Date(session.timestampMillis))
    }
    val minutes = (session.durationSeconds / 60).coerceAtLeast(1)
    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        accentTint = blue
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                gold.copy(alpha = 0.20f),
                                blue.copy(alpha = 0.20f)
                            )
                        )
                    )
                    .border(1.dp, luxuryGoldBlueBorderBrush(0.65f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = gold,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.bookTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = blue.copy(alpha = 0.16f),
                        border = BorderStroke(0.75.dp, blue.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${minutes}m read",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = blue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (session.pagesRead > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = gold.copy(alpha = 0.18f),
                            border = BorderStroke(0.75.dp, gold.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "${session.pagesRead} pages",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = gold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete session",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ManualSessionDialog(
    libraryBooks: List<ConvertedBook>,
    isGlass: Boolean,
    onDismiss: () -> Unit,
    onSave: (bookTitle: String, durationMins: Int, pagesRead: Int) -> Unit
) {
    var bookTitle by remember { mutableStateOf(libraryBooks.firstOrNull()?.title ?: "My Book") }
    var durationMinutesText by remember { mutableStateOf("25") }
    var pagesReadText by remember { mutableStateOf("15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalGlassPalette.current.dialogSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Log Reading Session", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    label = { Text("Book Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_session_book_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = durationMinutesText,
                    onValueChange = { durationMinutesText = it.filter { char -> char.isDigit() } },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_session_duration_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pagesReadText,
                    onValueChange = { pagesReadText = it.filter { char -> char.isDigit() } },
                    label = { Text("Pages Read") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_session_pages_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationMinutesText.toIntOrNull() ?: 15
                    val pages = pagesReadText.toIntOrNull() ?: 10
                    onSave(bookTitle.ifBlank { "Reading Session" }, duration, pages)
                },
                modifier = Modifier.testTag("manual_session_save_button")
            ) {
                Text("Save Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
