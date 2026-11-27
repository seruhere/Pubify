package com.serma.pubify.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.serma.pubify.converter.EpubMargin
import com.serma.pubify.converter.ImageCompressionLevel
import com.serma.pubify.reader.PageTurnMode
import com.serma.pubify.reader.ReaderReadingMode
import com.serma.pubify.reader.ReaderTextAlign
import com.serma.pubify.reader.ThemeMode
import com.serma.pubify.ui.components.GlassCard
import com.serma.pubify.ui.components.IosMotion
import com.serma.pubify.ui.components.LocalGlassPalette
import com.serma.pubify.ui.components.formatBytes
import com.serma.pubify.ui.components.iosTapBounce
import com.serma.pubify.ui.components.luxuryBlueColor
import com.serma.pubify.ui.components.luxuryFilterChipColors
import com.serma.pubify.ui.components.luxuryGoldBlueBorderBrush
import com.serma.pubify.ui.components.luxuryGoldColor
import com.serma.pubify.ui.components.luxurySwitchColors
import com.serma.pubify.ui.theme.GreenSuccess
import com.serma.pubify.ui.viewmodel.ConverterViewModel

private val CacheCoral = Color(0xFFEF4444)

@Composable
fun ToolsScreen(
    viewModel: ConverterViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val conversionOptions by viewModel.conversionOptions.collectAsStateWithLifecycle()
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val isClearingCache by viewModel.isClearingCache.collectAsStateWithLifecycle()
    val lastCacheDelta by viewModel.lastCacheDelta.collectAsStateWithLifecycle()
    val storagePermissionStatus by viewModel.storagePermissionStatus.collectAsStateWithLifecycle()

    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        viewModel.onLegacyStoragePermissionResult(granted)
    }

    val safOutputFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            viewModel.onSafOutputFolderGranted(treeUri)
        }
    }

    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()
    val isDark = LocalGlassPalette.current.isDark
    val goldChips = luxuryFilterChipColors(useGoldAccent = true)
    val blueChips = luxuryFilterChipColors(useGoldAccent = false)

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
        viewModel.refreshStoragePermissionState()
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

        // 1. Display & Theme Card
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
                        .iosTapBounce(pressedScale = 0.96f)
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
                            .iosTapBounce(pressedScale = 0.94f)
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
                            .iosTapBounce(pressedScale = 0.94f)
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
                            .iosTapBounce(pressedScale = 0.94f)
                            .testTag("theme_chip_system")
                    )
                }
            }
        }

        // 2. Conversion Defaults Card
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
                                modifier = Modifier
                                    .iosTapBounce(pressedScale = 0.94f)
                                    .testTag("setting_margin_${preset.name.lowercase()}")
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
                                modifier = Modifier
                                    .iosTapBounce(pressedScale = 0.94f)
                                    .testTag("setting_compression_${preset.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 3. Reader Preferences Card
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
                                    .iosTapBounce(pressedScale = 0.94f)
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
                                modifier = Modifier
                                    .iosTapBounce(pressedScale = 0.94f)
                                    .testTag("setting_page_turn_${mode.name.lowercase()}")
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
                                    .iosTapBounce(pressedScale = 0.94f)
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

        // 3b. Storage Permission & Output Folder Card
        val storageGranted = storagePermissionStatus.hasSharedStorageAccess
        val storageBadgeColor = if (storageGranted) GreenSuccess else gold
        val storageBadgeText = when {
            storagePermissionStatus.mode == com.serma.pubify.converter.DeviceStorageMode.SAF_FOLDER -> "Folder Granted"
            storagePermissionStatus.mode == com.serma.pubify.converter.DeviceStorageMode.SCOPED_DOWNLOADS -> "Allowed"
            storagePermissionStatus.mode == com.serma.pubify.converter.DeviceStorageMode.APP_ONLY -> "App Only"
            else -> "Not Granted"
        }

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("storage_permission_card"),
            shape = RoundedCornerShape(22.dp),
            accentTint = blue
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        SettingsSectionHeader(
                            icon = Icons.Default.FolderOpen,
                            title = "Storage Permission",
                            subtitle = "Where converted EPUBs are saved",
                            accentColor = blue,
                            titleColor = gold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = storageBadgeColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, storageBadgeColor.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("storage_permission_status_badge")
                    ) {
                        Text(
                            text = storageBadgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = storageBadgeColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = blue.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, blue.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Save Destination",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = storagePermissionStatus.displayLocation,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("storage_destination_text")
                        )
                    }
                }

                Button(
                    onClick = {
                        if (storagePermissionStatus.isLegacyRuntimePermissionRequired) {
                            legacyStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            safOutputFolderLauncher.launch(null)
                        }
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = Color(0xFF0A1020)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .iosTapBounce(pressedScale = 0.96f)
                        .testTag("settings_grant_storage_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (storagePermissionStatus.isLegacyRuntimePermissionRequired) {
                            if (storageGranted) "Storage Permission Granted" else "Request Storage Permission"
                        } else {
                            if (storagePermissionStatus.mode == com.serma.pubify.converter.DeviceStorageMode.SAF_FOLDER) {
                                "Change Authorized Folder"
                            } else {
                                "Grant Folder Access"
                            }
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (!storagePermissionStatus.isLegacyRuntimePermissionRequired &&
                    storagePermissionStatus.mode != com.serma.pubify.converter.DeviceStorageMode.SCOPED_DOWNLOADS
                ) {
                    OutlinedButton(
                        onClick = { viewModel.onConfirmScopedDownloads() },
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, blue.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .iosTapBounce(pressedScale = 0.96f)
                            .testTag("settings_use_downloads_button")
                    ) {
                        Text(
                            text = "Use Downloads/Pubify",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = gold
                        )
                    }
                }
            }
        }

        // 4. Clean Clear Cache Card
        val isCacheClean = cacheSizeBytes <= 0L
        val cacheStatusColor by animateColorAsState(
            targetValue = when {
                isCacheClean -> GreenSuccess
                cacheSizeBytes > 25L * 1024L * 1024L -> CacheCoral
                else -> gold
            },
            animationSpec = IosMotion.silkyTween(320),
            label = "cache_status_color"
        )
        val rawFillFraction = when {
            isCacheClean -> 0f
            else -> (cacheSizeBytes.toFloat() / (50f * 1024f * 1024f)).coerceIn(0.12f, 1f)
        }
        val animatedFillFraction by animateFloatAsState(
            targetValue = rawFillFraction,
            animationSpec = IosMotion.fluidSpring(dampingRatio = 0.84f, stiffness = 340f),
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

                AnimatedVisibility(
                    visible = lastCacheDelta != null,
                    enter = fadeIn(IosMotion.silkyTween(250)) + expandVertically(
                        animationSpec = IosMotion.fluidSpring(dampingRatio = 0.85f, stiffness = 400f)
                    )
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
                        .iosTapBounce(pressedScale = 0.96f, enabled = !isClearingCache)
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

        // 5. About & Privacy Architecture Card
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
                        text = "About Pubify",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Offline-First & Private • Zero Account Required • On-Device EPUB Engine • Unlimited Conversions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(92.dp)
        )
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
            modifier = Modifier
                .iosTapBounce(pressedScale = 0.93f)
                .testTag(testTag)
        )
    }
}
