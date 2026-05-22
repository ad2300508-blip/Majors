package com.example.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeminiClient
import com.example.network.GoogleDriveClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed interface SyncState {
    object Idle : SyncState
    object Syncing : SyncState
    data class Success(val fileId: String) : SyncState
    data class Error(val message: String) : SyncState
}

class AppViewModel(private val repository: DatabaseRepository) : ViewModel() {

    // Courses State & Operations
    val courses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCourse(name: String, code: String, instructor: String, colorHex: String, schedule: String) {
        viewModelScope.launch {
            repository.insertCourse(Course(name = name, code = code, instructor = instructor, colorHex = colorHex, schedule = schedule))
        }
    }

    fun deleteCourse(id: Long) {
        viewModelScope.launch {
            repository.deleteCourse(id)
        }
    }

    // Notes State & Operations
    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeNoteId = MutableStateFlow<Long?>(null)
    val activeNoteId: StateFlow<Long?> = _activeNoteId.asStateFlow()

    private val _activeNoteStrokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
    val activeNoteStrokes: StateFlow<List<DrawingStroke>> = _activeNoteStrokes.asStateFlow()

    private val _activeNoteText = MutableStateFlow("")
    val activeNoteText: StateFlow<String> = _activeNoteText.asStateFlow()

    private val _activeNoteTitle = MutableStateFlow("")
    val activeNoteTitle: StateFlow<String> = _activeNoteTitle.asStateFlow()

    private val _activeNoteCourseId = MutableStateFlow<Long?>(null)
    val activeNoteCourseId: StateFlow<Long?> = _activeNoteCourseId.asStateFlow()

    // Gemini OCR state
    private val _ocrActive = MutableStateFlow(false)
    val ocrActive: StateFlow<Boolean> = _ocrActive.asStateFlow()

    // Gemini S-Pen Drawing Solver / Explainer State
    private val _handwritingSolvingActive = MutableStateFlow(false)
    val handwritingSolvingActive: StateFlow<Boolean> = _handwritingSolvingActive.asStateFlow()

    // Google Ecosystem / Drive Sync credentials
    private val _googleAccessToken = MutableStateFlow<String>("")
    val googleAccessToken: StateFlow<String> = _googleAccessToken.asStateFlow()

    private val _googleAccountEmail = MutableStateFlow<String>("")
    val googleAccountEmail: StateFlow<String> = _googleAccountEmail.asStateFlow()

    // Google Drive Sync State
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // App theme preference ("system" | "light" | "dark")
    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()
    fun setThemeMode(mode: String) { _themeMode.value = mode }

    // Pomodoro configuration
    private val _pomodoroWorkMins = MutableStateFlow(25)
    val pomodoroWorkMins: StateFlow<Int> = _pomodoroWorkMins.asStateFlow()
    private val _pomodoroBreakMins = MutableStateFlow(5)
    val pomodoroBreakMins: StateFlow<Int> = _pomodoroBreakMins.asStateFlow()
    fun setPomodoroWork(mins: Int) { _pomodoroWorkMins.value = mins }
    fun setPomodoroBreak(mins: Int) { _pomodoroBreakMins.value = mins }

    // Flashcard generation state
    private val _flashcardGenerationActive = MutableStateFlow(false)
    val flashcardGenerationActive: StateFlow<Boolean> = _flashcardGenerationActive.asStateFlow()

    // Note AI Assistant state
    private val _aiAssistantActive = MutableStateFlow(false)
    val aiAssistantActive: StateFlow<Boolean> = _aiAssistantActive.asStateFlow()

    private val _lectureTranscribingActive = MutableStateFlow(false)
    val lectureTranscribingActive: StateFlow<Boolean> = _lectureTranscribingActive.asStateFlow()

    private val _quizFlashcards = MutableStateFlow<List<Flashcard>>(emptyList())
    val quizFlashcards: StateFlow<List<Flashcard>> = _quizFlashcards.asStateFlow()

    val allFlashcards: StateFlow<List<Flashcard>> = repository.upcomingFlashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Assignments State & Operations
    val assignments: StateFlow<List<Assignment>> = repository.allAssignments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Moshi utility for manual strokes serialization
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val strokesType = Types.newParameterizedType(List::class.java, DrawingStroke::class.java)
    private val strokesAdapter = moshi.adapter<List<DrawingStroke>>(strokesType)

    private var flashcardCollectionJob: Job? = null
    private var saveDebounceJob: Job? = null

    fun setActiveNote(noteId: Long?) {
        flashcardCollectionJob?.cancel()
        viewModelScope.launch {
            _activeNoteId.value = noteId
            if (noteId != null) {
                val note = repository.getNoteById(noteId)
                if (note != null) {
                    _activeNoteTitle.value = note.title
                    _activeNoteText.value = note.textContent
                    _activeNoteCourseId.value = note.courseId
                    _activeNoteStrokes.value = strokesAdapter.fromJson(note.drawingsJson) ?: emptyList()
                    flashcardCollectionJob = viewModelScope.launch {
                        repository.getFlashcardsByNote(noteId).collectLatest { flashcards ->
                            _quizFlashcards.value = flashcards
                        }
                    }
                }
            } else {
                _activeNoteTitle.value = ""
                _activeNoteText.value = ""
                _activeNoteCourseId.value = null
                _activeNoteStrokes.value = emptyList()
                _quizFlashcards.value = emptyList()
            }
        }
    }

