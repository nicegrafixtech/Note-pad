package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Checklist
import com.example.data.model.Note
import com.example.data.service.TrashService
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.NotelyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: NotelyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val trashNotes by viewModel.trashNotes.collectAsStateWithLifecycle()
    val trashChecklists by viewModel.trashChecklists.collectAsStateWithLifecycle()

    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var noteToDeletePermanently by remember { mutableStateOf<Note?>(null) }
    var checklistToDeletePermanently by remember { mutableStateOf<Checklist?>(null) }

    val totalTrash = trashNotes.size + trashChecklists.size

    // Automatically purge expired items upon viewing trash
    LaunchedEffect(Unit) {
        viewModel.trashService.purgeExpiredItems()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Trash & 30-Day Recovery", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (totalTrash > 0) {
                        TextButton(
                            onClick = { showEmptyTrashDialog = true },
                            modifier = Modifier.testTag("empty_trash_button")
                        ) {
                            Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                        }
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
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Items are preserved for 30 days, then automatically purged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (totalTrash == 0) {
                EmptyStateView(
                    icon = Icons.Default.Delete,
                    title = "Trash is Empty",
                    description = "Soft-deleted notes and checklists are kept here with 30-day automatic expiration.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Notes in Trash
                    items(trashNotes, key = { "trash_note_${it.id}" }) { note ->
                        val remainingText = TrashService.formatRemainingTime(note.deletedAt)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (note.title.isNotBlank()) note.title else "Untitled Note",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Note • $remainingText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.trashService.restoreNote(note.id)
                                            }
                                        },
                                        modifier = Modifier.testTag("restore_note_${note.id}")
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore Note", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { noteToDeletePermanently = note },
                                        modifier = Modifier.testTag("delete_permanently_note_${note.id}")
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "Permanently Delete Note", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Checklists in Trash
                    items(trashChecklists, key = { "trash_cl_${it.id}" }) { cl ->
                        val remainingText = TrashService.formatRemainingTime(cl.deletedAt)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (cl.title.isNotBlank()) cl.title else "Untitled Checklist",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Checklist (${cl.totalCount} items) • $remainingText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.trashService.restoreChecklist(cl.id)
                                            }
                                        },
                                        modifier = Modifier.testTag("restore_checklist_${cl.id}")
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore Checklist", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { checklistToDeletePermanently = cl },
                                        modifier = Modifier.testTag("delete_permanently_checklist_${cl.id}")
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "Permanently Delete Checklist", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation: Permanently Delete Note
    if (noteToDeletePermanently != null) {
        val note = noteToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { noteToDeletePermanently = null },
            title = { Text("Permanently Delete Note?") },
            text = { Text("Are you sure you want to permanently delete \"${if (note.title.isNotBlank()) note.title else "Untitled Note"}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.trashService.permanentlyDeleteNote(note.id)
                        }
                        noteToDeletePermanently = null
                    }
                ) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDeletePermanently = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation: Permanently Delete Checklist
    if (checklistToDeletePermanently != null) {
        val cl = checklistToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { checklistToDeletePermanently = null },
            title = { Text("Permanently Delete Checklist?") },
            text = { Text("Are you sure you want to permanently delete \"${if (cl.title.isNotBlank()) cl.title else "Untitled Checklist"}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.trashService.permanentlyDeleteChecklist(cl.id)
                        }
                        checklistToDeletePermanently = null
                    }
                ) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { checklistToDeletePermanently = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation: Empty Entire Trash
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash?") },
            text = { Text("All $totalTrash items in the trash will be permanently deleted immediately. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.trashService.emptyAllTrash()
                        }
                        showEmptyTrashDialog = false
                    }
                ) {
                    Text("Empty All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
