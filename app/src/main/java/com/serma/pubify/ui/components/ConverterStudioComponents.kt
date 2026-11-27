package com.serma.pubify.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.serma.pubify.R
import com.serma.pubify.data.model.ConvertedBook
import com.serma.pubify.reader.ThemeMode
import java.io.File

private val CrystalCyan = CrystalBlue

/**
 * Editorial luxury studio header featuring the custom gold-and-crystal app emblem
 * and glassmorphic theme switcher.
 */
@Composable
fun StudioBrandHeader(
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = themeMode == ThemeMode.DARK || themeMode == ThemeMode.LIQUID_GLASS
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Luxury app emblem squircle with champagne-gold specular rim
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = LuxuryGold.copy(alpha = 0.35f),
                        spotColor = LuxuryGold.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.25.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                LuxuryGold.copy(alpha = 0.9f),
                                CrystalCyan.copy(alpha = 0.5f),
                                LuxuryGoldDeep.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo_geometric_1795194079814),
                    contentDescription = "Pubify Studio Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = "PUBIFY",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.4.sp
                ),
                color = if (isDarkTheme) LuxuryGold else MaterialTheme.colorScheme.primary
            )
        }

        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .size(48.dp)
                .glassSurface(shape = CircleShape, elevated = true)
                .testTag("converter_dark_mode_toggle")
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Dark Mode",
                tint = if (isDarkTheme) LuxuryGold else MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Floating glassmorphic segmented pill switcher with an iOS-style GPU sliding thumb.
 */
@Composable
fun GlassSegmentedModePill(
    isMultiSelectMode: Boolean,
    batchCount: Int,
    onSelectSingle: () -> Unit,
    onSelectBatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val outerShape = RoundedCornerShape(100.dp)
    val thumbProgress = animateFloatAsState(
        targetValue = if (isMultiSelectMode) 1f else 0f,
        animationSpec = IosMotion.fluidSpring(dampingRatio = 0.80f, stiffness = 420f),
        label = "segment_sliding_thumb"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .glassSurface(shape = outerShape, elevated = false)
            .padding(4.dp)
            .testTag("converter_mode_selector")
    ) {
        val halfWidth = maxWidth / 2f

        // Sliding frosted pill thumb rendered purely via GPU translationX
        Box(
            modifier = Modifier
                .width(halfWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = thumbProgress.value * halfWidth.toPx()
                }
                .clip(outerShape)
                .background(palette.navActivePillBg, outerShape)
                .border(1.dp, palette.navActivePillBorder, outerShape)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassSegmentTab(
                label = "Single File",
                icon = Icons.Default.Description,
                selected = !isMultiSelectMode,
                palette = palette,
                onClick = onSelectSingle,
                modifier = Modifier
                    .weight(1f)
                    .testTag("mode_chip_single")
            )
            GlassSegmentTab(
                label = if (batchCount > 0) "Batch Queue ($batchCount)" else "Batch Studio",
                icon = Icons.Default.Queue,
                selected = isMultiSelectMode,
                palette = palette,
                onClick = onSelectBatch,
                modifier = Modifier
                    .weight(1f)
                    .testTag("mode_chip_multi_select")
            )
        }
    }
}

@Composable
private fun GlassSegmentTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    palette: GlassPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(100.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (selected) palette.navActiveContent else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = IosMotion.silkyTween(durationMillis = 220),
        label = "segment_content_$label"
    )

    Row(
        modifier = modifier
            .fillMaxHeight()
            .minimumInteractiveComponentSize()
            .iosPressScale(interactionSource = interactionSource, pressedScale = 0.96f)
            .clip(pillShape)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = palette.accentGlow),
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Creative Hero Transformation Studio Card featuring bespoke glass-and-gold artwork,
 * animated PDF -> EPUB morph badge, and tactile frosted actions.
 */
