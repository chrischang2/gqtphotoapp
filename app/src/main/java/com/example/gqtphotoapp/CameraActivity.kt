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
    private var photoLabel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get selected album from intent
        selectedAlbum = intent.getStringExtra("ALBUM_NAME")

        // Get photo label from intent
        photoLabel = intent.getStringExtra("PHOTO_LABEL")

        // Show user which photo type they're taking
        photoLabel?.let {
            Toast.makeText(this, "Taking: $it", Toast.LENGTH_SHORT).show()
        }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
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
            // Get the next number for this label
            val photoNumber = getNextPhotoNumber(photoLabel ?: "Photo")

            // Create filename with label and number
            val imageFileName = "${photoLabel ?: "Photo"} ($photoNumber).jpg"

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

            val uri =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            Log.d(
                "CameraActivity",
                "Created URI with filename '$imageFileName' in album '$selectedAlbum': $uri"
            )
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
                    // Increment the counter for this label
                    incrementPhotoNumber(photoLabel ?: "Photo")

                    val photoNumber = getCurrentPhotoNumber(photoLabel ?: "Photo")
                    Toast.makeText(
                        this,
                        "Saved: ${photoLabel ?: "Photo"} ($photoNumber)",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d("CameraActivity", "Photo saved successfully")

                    // Open camera again for next photo
                    openCamera()
                } ?: run {
                    Log.e("CameraActivity", "Photo URI is null")
                    Toast.makeText(this, "Error: Photo not saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                // User cancelled - delete the empty entry and exit
                currentPhotoUri?.let { uri ->
                    contentResolver.delete(uri, null, null)
                    Log.d("CameraActivity", "Deleted cancelled photo entry")
                }
                Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getNextPhotoNumber(label: String): Int {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val key = "photo_count_${selectedAlbum}_$label"
        return sharedPrefs.getInt(key, 0) + 1
    }

    private fun getCurrentPhotoNumber(label: String): Int {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val key = "photo_count_${selectedAlbum}_$label"
        return sharedPrefs.getInt(key, 0)
    }

    private fun incrementPhotoNumber(label: String) {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val key = "photo_count_${selectedAlbum}_$label"
        val currentCount = sharedPrefs.getInt(key, 0)
        sharedPrefs.edit().putInt(key, currentCount + 1).apply()
        Log.d("CameraActivity", "Incremented count for '$label' to ${currentCount + 1}")
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
                Toast.makeText(
                    this,
                    "Camera permission is required to take photos",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}