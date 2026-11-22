package com.serma.pubify

import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.test.junit4.createComposeRule
import com.serma.pubify.reader.ThemeMode
import com.serma.pubify.ui.components.GlassCard
import com.serma.pubify.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class DarkModeContrastTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dark and liquid glass GlassCard provide light LocalContentColor`() {
        var darkContentLuminance = -1f
        var liquidGlassContentLuminance = -1f

        composeTestRule.setContent {
            MyApplicationTheme(
                themeMode = ThemeMode.DARK,
                darkTheme = true,
                dynamicColor = false
            ) {
                GlassCard {
                    val c = LocalContentColor.current
                    darkContentLuminance = (0.299f * c.red + 0.587f * c.green + 0.114f * c.blue)
                }
            }
            MyApplicationTheme(
                themeMode = ThemeMode.LIQUID_GLASS,
                darkTheme = true,
                dynamicColor = false
            ) {
                GlassCard {
                    val c = LocalContentColor.current
                    liquidGlassContentLuminance = (0.299f * c.red + 0.587f * c.green + 0.114f * c.blue)
                }
            }
        }

        composeTestRule.waitForIdle()
        assertTrue(
            "Dark mode GlassCard content color must be light, got luminance=$darkContentLuminance",
            darkContentLuminance > 0.75f
        )
        assertTrue(
            "Liquid Glass GlassCard content color must be light, got luminance=$liquidGlassContentLuminance",
            liquidGlassContentLuminance > 0.75f
        )
    }
}
