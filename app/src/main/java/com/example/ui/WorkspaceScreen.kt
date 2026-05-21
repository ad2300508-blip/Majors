package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
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
    val syncState by viewModel.syncState.collectAsState()

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
                            title = activeNoteTitle,
                            text = activeNoteText,
                            courseId = activeNoteCourseId,
                            strokes = activeNoteStrokes,
                            courses = courses,
                            ocrActive = ocrActive,
                            syncState = syncState,
                            onTitleChange = { viewModel.updateActiveNoteTitle(it) },
                            onTextChange = { viewModel.updateActiveNoteText(it) },
                            onCourseChange = { viewModel.updateActiveNoteCourse(it) },
                            onStrokesChange = { viewModel.updateActiveNoteStrokes(it) },
                            onPerformOCR = { viewModel.performHandwritingOCR(it) },
                            onSyncToDrive = { viewModel.syncActiveNoteToGoogleDrive() },
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
                    title = activeNoteTitle,
                    text = activeNoteText,
                    courseId = activeNoteCourseId,
                    strokes = activeNoteStrokes,
                    courses = courses,
                    ocrActive = ocrActive,
                    syncState = syncState,
                    onTitleChange = { viewModel.updateActiveNoteTitle(it) },
                    onTextChange = { viewModel.updateActiveNoteText(it) },
                    onCourseChange = { viewModel.updateActiveNoteCourse(it) },
                    onStrokesChange = { viewModel.updateActiveNoteStrokes(it) },
                    onPerformOCR = { viewModel.performHandwritingOCR(it) },
                    onSyncToDrive = { viewModel.syncActiveNoteToGoogleDrive() },
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
                                IconButton(onClick = { onDeleteNote(note.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete note", tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
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
    title: String,
    text: String,
    courseId: Long?,
    strokes: List<com.example.data.DrawingStroke>,
    courses: List<Course>,
    ocrActive: Boolean,
    syncState: SyncState,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onCourseChange: (Long?) -> Unit,
    onStrokesChange: (List<com.example.data.DrawingStroke>) -> Unit,
    onPerformOCR: (Bitmap) -> Unit,
    onSyncToDrive: () -> Unit,
    onBackToNotesList: () -> Unit,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    // Canvas dimension states to accurately render export Bitmaps
    var canvasWidth by remember { mutableStateOf(400) }
    var canvasHeight by remember { mutableStateOf(400) }

    // Brush configurations
    val brushColors = listOf(
        Color(0xFF2C3E50), // Slate Black
        Color(0xFFE74C3C), // Crimson Red
        Color(0xFF27AE60), // Emerald Green
        Color(0xFF2980B9), // Royal Blue
        Color(0xFF8E44AD), // Amethyst Purple
        Color(0xFFF1C40F)  // S-Pen Yellow
    )
    var selectedBrushColor by remember { mutableStateOf(brushColors.first()) }
    var selectedBrushWidth by remember { mutableStateOf(6f) }
    var isEraserEnabled by remember { mutableStateOf(false) }

    val activeCourse = courses.find { it.id == courseId }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                // S-Pen AI transcription button
                Button(
                    onClick = {
                        val bitmap = strokesToBitmap(strokes, canvasWidth, canvasHeight)
                        onPerformOCR(bitmap)
                    },
                    modifier = Modifier.testTag("spen_ocr_button"),
                    enabled = strokes.isNotEmpty() && !ocrActive,
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

                Spacer(modifier = Modifier.width(10.dp))

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

        // Split-pane editing layout (optimized for tablet viewport)
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Hand-writing canvas (Left division)
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                // Drawing workspace heading and action row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("S-Pen Stylus Slate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row {
                        IconButton(onClick = { isEraserEnabled = !isEraserEnabled }) {
                            Icon(
                                if (isEraserEnabled) Icons.Default.BorderColor else Icons.Default.AutoFixNormal,
                                contentDescription = "Toggle Eraser Mode",
                                tint = if (isEraserEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { onStrokesChange(emptyList()) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Canvas")
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
                        selectedColor = selectedBrushColor,
                        selectedWidth = selectedBrushWidth,
                        onStrokesChanged = onStrokesChange,
                        isEraser = isEraserEnabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Canvas settings / Palette Toolbar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Slider for stroke width
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.LineWeight, contentDescription = null, modifier = Modifier.size(18.dp))
                            Slider(
                                value = selectedBrushWidth,
                                onValueChange = { selectedBrushWidth = it },
                                valueRange = 2f..24f,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // Colored circular selectors
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            brushColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = 2.dp,
                                            color = if (selectedBrushColor == color && !isEraserEnabled) MaterialTheme.colorScheme.onSurface else Color.Transparent,
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
                }
            }

            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Transcription text document area (Right division)
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                Text(
                    "Recognized Notes & Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Transcribed characters or notes keyboard additions go here...") },
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("note_text_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
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