@Composable
fun CreativeHeroDropzoneCard(
    isBatchMode: Boolean,
    onPrimaryAction: () -> Unit,
    onSecondaryBatchAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val cardShape = RoundedCornerShape(28.dp)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = cardShape,
                ambientColor = palette.navShadowColor,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            )
            .testTag(if (isBatchMode) "multi_pdf_dropzone_card" else "pdf_dropzone_card"),
        shape = cardShape,
        elevated = true,
        accentTint = if (palette.isDark) LuxuryGold else MaterialTheme.colorScheme.primary,
        borderWidth = 1.25.dp,
        onClick = onPrimaryAction
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Compact cinematic artwork showcase viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_converter_studio_hero_1795192892897),
                    contentDescription = "PDF transforming into luminous EPUB book",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Soft atmospheric scrim blending artwork into the glass card body
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x33040B1A),
                                    Color(0x1A040B1A),
                                    Color(0xD8071122)
                                )
                            )
                        )
                )

                // Bottom-center floating PDF -> EPUB live morph pill (isolated draw-phase glow = 0 card recompositions)
                GlowingMorphBadgePill(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )
            }

            // Glass Atelier Body & CTAs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isBatchMode) "Batch PDF Converter" else "Select PDF Document",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isBatchMode) {
                        "Queue multiple PDFs to craft structured, image-rich EPUB books in one seamless run."
                    } else {
                        "Tap anywhere to import a PDF and transform it into a reflowable, chapter-matched eBook."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (isBatchMode) {
                    Button(
                        onClick = onPrimaryAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .iosTapBounce(pressedScale = 0.965f)
                            .testTag("select_multiple_pdfs_button"),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Queue Multiple PDFs",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPrimaryAction,
                            modifier = Modifier
                                .weight(1.25f)
                                .height(50.dp)
                                .testTag("select_pdf_button"),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select PDF",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (onSecondaryBatchAction != null) {
                            OutlinedButton(
                                onClick = onSecondaryBatchAction,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("multi_select_quick_button"),
                                shape = RoundedCornerShape(100.dp),
                                border = BorderStroke(1.dp, palette.borderHighlight)
                            ) {
                                Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Batch",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean, uncluttered two-box highlight strip communicating the app's two core promises:
 * 1) Totally Unlimited Conversions
 * 2) 100% Offline & Private
 */
@Composable
fun StudioPromiseCards(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_promise_cards"),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PromiseGlassTile(
            icon = Icons.Default.AllInclusive,
            title = "Unlimited",
            subtitle = "Totally unlimited conversions",
            accent = LuxuryGold,
            modifier = Modifier
                .weight(1f)
                .testTag("promise_card_unlimited")
        )
        PromiseGlassTile(
            icon = Icons.Default.CloudOff,
            title = "100% Offline",
            subtitle = "Runs totally offline on device",
            accent = CrystalCyan,
            modifier = Modifier
                .weight(1f)
                .testTag("promise_card_offline")
        )
    }
}

@Composable
private fun PromiseGlassTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        accentTint = accent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact "Recently Crafted" shelf shown on the idle Converter screen when the user
 * already has converted books, allowing one-tap launch into the Reader.
 */
@Composable
fun RecentAtelierShelf(
    books: List<ConvertedBook>,
    onOpenBook: (ConvertedBook) -> Unit,
    onViewLibrary: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (books.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Crafted",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (onViewLibrary != null) {
                TextButton(
                    onClick = onViewLibrary,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(books.take(4), key = { it.id }) { book ->
                GlassCard(
                    onClick = { onOpenBook(book) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.width(196.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (book.coverImagePath != null && File(book.coverImagePath).exists()) {
                                AsyncImage(
                                    model = File(book.coverImagePath),
                                    contentDescription = book.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${book.chapterCount} ch • ${book.pageCount} pg",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Isolated PDF -> EPUB glowing pill badge. Reads its animated alpha exclusively inside
 * `drawBehind` so the parent hero card and artwork suffer zero per-frame recompositions.
 */
@Composable
private fun GlowingMorphBadgePill(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val glowAlphaState = infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xCC091529), RoundedCornerShape(100.dp))
            .drawBehind {
                val alpha = glowAlphaState.value
                val strokePx = 1.dp.toPx()
                val radius = size.height / 2f
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            LuxuryGold.copy(alpha = alpha),
                            CrystalCyan.copy(alpha = alpha)
                        )
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = strokePx)
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = LuxuryGold,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "PDF->EPUB",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                ),
                color = Color(0xFFF8FAFC)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = CrystalCyan,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
