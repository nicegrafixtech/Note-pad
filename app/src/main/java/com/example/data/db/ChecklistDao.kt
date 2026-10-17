package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Checklist
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklists WHERE isTrash = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveChecklists(): Flow<List<Checklist>>

    @Query("SELECT * FROM checklists WHERE isTrash = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedChecklists(): Flow<List<Checklist>>

    @Query("SELECT * FROM checklists WHERE isTrash = 1 ORDER BY deletedAt DESC")
    fun getTrashChecklists(): Flow<List<Checklist>>

    @Query("SELECT * FROM checklists WHERE id = :id LIMIT 1")
    suspend fun getChecklistById(id: Long): Checklist?

    @Query("SELECT * FROM checklists WHERE id = :id LIMIT 1")
    fun getChecklistByIdFlow(id: Long): Flow<Checklist?>

    @Query("SELECT * FROM checklists")
    suspend fun getAllChecklists(): List<Checklist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: Checklist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(checklists: List<Checklist>)

    @Update
    suspend fun updateChecklist(checklist: Checklist)

    @Delete
    suspend fun deleteChecklist(checklist: Checklist)

    @Query("DELETE FROM checklists WHERE id = :id")
    suspend fun deleteChecklistById(id: Long)

    @Query("DELETE FROM checklists WHERE isTrash = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM checklists")
    suspend fun clearAll()
}
