package com.example.gqtphotoapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
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

        // Open Camera button - shows dialog to choose mode
        btnOpenCamera.setOnClickListener {
            checkAndResumeSequence()
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

    // Single photo mode (your original behavior)
    private fun openCameraSingle() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val lastAlbum = sharedPrefs.getString("last_selected_album", null)

        val intent = Intent(this, CameraActivity::class.java)
        lastAlbum?.let {
            intent.putExtra("ALBUM_NAME", it)
        }
        startActivity(intent)
    }

    // Sequence mode (new)
    private fun openCameraSequence() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val lastAlbum = sharedPrefs.getString("last_selected_album", null)

        // Get the photo categories based on current settings
        val photoCategories = getCurrentPhotoCategories()
        val photoLabels = photoCategories.map { it.label }

        val intent = Intent(this, CameraActivity::class.java).apply {
            lastAlbum?.let {
                putExtra("ALBUM_NAME", it)
            }
            putExtra("SEQUENCE_MODE", true)
            putExtra("CURRENT_NUMBER", 1)
            putExtra("PHOTO_LABELS", photoLabels.toTypedArray())
        }
        startActivity(intent)

        val sampleCodeStr = when (selectedSampleCode) {
            PhotoLists.SampleCode.II_A -> "II-A"
            PhotoLists.SampleCode.II_B -> "II-B"
            PhotoLists.SampleCode.II_C -> "II-C"
            PhotoLists.SampleCode.II_D -> "II-D"
        }

        Toast.makeText(
            this,
            "Starting sequence: $sampleCodeStr (${photoLabels.size} photos)",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkAndResumeSequence() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        val sequenceInProgress = sharedPrefs.getBoolean("sequence_in_progress", false)

        if (sequenceInProgress) {
            val currentNumber = sharedPrefs.getInt("sequence_current_number", 1)
            val sequenceAlbum = sharedPrefs.getString("sequence_album", null)

            // Get saved photo labels or use current selection
            val savedLabelsSet = sharedPrefs.getStringSet("sequence_photo_labels", null)
            val photoLabels = if (savedLabelsSet != null) {
                savedLabelsSet.toList()
            } else {
                getCurrentPhotoCategories().map { it.label }
            }

            // Show dialog to resume or start new
            android.app.AlertDialog.Builder(this)
                .setTitle("Resume Sequence?")
                .setMessage("You have a sequence in progress: Photo $currentNumber of ${photoLabels.size}\n\nWhat would you like to do?")
                .setPositiveButton("Resume") { _, _ ->
                    resumeSequence(currentNumber, photoLabels, sequenceAlbum)
                }
                .setNegativeButton("Start New") { _, _ ->
                    // Clear old sequence and show camera mode dialog
                    clearSequenceProgress()
                    showCameraModeDialog()
                }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            // No sequence in progress, show normal camera mode dialog
            showCameraModeDialog()
        }
    }

    private fun showCameraModeDialog() {
        val photoCategories = getCurrentPhotoCategories()
        val photoCount = photoCategories.size

        val sampleCodeStr = when (selectedSampleCode) {
            PhotoLists.SampleCode.II_A -> "II-A"
            PhotoLists.SampleCode.II_B -> "II-B"
            PhotoLists.SampleCode.II_C -> "II-C"
            PhotoLists.SampleCode.II_D -> "II-D"
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Camera Mode")
            .setMessage("Choose camera mode:")
            .setPositiveButton("Single Photo") { _, _ ->
                openCameraSingle()
            }
            .setNegativeButton("Sequence ($sampleCodeStr: $photoCount photos)") { _, _ ->
                openCameraSequence()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun resumeSequence(currentNumber: Int, photoLabels: List<String>, albumName: String?) {
        val intent = Intent(this, CameraActivity::class.java).apply {
            albumName?.let {
                putExtra("ALBUM_NAME", it)
            }
            putExtra("SEQUENCE_MODE", true)
            putExtra("CURRENT_NUMBER", currentNumber)
            putExtra("PHOTO_LABELS", photoLabels.toTypedArray())
        }
        startActivity(intent)
    }

    private fun clearSequenceProgress() {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            remove("sequence_in_progress")
            remove("sequence_current_number")
            remove("sequence_album")
            remove("sequence_photo_labels")
            apply()
        }
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