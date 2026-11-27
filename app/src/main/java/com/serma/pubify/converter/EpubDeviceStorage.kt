package com.serma.pubify.converter

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.serma.pubify.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class DeviceStorageMode {
    UNSET,
    SAF_FOLDER,
    SCOPED_DOWNLOADS,
    APP_ONLY
}

data class StoragePermissionStatus(
    val mode: DeviceStorageMode,
    val isConfigured: Boolean,
    val hasSharedStorageAccess: Boolean,
    val isLegacyRuntimePermissionRequired: Boolean,
    val displayLocation: String,
    val treeUri: Uri? = null
)

data class SavedDeviceFileResult(
    val savedSuccessfully: Boolean,
    val displayPath: String? = null,
    val contentUri: Uri? = null,
    val folderName: String? = null,
    val errorMessage: String? = null
)

/**
 * Manages user-authorized storage permission and saves converted EPUB files to:
 * 1. A user-granted Storage Access Framework (SAF) folder on Android 10+ (API 29+) via
 *    `ActivityResultContracts.OpenDocumentTree` + `takePersistableUriPermission`,
 * 2. User-confirmed Scoped Storage `Downloads/<AppName>/` via `MediaStore.Downloads`,
 * 3. Public `Downloads/<AppName>/` on Android 7–9 (API 24–28) after runtime `WRITE_EXTERNAL_STORAGE` grant,
 * 4. Or strictly inside private app library storage if the user declines shared device storage access.
 */
object EpubDeviceStorage {

    private const val PREFS_NAME = "reader_app_prefs"
    private const val KEY_STORAGE_MODE = "device_storage_mode"
    private const val KEY_SAF_TREE_URI = "device_saf_tree_uri"
    private const val FALLBACK_FOLDER_NAME = "Pubify"
    private const val EPUB_MIME = "application/epub+zip"

    fun isLegacyWritePermissionRequired(): Boolean =
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    fun hasLegacyWritePermission(context: Context): Boolean {
        if (!isLegacyWritePermissionRequired()) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getAppFolderName(context: Context): String {
        val rawName = try {
            context.getString(R.string.app_name)
        } catch (_: Throwable) {
            FALLBACK_FOLDER_NAME
        }
        return rawName
            .replace(Regex("[/\\\\:*?\"<>|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { FALLBACK_FOLDER_NAME }
    }

    fun getPersistedOutputTreeUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SAF_TREE_URI, null)?.trim()
        if (raw.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val hasPersistedGrant = try {
            context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isWritePermission
            }
        } catch (_: Throwable) {
            false
        }
        return if (hasPersistedGrant) uri else null
    }

    fun savePersistedOutputTreeUri(context: Context, treeUri: Uri): Boolean {
        return try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAF_TREE_URI, treeUri.toString())
                .putString(KEY_STORAGE_MODE, DeviceStorageMode.SAF_FOLDER.name)
                .apply()
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun confirmScopedDownloadsAccess(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SAF_TREE_URI)
            .putString(KEY_STORAGE_MODE, DeviceStorageMode.SCOPED_DOWNLOADS.name)
            .apply()
    }

