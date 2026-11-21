package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.signInWithGoogle
import com.example.ui.components.CrystalBlue
import com.example.ui.components.GlassBackdrop
import com.example.ui.components.GlassCard
import com.example.ui.components.LocalGlassPalette
import com.example.ui.components.LuxuryGold
import com.example.ui.components.LuxuryGoldDeep
import com.example.ui.components.luxuryBlueColor
import com.example.ui.components.luxuryGoldBlueBorderBrush
import com.example.ui.components.luxuryGoldColor
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isSigningIn by remember { mutableStateOf(false) }
    var isCheckingSavedAccount by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val gold = luxuryGoldColor()
    val blue = luxuryBlueColor()
    val isDark = LocalGlassPalette.current.isDark

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onSuccess = {
                isCheckingSavedAccount = false
                errorMessage = null
            },
            onNoSavedCredentials = {
                isCheckingSavedAccount = false
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("auth_screen")
    ) {
        GlassBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Branded Geometric Logo & Title
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = LuxuryGold.copy(alpha = 0.35f),
                            spotColor = LuxuryGold.copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    gold,
                                    blue.copy(alpha = 0.85f),
                                    LuxuryGoldDeep
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo_geometric_1795194079814),
                        contentDescription = stringResource(id = R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            letterSpacing = 1.4.sp
                        ),
                        color = gold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.auth_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center
                    )
                }

                // Sign-In Container matching Reading Statistics container theme
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    elevated = true,
                    accentTint = gold,
                    borderWidth = 1.25.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "CLOUD LIBRARY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.8.sp,
                                    fontSize = 10.sp
                                ),
                                color = gold
                            )
                            Text(
                                text = stringResource(id = R.string.auth_card_title),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(id = R.string.auth_card_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Compact feature pills
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AuthBenefitRow(
                                icon = Icons.Default.CloudDone,
                                title = "Personal Cloud Backup",
                                subtitle = "Library catalog, highlights & bookmarks saved to your account",
                                gold = gold,
                                blue = blue,
                                isDark = isDark
                            )
                            AuthBenefitRow(
                                icon = Icons.Default.Sync,
                                title = "Reading Progress Sync",
                                subtitle = "Keep chapters, completion status & reading statistics safe",
                                gold = gold,
                                blue = blue,
                                isDark = isDark
                            )
                            AuthBenefitRow(
                                icon = Icons.Default.Lock,
                                title = "Private & Owner-Only",
                                subtitle = "Secured with Google Sign-In and strict Firestore isolation",
                                gold = gold,
                                blue = blue,
                                isDark = isDark
                            )
                        }

                        AnimatedVisibility(visible = errorMessage != null) {
                            errorMessage?.let { err ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_error_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = err,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (isSigningIn) return@Button
                                isSigningIn = true
                                errorMessage = null
                                scope.launch {
                                    signInWithGoogle(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onSuccess = {
                                            isSigningIn = false
                                            errorMessage = null
                                        },
                                        onAuthCancelled = {
                                            isSigningIn = false
                                        },
                                        onAuthError = { message ->
                                            isSigningIn = false
                                            errorMessage = message
                                        }
                                    )
                                }
                            },
                            enabled = !isSigningIn && !isCheckingSavedAccount,
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = gold,
                                contentColor = Color(0xFF0A1426),
                                disabledContainerColor = gold.copy(alpha = 0.45f),
                                disabledContentColor = Color(0xFF0A1426).copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isSigningIn || isCheckingSavedAccount) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.2.dp,
                                    color = Color(0xFF0A1426)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isCheckingSavedAccount) {
                                        stringResource(id = R.string.auth_checking_account)
                                    } else {
                                        stringResource(id = R.string.auth_signing_in)
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.auth_sign_in_google),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthBenefitRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gold: Color,
    blue: Color,
    isDark: Boolean
) {
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                gold.copy(alpha = if (isDark) 0.22f else 0.16f),
                                blue.copy(alpha = if (isDark) 0.22f else 0.14f)
                            )
                        )
                    )
                    .border(1.dp, luxuryGoldBlueBorderBrush(0.65f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = gold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = gold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
