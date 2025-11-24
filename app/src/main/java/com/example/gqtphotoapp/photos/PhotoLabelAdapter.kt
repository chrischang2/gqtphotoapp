package com.example.gqtphotoapp.photos

import android.R
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class PhotoLabelAdapter(
    context: Context,
    private val photoCategories: List<PhotoCategory>,
    private val photoCounts: Map<String, Int>,
    private val selectedAlbum: String?
) : ArrayAdapter<String>(context, R.layout.simple_spinner_item, photoCategories.map { it.label }) {

    init {
        setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val category = photoCategories[position]
        val currentCount = photoCounts[category.label] ?: 0

        // Format: "Label (x/mincount)" for the selected item display
        view.text = formatLabelWithCount(category, currentCount)
        setLabelColor(view, position)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        val category = photoCategories[position]
        val currentCount = photoCounts[category.label] ?: 0

        // Format: "Label (x/mincount)" for dropdown items
        view.text = formatLabelWithCount(category, currentCount)
        setLabelColor(view, position)
        return view
    }

    private fun formatLabelWithCount(category: PhotoCategory, currentCount: Int): String {
        return if (category.minCount > 1) {
            "${category.label} ($currentCount/${category.minCount})"
        } else {
            category.label
        }
    }

    private fun setLabelColor(textView: TextView, position: Int) {
        val category = photoCategories[position]
        val label = category.label
        val currentCount = photoCounts[label] ?: 0
        val minCount = category.minCount

        // Set background color based on whether minCount is met
        textView.setBackgroundColor(
            if (currentCount >= minCount) {
                Color.parseColor("#C8E6C9") // Light green - requirement met
            } else {
                Color.parseColor("#FFCDD2") // Light red - requirement not met
            }
        )

        // Keep text color black for readability
        textView.setTextColor(Color.BLACK)
    }
}