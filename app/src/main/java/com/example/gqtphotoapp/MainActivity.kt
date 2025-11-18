package com.example.gqtphotoapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var rgProductType: RadioGroup
    private lateinit var etNumContainers: EditText
    private lateinit var tvSampleCode: TextView
    private lateinit var tvSampleContainers: TextView

    private var currentProductType: PhotoLists.ProductType = PhotoLists.ProductType.OCC
    private var numContainers: Int = 0
    private var selectedSampleCode: PhotoLists.SampleCode = PhotoLists.SampleCode.II_A
    private var numSampleContainers: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        rgProductType = findViewById(R.id.rgProductType)
        etNumContainers = findViewById(R.id.etNumContainers)
        tvSampleCode = findViewById(R.id.tvSampleCode)
        tvSampleContainers = findViewById(R.id.tvSampleContainers)

        setupProductTypeSelection()
        setupNumContainersInput()

        // Setup window insets for system bars
        setupWindowInsets()

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

        // Open Camera button - shows photo label selection dialog
        btnOpenCamera.setOnClickListener {
            showPhotoLabelSelectionDialog()
        }
    }

    private fun setupProductTypeSelection() {
        rgProductType.setOnCheckedChangeListener { _, checkedId ->
            currentProductType = when (checkedId) {
                R.id.rbOCC -> PhotoLists.ProductType.OCC
                R.id.rbSPRN -> PhotoLists.ProductType.SPRN
                R.id.rbAluminium -> PhotoLists.ProductType.ALUMINIUM
                else -> PhotoLists.ProductType.OCC
            }
            updateSampleInfo()
        }
    }

    private fun setupNumContainersInput() {
        etNumContainers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                numContainers = if (input.isNotEmpty()) {
                    input.toIntOrNull() ?: 0
                } else {
                    0
                }
                updateSampleInfo()
            }
        })
    }

    private fun updateSampleInfo() {
        if (numContainers > 0) {
            val (sampleCode, numSample) = PhotoLists.getSampleInfo(numContainers)
            selectedSampleCode = sampleCode
            numSampleContainers = numSample

            // Display sample code
            tvSampleCode.text = when (sampleCode) {
                PhotoLists.SampleCode.II_A -> "II-A"
                PhotoLists.SampleCode.II_B -> "II-B"
                PhotoLists.SampleCode.II_C -> "II-C"
                PhotoLists.SampleCode.II_D -> "II-D"
            }

            // Display number of sample containers
            tvSampleContainers.text = numSample.toString()

            // Enable buttons based on valid input
            enableActionButtons(true)
        } else {
            tvSampleCode.text = "--"
            tvSampleContainers.text = "--"
            enableActionButtons(false)
        }
    }

    private fun enableActionButtons(enabled: Boolean) {
        findViewById<Button>(R.id.btnAddAlbum).isEnabled = enabled
        findViewById<Button>(R.id.btnOpenCamera).isEnabled = enabled
    }

    // Helper function to get current photo categories
    private fun getCurrentPhotoCategories(): List<PhotoCategory> {
        return if (numContainers > 0) {
            PhotoLists.getPhotoCategories(currentProductType, numContainers)
        } else {
            emptyList()
        }
    }

    override fun onResume() {
        super.onResume()
        // Update the camera button text to show current album
        updateCameraButtonText()
    }

    private fun setupWindowInsets() {
        // This handles the system bars (status bar and navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showPhotoLabelSelectionDialog() {
        val photoCategories = getCurrentPhotoCategories()

        if (photoCategories.isEmpty()) {
            Toast.makeText(this, "Please enter number of containers first", Toast.LENGTH_SHORT).show()
            return
        }

        // Create custom dialog with spinner
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_photo_label, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerPhotoLabel)

        // Get photo labels
        val photoLabels = photoCategories.map { it.label }

        // Setup spinner adapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, photoLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Select Photo Type")
            .setView(dialogView)
            .setPositiveButton("Take Photo") { _, _ ->
                val selectedPosition = spinner.selectedItemPosition
                val selectedLabel = photoLabels[selectedPosition]
                openCameraWithLabel(selectedLabel)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openCameraWithLabel(photoLabel: String) {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val lastAlbum = sharedPrefs.getString("last_selected_album", null)

        val intent = Intent(this, CameraActivity::class.java).apply {
            lastAlbum?.let {
                putExtra("ALBUM_NAME", it)
            }
            putExtra("PHOTO_LABEL", photoLabel)
        }
        startActivity(intent)
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