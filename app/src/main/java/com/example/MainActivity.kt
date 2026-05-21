package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup database and repository
        val database = AppDatabase.getDatabase(this)
        val repository = DatabaseRepository(database)
        
        // Initializing the central ViewModel
        val viewModel = ViewModelProvider(
            this, 
            AppViewModelFactory(repository)
        )[AppViewModel::class.java]

        // Prepopulate course databases on first launch for better premium visuals
        lifecycleScope.launch {
            val existingCourses = repository.allCourses.first()
            if (existingCourses.isEmpty()) {
                val csId = repository.insertCourse(
                    Course(
                        name = "Analysis of Algorithms",
                        code = "CS201",
                        instructor = "Prof. Alan Turing",
                        colorHex = "#1E88E5",
                        schedule = "Mon, Wed 10:00 - 11:30"
                    )
                )
                val mathId = repository.insertCourse(
                    Course(
                        name = "Advanced Linear Algebra",
                        code = "MATH251",
                        instructor = "Prof. Carl Gauss",
                        colorHex = "#D81B60",
                        schedule = "Tue, Thu 13:00 - 14:30"
                    )
                )
                val physicsId = repository.insertCourse(
                    Course(
                        name = "Electromagnetism Theory",
                        code = "PHYS301",
                        instructor = "Prof. James Maxwell",
                        colorHex = "#43A047",
                        schedule = "Fri 14:00 - 16:30"
                    )
                )

                // Add active deadlines
                repository.insertAssignment(
                    Assignment(
                        courseId = csId,
                        title = "Problem Set 2: Dijkstra's Proof",
                        dueDate = "2026-06-05",
                        notes = "Prove optimal greedy substructure"
                    )
                )
                repository.insertAssignment(
                    Assignment(
                        courseId = mathId,
                        title = "Matrix Orthogonality Homework",
                        dueDate = "2026-05-28",
                        notes = "Gram-Schmidt projections"
                    )
                )

                // Add initial study note
                val noteId = repository.insertNote(
                    Note(
                        courseId = csId,
                        title = "Lecture 1: Graph BFS & Matrix Models",
                        textContent = "A Breadth-First-Search (BFS) traverses a tree or graph level by level.\n\n" +
                                "It uses a FIFO Queue structure to queue adjacent vertices.\n" +
                                "Complexity bounds are O(V + E) where V is vertices, and E is edges.\n\n" +
                                "We can represent state transitions as matrices, linking and syncing mathematically.",
                         // Empty drawings to start, ready for S-Pen canvas strokes
                         drawingsJson = "[]"
                    )
                )

                // Automatically seed flashcards
                repository.insertFlashcard(
                    Flashcard(
                        noteId = noteId,
                        question = "What is the time complexity of Breadth-First-Search?",
                        answer = "O(V + E) where V represents Vertices and E represents Edges."
                    )
                )
                repository.insertFlashcard(
                    Flashcard(
                        noteId = noteId,
                        question = "What primary data structure does BFS use?",
                        answer = "A First-In-First-Out (FIFO) Queue."
                    )
                )
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: AppViewModel) {
    var currentPage by remember { mutableStateOf("dashboard") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isTablet = maxWidth > 840.dp

        if (isTablet) {
            // Elegant permanent layout for Galaxy Tab S10 Ultra 14.6" screen
            Row(modifier = Modifier.fillMaxSize()) {
                // Persistent Sidebar Navigation Columns
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Visual branding
                    Row(
                        modifier = Modifier.padding(bottom = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ScholarSpace",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Navigation Actions
                    NavigationSidebarItem(
                        title = "Dashboard Desk",
                        icon = Icons.Default.Dashboard,
                        selected = currentPage == "dashboard",
                        onClick = { currentPage = "dashboard" },
                        tag = "nav_dashboard"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NavigationSidebarItem(
                        title = "Notes Workspace",
                        icon = Icons.Default.BorderColor,
                        selected = currentPage == "notes",
                        onClick = { currentPage = "notes" },
                        tag = "nav_notes"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NavigationSidebarItem(
                        title = "Classes & Deadlines",
                        icon = Icons.Default.Class,
                        selected = currentPage == "courses",
                        onClick = { currentPage = "courses" },
                        tag = "nav_courses"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NavigationSidebarItem(
                        title = "AI Study Quiz",
                        icon = Icons.Default.AutoAwesome,
                        selected = currentPage == "flashcards",
                        onClick = { currentPage = "flashcards" },
                        tag = "nav_flashcards"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NavigationSidebarItem(
                        title = "Cloud Sync Hub",
                        icon = Icons.Default.CloudSync,
                        selected = currentPage == "synchub",
                        onClick = { currentPage = "synchub" },
                        tag = "nav_sync"
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Account visual connection footer
                    val email by viewModel.googleAccountEmail.collectAsState()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (email.isNotEmpty()) Icons.Default.CloudDone else Icons.Default.OfflineBolt,
                                contentDescription = null,
                                tint = if (email.isNotEmpty()) Color(0xFF4CAF50) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (email.isNotEmpty()) "Connected" else "Local Only",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = email.ifEmpty { "Offline Storage" },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Divider line separating dashboard panes
                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Main screen pages
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (currentPage) {
                        "dashboard" -> DashboardScreen(
                            viewModel = viewModel,
                            onCreateNote = {
                                viewModel.createNewNote()
                                currentPage = "notes"
                            },
                            onNavigateToPage = { currentPage = it }
                        )
                        "notes" -> WorkspaceScreen(viewModel = viewModel)
                        "courses" -> CoursesScreen(viewModel = viewModel)
                        "flashcards" -> FlashcardStudyScreen(viewModel = viewModel)
                        "synchub" -> SyncHubScreen(viewModel = viewModel)
                    }
                }
            }
        } else {
            // Mobile (Compact) viewport structure with lower navigation bar
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentPage == "dashboard",
                            onClick = { currentPage = "dashboard" },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Desk") }
                        )
                        NavigationBarItem(
                            selected = currentPage == "notes",
                            onClick = { currentPage = "notes" },
                            icon = { Icon(Icons.Default.BorderColor, contentDescription = "Notes") },
                            label = { Text("Notebook") }
                        )
                        NavigationBarItem(
                            selected = currentPage == "courses",
                            onClick = { currentPage = "courses" },
                            icon = { Icon(Icons.Default.Class, contentDescription = "Courses") },
                            label = { Text("Classes") }
                        )
                        NavigationBarItem(
                            selected = currentPage == "flashcards",
                            onClick = { currentPage = "flashcards" },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Study Quiz") },
                            label = { Text("AI Quiz") }
                        )
                        NavigationBarItem(
                            selected = currentPage == "synchub",
                            onClick = { currentPage = "synchub" },
                            icon = { Icon(Icons.Default.CloudSync, contentDescription = "Cloud Sync") },
                            label = { Text("Sync") }
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    when (currentPage) {
                        "dashboard" -> DashboardScreen(
                            viewModel = viewModel,
                            onCreateNote = {
                                viewModel.createNewNote()
                                currentPage = "notes"
                            },
                            onNavigateToPage = { currentPage = it }
                        )
                        "notes" -> WorkspaceScreen(viewModel = viewModel)
                        "courses" -> CoursesScreen(viewModel = viewModel)
                        "flashcards" -> FlashcardStudyScreen(viewModel = viewModel)
                        "synchub" -> SyncHubScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationSidebarItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
