package com.serma.pubify.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.serma.pubify.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Safely resolves the [Activity] from a given [Context].
 */
fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

/** Retrieves the Web Client ID generated from this variant's google-services.json. */
fun getServerClientId(context: Context): String =
    context.getString(R.string.default_web_client_id)

/**
 * Creates the Google Sign-In intent for legacy Google Play Services Auth fallback.
 */
fun launchGoogleSignInIntent(context: Context): Intent {
    val serverClientId = getServerClientId(context)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(serverClientId)
        .requestEmail()
        .build()
    val client = GoogleSignIn.getClient(context, gso)
    return client.signInIntent
}

/**
 * Handles the result from [launchGoogleSignInIntent].
 */
suspend fun handleGoogleSignInResult(
    data: Intent?,
    onSuccess: (FirebaseUser) -> Unit,
    onAuthError: (String) -> Unit
) {
    try {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.getResult(ApiException::class.java)
        val idToken = account.idToken
        if (idToken != null) {
            val authResult = Firebase.auth
                .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .await()
            val user = authResult.user
            if (user != null) {
                onSuccess(user)
            } else {
                onAuthError("Sign-in succeeded without an active Firebase user.")
            }
        } else {
            onAuthError("Google Sign-In returned an empty token.")
        }
    } catch (e: ApiException) {
        Log.e("Auth", "GoogleSignInClient failed code [${e.statusCode}]: ${e.message}", e)
        val friendlyMsg = when (e.statusCode) {
            10 -> "Google Sign-In Error 10 (DEVELOPER_ERROR): Register the SHA-1 fingerprint of this app's signing certificate in Firebase Console and enable Google as a sign-in provider."
            12500 -> "Google Sign-In Error 12500: Ensure Project Support Email is selected in Firebase Console -> Authentication -> Settings."
            7 -> "Network error: Unable to connect to Google Sign-In services."
            12501 -> "Sign-in cancelled by user."
            else -> "Google Sign-In Error [${e.statusCode}]: ${e.message ?: "Sign-in failed."}"
        }
        if (e.statusCode != 12501) {
            onAuthError(friendlyMsg)
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("Auth", "Unexpected error during Google Sign-In intent result", e)
        onAuthError("Error: ${e.localizedMessage ?: "Unable to complete sign-in."}")
    }
}

/**
 * Emits the current [FirebaseUser] whenever Firebase Auth sign-in or sign-out state changes.
 */
fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Silent returning-user sign-in using Credential Manager's [GetGoogleIdOption].
 */
suspend fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onSuccess: (FirebaseUser) -> Unit,
    onNoSavedCredentials: () -> Unit
) {
    try {
        val activityContext = context.findActivity() ?: context
        val serverClientId = getServerClientId(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(activityContext, request)
        val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        val authResult = Firebase.auth
            .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .await()
        val user = authResult.user
        if (user != null) {
            onSuccess(user)
        } else {
            onNoSavedCredentials()
        }
    } catch (e: NoCredentialException) {
        Log.d("Auth", "No previously authorized Google credentials found; awaiting manual sign-in.")
        onNoSavedCredentials()
    } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Auto sign-in cancelled by user", e)
        onNoSavedCredentials()
    } catch (e: GetCredentialException) {
        Log.w("Auth", "Silent auto sign-in unavailable: ${e.message}", e)
        onNoSavedCredentials()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w("Auth", "Silent auto sign-in failed: ${e.message}", e)
        onNoSavedCredentials()
    }
}

/**
 * Interactive Google Sign-In flow bound to explicit "Sign in with Google" buttons.
 * Attempts [GetSignInWithGoogleOption] first and triggers [onFallbackToPlayServicesIntent] if Credential Manager fails.
 */
suspend fun signInWithGoogle(
    context: Context,
    credentialManager: CredentialManager,
    onSuccess: (FirebaseUser) -> Unit,
    onAuthCancelled: () -> Unit,
    onAuthError: (String) -> Unit,
    onFallbackToPlayServicesIntent: () -> Unit
) {
    try {
        val activityContext = context.findActivity() ?: context
        val serverClientId = getServerClientId(context)
        Log.d("Auth", "Initiating Google Sign-In with serverClientId: $serverClientId")

        val credentialData = try {
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            val result = credentialManager.getCredential(activityContext, request)
            result.credential.data
        } catch (e: Exception) {
            if (e is CancellationException || e is GetCredentialCancellationException) throw e
            Log.w("Auth", "GetSignInWithGoogleOption failed, trying GetGoogleIdOption: ${e.message}", e)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(activityContext, request)
            result.credential.data
        }

        val idToken = GoogleIdTokenCredential.createFrom(credentialData).idToken
        val authResult = Firebase.auth
            .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .await()
        val user = authResult.user
        if (user != null) {
            onSuccess(user)
        } else {
            onAuthError("Sign-in succeeded without an active Firebase user.")
        }
    } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "User cancelled Google Sign-In", e)
        onAuthCancelled()
    } catch (e: NoCredentialException) {
        Log.w("Auth", "Credential Manager returned NoCredentialException, launching Play Services Intent...", e)
        onFallbackToPlayServicesIntent()
    } catch (e: GetCredentialException) {
        Log.w("Auth", "Credential Manager failed [${e.type}]: ${e.message}, launching Play Services Intent...", e)
        onFallbackToPlayServicesIntent()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("Auth", "Unexpected error during Google Sign-In, launching Play Services Intent...", e)
        onFallbackToPlayServicesIntent()
    }
}

/**
 * Signs out of Firebase Auth and clears Credential Manager state inside [NonCancellable].
 */
fun signOutFromFirebase(
    credentialManager: CredentialManager,
    scope: CoroutineScope
) {
    scope.launch {
        withContext(NonCancellable) {
            Firebase.auth.signOut()
            runCatching {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }.onFailure { err ->
                Log.w("Auth", "Failed to clear credential manager state on sign-out", err)
            }
        }
    }
}