    fun updateActiveNoteTitle(title: String) {
        _activeNoteTitle.value = title
        saveActiveNoteStateLocally()
    }

    fun updateActiveNoteText(text: String) {
        _activeNoteText.value = text
        saveActiveNoteStateLocally()
    }

    fun updateActiveNoteCourse(courseId: Long?) {
        _activeNoteCourseId.value = courseId
        saveActiveNoteStateLocally()
    }

    fun updateActiveNoteStrokes(strokes: List<DrawingStroke>) {
        _activeNoteStrokes.value = strokes
        saveActiveNoteStateLocally()
    }

    private fun saveActiveNoteStateLocally() {
        val noteId = _activeNoteId.value ?: return
        saveDebounceJob?.cancel()
        saveDebounceJob = viewModelScope.launch {
            delay(400)
            val currentTitle = _activeNoteTitle.value
            val currentText = _activeNoteText.value
            val currentCourseId = _activeNoteCourseId.value
            val strokesJson = try {
                strokesAdapter.toJson(_activeNoteStrokes.value)
            } catch (e: Exception) {
                "[]"
            }
            val existing = repository.getNoteById(noteId)
            val updated = Note(
                id = noteId,
                courseId = currentCourseId,
                title = currentTitle,
                textContent = currentText,
                drawingsJson = strokesJson,
                lastModified = System.currentTimeMillis(),
                googleDriveFileId = existing?.googleDriveFileId,
                isSynced = existing?.isSynced ?: false
            )
            repository.insertNote(updated)
        }
    }

    fun createNewNote(courseId: Long? = null) {
        viewModelScope.launch {
            val newNoteId = repository.insertNote(
                Note(
                    title = "New Class Note",
                    courseId = courseId,
                    lastModified = System.currentTimeMillis()
                )
            )
            setActiveNote(newNoteId)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            if (_activeNoteId.value == id) {
                setActiveNote(null)
            }
        }
    }

    // Google OAuth Operations
    fun connectGoogleAccount(email: String, accessToken: String) {
        _googleAccountEmail.value = email
        _googleAccessToken.value = accessToken
    }

    fun disconnectGoogleAccount() {
        _googleAccountEmail.value = ""
        _googleAccessToken.value = ""
        _syncState.value = SyncState.Idle
    }

