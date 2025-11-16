package com.example.gqtphotoapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gqtphotoapp.CameraActivity.Companion.PHOTO_LABELS

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAddAlbum = findViewById<Button>(R.id.btnAddAlbum)
        val btnChangeAlbum = findViewById<Button>(R.id.btnChangeAlbum)
        val btnOpenCamera = findViewById<Button>(R.id.btnOpenCamera)
        val btnViewPhotos = findViewById<Button>(R.id.btnViewPhotos)
        val btnLabelPhoto = findViewById<Button>(R.id.btnLabelPhoto)
        val btnViewAlbums = findViewById<Button>(R.id.btnViewAlbums)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        // Add Album button - now opens new activity
        btnAddAlbum.setOnClickListener {
            val intent = Intent(this, AddAlbumActivity::class.java)
            startActivity(intent)
        }

        btnOpenCamera.setOnClickListener {
            // Show dialog to choose mode
            android.app.AlertDialog.Builder(this)
                .setTitle("Camera Mode")
                .setMessage("Choose camera mode:")
                .setPositiveButton("Single Photo") { _, _ ->
                    openCamera()
                }
                .setNegativeButton("Sequence (1-${CameraActivity.PHOTO_LABELS.size})") { _, _ ->
                    openCameraSequence()
                }
                .setNeutralButton("Cancel", null)
                .show()
        }

        btnViewPhotos.setOnClickListener {
            viewPhotos()
        }

        btnLabelPhoto.setOnClickListener {
            Toast.makeText(this, "Label Photo - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // View Albums button - now functional!
        btnViewAlbums.setOnClickListener {
            val intent = Intent(this, ViewAlbumsActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnChangeAlbum.setOnClickListener {
            val intent = Intent(this, SelectAlbumActivity::class.java)
            startActivity(intent)
        }

        // Open Camera button - shows dialog to choose mode
        btnOpenCamera.setOnClickListener {
            checkAndResumeSequence()
        }
    }

    override fun onResume() {
        super.onResume()
        // Update the camera button text to show current album
        updateCameraButtonText()
    }

    private fun openCamera() {
        // Get last selected album
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val lastAlbum = sharedPrefs.getString("last_selected_album", null)

        val intent = Intent(this, CameraActivity::class.java)
        lastAlbum?.let {
            intent.putExtra("ALBUM_NAME", it)
        }
        startActivity(intent)
    }

    // Single photo mode (your original behavior)
    private fun openCameraSingle() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val lastAlbum = sharedPrefs.getString("last_selected_album", null)

        val intent = Intent(this, CameraActivity::class.java)
        lastAlbum?.let {
            intent.putExtra("ALBUM_NAME", it)
        }
        startActivity(intent)
    }

    // Sequence mode (new)
    private fun openCameraSequence() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val lastAlbum = sharedPrefs.getString("last_selected_album", null)

        val intent = Intent(this, CameraActivity::class.java).apply {
            lastAlbum?.let {
                putExtra("ALBUM_NAME", it)
            }
            putExtra("SEQUENCE_MODE", true)
            putExtra("CURRENT_NUMBER", 1)
            putExtra("TOTAL_PHOTOS", CameraActivity.PHOTO_LABELS.size)
        }
        startActivity(intent)
    }

    private fun checkAndResumeSequence() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val sequenceInProgress = sharedPrefs.getBoolean("sequence_in_progress", false)

        if (sequenceInProgress) {
            val currentNumber = sharedPrefs.getInt("sequence_current_number", 1)
            val totalPhotos = sharedPrefs.getInt("sequence_total_photos", CameraActivity.PHOTO_LABELS.size)
            val sequenceAlbum = sharedPrefs.getString("sequence_album", null)

            // Show dialog to resume or start new
            android.app.AlertDialog.Builder(this)
                .setTitle("Resume Sequence?")
                .setMessage("You have a sequence in progress: Photo $currentNumber of ${CameraActivity.PHOTO_LABELS.size}\n\nWhat would you like to do?")
                .setPositiveButton("Resume") { _, _ ->
                    resumeSequence(currentNumber, totalPhotos, sequenceAlbum)
                }
                .setNegativeButton("Start New") { _, _ ->
                    // Clear old sequence and show camera mode dialog
                    clearSequenceProgress()
                    showCameraModeDialog()
                }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            // No sequence in progress, show normal camera mode dialog
            showCameraModeDialog()
        }
    }

    private fun showCameraModeDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Camera Mode")
            .setMessage("Choose camera mode:")
            .setPositiveButton("Single Photo") { _, _ ->
                openCameraSingle()
            }
            .setNegativeButton("Sequence (1-${CameraActivity.PHOTO_LABELS.size})") { _, _ ->
                openCameraSequence()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun resumeSequence(currentNumber: Int, totalPhotos: Int, albumName: String?) {
        val intent = Intent(this, CameraActivity::class.java).apply {
            albumName?.let {
                putExtra("ALBUM_NAME", it)
            }
            putExtra("SEQUENCE_MODE", true)
            putExtra("CURRENT_NUMBER", currentNumber)
            putExtra("TOTAL_PHOTOS", totalPhotos)
        }
        startActivity(intent)
    }

    private fun clearSequenceProgress() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            remove("sequence_in_progress")
            remove("sequence_current_number")
            remove("sequence_total_photos")
            remove("sequence_album")
            apply()
        }
    }

    private fun viewPhotos() {
        val intent = Intent(this, ViewPhotosActivity::class.java)
        startActivity(intent)
    }

    private fun updateCameraButtonText() {
        val btnOpenCamera = findViewById<Button>(R.id.btnOpenCamera)
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val lastAlbum = sharedPrefs.getString("last_selected_album", null)

        btnOpenCamera.text = if (lastAlbum != null) {
            "Camera → $lastAlbum"
        } else {
            "Open Camera"
        }
    }

}