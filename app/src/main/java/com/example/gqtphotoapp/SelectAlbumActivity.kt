package com.example.gqtphotoapp

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SelectAlbumActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var albumsList: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_album)

        val listView = findViewById<ListView>(R.id.lvAlbums)
        val btnNoAlbum = findViewById<Button>(R.id.btnNoAlbum)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnDeleteAlbum = findViewById<Button>(R.id.btnDeleteAlbum)

        // Load albums
        loadAlbums()

        if (albumsList.isEmpty()) {
            Toast.makeText(this, "No albums created yet. Create an album first!", Toast.LENGTH_LONG).show()
        }

        // Show current selection
        showCurrentSelection()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, albumsList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedAlbum = albumsList[position]
            saveAlbumSelection(selectedAlbum)
        }

        btnNoAlbum.setOnClickListener {
            saveAlbumSelection(null)
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnDeleteAlbum.setOnClickListener {
            showDeleteDialog()
        }
    }

    private fun loadAlbums() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val albums = sharedPrefs.getStringSet("albums", mutableSetOf())?.toList() ?: emptyList()
        albumsList = albums.toMutableList()
    }

    private fun showCurrentSelection() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val currentAlbum = sharedPrefs.getString("last_selected_album", null)
        if (currentAlbum != null) {
            Toast.makeText(this, "Current: $currentAlbum", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Current: Default Folder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteDialog() {
        if (albumsList.isEmpty()) {
            Toast.makeText(this, "No albums to delete", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Album")
        builder.setItems(albumsList.toTypedArray()) { dialog, which ->
            val albumToDelete = albumsList[which]
            confirmDelete(albumToDelete)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun confirmDelete(albumName: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Delete")
        builder.setMessage("Delete album '$albumName'?\n\nNote: Photos will remain in your gallery, only the album organization will be removed.")
        builder.setPositiveButton("Delete") { _, _ ->
            deleteAlbum(albumName)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun deleteAlbum(albumName: String) {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Remove album from the set
        val albums = sharedPrefs.getStringSet("albums", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        albums.remove(albumName)
        editor.putStringSet("albums", albums)

        // If this was the selected album, clear the selection
        val currentAlbum = sharedPrefs.getString("last_selected_album", null)
        if (currentAlbum == albumName) {
            editor.remove("last_selected_album")
            editor.putBoolean("album_changed", true)
        }

        editor.apply()

        // Update the list and adapter
        albumsList.remove(albumName)
        adapter.notifyDataSetChanged()

        Toast.makeText(this, "Album '$albumName' deleted", Toast.LENGTH_SHORT).show()

        if (albumsList.isEmpty()) {
            Toast.makeText(this, "No albums remaining", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAlbumSelection(albumName: String?) {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        if (albumName != null) {
            editor.putString("last_selected_album", albumName)
            editor.putBoolean("album_changed", true)
            Toast.makeText(this, "Album '$albumName' selected for camera", Toast.LENGTH_SHORT).show()
        } else {
            editor.remove("last_selected_album")
            editor.putBoolean("album_changed", true)
            Toast.makeText(this, "Default folder selected for camera", Toast.LENGTH_SHORT).show()
        }

        editor.apply()
        finish()
    }
}