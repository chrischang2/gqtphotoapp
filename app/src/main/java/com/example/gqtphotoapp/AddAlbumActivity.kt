package com.example.gqtphotoapp

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddAlbumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_album)

        val etAlbumName = findViewById<EditText>(R.id.etAlbumName)
        val btnCreate = findViewById<Button>(R.id.btnCreateAlbum)
        val btnCancel = findViewById<Button>(R.id.btnCancelAlbum)

        btnCreate.setOnClickListener {
            val albumName = etAlbumName.text.toString().trim()
            if (albumName.isNotEmpty()) {
                if (createAlbum(albumName)) {
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

    private fun createAlbum(albumName: String): Boolean {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val albums = sharedPrefs.getStringSet("albums", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // Check if album already exists
        if (albums.contains(albumName)) {
            return false
        }

        // Add new album
        albums.add(albumName)
        sharedPrefs.edit().putStringSet("albums", albums).apply()

        return true
    }
}