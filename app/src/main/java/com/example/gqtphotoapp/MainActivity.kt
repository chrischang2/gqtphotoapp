package com.example.gqtphotoapp

import com.example.gqtphotoapp.R
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the button by its ID
        val btnOpenCamera = findViewById<Button>(R.id.btnOpenCamera)
        val btnViewPhotos = findViewById<Button>(R.id.btnViewPhotos)

        // Set up click listener
        btnOpenCamera.setOnClickListener {
            openCamera()
        }

        btnViewPhotos.setOnClickListener {
            viewPhotos()
        }
    }

    // button functions
    private fun openCamera() {
        // Create an intent to open the camera
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Check if there's a camera app available
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivity(cameraIntent)
        } else {
            // Show a message if no camera app is found
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun viewPhotos() {
        val intent = Intent(this, ViewPhotosActivity::class.java)
        startActivity(intent)
    }
}

