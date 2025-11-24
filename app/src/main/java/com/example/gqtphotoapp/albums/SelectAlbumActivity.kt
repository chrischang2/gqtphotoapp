package com.example.gqtphotoapp.albums

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.gqtphotoapp.R
import com.example.gqtphotoapp.dropbox.DropboxAuthActivity
import com.example.gqtphotoapp.dropbox.DropboxManager
import com.example.gqtphotoapp.dropbox.NetworkMonitor
import com.example.gqtphotoapp.dropbox.DropboxUploadWorker

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
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val albums = sharedPrefs.getStringSet("albums", mutableSetOf())?.toList() ?: emptyList()
        albumsList = albums.toMutableList()
    }

    private fun showCurrentSelection() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val currentAlbum = sharedPrefs.getString("last_selected_album", null)
        if (currentAlbum != null) {
            // Also show the saved material info if it exists
            val materialType = sharedPrefs.getString("${currentAlbum}_material_type", null)
            val containerNum = sharedPrefs.getInt("${currentAlbum}_num_containers", 0)

            val details = if (materialType != null && containerNum > 0) {
                " ($materialType, $containerNum containers)"
            } else {
                ""
            }

            Toast.makeText(this, "Current: $currentAlbum$details", Toast.LENGTH_LONG).show()
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
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
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

        // Remove the material info for this album
        editor.remove("${albumName}_material_type")
        editor.remove("${albumName}_num_containers")

        // Remove all container names for this album (up to a reasonable maximum)
        for (i in 1..50) {
            editor.remove("${albumName}_container_${i}_number")
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
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Get current material type and container number from MainActivity
        val currentMaterialType = sharedPrefs.getString("product_type", null)
        val currentNumContainers = sharedPrefs.getInt("num_containers", 0)

        // Save current album's settings before switching (if they exist)
        val previousAlbum = sharedPrefs.getString("last_selected_album", null)
        if (previousAlbum != null && currentMaterialType != null && currentNumContainers > 0) {
            editor.putString("${previousAlbum}_material_type", currentMaterialType)
            editor.putInt("${previousAlbum}_num_containers", currentNumContainers)

            // Save container names for the previous album
            saveContainerNamesForAlbum(previousAlbum, currentNumContainers)
        }

        if (albumName != null) {
            editor.putString("last_selected_album", albumName)
            editor.putBoolean("album_changed", true)

            // Check if this album has saved settings
            val savedMaterialType = sharedPrefs.getString("${albumName}_material_type", null)
            val savedNumContainers = sharedPrefs.getInt("${albumName}_num_containers", 0)

            if (savedMaterialType != null && savedNumContainers > 0) {
                Toast.makeText(
                    this,
                    "Album '$albumName' selected\nRestoring: $savedMaterialType - $savedNumContainers containers",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "Album '$albumName' selected (no saved settings)", Toast.LENGTH_SHORT).show()
            }
        } else {
            editor.remove("last_selected_album")
            editor.putBoolean("album_changed", true)
            Toast.makeText(this, "Default folder selected for camera", Toast.LENGTH_SHORT).show()
        }

        editor.apply()
        finish()
    }

    private fun saveContainerNamesForAlbum(albumName: String, numContainers: Int) {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val (_, numSample) = com.example.gqtphotoapp.photos.PhotoLists.getSampleInfo(numContainers)

        // Save each container name from the current album-less keys to album-specific keys
        for (i in 1..numSample) {
            val currentKey = "container_${i}_number"
            val albumKey = "${albumName}_container_${i}_number"

            val containerName = sharedPrefs.getString(currentKey, null)
            if (containerName != null) {
                editor.putString(albumKey, containerName)
            } else {
                editor.remove(albumKey)
            }
        }

        editor.apply()
    }
}