package com.example.gqtphotoapp

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : AppCompatActivity() {

    private val CAMERA_REQUEST_CODE = 100
    private val CAMERA_PERMISSION_CODE = 101
    private var currentPhotoUri: Uri? = null
    private var selectedAlbum: String? = null

    // Sequence mode variables
    private var isSequenceMode = false
    private var currentPhotoNumber = 1
    private var photoLabels: List<String> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get selected album from intent
        selectedAlbum = intent.getStringExtra("ALBUM_NAME")

        // Get sequence mode settings from intent
        isSequenceMode = intent.getBooleanExtra("SEQUENCE_MODE", false)
        currentPhotoNumber = intent.getIntExtra("CURRENT_NUMBER", 1)

        // Get the photo labels list from the intent
        val labelsArray = intent.getStringArrayExtra("PHOTO_LABELS")
        photoLabels = labelsArray?.toList() ?: emptyList()

        // Show user which photo they're about to take
        if (isSequenceMode) {
            val photoLabel = if (currentPhotoNumber <= photoLabels.size) {
                photoLabels[currentPhotoNumber - 1]
            } else {
                "Photo $currentPhotoNumber"
            }
            Toast.makeText(
                this,
                "Taking: $photoLabel ($currentPhotoNumber of ${photoLabels.size})",
                Toast.LENGTH_LONG
            ).show()
        }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val photoUri = createImageUri()

        photoUri?.let {
            currentPhotoUri = it
            Log.d("CameraActivity", "Photo will be saved to URI: $currentPhotoUri")

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)

            if (cameraIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: run {
            Toast.makeText(this, "Error creating photo location", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun createImageUri(): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            // Create filename with sequence number if in sequence mode
            val imageFileName = if (isSequenceMode) {
                "Photo_${currentPhotoNumber}_${timeStamp}.jpg"
            } else {
                "JPEG_${timeStamp}.jpg"
            }

            // Determine the path based on whether an album is selected
            val relativePath = if (selectedAlbum != null) {
                "Pictures/GQTPhotoApp/$selectedAlbum"
            } else {
                "Pictures/GQTPhotoApp"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            Log.d("CameraActivity", "Created URI in album '$selectedAlbum': $uri")
            uri
        } catch (e: Exception) {
            Log.e("CameraActivity", "Error creating image URI", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                currentPhotoUri?.let { uri ->

                    // If in sequence mode, save progress and continue to next photo
                    if (isSequenceMode && currentPhotoNumber < photoLabels.size) {
                        currentPhotoNumber++

                        // Save progress to SharedPreferences
                        saveSequenceProgress()

                        // Restart camera for next photo
                        val intent = Intent(this, CameraActivity::class.java).apply {
                            putExtra("ALBUM_NAME", selectedAlbum)
                            putExtra("SEQUENCE_MODE", true)
                            putExtra("CURRENT_NUMBER", currentPhotoNumber)
                            putExtra("PHOTO_LABELS", photoLabels.toTypedArray())
                        }
                        startActivity(intent)
                    } else if (isSequenceMode && currentPhotoNumber >= photoLabels.size) {
                        Toast.makeText(this, "All ${photoLabels.size} photos taken! ✓", Toast.LENGTH_LONG).show()
                        // Clear sequence progress when complete
                        clearSequenceProgress()
                    }
                } ?: run {
                    Log.e("CameraActivity", "Photo URI is null")
                    Toast.makeText(this, "Error: Photo not saved", Toast.LENGTH_SHORT).show()
                }
            } else {
                currentPhotoUri?.let { uri ->
                    contentResolver.delete(uri, null, null)
                    Log.d("CameraActivity", "Deleted cancelled photo entry")
                }

                // Save progress even when cancelled so user can resume
                if (isSequenceMode) {
                    saveSequenceProgress()
                    val photoLabel = if (currentPhotoNumber <= photoLabels.size) {
                        photoLabels[currentPhotoNumber - 1]
                    } else {
                        "Photo $currentPhotoNumber"
                    }
                    val message = "Sequence paused at: $photoLabel"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
                }
            }
            finish()
        }
    }

    private fun saveSequenceProgress() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putBoolean("sequence_in_progress", true)
            putInt("sequence_current_number", currentPhotoNumber)
            putString("sequence_album", selectedAlbum)
            // Save the photo labels as a string set (converting list to set)
            putStringSet("sequence_photo_labels", photoLabels.toSet())
            apply()
        }
        Log.d("CameraActivity", "Saved sequence progress: $currentPhotoNumber of ${photoLabels.size}")
    }

    private fun clearSequenceProgress() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        sharedPrefs.edit().apply {
            remove("sequence_in_progress")
            remove("sequence_current_number")
            remove("sequence_album")
            remove("sequence_photo_labels")
            apply()
        }
        Log.d("CameraActivity", "Cleared sequence progress")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}