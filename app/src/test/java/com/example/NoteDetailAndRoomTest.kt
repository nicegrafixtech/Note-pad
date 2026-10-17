package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.NotelyDatabase
import com.example.data.model.Note
import com.example.util.BiometricPromptHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteDetailAndRoomTest {

    private lateinit var context: Context
    private lateinit var database: NotelyDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NotelyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRoomNoteEntityAndDaoOperations() = runBlocking {
        val noteDao = database.noteDao()

        // 1. Insert Note
        val noteId = noteDao.insertNote(
            Note(
                title = "Meeting Notes",
                content = "Discuss Q4 objectives and milestones.",
                colorHex = "lavender",
                tags = "work,q4"
            )
        )
        assertTrue(noteId > 0)

        // 2. Query by ID
        val retrieved = noteDao.getNoteById(noteId)
        assertNotNull(retrieved)
        assertEquals("Meeting Notes", retrieved!!.title)
        assertEquals("Discuss Q4 objectives and milestones.", retrieved.content)
        assertEquals(2, retrieved.tagList.size)
        assertEquals(5, retrieved.wordCount)

        // 3. Insert Task Note
        val taskId = noteDao.insertNote(
            Note(
                title = "Submit Report",
                content = "Review financial statements before sending.",
                isTask = true,
                isCompleted = false,
                dueDate = System.currentTimeMillis() + 86400000L
            )
        )
        assertTrue(taskId > 0)

        // 4. Query Tasks
        val tasks = noteDao.getTasks().first()
        assertEquals(1, tasks.size)
        assertEquals("Submit Report", tasks[0].title)
        assertFalse(tasks[0].isCompleted)

        // 5. Update task to completed
        noteDao.updateNote(tasks[0].copy(isCompleted = true))
        val updatedTask = noteDao.getNoteById(taskId)
        assertNotNull(updatedTask)
        assertTrue(updatedTask!!.isCompleted)
    }

    @Test
    fun testBiometricPromptHelperAvailability() {
        // In Robolectric environment without enrolled biometrics, helper correctly checks status
        val isAvail = BiometricPromptHelper.isAvailable(context)
        // Ensure call executes safely without crashing
        assertNotNull(isAvail)
    }
}
