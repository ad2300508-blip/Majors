package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Course
import com.example.data.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val courses by viewModel.courses.collectAsState()

    val activeNoteId by viewModel.activeNoteId.collectAsState()
    val activeNoteTitle by viewModel.activeNoteTitle.collectAsState()
    val activeNoteText by viewModel.activeNoteText.collectAsState()
    val activeNoteCourseId by viewModel.activeNoteCourseId.collectAsState()
    val activeNoteStrokes by viewModel.activeNoteStrokes.collectAsState()

    val ocrActive by viewModel.ocrActive.collectAsState()
    val handwritingSolvingActive by viewModel.handwritingSolvingActive.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val aiAssistantActive by viewModel.aiAssistantActive.collectAsState()
    val lectureTranscribingActive by viewModel.lectureTranscribingActive.collectAsState()

    // Search and filter notes
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCourseId by remember { mutableStateOf<Long?>(null) }

    val filteredNotes = notes.filter { note ->
        (selectedFilterCourseId == null || note.courseId == selectedFilterCourseId) &&
                (searchQuery.isEmpty() || note.title.contains(searchQuery, ignoreCase = true) || note.textContent.contains(searchQuery, ignoreCase = true))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth > 840.dp

        if (isTabletLandscape) {
            // Tablet view: Dual-pane side-by-side permanent layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Left pane: Note explorer sidebar
                NoteExplorerSidebar(
                    notes = filteredNotes,
                    courses = courses,
                    activeNoteId = activeNoteId,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedFilterCourseId = selectedFilterCourseId,
                    onFilterCourseIdChange = { selectedFilterCourseId = it },
                    onSelectNote = { viewModel.setActiveNote(it) },
                    onCreateNewNote = { viewModel.createNewNote() },
                    onDeleteNote = { viewModel.deleteNote(it) },
                    modifier = Modifier.width(320.dp).fillMaxHeight()
                )

                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Right pane: S-Pen Handwriting Canvas and text container
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (activeNoteId != null) {
                        ActiveNoteEditor(
                            noteId = activeNoteId,
                            title = activeNoteTitle,
                            text = activeNoteText,
                            courseId = activeNoteCourseId,
                            strokes = activeNoteStrokes,
                            courses = courses,
                            ocrActive = ocrActive,
                            handwritingSolvingActive = handwritingSolvingActive,
                            syncState = syncState,
                            aiAssistantActive = aiAssistantActive,
                            lectureTranscribingActive = lectureTranscribingActive,
                            onTitleChange = { viewModel.updateActiveNoteTitle(it) },
                            onTextChange = { viewModel.updateActiveNoteText(it) },
                            onCourseChange = { viewModel.updateActiveNoteCourse(it) },
                            onStrokesChange = { viewModel.updateActiveNoteStrokes(it) },
                            onPerformOCR = { viewModel.performHandwritingOCR(it) },
                            onSolveOrExplainHandwriting = { viewModel.solveOrExplainHandwriting(it) },
                            onSyncToDrive = { viewModel.syncActiveNoteToGoogleDrive() },
                            onPerformAiAssistant = { viewModel.runNoteAiAssistant(it) },
                            onVoiceLectureTranscribe = { viewModel.runVoiceLectureTranscriber() },
                            onBackToNotesList = { viewModel.setActiveNote(null) },
                            isTablet = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Empty Workspace visual state
                        EmptyWorkspaceState(onCreateNote = { viewModel.createNewNote() })
                    }
                }
            }
        } else {
            // Mobile (Portrait / Compact) layout: Single-view switcher
            if (activeNoteId != null) {
                ActiveNoteEditor(
                    noteId = activeNoteId,
                    title = activeNoteTitle,
                    text = activeNoteText,
                    courseId = activeNoteCourseId,
                    strokes = activeNoteStrokes,
                    courses = courses,
                    ocrActive = ocrActive,
                    handwritingSolvingActive = handwritingSolvingActive,
                    syncState = syncState,
                    aiAssistantActive = aiAssistantActive,
                    lectureTranscribingActive = lectureTranscribingActive,
                    onTitleChange = { viewModel.updateActiveNoteTitle(it) },
                    onTextChange = { viewModel.updateActiveNoteText(it) },
                    onCourseChange = { viewModel.updateActiveNoteCourse(it) },
                    onStrokesChange = { viewModel.updateActiveNoteStrokes(it) },
                    onPerformOCR = { viewModel.performHandwritingOCR(it) },
                    onSolveOrExplainHandwriting = { viewModel.solveOrExplainHandwriting(it) },
                    onSyncToDrive = { viewModel.syncActiveNoteToGoogleDrive() },
                    onPerformAiAssistant = { viewModel.runNoteAiAssistant(it) },
                    onVoiceLectureTranscribe = { viewModel.runVoiceLectureTranscriber() },
                    onBackToNotesList = { viewModel.setActiveNote(null) },
                    isTablet = false,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                NoteExplorerSidebar(
                    notes = filteredNotes,
                    courses = courses,
                    activeNoteId = activeNoteId,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedFilterCourseId = selectedFilterCourseId,
                    onFilterCourseIdChange = { selectedFilterCourseId = it },
                    onSelectNote = { viewModel.setActiveNote(it) },
                    onCreateNewNote = { viewModel.createNewNote() },
                    onDeleteNote = { viewModel.deleteNote(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun NoteExplorerSidebar(
    notes: List<Note>,
    courses: List<Course>,
    activeNoteId: Long?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilterCourseId: Long?,
    onFilterCourseIdChange: (Long?) -> Unit,
    onSelectNote: (Long) -> Unit,
    onCreateNewNote: () -> Unit,
    onDeleteNote: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    if (noteToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note?") },
            text = { Text("Delete \"${noteToDelete!!.title}\"? This will also remove its flashcards.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNote(noteToDelete!!.id)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notebooks Explorer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = onCreateNewNote,
                modifier = Modifier.testTag("explorer_add_note_button")
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = "New Class Note", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search drafts, texts...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Course filter horizontal rail
        Text("Sort By Class", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All" filter chip
            FilterChip(
                selected = selectedFilterCourseId == null,
                onClick = { onFilterCourseIdChange(null) },
                label = { Text("All", fontSize = 11.sp) }
            )
            courses.forEach { course ->
                val color = Color(android.graphics.Color.parseColor(course.colorHex))
                FilterChip(
                    selected = selectedFilterCourseId == course.id,
                    onClick = { onFilterCourseIdChange(course.id) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier.size(8.dp).background(color, CircleShape)
                        )
                    },
                    label = { Text(course.code, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Document List
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            if (notes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("Workspace is clean.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(notes) { note ->
                    val course = courses.find { it.id == note.courseId }
                    val isSelected = note.id == activeNoteId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectNote(note.id) }
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { noteToDelete = note },
                                    modifier = Modifier.size(36.dp).minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete note",
                                        tint = Color.Red.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (note.textContent.isNotEmpty()) note.textContent else "Draw handwriting using stylus/S-Pen",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.getFormattedDate(),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                if (course != null) {
                                    val courseColor = Color(android.graphics.Color.parseColor(course.colorHex))
                                    Box(
                                        modifier = Modifier
                                            .background(courseColor, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(course.code, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveNoteEditor(
    noteId: Long?,
    title: String,
    text: String,
    courseId: Long?,
    strokes: List<com.example.data.DrawingStroke>,
    courses: List<Course>,
    ocrActive: Boolean,
    handwritingSolvingActive: Boolean,
    syncState: SyncState,
    aiAssistantActive: Boolean,
    lectureTranscribingActive: Boolean,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onCourseChange: (Long?) -> Unit,
    onStrokesChange: (List<com.example.data.DrawingStroke>) -> Unit,
    onPerformOCR: (Bitmap) -> Unit,
    onSolveOrExplainHandwriting: (Bitmap) -> Unit,
    onSyncToDrive: () -> Unit,
    onPerformAiAssistant: (String) -> Unit,
    onVoiceLectureTranscribe: () -> Unit,
    onBackToNotesList: () -> Unit,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    // Canvas dimension states to accurately render export Bitmaps
    var canvasWidth by remember { mutableStateOf(400) }
    var canvasHeight by remember { mutableStateOf(400) }

    val undoStack = remember { mutableStateListOf<List<com.example.data.DrawingStroke>>() }
    val redoStack = remember { mutableStateListOf<List<com.example.data.DrawingStroke>>() }

    // Clear history on loading another note
    LaunchedEffect(noteId) {
        undoStack.clear()
        redoStack.clear()
    }

    val handleStrokesChange: (List<com.example.data.DrawingStroke>) -> Unit = { newStrokes ->
        if (newStrokes != strokes) {
            undoStack.add(strokes.toList())
            redoStack.clear()
            onStrokesChange(newStrokes)
        }
    }

    // Brush and guide layout configurations
    var canvasBackgroundStyle by remember { mutableStateOf("blank") } // "blank", "ruled", "grid"
    var brushMode by remember { mutableStateOf("pen") } // "pen", "pencil", "highlighter"

    val brushColors = listOf(
        Color(0xFF2C3E50), // Slate Black
        Color(0xFFE74C3C), // Crimson Red
        Color(0xFF27AE60), // Emerald Green
        Color(0xFF2980B9), // Royal Blue
        Color(0xFF8E44AD), // Amethyst Purple
        Color(0xFFF1C40F)  // S-Pen Yellow
    )
    var selectedBrushColor by remember { mutableStateOf(brushColors.first()) }
    var selectedBrushWidth by remember { mutableStateOf(8f) }
    var isEraserEnabled by remember { mutableStateOf(false) }

    val activeCourse = courses.find { it.id == courseId }

    // Trigger brush size and opacity defaults when mode shifts
    LaunchedEffect(brushMode) {
        selectedBrushWidth = when (brushMode) {
            "pencil" -> 4f
            "highlighter" -> 36f
            else -> 8f
        }
    }

    val effectiveColor = when (brushMode) {
        "pencil" -> selectedBrushColor.copy(alpha = 0.6f)
        "highlighter" -> selectedBrushColor.copy(alpha = 0.35f)
        else -> selectedBrushColor
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Upper utility header
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (!isTablet) {
                    IconButton(onClick = onBackToNotesList) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Notes Explorer")
                    }
                }

                // Title input field
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(280.dp).testTag("note_title_input")
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Associated Course picker
                var showCoursePickerMenu by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { showCoursePickerMenu = true },
                        label = { Text(activeCourse?.code ?: "No Course Linked") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = activeCourse?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Gray
                            )
                        }
                    )
                    DropdownMenu(expanded = showCoursePickerMenu, onDismissRequest = { showCoursePickerMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Unlink Course") },
                            onClick = {
                                onCourseChange(null)
                                showCoursePickerMenu = false
                            }
                        )
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text("${course.code} - ${course.name}") },
                                onClick = {
                                    onCourseChange(course.id)
                                    showCoursePickerMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // G-Drive status & triggering actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // S-Pen AI transcription button
                Button(
                    onClick = {
                        val bitmap = strokesToBitmap(strokes, canvasWidth, canvasHeight)
                        onPerformOCR(bitmap)
                    },
                    modifier = Modifier.testTag("spen_ocr_button"),
                    enabled = strokes.isNotEmpty() && !ocrActive && !handwritingSolvingActive,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (ocrActive) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Gesture, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("S-Pen OCR Text")
                    }
                }

                // S-Pen AI Solve & Explain diagram/math button
                Button(
                    onClick = {
                        val bitmap = strokesToBitmap(strokes, canvasWidth, canvasHeight)
                        onSolveOrExplainHandwriting(bitmap)
                    },
                    modifier = Modifier.testTag("spen_solve_button"),
                    enabled = strokes.isNotEmpty() && !ocrActive && !handwritingSolvingActive,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    if (handwritingSolvingActive) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Solve & Explain")
                    }
                }

                // Google Drive Sync button
                IconButton(
                    onClick = onSyncToDrive,
                    modifier = Modifier.testTag("drive_sync_button")
                ) {
                    when (syncState) {
                        is SyncState.Syncing -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        is SyncState.Success -> Icon(Icons.Default.CloudDone, contentDescription = "Synced to Drive", tint = Color(0xFF4CAF50))
                        is SyncState.Error -> Icon(Icons.Default.CloudOff, contentDescription = "Sync Error", tint = MaterialTheme.colorScheme.error)
                        else -> Icon(Icons.Default.CloudUpload, contentDescription = "Sync to G-Drive", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Split-pane responsive layout wrapper
        ResponsiveSplitLayout(
            isTablet = isTablet,
            canvasSection = { canvasModifier ->
                Column(modifier = canvasModifier) {
                // Drawing workspace heading and action row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("S-Pen Stylus Slate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Draw vectors, sketch graphs, or transcribe", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Background layout toggles (blank, ruled, grid)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "blank" to "⬜",
                            "ruled" to "🗒️",
                            "grid" to "🌐"
                        ).forEach { (style, emoji) ->
                            val isSelected = canvasBackgroundStyle == style
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { canvasBackgroundStyle = style }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$emoji ${style.replaceFirstChar { it.lowercase() }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Undo action Button
                        IconButton(
                            onClick = {
                                if (strokes.isNotEmpty()) {
                                    val previous = if (undoStack.isNotEmpty()) undoStack.removeLast() else emptyList()
                                    redoStack.add(strokes)
                                    onStrokesChange(previous)
                                }
                            },
                            enabled = strokes.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Undo,
                                contentDescription = "Undo stroke",
                                tint = if (strokes.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        // Redo action Button
                        IconButton(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    val next = redoStack.removeLast()
                                    undoStack.add(strokes)
                                    onStrokesChange(next)
                                }
                            },
                            enabled = redoStack.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Redo,
                                contentDescription = "Redo stroke",
                                tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(
                            onClick = { isEraserEnabled = !isEraserEnabled },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isEraserEnabled) MaterialTheme.colorScheme.errorContainer else Color.Transparent
                            )
                        ) {
                            Icon(
                                Icons.Default.AutoFixNormal,
                                contentDescription = "Toggle Eraser Mode",
                                tint = if (isEraserEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { onStrokesChange(emptyList()) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Canvas", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        }
                    }
                }

                // The functional interactive drawing layer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .onGloballyPositioned { layoutCoordinates ->
                            canvasWidth = layoutCoordinates.size.width
                            canvasHeight = layoutCoordinates.size.height
                        }
                ) {
                    DrawingCanvas(
                        strokes = strokes,
                        selectedColor = effectiveColor,
                        selectedWidth = selectedBrushWidth,
                        onStrokesChanged = handleStrokesChange,
                        isEraser = isEraserEnabled,
                        backgroundStyle = canvasBackgroundStyle,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Canvas settings / Palette Toolbar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Professional Stylus Brushes tabs
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "pen" to "🖊️ Fountain",
                                    "pencil" to "✏️ Graphite",
                                    "highlighter" to "🖍️ Highlighter"
                                ).forEach { (mode, label) ->
                                    val isSelected = brushMode == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                            .clickable {
                                                brushMode = mode
                                                isEraserEnabled = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Colored circular selectors
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                brushColors.forEach { color ->
                                    val isSelected = selectedBrushColor == color && !isEraserEnabled
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(color, CircleShape)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                selectedBrushColor = color
                                                isEraserEnabled = false
                                            }
                                    )
                                }
                            }
                        }

                        // Slider for stroke width
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                Icons.Default.LineWeight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Thickness (${selectedBrushWidth.toInt()}px):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(90.dp)
                            )
                            Slider(
                                value = selectedBrushWidth,
                                onValueChange = { selectedBrushWidth = it },
                                valueRange = when (brushMode) {
                                    "pencil" -> 1f..12f
                                    "highlighter" -> 16f..80f
                                    else -> 2f..32f
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                }
            },
            textSummarySection = { summaryModifier ->
                Column(modifier = summaryModifier) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Recognized Notes & Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Interactive textbook and research board", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // AI Notebook Actions Toolbar Block
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Gemini Smart Study Assistants:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onPerformAiAssistant("format") },
                                modifier = Modifier.weight(1.1f).testTag("ai_format_notes_button"),
                                enabled = text.isNotEmpty() && !aiAssistantActive,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                if (aiAssistantActive) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Formatting Style", fontSize = 11.sp, maxLines = 1)
                                }
                            }

                            Button(
                                onClick = { onPerformAiAssistant("explain") },
                                modifier = Modifier.weight(1.1f).testTag("ai_explain_notes_button"),
                                enabled = text.isNotEmpty() && !aiAssistantActive,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                if (aiAssistantActive) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Concept Dive", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // 🎙️ Lecture Audio Memo Bench
                var isRecordingActive by remember { mutableStateOf(false) }
                var isRecordingPaused by remember { mutableStateOf(false) }
                var recordingTimeSeconds by remember { mutableStateOf(0) }
                val wavePeaks = remember { mutableStateListOf<Float>() }
                val localLectures = remember { mutableStateListOf<String>() }

                // Wave oscillator animation loop when active
                LaunchedEffect(isRecordingActive, isRecordingPaused) {
                    if (isRecordingActive && !isRecordingPaused) {
                        while (true) {
                            kotlinx.coroutines.delay(100)
                            val amplitude = (15..85).random().toFloat()
                            wavePeaks.add(amplitude)
                            if (wavePeaks.size > 24) {
                                wavePeaks.removeAt(0)
                            }
                        }
                    } else {
                        wavePeaks.clear()
                    }
                }

                LaunchedEffect(isRecordingActive, isRecordingPaused) {
                    if (isRecordingActive && !isRecordingPaused) {
                        while (true) {
                            kotlinx.coroutines.delay(1000)
                            recordingTimeSeconds += 1
                        }
                    }
                }

                val formattedTime = remember(recordingTimeSeconds) {
                    val minutes = recordingTimeSeconds / 60
                    val seconds = recordingTimeSeconds % 60
                    String.format("%02d:%02d", minutes, seconds)
                }

                val stopRecordingAndSave: () -> Unit = {
                    isRecordingActive = false
                    isRecordingPaused = false
                    val docTitle = if (title.startsWith("New Class Note") || title.trim().isEmpty()) "Lecture Session" else title
                    localLectures.add("$docTitle - Audio Lecture (${formattedTime})")
                    recordingTimeSeconds = 0
                }

                var audioBenchExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { audioBenchExpanded = !audioBenchExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isRecordingActive) Icons.Default.SettingsVoice else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isRecordingActive) Color.Red else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Class Lecture Audio Bench",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (isRecordingActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Recording $formattedTime",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (audioBenchExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (audioBenchExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons / waveform drawing bounds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isRecordingActive) {
                                    Button(
                                        onClick = {
                                            isRecordingActive = true
                                            isRecordingPaused = false
                                            recordingTimeSeconds = 0
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
                                    ) {
                                        Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Record Lecture", fontSize = 11.sp, maxLines = 1)
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { isRecordingPaused = !isRecordingPaused },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isRecordingPaused) "Resume" else "Pause", fontSize = 11.sp, maxLines = 1)
                                        }
                                        Button(
                                            onClick = stopRecordingAndSave,
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save Memo", fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Live Oscillating sound peaks waveform
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val density = LocalDensity.current
                                    val barWidthPx = with(density) { 3.dp.toPx() }
                                    val gapPx = with(density) { 2.dp.toPx() }
                                    val startXPx = with(density) { 6.dp.toPx() }
                                    val activeLineColor = Color.Red.copy(alpha = 0.8f)
                                    val inactiveLineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val midY = size.height / 2f
                                        
                                        if (isRecordingActive && !isRecordingPaused) {
                                            wavePeaks.forEachIndexed { idx, heightVal ->
                                                val x = startXPx + idx * (barWidthPx + gapPx)
                                                val h = with(density) { heightVal.dp.toPx() }.coerceAtMost(size.height * 0.8f)
                                                drawLine(
                                                    color = activeLineColor,
                                                    start = androidx.compose.ui.geometry.Offset(x, midY - h / 2f),
                                                    end = androidx.compose.ui.geometry.Offset(x, midY + h / 2f),
                                                    strokeWidth = barWidthPx
                                                )
                                            }
                                        } else {
                                            for (i in 0..20) {
                                                val x = startXPx + i * (barWidthPx + gapPx)
                                                drawLine(
                                                    color = inactiveLineColor,
                                                    start = androidx.compose.ui.geometry.Offset(x, midY - 3f),
                                                    end = androidx.compose.ui.geometry.Offset(x, midY + 3f),
                                                    strokeWidth = barWidthPx
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (localLectures.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Classroom Audio Saves:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    localLectures.forEach { lecture ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(lecture, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }

                                            Button(
                                                onClick = onVoiceLectureTranscribe,
                                                modifier = Modifier.height(26.dp).testTag("ai_transcribe_audio_button"),
                                                enabled = !lectureTranscribingActive,
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                if (lectureTranscribingActive) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(10.dp))
                                                } else {
                                                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(10.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("AI Transcribe", fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Markdown Formatting preset toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Format: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    
                    val insertMarkdown: (String) -> Unit = { tag ->
                        val newText = when (tag) {
                            "H1" -> "$text\n# "
                            "H2" -> "$text\n## "
                            "bold" -> "$text**Bold** "
                            "italic" -> "$text*Italic* "
                            "bullet" -> "$text\n- "
                            "todo" -> "$text\n- [ ] "
                            "timestamp" -> {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                "$text\n📅 ${sdf.format(java.util.Date())}: "
                            }
                            else -> text
                        }
                        onTextChange(newText)
                    }

                    listOf(
                        "H1" to "H1",
                        "H2" to "H2",
                        "bold" to "B",
                        "italic" to "I",
                        "bullet" to "•",
                        "todo" to "☑",
                        "timestamp" to "🕒 Time"
                    ).forEach { (tag, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { insertMarkdown(tag) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { onTextChange("") },
                        modifier = Modifier.size(36.dp).minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All Text",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Transcribed characters, manual typewriter inputs, or AI formatted texts will stream here...") },
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("note_text_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            }
        })
    }
}

@Composable
fun EmptyWorkspaceState(onCreateNote: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Open a study note, or draft a blank workspace.", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onCreateNote) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Note")
            }
        }
    }
}

@Composable
fun ColumnScope.ResponsiveSplitLayout(
    isTablet: Boolean,
    canvasSection: @Composable (Modifier) -> Unit,
    textSummarySection: @Composable (Modifier) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    if (isTablet) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            canvasSection(Modifier.weight(1.1f).fillMaxHeight().padding(12.dp))
            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            textSummarySection(Modifier.weight(0.9f).fillMaxHeight().padding(12.dp))
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Stylus Slate", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("AI Summary", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(12.dp)) {
                if (selectedTab == 0) {
                    canvasSection(Modifier.fillMaxSize())
                } else {
                    textSummarySection(Modifier.fillMaxSize())
                }
            }
        }
    }
}
