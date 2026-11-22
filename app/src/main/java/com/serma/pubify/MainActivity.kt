package com.serma.pubify

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.runtime.key
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.serma.pubify.data.firebase.FirebaseSyncRepository
import com.serma.pubify.data.firebase.authStateFlow
import com.serma.pubify.reader.ThemeMode
import com.serma.pubify.ui.components.CrystalBlue
import com.serma.pubify.ui.components.FloatingGlassPillNavBar
import com.serma.pubify.ui.components.GlassBackdrop
import com.serma.pubify.ui.components.GlassNavTabItem
import com.serma.pubify.ui.components.LuxuryGold
import com.serma.pubify.ui.components.LuxuryGoldDeep
import com.serma.pubify.ui.screens.AuthScreen
import com.serma.pubify.ui.screens.ConverterScreen
import com.serma.pubify.ui.screens.LibraryScreen
import com.serma.pubify.ui.screens.ReaderScreen
import com.serma.pubify.ui.screens.ReadingStatsScreen
import com.serma.pubify.ui.screens.ToolsScreen
import com.serma.pubify.ui.theme.MyApplicationTheme
import com.serma.pubify.ui.viewmodel.CloudSyncViewModel
import com.serma.pubify.ui.viewmodel.ConverterViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
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

            val currentUser by remember { Firebase.auth.authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

            var showLaunchLoadingScreen by rememberSaveable { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                if (showLaunchLoadingScreen) {
                    delay(1150L)
                    showLaunchLoadingScreen = false
                }
            }

            MyApplicationTheme(
                themeMode = themeMode,
                darkTheme = isDark,
                dynamicColor = (themeMode == ThemeMode.SYSTEM)
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val signedInUser = currentUser
                        val isGuestMode by viewModel.isGuestMode.collectAsStateWithLifecycle()

                        if (signedInUser == null && !isGuestMode) {
                            AuthScreen(
                                onSkipSignIn = { viewModel.setGuestMode(true) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (signedInUser != null) {
                            key(signedInUser.uid) {
                                val cloudSyncViewModel: CloudSyncViewModel = viewModel(
                                    key = signedInUser.uid,
                                    factory = viewModelFactory {
                                        initializer {
                                            val app = checkNotNull(this[APPLICATION_KEY])
                                            val db = FirebaseFirestore.getInstance(
                                                app.getString(R.string.firestore_database_id)
                                            )
                                            CloudSyncViewModel(
                                                repository = FirebaseSyncRepository(db),
                                                currentUserId = signedInUser.uid
                                            )
                                        }
                                    }
                                )

                                val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
                                LaunchedEffect(signedInUser.uid, allBooks) {
                                    cloudSyncViewModel.syncBooksSilently(
                                        displayName = signedInUser.displayName ?: "Pubify Reader",
                                        email = signedInUser.email,
                                        photoUrl = signedInUser.photoUrl?.toString(),
                                        books = allBooks
                                    )
                                }

                                MainAppContent(
                                    viewModel = viewModel,
                                    cloudSyncViewModel = cloudSyncViewModel,
                                    currentUser = signedInUser,
                                    onShareEpub = { filePath, title -> shareEpub(filePath, title) }
                                )
                            }
                        } else {
                            MainAppContent(
                                viewModel = viewModel,
                                cloudSyncViewModel = null,
                                currentUser = null,
                                onShareEpub = { filePath, title -> shareEpub(filePath, title) }
                            )
                        }

                        // Branded Launch Loading Screen showing App Name & Geometric Logo
                        AnimatedVisibility(
                            visible = showLaunchLoadingScreen,
                            enter = fadeIn(tween(120)),
                            exit = fadeOut(tween(420, easing = FastOutSlowInEasing)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AppLaunchLoadingScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
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
    cloudSyncViewModel: CloudSyncViewModel? = null,
    currentUser: FirebaseUser? = null,
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

    // Intercept back presses globally to act like a backstack
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

    // Root overlay Box: GlassBackdrop fills edge-to-edge, screens never resize when
    // the floating pill navigation bar hides/shows (eliminates reader re-pagination jitter).
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 0: Atmospheric Glassmorphism aurora backdrop
        GlassBackdrop(modifier = Modifier.fillMaxSize())

        // Layer 1: Active Screen Content
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(durationMillis = 200),
            label = "tab_crossfade",
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
                    cloudSyncViewModel = cloudSyncViewModel,
                    currentUser = currentUser,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            }
        }

        // Layer 2: Floating Glassmorphism Pill Navigation Bar Overlay
        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically(initialOffsetY = { it + 120 }) + fadeIn(tween(220)),
            exit = slideOutVertically(targetOffsetY = { it + 120 }) + fadeOut(tween(180)),
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
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    val logoScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.90f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "launch_logo_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "launch_content_alpha"
    )

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040B1A),
                        Color(0xFF091830),
                        Color(0xFF050A16)
                    )
                )
            )
            .testTag("app_launch_loading_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .alpha(contentAlpha)
                .scale(logoScale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = LuxuryGold.copy(alpha = 0.45f),
                        spotColor = LuxuryGold.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 1.6.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                LuxuryGold,
                                CrystalBlue.copy(alpha = 0.85f),
                                LuxuryGoldDeep
                            )
                        ),
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

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(156.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = LuxuryGold,
                trackColor = CrystalBlue.copy(alpha = 0.24f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}
