package com.serma.pubify

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serma.pubify.reader.ThemeMode
import com.serma.pubify.ui.components.CrystalBlue
import com.serma.pubify.ui.components.FloatingGlassPillNavBar
import com.serma.pubify.ui.components.GlassBackdrop
import com.serma.pubify.ui.components.GlassNavTabItem
import com.serma.pubify.ui.components.IosMotion
import com.serma.pubify.ui.components.LuxuryGold
import com.serma.pubify.ui.components.LuxuryGoldDeep
import com.serma.pubify.ui.screens.ConverterScreen
import com.serma.pubify.ui.screens.LibraryScreen
import com.serma.pubify.ui.screens.ReaderScreen
import com.serma.pubify.ui.screens.ReadingStatsScreen
import com.serma.pubify.ui.screens.ToolsScreen
import com.serma.pubify.ui.theme.MyApplicationTheme
import com.serma.pubify.ui.viewmodel.ConverterViewModel
import kotlinx.coroutines.delay
import java.io.File

enum class ScreenTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CONVERT("convert", "Convert", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    LIBRARY("library", "My Books", Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark),
    READER("reader", "Reader", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    STATS("stats", "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    TOOLS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private var activeViewModel: ConverterViewModel? = null

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (activeViewModel?.onVolumeKeyPressed(keyCode) == true) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferHighestDisplayRefreshRate()

        setContent {
            val viewModel: ConverterViewModel = viewModel()
            activeViewModel = viewModel
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemInDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.LIQUID_GLASS -> true
            }

            var showLaunchLoadingScreen by rememberSaveable { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                if (showLaunchLoadingScreen) {
                    delay(1050L)
                    showLaunchLoadingScreen = false
                }
            }

            val contentRevealScale = animateFloatAsState(
                targetValue = if (showLaunchLoadingScreen) 0.955f else 1f,
                animationSpec = IosMotion.fluidSpring(dampingRatio = 0.86f, stiffness = 310f),
                label = "app_open_scale"
            )
            val contentRevealAlpha = animateFloatAsState(
                targetValue = if (showLaunchLoadingScreen) 0.35f else 1f,
                animationSpec = IosMotion.silkyTween(durationMillis = 420),
                label = "app_open_alpha"
            )

            MyApplicationTheme(
                themeMode = themeMode,
                darkTheme = isDark,
                dynamicColor = (themeMode == ThemeMode.SYSTEM)
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainAppContent(
                            viewModel = viewModel,
                            onShareEpub = { filePath, title -> shareEpub(filePath, title) },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val s = contentRevealScale.value
                                    scaleX = s
                                    scaleY = s
                                    alpha = contentRevealAlpha.value
                                }
                        )

                        AnimatedVisibility(
                            visible = showLaunchLoadingScreen,
                            enter = fadeIn(tween(100)),
                            exit = fadeOut(IosMotion.silkyTween(durationMillis = 400)) +
                                scaleOut(
                                    targetScale = 1.07f,
                                    animationSpec = IosMotion.silkyTween(durationMillis = 400)
                                ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AppLaunchLoadingScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }

    private fun preferHighestDisplayRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val activeDisplay = display ?: return
                val currentMode = activeDisplay.mode
                val bestMode = activeDisplay.supportedModes
                    .filter {
                        it.physicalWidth == currentMode.physicalWidth &&
                            it.physicalHeight == currentMode.physicalHeight
                    }
                    .maxByOrNull { it.refreshRate }
                if (bestMode != null && bestMode.modeId != currentMode.modeId) {
                    val params = window.attributes
                    params.preferredDisplayModeId = bestMode.modeId
                    window.attributes = params
                }
            }
        }
    }

    private fun shareEpub(filePath: String, title: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(this, "EPUB file not found", Toast.LENGTH_SHORT).show()
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/epub+zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Here is '$title' converted with Pubify.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share '$title' via"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: ConverterViewModel,
    onShareEpub: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(ScreenTab.CONVERT) }
    val cacheMessage by viewModel.cacheOperationMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(cacheMessage) {
        cacheMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    BackHandler(enabled = selectedTab != ScreenTab.CONVERT) {
        if (selectedTab == ScreenTab.READER || selectedTab == ScreenTab.STATS) {
            selectedTab = ScreenTab.LIBRARY
        } else {
            selectedTab = ScreenTab.CONVERT
        }
    }

    val isReaderBarsVisible by viewModel.isReaderBarsVisible.collectAsStateWithLifecycle()
    val activeReaderBook by viewModel.activeReaderBook.collectAsStateWithLifecycle()
    val showBottomBar = selectedTab != ScreenTab.READER || activeReaderBook == null || isReaderBarsVisible

    val navItems = remember {
        ScreenTab.entries.map { tab ->
            GlassNavTabItem(
                route = tab.route,
                title = tab.title,
                selectedIcon = tab.selectedIcon,
                unselectedIcon = tab.unselectedIcon
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GlassBackdrop(modifier = Modifier.fillMaxSize())

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val movingForward = targetState.ordinal >= initialState.ordinal
                val enterOffset: (Int) -> Int = { fullWidth ->
                    val shift = (fullWidth * 0.055f).toInt()
                    if (movingForward) shift else -shift
                }
                val exitOffset: (Int) -> Int = { fullWidth ->
                    val shift = (fullWidth * 0.035f).toInt()
                    if (movingForward) -shift else shift
                }
                (fadeIn(animationSpec = IosMotion.silkyTween(250)) +
                    slideInHorizontally(
                        animationSpec = IosMotion.offsetSpring(dampingRatio = 0.86f, stiffness = 410f),
                        initialOffsetX = enterOffset
                    ) +
                    scaleIn(
                        initialScale = 0.982f,
                        animationSpec = IosMotion.silkyTween(250)
                    )) togetherWith
                    (fadeOut(animationSpec = IosMotion.silkyTween(175)) +
                        slideOutHorizontally(
                            animationSpec = IosMotion.offsetSpring(dampingRatio = 0.90f, stiffness = 440f),
                            targetOffsetX = exitOffset
                        ))
            },
            label = "ios_tab_transition",
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                ScreenTab.CONVERT -> ConverterScreen(
                    viewModel = viewModel,
                    onNavigateToReader = { selectedTab = ScreenTab.READER },
                    onShareEpub = onShareEpub,
                    onNavigateToLibrary = { selectedTab = ScreenTab.LIBRARY },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
                ScreenTab.LIBRARY -> LibraryScreen(
                    viewModel = viewModel,
                    onOpenReader = { book ->
                        viewModel.openBookInReader(book)
                        selectedTab = ScreenTab.READER
                    },
                    onShareEpub = onShareEpub,
                    onNavigateToConvert = { selectedTab = ScreenTab.CONVERT },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
                ScreenTab.READER -> ReaderScreen(
                    viewModel = viewModel,
                    onNavigateToLibrary = { selectedTab = ScreenTab.LIBRARY },
                    onShareEpub = onShareEpub,
                    modifier = Modifier.fillMaxSize()
                )
                ScreenTab.STATS -> ReadingStatsScreen(
                    viewModel = viewModel,
                    onOpenReader = { book ->
                        viewModel.openBookInReader(book)
                        selectedTab = ScreenTab.READER
                    },
                    onNavigateToLibrary = { selectedTab = ScreenTab.LIBRARY },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
                ScreenTab.TOOLS -> ToolsScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically(
                animationSpec = IosMotion.offsetSpring(dampingRatio = 0.84f, stiffness = 380f),
                initialOffsetY = { it + 120 }
            ) + fadeIn(IosMotion.silkyTween(220)),
            exit = slideOutVertically(
                animationSpec = IosMotion.offsetSpring(dampingRatio = 0.90f, stiffness = 420f),
                targetOffsetY = { it + 120 }
            ) + fadeOut(IosMotion.silkyTween(170)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingGlassPillNavBar(
                items = navItems,
                selectedRoute = selectedTab.route,
                onSelectRoute = { route ->
                    ScreenTab.entries.firstOrNull { it.route == route }?.let { selectedTab = it }
                }
            )
        }
    }
}

@Composable
private fun AppLaunchLoadingScreen(
    modifier: Modifier = Modifier
) {
    var logoRevealed by remember { mutableStateOf(false) }
    var textRevealed by remember { mutableStateOf(false) }
    var progressStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoRevealed = true
        delay(110L)
        textRevealed = true
        delay(70L)
        progressStarted = true
    }

    val auraScaleState = animateFloatAsState(
        targetValue = if (logoRevealed) 1.08f else 0.72f,
        animationSpec = IosMotion.silkyTween(durationMillis = 900),
        label = "launch_aura_scale"
    )
    val auraAlphaState = animateFloatAsState(
        targetValue = if (logoRevealed) 1f else 0f,
        animationSpec = IosMotion.silkyTween(durationMillis = 650),
        label = "launch_aura_alpha"
    )
    val logoScaleState = animateFloatAsState(
        targetValue = if (logoRevealed) 1f else 0.78f,
        animationSpec = IosMotion.fluidSpring(dampingRatio = 0.74f, stiffness = 270f),
        label = "launch_logo_scale"
    )
    val logoAlphaState = animateFloatAsState(
        targetValue = if (logoRevealed) 1f else 0f,
        animationSpec = IosMotion.silkyTween(durationMillis = 360),
        label = "launch_logo_alpha"
    )
    val textOffsetState = animateFloatAsState(
        targetValue = if (textRevealed) 0f else 28f,
        animationSpec = IosMotion.fluidSpring(dampingRatio = 0.84f, stiffness = 340f),
        label = "launch_text_offset"
    )
    val textAlphaState = animateFloatAsState(
        targetValue = if (textRevealed) 1f else 0f,
        animationSpec = IosMotion.silkyTween(durationMillis = 380),
        label = "launch_text_alpha"
    )
    val progressState = animateFloatAsState(
        targetValue = if (progressStarted) 1f else 0.04f,
        animationSpec = IosMotion.silkyTween(durationMillis = 800),
        label = "launch_progress"
    )

    val logoBorderBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                LuxuryGold,
                CrystalBlue.copy(alpha = 0.85f),
                LuxuryGoldDeep
            )
        )
    }
    val progressBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                LuxuryGoldDeep,
                LuxuryGold,
                CrystalBlue.copy(alpha = 0.90f)
            )
        )
    }

    Box(
        modifier = modifier
            .drawWithCache {
                val bgBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040B1A),
                        Color(0xFF091830),
                        Color(0xFF050A16)
                    )
                )
                val goldHalo = Brush.radialGradient(
                    colors = listOf(
                        LuxuryGold.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.44f),
                    radius = size.minDimension * 0.62f
                )
                val blueHalo = Brush.radialGradient(
                    colors = listOf(
                        CrystalBlue.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.56f),
                    radius = size.minDimension * 0.55f
                )
                onDrawBehind {
                    drawRect(brush = bgBrush)
                    drawRect(brush = goldHalo, alpha = auraAlphaState.value)
                    drawRect(brush = blueHalo, alpha = auraAlphaState.value)
                }
            }
            .testTag("app_launch_loading_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(106.dp)
                    .graphicsLayer {
                        val s = logoScaleState.value * (0.97f + 0.03f * auraScaleState.value)
                        scaleX = s
                        scaleY = s
                        alpha = logoAlphaState.value
                    }
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = LuxuryGold.copy(alpha = 0.45f),
                        spotColor = LuxuryGold.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 1.6.dp,
                        brush = logoBorderBrush,
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo_geometric_1795194079814),
                    contentDescription = stringResource(id = R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.graphicsLayer {
                    translationY = textOffsetState.value
                    alpha = textAlphaState.value
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = 1.6.sp
                    ),
                    color = LuxuryGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("launch_screen_app_name")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "PDF to EPUB  •  Offline Reader",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.1.sp
                    ),
                    color = CrystalBlue.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                Box(
                    modifier = Modifier
                        .width(164.dp)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(CrystalBlue.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = progressState.value.coerceIn(0.02f, 1f)
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            }
                            .clip(RoundedCornerShape(100.dp))
                            .background(progressBrush)
                    )
                }
            }
        }
    }
}
