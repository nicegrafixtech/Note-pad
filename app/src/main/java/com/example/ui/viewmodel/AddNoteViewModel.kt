package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.NotelyDatabase
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddNoteUiState(
    val title: String = "",
    val content: String = "",
    val selectedFolderId: Long? = null,
    val selectedColorId: String = "default",
    val isPinned: Boolean = false,
    val tags: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean
        get() = title.isNotBlank() || content.isNotBlank()
}

class AddNoteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NotelyDatabase.getDatabase(application)
    private val noteDao = database.noteDao()
    private val folderDao = database.folderDao()
    private val preferences = AppPreferences(application)

    private val _uiState = MutableStateFlow(
        AddNoteUiState(selectedColorId = preferences.defaultNoteColor.value)
    )
    val uiState: StateFlow<AddNoteUiState> = _uiState.asStateFlow()

    val folders: StateFlow<List<Folder>> = folderDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _saveSuccessEvent = MutableSharedFlow<Long>()
    val saveSuccessEvent: SharedFlow<Long> = _saveSuccessEvent.asSharedFlow()

    fun onTitleChange(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle, errorMessage = null)
    }

    fun onContentChange(newContent: String) {
        _uiState.value = _uiState.value.copy(content = newContent, errorMessage = null)
    }

    fun onFolderChange(folderId: Long?) {
        _uiState.value = _uiState.value.copy(selectedFolderId = folderId)
    }

    fun onColorChange(colorId: String) {
        _uiState.value = _uiState.value.copy(selectedColorId = colorId)
    }

    fun onPinToggle() {
        _uiState.value = _uiState.value.copy(isPinned = !_uiState.value.isPinned)
    }

    fun onTagsChange(newTags: String) {
        _uiState.value = _uiState.value.copy(tags = newTags)
    }

    fun saveNote(onSuccess: (Long) -> Unit = {}): kotlinx.coroutines.Job {
        val state = _uiState.value
        val titleTrimmed = state.title.trim()
        val contentTrimmed = state.content.trim()

        if (titleTrimmed.isBlank() && contentTrimmed.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter a title or note content")
            return viewModelScope.launch {}
        }

        _uiState.value = state.copy(isSaving = true)

        return viewModelScope.launch {
            try {
                val newNote = Note(
                    title = titleTrimmed,
                    content = contentTrimmed,
                    folderId = state.selectedFolderId,
                    colorHex = state.selectedColorId,
                    tags = state.tags.trim(),
                    isPinned = state.isPinned,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val generatedId = noteDao.insertNote(newNote)
                _uiState.value = AddNoteUiState(selectedColorId = preferences.defaultNoteColor.value)
                _saveSuccessEvent.emit(generatedId)
                onSuccess(generatedId)
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isSaving = false,
                    errorMessage = "Failed to save note: ${e.localizedMessage}"
                )
            }
        }
    }

    fun resetForm() {
        _uiState.value = AddNoteUiState(selectedColorId = preferences.defaultNoteColor.value)
    }
}
