package com.example.gqtphotoapp

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.example.gqtphotoapp.photos.PhotoLists

class ContainerManagementActivity : AppCompatActivity() {

    private lateinit var containerLayout: LinearLayout
    private var numSampleContainers = 0
    private var currentAlbum: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container_management)

        containerLayout = findViewById(R.id.containerLayout)

        // Get current album
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        currentAlbum = sharedPrefs.getString("last_selected_album", null)

        // Get number of sample containers
        val numContainers = sharedPrefs.getInt("num_containers", 0)
        val (_, numSample) = PhotoLists.getSampleInfo(numContainers)
        numSampleContainers = numSample

        // IMPORTANT: Load container names from album into working keys before creating buttons
        val album = currentAlbum
        if (album != null) {
            loadContainerNamesFromAlbum(album, numContainers)
        }

        // Create buttons for each container
        createContainerButtons()
    }

    private fun loadContainerNamesFromAlbum(albumName: String, numContainers: Int) {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val (_, numSample) = PhotoLists.getSampleInfo(numContainers)

        // Load container names from album-specific keys to current working keys
        for (i in 1..numSample) {
            val albumKey = "${albumName}_container_${i}_number"
            val workingKey = "container_${i}_number"

            val containerName = sharedPrefs.getString(albumKey, null)
            if (containerName != null) {
                editor.putString(workingKey, containerName)
            } else {
                // Clear the working key if album has no saved name
                editor.remove(workingKey)
            }
        }

        editor.apply()
    }

    private fun createContainerButtons() {
        containerLayout.removeAllViews()

        for (i in 1..numSampleContainers) {
            val button = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
                setPadding(32)
                textSize = 16f
                text = getContainerFolderName(i)

                setOnClickListener {
                    showRenameDialog(i)
                }
            }

            containerLayout.addView(button)
        }
    }

    private fun getContainerFolderName(containerNum: Int): String {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)

        // Always read from working keys (they're loaded from album in onCreate)
        val customName = sharedPrefs.getString("container_${containerNum}_number", null)
        val folderNum = containerNum + 2  // Container 1 → Folder 3, etc.

        return if (customName != null) {
            "${folderNum}. ${customName}"
        } else {
            "${folderNum}. Container ${containerNum}"
        }
    }

    private fun showRenameDialog(containerNum: Int) {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)

        // Use working keys consistently
        val key = "container_${containerNum}_number"
        val currentName = sharedPrefs.getString(key, containerNum.toString()) ?: containerNum.toString()

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(currentName)
            hint = "Enter container number/name"
            setPadding(64, 32, 64, 32)
        }

        val dialogTitle = if (currentAlbum != null) {
            "Rename Container $containerNum Folder ($currentAlbum)"
        } else {
            "Rename Container $containerNum Folder"
        }

        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setMessage("Enter new container number or identifier:")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val editor = sharedPrefs.edit()

                    // Save to working key
                    editor.putString(key, newName)

                    // Also save directly to album key if album exists
                    if (currentAlbum != null) {
                        editor.putString("${currentAlbum}_$key", newName)
                    }

                    editor.apply()

                    // Refresh the buttons to show updated names
                    createContainerButtons()

                    Toast.makeText(
                        this,
                        "Container $containerNum folder renamed",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Name cannot be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Reset to Default") { dialog, _ ->
                val editor = sharedPrefs.edit()

                // Remove from working key
                editor.remove(key)

                // Also remove from album key if album exists
                if (currentAlbum != null) {
                    editor.remove("${currentAlbum}_$key")
                }

                editor.apply()

                createContainerButtons()

                Toast.makeText(
                    this,
                    "Container $containerNum folder reset to default",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .show()
    }
}