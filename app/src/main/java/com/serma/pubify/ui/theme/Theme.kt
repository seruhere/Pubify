package com.serma.pubify.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.serma.pubify.ui.components.DarkGlassPalette
import com.serma.pubify.ui.components.LightGlassPalette
import com.serma.pubify.ui.components.LiquidGlassPalette
import com.serma.pubify.ui.components.LocalGlassPalette

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkTeal,
    onSecondary = DarkOnTeal,
    secondaryContainer = DarkTealContainer,
    onSecondaryContainer = Color.White,
    surface = Color(0xCC162033),
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0x99202D44),
    onSurfaceVariant = DarkOnSurfaceVariant,
    background = Color(0xFF090D1A),
    onBackground = DarkOnSurface,
    outline = Color(0x55818CF8),
    outlineVariant = Color(0x2E94A3B8)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = IndigoOnPrimary,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoOnContainer,
    secondary = TealAccent,
    onSecondary = TealOnAccent,
    secondaryContainer = TealContainer,
    onSecondaryContainer = TealOnContainer,
    surface = Color(0xCCFFFFFF),
    onSurface = LightOnSurface,
    surfaceVariant = Color(0x99F1F5FF),
    onSurfaceVariant = LightOnSurfaceVariant,
    background = Color(0xFFEBF3FF),
    onBackground = LightOnSurface,
    outline = Color(0x8894A3B8),
    outlineVariant = Color(0x55CBD5E1)
  )

private val LiquidGlassColorScheme =
  darkColorScheme(
    primary = Color(0xFF00E5FF), // Luminous liquid cyan
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF80F2FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF003549),
    secondaryContainer = Color(0xFF004D68),
    onSecondaryContainer = Color(0xFFBBE9FF),
    surface = Color(0xB80E1E36), // Frosted glass crystalline surface
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0x8C172D4A), // Translucent frosted container
    onSurfaceVariant = Color(0xFFB8C9DC),
    background = Color(0xFF040B1A), // Deep crystalline abyss
    onBackground = Color(0xFFF8FAFC),
    outline = Color(0x4D00E5FF), // Luminous liquid cyan glass border
    outlineVariant = Color(0x2800E5FF)
  )

@Composable
fun MyApplicationTheme(
  themeMode: com.serma.pubify.reader.ThemeMode = com.serma.pubify.reader.ThemeMode.SYSTEM,
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      themeMode == com.serma.pubify.reader.ThemeMode.LIQUID_GLASS -> LiquidGlassColorScheme
      themeMode == com.serma.pubify.reader.ThemeMode.LIGHT -> LightColorScheme
      themeMode == com.serma.pubify.reader.ThemeMode.DARK -> DarkColorScheme
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val glassPalette =
    when {
      themeMode == com.serma.pubify.reader.ThemeMode.LIQUID_GLASS -> LiquidGlassPalette
      themeMode == com.serma.pubify.reader.ThemeMode.LIGHT -> LightGlassPalette
      themeMode == com.serma.pubify.reader.ThemeMode.DARK -> DarkGlassPalette
      darkTheme -> DarkGlassPalette
      else -> LightGlassPalette
    }

  CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
