package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.NotelyDatabase
import com.example.data.model.Note
import com.example.data.repository.NotelyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchAndSwipeDeleteTest {

    private lateinit var application: Application
    private lateinit var database: NotelyDatabase
    private lateinit var repository: NotelyRepository

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        database = androidx.room.Room.inMemoryDatabaseBuilder(application, NotelyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NotelyRepository(database, com.example.data.preferences.AppPreferences(application), application)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRealtimeSearchFilteringByTitleAndContent() = runBlocking {
        val note1 = Note(title = "Android Compose Guide", content = "Learn declarative UI layouts.")
        val note2 = Note(title = "Shopping List", content = "Buy groceries including milk and apples.")
        val note3 = Note(title = "Workout Routine", content = "Gym session focused on cardio and core.")

        val id1 = repository.saveNote(note1)
        val id2 = repository.saveNote(note2)
        val id3 = repository.saveNote(note3)

        val allNotes = repository.activeNotes.first()
        assertEquals(3, allNotes.size)

        // 1. Search by title match: "compose"
        val query1 = "compose".lowercase()
        val matchTitle = allNotes.filter {
            it.title.lowercase().contains(query1) || it.content.lowercase().contains(query1)
        }
        assertEquals(1, matchTitle.size)
        assertEquals("Android Compose Guide", matchTitle[0].title)

        // 2. Search by content match: "groceries"
        val query2 = "groceries".lowercase()
        val matchContent = allNotes.filter {
            it.title.lowercase().contains(query2) || it.content.lowercase().contains(query2)
        }
        assertEquals(1, matchContent.size)
        assertEquals("Shopping List", matchContent[0].title)

        // 3. Search matching multiple: "and" (found in "Android" and in "cardio and core")
        val query3 = "and".lowercase()
        val matchMultiple = allNotes.filter {
            it.title.lowercase().contains(query3) || it.content.lowercase().contains(query3)
        }
        assertTrue(matchMultiple.size >= 2)

        // 4. Search no match
        val query4 = "nonexistentkeyword".lowercase()
        val matchNone = allNotes.filter {
            it.title.lowercase().contains(query4) || it.content.lowercase().contains(query4)
        }
        assertEquals(0, matchNone.size)
    }

    @Test
    fun testSwipeDeleteAndUndoRestore() = runBlocking {
        val note = Note(title = "Temporary Note", content = "Will be deleted and restored via undo.")
        val noteId = repository.saveNote(note)

        // Note is active
        var activeList = repository.activeNotes.first()
        assertTrue(activeList.any { it.id == noteId })

        // Swipe-to-delete moves to trash
        repository.moveToTrashNote(noteId)

        activeList = repository.activeNotes.first()
        assertFalse("Deleted note should not be in active notes", activeList.any { it.id == noteId })

        val trashList = repository.trashNotes.first()
        assertTrue("Deleted note should be in trash", trashList.any { it.id == noteId })

        // Undo action restores note
        repository.restoreNoteFromTrash(noteId)

        activeList = repository.activeNotes.first()
        assertTrue("Restored note should be back in active notes", activeList.any { it.id == noteId })
    }
}
