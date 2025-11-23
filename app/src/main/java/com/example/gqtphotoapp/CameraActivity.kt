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

        // Show user which photo type they're taking with counter
        photoLabel?.let {
            val currentCount = getCurrentPhotoNumber(it)
            val minCount = getMinCountForLabel(it)

            val displayMessage = if (minCount > 1) {
                "Taking: $it ($currentCount/$minCount)"
            } else {
                "Taking: $it"
            }
            Toast.makeText(this, displayMessage, Toast.LENGTH_SHORT).show()
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
            val label = photoLabel ?: "Photo"
            val photoNumber = getNextPhotoNumber(label)
            val expectsMultiple = checkIfMultipleExpected(label)

            val imageFileName = if (expectsMultiple) {
                PhotoLists.getPhotoFilename(label, photoNumber)
            } else {
                PhotoLists.getPhotoFilename(label, null)
            }

            // Get subfolder from PhotoLists
            val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
            val numContainers = sharedPrefs.getInt("num_containers", 0)
            val subfolder = PhotoLists.getSubfolder(label, numContainers, this)

            // Build the path: album/subfolder
            val relativePath = if (selectedAlbum != null) {
                "Pictures/GQTPhotoApp/$selectedAlbum/$subfolder"
            } else {
                "Pictures/GQTPhotoApp/$subfolder"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                }
            }

            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
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

                    // Return success to MainActivity
                    setResult(RESULT_OK)
                    finish()
                } ?: run {
                    Toast.makeText(this, "Error: Photo not saved", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }
            } else {
                // User cancelled - delete the empty entry and exit
                currentPhotoUri?.let { uri ->
                    contentResolver.delete(uri, null, null)
                }
                Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun getNextPhotoNumber(label: String): Int {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val key = "photo_count_${selectedAlbum}_$label"
        // Return current count + 1 (so first photo is 1, not 0)
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
    }

    private fun getMinCountForLabel(label: String): Int {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val numContainers = sharedPrefs.getInt("num_containers", 0)
        val productTypeName = sharedPrefs.getString("product_type", PhotoLists.ProductType.OCC.name)

        val productType = try {
            PhotoLists.ProductType.valueOf(productTypeName ?: PhotoLists.ProductType.OCC.name)
        } catch (e: Exception) {
            Log.e("CameraActivity", "Error parsing product type, defaulting to OCC", e)
            PhotoLists.ProductType.OCC
        }

        if (numContainers > 0) {
            val categories = PhotoLists.getPhotoCategories(productType, numContainers, selectedAlbum ?: "")
            val category = categories.find { it.label == label }
            return category?.minCount ?: 1
        }
        return 1
    }

    private fun checkIfMultipleExpected(label: String): Boolean {
        return getMinCountForLabel(label) > 1
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