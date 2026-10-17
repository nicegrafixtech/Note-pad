package com.example.data.service

import com.example.data.db.NotelyDatabase
import com.example.data.model.Checklist
import com.example.data.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class TrashItemInfo(
    val id: Long,
    val title: String,
    val type: TrashItemType,
    val deletedAt: Long,
    val daysRemaining: Int,
    val remainingFormatted: String
)

enum class TrashItemType {
    NOTE,
    CHECKLIST
}

class TrashService(
    private val database: NotelyDatabase
) {
    private val noteDao = database.noteDao()
    private val checklistDao = database.checklistDao()

    val trashNotes: Flow<List<Note>> = noteDao.getTrashNotes()
    val trashChecklists: Flow<List<Checklist>> = checklistDao.getTrashChecklists()

    companion object {
        val TRASH_RETENTION_MILLIS: Long = TimeUnit.DAYS.toMillis(30)

        fun calculateDaysRemaining(deletedAt: Long?, currentTime: Long = System.currentTimeMillis()): Int {
            if (deletedAt == null) return 30
            val elapsed = currentTime - deletedAt
            val remainingMillis = TRASH_RETENTION_MILLIS - elapsed
            if (remainingMillis <= 0) return 0
            val oneDay = TimeUnit.DAYS.toMillis(1)
            val days = (remainingMillis + oneDay - 1) / oneDay
            return maxOf(0, days.toInt())
        }

        fun formatRemainingTime(deletedAt: Long?, currentTime: Long = System.currentTimeMillis()): String {
            if (deletedAt == null) return "30 days left"
            val elapsed = currentTime - deletedAt
            val remainingMillis = TRASH_RETENTION_MILLIS - elapsed
            if (remainingMillis <= 0) return "Expiring now"

            val oneDay = TimeUnit.DAYS.toMillis(1)
            val days = (remainingMillis + oneDay - 1) / oneDay
            if (days > 1) return "$days days left"
            if (days == 1L) {
                val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
                return if (hours > 1) "$hours hours left" else "Expiring today"
            }
            return "Expiring soon"
        }
    }

    suspend fun purgeExpiredItems(): Int = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - TRASH_RETENTION_MILLIS
        var purgedCount = 0

        val notes = noteDao.getTrashNotes().firstOrNull() ?: emptyList()
        notes.forEach { note ->
            if (note.deletedAt != null && note.deletedAt <= threshold) {
                if (note.drawingPath != null) {
                    try {
                        val file = File(note.drawingPath)
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                noteDao.deleteNoteById(note.id)
                purgedCount++
            }
        }

        val checklists = checklistDao.getTrashChecklists().firstOrNull() ?: emptyList()
        checklists.forEach { cl ->
            if (cl.deletedAt != null && cl.deletedAt <= threshold) {
                checklistDao.deleteChecklistById(cl.id)
                purgedCount++
            }
        }

        purgedCount
    }

    suspend fun restoreNote(noteId: Long) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(noteId) ?: return@withContext
        noteDao.updateNote(
            note.copy(
                isTrash = false,
                deletedAt = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun permanentlyDeleteNote(noteId: Long) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(noteId)
        if (note?.drawingPath != null) {
            try {
                val file = File(note.drawingPath)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                // ignore
            }
        }
        noteDao.deleteNoteById(noteId)
    }

    suspend fun restoreChecklist(checklistId: Long) = withContext(Dispatchers.IO) {
        val cl = checklistDao.getChecklistById(checklistId) ?: return@withContext
        checklistDao.updateChecklist(
            cl.copy(
                isTrash = false,
                deletedAt = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun permanentlyDeleteChecklist(checklistId: Long) = withContext(Dispatchers.IO) {
        checklistDao.deleteChecklistById(checklistId)
    }

    suspend fun emptyAllTrash() = withContext(Dispatchers.IO) {
        noteDao.emptyTrash()
        checklistDao.emptyTrash()
    }
}
