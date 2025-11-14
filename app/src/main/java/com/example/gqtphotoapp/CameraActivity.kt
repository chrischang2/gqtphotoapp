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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get selected album from intent
        selectedAlbum = intent.getStringExtra("ALBUM_NAME")

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
            val imageFileName = "JPEG_${timeStamp}.jpg"

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
                    val message = if (selectedAlbum != null) {
                        "Photo saved to '$selectedAlbum' album!"
                    } else {
                        "Photo saved successfully!"
                    }
                    Log.d("CameraActivity", "Photo saved successfully at: $uri")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                } ?: run {
                    Log.e("CameraActivity", "Photo URI is null")
                    Toast.makeText(this, "Error: Photo not saved", Toast.LENGTH_SHORT).show()
                }
            } else {
                currentPhotoUri?.let { uri ->
                    contentResolver.delete(uri, null, null)
                    Log.d("CameraActivity", "Deleted cancelled photo entry")
                }
                Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
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