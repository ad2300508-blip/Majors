package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun dueDateUrgency(dueDate: String): Int {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val due = sdf.parse(dueDate) ?: return 2
        val diffDays = (due.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
        when {
            diffDays < 0  -> 0  // overdue
            diffDays <= 2 -> 1  // soon
            else          -> 2  // normal
        }
    } catch (e: Exception) { 2 }
}

@Composable
private fun urgencyColor(urgency: Int): Color = when (urgency) {
    0    -> MaterialTheme.colorScheme.error
    1    -> Color(0xFFF57C00)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

// ─── 1. DASHBOARD ─────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onCreateNote: () -> Unit,
    onNavigateToPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val courses     by viewModel.courses.collectAsState()
    val notes       by viewModel.notes.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val pending = assignments.filter { !it.isCompleted }
    var showAddCourseDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth > 720.dp
        val pad = if (isTablet) 24.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = pad)
        ) {
            Spacer(modifier = Modifier.height(pad))

            // ── Hero banner ───────────────────────────────────────────────────
            DashboardHeroBanner(isTablet = isTablet, onCreateNote = onCreateNote)

            Spacer(modifier = Modifier.height(if (isTablet) 20.dp else 14.dp))

            // ── Metric row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 10.dp)
            ) {
                StatChip(
                    label = "Courses",
                    value = courses.size,
                    icon = Icons.Default.School,
                    onClick = { onNavigateToPage("courses") },
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Notes",
                    value = notes.size,
                    icon = Icons.Default.Description,
                    onClick = { onNavigateToPage("notes") },
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Due",
                    value = pending.size,
                    icon = Icons.Default.Assignment,
                    onClick = { onNavigateToPage("courses") },
                    urgent = pending.any { dueDateUrgency(it.dueDate) < 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(if (isTablet) 20.dp else 14.dp))

            if (isTablet) {
                WeeklyScheduleSection(courses = courses, onAddClassClick = { showAddCourseDialog = true }, isTablet = true)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DashboardRecentNotesCard(notes = notes, courses = courses, viewModel = viewModel, onNavigate = onNavigateToPage, modifier = Modifier.weight(1.2f))
                    DashboardPendingCard(pending = pending, courses = courses, modifier = Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    WeeklyScheduleSection(courses = courses, onAddClassClick = { showAddCourseDialog = true }, isTablet = false)
                    DashboardRecentNotesCard(notes = notes, courses = courses, viewModel = viewModel, onNavigate = onNavigateToPage)
                    DashboardPendingCard(pending = pending, courses = courses)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (showAddCourseDialog) {
            AddCourseDialog(onDismiss = { showAddCourseDialog = false }, onConfirm = { name, code, instructor, color, schedule ->
                viewModel.addCourse(name, code, instructor, color, schedule)
                showAddCourseDialog = false
            })
        }
    }
}

@Composable
private fun DashboardHeroBanner(isTablet: Boolean, onCreateNote: () -> Unit) {
    val todayFormatted = remember {
        java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(java.util.Date())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet) 160.dp else 130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    0f to Color(0xFF1A3A8F),
                    0.6f to Color(0xFF2A52BE),
                    1f to Color(0xFF1A7A6E),
                )
            )
    ) {
        // subtle decorative circle
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-30).dp, y = (-40).dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = 30.dp)
                .background(Color.White.copy(alpha = 0.04f), CircleShape)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Welcome Back, Scholar",
                    style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    todayFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            Button(
                onClick = onCreateNote,
                modifier = Modifier.testTag("dashboard_new_note_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isTablet) "Quick S-Pen Note" else "New Note", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    urgent: Boolean = false,
) {
    val bg = if (urgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
             else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardRecentNotesCard(
    notes: List<Note>,
    courses: List<Course>,
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { onNavigate("notes") }) { Text("View All") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (notes.isEmpty()) {
                EmptyStateMinimal(icon = Icons.Default.BorderColor, text = "No notes yet")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.take(3).forEach { note ->
                        NoteRowItem(note = note, courses = courses) {
                            viewModel.setActiveNote(note.id)
                            onNavigate("notes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardPendingCard(
    pending: List<Assignment>,
    courses: List<Course>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Upcoming Deadlines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            if (pending.isEmpty()) {
                EmptyStateMinimal(icon = Icons.Default.CheckCircle, text = "All caught up!")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pending.take(5).forEach { assignment ->
                        val course = courses.find { it.id == assignment.courseId }
                        val urgency = remember(assignment.dueDate) { dueDateUrgency(assignment.dueDate) }
                        AssignmentRow(assignment = assignment, course = course, urgency = urgency)
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentRow(assignment: Assignment, course: Course?, urgency: Int) {
    val courseColor = course?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(courseColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Event, contentDescription = null, tint = courseColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(assignment.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Due ${assignment.dueDate}", style = MaterialTheme.typography.labelSmall, color = urgencyColor(urgency))
                if (course != null) {
                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text(course.code, style = MaterialTheme.typography.labelSmall, color = courseColor)
                }
            }
        }
        if (urgency == 0) {
            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.errorContainer) {
                Text("OVERDUE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        } else if (urgency == 1) {
            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF57C00).copy(alpha = 0.15f)) {
                Text("SOON", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFF57C00), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyStateMinimal(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NoteRowItem(note: Note, courses: List<Course>, onClick: () -> Unit) {
    val course = courses.find { it.id == note.courseId }
    val courseColor = course?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent dot
        Box(
            modifier = Modifier
                .size(38.dp)
                .background((courseColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = courseColor ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(note.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (note.textContent.isNotEmpty()) note.textContent else "S-Pen drawing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (course != null) {
                Surface(shape = RoundedCornerShape(6.dp), color = (courseColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.2f)) {
                    Text(course.code, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = courseColor ?: MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(note.getRelativeTime(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

// ─── 2. COURSES ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val courses     by viewModel.courses.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val notes       by viewModel.notes.collectAsState()

    var showAddCourseDialog     by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var courseToDelete          by remember { mutableStateOf<Course?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth > 720.dp
        val pad = if (isTablet) 24.dp else 16.dp

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = pad)) {
            Spacer(modifier = Modifier.height(pad))
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("My Courses", style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${courses.size} enrolled · ${assignments.count { !it.isCompleted }} pending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = { showAddAssignmentDialog = true }, modifier = Modifier.testTag("add_assignment_trigger")) {
                        Icon(Icons.Default.AddAlert, contentDescription = "Add Assignment")
                    }
                    FilledIconButton(onClick = { showAddCourseDialog = true }, modifier = Modifier.testTag("add_course_trigger")) {
                        Icon(Icons.Default.Add, contentDescription = "Enroll Class")
                    }
                }
            }
            Spacer(modifier = Modifier.height(if (isTablet) 20.dp else 16.dp))

            if (isTablet) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text("Enrolled Courses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        if (courses.isEmpty()) {
                            CourseEmptyState(onAdd = { showAddCourseDialog = true })
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(courses) { course ->
                                    CourseCard(
                                        course = course,
                                        noteCount = notes.count { it.courseId == course.id },
                                        onDelete = { courseToDelete = course }
                                    )
                                }
                            }
                        }
                    }
                    Surface(modifier = Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                        AssignmentPanel(assignments = assignments, courses = courses, viewModel = viewModel)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (courses.isEmpty()) {
                        CourseEmptyState(onAdd = { showAddCourseDialog = true })
                    } else {
                        courses.forEach { course ->
                            CourseCard(
                                course = course,
                                noteCount = notes.count { it.courseId == course.id },
                                onDelete = { courseToDelete = course },
                                compact = true
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Assignments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (assignments.isEmpty()) {
                        EmptyStateMinimal(icon = Icons.Default.CheckCircle, text = "No assignments yet")
                    } else {
                        assignments.forEach { assignment ->
                            val course = courses.find { it.id == assignment.courseId }
                            val urgency = remember(assignment.dueDate) { dueDateUrgency(assignment.dueDate) }
                            AssignmentListItem(assignment = assignment, course = course, urgency = urgency, onToggle = { viewModel.toggleAssignmentCompleted(assignment) }, onDelete = { viewModel.deleteAssignment(assignment.id) })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (showAddCourseDialog) {
            AddCourseDialog(
                onDismiss = { showAddCourseDialog = false },
                onConfirm = { name, code, instructor, color, schedule ->
                    viewModel.addCourse(name, code, instructor, color, schedule)
                    showAddCourseDialog = false
                }
            )
        }
        if (showAddAssignmentDialog) {
            AddAssignmentDialog(courses = courses, onDismiss = { showAddAssignmentDialog = false }, onConfirm = { title, due, courseId, notes ->
                viewModel.addAssignment(title, due, courseId, notes)
                showAddAssignmentDialog = false
            })
        }
        courseToDelete?.let { course ->
            val noteCount = notes.count { it.courseId == course.id }
            AlertDialog(
                onDismissRequest = { courseToDelete = null },
                title = { Text("Remove Course?") },
                text = {
                    Column {
                        Text("Remove \"${course.name}\" from your schedule?")
                        if (noteCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$noteCount note${if (noteCount != 1) "s" else ""} will become unlinked.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = { Button(onClick = { viewModel.deleteCourse(course.id); courseToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove") } },
                dismissButton = { TextButton(onClick = { courseToDelete = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun CourseCard(course: Course, noteCount: Int, onDelete: () -> Unit, compact: Boolean = false) {
    val color = Color(android.graphics.Color.parseColor(course.colorHex))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Color header strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(color)
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
                        Text(course.code, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(course.name, style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(course.instructor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(course.schedule, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(13.dp), tint = color.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("$noteCount note${if (noteCount != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseEmptyState(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text("No courses enrolled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Add your first class to get started", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enroll in a Course")
            }
        }
    }
}

@Composable
private fun AssignmentPanel(assignments: List<Assignment>, courses: List<Course>, viewModel: AppViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Assignments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        if (assignments.isEmpty()) {
            EmptyStateMinimal(icon = Icons.Default.CheckCircle, text = "No active assignments")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(assignments) { assignment ->
                    val course = courses.find { it.id == assignment.courseId }
                    val urgency = remember(assignment.dueDate) { dueDateUrgency(assignment.dueDate) }
                    AssignmentListItem(
                        assignment = assignment,
                        course = course,
                        urgency = urgency,
                        onToggle = { viewModel.toggleAssignmentCompleted(assignment) },
                        onDelete = { viewModel.deleteAssignment(assignment.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AssignmentListItem(
    assignment: Assignment,
    course: Course?,
    urgency: Int,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val courseColor = course?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
    val alpha = if (assignment.isCompleted) 0.5f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar (course color)
        Box(modifier = Modifier.width(3.dp).height(52.dp).background(courseColor))
        Checkbox(
            checked = assignment.isCompleted,
            onCheckedChange = { onToggle() },
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                assignment.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Due ${assignment.dueDate}", style = MaterialTheme.typography.labelSmall, color = urgencyColor(urgency).copy(alpha = alpha))
                if (urgency == 0) {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Text("OVERDUE", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Shared Dialogs ────────────────────────────────────────────────────────────

@Composable
fun AddCourseDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var instructor by remember { mutableStateOf("") }
    var timeInput by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    val colorOptions = listOf("#1E88E5", "#D81B60", "#43A047", "#8E24AA", "#FFB300", "#00ACC1")
    var selectedColor by remember { mutableStateOf(colorOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enroll in Course") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Course Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = instructor, onValueChange = { instructor = it }, label = { Text("Instructor") }, modifier = Modifier.fillMaxWidth())
                Text("Class Days", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Mon","Tue","Wed","Thu","Fri").forEach { day ->
                        val sel = selectedDays.contains(day)
                        FilterChip(selected = sel, onClick = { selectedDays = if (sel) selectedDays - day else selectedDays + day }, label = { Text(day, fontSize = 11.sp) })
                    }
                }
                OutlinedTextField(value = timeInput, onValueChange = { timeInput = it }, placeholder = { Text("10:00 AM – 11:30 AM") }, label = { Text("Time") }, modifier = Modifier.fillMaxWidth())
                Text("Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { col ->
                        Box(
                            modifier = Modifier.size(36.dp).background(Color(android.graphics.Color.parseColor(col)), CircleShape)
                                .border(if (selectedColor == col) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { selectedColor = col }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotEmpty() && code.isNotEmpty()) {
                    val schedule = if (selectedDays.isNotEmpty()) "${selectedDays.joinToString(", ")} ${timeInput.trim()}" else timeInput.trim()
                    onConfirm(name, code, instructor, selectedColor, schedule)
                }
            }) { Text("Enroll") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddAssignmentDialog(courses: List<Course>, onDismiss: () -> Unit, onConfirm: (String, String, Long?, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Assignment") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, placeholder = { Text("YYYY-MM-DD") }, label = { Text("Due Date") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                if (courses.isNotEmpty()) {
                    Text("Link to Course", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    courses.forEach { course ->
                        Row(Modifier.fillMaxWidth().clickable { selectedCourseId = course.id }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedCourseId == course.id, onClick = { selectedCourseId = course.id })
                            Spacer(Modifier.width(8.dp))
                            Text("${course.code} – ${course.name}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotEmpty()) onConfirm(title, dueDate, selectedCourseId, notes) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─── 3. FLASHCARDS ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStudyScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val quizCards      by viewModel.quizFlashcards.collectAsState()
    val allFlashcards  by viewModel.allFlashcards.collectAsState()
    val activeNoteId   by viewModel.activeNoteId.collectAsState()
    val generationActive by viewModel.flashcardGenerationActive.collectAsState()

    val displayCards = if (activeNoteId != null) quizCards else allFlashcards
    var cardIndex  by remember { mutableStateOf(0) }
    var flipStatus by remember { mutableStateOf(false) }
    var correct    by remember { mutableStateOf(0) }
    var total      by remember { mutableStateOf(0) }

    LaunchedEffect(displayCards.size) {
        if (displayCards.isNotEmpty() && cardIndex >= displayCards.size) {
            cardIndex = displayCards.size - 1
            flipStatus = false
        }
    }

    val cardRotation by animateFloatAsState(
        targetValue = if (flipStatus) 180f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "CardFlip"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth > 720.dp
        val pad = if (isTablet) 24.dp else 16.dp

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = pad).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(pad))

            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Study Flashcards", style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (activeNoteId != null) "${displayCards.size} cards from active note" else "All ${allFlashcards.size} cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (activeNoteId != null) {
                    FilledTonalButton(
                        onClick = { viewModel.generateAIStudyFlashcards() },
                        enabled = !generationActive,
                        modifier = Modifier.testTag("ai_generate_deck_button")
                    ) {
                        if (generationActive) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Generate")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (displayCards.isEmpty()) {
                Spacer(modifier = Modifier.height(60.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CardMembership, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    Text("No flashcards yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Open a note and generate cards with Gemini AI", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else {
                val safeIndex  = cardIndex.coerceIn(0, displayCards.size - 1)
                val activeCard = displayCards[safeIndex]

                // Progress bar
                LinearProgressIndicator(
                    progress = { (safeIndex + 1).toFloat() / displayCards.size },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${safeIndex + 1} / ${displayCards.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Flip card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .height(if (isTablet) 300.dp else 240.dp)
                        .graphicsLayer {
                            rotationY = cardRotation
                            cameraDistance = 10f * density
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (cardRotation > 90f)
                                Brush.linearGradient(listOf(Color(0xFF0A2E9E), Color(0xFF1A3A8F)))
                            else
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant))
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        .clickable { flipStatus = !flipStatus }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp).graphicsLayer { if (cardRotation > 90f) rotationY = 180f },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cardRotation > 90f) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.15f)) {
                                    Text("ANSWER", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(activeCard.answer, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Color.White)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("QUESTION", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(activeCard.question, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    // tap hint
                    if (cardRotation < 10f) {
                        Text(
                            "Tap to flip",
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation row
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { flipStatus = false; cardIndex = (cardIndex - 1 + displayCards.size) % displayCards.size }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev")
                    }
                    Button(onClick = { flipStatus = false; cardIndex = (cardIndex + 1) % displayCards.size }) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.deleteFlashcard(activeCard.id) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete card", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }

                // Reveal answer buttons
                AnimatedVisibility(visible = flipStatus, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.markFlashcardReviewed(activeCard.id, false); total++; flipStatus = false; cardIndex = (cardIndex + 1) % displayCards.size },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Missed it")
                        }
                        Button(
                            onClick = { viewModel.markFlashcardReviewed(activeCard.id, true); correct++; total++; flipStatus = false; cardIndex = (cardIndex + 1) % displayCards.size },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Knew it!")
                        }
                    }
                }

                // Session score pill
                if (total > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val scoreRatio = correct.toFloat() / total
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (scoreRatio >= 0.7f) Color(0xFF1B5E20) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (scoreRatio >= 0.7f) Icons.Default.EmojiEvents else Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (scoreRatio >= 0.7f) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("$correct / $total correct · ${(scoreRatio * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = if (scoreRatio >= 0.7f) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Manual card creator
            if (activeNoteId != null) {
                val noteId = activeNoteId!!
                var manQ by remember { mutableStateOf("") }
                var manA by remember { mutableStateOf("") }
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Card Manually", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedTextField(value = manQ, onValueChange = { manQ = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = manA, onValueChange = { manA = it }, label = { Text("Answer") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(
                            modifier = Modifier.align(Alignment.End),
                            onClick = {
                                if (manQ.isNotEmpty() && manA.isNotEmpty()) {
                                    viewModel.addFlashcardManual(noteId, manQ, manA)
                                    manQ = ""; manA = ""
                                }
                            }
                        ) { Text("Save Card") }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ─── 4. SYNC HUB ─────────────────────────────────────────────────────────────

@Composable
fun SyncHubScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val email     by viewModel.googleAccountEmail.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val notes     by viewModel.notes.collectAsState()
    var tokenInput by remember { mutableStateOf("") }
    val syncedCount = notes.count { it.isSynced }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {

        Text("Cloud Sync Hub", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Backup notebooks securely to Google Drive.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth > 560.dp) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SyncOAuthCard(email = email, tokenInput = tokenInput, onTokenInput = { tokenInput = it }, onConnect = { viewModel.connectGoogleAccount("workspace.student@gmail.com", it) }, onDemo = { viewModel.connectGoogleAccount("demo@scholar-space.app", "MOCK_DEMO_TOKEN") }, onDisconnect = { viewModel.disconnectGoogleAccount() }, modifier = Modifier.weight(1.2f))
                    SyncStatusCard(syncedCount = syncedCount, total = notes.size, syncState = syncState, modifier = Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SyncOAuthCard(email = email, tokenInput = tokenInput, onTokenInput = { tokenInput = it }, onConnect = { viewModel.connectGoogleAccount("workspace.student@gmail.com", it) }, onDemo = { viewModel.connectGoogleAccount("demo@scholar-space.app", "MOCK_DEMO_TOKEN") }, onDisconnect = { viewModel.disconnectGoogleAccount() })
                    SyncStatusCard(syncedCount = syncedCount, total = notes.size, syncState = syncState)
                }
            }
        }
    }
}

@Composable
private fun SyncOAuthCard(
    email: String,
    tokenInput: String,
    onTokenInput: (String) -> Unit,
    onConnect: (String) -> Unit,
    onDemo: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Google Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (email.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFF1B5E20), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("CONNECTED", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        Text(email, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedButton(onClick = onDisconnect, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)) {
                    Text("Disconnect")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("NOT CONNECTED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text("Sign in to sync notes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(value = tokenInput, onValueChange = onTokenInput, label = { Text("OAuth Token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { if (tokenInput.isNotEmpty()) { onConnect(tokenInput) } }, modifier = Modifier.weight(1f)) { Text("Connect") }
                    OutlinedButton(onClick = onDemo, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Demo")
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusCard(syncedCount: Int, total: Int, syncState: SyncState, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Sync Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Synced ratio
            val ratio = if (total > 0) syncedCount.toFloat() / total else 0f
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Synced notebooks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$syncedCount / $total", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = if (syncedCount == total && total > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when (syncState) {
                    is SyncState.Idle -> {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        Text("Standby", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is SyncState.Syncing -> {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Uploading to Drive…", style = MaterialTheme.typography.bodySmall)
                    }
                    is SyncState.Success -> {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        Text("Sync complete!", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                    }
                    is SyncState.Error -> {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(syncState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 2)
                    }
                }
            }
        }
    }
}

// ─── Metric cards ─────────────────────────────────────────────────────────────

@Composable
fun MiniMetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(76.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(100.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
    }
}

// ─── Weekly schedule ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleSection(courses: List<Course>, onAddClassClick: () -> Unit, isTablet: Boolean) {
    val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday")
    val currentDay = remember {
        val cal = java.util.Calendar.getInstance()
        when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY    -> "Monday"
            java.util.Calendar.TUESDAY   -> "Tuesday"
            java.util.Calendar.WEDNESDAY -> "Wednesday"
            java.util.Calendar.THURSDAY  -> "Thursday"
            java.util.Calendar.FRIDAY    -> "Friday"
            else -> "Monday"
        }
    }
    var selectedDay by remember { mutableStateOf(currentDay) }
    val dayCourses = courses.filter { isCourseDay(it.schedule, selectedDay) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("weekly_schedule_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Weekly Timetable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onAddClassClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (isTablet) 10.dp else 4.dp)) {
                days.forEach { day ->
                    val sel = selectedDay == day
                    val isToday = day == currentDay
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    sel -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable { selectedDay = day }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTablet) day else day.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (sel || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                sel -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (dayCourses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(72.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No classes on this day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    dayCourses.forEach { course ->
                        val color = Color(android.graphics.Color.parseColor(course.colorHex))
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
                            border = BorderStroke(1.2.dp, color.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(modifier = Modifier.background(color, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(course.code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(course.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(course.instructor, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(course.schedule, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    if (dayCourses.size == 1 && isTablet) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

fun isCourseDay(scheduleStr: String, day: String): Boolean {
    val s = scheduleStr.lowercase()
    return when (day.lowercase()) {
        "monday"    -> s.contains("mon")
        "tuesday"   -> s.contains("tue")
        "wednesday" -> s.contains("wed")
        "thursday"  -> s.contains("thu")
        "friday"    -> s.contains("fri")
        else        -> false
    }
}
