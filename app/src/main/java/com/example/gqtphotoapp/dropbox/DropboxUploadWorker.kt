package com.example.gqtphotoapp.dropbox

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.gqtphotoapp.dropbox.DropboxManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DropboxUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_ALBUM_NAME = "album_name"
        const val KEY_UPLOADED_COUNT = "uploaded_count"
        const val KEY_TOTAL_COUNT = "total_count"
        const val TAG = "DropboxUploadWorker"
    }

    private val dropboxManager = DropboxManager.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if authenticated
            val client = dropboxManager.getClient()
            if (client == null) {
                Log.e(TAG, "Not authenticated with Dropbox")
                return@withContext Result.failure(
                    workDataOf("error" to "Not authenticated")
                )
            }

            val albumName = inputData.getString(KEY_ALBUM_NAME)
            if (albumName == null) {
                return@withContext Result.failure(
                    workDataOf("error" to "Invalid album name")
                )
            }

            // Query photos from MediaStore for this album
            val photos = getPhotosFromAlbum(albumName)

            if (photos.isEmpty()) {
                return@withContext Result.success(
                    workDataOf(
                        KEY_UPLOADED_COUNT to 0,
                        KEY_TOTAL_COUNT to 0,
                        "message" to "No photos found in album"
                    )
                )
            }

            Log.d(TAG, "Found ${photos.size} photos in album '$albumName'")

            // 🔥 SET INITIAL PROGRESS WITH TOTAL COUNT
            setProgress(workDataOf(
                KEY_UPLOADED_COUNT to 0,
                KEY_TOTAL_COUNT to photos.size
            ))

            val folderPath = "/GQTPhotoApp/$albumName"
            var uploadedCount = 0
            val failedPhotos = mutableListOf<String>()

            // Create folder if it doesn't exist
            try {
                client.files().createFolderV2(folderPath)
                Log.d(TAG, "Created Dropbox folder: $folderPath")
            } catch (e: Exception) {
                // Folder might already exist, that's okay
                Log.d(TAG, "Folder may already exist: ${e.message}")
            }

            // Upload each photo
            photos.forEachIndexed { index, photoInfo ->
                try {
                    val remotePath = "$folderPath/${photoInfo.displayName}"

                    Log.d(TAG, "Attempting to upload: ${photoInfo.displayName} from ${photoInfo.uri}")

                    val inputStream = applicationContext.contentResolver.openInputStream(photoInfo.uri)

                    if (inputStream == null) {
                        Log.e(TAG, "Failed to open input stream for: ${photoInfo.displayName}")
                        failedPhotos.add(photoInfo.displayName)
                    } else {
                        inputStream.use { stream ->
                            val metadata = client.files().uploadBuilder(remotePath)
                                .withMode(com.dropbox.core.v2.files.WriteMode.OVERWRITE)
                                .uploadAndFinish(stream)

                            Log.d(TAG, "Successfully uploaded: ${photoInfo.displayName}, size: ${metadata.size}")
                        }

                        uploadedCount++
                        Log.d(TAG, "Uploaded: ${photoInfo.displayName} ($uploadedCount/${photos.size})")

                        // Update progress
                        setProgress(workDataOf(
                            KEY_UPLOADED_COUNT to uploadedCount,
                            KEY_TOTAL_COUNT to photos.size
                        ))
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload photo: ${photoInfo.displayName}", e)
                    Log.e(TAG, "Error details: ${e.javaClass.simpleName}: ${e.message}")
                    e.printStackTrace()
                    failedPhotos.add(photoInfo.displayName)
                    // Continue with other photos
                }
            }

            val resultData = workDataOf(
                KEY_UPLOADED_COUNT to uploadedCount,
                KEY_TOTAL_COUNT to photos.size
            )

            if (failedPhotos.isNotEmpty()) {
                Log.e(TAG, "Failed to upload ${failedPhotos.size} photos: ${failedPhotos.joinToString()}")
            }

            Result.success(resultData)

        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            e.printStackTrace()
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }

    private data class PhotoInfo(val uri: android.net.Uri, val displayName: String)

    private fun getPhotosFromAlbum(albumName: String): List<PhotoInfo> {
        val photos = mutableListOf<PhotoInfo>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%GQTPhotoApp/$albumName%")

        applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val uri = android.net.Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                photos.add(PhotoInfo(uri, name))
                Log.d(TAG, "Found photo: $name")
            }
        }

        return photos
    }
}