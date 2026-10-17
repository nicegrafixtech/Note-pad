package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Checklist
import com.example.data.model.ChecklistItem
import com.example.ui.components.ChecklistCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.QuickCaptureFab
import com.example.ui.viewmodel.NotelyViewModel
import com.example.ui.viewmodel.TodoFilter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistsScreen(
    viewModel: NotelyViewModel,
    onChecklistClick: (Long) -> Unit,
    onNewChecklist: () -> Unit,
    onNewNote: () -> Unit,
    onNewDrawing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChecklists by viewModel.activeChecklists.collectAsStateWithLifecycle()
    val todoFilter by viewModel.todoFilter.collectAsStateWithLifecycle()

    var quickTaskTitle by remember { mutableStateOf("") }
    var isCompletedSectionExpanded by remember { mutableStateOf(false) }

    // Filter checklists by selected tab
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endOfToday = startOfToday + 86400000L

    val filteredList = remember(activeChecklists, todoFilter) {
        when (todoFilter) {
            TodoFilter.TODAY -> activeChecklists.filter {
                val d = it.dueDate
                d != null && d >= startOfToday && d < endOfToday
            }
            TodoFilter.UPCOMING -> activeChecklists.filter {
                val d = it.dueDate
                d != null && d >= endOfToday
            }
            TodoFilter.COMPLETED -> activeChecklists.filter { it.isAllCompleted }
            TodoFilter.ALL -> activeChecklists
        }
    }

    val incompletedChecklists = remember(filteredList) {
        if (todoFilter == TodoFilter.COMPLETED) filteredList else filteredList.filter { !it.isAllCompleted }
    }
    val completedChecklists = remember(filteredList) {
        if (todoFilter == TodoFilter.COMPLETED) emptyList() else filteredList.filter { it.isAllCompleted }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "To-Do & Tasks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
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
                onVoiceMemo = {
                    viewModel.createVoiceMemo("Task Memo", 30)
                },
                onPasteClipboard = {
                    // Quick paste
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = todoFilter == TodoFilter.ALL,
                    onClick = { viewModel.todoFilter.value = TodoFilter.ALL },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = todoFilter == TodoFilter.TODAY,
                    onClick = { viewModel.todoFilter.value = TodoFilter.TODAY },
                    label = { Text("Today") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = todoFilter == TodoFilter.UPCOMING,
                    onClick = { viewModel.todoFilter.value = TodoFilter.UPCOMING },
                    label = { Text("Upcoming") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = todoFilter == TodoFilter.COMPLETED,
                    onClick = { viewModel.todoFilter.value = TodoFilter.COMPLETED },
                    label = { Text("Completed") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Quick task inline adder
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickTaskTitle,
                        onValueChange = { quickTaskTitle = it },
                        placeholder = { Text("Quick add task or checklist...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_task_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (quickTaskTitle.isNotBlank()) {
                                val item = ChecklistItem(text = quickTaskTitle.trim(), isCompleted = false)
                                viewModel.saveChecklist(
                                    Checklist(
                                        title = quickTaskTitle.trim(),
                                        itemsJson = Checklist.serializeItems(listOf(item)),
                                        dueDate = if (todoFilter == TodoFilter.TODAY) System.currentTimeMillis() else null
                                    )
                                )
                                quickTaskTitle = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Add quick task",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.CheckBox,
                    title = when (todoFilter) {
                        TodoFilter.TODAY -> "No tasks due today"
                        TodoFilter.UPCOMING -> "No upcoming tasks"
                        TodoFilter.COMPLETED -> "No completed checklists"
                        TodoFilter.ALL -> "No checklists yet"
                    },
                    description = "Stay productive! Create structured checklists with due dates and reorderable steps.",
                    actionButtonText = "+ Create Checklist",
                    onActionClick = onNewChecklist,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(incompletedChecklists, key = { it.id }) { cl ->
                        ChecklistCard(
                            checklist = cl,
                            onClick = { onChecklistClick(cl.id) },
                            onToggleItem = { itemId -> viewModel.toggleChecklistItem(cl.id, itemId) },
                            onTogglePin = { viewModel.togglePinChecklist(cl.id) },
                            onArchive = { viewModel.toggleArchiveChecklist(cl.id) },
                            onDelete = { viewModel.moveToTrashChecklist(cl.id) }
                        )
                    }

                    // Completed Section (collapsible)
                    if (completedChecklists.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isCompletedSectionExpanded = !isCompletedSectionExpanded }
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Completed (${completedChecklists.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = if (isCompletedSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle completed"
                                    )
                                }
                            }
                        }

                        if (isCompletedSectionExpanded) {
                            items(completedChecklists, key = { it.id }) { cl ->
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
}
