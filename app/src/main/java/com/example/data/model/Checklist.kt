package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val order: Int = 0
)

@Entity(tableName = "checklists")
data class Checklist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val itemsJson: String = "[]",
    val dueDate: Long? = null, // timestamp in millis
    val dueTime: String? = null, // "HH:mm" e.g. "17:00"
    val hasReminder: Boolean = false,
    val folderId: Long? = null,
    val colorHex: String = "default",
    val tags: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrash: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val items: List<ChecklistItem>
        get() {
            if (itemsJson.isBlank() || itemsJson == "[]") return emptyList()
            return try {
                val array = JSONArray(itemsJson)
                val list = mutableListOf<ChecklistItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ChecklistItem(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            text = obj.optString("text", ""),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            order = obj.optInt("order", i)
                        )
                    )
                }
                list.sortedBy { it.order }
            } catch (e: Exception) {
                emptyList()
            }
        }

    val completedCount: Int
        get() = items.count { it.isCompleted }

    val totalCount: Int
        get() = items.size

    val progress: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    val isAllCompleted: Boolean
        get() = totalCount > 0 && completedCount == totalCount

    val tagList: List<String>
        get() = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    companion object {
        fun serializeItems(items: List<ChecklistItem>): String {
            val array = JSONArray()
            items.forEachIndexed { index, item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("text", item.text)
                obj.put("isCompleted", item.isCompleted)
                obj.put("order", index)
                array.put(obj)
            }
            return array.toString()
        }
    }
}
