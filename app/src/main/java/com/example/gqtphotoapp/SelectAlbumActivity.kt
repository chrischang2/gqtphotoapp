package com.example.gqtphotoapp

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SelectAlbumActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_album)

        val listView = findViewById<ListView>(R.id.lvAlbums)
        val btnNoAlbum = findViewById<Button>(R.id.btnNoAlbum)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        // Load albums
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val albums = sharedPrefs.getStringSet("albums", mutableSetOf())?.toList() ?: emptyList()

        if (albums.isEmpty()) {
            Toast.makeText(this, "No albums created yet. Create an album first!", Toast.LENGTH_LONG).show()
        }

        // Show current selection
        val currentAlbum = sharedPrefs.getString("last_selected_album", null)
        if (currentAlbum != null) {
            Toast.makeText(this, "Current: $currentAlbum", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Current: Default Folder", Toast.LENGTH_SHORT).show()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, albums)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedAlbum = albums[position]
            saveAlbumSelection(selectedAlbum)
        }

        btnNoAlbum.setOnClickListener {
            saveAlbumSelection(null)
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveAlbumSelection(albumName: String?) {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        if (albumName != null) {
            editor.putString("last_selected_album", albumName)
            editor.putBoolean("album_changed", true)  // Set flag to clear MainActivity values
            Toast.makeText(this, "Album '$albumName' selected for camera", Toast.LENGTH_SHORT).show()
        } else {
            editor.remove("last_selected_album")
            editor.putBoolean("album_changed", true)  // Set flag to clear MainActivity values
            Toast.makeText(this, "Default folder selected for camera", Toast.LENGTH_SHORT).show()
        }

        editor.apply()
        finish() // Close the selection screen
    }
}