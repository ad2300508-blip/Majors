package com.example.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Course
import com.example.data.Note

// ── Drawing tool types ─────────────────────────────────────────────────────────
enum class DrawTool { Pen, Pencil, Brush, Highlighter, Eraser }

private val kPenColors = listOf(
    Color(0xFF1A1A1A), Color(0xFF616161), Color(0xFFFFFFFF),
    Color(0xFFB71C1C), Color(0xFFD32F2F), Color(0xFFE65100),
    Color(0xFFF57F17), Color(0xFF1B5E20), Color(0xFF006064),
    Color(0xFF0D47A1), Color(0xFF4A148C), Color(0xFF880E4F),
)

private val kHighlighterColors = listOf(
    Color(0xFFFFF176), Color(0xFFB9F6CA), Color(0xFF80DEEA),
    Color(0xFFFFAB91), Color(0xFFE1BEE7), Color(0xFFFFE0B2),
)

// ── 1. WorkspaceScreen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val notes                    by viewModel.notes.collectAsState()
    val courses                  by viewModel.courses.collectAsState()
    val activeNoteId             by viewModel.activeNoteId.collectAsState()
    val activeNoteTitle          by viewModel.activeNoteTitle.collectAsState()
    val activeNoteText           by viewModel.activeNoteText.collectAsState()
    val activeNoteCourseId       by viewModel.activeNoteCourseId.collectAsState()
    val activeNoteStrokes        by viewModel.activeNoteStrokes.collectAsState()
    val ocrActive                by viewModel.ocrActive.collectAsState()
    val handwritingSolvingActive by viewModel.handwritingSolvingActive.collectAsState()
    val syncState                by viewModel.syncState.collectAsState()
    val aiAssistantActive        by viewModel.aiAssistantActive.collectAsState()
    val lectureTranscribingActive by viewModel.lectureTranscribingActive.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCourseId by remember { mutableStateOf<Long?>(null) }

    val filteredNotes = notes.filter { note ->
        (selectedFilterCourseId == null || note.courseId == selectedFilterCourseId) &&
                (searchQuery.isEmpty() || note.title.contains(searchQuery, ignoreCase = true) ||
                        note.textContent.contains(searchQuery, ignoreCase = true))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth > 840.dp

        if (isTabletLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                NoteExplorerSidebar(
                    notes = filteredNotes, courses = courses, activeNoteId = activeNoteId,
                    searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it },
                    selectedFilterCourseId = selectedFilterCourseId,
                    onFilterCourseIdChange = { selectedFilterCourseId = it },
                    onSelectNote = { viewModel.setActiveNote(it) },
                    onCreateNewNote = { viewModel.createNewNote() },
                    onDeleteNote = { viewModel.deleteNote(it) },
                    onTogglePin = { viewModel.toggleNotePin(it) },
                    modifier = Modifier.width(300.dp).fillMaxHeight()
                )
                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (activeNoteId != null) {
                        ActiveNoteEditor(
                            noteId = activeNoteId,
                            title = activeNoteTitle, text = activeNoteText,
                            courseId = activeNoteCourseId, strokes = activeNoteStrokes,
                            courses = courses, ocrActive = ocrActive,
                            handwritingSolvingActive = handwritingSolvingActive,
                            syncState = syncState, aiAssistantActive = aiAssistantActive,
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
                            isTablet = true, modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        EmptyWorkspaceState(onCreateNote = { viewModel.createNewNote() })
                    }
                }
            }
        } else {
            if (activeNoteId != null) {
                ActiveNoteEditor(
                    noteId = activeNoteId,
                    title = activeNoteTitle, text = activeNoteText,
                    courseId = activeNoteCourseId, strokes = activeNoteStrokes,
                    courses = courses, ocrActive = ocrActive,
                    handwritingSolvingActive = handwritingSolvingActive,
                    syncState = syncState, aiAssistantActive = aiAssistantActive,
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
                    isTablet = false, modifier = Modifier.fillMaxSize()
                )
            } else {
                NoteExplorerSidebar(
                    notes = filteredNotes, courses = courses, activeNoteId = activeNoteId,
                    searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it },
                    selectedFilterCourseId = selectedFilterCourseId,
                    onFilterCourseIdChange = { selectedFilterCourseId = it },
                    onSelectNote = { viewModel.setActiveNote(it) },
                    onCreateNewNote = { viewModel.createNewNote() },
                    onDeleteNote = { viewModel.deleteNote(it) },
                    onTogglePin = { viewModel.toggleNotePin(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ── 2. Note Explorer Sidebar ──────────────────────────────────────────────────

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
    onTogglePin: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note?") },
            text = { Text("\"${noteToDelete!!.title}\" will be permanently deleted along with its flashcards.") },
            confirmButton = {
                Button(
                    onClick = { onDeleteNote(noteToDelete!!.id); noteToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Cancel") } }
        )
    }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {

        // Header
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Notebooks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${notes.size} notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(
                    onClick = onCreateNewNote,
                    modifier = Modifier.testTag("explorer_add_note_button")
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "New Note")
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search notes…", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Course filter chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                            Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
                        },
                        label = { Text(course.code, fontSize = 11.sp) }
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Note list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (notes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 56.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BorderColor, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            }
                            Text("No notes yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Tap + to start writing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(notes) { note ->
                    NoteListItem(
                        note = note,
                        courses = courses,
                        isSelected = note.id == activeNoteId,
                        onSelect = { onSelectNote(note.id) },
                        onDelete = { noteToDelete = note },
                        onTogglePin = { onTogglePin(note.id) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteListItem(
    note: Note,
    courses: List<Course>,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val course = courses.find { it.id == note.courseId }
    val courseColor = course?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                else Color.Transparent
            )
            .clickable(onClick = onSelect)
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 72.dp)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else courseColor.copy(alpha = 0.4f))
        )
        Row(modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
            // Content
            Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (note.textContent.isNotBlank()) note.textContent else "Drawing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        note.getRelativeTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (note.isSynced) {
                            Icon(Icons.Default.CloudDone, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                        }
                        if (note.isPinned) {
                            Icon(Icons.Default.PushPin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        }
                        if (course != null) {
                            Surface(shape = RoundedCornerShape(4.dp), color = courseColor.copy(alpha = 0.15f)) {
                                Text(
                                    course.code, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    color = courseColor, fontSize = 9.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons
            Column(verticalArrangement = Arrangement.Center) {
                IconButton(onClick = onTogglePin, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.PushPin, null,
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ── 3. Active Note Editor ─────────────────────────────────────────────────────

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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Undo/redo stacks
    val undoStack = remember { mutableStateListOf<List<com.example.data.DrawingStroke>>() }
    val redoStack = remember { mutableStateListOf<List<com.example.data.DrawingStroke>>() }
    var activePage by remember { mutableStateOf(0) }
    var pageCount  by remember { mutableStateOf(1) }
    LaunchedEffect(noteId) { undoStack.clear(); redoStack.clear(); activePage = 0; pageCount = 1 }

    // Drawing state
    var drawTool by remember { mutableStateOf(DrawTool.Pen) }
    var selectedPenColor by remember { mutableStateOf(kPenColors.first()) }
    var selectedHighlighterColor by remember { mutableStateOf(kHighlighterColors.first()) }
    var strokeWidth by remember { mutableStateOf(6f) }
    var canvasBackground by remember { mutableStateOf("blank") }

    LaunchedEffect(drawTool) {
        strokeWidth = when (drawTool) {
            DrawTool.Pencil     -> 3f
            DrawTool.Brush      -> 10f
            DrawTool.Highlighter -> 22f
            else                -> 6f
        }
    }

    val handleStrokesChange: (List<com.example.data.DrawingStroke>) -> Unit = { newStrokes ->
        if (newStrokes != strokes) {
            undoStack.add(strokes.toList())
            redoStack.clear()
            onStrokesChange(newStrokes)
        }
    }

    val isEraserEnabled = drawTool == DrawTool.Eraser
    val effectiveColor = when (drawTool) {
        DrawTool.Pencil      -> selectedPenColor.copy(alpha = 0.62f)
        DrawTool.Brush       -> selectedPenColor.copy(alpha = 0.88f)
        DrawTool.Highlighter -> selectedHighlighterColor.copy(alpha = 0.40f)
        DrawTool.Eraser      -> Color(0xFFBBBBBB)
        else                 -> selectedPenColor
    }
    val effectiveWidth = when (drawTool) {
        DrawTool.Brush -> strokeWidth * 1.5f
        else           -> strokeWidth
    }

    var canvasWidth by remember { mutableStateOf(400) }
    var canvasHeight by remember { mutableStateOf(400) }
    val activeCourse = courses.find { it.id == courseId }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Top bar ──────────────────────────────────────────────────────────
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isTablet) {
                    IconButton(onClick = onBackToNotesList, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(22.dp))
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f).testTag("note_title_input"),
                    placeholder = { Text("Untitled Note", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) }
                )

                // Course chip
                var showCourseMenu by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { showCourseMenu = true },
                        label = { Text(activeCourse?.code ?: "No Course", maxLines = 1, fontSize = 12.sp) },
                        leadingIcon = {
                            Box(modifier = Modifier.size(8.dp).background(
                                activeCourse?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
                                    ?: MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            ))
                        },
                        modifier = Modifier.height(32.dp)
                    )
                    DropdownMenu(expanded = showCourseMenu, onDismissRequest = { showCourseMenu = false }) {
                        DropdownMenuItem(text = { Text("No Course") }, onClick = { onCourseChange(null); showCourseMenu = false })
                        courses.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.code} – ${c.name}") },
                                onClick = { onCourseChange(c.id); showCourseMenu = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Action icons
                IconButton(
                    onClick = { onPerformOCR(strokesToBitmap(strokes, canvasWidth, canvasHeight)) },
                    enabled = strokes.isNotEmpty() && !ocrActive && !handwritingSolvingActive,
                    modifier = Modifier.size(40.dp).testTag("spen_ocr_button")
                ) {
                    if (ocrActive) CircularProgressIndicator(Modifier.size(18.dp))
                    else Icon(Icons.Default.Gesture, "OCR", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                }
                IconButton(
                    onClick = { onSolveOrExplainHandwriting(strokesToBitmap(strokes, canvasWidth, canvasHeight)) },
                    enabled = strokes.isNotEmpty() && !ocrActive && !handwritingSolvingActive,
                    modifier = Modifier.size(40.dp).testTag("spen_solve_button")
                ) {
                    if (handwritingSolvingActive) CircularProgressIndicator(Modifier.size(18.dp))
                    else Icon(Icons.Default.AutoAwesome, "AI Solve", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onSyncToDrive, modifier = Modifier.size(40.dp).testTag("drive_sync_button")) {
                    when (syncState) {
                        is SyncState.Syncing -> CircularProgressIndicator(Modifier.size(18.dp))
                        is SyncState.Success -> Icon(Icons.Default.CloudDone, "Synced", tint = Color(0xFF4CAF50))
                        is SyncState.Error   -> Icon(Icons.Default.CloudOff, "Error", tint = MaterialTheme.colorScheme.error)
                        else                 -> Icon(Icons.Default.CloudUpload, "Sync", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(
                    onClick = {
                        context.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, title); putExtra(Intent.EXTRA_TEXT, "$title\n\n$text") },
                            "Share Note"
                        ))
                    },
                    enabled = text.isNotEmpty(),
                    modifier = Modifier.size(40.dp).testTag("share_note_button")
                ) {
                    Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Drawing toolbar (Samsung Notes-style) ─────────────────────────
        DrawingToolbar(
            drawTool = drawTool,
            onToolChange = { drawTool = it },
            canUndo = strokes.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            onUndo = {
                val prev = if (undoStack.isNotEmpty()) undoStack.removeLast() else emptyList()
                redoStack.add(strokes.toList())
                onStrokesChange(prev)
            },
            onRedo = {
                if (redoStack.isNotEmpty()) {
                    val next = redoStack.removeLast()
                    undoStack.add(strokes.toList())
                    onStrokesChange(next)
                }
            },
            onClear = { onStrokesChange(emptyList()) },
            backgroundStyle = canvasBackground,
            onBackgroundStyleChange = { canvasBackground = it },
        )

        // ── Drawing properties (color + size, hidden when eraser) ─────────
        AnimatedVisibility(
            visible = drawTool != DrawTool.Eraser,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit  = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            DrawingPropertiesStrip(
                drawTool = drawTool,
                penColor = selectedPenColor,
                highlighterColor = selectedHighlighterColor,
                onPenColorChange = { selectedPenColor = it },
                onHighlighterColorChange = { selectedHighlighterColor = it },
                strokeWidth = strokeWidth,
                onStrokeWidthChange = { strokeWidth = it },
            )
        }

        PageTabsRow(
            activePage = activePage,
            pageCount  = pageCount,
            onPageChange = { activePage = it },
            onAddPage    = { pageCount++ }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ── Canvas + text split ────────────────────────────────────────────
        ResponsiveSplitLayout(
            isTablet = isTablet,
            canvasSection = { canvasMod ->
                Box(
                    modifier = canvasMod
                        .onGloballyPositioned {
                            canvasWidth = it.size.width
                            canvasHeight = it.size.height
                        }
                ) {
                    DrawingCanvas(
                        strokes = strokes,
                        selectedColor = effectiveColor,
                        selectedWidth = effectiveWidth,
                        onStrokesChanged = handleStrokesChange,
                        isEraser = isEraserEnabled,
                        backgroundStyle = canvasBackground,
                        modifier = Modifier.fillMaxSize()
                    )
                    // S Pen hint pill
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 14.dp, bottom = 14.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit, null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                "S Pen · Long-press for tools",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                letterSpacing = 0.04.sp
                            )
                        }
                    }
                }
            },
            textSummarySection = { summaryMod ->
                NoteTextSection(
                    text = text,
                    onTextChange = onTextChange,
                    aiAssistantActive = aiAssistantActive,
                    lectureTranscribingActive = lectureTranscribingActive,
                    onPerformAiAssistant = onPerformAiAssistant,
                    onVoiceLectureTranscribe = onVoiceLectureTranscribe,
                    noteTitle = title,
                    modifier = summaryMod,
                )
            }
        )
    }
}

// ── 4. Drawing Toolbar ─────────────────────────────────────────────────────────

@Composable
private fun DrawingToolbar(
    drawTool: DrawTool,
    onToolChange: (DrawTool) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    backgroundStyle: String,
    onBackgroundStyleChange: (String) -> Unit,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear canvas?") },
            text = { Text("All drawings will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = { onClear(); showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Undo / Redo capsule
            ToolCapsule {
                IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Undo, "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                        modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Redo, "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                        modifier = Modifier.size(18.dp))
                }
            }

            // Drawing tools capsule
            ToolCapsule {
                DrawToolChip(Icons.Default.Edit,        drawTool == DrawTool.Pen)         { onToolChange(DrawTool.Pen) }
                DrawToolChip(Icons.Default.Gesture,     drawTool == DrawTool.Pencil)      { onToolChange(DrawTool.Pencil) }
                DrawToolChip(Icons.Default.Brush,       drawTool == DrawTool.Brush)       { onToolChange(DrawTool.Brush) }
                DrawToolChip(Icons.Default.BorderColor, drawTool == DrawTool.Highlighter) { onToolChange(DrawTool.Highlighter) }
            }

            // Eraser capsule
            ToolCapsule {
                DrawToolChip(Icons.Default.AutoFixNormal, drawTool == DrawTool.Eraser) { onToolChange(DrawTool.Eraser) }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Background style capsule
            ToolCapsule {
                listOf("blank" to "—", "ruled" to "≡", "grid" to "#", "dotted" to "⠿").forEach { (style, label) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (backgroundStyle == style) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { onBackgroundStyleChange(style) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (backgroundStyle == style) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Clear canvas
            IconButton(onClick = { showClearDialog = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.DeleteSweep, "Clear",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ToolCapsule(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

@Composable
private fun DrawToolChip(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── 5. Drawing Properties Strip ───────────────────────────────────────────────

@Composable
private fun DrawingPropertiesStrip(
    drawTool: DrawTool,
    penColor: Color,
    highlighterColor: Color,
    onPenColorChange: (Color) -> Unit,
    onHighlighterColorChange: (Color) -> Unit,
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
) {
    val isHighlighter = drawTool == DrawTool.Highlighter
    val palette       = if (isHighlighter) kHighlighterColors else kPenColors
    val selected      = if (isHighlighter) highlighterColor else penColor

    val widthRange = when (drawTool) {
        DrawTool.Pencil      -> 1f..12f
        DrawTool.Highlighter -> 15f..60f
        DrawTool.Brush       -> 4f..36f
        else                 -> 2f..28f
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Color dots
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                palette.forEach { color ->
                    val isSelected = selected == color
                    val displayColor = if (isHighlighter) color.copy(alpha = 0.75f) else color
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 30.dp else 25.dp)
                            .clip(CircleShape)
                            .background(displayColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clickable {
                                if (isHighlighter) onHighlighterColorChange(color)
                                else onPenColorChange(color)
                            }
                    )
                }
            }

            VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Visual size preview dot
            val fraction = (strokeWidth - widthRange.start) / (widthRange.endInclusive - widthRange.start)
            val previewDp = (4f + fraction * 24f).dp
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(previewDp)
                        .background(
                            if (isHighlighter) selected.copy(alpha = 0.55f) else selected,
                            CircleShape
                        )
                )
            }

            // Stroke width slider
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = widthRange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    }
}

// ── 6. Note Text Section ──────────────────────────────────────────────────────

@Composable
private fun NoteTextSection(
    text: String,
    onTextChange: (String) -> Unit,
    aiAssistantActive: Boolean,
    lectureTranscribingActive: Boolean,
    onPerformAiAssistant: (String) -> Unit,
    onVoiceLectureTranscribe: () -> Unit,
    noteTitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {

        // AI action chips
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
            shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text("Gemini Assistants", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { onPerformAiAssistant("format") },
                        label = {
                            if (aiAssistantActive) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)); Text("Working…", fontSize = 11.sp)
                                }
                            } else {
                                Text("Format Notes", fontSize = 11.sp)
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp)) },
                        enabled = text.isNotEmpty() && !aiAssistantActive,
                        modifier = Modifier.testTag("ai_format_notes_button"),
                    )
                    AssistChip(
                        onClick = { onPerformAiAssistant("explain") },
                        label = { Text("Concept Dive", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.School, null, modifier = Modifier.size(14.dp)) },
                        enabled = text.isNotEmpty() && !aiAssistantActive,
                        modifier = Modifier.testTag("ai_explain_notes_button"),
                    )
                }
            }
        }

        // Audio lecture bench (collapsible)
        AudioLectureBench(
            noteTitle = noteTitle,
            lectureTranscribingActive = lectureTranscribingActive,
            onVoiceLectureTranscribe = onVoiceLectureTranscribe,
        )

        // Markdown format toolbar
        MarkdownToolbar(text = text, onTextChange = onTextChange)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Text editor
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Transcribed text, notes, or AI-formatted content appears here…", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("note_text_input"),
            shape = RoundedCornerShape(0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        )

        // Word / char count
        val wordCount = remember(text) { if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size }
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                "$wordCount words · ${text.length} chars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun MarkdownToolbar(text: String, onTextChange: (String) -> Unit) {
    val insertMarkdown: (String) -> Unit = { tag ->
        val newText = when (tag) {
            "H1"        -> "$text\n# "
            "H2"        -> "$text\n## "
            "bold"      -> "$text**Bold** "
            "italic"    -> "$text*Italic* "
            "bullet"    -> "$text\n- "
            "todo"      -> "$text\n- [ ] "
            "timestamp" -> {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                "$text\n📅 ${sdf.format(java.util.Date())}: "
            }
            else -> text
        }
        onTextChange(newText)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("H1" to "H1", "H2" to "H2", "bold" to "B", "italic" to "I", "bullet" to "•", "todo" to "☑", "timestamp" to "🕒").forEach { (tag, label) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { insertMarkdown(tag) }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { onTextChange("") }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.DeleteSweep, "Clear text",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AudioLectureBench(
    noteTitle: String,
    lectureTranscribingActive: Boolean,
    onVoiceLectureTranscribe: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var isRecordingActive by remember { mutableStateOf(false) }
    var isRecordingPaused by remember { mutableStateOf(false) }
    var recordingTimeSeconds by remember { mutableStateOf(0) }
    val wavePeaks = remember { mutableStateListOf<Float>() }
    val localLectures = remember { mutableStateListOf<String>() }

    LaunchedEffect(isRecordingActive, isRecordingPaused) {
        if (isRecordingActive && !isRecordingPaused) {
            while (true) {
                kotlinx.coroutines.delay(100)
                wavePeaks.add((15..85).random().toFloat())
                if (wavePeaks.size > 24) wavePeaks.removeAt(0)
            }
        } else wavePeaks.clear()
    }
    LaunchedEffect(isRecordingActive, isRecordingPaused) {
        if (isRecordingActive && !isRecordingPaused) {
            while (true) { kotlinx.coroutines.delay(1000); recordingTimeSeconds += 1 }
        }
    }

    val formattedTime = remember(recordingTimeSeconds) {
        String.format("%02d:%02d", recordingTimeSeconds / 60, recordingTimeSeconds % 60)
    }
    val stopAndSave: () -> Unit = {
        isRecordingActive = false; isRecordingPaused = false
        val docTitle = if (noteTitle.startsWith("New Class Note") || noteTitle.isBlank()) "Lecture Session" else noteTitle
        localLectures.add("$docTitle – Audio ($formattedTime)")
        recordingTimeSeconds = 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isRecordingActive) Icons.Default.SettingsVoice else Icons.Default.Mic,
                        null,
                        tint = if (isRecordingActive) Color(0xFFE53935) else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Lecture Audio", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    if (isRecordingActive) {
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFE53935), CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text("Recording $formattedTime", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                    }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (!isRecordingActive) {
                            Button(
                                onClick = { isRecordingActive = true; isRecordingPaused = false; recordingTimeSeconds = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FiberManualRecord, null, Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Record", fontSize = 11.sp)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { isRecordingPaused = !isRecordingPaused },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(if (isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isRecordingPaused) "Resume" else "Pause", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = stopAndSave,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Stop, null, Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save", fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        // Waveform
                        val density = LocalDensity.current
                        val activeColor = Color(0xFFE53935).copy(alpha = 0.8f)
                        val inactiveColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barW = with(density) { 3.dp.toPx() }
                                val gap  = with(density) { 2.dp.toPx() }
                                val startX = with(density) { 6.dp.toPx() }
                                val midY = size.height / 2f
                                if (isRecordingActive && !isRecordingPaused) {
                                    wavePeaks.forEachIndexed { i, h ->
                                        val x = startX + i * (barW + gap)
                                        val hp = with(density) { h.dp.toPx() }.coerceAtMost(size.height * 0.8f)
                                        drawLine(activeColor, androidx.compose.ui.geometry.Offset(x, midY - hp / 2f), androidx.compose.ui.geometry.Offset(x, midY + hp / 2f), barW)
                                    }
                                } else {
                                    for (i in 0..20) {
                                        val x = startX + i * (barW + gap)
                                        drawLine(inactiveColor, androidx.compose.ui.geometry.Offset(x, midY - 3f), androidx.compose.ui.geometry.Offset(x, midY + 3f), barW)
                                    }
                                }
                            }
                        }
                    }
                    if (localLectures.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Saved Recordings", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            localLectures.forEach { lecture ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.AudioFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(lecture, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Button(
                                        onClick = onVoiceLectureTranscribe,
                                        modifier = Modifier.height(26.dp).testTag("ai_transcribe_audio_button"),
                                        enabled = !lectureTranscribingActive,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        if (lectureTranscribingActive) CircularProgressIndicator(Modifier.size(10.dp), color = Color.White)
                                        else {
                                            Icon(Icons.Default.GraphicEq, null, Modifier.size(10.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Transcribe", fontSize = 9.sp)
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
}

// ── 6b. Page tabs ─────────────────────────────────────────────────────────────

@Composable
private fun PageTabsRow(
    activePage: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
    onAddPage: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (i in 0 until pageCount) {
                PageTab(label = "Page ${i + 1}", selected = activePage == i, onClick = { onPageChange(i) })
            }
            PageTab(label = "+ Add page", selected = false, faint = true, onClick = onAddPage)
        }
    }
}

@Composable
private fun PageTab(
    label: String,
    selected: Boolean,
    faint: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                selected -> MaterialTheme.colorScheme.primary
                faint    -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else     -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 5.dp, bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                )
        )
    }
}

// ── 7. Empty workspace state ──────────────────────────────────────────────────

@Composable
fun EmptyWorkspaceState(onCreateNote: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(96.dp).background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f)
                        )
                    ), CircleShape
                ))
                Box(
                    modifier = Modifier.size(68.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EditCalendar, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Select a note to begin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Choose a note from the sidebar or start a new one",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Button(onClick = onCreateNote, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Note", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── 8. Responsive split layout ────────────────────────────────────────────────

@Composable
fun ColumnScope.ResponsiveSplitLayout(
    isTablet: Boolean,
    canvasSection: @Composable (Modifier) -> Unit,
    textSummarySection: @Composable (Modifier) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    if (isTablet) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            canvasSection(Modifier.weight(1.2f).fillMaxHeight())
            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            textSummarySection(Modifier.weight(0.8f).fillMaxHeight())
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
                    text = { Text("Drawing", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Gesture, null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Notes", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp)) }
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (selectedTab == 0) canvasSection(Modifier.fillMaxSize())
                else textSummarySection(Modifier.fillMaxSize())
            }
        }
    }
}
