package com.example.gqtphotoapp

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ViewAlbumsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var albumAdapter: AlbumAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_albums)

        recyclerView = findViewById(R.id.recyclerViewAlbums)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadAlbums()
    }

    override fun onResume() {
        super.onResume()
        loadAlbums() // Refresh albums when returning to this activity
    }

    private fun loadAlbums() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val albums = sharedPrefs.getStringSet("albums", setOf())?.toList() ?: emptyList()

        if (albums.isEmpty()) {
            Toast.makeText(this, "No albums yet. Create one!", Toast.LENGTH_SHORT).show()
        }

        albumAdapter = AlbumAdapter(albums) { albumName ->
            // Handle album click - will open photos in that album
            Toast.makeText(this, "Clicked on: $albumName", Toast.LENGTH_SHORT).show()
            // TODO: Open album photos view
        }
        recyclerView.adapter = albumAdapter
    }
}