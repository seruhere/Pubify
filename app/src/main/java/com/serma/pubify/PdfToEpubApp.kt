package com.serma.pubify

import android.app.Application
import com.serma.pubify.data.db.AppDatabase
import com.serma.pubify.data.repository.BookRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfToEpubApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: BookRepository
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox resource loader for Android
        PDFBoxResourceLoader.init(applicationContext)

        // Initialize Room database & repository
        database = AppDatabase.getInstance(this)
        repository = BookRepository(database.bookDao())

        // Ensure the app-named output folder exists on device storage
        Thread {
            try {
                com.serma.pubify.converter.EpubDeviceStorage.ensureAppFolderExists(applicationContext)
            } catch (_: Throwable) {}
        }.start()
    }
}
