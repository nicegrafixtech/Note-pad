package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.ui.components.AddNoteForm
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard
import com.example.ui.components.NotesLazyColumn
import com.example.ui.components.QuickCaptureFab
import com.example.ui.viewmodel.AddNoteViewModel
import com.example.ui.viewmodel.NotelyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotelyViewModel,
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit,
    onNewChecklist: () -> Unit,
    onNewDrawing: () -> Unit,
    onNavigateToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var showAddNoteFormDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showVoiceMemoDialog by remember { mutableStateOf(false) }
    var voiceMemoTitle by remember { mutableStateOf("") }

    // Filter notes by selected folder and real-time search query
    val filteredNotes = remember(activeNotes, selectedFolderId) {
        if (selectedFolderId == null) activeNotes
        else activeNotes.filter { it.folderId == selectedFolderId }
    }

    val searchFilteredNotes = remember(filteredNotes, searchQuery) {
        if (searchQuery.isBlank()) {
            filteredNotes
        } else {
            val query = searchQuery.trim().lowercase()
            filteredNotes.filter { note ->
                note.title.lowercase().contains(query) || note.content.lowercase().contains(query)
            }
        }
    }

    val pinnedNotes = remember(searchFilteredNotes) { searchFilteredNotes.filter { it.isPinned } }
    val unpinnedNotes = remember(searchFilteredNotes) { searchFilteredNotes.filter { !it.isPinned } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("notes_snackbar_host")
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Notely",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Notes",
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddNoteFormDialog = true },
                        modifier = Modifier.testTag("quick_add_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Quick Add Note"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.preferences.setViewMode(if (viewMode == "grid") "list" else "grid")
                        },
                        modifier = Modifier.testTag("toggle_view_mode_button")
                    ) {
                        Icon(
                            imageVector = if (viewMode == "grid") Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle view mode"
                        )
                    }
                    IconButton(onClick = onNavigateToTrash) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Trash"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            QuickCaptureFab(
                onNewNote = onNewNote,
                onNewChecklist = onNewChecklist,
                onNewDrawing = onNewDrawing,
                onVoiceMemo = { showVoiceMemoDialog = true },
                onPasteClipboard = {
                    val clip = clipboardManager.getText()?.text
                    if (!clip.isNullOrBlank()) {
                        viewModel.pasteFromClipboard(clip)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar at the top of notes list screen (filters in real-time based on title or content)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("notes_search_bar"),
                placeholder = {
                    Text("Search notes by title or content...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            )

            // Folder chip row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { viewModel.selectedFolderId.value = null },
                    label = { Text("All Notes") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                folders.forEach { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { viewModel.selectedFolderId.value = folder.id },
                        label = { Text(folder.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Add Folder button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { showNewFolderDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Folder",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Folder",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (searchFilteredNotes.isEmpty()) {
                if (searchQuery.isNotBlank()) {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = "No matching notes",
                        description = "No notes matched \"$searchQuery\". Try checking for typos or searching with different keywords.",
                        actionButtonText = "Clear Search",
                        onActionClick = { searchQuery = "" },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    EmptyStateView(
                        icon = Icons.Default.Description,
                        title = if (selectedFolderId != null) "Folder is empty" else "No notes yet",
                        description = "Capture thoughts, organize ideas, add checklists, or sketch ideas freely.",
                        actionButtonText = "+ Create First Note",
                        onActionClick = onNewNote,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                if (viewMode == "grid") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Text(
                                    text = "PINNED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(pinnedNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    folder = folders.find { it.id == note.folderId },
                                    onClick = { onNoteClick(note.id) },
                                    onTogglePin = { viewModel.togglePinNote(note.id) },
                                    onArchive = { viewModel.toggleArchiveNote(note.id) },
                                    onDelete = {
                                        viewModel.moveToTrashNote(note.id)
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            val result = snackbarHostState.showSnackbar(
                                                message = if (note.title.isNotBlank()) "Deleted \"${note.title}\"" else "Note deleted",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreNote(note.id)
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        if (unpinnedNotes.isNotEmpty()) {
                            if (pinnedNotes.isNotEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    Text(
                                        text = "OTHERS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                            }
                            items(unpinnedNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    folder = folders.find { it.id == note.folderId },
                                    onClick = { onNoteClick(note.id) },
                                    onTogglePin = { viewModel.togglePinNote(note.id) },
                                    onArchive = { viewModel.toggleArchiveNote(note.id) },
                                    onDelete = {
                                        viewModel.moveToTrashNote(note.id)
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            val result = snackbarHostState.showSnackbar(
                                                message = if (note.title.isNotBlank()) "Deleted \"${note.title}\"" else "Note deleted",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreNote(note.id)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    NotesLazyColumn(
                        notes = searchFilteredNotes,
                        folders = folders,
                        onNoteClick = onNoteClick,
                        onTogglePin = { viewModel.togglePinNote(it) },
                        onArchive = { viewModel.toggleArchiveNote(it) },
                        onDelete = { noteId ->
                            val targetNote = searchFilteredNotes.find { it.id == noteId }
                            viewModel.moveToTrashNote(noteId)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = if (targetNote != null && targetNote.title.isNotBlank()) "Deleted \"${targetNote.title}\"" else "Note deleted",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreNote(noteId)
                                }
                            }
                        },
                        onSwipeDelete = { note ->
                            viewModel.moveToTrashNote(note.id)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = if (note.title.isNotBlank()) "Deleted \"${note.title}\"" else "Note deleted",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreNote(note.id)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Quick Add Note Form Dialog
    if (showAddNoteFormDialog) {
        val addNoteVm: AddNoteViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddNoteFormDialog = false }) {
            AddNoteForm(
                viewModel = addNoteVm,
                onNoteSaved = { newId ->
                    showAddNoteFormDialog = false
                    onNoteClick(newId)
                },
                onCancel = { showAddNoteFormDialog = false }
            )
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Voice Memo Dialog
    if (showVoiceMemoDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceMemoDialog = false },
            title = { Text("Record Voice Memo") },
            text = {
                Column {
                    Text(
                        text = "Enter a title or quick summary for your audio memo:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = voiceMemoTitle,
                        onValueChange = { voiceMemoTitle = it },
                        placeholder = { Text("e.g. Project Brainstorm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createVoiceMemo(voiceMemoTitle, 45) // 45 seconds sample voice note
                        voiceMemoTitle = ""
                        showVoiceMemoDialog = false
                    }
                ) {
                    Text("Save Memo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceMemoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
