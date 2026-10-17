package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.NotelyDatabase
import com.example.data.model.BackupPreview
import com.example.data.model.Checklist
import com.example.data.model.ChecklistItem
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NotelyRepository
import com.example.data.repository.RestoreMode
import com.example.data.service.TrashService
import com.example.util.DataExportUtility
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchTypeFilter {
    ALL,
    NOTES,
    CHECKLISTS
}

enum class SortOption {
    UPDATED_DESC,
    CREATED_DESC,
    TITLE_ASC,
    COLOR
}

enum class TodoFilter {
    TODAY,
    UPCOMING,
    ALL,
    COMPLETED
}

class NotelyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NotelyDatabase.getDatabase(application)
    val preferences = AppPreferences(application)
    val repository = NotelyRepository(database, preferences, application)
    val trashService = TrashService(database)

    // UI event flow for Snackbars
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Preferences Flows
    val themeMode: StateFlow<String> = preferences.themeMode
    val accentColor: StateFlow<String> = preferences.accentColor
    val fontScale: StateFlow<Float> = preferences.fontScale
    val defaultNoteColor: StateFlow<String> = preferences.defaultNoteColor
    val viewMode: StateFlow<String> = preferences.viewMode
    val isPinEnabled: StateFlow<Boolean> = preferences.isPinEnabled
    val isBiometricEnabled: StateFlow<Boolean> = preferences.isBiometricEnabled
    val pinCode: StateFlow<String> = preferences.pinCode
    val language: StateFlow<String> = preferences.language
    val autoBackupReminder: StateFlow<Boolean> = preferences.autoBackupReminder
    val lastBackupTime: StateFlow<Long> = preferences.lastBackupTime

    // App Lock runtime unlock state
    private val _isAppUnlocked = MutableStateFlow(!preferences.isPinEnabled.value)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    init {
        // If pin is not enabled, unlock immediately
        if (!preferences.isPinEnabled.value) {
            _isAppUnlocked.value = true
        }

        // Automatic 30-day trash expiration purge on startup
        viewModelScope.launch {
            trashService.purgeExpiredItems()
        }
    }

    fun unlockApp(pin: String): Boolean {
        if (pin == preferences.pinCode.value) {
            _isAppUnlocked.value = true
            return true
        }
        return false
    }

    fun unlockWithBiometrics() {
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        if (preferences.isPinEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    // Repository Flows
    val activeNotes = repository.activeNotes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val archivedNotes = repository.archivedNotes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val trashNotes = repository.trashNotes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activeChecklists = repository.activeChecklists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val archivedChecklists = repository.archivedChecklists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val trashChecklists = repository.trashChecklists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allFolders = repository.allFolders.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Filter & Search states
    val selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedTag = MutableStateFlow<String?>(null)

    val searchQuery = MutableStateFlow("")
    val searchTypeFilter = MutableStateFlow(SearchTypeFilter.ALL)
    val searchColorFilter = MutableStateFlow<String?>(null)
    val searchFolderFilter = MutableStateFlow<Long?>(null)
    val sortOption = MutableStateFlow(SortOption.UPDATED_DESC)

    val todoFilter = MutableStateFlow(TodoFilter.ALL)

    // Note actions
    fun saveNote(note: Note, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveNote(note)
            onSaved(id)
        }
    }

    fun togglePinNote(id: Long) {
        viewModelScope.launch {
            repository.togglePinNote(id)
        }
    }

    fun toggleArchiveNote(id: Long) {
        viewModelScope.launch {
            repository.toggleArchiveNote(id)
            _userMessage.emit("Note archived")
        }
    }

    fun toggleLockNote(id: Long) {
        viewModelScope.launch {
            repository.toggleLockNote(id)
        }
    }

    fun moveToTrashNote(id: Long) {
        viewModelScope.launch {
            repository.moveToTrashNote(id)
            _userMessage.emit("Note moved to Trash")
        }
    }

    fun restoreNote(id: Long) {
        viewModelScope.launch {
            repository.restoreNoteFromTrash(id)
            _userMessage.emit("Note restored")
        }
    }

    fun deleteNotePermanently(id: Long) {
        viewModelScope.launch {
            repository.deleteNotePermanently(id)
            _userMessage.emit("Note permanently deleted")
        }
    }

    // Checklist actions
    fun saveChecklist(checklist: Checklist, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveChecklist(checklist)
            onSaved(id)
        }
    }

    fun togglePinChecklist(id: Long) {
        viewModelScope.launch {
            repository.togglePinChecklist(id)
        }
    }

    fun toggleArchiveChecklist(id: Long) {
        viewModelScope.launch {
            repository.toggleArchiveChecklist(id)
            _userMessage.emit("Checklist archived")
        }
    }

    fun moveToTrashChecklist(id: Long) {
        viewModelScope.launch {
            repository.moveToTrashChecklist(id)
            _userMessage.emit("Checklist moved to Trash")
        }
    }

    fun restoreChecklist(id: Long) {
        viewModelScope.launch {
            repository.restoreChecklistFromTrash(id)
            _userMessage.emit("Checklist restored")
        }
    }

    fun deleteChecklistPermanently(id: Long) {
        viewModelScope.launch {
            repository.deleteChecklistPermanently(id)
            _userMessage.emit("Checklist permanently deleted")
        }
    }

    fun toggleChecklistItem(checklistId: Long, itemId: String) {
        viewModelScope.launch {
            val cl = repository.getChecklistById(checklistId) ?: return@launch
            val updatedItems = cl.items.map { item ->
                if (item.id == itemId) item.copy(isCompleted = !item.isCompleted) else item
            }
            repository.saveChecklist(cl.copy(itemsJson = Checklist.serializeItems(updatedItems)))
        }
    }

    // Folders
    fun createFolder(name: String, colorHex: String = "indigo") {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.addFolder(name.trim(), colorHex)
                _userMessage.emit("Folder '$name' created")
            }
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            repository.deleteFolder(id)
            if (selectedFolderId.value == id) {
                selectedFolderId.value = null
            }
            _userMessage.emit("Folder deleted")
        }
    }

    // Trash
    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            _userMessage.emit("Trash emptied")
        }
    }

    // Quick Capture
    fun pasteFromClipboard(text: String): Long? {
        if (text.isBlank()) return null
        val lines = text.trim().lines()
        val title = lines.firstOrNull()?.take(60) ?: "Clipboard Note"
        val content = if (lines.size > 1) lines.drop(1).joinToString("\n") else text

        var newId: Long = 0
        viewModelScope.launch {
            newId = repository.saveNote(
                Note(
                    title = title,
                    content = content,
                    colorHex = preferences.defaultNoteColor.value,
                    tags = "clipboard"
                )
            )
            _userMessage.emit("Saved clipboard text as note!")
        }
        return newId
    }

    fun createVoiceMemo(title: String, durationSeconds: Int) {
        viewModelScope.launch {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            val durationStr = String.format("%02d:%02d", minutes, seconds)
            repository.saveNote(
                Note(
                    title = if (title.isBlank()) "Voice Memo ($durationStr)" else title,
                    content = "🎙️ Audio Memo recorded.\nDuration: $durationStr\nRecorded on: ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                    colorHex = "amber",
                    tags = "voicememo,audio"
                )
            )
            _userMessage.emit("Voice memo saved!")
        }
    }

    // Backup & Restore
    fun exportAndShareArchive(context: Context) {
        viewModelScope.launch {
            val result = DataExportUtility.generateExportArchive(context, database)
            result.onSuccess { file ->
                DataExportUtility.shareExportArchive(context, file)
                preferences.setLastBackupTime(System.currentTimeMillis())
                _userMessage.emit("Export archive created and ready to share!")
            }.onFailure { error ->
                _userMessage.emit("Export failed: ${error.localizedMessage}")
            }
        }
    }

    fun validateBackup(json: String): BackupPreview {
        return repository.validateBackupJson(json)
    }

    fun restoreBackup(json: String, mode: RestoreMode, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreBackup(json, mode)
            result.onSuccess { message ->
                _userMessage.emit(message)
                onResult(true, message)
            }.onFailure { error ->
                onResult(false, error.localizedMessage ?: "Unknown restore failure")
            }
        }
    }
}
