package com.example.gqtphotoapp.albums

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gqtphotoapp.R
import com.example.gqtphotoapp.photos.ViewPhotosActivity

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
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val albums = sharedPrefs.getStringSet("albums", setOf())?.toList() ?: emptyList()

        if (albums.isEmpty()) {
            Toast.makeText(this, "No albums yet. Create one!", Toast.LENGTH_SHORT).show()
        }

        albumAdapter = AlbumAdapter(albums) { albumName ->
            // Handle album click - will open photos in that album
            val intent = Intent(this, ViewPhotosActivity::class.java)
            intent.putExtra("ALBUM_NAME", albumName)
            startActivity(intent)
        }
        recyclerView.adapter = albumAdapter
    }
}