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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Welcome and Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome Back, Scholar!",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Ready to conquer your classes today?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onCreateNote,
                modifier = Modifier.testTag("dashboard_new_note_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rapid S-Pen Note")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Metrics Summary Cards row
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

        Spacer(modifier = Modifier.height(24.dp))

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
                                            Text("Due: ${assignment.dueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
@Composable
fun CoursesScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val assignments by viewModel.assignments.collectAsState()

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Academy Directory & Deadlines", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Row {
                Button(onClick = { showAddAssignmentDialog = true }, modifier = Modifier.testTag("add_assignment_trigger")) {
                    Icon(Icons.Default.AddAlert, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Assignment")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = { showAddCourseDialog = true }, modifier = Modifier.testTag("add_course_trigger")) {
                    Icon(Icons.Default.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enroll Class")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                            Text("No courses registered. Click Enroll Class to begin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        IconButton(onClick = { viewModel.deleteCourse(course.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(course.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Lec: ${course.instructor}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(course.schedule, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                Text("Due: ${assignment.dueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    }

    // dialogs
    if (showAddCourseDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var instructor by remember { mutableStateOf("") }
        var schedule by remember { mutableStateOf("") }
        val colors = listOf("#1E88E5", "#D81B60", "#43A047", "#8E24AA", "#FFB300", "#00ACC1")
        var selectedColor by remember { mutableStateOf(colors.first()) }

        AlertDialog(
            onDismissRequest = { showAddCourseDialog = false },
            title = { Text("Enroll in Class") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Course Name") })
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code (e.g. CS101)") })
                    OutlinedTextField(value = instructor, onValueChange = { instructor = it }, label = { Text("Instructor Name") })
                    OutlinedTextField(value = schedule, onValueChange = { schedule = it }, label = { Text("Schedule Description") })

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
                            viewModel.addCourse(name, code, instructor, selectedColor, schedule)
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
        var selectedCourseId by remember { mutableStateOf<Long?>(null) }

        AlertDialog(
            onDismissRequest = { showAddAssignmentDialog = false },
            title = { Text("Add Assignment Deadline") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Assignment Title") })
                    OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, placeholder = { Text("YYYY-MM-DD") }, label = { Text("Due Date") })

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
                            viewModel.addAssignment(title, dueDate, selectedCourseId)
                            showAddAssignmentDialog = false
                        }
                    }
                ) { Text("Add Deadline") }
            },
            dismissButton = { TextButton(onClick = { showAddAssignmentDialog = false }) { Text("Cancel") } }
        )
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
    val activeNoteId by viewModel.activeNoteId.collectAsState()
    val generationActive by viewModel.flashcardGenerationActive.collectAsState()

    var cardIndex by remember { mutableStateOf(0) }
    var flipStatus by remember { mutableStateOf(false) } // False: question, True: answer

    // Dynamic rotation for card flipping animation
    val cardRotation by animateFloatAsState(
        targetValue = if (flipStatus) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "CardFlipAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp)
                ) {
                    Text("Select a Note from Workspace to activate study deck options.")
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        if (quizCards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CardMembership, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Decks currently empty for this workspace.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Create flashcards manually contextually below or generate with Gemini above.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val safeIndex = cardIndex.coerceIn(0, quizCards.size - 1)
            val activeCard = quizCards[safeIndex]

            Text("CARD ${safeIndex + 1} OF ${quizCards.size}", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Study deck flipping card container using overlapping perspective
            Box(
                modifier = Modifier
                    .width(480.dp)
                    .height(280.dp)
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
                        .padding(24.dp)
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
                                style = MaterialTheme.typography.titleLarge,
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
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Control Actions
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        flipStatus = false
                        cardIndex = (cardIndex - 1 + quizCards.size) % quizCards.size
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Previous")
                }

                Button(
                    onClick = {
                        flipStatus = false
                        cardIndex = (cardIndex + 1) % quizCards.size
                    }
                ) {
                    Text("Next Card")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }

                IconButton(
                    onClick = { viewModel.deleteFlashcard(activeCard.id) }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete card", tint = Color.Red.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Manual Deck Builder panel (Bottom supporting panel)
        val noteId = activeNoteId
        if (noteId != null) {
            Card(
                modifier = Modifier.width(480.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                var manualQuestion by remember { mutableStateOf("") }
                var manualAnswer by remember { mutableStateOf("") }

                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create manual card in active desk", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
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
            .padding(24.dp)
    ) {
        Text("Google Ecosystem Cloud Hub", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Text("Connect, telemetry audit, and sync notebook structures to Google Drive securely.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(32.dp))

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

                        VerticalDivider(Modifier.height(1.dp))

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

                            Button(
                                onClick = {
                                    // Simulated high fidelity sync for instant user playing
                                    viewModel.connectGoogleAccount("dev.student.s10@gmail.com", "MOCK_DEVELOPMENT_DRIVE_OAUTH_TOKEN_ACTIVE")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Quick Connect Demo Account")
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
    }
}
