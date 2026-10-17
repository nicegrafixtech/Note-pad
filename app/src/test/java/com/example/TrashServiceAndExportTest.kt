package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.NotelyDatabase
import com.example.data.model.Checklist
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.service.TrashService
import com.example.util.DataExportUtility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrashServiceAndExportTest {

    private lateinit var context: Context
    private lateinit var database: NotelyDatabase
    private lateinit var trashService: TrashService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NotelyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trashService = TrashService(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testTrashServiceRetentionCalculations() {
        val now = System.currentTimeMillis()

        // Just deleted (0 days ago) -> 30 days left
        val days0 = TrashService.calculateDaysRemaining(now, now)
        assertEquals(30, days0)
        val formatted0 = TrashService.formatRemainingTime(now, now)
        assertTrue(formatted0.contains("30 days"))

        // Deleted 10 days ago -> 20 days left
        val deleted10DaysAgo = now - TimeUnit.DAYS.toMillis(10)
        val days10 = TrashService.calculateDaysRemaining(deleted10DaysAgo, now)
        assertEquals(20, days10)
        val formatted10 = TrashService.formatRemainingTime(deleted10DaysAgo, now)
        assertTrue(formatted10.contains("20 days"))

        // Deleted 31 days ago -> 0 days left
        val deleted31DaysAgo = now - TimeUnit.DAYS.toMillis(31)
        val days31 = TrashService.calculateDaysRemaining(deleted31DaysAgo, now)
        assertEquals(0, days31)
    }

    @Test
    fun testTrashServiceRestoreAndPermanentDelete() = runBlocking {
        val noteDao = database.noteDao()

        // Insert a soft-deleted note
        val noteId = noteDao.insertNote(
            Note(
                title = "Trashed Note",
                content = "Body",
                isTrash = true,
                deletedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
            )
        )

        val trashedNotesBefore = trashService.trashNotes.first()
        assertEquals(1, trashedNotesBefore.size)

        // Restore note
        trashService.restoreNote(noteId)
        val trashedNotesAfterRestore = trashService.trashNotes.first()
        assertEquals(0, trashedNotesAfterRestore.size)

        val restoredNote = noteDao.getNoteById(noteId)
        assertNotNull(restoredNote)
        assertFalse(restoredNote!!.isTrash)
        assertNull(restoredNote.deletedAt)

        // Move to trash again and permanently delete
        noteDao.updateNote(restoredNote.copy(isTrash = true, deletedAt = System.currentTimeMillis()))
        trashService.permanentlyDeleteNote(noteId)

        val deletedNote = noteDao.getNoteById(noteId)
        assertNull(deletedNote)
    }

    @Test
    fun testTrashServicePurgeExpiredItems() = runBlocking {
        val noteDao = database.noteDao()
        val checklistDao = database.checklistDao()

        val now = System.currentTimeMillis()

        // Fresh trash item (5 days old) -> should NOT be purged
        noteDao.insertNote(
            Note(
                title = "Recent Trash",
                isTrash = true,
                deletedAt = now - TimeUnit.DAYS.toMillis(5)
            )
        )

        // Expired trash note (35 days old) -> SHOULD be purged
        noteDao.insertNote(
            Note(
                title = "Expired Note",
                isTrash = true,
                deletedAt = now - TimeUnit.DAYS.toMillis(35)
            )
        )

        // Expired trash checklist (32 days old) -> SHOULD be purged
        checklistDao.insertChecklist(
            Checklist(
                title = "Expired Checklist",
                isTrash = true,
                deletedAt = now - TimeUnit.DAYS.toMillis(32)
            )
        )

        val purgedCount = trashService.purgeExpiredItems()
        assertEquals("Should have purged 2 expired items", 2, purgedCount)

        val remainingTrashNotes = trashService.trashNotes.first()
        assertEquals(1, remainingTrashNotes.size)
        assertEquals("Recent Trash", remainingTrashNotes[0].title)

        val remainingTrashChecklists = trashService.trashChecklists.first()
        assertEquals(0, remainingTrashChecklists.size)
    }

    @Test
    fun testDataExportUtilityGeneratesFileWithChecksum() = runBlocking {
        val noteDao = database.noteDao()
        val folderDao = database.folderDao()

        folderDao.insertFolder(Folder(name = "Personal", colorHex = "indigo"))
        noteDao.insertNote(Note(title = "Export Test Note", content = "Export Content", tags = "export"))

        val result = DataExportUtility.generateExportArchive(context, database)
        assertTrue("Archive generation should succeed", result.isSuccess)

        val file = result.getOrNull()
        assertNotNull(file)
        assertTrue("Archive file should exist", file!!.exists())
        assertTrue("File name should have .json extension", file.name.endsWith(".json"))

        val content = file.readText()
        assertTrue("Content should contain Notely", content.contains("Notely"))
        assertTrue("Content should contain checksum", content.contains("checksum"))
        assertTrue("Content should contain Export Test Note", content.contains("Export Test Note"))
    }
}
