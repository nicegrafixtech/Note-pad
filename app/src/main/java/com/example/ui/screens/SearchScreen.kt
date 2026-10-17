package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Checklist
import com.example.data.model.Note
import com.example.ui.components.ChecklistCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard
import com.example.ui.viewmodel.NotelyViewModel
import com.example.ui.viewmodel.SearchTypeFilter
import com.example.ui.viewmodel.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: NotelyViewModel,
    onNoteClick: (Long) -> Unit,
    onChecklistClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val activeChecklists by viewModel.activeChecklists.collectAsStateWithLifecycle()
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()

    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.searchTypeFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    // Matching Notes
    val matchedNotes = remember(activeNotes, query, typeFilter, sortOption) {
        if (typeFilter == SearchTypeFilter.CHECKLISTS) emptyList()
        else {
            val q = query.trim().lowercase()
            val list = if (q.isEmpty()) activeNotes else {
                activeNotes.filter { note ->
                    note.title.lowercase().contains(q) ||
                            note.content.lowercase().contains(q) ||
                            note.tags.lowercase().contains(q)
                }
            }
            when (sortOption) {
                SortOption.UPDATED_DESC -> list.sortedByDescending { it.updatedAt }
                SortOption.CREATED_DESC -> list.sortedByDescending { it.createdAt }
                SortOption.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
                SortOption.COLOR -> list.sortedBy { it.colorHex }
            }
        }
    }

    // Matching Checklists
    val matchedChecklists = remember(activeChecklists, query, typeFilter, sortOption) {
        if (typeFilter == SearchTypeFilter.NOTES) emptyList()
        else {
            val q = query.trim().lowercase()
            val list = if (q.isEmpty()) activeChecklists else {
                activeChecklists.filter { cl ->
                    cl.title.lowercase().contains(q) ||
                            cl.tags.lowercase().contains(q) ||
                            cl.items.any { it.text.lowercase().contains(q) }
                }
            }
            when (sortOption) {
                SortOption.UPDATED_DESC -> list.sortedByDescending { it.updatedAt }
                SortOption.CREATED_DESC -> list.sortedByDescending { it.createdAt }
                SortOption.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
                SortOption.COLOR -> list.sortedBy { it.colorHex }
            }
        }
    }

    val totalMatches = matchedNotes.size + matchedChecklists.size

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Search & Filter", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search by keywords, tags, or content...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_text_input")
            )

            // Filter Chips (Type)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = typeFilter == SearchTypeFilter.ALL,
                    onClick = { viewModel.searchTypeFilter.value = SearchTypeFilter.ALL },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = typeFilter == SearchTypeFilter.NOTES,
                    onClick = { viewModel.searchTypeFilter.value = SearchTypeFilter.NOTES },
                    label = { Text("Notes (${activeNotes.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = typeFilter == SearchTypeFilter.CHECKLISTS,
                    onClick = { viewModel.searchTypeFilter.value = SearchTypeFilter.CHECKLISTS },
                    label = { Text("Checklists (${activeChecklists.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Sort selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sort:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = sortOption == SortOption.UPDATED_DESC,
                    onClick = { viewModel.sortOption.value = SortOption.UPDATED_DESC },
                    label = { Text("Recent") }
                )
                FilterChip(
                    selected = sortOption == SortOption.TITLE_ASC,
                    onClick = { viewModel.sortOption.value = SortOption.TITLE_ASC },
                    label = { Text("Title A-Z") }
                )
                FilterChip(
                    selected = sortOption == SortOption.COLOR,
                    onClick = { viewModel.sortOption.value = SortOption.COLOR },
                    label = { Text("Color") }
                )
            }

            // Results count banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (query.isBlank()) "Showing all items ($totalMatches)" else "Found $totalMatches result(s)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (totalMatches == 0) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = "No matches found",
                    description = "Try searching for a different keyword, title, or clearing filter chips.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (matchedNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = "NOTES (${matchedNotes.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(matchedNotes, key = { "note_${it.id}" }) { note ->
                            NoteCard(
                                note = note,
                                folder = folders.find { it.id == note.folderId },
                                onClick = { onNoteClick(note.id) },
                                onTogglePin = { viewModel.togglePinNote(note.id) },
                                onArchive = { viewModel.toggleArchiveNote(note.id) },
                                onDelete = { viewModel.moveToTrashNote(note.id) }
                            )
                        }
                    }

                    if (matchedChecklists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "CHECKLISTS (${matchedChecklists.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(matchedChecklists, key = { "cl_${it.id}" }) { cl ->
                            ChecklistCard(
                                checklist = cl,
                                onClick = { onChecklistClick(cl.id) },
                                onToggleItem = { itemId -> viewModel.toggleChecklistItem(cl.id, itemId) },
                                onTogglePin = { viewModel.togglePinChecklist(cl.id) },
                                onArchive = { viewModel.toggleArchiveChecklist(cl.id) },
                                onDelete = { viewModel.moveToTrashChecklist(cl.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
