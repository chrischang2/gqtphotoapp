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

class ContainerManagementActivity : AppCompatActivity() {

    private lateinit var containerLayout: LinearLayout
    private var numSampleContainers = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container_management)

        containerLayout = findViewById(R.id.containerLayout)

        // Get number of sample containers
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val numContainers = sharedPrefs.getInt("num_containers", 0)
        val (_, numSample) = PhotoLists.getSampleInfo(numContainers)
        numSampleContainers = numSample

        // Create buttons for each container
        createContainerButtons()
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
        val customName = sharedPrefs.getString("container_${containerNum}_number", null)

        val folderNum = containerNum + 2  // Container 1 → Folder 3, etc.

        return if (customName != null) {
            "${folderNum}. ${customName}"  // CHANGED: Removed "Container_" prefix
        } else {
            "${folderNum}. Container ${containerNum}"  // Default shows "Container 1", etc.
        }
    }

    private fun showRenameDialog(containerNum: Int) {
        val sharedPrefs = getSharedPreferences("GQTPhotoApp", MODE_PRIVATE)
        val currentName = sharedPrefs.getString("container_${containerNum}_number", containerNum.toString()) ?: containerNum.toString()

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(currentName)
            hint = "Enter container number/name"
            setPadding(64, 32, 64, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Rename Container $containerNum Folder")
            .setMessage("Enter new container number or identifier:")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // Save the new name
                    sharedPrefs.edit()
                        .putString("container_${containerNum}_number", newName)
                        .apply()

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
                // Reset to default (container number)
                sharedPrefs.edit()
                    .remove("container_${containerNum}_number")
                    .apply()

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