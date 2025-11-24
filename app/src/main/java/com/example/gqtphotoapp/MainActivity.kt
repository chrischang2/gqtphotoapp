package com.example.gqtphotoapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gqtphotoapp.albums.AddAlbumActivity
import com.example.gqtphotoapp.albums.SelectAlbumActivity
import com.example.gqtphotoapp.albums.ViewAlbumsActivity
import com.example.gqtphotoapp.camera.CameraActivity
import com.example.gqtphotoapp.photos.PhotoCategory
import com.example.gqtphotoapp.photos.PhotoLabelAdapter
import com.example.gqtphotoapp.photos.PhotoLists
import com.example.gqtphotoapp.photos.ViewPhotosActivity
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.gqtphotoapp.dropbox.DropboxAuthActivity
import com.example.gqtphotoapp.dropbox.DropboxManager
import com.example.gqtphotoapp.dropbox.NetworkMonitor
import com.example.gqtphotoapp.dropbox.DropboxUploadWorker

class MainActivity : AppCompatActivity() {

    private lateinit var rgProductType: RadioGroup
    private lateinit var etNumContainers: EditText
    private lateinit var tvSampleCode: TextView
    private lateinit var tvSampleContainers: TextView

    private var currentProductType: PhotoLists.ProductType = PhotoLists.ProductType.OCC
    private var numContainers: Int = 0
    private var selectedSampleCode: PhotoLists.SampleCode = PhotoLists.SampleCode.II_A
    private var numSampleContainers: Int = 0

    // For sequential photo capture
    private var currentPhotoLabel: String? = null

    companion object {
        private const val PREFS_NAME = "GQTPhotoApp"
        private const val KEY_PRODUCT_TYPE = "product_type"
        private const val KEY_NUM_CONTAINERS = "num_containers"
        private const val KEY_LAST_ALBUM = "last_selected_album"
        private const val KEY_LAST_PHOTO_LABEL = "last_photo_label"
        private const val CAMERA_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        rgProductType = findViewById(R.id.rgProductType)
        etNumContainers = findViewById(R.id.etNumContainers)
        tvSampleCode = findViewById(R.id.tvSampleCode)
        tvSampleContainers = findViewById(R.id.tvSampleContainers)

        // Restore saved values
        restoreSavedValues()

        setupProductTypeSelection()
        setupNumContainersInput()

        // Setup window insets for system bars
        setupWindowInsets()

        val btnAddAlbum = findViewById<Button>(R.id.btnAddAlbum)
        val btnChangeAlbum = findViewById<Button>(R.id.btnChangeAlbum)
        val btnOpenCamera = findViewById<Button>(R.id.btnOpenCamera)
        val btnViewPhotos = findViewById<Button>(R.id.btnViewPhotos)
        val btnViewAlbums = findViewById<Button>(R.id.btnViewAlbums)
        val btnManageContainers = findViewById<Button>(R.id.btnManageContainers)

        // In onCreate(), after the other button declarations:
        val btnDropboxSettings = findViewById<Button>(R.id.btnDropboxSettings)
        val btnUploadToDropbox = findViewById<Button>(R.id.btnUploadToDropbox)

        // Add these click listeners after the other button listeners:
        btnDropboxSettings.setOnClickListener {
            val intent = Intent(this, DropboxAuthActivity::class.java)
            startActivity(intent)
        }

        btnUploadToDropbox.setOnClickListener {
            showUploadDialog()
        }

        // Add Album button - now opens new activity
        btnAddAlbum.setOnClickListener {
            val intent = Intent(this, AddAlbumActivity::class.java)
            startActivity(intent)
        }

        btnViewPhotos.setOnClickListener {
            viewPhotos()
        }

        // View Albums button - now functional!
        btnViewAlbums.setOnClickListener {
            val intent = Intent(this, ViewAlbumsActivity::class.java)
            startActivity(intent)
        }

        btnChangeAlbum.setOnClickListener {
            val intent = Intent(this, SelectAlbumActivity::class.java)
            startActivity(intent)
        }

        // Open Camera button - shows photo label selection dialog
        btnOpenCamera.setOnClickListener {
            showPhotoLabelSelectionDialog()
        }

        // Manage Container Folders button
        btnManageContainers.setOnClickListener {
            val intent = Intent(this, ContainerManagementActivity::class.java)
            startActivity(intent)
        }
    }

