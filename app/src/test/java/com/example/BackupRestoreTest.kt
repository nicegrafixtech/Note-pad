package com.example

import com.example.data.model.BackupManager
import com.example.data.model.Checklist
import com.example.data.model.ChecklistItem
import com.example.data.model.Folder
import com.example.data.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreTest {

    @Test
    fun testValidBackupExportAndValidate() {
        val notes = listOf(
            Note(id = 1, title = "Test Note 1", content = "Content 1", colorHex = "mint", tags = "test"),
            Note(id = 2, title = "Test Note 2", content = "Content 2", isPinned = true)
        )
        val checklists = listOf(
            Checklist(
                id = 1,
                title = "To-Do List",
                itemsJson = Checklist.serializeItems(
                    listOf(
                        ChecklistItem(id = "1", text = "Buy milk", isCompleted = false),
                        ChecklistItem(id = "2", text = "Call dentist", isCompleted = true)
                    )
                )
            )
        )
        val folders = listOf(
            Folder(id = 1, name = "Work", colorHex = "indigo")
        )

        val exportedJson = BackupManager.exportToJson(notes, checklists, folders)
        assertNotNull(exportedJson)
        assertTrue(exportedJson.contains("Notely"))

        val preview = BackupManager.parseAndValidate(exportedJson)
        assertTrue("Preview should be valid", preview.isValid)
        assertTrue("Checksum should be valid", preview.isChecksumValid)
        assertEquals(2, preview.notesCount)
        assertEquals(1, preview.checklistsCount)
        assertEquals(1, preview.foldersCount)

        val (extractedNotes, extractedChecklists, extractedFolders) = BackupManager.extractData(exportedJson)
        assertEquals(2, extractedNotes.size)
        assertEquals("Test Note 1", extractedNotes[0].title)
        assertEquals("mint", extractedNotes[0].colorHex)
        assertEquals(1, extractedChecklists.size)
        assertEquals(2, extractedChecklists[0].items.size)
        assertEquals("Buy milk", extractedChecklists[0].items[0].text)
        assertFalse(extractedChecklists[0].items[0].isCompleted)
        assertTrue(extractedChecklists[0].items[1].isCompleted)
        assertEquals(1, extractedFolders.size)
        assertEquals("Work", extractedFolders[0].name)
    }

    @Test
    fun testEmptyFileValidation() {
        val preview = BackupManager.parseAndValidate("")
        assertFalse("Empty file should be invalid", preview.isValid)
        assertEquals("File is empty.", preview.errorMessage)

        val whitespacePreview = BackupManager.parseAndValidate("    \n  ")
        assertFalse("Whitespace file should be invalid", whitespacePreview.isValid)
    }

    @Test
    fun testCorruptJsonValidation() {
        val corruptJson = "{ \"app\": \"Notely\", \"data\": { \"notes\": [ { broken json"
        val preview = BackupManager.parseAndValidate(corruptJson)
        assertFalse("Corrupt JSON should be invalid", preview.isValid)
        assertNotNull(preview.errorMessage)
        assertTrue(preview.errorMessage!!.contains("Invalid JSON format"))
    }

    @Test
    fun testWrongSchemaVersionValidation() {
        val wrongVersionJson = """
            {
                "app": "Notely",
                "schemaVersion": 0,
                "checksum": "123",
                "data": {}
            }
        """.trimIndent()

        val preview = BackupManager.parseAndValidate(wrongVersionJson)
        assertFalse("Zero or negative schema version should fail", preview.isValid)
        assertTrue(preview.errorMessage!!.contains("Unsupported schema version"))
    }

    @Test
    fun testWrongAppIdentifierValidation() {
        val foreignAppJson = """
            {
                "app": "ForeignApp",
                "schemaVersion": 1,
                "checksum": "123",
                "data": {}
            }
        """.trimIndent()

        val preview = BackupManager.parseAndValidate(foreignAppJson)
        assertFalse("Foreign app backup should fail", preview.isValid)
        assertTrue(preview.errorMessage!!.contains("Not a recognized Notely backup file"))
    }

    @Test
    fun testChecksumMismatchValidation() {
        val notes = listOf(Note(id = 1, title = "Original", content = "Clean"))
        val json = BackupManager.exportToJson(notes, emptyList(), emptyList())

        // Tamper with data content while keeping original checksum
        val tamperedJson = json.replace("Clean", "ModifiedTamperedPayload")
        val preview = BackupManager.parseAndValidate(tamperedJson)

        assertTrue("JSON is still valid structure", preview.isValid)
        assertFalse("Checksum should detect tamper / mismatch", preview.isChecksumValid)
        assertNotNull(preview.errorMessage)
        assertTrue(preview.errorMessage!!.contains("Checksum mismatch"))
    }

    @Test
    fun testChecklistSerialization() {
        val items = listOf(
            ChecklistItem(id = "a1", text = "First item", isCompleted = false, order = 0),
            ChecklistItem(id = "b2", text = "Second item", isCompleted = true, order = 1)
        )
        val serialized = Checklist.serializeItems(items)
        val checklist = Checklist(title = "Test", itemsJson = serialized)

        assertEquals(2, checklist.totalCount)
        assertEquals(1, checklist.completedCount)
        assertEquals(0.5f, checklist.progress, 0.01f)
        assertFalse(checklist.isAllCompleted)
    }
}
