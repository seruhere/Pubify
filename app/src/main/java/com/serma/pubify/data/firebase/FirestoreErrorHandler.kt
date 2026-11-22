package com.serma.pubify.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET,
    WRITE
}

/**
 * Formats and logs a structured diagnostic JSON payload whenever a Firestore operation fails
 * (e.g., PERMISSION_DENIED or schema rejection), including authenticated user metadata.
 */
fun logFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    val providerArray = JSONArray().apply {
        currentUser?.providerData?.forEach { profile ->
            put(
                JSONObject().apply {
                    put("providerId", profile.providerId)
                    put("displayName", profile.displayName ?: JSONObject.NULL)
                    put("email", profile.email ?: JSONObject.NULL)
                    put("photoUrl", profile.photoUrl?.toString() ?: JSONObject.NULL)
                }
            )
        }
    }

    val payload = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.name.lowercase())
        put("path", path ?: JSONObject.NULL)
        put(
            "authInfo",
            JSONObject().apply {
                put("userId", currentUser?.uid ?: JSONObject.NULL)
                put("email", currentUser?.email ?: JSONObject.NULL)
                put("emailVerified", currentUser?.isEmailVerified ?: false)
                put("isAnonymous", currentUser?.isAnonymous ?: false)
                put("providerInfo", providerArray)
            }
        )
    }.toString()

    Log.e("FirestoreError", payload, exception)
    return payload
}

/**
 * Logs structured Firestore diagnostic JSON and throws an IllegalStateException wrapping the cause.
 */
fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): Nothing {
    val payload = logFirestoreError(exception, operationType, path)
    throw IllegalStateException("FirestoreError: $payload", exception)
}
