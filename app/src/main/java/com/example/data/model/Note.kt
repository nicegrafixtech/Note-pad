package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val folderId: Long? = null,
    val colorHex: String = "default", // key into AvailableNoteColors
    val tags: String = "", // comma-separated e.g. "ideas,personal"
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isLocked: Boolean = false,
    val isTrash: Boolean = false,
    val deletedAt: Long? = null,
    val drawingPath: String? = null, // local file path to sketch PNG if any
    val titleFontSize: Float = 22f,
    val bodyFontSize: Float = 16f,
    val isTask: Boolean = false,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val tagList: List<String>
        get() = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val wordCount: Int
        get() {
            val text = content.trim()
            if (text.isEmpty()) return 0
            return text.split("\\s+".toRegex()).count { it.isNotEmpty() }
        }

    val charCount: Int
        get() = content.length
}
