package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.db.NotelyDatabase
import com.example.data.model.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExportUtility {

    suspend fun generateExportArchive(
        context: Context,
        database: NotelyDatabase
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val notes = database.noteDao().getAllNotes()
            val checklists = database.checklistDao().getAllChecklists()
            val folders = database.folderDao().getAllFoldersList()

            val jsonContent = BackupManager.exportToJson(notes, checklists, folders)

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(exportDir, "notely_backup_$timestamp.json")

            FileOutputStream(exportFile).use { stream ->
                stream.write(jsonContent.toByteArray(Charsets.UTF_8))
            }

            Result.success(exportFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareExportArchive(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Notely Backup Archive - ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())}"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is your Notely backup archive (JSON schema v${BackupManager.CURRENT_SCHEMA_VERSION} with SHA-256 integrity checksum)."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Notely Backup Archive").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
