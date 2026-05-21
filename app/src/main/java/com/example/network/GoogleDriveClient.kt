package com.example.network

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class DriveFileMetadata(
    val name: String,
    val mimeType: String? = null,
    val parents: List<String>? = null,
    val id: String? = null
)

@JsonClass(generateAdapter = true)
data class DriveFileListResponse(
    val files: List<DriveFileMetadata>
)

interface GoogleDriveApiService {

    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") authHeader: String,
        @Query("q") query: String
    ): DriveFileListResponse

    @POST("drive/v3/files")
    suspend fun createMetadataFile(
        @Header("Authorization") authHeader: String,
        @Body metadata: DriveFileMetadata
    ): DriveFileMetadata

    @PUT("upload/drive/v3/files/{fileId}?uploadType=media")
    suspend fun uploadFileContent(
        @Header("Authorization") authHeader: String,
        @Header("Content-Type") contentType: String,
        @Path("fileId") fileId: String,
        @Body content: okhttp3.RequestBody
    ): DriveFileMetadata
}

object GoogleDriveClient {
    private const val BASE_URL = "https://www.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val service: GoogleDriveApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleDriveApiService::class.java)
    }

    /**
     * Authenticate and upload a student note block directly to the user's Google Drive.
     * It connects to the Google API and uploads the note as a structured file.
     */
    suspend fun syncNoteToGoogleDrive(
        accessToken: String,
        title: String,
        content: String,
        existingFileId: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val authHeader = "Bearer $accessToken"
        try {
            var fileId = existingFileId

            if (fileId == null) {
                // Step 1: Search if ScholarWorkspace folder exists or create it
                val folderQuery = "name='ScholarWorkspace' and mimeType='application/vnd.google-apps.folder' and trashed=false"
                val folders = service.listFiles(authHeader, folderQuery).files
                val folderId = if (folders.isNotEmpty()) {
                    folders.first().id!!
                } else {
                    val createFolderMeta = DriveFileMetadata(
                        name = "ScholarWorkspace",
                        mimeType = "application/vnd.google-apps.folder"
                    )
                    service.createMetadataFile(authHeader, createFolderMeta).id!!
                }

                // Step 2: Create a metadata-only file placeholder for the note inside the folder
                val createNoteMeta = DriveFileMetadata(
                    name = "$title.txt",
                    mimeType = "text/plain",
                    parents = listOf(folderId)
                )
                val placeholderFile = service.createMetadataFile(authHeader, createNoteMeta)
                fileId = placeholderFile.id!!
            }

            // Step 3: Upload the actual text contents of the student note to that file Id
            val bodyText = "TITLE: $title\n\nLAST SYNCED: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}\n\n=== CONTENTS ===\n$content"
            val requestBody = bodyText.toRequestBody("text/plain".toMediaTypeOrNull())
            service.uploadFileContent(authHeader, "text/plain", fileId, requestBody)

            Result.success(fileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
