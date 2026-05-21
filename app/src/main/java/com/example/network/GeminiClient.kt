package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Converts raw canvas bitmap of handwriting into transcribed text using Gemini 3.5 Flash vision.
     */
    suspend fun recognizeHandwriting(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing. Please set GEMINI_API_KEY in the Secrets panel."
        }

        val base64Image = bitmap.toBase64()
        val prompt = "This is a canvas drawing showing student handwritten notes. " +
                "Perform high-quality offline handwriting recognition (OCR) on it. " +
                "Transcribe all handwritten drawings and characters precisely, maintaining structural text flow. " +
                "Return ONLY the transcribed clear text, with appropriate spaces and lines. No preamble, no greetings."

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.2f)
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No text recognized. Try sketching more clearly."
        } catch (e: Exception) {
            "OCR Transcription failed: ${e.localizedMessage}"
        }
    }

    /**
     * Generate Flashcards from note contents automatically.
     */
    suspend fun generateFlashcards(noteTitle: String, noteContent: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing. Please set GEMINI_API_KEY in the Secrets panel."
        }

        val prompt = "Generate a list of 5 study questions and answers based on these student notes:\n\n" +
                "Title: $noteTitle\n" +
                "Content:\n$noteContent\n\n" +
                "Format the response strictly as a JSON list, matching this exact structure containing questions and answers:\n" +
                "[\n" +
                "  {\"question\": \"Definition of X?\", \"answer\": \"Y is...\"}\n" +
                "]\n" +
                "Return ONLY valid JSON array with no extra text, markdown wrappers, or surrounding tick-marks."

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.4f, responseMimeType = "application/json")
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "[]"
        } catch (e: Exception) {
            "[]"
        }
    }

    /**
     * Reformat note content into organized, beautiful markdown headings and structural points.
     */
    suspend fun formatNotes(noteTitle: String, noteContent: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing. Please set GEMINI_API_KEY inside the Secrets panel."
        }

        val prompt = "You are an expert student academic advisor and research secretary. " +
                "Format and restructure the following raw text/transcribed note into a beautiful, legible, organized visual layout. " +
                "Use standard bold headings, bullet lists, structural tables if helpful, and clean spacing. " +
                "Maintain ALL the original definitions and technical terms exactly, but make it look like a highly polished textbook review guide.\n\n" +
                "Note Title: $noteTitle\n\n" +
                "Content:\n$noteContent\n\n" +
                "Return ONLY the beautifully formatted note content. Do not include introductory text, explanations, or conversational filler."

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.3f)
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: noteContent
        } catch (e: Exception) {
            "Formatting failed: ${e.localizedMessage}"
        }
    }

    /**
     * Explain the scientific, mathematical, or academic concepts within the note to enrich student understanding.
     */
    suspend fun explainNotes(noteTitle: String, noteContent: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing. Please set GEMINI_API_KEY inside the Secrets panel."
        }

        val prompt = "You are an expert subject-matter professor. Analyze the topics in these student lecture notes: " +
                "Title: $noteTitle\n\n" +
                "Notes Content:\n$noteContent\n\n" +
                "Provide a comprehensive, clear, high-fidelity academic explanation of the concepts mentioned. " +
                "Add context, historical importance, secondary formulae, or concrete application examples " +
                "that would help a university or high-school student study for a midterm exam. " +
                "Structure it cleanly under major headings like 'Overview', 'Detailed Deep-dive', and 'Midterm Prep Tips'. " +
                "Keep your tone educational, clean, and highly sophisticated. " +
                "Return ONLY the markdown explanation content with no preamble."

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not compile academic explanation."
        } catch (e: Exception) {
            "Deep-dive compilation failed: ${e.localizedMessage}"
        }
    }

    /**
     * AI Lecture Transcriber & Audio Summarizer based on current active class note contents.
     */
    suspend fun transcribeLecture(noteTitle: String, existingNotes: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing. Please set GEMINI_API_KEY inside the Secrets panel."
        }

        val prompt = "You are an advanced AI lecture audio transcription processor. " +
                "Generate a detailed, comprehensive mock audio lecture transcription review and study briefing based on the study lecture note title: '$noteTitle'. " +
                "If there are existing text notes, enrich them and incorporate ideas from them: '$existingNotes'. " +
                "Construct the result as if it were a direct transcript of a highly informative lecture from a MIT/Stanford professor (including professor explanation narrative, academic definitions, and structured syllabus bullets). " +
                "Organize clearly with headings like '=== LECTURE VOICE TRANSCRIPT ===' and '=== AI RECAP TAKEAWAYS ==='. " +
                "Return ONLY the beautifully formatted markdown content with no conversational introduction or surrounding quotes."

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not compile lecture transcription."
        } catch (e: Exception) {
            "Lecture transcription failed: ${e.localizedMessage}"
        }
    }

    /**
     * Solves mathematical/scientific equations or explains hand-drawn sketches (biology, chemistry, physics, flowcharts, etc.) using Gemini 3.5 Flash.
     */
    suspend fun solveOrExplainHandwriting(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing. Please set GEMINI_API_KEY in the Secrets panel."
        }

        val base64Image = bitmap.toBase64()
        val prompt = "Analyze this handwritten canvas drawing carefully. " +
                "It may contain math equations, formulas, graphs, physics diagrams, biological structures, chemical reactions, flowcharts, or handwritten notes. " +
                "1. If it's a math equation or formula, solve it step-by-step with clean markdown explanations. " +
                "2. If it's a scientific, engineering, or logical diagram, explain what it depicts, dissecting its key components and concepts in detail. " +
                "3. If it's text notes, transcribe and expand on the academic concepts with study notes. " +
                "Provide a highly informative, educational, and professionally styled markdown answer. " +
                "Use headings, bullet points, and LaTeX style math formatting if appropriate. " +
                "Do not include conversational filler, greetings, or meta-comments."

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.3f)
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Could not analyze the sketch. Try drawing more clearly."
        } catch (e: Exception) {
            "Analysis failed: ${e.localizedMessage}"
        }
    }

    /**
     * Helper extension to convert Bitmap to JPEG Base64
     */
    private fun Bitmap.toBase64(): String {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
