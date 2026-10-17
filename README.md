# Notely - Professional Mobile Notes & To-Do

Notely is an offline-first, private, and highly polished mobile notes, to-do, and canvas sketch app built with Kotlin, Jetpack Compose, and Room.

---

## 1. Architecture Summary & Data Model

Notely is structured using **MVVM (Model-View-ViewModel)** and **Repository Pattern**:

- **Data Layer (`/data`)**:
  - `model/Note.kt`: Note entity supporting title, rich text markdown body, separate font scales, color labels, folders, tags, pin, archive, lock, trash, and sketch drawing file paths.
  - `model/Checklist.kt`: Checklist entity with `ChecklistItem` list, due dates, due times, reminder badges, and completion progress.
  - `model/Folder.kt`: Custom user folders (Personal, Work, Ideas, Study, etc.).
  - `model/BackupModels.kt`: Versioned JSON export/import system (`schemaVersion`, `exportedAt`, `checksum` via SHA-256, `data`).
  - `db/NotelyDatabase.kt`: Room SQLite database with DAOs for reactive Flow observations.
  - `repository/NotelyRepository.kt`: Clean interface managing notes, checklists, folders, safety snapshots, and restore modes.
  - `preferences/AppPreferences.kt`: User settings (Theme, Accent Color, Font Scale, Default Note Color, View Mode, App Lock PIN, Language, Auto-backup reminder).
- **UI Layer (`/ui`)**:
  - `screens/`: `NotesScreen`, `NoteEditorScreen`, `ChecklistsScreen`, `ChecklistEditorScreen`, `DrawingScreen`, `SearchScreen`, `SettingsScreen`, `BackupRestoreScreen`, `TrashScreen`, `LockScreen`.
  - `components/`: `NoteCard`, `ChecklistCard`, `RichTextToolbar`, `ColorPickerRow`, `PinEntryPad`, `QuickCaptureFab`, `EmptyStateView`.
  - `theme/`: M3 theme with customizable accents (Indigo, Emerald, Rose, Amber, Violet, Ocean) and pastel note containers for light and dark modes.

---

## 2. Core Features

1. **Notes**:
   - Create, edit, delete, pin, archive, and color-label notes with 8 pastel palettes.
   - Rich text formatting: Bold, Italic, Underline, Bullet lists, Numbered lists, H1, H2, and Check items.
   - Title and body have independent font sizes (body does NOT default to title size).
   - Auto-save with live word count and character count.
2. **To-Do Lists**:
   - Checklist items with add, check, reorder (Move Up/Down), and delete.
   - Due date and time picker.
   - "Today", "Upcoming", "All", and "Completed" filters.
3. **Drawing Canvas**:
   - Pen, Highlighter (semi-transparent wider stroke), and Eraser.
   - 6 vibrant colors and 3 stroke sizes (S, M, L).
   - Undo, Redo, and Clear actions.
   - Save sketches directly into notes as high-resolution PNG images.
4. **Quick Capture**:
   - Floating speed-dial FAB with options for Note, Checklist, Canvas Drawing, Voice memo, and Paste from Clipboard.
5. **Organization & Search**:
   - Folders and Tags.
   - Real-time full-text search across titles, contents, and checklist items.
   - Sorting by Updated Date, Created Date, Title (A-Z), and Color.
   - Staggered Grid and List view toggle.
6. **Reliable Backup & Restore**:
   - Versioned JSON export with SHA-256 checksum verification.
   - Import pre-validation with content preview ("Found X notes, Y checklists").
   - Choices for "Merge" or "Replace All" (with automatic safety snapshot before replace).
   - "Move to a new phone" illustrated transfer guide.
   - 7-day auto-backup reminder.
7. **Privacy & Security**:
   - Optional App Lock with 4-digit PIN.
   - Lock individual notes with PIN protection.
   - 30-day Trash recovery with auto-expiration.
   - 100% offline, zero trackers, zero data collection.

---

## 3. Build & Run

- **Run in Android Studio / AI Studio**: Run the project on an Android device or emulator (minSdk 24, targetSdk 36).
- **Run Unit Tests**:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Assemble APK**:
  ```bash
  gradle :app:assembleDebug
  ```

---

## 4. Next Steps for Play Store Release

1. **Cross-Platform / PWA / Wrapper**:
   - The native Kotlin/Compose codebase can be packaged directly for the Google Play Store as an `.aab` (Android App Bundle).
   - Alternatively, for web deployment, the React/TypeScript/PWA frontend can be bundled with Capacitor (`@capacitor/android`) for zero-overhead multi-platform deployment.
2. **Optional Cloud Sync**:
   - Integrate optional end-to-end encrypted cloud sync using Supabase (PostgreSQL + Row Level Security) or Firebase Firestore, authenticated with Google Sign-In via Credential Manager (`androidx.credentials`).
3. **Home-Screen Widgets**:
   - Implement Android Jetpack Glance (`androidx.glance:glance-appwidget`) to provide interactive home-screen widgets for Today's tasks, quick note capture, and pinned notes.
