package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.ui.components.ColorPickerRow
import com.example.ui.components.PinEntryPad
import com.example.ui.components.RichTextToolbar
import com.example.ui.components.getNoteContainerColor
import com.example.ui.viewmodel.NotelyViewModel
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NotelyViewModel,
    onBack: () -> Unit,
    onOpenDrawing: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
    val globalFontScale by viewModel.fontScale.collectAsStateWithLifecycle()

    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf(TextFieldValue("")) }
    var selectedColorId by remember { mutableStateOf("default") }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var currentFolderId by remember { mutableStateOf<Long?>(null) }
    var tagsString by remember { mutableStateOf("") }
    var drawingPath by remember { mutableStateOf<String?>(null) }
    var titleFontSize by remember { mutableFloatStateOf(24f) }
    var bodyFontSize by remember { mutableFloatStateOf(16f) }
    var currentNoteId by remember { mutableLongStateOf(noteId) }

    var isLoaded by remember { mutableStateOf(false) }
    var isUnlockedForSession by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    // Load initial note data if editing
    LaunchedEffect(noteId) {
        if (noteId > 0) {
            val existing = viewModel.repository.getNoteById(noteId)
            if (existing != null) {
                noteTitle = existing.title
                noteContent = TextFieldValue(existing.content)
                selectedColorId = existing.colorHex
                isPinned = existing.isPinned
                isArchived = existing.isArchived
                isLocked = existing.isLocked
                currentFolderId = existing.folderId
                tagsString = existing.tags
                drawingPath = existing.drawingPath
                titleFontSize = existing.titleFontSize
                bodyFontSize = existing.bodyFontSize
                currentNoteId = existing.id
            }
        } else {
            selectedColorId = viewModel.preferences.defaultNoteColor.value
            bodyFontSize = 16f * globalFontScale
        }
        isLoaded = true
    }

    // Save note helper
    fun saveCurrentState() {
        if (!isLoaded) return
        val currentTitle = noteTitle.trim()
        val currentText = noteContent.text.trim()
        if (currentTitle.isEmpty() && currentText.isEmpty() && drawingPath == null && currentNoteId == 0L) {
            return
        }

        val note = Note(
            id = currentNoteId,
            title = noteTitle,
            content = noteContent.text,
            folderId = currentFolderId,
            colorHex = selectedColorId,
            tags = tagsString,
            isPinned = isPinned,
            isArchived = isArchived,
            isLocked = isLocked,
            drawingPath = drawingPath,
            titleFontSize = titleFontSize,
            bodyFontSize = bodyFontSize
        )
        viewModel.saveNote(note) { newId ->
            currentNoteId = newId
        }
    }

    // Debounced auto-save every 2.5 seconds
    LaunchedEffect(noteTitle, noteContent.text, selectedColorId, isPinned, isLocked, currentFolderId, tagsString) {
        if (!isLoaded) return@LaunchedEffect
        delay(2500)
        saveCurrentState()
    }

    // Save when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            saveCurrentState()
        }
    }

    BackHandler {
        saveCurrentState()
        onBack()
    }

    // Check if locked and not yet unlocked
    if (isLocked && !isUnlockedForSession && isPinEnabled) {
        var enteredPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }
        val activity = remember(context) { context.findFragmentActivity() }
        val isBiometricAvailable = remember(context) { com.example.util.BiometricHelper.isBiometricAvailable(context) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Locked Note") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { p ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(p),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PinEntryPad(
                    title = "Unlock Note",
                    subtitle = "Enter 4-digit PIN or use Biometrics",
                    pin = enteredPin,
                    onPinChange = { pin ->
                        enteredPin = pin
                        pinError = null
                        if (pin.length == 4) {
                            if (pin == viewModel.preferences.pinCode.value) {
                                isUnlockedForSession = true
                            } else {
                                pinError = "Incorrect PIN"
                                enteredPin = ""
                            }
                        }
                    },
                    errorMessage = pinError
                )

                if (isBiometricAvailable && activity != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            com.example.util.BiometricHelper.authenticate(
                                activity = activity,
                                title = "Unlock Note",
                                subtitle = "Confirm biometrics to view note",
                                onSuccess = { isUnlockedForSession = true },
                                onError = { /* fallback */ }
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Biometrics")
                    }
                }
            }
        }
        return
    }

    val backgroundColor = getNoteContainerColor(selectedColorId)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            saveCurrentState()
                            onBack()
                        },
                        modifier = Modifier.testTag("note_editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Sketch button
                    IconButton(
                        onClick = {
                            saveCurrentState()
                            onOpenDrawing(currentNoteId)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Canvas sketch",
                            tint = if (drawingPath != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Pin toggle
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (isPinned) "Unpin" else "Pin",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Lock toggle
                    if (isPinEnabled) {
                        IconButton(onClick = { isLocked = !isLocked }) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isLocked) "Unlock note" else "Lock note",
                                tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Color palette toggle
                    IconButton(onClick = { showColorPicker = !showColorPicker }) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Change color")
                    }

                    // Overflow Menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isArchived) "Unarchive" else "Archive") },
                                onClick = {
                                    showMenu = false
                                    isArchived = !isArchived
                                    saveCurrentState()
                                    onBack()
                                },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Trash") },
                                onClick = {
                                    showMenu = false
                                    if (currentNoteId > 0) {
                                        viewModel.moveToTrashNote(currentNoteId)
                                    }
                                    onBack()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column {
                // Word and character count status bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val words = if (noteContent.text.isBlank()) 0 else noteContent.text.trim().split("\\s+".toRegex()).size
                        val chars = noteContent.text.length
                        Text(
                            text = "$words words  •  $chars chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Adjust font size button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "A-",
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .clickable {
                                        if (bodyFontSize > 12f) bodyFontSize -= 2f
                                    },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "A+",
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .clickable {
                                        if (bodyFontSize < 28f) bodyFontSize += 2f
                                    },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Rich text toolbar
                RichTextToolbar(
                    textFieldValue = noteContent,
                    onValueChange = { noteContent = it }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Optional Color picker dropdown
            if (showColorPicker) {
                ColorPickerRow(
                    selectedColorId = selectedColorId,
                    onColorSelected = { selectedColorId = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Folder & Tag metadata strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { showFolderDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val folderName = folders.find { it.id == currentFolderId }?.name ?: "No Folder"
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tags
                tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                // Add Tag Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showTagDialog = true }
                ) {
                    Text(
                        text = "+ Tag",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attached Drawing Preview (if any)
            if (drawingPath != null) {
                val drawingFile = File(drawingPath!!)
                if (drawingFile.exists()) {
                    val bitmap = remember(drawingPath) {
                        BitmapFactory.decodeFile(drawingPath)
                    }
                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable {
                                    saveCurrentState()
                                    onOpenDrawing(currentNoteId)
                                }
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Attached Canvas Drawing",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(Color.White),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            // Note Title (Distinct larger font size, does not affect body)
            TextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                placeholder = {
                    Text(
                        text = "Title",
                        fontSize = titleFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                textStyle = TextStyle(
                    fontSize = titleFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            // Note Body (Separate, adjustable font size, distinct from title)
            TextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                placeholder = {
                    Text(
                        text = "Start typing your note...\nUse toolbar for bold, lists, and headings.",
                        fontSize = bodyFontSize.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                textStyle = TextStyle(
                    fontSize = bodyFontSize.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = (bodyFontSize * 1.45f).sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .testTag("note_body_input")
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Select Folder Dialog
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Move to Folder") },
            text = {
                Column {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentFolderId = null
                                showFolderDialog = false
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text("No Folder (None)", fontWeight = if (currentFolderId == null) FontWeight.Bold else FontWeight.Normal)
                    }
                    folders.forEach { folder ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentFolderId = folder.id
                                    showFolderDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(folder.name, fontWeight = if (currentFolderId == folder.id) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Tag Dialog
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Add Tag") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("e.g. project, urgent") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanTag = newTagInput.trim().replace("#", "")
                        if (cleanTag.isNotEmpty()) {
                            tagsString = if (tagsString.isBlank()) cleanTag else "$tagsString,$cleanTag"
                        }
                        newTagInput = ""
                        showTagDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
