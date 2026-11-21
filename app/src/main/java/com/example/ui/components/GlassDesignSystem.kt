package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Unified Editorial Luxury Gold & Crystal Blue Palette (low-saturation pale champagne-gold)
val LuxuryGold = Color(0xFFC6B89D)
val LuxuryGoldLight = Color(0xFF8E7B58)
val LuxuryGoldDeep = Color(0xFF96876B)
val CrystalBlue = Color(0xFF38BDF8)
val CrystalBlueLight = Color(0xFF0284C7)
val RoyalSapphire = Color(0xFF2563EB)

@Composable
fun luxuryGoldColor(): Color = if (LocalGlassPalette.current.isDark) LuxuryGold else LuxuryGoldLight

@Composable
fun luxuryBlueColor(): Color = if (LocalGlassPalette.current.isDark) CrystalBlue else CrystalBlueLight

@Composable
fun luxuryGoldBlueBorderBrush(alpha: Float = 0.72f): Brush {
    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()
    return Brush.linearGradient(
        colors = listOf(
            gold.copy(alpha = alpha * 0.82f),
            blue.copy(alpha = alpha * 0.68f),
            LuxuryGoldDeep.copy(alpha = alpha * 0.45f)
        )
    )
}

@Composable
fun luxuryFilterChipColors(useGoldAccent: Boolean = false): SelectableChipColors {
    val isDark = LocalGlassPalette.current.isDark
    val accent = if (useGoldAccent) luxuryGoldColor() else luxuryBlueColor()
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = accent.copy(alpha = if (isDark) 0.22f else 0.14f),
        selectedLabelColor = accent,
        selectedLeadingIconColor = accent,
        containerColor = LocalGlassPalette.current.subtleSurface.copy(alpha = if (isDark) 0.45f else 0.65f),
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun luxurySwitchColors(): SwitchColors {
    val isDark = LocalGlassPalette.current.isDark
    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()
    return SwitchDefaults.colors(
        checkedThumbColor = if (isDark) Color(0xFF071122) else Color.White,
        checkedTrackColor = gold,
        checkedBorderColor = gold,
        uncheckedThumbColor = blue.copy(alpha = 0.85f),
        uncheckedTrackColor = LocalGlassPalette.current.subtleSurface,
        uncheckedBorderColor = blue.copy(alpha = 0.45f)
    )
}

@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val backdropTop: Color,
    val backdropMid: Color,
    val backdropBottom: Color,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val orbTertiary: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val elevatedCardTop: Color,
    val elevatedCardBottom: Color,
    val subtleSurface: Color,
    val borderHighlight: Color,
    val borderMid: Color,
    val borderShadow: Color,
    val navPillTop: Color,
    val navPillBottom: Color,
    val navPillBorderTop: Color,
    val navPillBorderBottom: Color,
    val navActivePillBg: Color,
    val navActivePillBorder: Color,
    val navActiveContent: Color,
    val navInactiveContent: Color,
    val navShadowColor: Color,
    val dialogSurface: Color,
    val accentGlow: Color
)

val LiquidGlassPalette = GlassPalette(
    isDark = true,
    backdropTop = Color(0xFF040B1A),
    backdropMid = Color(0xFF091830),
    backdropBottom = Color(0xFF050A16),
    orbPrimary = Color(0x3D00E5FF),
    orbSecondary = Color(0x336366F1),
    orbTertiary = Color(0x2A14B8A6),
    cardTop = Color(0xAD152946),
    cardBottom = Color(0x880B182C),
    elevatedCardTop = Color(0xC4193254),
    elevatedCardBottom = Color(0x9E0F2038),
    subtleSurface = Color(0x66132744),
    borderHighlight = Color(0x66B8F4FF),
    borderMid = Color(0x3000E5FF),
    borderShadow = Color(0x1AFFFFFF),
    navPillTop = Color(0xE00E2038),
    navPillBottom = Color(0xEB071120),
    navPillBorderTop = Color(0x78A5F3FC),
    navPillBorderBottom = Color(0x2800E5FF),
    navActivePillBg = Color(0x3800E5FF),
    navActivePillBorder = Color(0x6600E5FF),
    navActiveContent = Color(0xFF5CEFFF),
    navInactiveContent = Color(0xFF94A3B8),
    navShadowColor = Color(0x99000914),
    dialogSurface = Color(0xF20D1B2E),
    accentGlow = Color(0xFF00E5FF)
)

