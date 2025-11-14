package com.example.gqtphotoapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class AddAlbumActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_album)

        // Check permissions
        checkPermissions()

        val etAlbumName = findViewById<EditText>(R.id.etAlbumName)
        val btnCreate = findViewById<Button>(R.id.btnCreateAlbum)
        val btnCancel = findViewById<Button>(R.id.btnCancelAlbum)

        btnCreate.setOnClickListener {
            val albumName = etAlbumName.text.toString().trim()
            if (albumName.isNotEmpty()) {
                if (createAlbumFolder(albumName)) {
                    Toast.makeText(this, "Album '$albumName' created!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Album '$albumName' already exists!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Album name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - Request READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            // Android 12 and below
            val permissions = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun createAlbumFolder(albumName: String): Boolean {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val albums = sharedPrefs.getStringSet("albums", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // Check if album already exists
        if (albums.contains(albumName)) {
            return false
        }

        // Add new album to SharedPreferences
        albums.add(albumName)
        sharedPrefs.edit().putStringSet("albums", albums).apply()

        // Create folder in app-specific directory (visible in gallery)
        createMediaStoreFolder(albumName)

        return true
    }

    private fun createMediaStoreFolder(albumName: String) {
        // For Android 10+, create a folder reference that will be used when saving images
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // The folder will be created automatically when you save the first image
            // Store the relative path for later use
            val relativePath = "${Environment.DIRECTORY_PICTURES}/GQTPhotoApp/$albumName"

            // Save this path for use when capturing photos
            val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("album_path_$albumName", relativePath).apply()
        } else {
            // For older Android versions, create actual directory
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val albumDir = File(picturesDir, "GQTPhotoApp/$albumName")
            if (!albumDir.exists()) {
                albumDir.mkdirs()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied - some features may not work", Toast.LENGTH_SHORT).show()
            }
        }
    }
}