package com.example.converter

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class SavedDeviceFileResult(
    val savedSuccessfully: Boolean,
    val displayPath: String? = null,
    val contentUri: Uri? = null,
    val folderName: String? = null,
    val errorMessage: String? = null
)

/**
 * Automatically creates a device folder named after the app (e.g. "Downloads/PDF to EPUB Converter/")
 * and stores every converted EPUB file inside it using zero-permission MediaStore on Android 10+ (API 29+)
 * and public Downloads / external Downloads fallback on older APIs.
 */
object EpubDeviceStorage {

    private const val FALLBACK_FOLDER_NAME = "Pubify"
    private const val EPUB_MIME = "application/epub+zip"

    /**
     * Resolves the folder name directly from the application's display name (`R.string.app_name`),
     * sanitized for filesystem compatibility across Android devices.
     */
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

    /**
     * Proactively creates the app-named output directory on device storage when direct directory
     * creation is allowed (pre-API 29 public Downloads and app external Downloads directory).
     * On API 29+, MediaStore.Downloads also automatically creates "Downloads/<AppName>/" on first write.
     */
    @Suppress("DEPRECATION")
    fun ensureAppFolderExists(context: Context): File? {
        val folderName = getAppFolderName(context)
        val candidates = buildList {
            try {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (publicDownloads != null) {
                    add(File(publicDownloads, folderName))
                }
            } catch (_: Throwable) {}
            try {
                val appExternalDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (appExternalDownloads != null) {
                    add(File(appExternalDownloads, folderName))
                }
            } catch (_: Throwable) {}
        }

        var createdDir: File? = null
        for (dir in candidates) {
            try {
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                if (dir.exists() && dir.isDirectory && createdDir == null) {
                    createdDir = dir
                }
            } catch (_: Throwable) {}
        }
        return createdDir
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

        return saveViaLegacyOrExternalDownloads(
            context = context,
            sourceEpubFile = sourceEpubFile,
            fileName = targetFileName,
            appFolderName = appFolderName
        )
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
    private fun saveViaLegacyOrExternalDownloads(
        context: Context,
        sourceEpubFile: File,
        fileName: String,
        appFolderName: String
    ): SavedDeviceFileResult {
        val candidateDirs = buildList {
            try {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (publicDownloads != null) {
                    add(File(publicDownloads, appFolderName) to "Downloads/$appFolderName")
                }
            } catch (_: Throwable) {}
            try {
                val appExternalDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (appExternalDownloads != null) {
                    add(File(appExternalDownloads, appFolderName) to "Android/data/${context.packageName}/files/Download/$appFolderName")
                }
            } catch (_: Throwable) {}
        }

        for ((dir, displayPrefix) in candidateDirs) {
            try {
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
                            displayPath = "$displayPrefix/${targetFile.name}",
                            contentUri = Uri.fromFile(targetFile),
                            folderName = appFolderName
                        )
                    }
                }
            } catch (_: Throwable) {
                // Fall through to next candidate directory
            }
        }

        return SavedDeviceFileResult(
            savedSuccessfully = false,
            folderName = appFolderName,
            errorMessage = "Could not write EPUB to device folder $appFolderName"
        )
    }
}