val DarkGlassPalette = GlassPalette(
    isDark = true,
    backdropTop = Color(0xFF090D1A),
    backdropMid = Color(0xFF11182C),
    backdropBottom = Color(0xFF080C17),
    orbPrimary = Color(0x36818CF8),
    orbSecondary = Color(0x2E2DD4BF),
    orbTertiary = Color(0x26C084FC),
    cardTop = Color(0xAD1E293B),
    cardBottom = Color(0x8C111827),
    elevatedCardTop = Color(0xC2263449),
    elevatedCardBottom = Color(0xA6162033),
    subtleSurface = Color(0x661E293B),
    borderHighlight = Color(0x52FFFFFF),
    borderMid = Color(0x28A5B4FC),
    borderShadow = Color(0x14FFFFFF),
    navPillTop = Color(0xE0182235),
    navPillBottom = Color(0xEB0E1524),
    navPillBorderTop = Color(0x66E2E8F0),
    navPillBorderBottom = Color(0x24818CF8),
    navActivePillBg = Color(0x36818CF8),
    navActivePillBorder = Color(0x60A5B4FC),
    navActiveContent = Color(0xFFA5B4FC),
    navInactiveContent = Color(0xFF94A3B8),
    navShadowColor = Color(0x9903060E),
    dialogSurface = Color(0xF2151E30),
    accentGlow = Color(0xFF818CF8)
)

val LightGlassPalette = GlassPalette(
    isDark = false,
    backdropTop = Color(0xFFEBF3FF),
    backdropMid = Color(0xFFF3F0FF),
    backdropBottom = Color(0xFFE9F8FA),
    orbPrimary = Color(0x4560A5FA),
    orbSecondary = Color(0x3CA78BFA),
    orbTertiary = Color(0x382DD4BF),
    cardTop = Color(0xCCFFFFFF),
    cardBottom = Color(0x99F8FAFF),
    elevatedCardTop = Color(0xDEFFFFFF),
    elevatedCardBottom = Color(0xB8F1F5FF),
    subtleSurface = Color(0x80FFFFFF),
    borderHighlight = Color(0xF0FFFFFF),
    borderMid = Color(0x70CBD5E1),
    borderShadow = Color(0x386366F1),
    navPillTop = Color(0xE0FFFFFF),
    navPillBottom = Color(0xCCF1F5FF),
    navPillBorderTop = Color(0xF5FFFFFF),
    navPillBorderBottom = Color(0x55818CF8),
    navActivePillBg = Color(0x2B4F46E5),
    navActivePillBorder = Color(0x554F46E5),
    navActiveContent = Color(0xFF3730A3),
    navInactiveContent = Color(0xFF64748B),
    navShadowColor = Color(0x351E293B),
    dialogSurface = Color(0xF5F8FAFF),
    accentGlow = Color(0xFF4F46E5)
)

val LocalGlassPalette = staticCompositionLocalOf { LiquidGlassPalette }

