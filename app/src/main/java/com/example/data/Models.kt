package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class StylusPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

data class DrawingStroke(
    val points: List<StylusPoint>,
    val color: Int,
    val width: Float
)

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val instructor: String,
    val colorHex: String,
    val schedule: String
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long? = null,
    val title: String,
    val textContent: String = "",
    val drawingsJson: String = "[]", // Serialized List<DrawingStroke>
    val lastModified: Long = System.currentTimeMillis(),
    val googleDriveFileId: String? = null,
    val isSynced: Boolean = false
) {
    fun getFormattedDate(): String {
        return java.text.DateFormat.getDateTimeInstance().format(java.util.Date(lastModified))
    }
}

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long? = null,
    val title: String,
    val dueDate: String,
    val notes: String = "",
    val isCompleted: Boolean = false
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val question: String,
    val answer: String,
    val nextReview: Long = System.currentTimeMillis()
)

class DatabaseTypeConverters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val strokeListType = Types.newParameterizedType(List::class.java, DrawingStroke::class.java)
    private val adapter = moshi.adapter<List<DrawingStroke>>(strokeListType)

    @TypeConverter
    fun stringToStrokes(value: String): List<DrawingStroke> {
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun strokesToString(strokes: List<DrawingStroke>): String {
        return try {
            adapter.toJson(strokes)
        } catch (e: Exception) {
            "[]"
        }
    }
}
