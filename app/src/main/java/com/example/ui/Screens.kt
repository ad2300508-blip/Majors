package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.*
import kotlinx.coroutines.launch

// -----------------------------------------------------
// 1. DASHBOARD SCREEN
// -----------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onCreateNote: () -> Unit,
    onNavigateToPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val assignments by viewModel.assignments.collectAsState()

    val pendingAssignments = assignments.filter { !it.isCompleted }
    var showAddCourseDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth > 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTablet) 24.dp else 16.dp)
        ) {
            // Welcome and Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome Back, Scholar!",
                        style = if (isTablet) MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ) else MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    val todayFormatted = remember {
                        java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date())
                    }
                    Text(
                        text = todayFormatted,
                        style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onCreateNote,
                    modifier = Modifier.testTag("dashboard_new_note_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = if (isTablet) ButtonDefaults.ContentPadding else PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Note")
                    if (isTablet) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rapid S-Pen Note")
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

            // Metrics Summary Cards row
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "Enrolled Classes",
                        value = courses.size.toString(),
                        icon = Icons.Default.School,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { onNavigateToPage("courses") },
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Notes Taken",
                        value = notes.size.toString(),
                        icon = Icons.Default.Description,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = { onNavigateToPage("notes") },
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Pending Assignments",
                        value = pendingAssignments.size.toString(),
                        icon = Icons.Default.Assignment,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = { onNavigateToPage("courses") },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniMetricCard(
                        title = "Classes",
                        value = courses.size.toString(),
                        icon = Icons.Default.School,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { onNavigateToPage("courses") },
                        modifier = Modifier.weight(1f)
                    )
                    MiniMetricCard(
                        title = "Notes",
                        value = notes.size.toString(),
                        icon = Icons.Default.Description,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = { onNavigateToPage("notes") },
                        modifier = Modifier.weight(1f)
                    )
                    MiniMetricCard(
                        title = "Pending",
                        value = pendingAssignments.size.toString(),
                        icon = Icons.Default.Assignment,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = { onNavigateToPage("courses") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

            if (isTablet) {
                WeeklyScheduleSection(
                    courses = courses,
                    onAddClassClick = { showAddCourseDialog = true },
                    isTablet = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Large screen dual layout (Recent Notes + Assignments)
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left: Recent Notes
                    Card(
                        modifier = Modifier.weight(1.2f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent Class Workspace", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { onNavigateToPage("notes") }) { Text("View All") }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            if (notes.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.BorderColor, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No notes taken yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(notes.take(4)) { note ->
                                        NoteRowItem(note = note, courses = courses) {
                                            viewModel.setActiveNote(note.id)
                                            onNavigateToPage("notes")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right: Pending Assignments
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Deadlines & Assignments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            if (pendingAssignments.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("All caught up! Zero due assignments.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(pendingAssignments.take(5)) { assignment ->
                                        val course = courses.find { it.id == assignment.courseId }
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Event,
                                                    contentDescription = null,
                                                    tint = course?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(assignment.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    val dueDateColor = remember(assignment.dueDate) {
                                                        try {
                                                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                            val due = sdf.parse(assignment.dueDate)
                                                            val now = java.util.Date()
                                                            val diffMs = due!!.time - now.time
                                                            val diffDays = diffMs / (1000 * 60 * 60 * 24)
                                                            when {
                                                                diffDays < 0 -> null // overdue = red
                                                                diffDays <= 2 -> 1 // soon = orange
                                                                else -> 2 // normal
                                                            }
                                                        } catch (e: Exception) { 2 }
                                                    }
                                                    Text(
                                                        text = "Due: ${assignment.dueDate}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = when (dueDateColor) {
                                                            null -> MaterialTheme.colorScheme.error
                                                            1 -> Color(0xFFE65100)
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Mobile stacked layout with vertical scrolling
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WeeklyScheduleSection(
                        courses = courses,
                        onAddClassClick = { showAddCourseDialog = true },
                        isTablet = false
                    )

                    // Recent Notes card block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { onNavigateToPage("notes") }) { Text("All", fontSize = 12.sp) }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            if (notes.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("No notes taken yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    notes.take(3).forEach { note ->
                                        NoteRowItem(note = note, courses = courses) {
                                            viewModel.setActiveNote(note.id)
                                            onNavigateToPage("notes")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pending Assignments Card Block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Upcoming Deadlines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            if (pendingAssignments.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("All caught up! Zero deadlines.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    pendingAssignments.take(3).forEach { assignment ->
                                        val course = courses.find { it.id == assignment.courseId }
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Event,
                                                    contentDescription = null,
                                                    tint = course?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(assignment.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    val dueDateColor = remember(assignment.dueDate) {
                                                        try {
                                                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                            val due = sdf.parse(assignment.dueDate)
                                                            val now = java.util.Date()
                                                            val diffMs = due!!.time - now.time
                                                            val diffDays = diffMs / (1000 * 60 * 60 * 24)
                                                            when {
                                                                diffDays < 0 -> null // overdue = red
                                                                diffDays <= 2 -> 1 // soon = orange
                                                                else -> 2 // normal
                                                            }
                                                        } catch (e: Exception) { 2 }
                                                    }
                                                    Text(
                                                        text = "Due: ${assignment.dueDate}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = when (dueDateColor) {
                                                            null -> MaterialTheme.colorScheme.error
                                                            1 -> Color(0xFFE65100)
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
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

        if (showAddCourseDialog) {
            var name by remember { mutableStateOf("") }
            var code by remember { mutableStateOf("") }
            var instructor by remember { mutableStateOf("") }
            var timeInput by remember { mutableStateOf("") }
            var selectedDays by remember { mutableStateOf(setOf<String>()) }
            val colors = listOf("#1E88E5", "#D81B60", "#43A047", "#8E24AA", "#FFB300", "#00ACC1")
            var selectedColor by remember { mutableStateOf(colors.first()) }

            AlertDialog(
                onDismissRequest = { showAddCourseDialog = false },
                title = { Text("Enroll in Class") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Course Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Course Code (e.g. CS101)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = instructor,
                            onValueChange = { instructor = it },
                            label = { Text("Instructor Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Choose Class Days:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val daysOfWeekList = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                            daysOfWeekList.forEach { dayAbbr ->
                                val isSelected = selectedDays.contains(dayAbbr)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDays = if (isSelected) {
                                            selectedDays - dayAbbr
                                        } else {
                                            selectedDays + dayAbbr
                                        }
                                    },
                                    label = { Text(dayAbbr, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = timeInput,
                            onValueChange = { timeInput = it },
                            placeholder = { Text("e.g. 10:00 AM - 11:30 AM") },
                            label = { Text("Lecture Time") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Pick Course Color")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(android.graphics.Color.parseColor(col)), CircleShape)
                                        .border(
                                            2.dp,
                                            if (selectedColor == col) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { selectedColor = col }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && code.isNotEmpty()) {
                                val finalSchedule = if (selectedDays.isNotEmpty()) {
                                    "${selectedDays.joinToString(", ")} ${timeInput.trim()}"
                                } else {
                                    timeInput.trim()
                                }
                                viewModel.addCourse(name, code, instructor, selectedColor, finalSchedule)
                                showAddCourseDialog = false
                            }
                        }
                    ) { Text("Enroll") }
                },
                dismissButton = { TextButton(onClick = { showAddCourseDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun MiniMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(76.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun NoteRowItem(
    note: Note,
    courses: List<Course>,
    onClick: () -> Unit
) {
    val course = courses.find { it.id == note.courseId }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (note.textContent.isNotEmpty()) note.textContent else "S-Pen Canvas ink shapes saved",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (course != null) {
                    val color = Color(android.graphics.Color.parseColor(course.colorHex))
                    Box(
                        modifier = Modifier
                            .background(color, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(course.code, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (note.isSynced) {
                    Icon(Icons.Default.CloudDone, contentDescription = "Synced to Drive", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.CloudQueue, contentDescription = "Local note", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// -----------------------------------------------------
// 2. COURSES SCREEN (CLASSES & ASSIGNMENTS)
// -----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val notes by viewModel.notes.collectAsState()

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var courseToDelete by remember { mutableStateOf<com.example.data.Course?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth > 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTablet) 24.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Academy Directory",
                        style = if (isTablet) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage registered classes and assignments.",
                        style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showAddAssignmentDialog = true },
                        modifier = Modifier
                            .testTag("add_assignment_trigger")
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = "Add Assignment", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    IconButton(
                        onClick = { showAddCourseDialog = true },
                        modifier = Modifier
                            .testTag("add_course_trigger")
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Enroll Class", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Course Grid
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text("Enrolled Courses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (courses.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No courses registered. Get started today!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { showAddCourseDialog = true }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enroll in a Class")
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(courses) { course ->
                                    val color = Color(android.graphics.Color.parseColor(course.colorHex))
                                    val noteCount = notes.count { it.courseId == course.id }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
                                        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Box(
                                                    modifier = Modifier.background(color, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(course.code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                IconButton(
                                                    onClick = { courseToDelete = course },
                                                    modifier = Modifier.size(36.dp).minimumInteractiveComponentSize()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color.Red.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(course.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Lec: ${course.instructor}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(course.schedule, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp), tint = color.copy(alpha = 0.7f))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("$noteCount note${if (noteCount != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Assignments List
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Assignments Audit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))

                            if (assignments.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No assignments active.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(assignments) { assignment ->
                                        val course = courses.find { it.id == assignment.courseId }
                                        val color = course?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = assignment.isCompleted,
                                                        onCheckedChange = { viewModel.toggleAssignmentCompleted(assignment) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = assignment.title,
                                                            fontWeight = FontWeight.SemiBold,
                                                            style = if (assignment.isCompleted) MaterialTheme.typography.bodyMedium.copy(color = Color.Gray) else MaterialTheme.typography.bodyMedium
                                                        )
                                                        val dueDateColor = remember(assignment.dueDate) {
                                                            try {
                                                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                                val due = sdf.parse(assignment.dueDate)
                                                                val now = java.util.Date()
                                                                val diffMs = due!!.time - now.time
                                                                val diffDays = diffMs / (1000 * 60 * 60 * 24)
                                                                when {
                                                                    diffDays < 0 -> null
                                                                    diffDays <= 2 -> 1
                                                                    else -> 2
                                                                }
                                                            } catch (e: Exception) { 2 }
                                                        }
                                                        Text(
                                                            text = "Due: ${assignment.dueDate}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = when (dueDateColor) {
                                                                null -> MaterialTheme.colorScheme.error
                                                                1 -> Color(0xFFE65100)
                                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                            }
                                                        )
                                                        if (assignment.notes.isNotEmpty()) {
                                                            Text(assignment.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                }
                                                IconButton(onClick = { viewModel.deleteAssignment(assignment.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.5f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Mobile stacked layout with vertical scrolling
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Enrolled courses section
                    Text("Enrolled Courses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (courses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No courses registered.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showAddCourseDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enroll in a Class")
                                }
                            }
                        }
                    } else {
                        courses.forEach { course ->
                            val color = Color(android.graphics.Color.parseColor(course.colorHex))
                            val noteCount = notes.count { it.courseId == course.id }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier.background(color, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(course.code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                            Text("$noteCount note${if (noteCount != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                                        }
                                        IconButton(
                                            onClick = { courseToDelete = course },
                                            modifier = Modifier.size(36.dp).minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete class",
                                                tint = Color.Red.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("Lec: ${course.instructor} • ${course.schedule}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Assignments section
                    Text("Assignments Audit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (assignments.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("No assignments active.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        assignments.forEach { assignment ->
                            val course = courses.find { it.id == assignment.courseId }
                            val color = course?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = assignment.isCompleted,
                                            onCheckedChange = { viewModel.toggleAssignmentCompleted(assignment) }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = assignment.title,
                                                fontWeight = FontWeight.SemiBold,
                                                style = if (assignment.isCompleted) MaterialTheme.typography.bodyMedium.copy(color = Color.Gray) else MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val dueDateColor = remember(assignment.dueDate) {
                                                try {
                                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                    val due = sdf.parse(assignment.dueDate)
                                                    val now = java.util.Date()
                                                    val diffMs = due!!.time - now.time
                                                    val diffDays = diffMs / (1000 * 60 * 60 * 24)
                                                    when {
                                                        diffDays < 0 -> null // overdue = red
                                                        diffDays <= 2 -> 1 // soon = orange
                                                        else -> 2 // normal
                                                    }
                                                } catch (e: Exception) { 2 }
                                            }
                                            Text(
                                                text = "Due: ${assignment.dueDate}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = when (dueDateColor) {
                                                    null -> MaterialTheme.colorScheme.error
                                                    1 -> Color(0xFFE65100)
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteAssignment(assignment.id) },
                                        modifier = Modifier.size(36.dp).minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // dialogs
        if (showAddCourseDialog) {
            var name by remember { mutableStateOf("") }
            var code by remember { mutableStateOf("") }
            var instructor by remember { mutableStateOf("") }
            var timeInput by remember { mutableStateOf("") }
            var selectedDays by remember { mutableStateOf(setOf<String>()) }
            val colors = listOf("#1E88E5", "#D81B60", "#43A047", "#8E24AA", "#FFB300", "#00ACC1")
            var selectedColor by remember { mutableStateOf(colors.first()) }

            AlertDialog(
                onDismissRequest = { showAddCourseDialog = false },
                title = { Text("Enroll in Class") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Course Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Course Code (e.g. CS101)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = instructor,
                            onValueChange = { instructor = it },
                            label = { Text("Instructor Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Choose Class Days:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val daysOfWeekList = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                            daysOfWeekList.forEach { dayAbbr ->
                                val isSelected = selectedDays.contains(dayAbbr)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDays = if (isSelected) {
                                            selectedDays - dayAbbr
                                        } else {
                                            selectedDays + dayAbbr
                                        }
                                    },
                                    label = { Text(dayAbbr, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = timeInput,
                            onValueChange = { timeInput = it },
                            placeholder = { Text("e.g. 10:00 AM - 11:30 AM") },
                            label = { Text("Lecture Time") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Pick Course Color")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(android.graphics.Color.parseColor(col)), CircleShape)
                                        .border(
                                            2.dp,
                                            if (selectedColor == col) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { selectedColor = col }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && code.isNotEmpty()) {
                                val finalSchedule = if (selectedDays.isNotEmpty()) {
                                    "${selectedDays.joinToString(", ")} ${timeInput.trim()}"
                                } else {
                                    timeInput.trim()
                                }
                                viewModel.addCourse(name, code, instructor, selectedColor, finalSchedule)
                                showAddCourseDialog = false
                            }
                        }
                    ) { Text("Enroll") }
                },
                dismissButton = { TextButton(onClick = { showAddCourseDialog = false }) { Text("Cancel") } }
            )
        }

    if (showAddAssignmentDialog) {
        var title by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("") }
        var assignmentNotes by remember { mutableStateOf("") }
        var selectedCourseId by remember { mutableStateOf<Long?>(null) }

        AlertDialog(
            onDismissRequest = { showAddAssignmentDialog = false },
            title = { Text("Add Assignment Deadline") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Assignment Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, placeholder = { Text("YYYY-MM-DD") }, label = { Text("Due Date") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = assignmentNotes, onValueChange = { assignmentNotes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)

                    Text("Associate class (Optional)")
                    courses.forEach { course ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedCourseId = course.id }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedCourseId == course.id, onClick = { selectedCourseId = course.id })
                            Spacer(Modifier.width(8.dp))
                            Text(course.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotEmpty()) {
                            viewModel.addAssignment(title, dueDate, selectedCourseId, assignmentNotes)
                            showAddAssignmentDialog = false
                        }
                    }
                ) { Text("Add Deadline") }
            },
            dismissButton = { TextButton(onClick = { showAddAssignmentDialog = false }) { Text("Cancel") } }
        )
    }

    courseToDelete?.let { course ->
        val noteCount = notes.count { it.courseId == course.id }
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text("Remove Course?") },
            text = {
                Column {
                    Text("Are you sure you want to remove \"${course.name}\"?")
                    if (noteCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "$noteCount note${if (noteCount != 1) "s" else ""} linked to this course will become unlinked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCourse(course.id)
                        courseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { courseToDelete = null }) { Text("Cancel") } }
        )
    }
}
}

// -----------------------------------------------------
// 3. FLASHCARDS STUDY SCREEN
// -----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardStudyScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val quizCards by viewModel.quizFlashcards.collectAsState()
    val allFlashcards by viewModel.allFlashcards.collectAsState()
    val activeNoteId by viewModel.activeNoteId.collectAsState()
    val generationActive by viewModel.flashcardGenerationActive.collectAsState()

    val displayCards = if (activeNoteId != null) quizCards else allFlashcards

    var cardIndex by remember { mutableStateOf(0) }
    var flipStatus by remember { mutableStateOf(false) }
    var studyScore by remember { mutableStateOf(0 to 0) } // correct to total

    LaunchedEffect(displayCards.size) {
        if (displayCards.isNotEmpty() && cardIndex >= displayCards.size) {
            cardIndex = displayCards.size - 1
            flipStatus = false
        }
    }

    // Dynamic rotation for card flipping animation
    val cardRotation by animateFloatAsState(
        targetValue = if (flipStatus) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "CardFlipAnimation"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth > 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTablet) 24.dp else 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Adaptive Header Row/Column
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Study Flashcards", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                        Text("Flip and review generated study decks offline.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (activeNoteId != null) {
                        Button(
                            onClick = { viewModel.generateAIStudyFlashcards() },
                            enabled = !generationActive,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.testTag("ai_generate_deck_button")
                        ) {
                            if (generationActive) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Draft deck with Gemini")
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Showing all ${allFlashcards.size} flashcards. Open a note to generate more.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("AI Study Flashcards", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Flip and review generated study decks offline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (activeNoteId != null) {
                        Button(
                            onClick = { viewModel.generateAIStudyFlashcards() },
                            enabled = !generationActive,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth().testTag("ai_generate_deck_button")
                        ) {
                            if (generationActive) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Draft deck with Gemini")
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Showing all ${allFlashcards.size} flashcards across all notes.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isTablet) 36.dp else 24.dp))

            if (displayCards.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CardMembership, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Decks currently empty for this workspace.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Text("Create flashcards manually contextually below or generate with Gemini above.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                val safeIndex = cardIndex.coerceIn(0, displayCards.size - 1)
                val activeCard = displayCards[safeIndex]

                Text("CARD ${safeIndex + 1} OF ${displayCards.size}", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // Study deck flipping card container using overlapping perspective
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .height(if (isTablet) 280.dp else 220.dp)
                        .graphicsLayer {
                            rotationY = cardRotation
                            cameraDistance = 8f * density
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { flipStatus = !flipStatus }
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .background(
                            if (cardRotation > 90f) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface
                        )
                ) {
                    // Adjusting gravity for text depending on perspective rotation
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isTablet) 24.dp else 16.dp)
                            .graphicsLayer {
                                // Counter-rotate text on backward perspective
                                if (cardRotation > 90f) {
                                    rotationY = 180f
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cardRotation > 90f) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text("ANSWER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    activeCard.answer,
                                    style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text("QUESTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    activeCard.question,
                                    style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isTablet) 36.dp else 20.dp))

                // Control Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            flipStatus = false
                            cardIndex = (cardIndex - 1 + displayCards.size) % displayCards.size
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            flipStatus = false
                            cardIndex = (cardIndex + 1) % displayCards.size
                        }
                    ) {
                        Text("Next", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { viewModel.deleteFlashcard(activeCard.id) },
                        modifier = Modifier.size(40.dp).minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete card", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                    }
                }

                if (flipStatus) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.markFlashcardReviewed(activeCard.id, correct = false)
                                studyScore = studyScore.copy(second = studyScore.second + 1)
                                flipStatus = false
                                cardIndex = (cardIndex + 1) % displayCards.size
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Missed it", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.markFlashcardReviewed(activeCard.id, correct = true)
                                studyScore = studyScore.copy(first = studyScore.first + 1, second = studyScore.second + 1)
                                flipStatus = false
                                cardIndex = (cardIndex + 1) % displayCards.size
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Knew it!", fontSize = 12.sp)
                        }
                    }
                }

                if (studyScore.second > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Session: ${studyScore.first}/${studyScore.second} correct",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (studyScore.first.toFloat() / studyScore.second >= 0.7f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isTablet) 36.dp else 24.dp))

            // Manual Deck Builder panel (Bottom supporting panel)
            val noteId = activeNoteId
            if (noteId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    var manualQuestion by remember { mutableStateOf("") }
                    var manualAnswer by remember { mutableStateOf("") }

                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Create manual card in active deck", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = manualQuestion,
                            onValueChange = { manualQuestion = it },
                            label = { Text("Question") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = manualAnswer,
                            onValueChange = { manualAnswer = it },
                            label = { Text("Answer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            modifier = Modifier.align(Alignment.End),
                            onClick = {
                                if (manualQuestion.isNotEmpty() && manualAnswer.isNotEmpty()) {
                                    viewModel.addFlashcardManual(noteId, manualQuestion, manualAnswer)
                                    manualQuestion = ""
                                    manualAnswer = ""
                                }
                            }
                        ) {
                            Text("Save Card")
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// 4. GOOGLE ECOSYSTEM BACKUP & TELEMETRY SYNC HUB
// -----------------------------------------------------
@Composable
fun SyncHubScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val email by viewModel.googleAccountEmail.collectAsState()
    val token by viewModel.googleAccessToken.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val notes by viewModel.notes.collectAsState()

    var customAccessTokenInput by remember { mutableStateOf("") }
    var mockEnabled by remember { mutableStateOf(false) }

    val syncedCount = notes.count { it.isSynced }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Google Ecosystem Cloud Hub", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Text("Connect, telemetry audit, and sync notebook structures to Google Drive securely.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(32.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isSyncWide = maxWidth > 560.dp
        if (isSyncWide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Status Card
            Card(
                modifier = Modifier.weight(1.2f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("OAuth 2.0 Identity Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    if (email.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("CONNECTED AS", style = MaterialTheme.typography.labelSmall)
                                Text(email, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Button(
                            onClick = { viewModel.disconnectGoogleAccount() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Disconnect Account")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ACCOUNT STATUS", style = MaterialTheme.typography.labelSmall)
                                Text("Not Connected", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Set up real Access Token manual link below to trigger file sync operations instantly:")
                        OutlinedTextField(
                            value = customAccessTokenInput,
                            onValueChange = { customAccessTokenInput = it },
                            label = { Text("Manual OAuth Google Token") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    if (customAccessTokenInput.isNotEmpty()) {
                                        viewModel.connectGoogleAccount("workspace.student@gmail.com", customAccessTokenInput)
                                        customAccessTokenInput = ""
                                    }
                                }
                            ) {
                                Text("Link OAuth")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.connectGoogleAccount("demo@scholar-space.app", "MOCK_DEMO_TOKEN")
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Try Demo Mode")
                            }
                        }
                    }
                }
            }

            // Right Telemetry Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("G-Drive Sync Audit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cloud Target Folder:")
                        Text("ScholarWorkspace/", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cloud Synced Notebooks:")
                        Text("$syncedCount / ${notes.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    }

                    HorizontalDivider()

                    Text("Active Telemetry Live Actions:", fontWeight = FontWeight.SemiBold)
                    when (syncState) {
                        is SyncState.Idle -> Text("Status: Cloud Connection is Standby.")
                        is SyncState.Syncing -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Status: Syncing files to Drive container...")
                        }
                        is SyncState.Success -> {
                            Text("Status: Final backup upload success!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            Text("File ID: ${(syncState as SyncState.Success).fileId}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is SyncState.Error -> Text("Error: ${(syncState as SyncState.Error).message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // OAuth Status Card (mobile)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Account Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (email.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("CONNECTED AS", style = MaterialTheme.typography.labelSmall)
                                    Text(email, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Button(onClick = { viewModel.disconnectGoogleAccount() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Text("Disconnect")
                            }
                        } else {
                            OutlinedTextField(value = customAccessTokenInput, onValueChange = { customAccessTokenInput = it }, label = { Text("OAuth Access Token") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { if (customAccessTokenInput.isNotEmpty()) { viewModel.connectGoogleAccount("workspace.student@gmail.com", customAccessTokenInput); customAccessTokenInput = "" } }) { Text("Link") }
                                OutlinedButton(onClick = { viewModel.connectGoogleAccount("demo@scholar-space.app", "MOCK_DEMO_TOKEN") }) { Text("Demo") }
                            }
                        }
                    }
                }

                // Sync Status Card (mobile)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Drive Sync Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Synced:")
                            Text("$syncedCount / ${notes.size}", fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                        when (syncState) {
                            is SyncState.Idle -> Text("Standby", style = MaterialTheme.typography.bodySmall)
                            is SyncState.Syncing -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...", style = MaterialTheme.typography.bodySmall)
                            }
                            is SyncState.Success -> Text("Sync successful!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            is SyncState.Error -> Text("Error: ${(syncState as SyncState.Error).message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        } // end BoxWithConstraints
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleSection(
    courses: List<Course>,
    onAddClassClick: () -> Unit,
    isTablet: Boolean
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    val currentDayOfWeek = remember {
        val calendar = java.util.Calendar.getInstance()
        when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "Monday"
            java.util.Calendar.TUESDAY -> "Tuesday"
            java.util.Calendar.WEDNESDAY -> "Wednesday"
            java.util.Calendar.THURSDAY -> "Thursday"
            java.util.Calendar.FRIDAY -> "Friday"
            else -> "Monday"
        }
    }
    var selectedDay by remember { mutableStateOf(currentDayOfWeek) }
    
    val dayCourses = courses.filter { isCourseDay(it.schedule, selectedDay) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("weekly_schedule_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Weekly Timetable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Your class schedule by day of week", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onAddClassClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Enroll Class", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Weekday picker row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                days.forEach { day ->
                    val isSelected = selectedDay == day
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .clickable { selectedDay = day }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTablet) day else day.take(3),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (dayCourses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Research or offline study. No classes enrolled today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    dayCourses.forEach { course ->
                        val color = Color(android.graphics.Color.parseColor(course.colorHex))
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
                            border = BorderStroke(1.2.dp, color.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(color, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(course.code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Lec: ${course.instructor}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = course.schedule,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = color,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (dayCourses.size == 1 && isTablet) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

fun isCourseDay(scheduleStr: String, day: String): Boolean {
    val s = scheduleStr.lowercase()
    return when(day.lowercase()) {
        "monday" -> s.contains("mon")
        "tuesday" -> s.contains("tue")
        "wednesday" -> s.contains("wed")
        "thursday" -> s.contains("thu")
        "friday" -> s.contains("fri")
        else -> false
    }
}