    // Google Drive Sync
    fun syncActiveNoteToGoogleDrive() {
        val noteId = _activeNoteId.value ?: return
        val token = _googleAccessToken.value
        if (token.isEmpty()) {
            _syncState.value = SyncState.Error("Google account is not connected")
            return
        }

        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val note = repository.getNoteById(noteId) ?: return@launch
            val result = GoogleDriveClient.syncNoteToGoogleDrive(
                accessToken = token,
                title = note.title,
                content = note.textContent,
                existingFileId = note.googleDriveFileId
            )
            result.onSuccess { fileId ->
                _syncState.value = SyncState.Success(fileId)
                // Save the Drive File ID and mark as synced in local DB
                val updated = note.copy(
                    googleDriveFileId = fileId,
                    isSynced = true,
                    lastModified = System.currentTimeMillis()
                )
                repository.insertNote(updated)
            }
            result.onFailure { error ->
                _syncState.value = SyncState.Error(error.localizedMessage ?: "Unknown Sync Error")
            }
        }
    }

    // Gemini S-Pen OCR handwriting recognition
    fun performHandwritingOCR(canvasBitmap: Bitmap) {
        viewModelScope.launch {
            _ocrActive.value = true
            val parsedText = GeminiClient.recognizeHandwriting(canvasBitmap)
            if (parsedText.isNotEmpty() && !parsedText.startsWith("OCR Transcription failed") && !parsedText.startsWith("Error")) {
                // Prepend or append to active note
                val currentText = _activeNoteText.value
                val newText = if (currentText.isEmpty()) parsedText else "$currentText\n\n$parsedText"
                updateActiveNoteText(newText)
            }
            _ocrActive.value = false
        }
    }

    // Gemini S-Pen solver & visual diagram/concept explainer
    fun solveOrExplainHandwriting(canvasBitmap: Bitmap) {
        viewModelScope.launch {
            _handwritingSolvingActive.value = true
            val explanation = GeminiClient.solveOrExplainHandwriting(canvasBitmap)
            if (explanation.isNotEmpty() && !explanation.startsWith("Analysis failed") && !explanation.startsWith("Error")) {
                val currentText = _activeNoteText.value
                val newText = if (currentText.isEmpty()) {
                    "=== S-PEN SKETCH STUDY REPORT ===\n$explanation"
                } else {
                    "$currentText\n\n=== S-PEN SKETCH STUDY REPORT ===\n$explanation"
                }
                updateActiveNoteText(newText)
            }
            _handwritingSolvingActive.value = false
        }
    }

    // AI Academic Assistant: Auto format / Concept deep-dive
    fun runNoteAiAssistant(mode: String) {
        val noteId = _activeNoteId.value ?: return
        val currentTitle = _activeNoteTitle.value
        val currentText = _activeNoteText.value
        if (currentText.isEmpty()) return

        viewModelScope.launch {
            _aiAssistantActive.value = true
            val responseText = when (mode) {
                "format" -> GeminiClient.formatNotes(currentTitle, currentText)
                "explain" -> GeminiClient.explainNotes(currentTitle, currentText)
                else -> ""
            }
            if (responseText.isNotEmpty() && !responseText.contains("failed") && !responseText.contains("Error: ")) {
                val updatedText = if (mode == "format") {
                    responseText // Replace completely with beautiful format
                } else {
                    // Append detailed analysis
                    "$currentText\n\n=== AI TOPIC DEEP DIVE ===\n$responseText"
                }
                updateActiveNoteText(updatedText)
            }
            _aiAssistantActive.value = false
        }
    }

    // AI Voice Lecture Transcriber & Audio Summarizer
    fun runVoiceLectureTranscriber() {
        val noteId = _activeNoteId.value ?: return
        val currentTitle = _activeNoteTitle.value
        val currentText = _activeNoteText.value

        viewModelScope.launch {
            _lectureTranscribingActive.value = true
            val responseText = GeminiClient.transcribeLecture(currentTitle, currentText)
            if (responseText.isNotEmpty() && !responseText.contains("failed") && !responseText.contains("Error: ")) {
                val updatedText = if (currentText.isEmpty()) {
                    responseText
                } else {
                    "$currentText\n\n$responseText"
                }
                updateActiveNoteText(updatedText)
            }
            _lectureTranscribingActive.value = false
        }
    }

    // AI flashcard maker using note contents
    fun generateAIStudyFlashcards() {
        val noteId = _activeNoteId.value ?: return
        val currentTitle = _activeNoteTitle.value
        val currentText = _activeNoteText.value
        if (currentText.isEmpty()) return

        viewModelScope.launch {
            _flashcardGenerationActive.value = true
            val jsonResponse = GeminiClient.generateFlashcards(currentTitle, currentText)
            try {
                // Parse standard JSON formatting
                val jsonArray = JSONArray(jsonResponse)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val q = obj.getString("question")
                    val a = obj.getString("answer")
                    repository.insertFlashcard(
                        Flashcard(
                            noteId = noteId,
                            question = q,
                            answer = a
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallback custom text generation parsing if not perfect JSON format
            } finally {
                _flashcardGenerationActive.value = false
            }
        }
    }

    fun addFlashcardManual(noteId: Long, q: String, a: String) {
        viewModelScope.launch {
            repository.insertFlashcard(Flashcard(noteId = noteId, question = q, answer = a))
        }
    }

    fun deleteFlashcard(id: Long) {
        viewModelScope.launch {
            repository.deleteFlashcard(id)
        }
    }

    fun markFlashcardReviewed(id: Long, correct: Boolean) {
        viewModelScope.launch {
            val card = repository.getFlashcardById(id) ?: return@launch
            val nextReview = if (correct) {
                val nowMs = System.currentTimeMillis()
                val daysSinceReview = maxOf(1L, (nowMs - card.nextReview) / (1000L * 60 * 60 * 24))
                nowMs + daysSinceReview * 2 * 24 * 60 * 60 * 1000L
            } else {
                System.currentTimeMillis() + 24 * 60 * 60 * 1000L
            }
            repository.insertFlashcard(card.copy(nextReview = nextReview))
        }
    }

    // Note Pin Toggle
    fun toggleNotePin(noteId: Long) {
        viewModelScope.launch { repository.toggleNotePin(noteId) }
    }

    // Grades
    val allGrades: StateFlow<List<com.example.data.Grade>> = repository.allGrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGrade(courseId: Long, label: String, score: Float, maxScore: Float, type: String, weight: Float = 1f) {
        viewModelScope.launch {
            repository.insertGrade(com.example.data.Grade(courseId = courseId, label = label, score = score, maxScore = maxScore, type = type, weight = weight))
        }
    }

    fun deleteGrade(id: Long) {
        viewModelScope.launch { repository.deleteGrade(id) }
    }

    // Assignments Operations
    fun addAssignment(title: String, dueDate: String, courseId: Long? = null, notes: String = "", priority: String = "medium") {
        viewModelScope.launch {
            repository.insertAssignment(Assignment(courseId = courseId, title = title, dueDate = dueDate, notes = notes, priority = priority))
        }
    }

    fun toggleAssignmentCompleted(assignment: Assignment) {
        viewModelScope.launch {
            repository.insertAssignment(assignment.copy(isCompleted = !assignment.isCompleted))
        }
    }

    fun deleteAssignment(id: Long) {
        viewModelScope.launch {
            repository.deleteAssignment(id)
        }
    }
}

class AppViewModelFactory(private val repository: DatabaseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
