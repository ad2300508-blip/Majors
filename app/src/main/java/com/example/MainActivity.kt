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
    val courses by viewModel.courses.collectAsState()

    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Brand
        Row(
            modifier = Modifier.padding(start = 10.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "M",
                    color = MaterialTheme.colorScheme.background,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
            Column {
                Text("Majors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "STUDY OS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    letterSpacing = 0.12.sp
                )
            }
        }

        // WORKSPACE section
        Text(
            "WORKSPACE",
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            letterSpacing = 0.12.sp
        )
        NAV_ITEMS.take(4).forEach { item ->
            SidebarNavItem(
                item = item,
                selected = currentPage == item.id,
                onClick = { onNavigate(item.id) },
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LIBRARY section — show courses as colored dot items
        Text(
            "LIBRARY",
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            letterSpacing = 0.12.sp
        )
        courses.take(5).forEach { course ->
            val courseColor = try {
                Color(android.graphics.Color.parseColor(course.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigate("courses") }
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(courseColor, RoundedCornerShape(3.dp))
                )
                Text(
                    course.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Settings nav item
        SidebarNavItem(
            item = NAV_ITEMS.last(),
            selected = currentPage == NAV_ITEMS.last().id,
            onClick = { onNavigate(NAV_ITEMS.last().id) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Account footer
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.background,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Avatar with gradient
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "MV",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (email.isNotEmpty()) "Connected" else "Local Only",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Text(
                        email.ifEmpty { "Offline storage" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
                // Sync dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (email.isNotEmpty()) Color(0xFF4A8053) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun SidebarNavItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        animationSpec = tween(180),
        label = "SidebarBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "SidebarFg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .testTag(item.tag)
    ) {
        // Left accent bar
        if (selected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-14).dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
            )
        }
        Row(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(item.icon, contentDescription = item.label, tint = contentColor, modifier = Modifier.size(18.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                fontSize = 14.sp
            )
        }
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
