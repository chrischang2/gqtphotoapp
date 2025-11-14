package com.example.gqtphotoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAddAlbum = findViewById<Button>(R.id.btnAddAlbum)
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
            openCamera()
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
    }

    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun viewPhotos() {
        val intent = Intent(this, ViewPhotosActivity::class.java)
        startActivity(intent)
    }
}