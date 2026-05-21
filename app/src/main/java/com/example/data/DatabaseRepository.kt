package com.example.data

import kotlinx.coroutines.flow.Flow

class DatabaseRepository(private val database: AppDatabase) {

    // Courses
    val allCourses: Flow<List<Course>> = database.courseDao().getAllCourses()

    suspend fun insertCourse(course: Course): Long {
        return database.courseDao().insertCourse(course)
    }

    suspend fun deleteCourse(id: Long) {
        database.courseDao().deleteCourseById(id)
    }

    // Notes
    val allNotes: Flow<List<Note>> = database.noteDao().getAllNotes()

    fun getNotesByCourse(courseId: Long): Flow<List<Note>> {
        return database.noteDao().getNotesByCourse(courseId)
    }

    suspend fun getNoteById(id: Long): Note? {
        return database.noteDao().getNoteById(id)
    }

    suspend fun insertNote(note: Note): Long {
        return database.noteDao().insertNote(note)
    }

    suspend fun deleteNote(id: Long) {
        database.noteDao().deleteNoteById(id)
    }

    // Assignments
    val allAssignments: Flow<List<Assignment>> = database.assignmentDao().getAllAssignments()

    fun getAssignmentsByCourse(courseId: Long): Flow<List<Assignment>> {
        return database.assignmentDao().getAssignmentsByCourse(courseId)
    }

    suspend fun insertAssignment(assignment: Assignment): Long {
        return database.assignmentDao().insertAssignment(assignment)
    }

    suspend fun deleteAssignment(id: Long) {
        database.assignmentDao().deleteAssignmentById(id)
    }

    // Flashcards
    val upcomingFlashcards: Flow<List<Flashcard>> = database.flashcardDao().getUpcomingFlashcards()

    fun getFlashcardsByNote(noteId: Long): Flow<List<Flashcard>> {
        return database.flashcardDao().getFlashcardsByNote(noteId)
    }

    suspend fun insertFlashcard(flashcard: Flashcard): Long {
        return database.flashcardDao().insertFlashcard(flashcard)
    }

    suspend fun deleteFlashcard(id: Long) {
        database.flashcardDao().deleteFlashcardById(id)
    }
}
