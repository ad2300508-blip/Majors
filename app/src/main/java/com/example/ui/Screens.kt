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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.delay

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
    val courses          by viewModel.courses.collectAsState()
    val notes            by viewModel.notes.collectAsState()
    val assignments      by viewModel.assignments.collectAsState()
    val pomodoroWorkMins by viewModel.pomodoroWorkMins.collectAsState()
    val pomodoroBreakMins by viewModel.pomodoroBreakMins.collectAsState()
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

            Spacer(modifier = Modifier.height(if (isTablet) 12.dp else 10.dp))

            TodayFocusCard(pending = pending, courses = courses)

            Spacer(modifier = Modifier.height(if (isTablet) 12.dp else 10.dp))

            if (isTablet) {
                // Pomodoro + Schedule side by side on tablet
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PomodoroTimerCard(workMins = pomodoroWorkMins, breakMins = pomodoroBreakMins, modifier = Modifier.width(240.dp))
                    WeeklyScheduleSection(courses = courses, onAddClassClick = { showAddCourseDialog = true }, isTablet = true, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DashboardRecentNotesCard(notes = notes, courses = courses, viewModel = viewModel, onNavigate = onNavigateToPage, modifier = Modifier.weight(1.2f))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MiniCalendarCard(assignments = assignments, courses = courses)
                        DashboardPendingCard(pending = pending, courses = courses)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PomodoroTimerCard(workMins = pomodoroWorkMins, breakMins = pomodoroBreakMins)
                    WeeklyScheduleSection(courses = courses, onAddClassClick = { showAddCourseDialog = true }, isTablet = false)
                    MiniCalendarCard(assignments = assignments, courses = courses)
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
private fun MiniCalendarCard(assignments: List<Assignment>, courses: List<Course>, modifier: Modifier = Modifier) {
    val today = remember {
        java.util.Calendar.getInstance().let {
            Triple(it.get(java.util.Calendar.YEAR), it.get(java.util.Calendar.MONTH), it.get(java.util.Calendar.DAY_OF_MONTH))
        }
    }
    var displayYear  by remember { mutableStateOf(today.first) }
    var displayMonth by remember { mutableStateOf(today.second) }

    val firstDayOfWeek = remember(displayYear, displayMonth) {
        java.util.Calendar.getInstance().apply { set(displayYear, displayMonth, 1) }
            .get(java.util.Calendar.DAY_OF_WEEK) - 1
    }
    val daysInMonth = remember(displayYear, displayMonth) {
        java.util.Calendar.getInstance().apply { set(displayYear, displayMonth, 1) }
            .getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    }
    val monthLabel = remember(displayYear, displayMonth) {
        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(
            java.util.Calendar.getInstance().apply { set(displayYear, displayMonth, 1) }.time
        )
    }
    val dayDots = remember(assignments, displayYear, displayMonth) {
        val map = mutableMapOf<Int, List<String>>()
        assignments.filter { !it.isCompleted }.forEach { a ->
            runCatching {
                val parts = a.dueDate.split("-")
                val y = parts[0].toInt(); val m = parts[1].toInt() - 1; val d = parts[2].toInt()
                if (y == displayYear && m == displayMonth) {
                    val hex = courses.find { it.id == a.courseId }?.colorHex ?: "#1E88E5"
                    map[d] = (map[d] ?: emptyList()) + hex
                }
            }
        }
        map
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth--
                }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(monthLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = {
                    if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++
                }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S","M","T","W","T","F","S").forEach { d ->
                    Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(4.dp))
            val rows = ((firstDayOfWeek + daysInMonth) + 6) / 7
            repeat(rows) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val day = row * 7 + col - firstDayOfWeek + 1
                        val isToday = day == today.third && displayMonth == today.second && displayYear == today.first
                        val dots = dayDots[day]
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (day in 1..daysInMonth) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Box(
                                        modifier = Modifier.size(22.dp).background(
                                            if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$day", style = MaterialTheme.typography.labelSmall,
                                            color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                                    }
                                    if (!dots.isNullOrEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.height(5.dp)) {
                                            dots.take(3).forEach { hex ->
                                                Box(modifier = Modifier.size(4.dp).background(
                                                    Color(android.graphics.Color.parseColor(hex)), CircleShape))
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
}

@Composable
private fun TodayFocusCard(pending: List<Assignment>, courses: List<Course>, modifier: Modifier = Modifier) {
    val mostUrgent = remember(pending) {
        pending.sortedWith(compareBy { dueDateUrgency(it.dueDate) }).firstOrNull()
    }
    val urgency = remember(mostUrgent) { mostUrgent?.let { dueDateUrgency(it.dueDate) } }
    val isOverdue = urgency == 0
    val containerColor = when {
        isOverdue -> MaterialTheme.colorScheme.errorContainer
        urgency == 1 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (mostUrgent == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (mostUrgent == null) Icons.Default.CheckCircle else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Today's Focus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (mostUrgent == null) {
                    Text("All caught up!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("No pending assignments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(mostUrgent.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        val course = courses.find { it.id == mostUrgent.courseId }
                        Text(
                            text = when (urgency) {
                                0 -> "Overdue"
                                1 -> "Due today"
                                2 -> "Due tomorrow"
                                else -> "Due ${mostUrgent.dueDate}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (course != null) {
                            Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text(course.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (mostUrgent != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when (urgency) {
                            0 -> "!"
                            1 -> "Today"
                            2 -> "1 day"
                            else -> "${urgency}d"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
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
fun CoursesScreen(viewModel: AppViewModel, onNavigateToPage: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    val courses     by viewModel.courses.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val notes       by viewModel.notes.collectAsState()
    val allGrades   by viewModel.allGrades.collectAsState()

    var detailCourse            by remember { mutableStateOf<Course?>(null) }
    var showAddCourseDialog     by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var showAddGradeDialog      by remember { mutableStateOf(false) }
    var courseToDelete          by remember { mutableStateOf<Course?>(null) }
    var selectedTab             by remember { mutableStateOf(0) }

    AnimatedContent(
        targetState = detailCourse,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        modifier = modifier.fillMaxSize(),
        label = "CourseNav"
    ) { activeCourse ->
    if (activeCourse != null) {
        CourseDetailScreen(
            course = activeCourse,
            viewModel = viewModel,
            onBack = { detailCourse = null },
            onOpenNote = { noteId ->
                viewModel.setActiveNote(noteId)
                onNavigateToPage("notes")
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth > 720.dp
        val pad = if (isTablet) 24.dp else 16.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = pad).padding(top = pad),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Academy", style = if (isTablet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${courses.size} courses · ${assignments.count { !it.isCompleted }} pending · ${allGrades.size} grades", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (selectedTab) {
                        0 -> FilledIconButton(onClick = { showAddCourseDialog = true }, modifier = Modifier.testTag("add_course_trigger")) {
                            Icon(Icons.Default.Add, contentDescription = "Enroll")
                        }
                        1 -> FilledTonalIconButton(onClick = { showAddGradeDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Grade")
                        }
                        2 -> FilledTonalIconButton(onClick = { showAddAssignmentDialog = true }, modifier = Modifier.testTag("add_assignment_trigger")) {
                            Icon(Icons.Default.AddAlert, contentDescription = "Add Assignment")
                        }
                    }
                }
            }

            // ── Tab row ───────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth().padding(horizontal = pad / 2),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Courses", style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(Icons.Default.Class, contentDescription = null, modifier = Modifier.size(16.dp)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Grades", style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(16.dp)) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    text = { Text("Deadlines", style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp)) })
            }

            // ── Tab content ───────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = pad)) {
                when (selectedTab) {
                    0 -> CourseListContent(
                        courses = courses,
                        notes = notes,
                        isTablet = isTablet,
                        onAddCourse = { showAddCourseDialog = true },
                        onDeleteCourse = { courseToDelete = it },
                        onCourseClick = { detailCourse = it }
                    )
                    1 -> GradeTrackerContent(
                        courses = courses,
                        allGrades = allGrades,
                        isTablet = isTablet,
                        onDeleteGrade = { viewModel.deleteGrade(it) }
                    )
                    2 -> DeadlinesContent(
                        assignments = assignments,
                        courses = courses,
                        viewModel = viewModel
                    )
                }
            }
        }

        // ── Dialogs ───────────────────────────────────────────────────────────
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
            AddAssignmentDialog(courses = courses, onDismiss = { showAddAssignmentDialog = false }, onConfirm = { title, due, courseId, assignNotes, priority ->
                viewModel.addAssignment(title, due, courseId, assignNotes, priority)
                showAddAssignmentDialog = false
            })
        }
        if (showAddGradeDialog) {
            AddGradeDialog(courses = courses, onDismiss = { showAddGradeDialog = false }, onConfirm = { courseId, label, score, max, type, weight ->
                viewModel.addGrade(courseId, label, score, max, type, weight)
                showAddGradeDialog = false
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
    } // else (no detailCourse)
    } // AnimatedContent
}

@Composable
private fun CourseListContent(
    courses: List<Course>,
    notes: List<Note>,
    isTablet: Boolean,
    onAddCourse: () -> Unit,
    onDeleteCourse: (Course) -> Unit,
    onCourseClick: (Course) -> Unit = {},
) {
    if (courses.isEmpty()) {
        CourseEmptyState(onAdd = onAddCourse)
        return
    }
    if (isTablet) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            items(courses) { course ->
                CourseCard(course = course, noteCount = notes.count { it.courseId == course.id }, onDelete = { onDeleteCourse(course) }, onClick = { onCourseClick(course) })
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
            items(courses) { course ->
                CourseCard(course = course, noteCount = notes.count { it.courseId == course.id }, onDelete = { onDeleteCourse(course) }, onClick = { onCourseClick(course) }, compact = true)
            }
        }
    }
}

@Composable
private fun DeadlinesContent(assignments: List<Assignment>, courses: List<Course>, viewModel: AppViewModel) {
    if (assignments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateMinimal(icon = Icons.Default.CheckCircle, text = "No active assignments")
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
        items(assignments) { assignment ->
            val course = courses.find { it.id == assignment.courseId }
            val urgency = remember(assignment.dueDate) { dueDateUrgency(assignment.dueDate) }
            AssignmentListItem(
                assignment = assignment, course = course, urgency = urgency,
                onToggle = { viewModel.toggleAssignmentCompleted(assignment) },
                onDelete = { viewModel.deleteAssignment(assignment.id) }
            )
        }
    }
}

@Composable
fun GradeTrackerContent(
    courses: List<Course>,
    allGrades: List<Grade>,
    isTablet: Boolean,
    onDeleteGrade: (Long) -> Unit,
) {
    if (courses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateMinimal(icon = Icons.Default.Grade, text = "Enroll in courses to track grades")
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
        items(courses) { course ->
            val courseGrades = allGrades.filter { it.courseId == course.id }
            val gpa = if (courseGrades.isEmpty()) null else {
                val weightedSum = courseGrades.sumOf { (it.score / it.maxScore * it.weight).toDouble() }
                val totalWeight = courseGrades.sumOf { it.weight.toDouble() }
                if (totalWeight > 0) (weightedSum / totalWeight * 100).toFloat() else null
            }
            val courseColor = Color(android.graphics.Color.parseColor(course.colorHex))
            var expanded by remember { mutableStateOf(true) }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    // Color strip
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(courseColor))
                    // Course header row
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(6.dp), color = courseColor.copy(alpha = 0.15f)) {
                                Text(course.code, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium, color = courseColor, fontWeight = FontWeight.Bold)
                            }
                            Text(course.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (gpa != null) {
                                val grade = Grade(courseId = course.id, label = "", score = gpa, maxScore = 100f)
                                GpaChip(percentage = gpa, letterGrade = grade.letterGrade)
                            } else {
                                Text("No grades", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (courseGrades.isEmpty()) {
                                Text("No grades recorded for this course yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                            } else {
                                courseGrades.forEach { g ->
                                    GradeRow(grade = g, onDelete = { onDeleteGrade(g.id) })
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
private fun GpaChip(percentage: Float, letterGrade: String) {
    val color = when {
        percentage >= 90f -> Color(0xFF2E7D32)
        percentage >= 80f -> Color(0xFF1565C0)
        percentage >= 70f -> Color(0xFFF57C00)
        percentage >= 60f -> Color(0xFFE65100)
        else              -> MaterialTheme.colorScheme.error
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(letterGrade, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = color)
            Text("${"%.1f".format(percentage)}%", style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun GradeRow(grade: Grade, onDelete: () -> Unit) {
    val typeColor = when (grade.type) {
        "exam"     -> Color(0xFF1565C0)
        "quiz"     -> Color(0xFF6A1B9A)
        "homework" -> Color(0xFF2E7D32)
        "project"  -> Color(0xFFF57C00)
        else       -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(grade.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = typeColor.copy(alpha = 0.12f)) {
                    Text(grade.type.replaceFirstChar { it.uppercase() }, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = typeColor)
                }
                Text("${grade.score.toInt()} / ${grade.maxScore.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        GpaChip(percentage = grade.percentage, letterGrade = grade.letterGrade)
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun AddGradeDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, Float, Float, String, Float) -> Unit,
) {
    var selectedCourseId by remember { mutableStateOf(courses.firstOrNull()?.id) }
    var label  by remember { mutableStateOf("") }
    var score  by remember { mutableStateOf("") }
    var max    by remember { mutableStateOf("100") }
    var weight by remember { mutableStateOf("1") }
    val types  = listOf("exam", "quiz", "homework", "project", "other")
    var selectedType by remember { mutableStateOf("exam") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Grade Entry") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (courses.isNotEmpty()) {
                    Text("Course", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        courses.forEach { course ->
                            val color = Color(android.graphics.Color.parseColor(course.colorHex))
                            FilterChip(
                                selected = selectedCourseId == course.id,
                                onClick = { selectedCourseId = course.id },
                                label = { Text(course.code, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color.copy(alpha = 0.2f), selectedLabelColor = color)
                            )
                        }
                    }
                }
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (e.g. Midterm Exam)") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = score, onValueChange = { score = it }, label = { Text("Score") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = max, onValueChange = { max = it }, label = { Text("Max") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Text("Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        FilterChip(selected = selectedType == t, onClick = { selectedType = t }, label = { Text(t.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val s = score.toFloatOrNull() ?: return@Button
                val m = max.toFloatOrNull() ?: return@Button
                val w = weight.toFloatOrNull() ?: 1f
                val cId = selectedCourseId ?: return@Button
                if (label.isNotEmpty()) onConfirm(cId, label, s, m, selectedType, w)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─── Course Detail Screen ─────────────────────────────────────────────────────

@Composable
fun CourseDetailScreen(
    course: Course,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allNotes        by viewModel.notes.collectAsState()
    val allGrades       by viewModel.allGrades.collectAsState()
    val allAssignments  by viewModel.assignments.collectAsState()
    val courses         by viewModel.courses.collectAsState()

    val courseNotes       = remember(allNotes, course.id)       { allNotes.filter { it.courseId == course.id } }
    val courseGrades      = remember(allGrades, course.id)      { allGrades.filter { it.courseId == course.id } }
    val courseAssignments = remember(allAssignments, course.id) { allAssignments.filter { it.courseId == course.id } }

    val courseColor = Color(android.graphics.Color.parseColor(course.colorHex))
    var selectedTab         by remember { mutableStateOf(0) }
    var showAddGradeDialog  by remember { mutableStateOf(false) }
    var showAddAssignment   by remember { mutableStateOf(false) }

    val gpa = remember(courseGrades) {
        if (courseGrades.isEmpty()) null
        else {
            val weightedSum = courseGrades.sumOf { (it.score / it.maxScore * it.weight).toDouble() }
            val totalWeight  = courseGrades.sumOf { it.weight.toDouble() }
            if (totalWeight > 0) (weightedSum / totalWeight * 100).toFloat() else null
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Gradient header ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(courseColor, courseColor.copy(alpha = 0.65f)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.MAX_VALUE, Float.MAX_VALUE)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.2f)) {
                        Text(course.code, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(course.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(13.dp))
                        Text(course.instructor, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(13.dp))
                        Text(course.schedule, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CourseStatBubble("${courseNotes.size}", "Notes", Icons.Default.Description)
                    CourseStatBubble("${courseGrades.size}", "Grades", Icons.Default.Grade)
                    CourseStatBubble("${courseAssignments.count { !it.isCompleted }}", "Tasks", Icons.Default.Assignment)
                    if (gpa != null) CourseStatBubble("${gpa.toInt()}%", "GPA", Icons.Default.Star)
                }
            }
        }

        // ── Tab bar ───────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = courseColor,
            divider = {}
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("Notes", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp)) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("Grades", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.Grade, null, modifier = Modifier.size(16.dp)) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                text = { Text("Tasks", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.Assignment, null, modifier = Modifier.size(16.dp)) })
        }

        // ── Tab content + FAB ─────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> {
                    if (courseNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyStateMinimal(icon = Icons.Default.BorderColor, text = "No notes for this course yet")
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(courseNotes) { note ->
                                NoteRowItem(note = note, courses = courses, onClick = { onOpenNote(note.id) })
                            }
                        }
                    }
                }
                1 -> GradeTrackerContent(
                    courses = listOf(course),
                    allGrades = courseGrades,
                    isTablet = false,
                    onDeleteGrade = { viewModel.deleteGrade(it) }
                )
                2 -> {
                    if (courseAssignments.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyStateMinimal(icon = Icons.Default.CheckCircle, text = "No tasks for this course")
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(courseAssignments) { assignment ->
                                val urgency = remember(assignment.dueDate) { dueDateUrgency(assignment.dueDate) }
                                AssignmentListItem(
                                    assignment = assignment, course = course, urgency = urgency,
                                    onToggle = { viewModel.toggleAssignmentCompleted(assignment) },
                                    onDelete = { viewModel.deleteAssignment(assignment.id) }
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab == 1 || selectedTab == 2) {
                FloatingActionButton(
                    onClick = { if (selectedTab == 1) showAddGradeDialog = true else showAddAssignment = true },
                    containerColor = courseColor,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    }

    if (showAddGradeDialog) {
        AddGradeDialog(courses = listOf(course), onDismiss = { showAddGradeDialog = false }, onConfirm = { cId, label, score, max, type, weight ->
            viewModel.addGrade(cId, label, score, max, type, weight)
            showAddGradeDialog = false
        })
    }
    if (showAddAssignment) {
        AddAssignmentDialog(courses = listOf(course), onDismiss = { showAddAssignment = false }, onConfirm = { title, due, cId, notes, priority ->
            viewModel.addAssignment(title, due, cId, notes, priority)
            showAddAssignment = false
        })
    }
}

@Composable
private fun CourseStatBubble(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.2f)) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Text(value, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun CourseCard(course: Course, noteCount: Int, onDelete: () -> Unit, onClick: () -> Unit = {}, compact: Boolean = false) {
    val color = Color(android.graphics.Color.parseColor(course.colorHex))
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
        if (assignment.priority == "high") {
            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)) {
                Text("HIGH", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        } else if (assignment.priority == "low") {
            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)) {
                Text("LOW", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
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
fun AddAssignmentDialog(courses: List<Course>, onDismiss: () -> Unit, onConfirm: (String, String, Long?, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var selectedPriority by remember { mutableStateOf("medium") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Assignment") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, placeholder = { Text("YYYY-MM-DD") }, label = { Text("Due Date") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("high" to "🔴 High", "medium" to "🟡 Medium", "low" to "🟢 Low").forEach { (value, label) ->
                        FilterChip(
                            selected = selectedPriority == value,
                            onClick = { selectedPriority = value },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
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
        confirmButton = { Button(onClick = { if (title.isNotEmpty()) onConfirm(title, dueDate, selectedCourseId, notes, selectedPriority) }) { Text("Add") } },
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

    val baseCards  = if (activeNoteId != null) quizCards else allFlashcards
    var shuffled   by remember { mutableStateOf(false) }
    val displayCards = remember(baseCards, shuffled) { if (shuffled) baseCards.shuffled() else baseCards }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { shuffled = !shuffled; cardIndex = 0; flipStatus = false },
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                } // Row wrapping shuffle + AI button
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
    val email          by viewModel.googleAccountEmail.collectAsState()
    val syncState      by viewModel.syncState.collectAsState()
    val notes          by viewModel.notes.collectAsState()
    val themeMode      by viewModel.themeMode.collectAsState()
    val workMins       by viewModel.pomodoroWorkMins.collectAsState()
    val breakMins      by viewModel.pomodoroBreakMins.collectAsState()
    var tokenInput by remember { mutableStateOf("") }
    val syncedCount = notes.count { it.isSynced }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // ── Appearance ──────────────────────────────────────────────────────────
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                        FilterChip(
                            selected = themeMode == value,
                            onClick = { viewModel.setThemeMode(value) },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(
                                    when (value) {
                                        "light" -> Icons.Default.LightMode
                                        "dark"  -> Icons.Default.DarkMode
                                        else    -> Icons.Default.BrightnessAuto
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // ── Study Timer ─────────────────────────────────────────────────────────
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Study Timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("Work session duration", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 20, 25, 30, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = workMins == mins,
                            onClick = { viewModel.setPomodoroWork(mins) },
                            label = { Text("${mins}m") }
                        )
                    }
                }
                Text("Break duration", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15).forEach { mins ->
                        FilterChip(
                            selected = breakMins == mins,
                            onClick = { viewModel.setPomodoroBreak(mins) },
                            label = { Text("${mins}m") }
                        )
                    }
                }
            }
        }

        // ── Cloud Sync ──────────────────────────────────────────────────────────
        Text("Cloud Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

        // ── About ───────────────────────────────────────────────────────────────
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text("ScholarSpace", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("v3.0 · Student Productivity Suite", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Powered by Gemini AI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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

// ─── Pomodoro Timer ───────────────────────────────────────────────────────────

@Composable
fun PomodoroTimerCard(workMins: Int = 25, breakMins: Int = 5, modifier: Modifier = Modifier) {
    var ticking   by remember { mutableStateOf(false) }
    var isBreak   by remember { mutableStateOf(false) }
    var secLeft   by remember(workMins) { mutableStateOf(workMins * 60) }
    var sessions  by remember { mutableStateOf(0) }

    val totalSecs = if (isBreak) breakMins * 60 else workMins * 60
    val progress  = secLeft.toFloat() / totalSecs
    val mm = secLeft / 60
    val ss = secLeft % 60
    val timeLabel = "%02d:%02d".format(mm, ss)
    val phaseLabel = if (isBreak) "Break" else "Focus"
    val phaseColor = if (isBreak) Color(0xFF2E7D5A) else MaterialTheme.colorScheme.primary

    // Tick every second when running
    LaunchedEffect(ticking) {
        while (ticking && secLeft > 0) {
            delay(1000L)
            secLeft--
        }
        if (ticking && secLeft == 0) {
            if (!isBreak) sessions++
            isBreak = !isBreak
            secLeft = if (isBreak) breakMins * 60 else workMins * 60
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular arc progress
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 6.dp.toPx()
                    val inset = strokeW / 2f
                    drawArc(
                        color = phaseColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - strokeW, size.height - strokeW),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = phaseColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - strokeW, size.height - strokeW),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(timeLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(phaseLabel, style = MaterialTheme.typography.labelSmall, color = phaseColor)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Study Timer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (sessions > 0) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("$sessions", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { ticking = !ticking },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(if (ticking) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (ticking) "Pause" else if (secLeft < totalSecs) "Resume" else "Start", style = MaterialTheme.typography.labelMedium)
                    }
                    if (secLeft < totalSecs || isBreak) {
                        OutlinedButton(
                            onClick = { ticking = false; isBreak = false; secLeft = workMins * 60 },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
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
fun WeeklyScheduleSection(courses: List<Course>, onAddClassClick: () -> Unit, isTablet: Boolean, modifier: Modifier = Modifier) {
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
        modifier = modifier.fillMaxWidth().testTag("weekly_schedule_card"),
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