    private fun restoreSavedValues() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Restore product type
        val savedProductType = sharedPrefs.getString(KEY_PRODUCT_TYPE, PhotoLists.ProductType.OCC.name)
        currentProductType = PhotoLists.ProductType.valueOf(savedProductType ?: PhotoLists.ProductType.OCC.name)

        // Set the radio button based on saved product type
        val radioButtonId = when (currentProductType) {
            PhotoLists.ProductType.OCC -> R.id.rbOCC
            PhotoLists.ProductType.SPRN -> R.id.rbSPRN
            PhotoLists.ProductType.ALUMINIUM -> R.id.rbAluminium
        }
        rgProductType.check(radioButtonId)

        // Restore number of containers
        numContainers = sharedPrefs.getInt(KEY_NUM_CONTAINERS, 0)
        if (numContainers > 0) {
            etNumContainers.setText(numContainers.toString())
        }

        // Update sample info with restored values
        updateSampleInfo()
    }

    private fun saveCurrentValues() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString(KEY_PRODUCT_TYPE, currentProductType.name)
            putInt(KEY_NUM_CONTAINERS, numContainers)
            apply()
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
            saveCurrentValues()
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
                saveCurrentValues()
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
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val albumName = sharedPrefs.getString(KEY_LAST_ALBUM, "") ?: ""

        return if (numContainers > 0) {
            PhotoLists.getPhotoCategories(currentProductType, numContainers, albumName)
        } else {
            emptyList()
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if album was changed
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val albumChangedFlag = sharedPrefs.getBoolean("album_changed", false)

        if (albumChangedFlag) {
            // Restore material info for the new album (including container names)
            restoreAlbumMaterialInfo()

            // Clear the album_changed flag and last_photo_label
            sharedPrefs.edit().apply {
                putBoolean("album_changed", false)
                remove(KEY_LAST_PHOTO_LABEL)
                apply()
            }
        } else {
            // Even if album didn't change, ensure container names are in sync
            // This handles the case where user renamed containers and came back to MainActivity
            val currentAlbum = sharedPrefs.getString(KEY_LAST_ALBUM, null)
            val numContainers = sharedPrefs.getInt(KEY_NUM_CONTAINERS, 0)
            if (currentAlbum != null && numContainers > 0) {
                restoreContainerNamesForAlbum(currentAlbum, numContainers)
            }
        }

        // Update the camera button text to show current album
        updateCameraButtonText()
    }

    private fun checkAlbumChange() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val albumChangedFlag = sharedPrefs.getBoolean("album_changed", false)

        if (albumChangedFlag) {
            // Clear the product type and containers
            clearInputValues()

            // Also clear the last photo label
            sharedPrefs.edit().apply {
                remove(KEY_LAST_PHOTO_LABEL)
                putBoolean("album_changed", false)
                apply()
            }
        }
    }

    private fun clearInputValues() {
        // Clear SharedPreferences
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            remove(KEY_PRODUCT_TYPE)
            remove(KEY_NUM_CONTAINERS)
            apply()
        }

        // Reset UI
        rgProductType.check(R.id.rbOCC)
        currentProductType = PhotoLists.ProductType.OCC
        etNumContainers.setText("")
        numContainers = 0

        // Update sample info
        updateSampleInfo()
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

        // Get the last selected photo label
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastPhotoLabel = sharedPrefs.getString(KEY_LAST_PHOTO_LABEL, null)

        // Find the index of the last selected label
        var defaultSelection = 0
        if (lastPhotoLabel != null) {
            val lastIndex = photoLabels.indexOf(lastPhotoLabel)
            if (lastIndex >= 0) {
                defaultSelection = lastIndex
            }
        }

        // Get current photo counts for all labels
        val photoCounts = getPhotoCountsForLabels(photoLabels)

        // Setup custom spinner adapter with color coding
        val adapter = PhotoLabelAdapter(
            this,
            photoCategories,
            photoCounts,
            sharedPrefs.getString(KEY_LAST_ALBUM, null)
        )
        spinner.adapter = adapter

        // Set selection after adapter is set
        spinner.setSelection(defaultSelection)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Photo Type")
            .setView(dialogView)
            .setPositiveButton("Take Photo") { _, _ ->
                val selectedPosition = spinner.selectedItemPosition
                val selectedLabel = photoLabels[selectedPosition]

                // Save the selected photo label
                sharedPrefs.edit().putString(KEY_LAST_PHOTO_LABEL, selectedLabel).apply()

                // Store current photo label for sequential capture
                currentPhotoLabel = selectedLabel

                // Launch camera with startActivityForResult
                openCameraWithLabel(selectedLabel)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun getPhotoCountsForLabels(labels: List<String>): Map<String, Int> {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val selectedAlbum = sharedPrefs.getString(KEY_LAST_ALBUM, null)

        return labels.associateWith { label ->
            val key = "photo_count_${selectedAlbum}_$label"
            sharedPrefs.getInt(key, 0)
        }
    }

    private fun openCameraWithLabel(photoLabel: String) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastAlbum = sharedPrefs.getString(KEY_LAST_ALBUM, null)

        val intent = Intent(this, CameraActivity::class.java).apply {
            lastAlbum?.let {
                putExtra("ALBUM_NAME", it)
            }
            putExtra("PHOTO_LABEL", photoLabel)
        }
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Photo was taken successfully, automatically open camera again
                currentPhotoLabel?.let { label ->
                    openCameraWithLabel(label)
                }
            } else {
                // User cancelled - stop the sequential capture
                currentPhotoLabel = null
                Toast.makeText(this, "Photo capture stopped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun viewPhotos() {
        val intent = Intent(this, ViewPhotosActivity::class.java)
        startActivity(intent)
    }

    private fun updateCameraButtonText() {
        val btnOpenCamera = findViewById<Button>(R.id.btnOpenCamera)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastAlbum = sharedPrefs.getString(KEY_LAST_ALBUM, null)

        btnOpenCamera.text = if (lastAlbum != null) {
            "Camera → $lastAlbum"
        } else {
            "Open Camera"
        }
    }

    private fun restoreAlbumMaterialInfo() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentAlbum = sharedPrefs.getString(KEY_LAST_ALBUM, null)

        if (currentAlbum != null) {
            // Try to restore material type and containers for this specific album
            val albumMaterialType = sharedPrefs.getString("${currentAlbum}_material_type", null)
            val albumNumContainers = sharedPrefs.getInt("${currentAlbum}_num_containers", 0)

            if (albumMaterialType != null && albumNumContainers > 0) {
                // Album has saved settings - restore them
                try {
                    currentProductType = PhotoLists.ProductType.valueOf(albumMaterialType)
                    val radioButtonId = when (currentProductType) {
                        PhotoLists.ProductType.OCC -> R.id.rbOCC
                        PhotoLists.ProductType.SPRN -> R.id.rbSPRN
                        PhotoLists.ProductType.ALUMINIUM -> R.id.rbAluminium
                    }
                    rgProductType.check(radioButtonId)
                } catch (e: IllegalArgumentException) {
                    // If invalid product type, use default
                    currentProductType = PhotoLists.ProductType.OCC
                    rgProductType.check(R.id.rbOCC)
                }

                // Restore the number of containers
                numContainers = albumNumContainers
                etNumContainers.setText(numContainers.toString())

                // Restore container names for this album
                restoreContainerNamesForAlbum(currentAlbum, albumNumContainers)

                // Update sample info
                updateSampleInfo()

                // Save these as current values
                saveCurrentValues()

                Toast.makeText(
                    this,
                    "Restored: $albumMaterialType - $albumNumContainers containers",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Album has no saved settings - clear everything
                clearInputValues()
                clearContainerNames()
                Toast.makeText(
                    this,
                    "Album '$currentAlbum' has no saved settings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // No album selected - clear everything
            clearInputValues()
            clearContainerNames()
        }
    }

    private fun restoreContainerNamesForAlbum(albumName: String, numContainers: Int) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val (_, numSample) = PhotoLists.getSampleInfo(numContainers)

        // Load container names from album-specific keys to current keys
        for (i in 1..numSample) {
            val albumKey = "${albumName}_container_${i}_number"
            val currentKey = "container_${i}_number"

            val containerName = sharedPrefs.getString(albumKey, null)
            if (containerName != null) {
                editor.putString(currentKey, containerName)
            } else {
                editor.remove(currentKey)
            }
        }

        editor.apply()
    }

    private fun clearContainerNames() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Clear all current container names (up to a reasonable maximum)
        for (i in 1..50) {
            editor.remove("container_${i}_number")
        }

        editor.apply()
    }

    private fun showUploadDialog() {
        // Load albums from shared preferences
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val albums = sharedPrefs.getStringSet("albums", mutableSetOf())?.toList() ?: emptyList()

        if (albums.isEmpty()) {
            Toast.makeText(this, "No albums to upload", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select an album to upload:")
            .setItems(albums.toTypedArray()) { _, which ->
                val albumToUpload = albums[which]
                uploadAlbumToDropbox(albumToUpload)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadAlbumToDropbox(albumName: String) {
        val dropboxManager = DropboxManager.getInstance(this)

        // Check if authenticated
        if (!dropboxManager.isAuthenticated()) {
            AlertDialog.Builder(this)
                .setTitle("Connect to Dropbox")
                .setMessage("You need to connect to Dropbox first. Go to Dropbox settings?")
                .setPositiveButton("Yes") { _, _ ->
                    val intent = Intent(this, DropboxAuthActivity::class.java)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        // Check WiFi
        val networkMonitor = NetworkMonitor(this)
        if (!networkMonitor.isWiFiConnected()) {
            AlertDialog.Builder(this)
                .setTitle("WiFi Required")
                .setMessage("Upload requires WiFi connection. The upload will start automatically when you connect to WiFi.")
                .setPositiveButton("OK") { _, _ ->
                    scheduleUpload(albumName)
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        // Schedule upload
        scheduleUpload(albumName)
    }

    private fun scheduleUpload(albumName: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<DropboxUploadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(DropboxUploadWorker.KEY_ALBUM_NAME to albumName))
            .build()

        WorkManager.getInstance(this).enqueue(uploadWorkRequest)

        Toast.makeText(this, "Upload queued for '$albumName'", Toast.LENGTH_SHORT).show()

        // Observe progress
        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(uploadWorkRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val uploaded = workInfo.progress.getInt(DropboxUploadWorker.KEY_UPLOADED_COUNT, 0)
                            val total = workInfo.progress.getInt(DropboxUploadWorker.KEY_TOTAL_COUNT, 0)
                            Toast.makeText(this, "Uploading: $uploaded/$total", Toast.LENGTH_SHORT).show()
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val uploaded = workInfo.outputData.getInt(DropboxUploadWorker.KEY_UPLOADED_COUNT, 0)
                            val total = workInfo.outputData.getInt(DropboxUploadWorker.KEY_TOTAL_COUNT, 0)
                            Toast.makeText(this, "Upload complete! $uploaded/$total photos uploaded", Toast.LENGTH_LONG).show()
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString("error")
                            Toast.makeText(this, "Upload failed: $error", Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }
            }
    }
}