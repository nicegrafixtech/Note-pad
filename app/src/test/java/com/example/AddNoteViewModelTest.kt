package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.NotelyDatabase
import com.example.data.model.Note
import com.example.ui.viewmodel.AddNoteViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddNoteViewModelTest {

    private lateinit var application: Application
    private lateinit var viewModel: AddNoteViewModel
    private lateinit var database: NotelyDatabase

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        database = NotelyDatabase.getDatabase(application)
        viewModel = AddNoteViewModel(application)
    }

    @Test
    fun testInitialStateCannotSave() {
        val state = viewModel.uiState.value
        assertEquals("", state.title)
        assertEquals("", state.content)
        assertFalse("Cannot save empty note", state.canSave)
        assertNull(state.errorMessage)
    }

    @Test
    fun testInputChangesEnableSave() {
        viewModel.onTitleChange("Grocery List")
        assertTrue("Can save with title", viewModel.uiState.value.canSave)

        viewModel.onContentChange("Milk, Eggs, Bread")
        assertEquals("Grocery List", viewModel.uiState.value.title)
        assertEquals("Milk, Eggs, Bread", viewModel.uiState.value.content)

        viewModel.onPinToggle()
        assertTrue("Note should be pinned", viewModel.uiState.value.isPinned)
    }

    @Test
    fun testSaveNoteInsertsToDatabase() = runBlocking {
        viewModel.onTitleChange("Saved Note Test")
        viewModel.onContentChange("This is test content saved from AddNoteViewModel.")

        var savedId: Long = 0L
        val job = viewModel.saveNote { id ->
            savedId = id
        }
        job.join()

        val insertedNote = database.noteDao().getNoteById(savedId)
        assertNotNull("Inserted note should exist in database", insertedNote)
        assertEquals("Saved Note Test", insertedNote!!.title)
        assertEquals("This is test content saved from AddNoteViewModel.", insertedNote.content)

        // Test modifying and saving back
        val modified = insertedNote.copy(title = "Updated Title", content = "Updated Content")
        database.noteDao().updateNote(modified)

        val retrievedModified = database.noteDao().getNoteById(savedId)
        assertNotNull(retrievedModified)
        assertEquals("Updated Title", retrievedModified!!.title)
        assertEquals("Updated Content", retrievedModified.content)
    }
}