    fun setAppOnlyStorageMode(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SAF_TREE_URI)
            .putString(KEY_STORAGE_MODE, DeviceStorageMode.APP_ONLY.name)
            .apply()
    }

    fun getStoragePermissionStatus(context: Context): StoragePermissionStatus {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedMode = runCatching {
            DeviceStorageMode.valueOf(prefs.getString(KEY_STORAGE_MODE, DeviceStorageMode.UNSET.name) ?: DeviceStorageMode.UNSET.name)
        }.getOrDefault(DeviceStorageMode.UNSET)
        val appFolderName = getAppFolderName(context)

        if (isLegacyWritePermissionRequired()) {
            val legacyGranted = hasLegacyWritePermission(context)
            return when {
                legacyGranted -> StoragePermissionStatus(
                    mode = DeviceStorageMode.SCOPED_DOWNLOADS,
                    isConfigured = true,
                    hasSharedStorageAccess = true,
                    isLegacyRuntimePermissionRequired = true,
                    displayLocation = "Downloads/$appFolderName"
                )
                savedMode == DeviceStorageMode.APP_ONLY -> StoragePermissionStatus(
                    mode = DeviceStorageMode.APP_ONLY,
                    isConfigured = true,
                    hasSharedStorageAccess = false,
                    isLegacyRuntimePermissionRequired = true,
                    displayLocation = "App Private Library Only"
                )
                else -> StoragePermissionStatus(
                    mode = DeviceStorageMode.UNSET,
                    isConfigured = false,
                    hasSharedStorageAccess = false,
                    isLegacyRuntimePermissionRequired = true,
                    displayLocation = "Permission Not Granted"
                )
            }
        }

        // Android 10+ (API 29+)
        val safUri = getPersistedOutputTreeUri(context)
        if (safUri != null) {
            return StoragePermissionStatus(
                mode = DeviceStorageMode.SAF_FOLDER,
                isConfigured = true,
                hasSharedStorageAccess = true,
                isLegacyRuntimePermissionRequired = false,
                displayLocation = formatTreeUriDisplayPath(safUri),
                treeUri = safUri
            )
        }

        return when (savedMode) {
            DeviceStorageMode.SCOPED_DOWNLOADS -> StoragePermissionStatus(
                mode = DeviceStorageMode.SCOPED_DOWNLOADS,
                isConfigured = true,
                hasSharedStorageAccess = true,
                isLegacyRuntimePermissionRequired = false,
                displayLocation = "Downloads/$appFolderName"
            )
            DeviceStorageMode.APP_ONLY -> StoragePermissionStatus(
                mode = DeviceStorageMode.APP_ONLY,
                isConfigured = true,
                hasSharedStorageAccess = false,
                isLegacyRuntimePermissionRequired = false,
                displayLocation = "App Private Library Only"
            )
            else -> StoragePermissionStatus(
                mode = DeviceStorageMode.UNSET,
                isConfigured = false,
                hasSharedStorageAccess = false,
                isLegacyRuntimePermissionRequired = false,
                displayLocation = "Permission Not Granted"
            )
        }
    }

    fun formatTreeUriDisplayPath(treeUri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri).orEmpty()
            when {
                docId.startsWith("primary:", ignoreCase = true) -> {
                    val subPath = docId.substringAfter(":").trim('/')
                    if (subPath.isBlank()) "Internal Storage" else subPath
                }
                docId.contains(":") -> {
                    val subPath = docId.substringAfter(":").trim('/')
                    if (subPath.isBlank()) "External Storage" else "SD Card/$subPath"
                }
                docId.isNotBlank() -> docId
                else -> "Selected Folder"
            }
        } catch (_: Throwable) {
            "Selected Folder"
        }
    }

    fun saveEpubToDeviceDownloads(
        context: Context,
        sourceEpubFile: File,
        bookTitle: String
    ): SavedDeviceFileResult {
        if (!sourceEpubFile.exists() || sourceEpubFile.length() == 0L) {
            return SavedDeviceFileResult(
                savedSuccessfully = false,
                errorMessage = "Source EPUB file is missing or empty"
            )
        }

        val status = getStoragePermissionStatus(context)
        if (!status.hasSharedStorageAccess) {
            return SavedDeviceFileResult(
                savedSuccessfully = false,
                displayPath = null,
                errorMessage = "Saved in private app library only (device storage permission not granted)"
            )
        }

        val appFolderName = getAppFolderName(context)
        val cleanBaseName = bookTitle
            .trim()
            .replace(Regex("[^a-zA-Z0-9._\\- ]"), "")
            .replace(Regex("\\s+"), "_")
            .take(50)
            .ifBlank { "Converted_Book" }

        val targetFileName = if (cleanBaseName.endsWith(".epub", ignoreCase = true)) {
            cleanBaseName
        } else {
            "$cleanBaseName.epub"
        }

        // 1. User-granted Storage Access Framework (SAF) folder takes priority
        val safUri = status.treeUri
        if (safUri != null) {
            val safResult = saveViaSafTreeUri(
                context = context,
                treeUri = safUri,
                sourceEpubFile = sourceEpubFile,
                fileName = targetFileName
            )
            if (safResult.savedSuccessfully) {
                return safResult
            }
        }

        // 2. Scoped Storage MediaStore.Downloads on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mediaStoreResult = saveViaMediaStoreDownloads(
                context = context,
                sourceEpubFile = sourceEpubFile,
                fileName = targetFileName,
                appFolderName = appFolderName
            )
            if (mediaStoreResult.savedSuccessfully) {
                return mediaStoreResult
            }
        }

        // 3. Legacy public Downloads on Android 7–9 (requires WRITE_EXTERNAL_STORAGE)
        return saveViaLegacyDownloads(
            context = context,
            sourceEpubFile = sourceEpubFile,
            fileName = targetFileName,
            appFolderName = appFolderName
        )
    }

    private fun saveViaSafTreeUri(
        context: Context,
        treeUri: Uri,
        sourceEpubFile: File,
        fileName: String
    ): SavedDeviceFileResult {
        return try {
            val resolver = context.contentResolver
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            val createdUri = DocumentsContract.createDocument(
                resolver,
                parentDocumentUri,
                EPUB_MIME,
                fileName
            ) ?: return SavedDeviceFileResult(
                savedSuccessfully = false,
                errorMessage = "Could not create file in selected folder"
            )

            try {
                resolver.openOutputStream(createdUri)?.use { out ->
                    FileInputStream(sourceEpubFile).use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("Unable to open output stream in selected folder")

                val folderLabel = formatTreeUriDisplayPath(treeUri)
                SavedDeviceFileResult(
                    savedSuccessfully = true,
                    displayPath = "$folderLabel/$fileName",
                    contentUri = createdUri,
                    folderName = folderLabel
                )
            } catch (e: Throwable) {
                runCatching { DocumentsContract.deleteDocument(resolver, createdUri) }
                SavedDeviceFileResult(
                    savedSuccessfully = false,
                    errorMessage = e.localizedMessage ?: "Failed writing to selected folder"
                )
            }
        } catch (e: Throwable) {
            SavedDeviceFileResult(
                savedSuccessfully = false,
                errorMessage = e.localizedMessage ?: "Selected folder unavailable"
            )
        }
    }

    private fun saveViaMediaStoreDownloads(
        context: Context,
        sourceEpubFile: File,
        fileName: String,
        appFolderName: String
    ): SavedDeviceFileResult {
        return try {
            val resolver = context.contentResolver
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$appFolderName"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, EPUB_MIME)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val insertedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return SavedDeviceFileResult(
                    savedSuccessfully = false,
                    folderName = appFolderName,
                    errorMessage = "MediaStore insert returned null"
                )

            try {
                resolver.openOutputStream(insertedUri)?.use { out ->
                    FileInputStream(sourceEpubFile).use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("Unable to open MediaStore output stream")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val readyValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    resolver.update(insertedUri, readyValues, null, null)
                }

                SavedDeviceFileResult(
                    savedSuccessfully = true,
                    displayPath = "Downloads/$appFolderName/$fileName",
                    contentUri = insertedUri,
                    folderName = appFolderName
                )
            } catch (e: Throwable) {
                try {
                    resolver.delete(insertedUri, null, null)
                } catch (_: Throwable) {}
                SavedDeviceFileResult(
                    savedSuccessfully = false,
                    folderName = appFolderName,
                    errorMessage = e.localizedMessage ?: "MediaStore write failed"
                )
            }
        } catch (e: Throwable) {
            SavedDeviceFileResult(
                savedSuccessfully = false,
                folderName = appFolderName,
                errorMessage = e.localizedMessage ?: "MediaStore unavailable"
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun saveViaLegacyDownloads(
        context: Context,
        sourceEpubFile: File,
        fileName: String,
        appFolderName: String
    ): SavedDeviceFileResult {
        if (!hasLegacyWritePermission(context)) {
            return SavedDeviceFileResult(
                savedSuccessfully = false,
                folderName = appFolderName,
                errorMessage = "Storage permission not granted"
            )
        }

        val publicDownloads = try {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } catch (_: Throwable) {
            null
        } ?: return SavedDeviceFileResult(
            savedSuccessfully = false,
            folderName = appFolderName,
            errorMessage = "Downloads directory unavailable"
        )

        val dir = File(publicDownloads, appFolderName)
        return try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (dir.exists() && dir.canWrite()) {
                var targetFile = File(dir, fileName)
                if (targetFile.exists()) {
                    val base = fileName.removeSuffix(".epub")
                    targetFile = File(dir, "${base}_${System.currentTimeMillis() % 10000}.epub")
                }
                FileInputStream(sourceEpubFile).use { input ->
                    FileOutputStream(targetFile).use { out ->
                        input.copyTo(out)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0L) {
                    return SavedDeviceFileResult(
                        savedSuccessfully = true,
                        displayPath = "Downloads/$appFolderName/${targetFile.name}",
                        contentUri = Uri.fromFile(targetFile),
                        folderName = appFolderName
                    )
                }
            }
            SavedDeviceFileResult(
                savedSuccessfully = false,
                folderName = appFolderName,
                errorMessage = "Could not write EPUB to Downloads/$appFolderName"
            )
        } catch (e: Throwable) {
            SavedDeviceFileResult(
                savedSuccessfully = false,
                folderName = appFolderName,
                errorMessage = e.localizedMessage ?: "Could not write EPUB to device storage"
            )
        }
    }
}
