package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

data class BackupPreview(
    val schemaVersion: Int,
    val notesCount: Int,
    val checklistsCount: Int,
    val foldersCount: Int,
    val isValid: Boolean,
    val isChecksumValid: Boolean,
    val errorMessage: String? = null,
    val rawJson: String = ""
)

object BackupManager {
    const val CURRENT_SCHEMA_VERSION = 1
    const val APP_IDENTIFIER = "Notely"

    fun calculateSha256(content: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(content.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun exportToJson(
        notes: List<Note>,
        checklists: List<Checklist>,
        folders: List<Folder>
    ): String {
        val dataObj = JSONObject()

        // Notes array
        val notesArray = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("folderId", note.folderId ?: JSONObject.NULL)
                put("colorHex", note.colorHex)
                put("tags", note.tags)
                put("isPinned", note.isPinned)
                put("isArchived", note.isArchived)
                put("isLocked", note.isLocked)
                put("isTrash", note.isTrash)
                put("deletedAt", note.deletedAt ?: JSONObject.NULL)
                put("drawingPath", note.drawingPath ?: JSONObject.NULL)
                put("titleFontSize", note.titleFontSize.toDouble())
                put("bodyFontSize", note.bodyFontSize.toDouble())
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
            }
            notesArray.put(obj)
        }

        // Checklists array
        val checklistsArray = JSONArray()
        checklists.forEach { cl ->
            val obj = JSONObject().apply {
                put("id", cl.id)
                put("title", cl.title)
                put("itemsJson", cl.itemsJson)
                put("dueDate", cl.dueDate ?: JSONObject.NULL)
                put("dueTime", cl.dueTime ?: JSONObject.NULL)
                put("hasReminder", cl.hasReminder)
                put("folderId", cl.folderId ?: JSONObject.NULL)
                put("colorHex", cl.colorHex)
                put("tags", cl.tags)
                put("isPinned", cl.isPinned)
                put("isArchived", cl.isArchived)
                put("isTrash", cl.isTrash)
                put("deletedAt", cl.deletedAt ?: JSONObject.NULL)
                put("createdAt", cl.createdAt)
                put("updatedAt", cl.updatedAt)
            }
            checklistsArray.put(obj)
        }

        // Folders array
        val foldersArray = JSONArray()
        folders.forEach { folder ->
            val obj = JSONObject().apply {
                put("id", folder.id)
                put("name", folder.name)
                put("colorHex", folder.colorHex)
                put("icon", folder.icon)
                put("createdAt", folder.createdAt)
            }
            foldersArray.put(obj)
        }

        dataObj.put("notes", notesArray)
        dataObj.put("checklists", checklistsArray)
        dataObj.put("folders", foldersArray)

        // Compute checksum of payload data string
        val payloadString = dataObj.toString()
        val checksum = calculateSha256(payloadString)

        val root = JSONObject().apply {
            put("app", APP_IDENTIFIER)
            put("schemaVersion", CURRENT_SCHEMA_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("checksum", checksum)
            put("data", dataObj)
        }

        return root.toString(2)
    }

    fun parseAndValidate(jsonString: String): BackupPreview {
        if (jsonString.isBlank()) {
            return BackupPreview(0, 0, 0, 0, isValid = false, isChecksumValid = false, errorMessage = "File is empty.")
        }
        return try {
            val root = JSONObject(jsonString)
            val app = root.optString("app", "")
            if (app != APP_IDENTIFIER && app != "NotelyBackup") {
                return BackupPreview(
                    0, 0, 0, 0,
                    isValid = false,
                    isChecksumValid = false,
                    errorMessage = "Not a recognized Notely backup file (missing or invalid 'app' field)."
                )
            }

            val schemaVersion = root.optInt("schemaVersion", -1)
            if (schemaVersion <= 0) {
                return BackupPreview(
                    0, 0, 0, 0,
                    isValid = false,
                    isChecksumValid = false,
                    errorMessage = "Unsupported schema version: $schemaVersion"
                )
            }

            val expectedChecksum = root.optString("checksum", "")
            val dataObj = root.optJSONObject("data")
                ?: return BackupPreview(
                    schemaVersion, 0, 0, 0,
                    isValid = false,
                    isChecksumValid = false,
                    errorMessage = "Missing 'data' object in backup file."
                )

            val actualChecksum = calculateSha256(dataObj.toString())
            val isChecksumMatch = expectedChecksum.isNotEmpty() && expectedChecksum.equals(actualChecksum, ignoreCase = true)

            val notesArray = dataObj.optJSONArray("notes")
            val checklistsArray = dataObj.optJSONArray("checklists")
            val foldersArray = dataObj.optJSONArray("folders")

            val notesCount = notesArray?.length() ?: 0
            val checklistsCount = checklistsArray?.length() ?: 0
            val foldersCount = foldersArray?.length() ?: 0

            BackupPreview(
                schemaVersion = schemaVersion,
                notesCount = notesCount,
                checklistsCount = checklistsCount,
                foldersCount = foldersCount,
                isValid = true,
                isChecksumValid = isChecksumMatch,
                errorMessage = if (!isChecksumMatch) "Checksum mismatch! File may have been edited or corrupted." else null,
                rawJson = jsonString
            )
        } catch (e: Exception) {
            BackupPreview(
                schemaVersion = 0,
                notesCount = 0,
                checklistsCount = 0,
                foldersCount = 0,
                isValid = false,
                isChecksumValid = false,
                errorMessage = "Invalid JSON format: ${e.localizedMessage ?: "Unknown parse error"}"
            )
        }
    }

    fun extractData(jsonString: String): Triple<List<Note>, List<Checklist>, List<Folder>> {
        val root = JSONObject(jsonString)
        val dataObj = root.getJSONObject("data")

        val notes = mutableListOf<Note>()
        val notesArray = dataObj.optJSONArray("notes")
        if (notesArray != null) {
            for (i in 0 until notesArray.length()) {
                val obj = notesArray.getJSONObject(i)
                notes.add(
                    Note(
                        id = 0, // Will be generated on insert or re-keyed
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        folderId = if (obj.isNull("folderId")) null else obj.optLong("folderId"),
                        colorHex = obj.optString("colorHex", "default"),
                        tags = obj.optString("tags", ""),
                        isPinned = obj.optBoolean("isPinned", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        isLocked = obj.optBoolean("isLocked", false),
                        isTrash = obj.optBoolean("isTrash", false),
                        deletedAt = if (obj.isNull("deletedAt")) null else obj.optLong("deletedAt"),
                        drawingPath = if (obj.isNull("drawingPath")) null else obj.optString("drawingPath"),
                        titleFontSize = obj.optDouble("titleFontSize", 22.0).toFloat(),
                        bodyFontSize = obj.optDouble("bodyFontSize", 16.0).toFloat(),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val checklists = mutableListOf<Checklist>()
        val checklistsArray = dataObj.optJSONArray("checklists")
        if (checklistsArray != null) {
            for (i in 0 until checklistsArray.length()) {
                val obj = checklistsArray.getJSONObject(i)
                checklists.add(
                    Checklist(
                        id = 0,
                        title = obj.optString("title", ""),
                        itemsJson = obj.optString("itemsJson", "[]"),
                        dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate"),
                        dueTime = if (obj.isNull("dueTime")) null else obj.optString("dueTime"),
                        hasReminder = obj.optBoolean("hasReminder", false),
                        folderId = if (obj.isNull("folderId")) null else obj.optLong("folderId"),
                        colorHex = obj.optString("colorHex", "default"),
                        tags = obj.optString("tags", ""),
                        isPinned = obj.optBoolean("isPinned", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        isTrash = obj.optBoolean("isTrash", false),
                        deletedAt = if (obj.isNull("deletedAt")) null else obj.optLong("deletedAt"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val folders = mutableListOf<Folder>()
        val foldersArray = dataObj.optJSONArray("folders")
        if (foldersArray != null) {
            for (i in 0 until foldersArray.length()) {
                val obj = foldersArray.getJSONObject(i)
                folders.add(
                    Folder(
                        id = 0,
                        name = obj.optString("name", "Untitled Folder"),
                        colorHex = obj.optString("colorHex", "indigo"),
                        icon = obj.optString("icon", "folder"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return Triple(notes, checklists, folders)
    }
}
