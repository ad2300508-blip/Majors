package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.*
import com.example.ui.*
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class NavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val tag: String,
)

val NAV_ITEMS = listOf(
    NavItem("dashboard",  "Dashboard",  Icons.Default.Dashboard,    "nav_dashboard"),
    NavItem("notes",      "Notebook",   Icons.Default.BorderColor,  "nav_notes"),
    NavItem("courses",    "Classes",    Icons.Default.Class,        "nav_courses"),
    NavItem("flashcards", "AI Quiz",    Icons.Default.AutoAwesome,  "nav_flashcards"),
    NavItem("synchub",    "Settings",   Icons.Default.Settings,     "nav_settings"),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database   = AppDatabase.getDatabase(this)
        val repository = DatabaseRepository(database)
        val viewModel  = ViewModelProvider(this, AppViewModelFactory(repository))[AppViewModel::class.java]

        lifecycleScope.launch {
            val existingCourses = repository.allCourses.first()
            if (existingCourses.isEmpty()) {
                val csId = repository.insertCourse(Course(name = "Analysis of Algorithms", code = "CS201", instructor = "Prof. Alan Turing", colorHex = "#1E88E5", schedule = "Mon, Wed 10:00 - 11:30"))
                val mathId = repository.insertCourse(Course(name = "Advanced Linear Algebra", code = "MATH251", instructor = "Prof. Carl Gauss", colorHex = "#D81B60", schedule = "Tue, Thu 13:00 - 14:30"))
                val physicsId = repository.insertCourse(Course(name = "Electromagnetism Theory", code = "PHYS301", instructor = "Prof. James Maxwell", colorHex = "#43A047", schedule = "Fri 14:00 - 16:30"))
                repository.insertAssignment(Assignment(courseId = csId, title = "Problem Set 2: Dijkstra's Proof", dueDate = "2026-06-05", notes = "Prove optimal greedy substructure"))
                repository.insertAssignment(Assignment(courseId = mathId, title = "Matrix Orthogonality Homework", dueDate = "2026-05-28", notes = "Gram-Schmidt projections"))
                val noteId = repository.insertNote(Note(courseId = csId, title = "Lecture 1: Graph BFS & Matrix Models", textContent = "A Breadth-First-Search (BFS) traverses a tree or graph level by level.\n\nIt uses a FIFO Queue structure to queue adjacent vertices.\nComplexity bounds are O(V + E) where V is vertices, and E is edges.\n\nWe can represent state transitions as matrices, linking and syncing mathematically.", drawingsJson = "[]"))
                repository.insertFlashcard(Flashcard(noteId = noteId, question = "What is the time complexity of Breadth-First-Search?", answer = "O(V + E) where V represents Vertices and E represents Edges."))
                repository.insertFlashcard(Flashcard(noteId = noteId, question = "What primary data structure does BFS use?", answer = "A First-In-First-Out (FIFO) Queue."))
            }
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "dark"  -> true
                "light" -> false
                else    -> isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
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
            Row(modifier = Modifier.fillMaxSize()) {
                // ── Sidebar ──────────────────────────────────────────────────
                TabletSidebar(
                    currentPage = currentPage,
                    onNavigate = { currentPage = it },
                    viewModel = viewModel,
                )
                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                // ── Content ───────────────────────────────────────────────────
                AnimatedContent(
                    targetState = currentPage,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    transitionSpec = {
                        val enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f)
                        val exit  = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.98f)
                        enter togetherWith exit
                    },
                    label = "PageTransition"
                ) { page ->
                    PageContent(page, viewModel) { currentPage = it }
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    ScholarBottomBar(
                        currentPage = currentPage,
                        onNavigate = { currentPage = it },
                    )
                }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = currentPage,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    transitionSpec = {
                        val cur = NAV_ITEMS.indexOfFirst { it.id == initialState }
                        val tgt = NAV_ITEMS.indexOfFirst { it.id == targetState }
                        val dir = if (tgt > cur) 1 else -1
                        val enter = slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { dir * it / 3 } + fadeIn(tween(280))
                        val exit  = slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -dir * it / 3 } + fadeOut(tween(200))
                        enter togetherWith exit
                    },
                    label = "MobilePageTransition"
                ) { page ->
                    PageContent(page, viewModel) { currentPage = it }
                }
            }
        }
    }
}

@Composable
fun PageContent(page: String, viewModel: AppViewModel, onNavigate: (String) -> Unit) {
    when (page) {
        "dashboard"  -> DashboardScreen(viewModel = viewModel, onCreateNote = { viewModel.createNewNote(); onNavigate("notes") }, onNavigateToPage = onNavigate)
        "notes"      -> WorkspaceScreen(viewModel = viewModel)
        "courses"    -> CoursesScreen(viewModel = viewModel, onNavigateToPage = onNavigate)
        "flashcards" -> FlashcardStudyScreen(viewModel = viewModel)
        "synchub"    -> SyncHubScreen(viewModel = viewModel)
    }
}

@Composable
fun TabletSidebar(
    currentPage: String,
    onNavigate: (String) -> Unit,
    viewModel: AppViewModel,
) {
    val email by viewModel.googleAccountEmail.collectAsState()

    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Brand header
        Row(
            modifier = Modifier.padding(start = 8.dp, bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("ScholarSpace", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Study OS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Nav items
        NAV_ITEMS.forEach { item ->
            SidebarNavItem(
                item = item,
                selected = currentPage == item.id,
                onClick = { onNavigate(item.id) },
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Account footer
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (email.isNotEmpty()) Color(0xFF1B5E20) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (email.isNotEmpty()) Icons.Default.CloudDone else Icons.Default.OfflineBolt,
                        contentDescription = null,
                        tint = if (email.isNotEmpty()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(if (email.isNotEmpty()) "Connected" else "Local Only", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(email.ifEmpty { "Offline Storage" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun SidebarNavItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
        animationSpec = tween(200),
        label = "SidebarBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "SidebarFg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 14.dp)
            .testTag(item.tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent line for selected
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(24.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(item.icon, contentDescription = item.label, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
fun ScholarBottomBar(currentPage: String, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        NAV_ITEMS.forEach { item ->
            NavigationBarItem(
                selected = currentPage == item.id,
                onClick = { onNavigate(item.id) },
                icon = {
                    Icon(item.icon, contentDescription = item.label)
                },
                label = { Text(item.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
