package com.spyglass.alprlaunchersdksample.ui.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    fun copyDatabaseFromAssets(): File? {
        val dbFolder = File(context.filesDir, "embedded-database")
        if (!dbFolder.exists()) {
            dbFolder.mkdir()
        }

        val databaseFile = File(dbFolder, "hotlist_database.db")
        return try {
            context.assets.open("hotlist_database.db").use { inputStream ->
                FileOutputStream(databaseFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            databaseFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun copyDatabaseToInternalStorage(uri: Uri): File? {
        val dbFolder = File(context.filesDir, "embedded-database")
        if (!dbFolder.exists()) {
            dbFolder.mkdir()
        }

        val fileExtension = getFileExtensionFromUri(uri) ?: "sqlite"
        val outputFile = File(dbFolder, "temp.$fileExtension")

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            outputFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileExtensionFromUri(uri: Uri): String? {
        var fileExtension: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                fileExtension = fileName?.substringAfterLast('.', "")
            }
        }
        return fileExtension.takeIf { it?.isNotEmpty() == true }
    }

    fun getAllTablesAndColumns(databaseFile: File): List<String> {
        val tablesAndColumnsMap = mutableListOf<String>()
        val sqliteDatabase = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        val cursor =
            sqliteDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        if (cursor.moveToFirst()) {
            do {
                val tableName = cursor.getString(0)
                tablesAndColumnsMap.add(tableName)
            } while (cursor.moveToNext())
        }
        cursor.close()
        sqliteDatabase.close()
        return tablesAndColumnsMap
    }
}