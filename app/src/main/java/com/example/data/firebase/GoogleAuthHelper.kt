package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
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
 * Silent returning-user sign-in using Credential Manager's [GetGoogleIdOption]
 * (strictly ONE option per GetCredentialRequest for Android 16 / API 36 compatibility).
 */
suspend fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onSuccess: (FirebaseUser) -> Unit,
    onNoSavedCredentials: () -> Unit
) {
    try {
        val serverClientId = context.getString(R.string.default_web_client_id)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
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
 * Interactive Google Sign-In flow bound to an explicit "Sign in with Google" button.
 * Uses [GetSignInWithGoogleOption] with strictly ONE option per GetCredentialRequest.
 */
suspend fun signInWithGoogle(
    context: Context,
    credentialManager: CredentialManager,
    onSuccess: (FirebaseUser) -> Unit,
    onAuthCancelled: () -> Unit,
    onAuthError: (String) -> Unit
) {
    try {
        val serverClientId = context.getString(R.string.default_web_client_id)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
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
        Log.w("Auth", "No Google account available on device", e)
        onAuthError("No Google account found on this device. Please add a Google account in device settings and try again.")
    } catch (e: GetCredentialException) {
        Log.e("Auth", "Google Sign-In failed: ${e.message}", e)
        onAuthError(e.errorMessage?.toString() ?: e.message ?: "Google Sign-In failed. Please try again.")
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("Auth", "Unexpected error during Google Sign-In", e)
        onAuthError(e.localizedMessage ?: "Unable to complete sign-in. Please check your connection.")
    }
}

/**
 * Signs out of Firebase Auth and clears Credential Manager state inside [NonCancellable]
 * so sign-out cleanup always completes even if the calling screen unmounts immediately.
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