/**
 * Ambient atmospheric aurora backdrop rendered via drawBehind so glassmorphic
 * cards and the floating pill navigation bar exhibit real translucent depth.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier = modifier.drawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.backdropTop,
                        palette.backdropMid,
                        palette.backdropBottom
                    )
                )
            )

            // Top-left primary aurora glow
            val r1 = size.width * 0.78f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbPrimary, Color.Transparent),
                    center = Offset(size.width * 0.14f, size.height * 0.10f),
                    radius = r1
                ),
                radius = r1,
                center = Offset(size.width * 0.14f, size.height * 0.10f)
            )

            // Mid-right secondary aurora glow
            val r2 = size.width * 0.72f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbSecondary, Color.Transparent),
                    center = Offset(size.width * 0.90f, size.height * 0.44f),
                    radius = r2
                ),
                radius = r2,
                center = Offset(size.width * 0.90f, size.height * 0.44f)
            )

            // Bottom-left subtle teal/indigo glow behind floating pill nav bar
            val r3 = size.width * 0.68f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbTertiary, Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.90f),
                    radius = r3
                ),
                radius = r3,
                center = Offset(size.width * 0.25f, size.height * 0.90f)
            )
        }
    )
}

/**
 * Modifier that applies a frosted glass translucent gradient fill with a crisp
 * directional specular rim border (luminous top-left catchlight to soft bottom-right edge).
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    elevated: Boolean = false,
    accentTint: Color? = null,
    borderWidth: Dp = 1.dp
): Modifier {
    val palette = LocalGlassPalette.current
    val baseTop = if (elevated) palette.elevatedCardTop else palette.cardTop
    val topColor = if (accentTint != null) {
        accentTint.copy(alpha = if (palette.isDark) 0.055f else 0.05f).compositeOver(baseTop)
    } else {
        baseTop
    }
    val bottomColor = if (elevated) palette.elevatedCardBottom else palette.cardBottom
    val rimColors = if (accentTint != null) {
        listOf(
            palette.borderHighlight.copy(alpha = if (palette.isDark) 0.24f else 0.70f),
            accentTint.copy(alpha = if (palette.isDark) 0.28f else 0.22f),
            palette.borderShadow
        )
    } else {
        listOf(
            palette.borderHighlight,
            palette.borderMid,
            palette.borderShadow
        )
    }

    return this
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(topColor, bottomColor),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ),
            shape = shape
        )
        .border(
            width = borderWidth,
            brush = Brush.linearGradient(
                colors = rimColors,
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ),
            shape = shape
        )
}

/**
 * Reusable Glassmorphism card container with translucent frosted gradient fill
 * and directional specular rim border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevated: Boolean = false,
    accentTint: Color? = null,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .glassSurface(
            shape = shape,
            elevated = elevated,
            accentTint = accentTint,
            borderWidth = borderWidth
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier = baseModifier,
            content = content
        )
    }
}

data class GlassNavTabItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Floating Pill Geometry bottom navigation bar crafted with multi-layer glassmorphism:
 * - Capsule/pill silhouette (`RoundedCornerShape(100.dp)`)
 * - Proper `WindowInsets.navigationBars` handling + floating margins
 * - Luminous top-left-to-bottom-right specular border & ambient shadow
 * - Spring-animated frosted active capsule indicator
 * - Minimum 48dp touch targets & accessibility semantics
 */
@Composable
fun FloatingGlassPillNavBar(
    items: List<GlassNavTabItem>,
    selectedRoute: String,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val gold = luxuryGoldColor()
    val pillShape = RoundedCornerShape(100.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = pillShape,
                    ambientColor = palette.navShadowColor,
                    spotColor = gold.copy(alpha = if (palette.isDark) 0.30f else 0.22f)
                )
                .clip(pillShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(palette.navPillTop, palette.navPillBottom)
                    ),
                    shape = pillShape
                )
                .border(
                    width = 1.35.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            gold.copy(alpha = if (palette.isDark) 0.92f else 0.85f),
                            LuxuryGoldDeep.copy(alpha = if (palette.isDark) 0.65f else 0.55f),
                            gold.copy(alpha = if (palette.isDark) 0.50f else 0.40f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = pillShape
                )
                .testTag("bottom_nav_bar"),
            shape = pillShape,
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { tab ->
                    val isSelected = tab.route == selectedRoute
                    FloatingPillNavItem(
                        tab = tab,
                        isSelected = isSelected,
                        palette = palette,
                        goldColor = gold,
                        onClick = { onSelectRoute(tab.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingPillNavItem(
    tab: GlassNavTabItem,
    isSelected: Boolean,
    palette: GlassPalette,
    goldColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemPillShape = RoundedCornerShape(100.dp)
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            goldColor.copy(alpha = if (palette.isDark) 0.20f else 0.16f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_pill_bg_${tab.route}"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            goldColor.copy(alpha = if (palette.isDark) 0.72f else 0.60f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_pill_border_${tab.route}"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) goldColor else palette.navInactiveContent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_pill_content_${tab.route}"
    )
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 22.dp else 20.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "nav_icon_size_${tab.route}"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .minimumInteractiveComponentSize()
            .clip(itemPillShape)
            .background(color = bgColor, shape = itemPillShape)
            .border(width = 1.dp, color = borderColor, shape = itemPillShape)
            .semantics { selected = isSelected }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = goldColor),
                role = Role.Tab,
                onClick = onClick
            )
            .testTag("nav_tab_${tab.route}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.title,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tab.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.1.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
