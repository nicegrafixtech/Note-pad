package com.example.data.repository

import android.content.Context
import com.example.data.db.NotelyDatabase
import com.example.data.model.BackupManager
import com.example.data.model.BackupPreview
import com.example.data.model.Checklist
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

enum class RestoreMode {
    MERGE,
    REPLACE
}

class NotelyRepository(
    private val database: NotelyDatabase,
    val preferences: AppPreferences,
    private val context: Context
) {
    private val noteDao = database.noteDao()
    private val checklistDao = database.checklistDao()
    private val folderDao = database.folderDao()

    // Notes Flows
    val activeNotes: Flow<List<Note>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()
    val trashNotes: Flow<List<Note>> = noteDao.getTrashNotes()

    // Checklists Flows
    val activeChecklists: Flow<List<Checklist>> = checklistDao.getActiveChecklists()
    val archivedChecklists: Flow<List<Checklist>> = checklistDao.getArchivedChecklists()
    val trashChecklists: Flow<List<Checklist>> = checklistDao.getTrashChecklists()

    // Folders Flow
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()

    // Note Actions
    suspend fun getNoteById(id: Long): Note? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)
    }

    suspend fun saveNote(note: Note): Long = withContext(Dispatchers.IO) {
        if (note.id == 0L) {
            noteDao.insertNote(note.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        } else {
            noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            note.id
        }
    }

    suspend fun togglePinNote(id: Long) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(id) ?: return@withContext
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleArchiveNote(id: Long) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(id) ?: return@withContext
        noteDao.updateNote(note.copy(isArchived = !note.isArchived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleLockNote(id: Long) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(id) ?: return@withContext
        noteDao.updateNote(note.copy(isLocked = !note.isLocked, updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveToTrashNote(id: Long) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(id) ?: return@withContext
        noteDao.updateNote(note.copy(isTrash = true, deletedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun restoreNoteFromTrash(id: Long) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(id) ?: return@withContext
        noteDao.updateNote(note.copy(isTrash = false, deletedAt = null, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNotePermanently(id: Long) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(id)
        if (note?.drawingPath != null) {
            try {
                val file = File(note.drawingPath)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                // Ignore file cleanup error
            }
        }
        noteDao.deleteNoteById(id)
    }

    // Checklist Actions
    suspend fun getChecklistById(id: Long): Checklist? = withContext(Dispatchers.IO) {
        checklistDao.getChecklistById(id)
    }

    suspend fun saveChecklist(checklist: Checklist): Long = withContext(Dispatchers.IO) {
        if (checklist.id == 0L) {
            checklistDao.insertChecklist(checklist.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        } else {
            checklistDao.updateChecklist(checklist.copy(updatedAt = System.currentTimeMillis()))
            checklist.id
        }
    }

    suspend fun togglePinChecklist(id: Long) = withContext(Dispatchers.IO) {
        val cl = checklistDao.getChecklistById(id) ?: return@withContext
        checklistDao.updateChecklist(cl.copy(isPinned = !cl.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleArchiveChecklist(id: Long) = withContext(Dispatchers.IO) {
        val cl = checklistDao.getChecklistById(id) ?: return@withContext
        checklistDao.updateChecklist(cl.copy(isArchived = !cl.isArchived, updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveToTrashChecklist(id: Long) = withContext(Dispatchers.IO) {
        val cl = checklistDao.getChecklistById(id) ?: return@withContext
        checklistDao.updateChecklist(cl.copy(isTrash = true, deletedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun restoreChecklistFromTrash(id: Long) = withContext(Dispatchers.IO) {
        val cl = checklistDao.getChecklistById(id) ?: return@withContext
        checklistDao.updateChecklist(cl.copy(isTrash = false, deletedAt = null, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteChecklistPermanently(id: Long) = withContext(Dispatchers.IO) {
        checklistDao.deleteChecklistById(id)
    }

    // Folder Actions
    suspend fun addFolder(name: String, colorHex: String = "indigo", icon: String = "folder"): Long =
        withContext(Dispatchers.IO) {
            folderDao.insertFolder(Folder(name = name, colorHex = colorHex, icon = icon))
        }

    suspend fun deleteFolder(id: Long) = withContext(Dispatchers.IO) {
        folderDao.deleteFolderById(id)
    }

    // Trash
    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        noteDao.emptyTrash()
        checklistDao.emptyTrash()
    }

    // Backup & Restore
    suspend fun exportDataJson(): String = withContext(Dispatchers.IO) {
        val notes = noteDao.getAllNotes()
        val checklists = checklistDao.getAllChecklists()
        val folders = folderDao.getAllFoldersList()
        val json = BackupManager.exportToJson(notes, checklists, folders)
        preferences.setLastBackupTime(System.currentTimeMillis())
        json
    }

    fun validateBackupJson(json: String): BackupPreview {
        return BackupManager.parseAndValidate(json)
    }

    suspend fun createSafetySnapshot(): String = withContext(Dispatchers.IO) {
        val notes = noteDao.getAllNotes()
        val checklists = checklistDao.getAllChecklists()
        val folders = folderDao.getAllFoldersList()
        val snapshotJson = BackupManager.exportToJson(notes, checklists, folders)
        try {
            val snapshotFile = File(context.filesDir, "safety_snapshot_${System.currentTimeMillis()}.json")
            snapshotFile.writeText(snapshotJson)
        } catch (e: Exception) {
            // Non-fatal
        }
        snapshotJson
    }

    suspend fun restoreBackup(json: String, mode: RestoreMode): Result<String> = withContext(Dispatchers.IO) {
        val preview = BackupManager.parseAndValidate(json)
        if (!preview.isValid) {
            return@withContext Result.failure(Exception(preview.errorMessage ?: "Corrupt or invalid backup file"))
        }

        try {
            // Safety snapshot before any destructive change
            createSafetySnapshot()

            val (notes, checklists, folders) = BackupManager.extractData(json)

            if (mode == RestoreMode.REPLACE) {
                noteDao.clearAll()
                checklistDao.clearAll()
                folderDao.clearAll()
            }

            folderDao.insertAll(folders)
            noteDao.insertAll(notes)
            checklistDao.insertAll(checklists)

            preferences.setLastBackupTime(System.currentTimeMillis())
            Result.success("Successfully restored ${notes.size} notes, ${checklists.size} checklists, and ${folders.size} folders.")
        } catch (e: Exception) {
            Result.failure(Exception("Restore failed: ${e.localizedMessage}"))
        }
    }
}
