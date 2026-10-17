package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.NotelyScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.ChecklistEditorScreen
import com.example.ui.screens.ChecklistsScreen
import com.example.ui.screens.DrawingScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.NotelyTheme
import com.example.ui.viewmodel.NotelyViewModel
import com.example.util.BiometricPromptHelper
import kotlinx.coroutines.flow.collectLatest

class MainActivity : FragmentActivity() {
    private val viewModel: NotelyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // BiometricPrompt startup security verification
        val biometricHelper = BiometricPromptHelper(this)
        if (viewModel.isPinEnabled.value && viewModel.isBiometricEnabled.value && biometricHelper.isBiometricAvailable()) {
            biometricHelper.authenticateOnStartup(
                onSuccess = { viewModel.unlockWithBiometrics() },
                onError = { _, _ -> /* fallback to PIN */ }
            )
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()

            NotelyTheme(
                themeMode = themeMode,
                accentColor = accentColor
            ) {
                NotelyApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NotelyApp(viewModel: NotelyViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()

    // Listen to ViewModel snackbar events
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (isPinEnabled && !isAppUnlocked) {
        LockScreen(
            viewModel = viewModel,
            onUnlocked = {}
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom navigation on primary top-level tabs
    val showBottomBar = currentRoute in listOf(
        NotelyScreen.Notes.route,
        NotelyScreen.Todo.route,
        NotelyScreen.Search.route,
        NotelyScreen.Settings.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NotelyBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NotelyScreen.Notes.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Notes Tab
            composable(NotelyScreen.Notes.route) {
                NotesScreen(
                    viewModel = viewModel,
                    onNoteClick = { noteId ->
                        navController.navigate(NotelyScreen.NoteEditor.createRoute(noteId))
                    },
                    onNewNote = {
                        navController.navigate(NotelyScreen.NoteEditor.createRoute(0L))
                    },
                    onNewChecklist = {
                        navController.navigate(NotelyScreen.ChecklistEditor.createRoute(0L))
                    },
                    onNewDrawing = {
                        navController.navigate(NotelyScreen.Drawing.createRoute(0L))
                    },
                    onNavigateToTrash = {
                        navController.navigate(NotelyScreen.Trash.route)
                    }
                )
            }

            // To-do Tab
            composable(NotelyScreen.Todo.route) {
                ChecklistsScreen(
                    viewModel = viewModel,
                    onChecklistClick = { checklistId ->
                        navController.navigate(NotelyScreen.ChecklistEditor.createRoute(checklistId))
                    },
                    onNewChecklist = {
                        navController.navigate(NotelyScreen.ChecklistEditor.createRoute(0L))
                    },
                    onNewNote = {
                        navController.navigate(NotelyScreen.NoteEditor.createRoute(0L))
                    },
                    onNewDrawing = {
                        navController.navigate(NotelyScreen.Drawing.createRoute(0L))
                    }
                )
            }

            // Search Tab
            composable(NotelyScreen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onNoteClick = { noteId ->
                        navController.navigate(NotelyScreen.NoteDetail.createRoute(noteId))
                    },
                    onChecklistClick = { checklistId ->
                        navController.navigate(NotelyScreen.ChecklistEditor.createRoute(checklistId))
                    }
                )
            }

            // Settings Tab
            composable(NotelyScreen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToBackup = {
                        navController.navigate(NotelyScreen.BackupRestore.route)
                    },
                    onNavigateToTrash = {
                        navController.navigate(NotelyScreen.Trash.route)
                    }
                )
            }

            // Note Detail Screen
            composable(
                route = NotelyScreen.NoteDetail.route,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                NoteDetailScreen(
                    noteId = noteId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEditNote = { nId ->
                        navController.navigate(NotelyScreen.NoteEditor.createRoute(nId))
                    }
                )
            }

            // Note Editor Screen
            composable(
                route = NotelyScreen.NoteEditor.route,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                NoteEditorScreen(
                    noteId = noteId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenDrawing = { nId ->
                        navController.navigate(NotelyScreen.Drawing.createRoute(nId))
                    }
                )
            }

            // Checklist Editor Screen
            composable(
                route = NotelyScreen.ChecklistEditor.route,
                arguments = listOf(navArgument("checklistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val checklistId = backStackEntry.arguments?.getLong("checklistId") ?: 0L
                ChecklistEditorScreen(
                    checklistId = checklistId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Drawing Canvas Screen
            composable(
                route = NotelyScreen.Drawing.route,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                DrawingScreen(
                    noteId = noteId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Backup & Restore Screen
            composable(NotelyScreen.BackupRestore.route) {
                BackupRestoreScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Trash Screen
            composable(NotelyScreen.Trash.route) {
                TrashScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun NotelyBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == NotelyScreen.Notes.route,
            onClick = { onNavigate(NotelyScreen.Notes.route) },
            icon = { Icon(Icons.Default.Description, contentDescription = "Notes") },
            label = { Text("Notes") },
            modifier = Modifier.testTag("nav_notes")
        )
        NavigationBarItem(
            selected = currentRoute == NotelyScreen.Todo.route,
            onClick = { onNavigate(NotelyScreen.Todo.route) },
            icon = { Icon(Icons.Default.CheckBox, contentDescription = "To-Do") },
            label = { Text("To-Do") },
            modifier = Modifier.testTag("nav_todo")
        )
        NavigationBarItem(
            selected = currentRoute == NotelyScreen.Search.route,
            onClick = { onNavigate(NotelyScreen.Search.route) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") },
            modifier = Modifier.testTag("nav_search")
        )
        NavigationBarItem(
            selected = currentRoute == NotelyScreen.Settings.route,
            onClick = { onNavigate(NotelyScreen.Settings.route) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}
