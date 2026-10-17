package com.example.ui.navigation

sealed class NotelyScreen(val route: String) {
    object Notes : NotelyScreen("notes")
    object Todo : NotelyScreen("todo")
    object Search : NotelyScreen("search")
    object Settings : NotelyScreen("settings")
    object NoteEditor : NotelyScreen("note_editor/{noteId}") {
        fun createRoute(noteId: Long) = "note_editor/$noteId"
    }
    object NoteDetail : NotelyScreen("note_detail/{noteId}") {
        fun createRoute(noteId: Long) = "note_detail/$noteId"
    }
    object ChecklistEditor : NotelyScreen("checklist_editor/{checklistId}") {
        fun createRoute(checklistId: Long) = "checklist_editor/$checklistId"
    }
    object Drawing : NotelyScreen("drawing/{noteId}") {
        fun createRoute(noteId: Long) = "drawing/$noteId"
    }
    object BackupRestore : NotelyScreen("backup_restore")
    object Trash : NotelyScreen("trash")
}
