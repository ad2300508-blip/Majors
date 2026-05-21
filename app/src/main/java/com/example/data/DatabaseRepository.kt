package com.example.data

import kotlinx.coroutines.flow.Flow

class DatabaseRepository(private val database: AppDatabase) {

    // Courses
    val allCourses: Flow<List<Course>> = database.courseDao().getAllCourses()

    suspend fun insertCourse(course: Course): Long = database.courseDao().insertCourse(course)
    suspend fun deleteCourse(id: Long) = database.courseDao().deleteCourseById(id)

    // Notes (sorted: pinned first, then by lastModified)
    val allNotes: Flow<List<Note>> = database.noteDao().getAllNotes()

    fun getNotesByCourse(courseId: Long): Flow<List<Note>> = database.noteDao().getNotesByCourse(courseId)
    suspend fun getNoteById(id: Long): Note? = database.noteDao().getNoteById(id)
    suspend fun insertNote(note: Note): Long = database.noteDao().insertNote(note)

    suspend fun deleteNote(id: Long) {
        database.flashcardDao().deleteFlashcardsByNoteId(id)
        database.noteDao().deleteNoteById(id)
    }

    suspend fun toggleNotePin(id: Long) {
        val note = database.noteDao().getNoteById(id) ?: return
        database.noteDao().insertNote(note.copy(isPinned = !note.isPinned))
    }

    // Assignments
    val allAssignments: Flow<List<Assignment>> = database.assignmentDao().getAllAssignments()

    fun getAssignmentsByCourse(courseId: Long): Flow<List<Assignment>> =
        database.assignmentDao().getAssignmentsByCourse(courseId)

    suspend fun insertAssignment(assignment: Assignment): Long =
        database.assignmentDao().insertAssignment(assignment)

    suspend fun deleteAssignment(id: Long) = database.assignmentDao().deleteAssignmentById(id)

    // Flashcards
    val upcomingFlashcards: Flow<List<Flashcard>> = database.flashcardDao().getUpcomingFlashcards()

    fun getFlashcardsByNote(noteId: Long): Flow<List<Flashcard>> =
        database.flashcardDao().getFlashcardsByNote(noteId)

    suspend fun getFlashcardById(id: Long): Flashcard? = database.flashcardDao().getFlashcardById(id)
    suspend fun insertFlashcard(flashcard: Flashcard): Long = database.flashcardDao().insertFlashcard(flashcard)
    suspend fun deleteFlashcard(id: Long) = database.flashcardDao().deleteFlashcardById(id)

    // Grades
    val allGrades: Flow<List<Grade>> = database.gradeDao().getAllGrades()

    fun getGradesByCourse(courseId: Long): Flow<List<Grade>> =
        database.gradeDao().getGradesByCourse(courseId)

    suspend fun insertGrade(grade: Grade): Long = database.gradeDao().insertGrade(grade)
    suspend fun deleteGrade(id: Long) = database.gradeDao().deleteGradeById(id)
}
