package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.converter.EpubMargin
import com.example.converter.ImageCompressionLevel
import com.example.reader.PageTurnMode
import com.example.reader.ReaderReadingMode
import com.example.reader.ReaderTextAlign
import com.example.reader.ThemeMode
import com.example.ui.components.GlassCard
import com.example.ui.components.LocalGlassPalette
import com.example.ui.components.formatBytes
import com.example.ui.components.luxuryBlueColor
import com.example.ui.components.luxuryFilterChipColors
import com.example.ui.components.luxuryGoldBlueBorderBrush
import com.example.ui.components.luxuryGoldColor
import com.example.ui.components.luxurySwitchColors
import com.example.ui.theme.GreenSuccess
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.signOutFromFirebase
import com.example.ui.viewmodel.CloudSyncViewModel
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.ConverterViewModel
import com.google.firebase.auth.FirebaseUser

private val CacheAmber = Color(0xFFF59E0B)
private val CacheCoral = Color(0xFFEF4444)

@Composable
fun ToolsScreen(
    viewModel: ConverterViewModel,
    cloudSyncViewModel: CloudSyncViewModel? = null,
    currentUser: FirebaseUser? = null,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val conversionOptions by viewModel.conversionOptions.collectAsStateWithLifecycle()
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val isClearingCache by viewModel.isClearingCache.collectAsStateWithLifecycle()
    val lastCacheDelta by viewModel.lastCacheDelta.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val readingSessions by viewModel.readingSessions.collectAsStateWithLifecycle()
    val allHighlights by viewModel.allHighlights.collectAsStateWithLifecycle()
    val allBookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()

    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()
    val isDark = LocalGlassPalette.current.isDark
    val goldChips = luxuryFilterChipColors(useGoldAccent = true)
    val blueChips = luxuryFilterChipColors(useGoldAccent = false)

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Luxury Gold & Blue Editorial Header
        Row(
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
                                blue.copy(alpha = if (isDark) 0.22f else 0.14f)
                            )
                        )
                    )
                    .border(1.2.dp, luxuryGoldBlueBorderBrush(0.8f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = gold,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        fontSize = 10.sp
                    ),
                    color = gold
                )
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("settings_header_title")
                )
            }
        }

        // 0. Cloud Sync & Account Card (Firebase Auth + Firestore)
        if (cloudSyncViewModel != null && currentUser != null) {
            CloudAccountSyncCard(
                cloudSyncViewModel = cloudSyncViewModel,
                currentUser = currentUser,
                onSyncNow = {
                    cloudSyncViewModel.syncFullLibraryNow(
                        displayName = currentUser.displayName ?: "Pubify Reader",
                        email = currentUser.email,
                        photoUrl = currentUser.photoUrl?.toString(),
                        books = allBooks,
                        sessions = readingSessions,
                        highlights = allHighlights,
                        bookmarks = allBookmarks
                    )
                },
                gold = gold,
                blue = blue,
                isDark = isDark
            )
        }

        // 1. Display & Theme Card (Gold Accent)
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("theme_mode_options_card"),
            shape = RoundedCornerShape(22.dp),
            elevated = true,
            accentTint = gold,
            borderWidth = 1.2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingsSectionHeader(
                    icon = Icons.Default.Palette,
                    title = "Display & Theme",
                    subtitle = "Liquid Glass, Dark, or Light appearance",
                    accentColor = gold
                )

                FilterChip(
                    selected = themeMode == ThemeMode.LIQUID_GLASS,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIQUID_GLASS) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.BlurOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = blue
                        )
                    },
                    label = { Text("Liquid Glass (Frosted Neon)", fontWeight = FontWeight.Bold) },
                    colors = goldChips,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_chip_liquid_glass")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        leadingIcon = {
                            Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Dark", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = blueChips,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_chip_dark")
                    )

                    FilterChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        leadingIcon = {
                            Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Light", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = goldChips,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_chip_light")
                    )

                    FilterChip(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        leadingIcon = {
                            Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("System", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = blueChips,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_chip_system")
                    )
                }
            }
        }

        // 2. Conversion Defaults Card (Crystal Blue Accent)
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("conversion_settings_card"),
            shape = RoundedCornerShape(22.dp),
            accentTint = blue
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsSectionHeader(
                    icon = Icons.Default.Tune,
                    title = "Conversion Defaults",
                    subtitle = "Default behavior for PDF to EPUB exports",
                    accentColor = blue,
                    titleColor = gold
                )

                HorizontalDivider(color = gold.copy(alpha = 0.22f))

                SettingsSwitchRow(
                    title = "Auto-Extract Cover Image",
                    subtitle = "Use the first PDF page as the eBook cover",
                    checked = conversionOptions.extractCover,
                    onCheckedChange = { checked ->
                        viewModel.updateConversionOptions { it.copy(extractCover = checked) }
                    },
                    testTag = "setting_extract_cover_switch"
                )

                SettingsSwitchRow(
                    title = "Strip Headers & Footers",
                    subtitle = "Remove repeated page numbers and running headers",
                    checked = conversionOptions.stripHeadersFooters,
                    onCheckedChange = { checked ->
                        viewModel.updateConversionOptions { it.copy(stripHeadersFooters = checked) }
                    },
                    testTag = "setting_strip_headers_switch"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Default Page Margins",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = gold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val marginPresets = listOf(
                            EpubMargin.MATCH_PDF to "Match PDF",
                            EpubMargin.COMPACT to "Compact",
                            EpubMargin.STANDARD to "Standard",
                            EpubMargin.WIDE to "Spacious"
                        )
                        marginPresets.forEach { (preset, label) ->
                            FilterChip(
                                selected = conversionOptions.margin == preset,
                                onClick = {
                                    viewModel.updateConversionOptions {
                                        it.copy(margin = preset, customMarginPercent = null)
                                    }
                                },
                                label = { Text(label) },
                                colors = blueChips,
                                modifier = Modifier.testTag("setting_margin_${preset.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Image Compression",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = gold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val compressionPresets = listOf(
                            ImageCompressionLevel.LOW to "High Quality",
                            ImageCompressionLevel.BALANCED to "Balanced",
                            ImageCompressionLevel.HIGH to "Compact",
                            ImageCompressionLevel.MAXIMUM to "Smallest"
                        )
                        compressionPresets.forEach { (preset, label) ->
                            FilterChip(
                                selected = conversionOptions.imageCompression == preset,
                                onClick = {
                                    viewModel.updateConversionOptions {
                                        it.copy(imageCompression = preset, customImageQuality = null)
                                    }
                                },
                                label = { Text(label) },
                                colors = goldChips,
                                modifier = Modifier.testTag("setting_compression_${preset.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 3. Reader Preferences Card (Blue Section Border + Golden Typography matching Conversion Defaults)
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reader_settings_card"),
            shape = RoundedCornerShape(22.dp),
            accentTint = blue
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsSectionHeader(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Reader Preferences",
                    subtitle = "Default layout, navigation & typography",
                    accentColor = blue,
                    titleColor = gold
                )

                HorizontalDivider(color = gold.copy(alpha = 0.22f))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Reading Layout",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = gold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderReadingMode.entries.forEach { mode ->
                            FilterChip(
                                selected = readerSettings.readingMode == mode,
                                onClick = {
                                    viewModel.updateReaderSettings { it.copy(readingMode = mode) }
                                },
                                label = { Text(mode.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = blueChips,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("setting_reading_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Page Turn Controls",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = gold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PageTurnMode.entries.forEach { mode ->
                            FilterChip(
                                selected = readerSettings.pageTurnMode == mode,
                                onClick = {
                                    viewModel.updateReaderSettings { it.copy(pageTurnMode = mode) }
                                },
                                label = { Text(mode.displayName) },
                                colors = goldChips,
                                modifier = Modifier.testTag("setting_page_turn_${mode.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Text Alignment",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = gold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ReaderTextAlign.JUSTIFY,
                            ReaderTextAlign.LEFT,
                            ReaderTextAlign.CENTER
                        ).forEach { align ->
                            FilterChip(
                                selected = readerSettings.textAlign == align,
                                onClick = {
                                    viewModel.updateReaderSettings { it.copy(textAlign = align) }
                                },
                                label = { Text(align.displayName) },
                                colors = blueChips,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("setting_text_align_${align.name.lowercase()}")
                            )
                        }
                    }
                }

                SettingsSwitchRow(
                    title = "Show Tap Zones Overlay",
                    subtitle = "Highlight screen tap regions in the Reader",
                    checked = readerSettings.showTapZonesOverlay,
                    onCheckedChange = { checked ->
                        viewModel.updateReaderSettings { it.copy(showTapZonesOverlay = checked) }
                    },
                    testTag = "setting_tap_zones_switch"
                )
            }
        }

        // 4. Clean Clear Cache Card (Blue Section Border + Golden Typography + Live Color-Coded Change Feedback)
        val isCacheClean = cacheSizeBytes <= 0L
        val cacheStatusColor by animateColorAsState(
            targetValue = when {
                isCacheClean -> GreenSuccess
                cacheSizeBytes > 25L * 1024L * 1024L -> CacheCoral
                else -> gold
            },
            animationSpec = tween(400),
            label = "cache_status_color"
        )
        val rawFillFraction = when {
            isCacheClean -> 0f
            else -> (cacheSizeBytes.toFloat() / (50f * 1024f * 1024f)).coerceIn(0.12f, 1f)
        }
        val animatedFillFraction by animateFloatAsState(
            targetValue = rawFillFraction,
            animationSpec = tween(500),
            label = "cache_fill_bar"
        )

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clear_cache_card"),
            shape = RoundedCornerShape(22.dp),
            accentTint = blue
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(blue.copy(alpha = 0.16f))
                                .border(1.dp, blue.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = blue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Clear Cache",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = gold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = cacheStatusColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, cacheStatusColor.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("cache_size_badge")
                    ) {
                        Text(
                            text = if (isCacheClean) "0 B • Clean" else formatBytes(cacheSizeBytes),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = cacheStatusColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // Animated visual cache gauge bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(blue.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isCacheClean) 1f else animatedFillFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (isCacheClean) GreenSuccess.copy(alpha = 0.65f) else cacheStatusColor
                            )
                    )
                }

                // Visual before -> after cache change feedback pill
                AnimatedVisibility(
                    visible = lastCacheDelta != null,
                    enter = fadeIn(tween(250)) + expandVertically()
                ) {
                    lastCacheDelta?.let { delta ->
                        val hasFreed = delta.freedBytes > 0L
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = blue.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, if (hasFreed) GreenSuccess.copy(alpha = 0.55f) else gold.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cache_delta_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GreenSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    if (hasFreed) {
                                        Text(
                                            text = formatBytes(delta.beforeBytes),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = gold
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = blue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = formatBytes(delta.afterBytes),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = GreenSuccess
                                        )
                                    } else {
                                        Text(
                                            text = "Already clean • 0 B",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = gold
                                        )
                                    }
                                }

                                if (hasFreed) {
                                    Text(
                                        text = "-${formatBytes(delta.freedBytes)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = GreenSuccess
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.clearAppCache() },
                    enabled = !isClearingCache,
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.1.dp, blue.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("clear_cache_button")
                ) {
                    if (isClearingCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = gold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clearing Cache...", color = gold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clear Cache",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = gold
                        )
                    }
                }
            }
        }

        // 5. About & Privacy Architecture Card (Gold & Blue Rim)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            accentTint = blue
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(gold.copy(alpha = 0.16f))
                            .border(1.dp, luxuryGoldBlueBorderBrush(0.65f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "About App",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "100% Offline Processing • On-Device EPUB Engine • Unlimited Conversions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bottom clearance above floating glass pill navigation bar
        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(92.dp)
        )
    }
}

@Composable
private fun CloudAccountSyncCard(
    cloudSyncViewModel: CloudSyncViewModel,
    currentUser: FirebaseUser,
    onSyncNow: () -> Unit,
    gold: Color,
    blue: Color,
    isDark: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val cloudBooksState by cloudSyncViewModel.cloudBooksState.collectAsStateWithLifecycle()
    val cloudSessionsState by cloudSyncViewModel.cloudSessionsState.collectAsStateWithLifecycle()
    val isSyncing by cloudSyncViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusMessage by cloudSyncViewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val syncError by cloudSyncViewModel.syncError.collectAsStateWithLifecycle()

    val syncedBooksLabel = when (val state = cloudBooksState) {
        is CloudUiState.Loading -> "Loading..."
        is CloudUiState.Success -> "${state.data.size} synced"
        is CloudUiState.Error -> "Offline"
    }
    val syncedSessionsLabel = when (val state = cloudSessionsState) {
        is CloudUiState.Loading -> "..."
        is CloudUiState.Success -> "${state.data.size} logs"
        is CloudUiState.Error -> "Offline"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_sync_account_card"),
        shape = RoundedCornerShape(22.dp),
        elevated = true,
        accentTint = gold,
        borderWidth = 1.25.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        gold.copy(alpha = if (isDark) 0.22f else 0.16f),
                                        blue.copy(alpha = if (isDark) 0.22f else 0.14f)
                                    )
                                )
                            )
                            .border(1.dp, luxuryGoldBlueBorderBrush(0.75f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(id = R.string.cloud_sync_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = gold
                        )
                        Text(
                            text = "Firebase Firestore • Connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = GreenSuccess.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GreenSuccess,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Account identity & live cloud counts pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x33152946) else blue.copy(alpha = 0.08f),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(gold.copy(alpha = 0.45f), blue.copy(alpha = 0.45f))
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(gold.copy(alpha = 0.18f))
                            .border(1.dp, gold.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "Pubify Reader",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        currentUser.email?.takeIf { it.isNotBlank() }?.let { email ->
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Books: $syncedBooksLabel  •  Sessions: $syncedSessionsLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = gold
                        )
                    }
                }
            }

            AnimatedVisibility(visible = syncStatusMessage != null || syncError != null) {
                val isError = syncError != null
                val message = syncError ?: syncStatusMessage.orEmpty()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else blue.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.55f) else GreenSuccess.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isError) MaterialTheme.colorScheme.error else GreenSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = Color(0xFF0A1426),
                        disabledContainerColor = gold.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("cloud_sync_now_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0A1426)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.cloud_sync_now),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = { signOutFromFirebase(credentialManager, scope) },
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.1.dp, blue.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("cloud_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.cloud_sign_out),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.16f))
                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = luxurySwitchColors(),
            modifier = Modifier.testTag(testTag)
        )
    }
}
