package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Checklist
import com.example.data.model.Folder
import com.example.data.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Note::class, Checklist::class, Folder::class],
    version = 1,
    exportSchema = false
)
abstract class NotelyDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: NotelyDatabase? = null

        fun getDatabase(context: Context): NotelyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotelyDatabase::class.java,
                    "notely_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default folders on first launch
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.folderDao()?.insertAll(
                                    listOf(
                                        Folder(id = 1, name = "Personal", colorHex = "indigo", icon = "person"),
                                        Folder(id = 2, name = "Work", colorHex = "emerald", icon = "work"),
                                        Folder(id = 3, name = "Ideas", colorHex = "amber", icon = "lightbulb"),
                                        Folder(id = 4, name = "Study", colorHex = "ocean", icon = "school")
                                    )
                                )
                                // Sample welcome note explaining Notely's features
                                INSTANCE?.noteDao()?.insertNote(
                                    Note(
                                        title = "Welcome to Notely! ✨",
                                        content = """# Your New Daily Companion
Notely is designed for fast, elegant notes, to-dos, and sketches.

## Key Features
- **Rich Text**: Format with **bold**, *italic*, <u>underline</u>, headings, and bullet points.
- **Checklists**: Track tasks, set due dates and progress.
- **Drawing Canvas**: Sketch ideas directly in notes with pen, highlighter, and undo/redo.
- **Privacy & Security**: Lock sensitive notes or enable 4-digit App Lock in Settings.
- **Reliable Backups**: 100% offline, versioned JSON exports with SHA-256 validation.

Tap anywhere to edit this note!""".trimIndent(),
                                        folderId = 1,
                                        colorHex = "lavender",
                                        tags = "welcome,quickstart",
                                        isPinned = true
                                    )
                                )
                                // Sample starter checklist
                                INSTANCE?.checklistDao()?.insertChecklist(
                                    Checklist(
                                        title = "Get Started with Notely",
                                        itemsJson = """[
                                            {"id":"1","text":"Create your first note","isCompleted":false,"order":0},
                                            {"id":"2","text":"Try drawing on the canvas","isCompleted":false,"order":1},
                                            {"id":"3","text":"Set up your favorite accent color in Settings","isCompleted":false,"order":2},
                                            {"id":"4","text":"Explore folder organization & search","isCompleted":false,"order":3}
                                        ]""",
                                        dueDate = System.currentTimeMillis() + 86400000L,
                                        dueTime = "18:00",
                                        folderId = 1,
                                        colorHex = "mint",
                                        tags = "todo,starter",
                                        isPinned = true
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
