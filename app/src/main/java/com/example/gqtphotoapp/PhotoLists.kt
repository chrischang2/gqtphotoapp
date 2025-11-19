package com.example.gqtphotoapp

import android.content.Context

data class PhotoCategory(
    val label: String,
    val minCount: Int,
    val isRequired: Boolean = true
)

object PhotoLists {

    // Enums for better type safety
    enum class ProductType {
        OCC, SPRN, ALUMINIUM
    }

    enum class SampleCode {
        II_A, II_B, II_C, II_D
    }

    // Function to determine sample code and container count
    fun getSampleInfo(numContainers: Int): Pair<SampleCode, Int> {
        return when {
            numContainers <= 8 -> Pair(SampleCode.II_A, 2)
            numContainers <= 15 -> Pair(SampleCode.II_B, 3)
            numContainers <= 25 -> Pair(SampleCode.II_C, 5)
            else -> Pair(SampleCode.II_D, 8)
        }
    }

    // Filename mapping for photo categories
    private val filenameMap = mapOf(
        "Container List" to "Container List",
        "Overview" to "OV",
        "Close View" to "CV",
        "Radiation background" to "1",
        "Radiation towards bales" to "R",
        "Moisture Level" to "M",
        "Sample Bale Weight" to "BW",
        "Scale Cert" to "Scale Cert",
        "Scale S/N" to "Scale S/N",
        "Sample Bale on ground/scale" to "BL",
        "Selfie with Sample Bale" to "BLS",
        "Loosed Sample Bale" to "SG",
        "Selfie with Loosed Sample Bale" to "SGS",
        "Non-Paper Component Findings" to "NP",
        "Selfie with Non-Paper Component Findings" to "NPS",
        "Non-Paper component weights" to "NPW",
        "Total Unwanted Material Findings" to "TUM",
        "Selfie with Total Unwanted Material Findings" to "TUMS",
        "Total Unwanted Material Weights" to "TUMW",
        "Empty Container" to "Empty Container",
        "Selfie with Loading Container" to "Selfie with Loading Container"
    )

    /**
     * Generate filename for a photo based on its category label and optional index
     * @param categoryLabel The label of the photo category
     * @param photoIndex The index of the photo within that category (1-based).
     *                   If null, no numbering is added (for single photos).
     *                   For duplicates, numbering starts from 2.
     * @param extension File extension (defaults to "jpg")
     * @return The filename to use when saving the photo
     */
    fun getPhotoFilename(categoryLabel: String, photoIndex: Int?, extension: String = "jpg"): String {
        // Check if it's a container-specific category
        val containerPattern = Regex("""Container (\d+) - (.+)""")
        val containerMatch = containerPattern.find(categoryLabel)

        if (containerMatch != null) {
            val containerNum = containerMatch.groupValues[1]
            val containerAction = containerMatch.groupValues[2]

            val actionCode = when (containerAction) {
                "Full Loaded" -> "FL"
                "Selfie with Full Loaded" -> "FLS"
                "Closed" -> "CL"
                "Selfie with Closed" -> "CLS"
                "Seal" -> "SEAL"
                else -> containerAction
            }

            // Container photos are always single, no numbering
            return "${actionCode}.${extension}"
        }

        // Use the mapping for standard categories
        val baseFilename = filenameMap[categoryLabel] ?: categoryLabel

        // Add enumeration only if photoIndex is provided and > 1 (duplicates start at 2)
        return if (photoIndex != null) {
            "${baseFilename}(${photoIndex}).${extension}"
        } else {
            "${baseFilename}.${extension}"
        }
    }

    // Generate paper photo categories based on num_sample_containers
    fun getPaperPhotoCategories(numSampleContainers: Int): List<PhotoCategory> {
        val numSampleBales = numSampleContainers * 2

        val categories = mutableListOf(
            PhotoCategory("Container List", minCount = 1),
            PhotoCategory("Overview", minCount = 3),
            PhotoCategory("Close View", minCount = 2),
            PhotoCategory("Radiation background", minCount = 1),
            PhotoCategory("Radiation towards bales", minCount = numSampleContainers),
            PhotoCategory("Moisture Level", minCount = numSampleBales),
            PhotoCategory("Sample Bale Weight", minCount = numSampleBales),
            PhotoCategory("Scale Cert", minCount = 1),
            PhotoCategory("Scale S/N", minCount = 1),
            PhotoCategory("Sample Bale on ground/scale", minCount = numSampleBales),
            PhotoCategory("Selfie with Sample Bale", minCount = 1),
            PhotoCategory("Loosed Sample Bale", minCount = numSampleBales),
            PhotoCategory("Selfie with Loosed Sample Bale", minCount = 1),
            PhotoCategory("Non-Paper Component Findings", minCount = 1),
            PhotoCategory("Selfie with Non-Paper Component Findings", minCount = 1),
            PhotoCategory("Non-Paper component weights", minCount = numSampleBales),
            PhotoCategory("Total Unwanted Material Findings", minCount = 1),
            PhotoCategory("Selfie with Total Unwanted Material Findings", minCount = 1),
            PhotoCategory("Total Unwanted Material Weights", minCount = numSampleBales),
            PhotoCategory("Empty Container", minCount = 1),
            PhotoCategory("Selfie with Loading Container", minCount = 1)
        )

        // Generate individual container categories
        for (i in 1..numSampleContainers) {
            categories.add(PhotoCategory("Container $i - Full Loaded", minCount = 1))
            categories.add(PhotoCategory("Container $i - Selfie with Full Loaded", minCount = 1))
            categories.add(PhotoCategory("Container $i - Closed", minCount = 1))
            categories.add(PhotoCategory("Container $i - Selfie with Closed", minCount = 1))
            categories.add(PhotoCategory("Container $i - Seal", minCount = 1))
        }

        return categories
    }

    // Generate metal photo categories
    fun getMetalPhotoCategories(numSampleContainers: Int): List<PhotoCategory> {
        return listOf(
            PhotoCategory("Photo 1", minCount = 1),
            PhotoCategory("Photo 2", minCount = 1),
            PhotoCategory("Photo 3", minCount = 1)
        )
    }

    // Main function to get categories based on product type
    fun getPhotoCategories(
        productType: ProductType,
        numContainers: Int
    ): List<PhotoCategory> {
        val (_, numSampleContainers) = getSampleInfo(numContainers)

        return when (productType) {
            ProductType.OCC, ProductType.SPRN -> getPaperPhotoCategories(numSampleContainers)
            ProductType.ALUMINIUM -> getMetalPhotoCategories(numSampleContainers)
        }
    }

    /**
     * Determines which subfolder a photo should be saved to based on its label
     */
    fun getSubfolder(label: String, numContainers: Int, context: Context): String {
        // Overview photos
        if (label.startsWith("Overview") || label.startsWith("OV") ||
            label.startsWith("Close View") || label.startsWith("CV") ||
            label == "Container List") {
            return "1. Overview"
        }

        // Inspection photos
        if (label in listOf(
                "Radiation background",
                "Radiation towards bales",
                "Moisture Level",
                "Sample Bale Weight",
                "Scale Cert",
                "Scale S/N",
                "Sample Bale on ground/scale",
                "Selfie with Sample Bale",
                "Loosed Sample Bale",
                "Selfie with Loosed Sample Bale",
                "Non-Paper Component Findings",
                "Selfie with Non-Paper Component Findings",
                "Non-Paper component weights",
                "Total Unwanted Material Findings",
                "Selfie with Total Unwanted Material Findings",
                "Total Unwanted Material Weights"
            ) || label == "1" || label.startsWith("R") || label.startsWith("M") ||
            label.startsWith("BW") || label.startsWith("BL") || label == "BLS" ||
            label.startsWith("SG") || label == "SGS" || label == "NP" ||
            label.startsWith("NPW") || label == "NPS" || label == "TUM" ||
            label.startsWith("TUMW") || label == "TUMS"
        ) {
            return "2. Inspection"
        }

        // Empty Container and Selfie with Loading Container (go to first container folder)
        if (label == "Empty Container" || label == "Selfie with Loading Container") {
            val sharedPrefs = context.getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
            val containerName = sharedPrefs.getString("container_1_number", "1") ?: "1"
            return "3. ${containerName}"  // CHANGED: Removed "Container_" prefix
        }

        // Container-specific photos (using the "Container X - ..." format)
        val containerPattern = Regex("""Container (\d+) - .+""")
        val containerMatch = containerPattern.find(label)

        if (containerMatch != null) {
            val containerNum = containerMatch.groupValues[1].toInt()
            val folderNum = containerNum + 2  // Container 1 goes to folder 3, etc.

            val sharedPrefs = context.getSharedPreferences("GQTPhotoApp", Context.MODE_PRIVATE)
            val containerName = sharedPrefs.getString("container_${containerNum}_number", containerNum.toString())
                ?: containerNum.toString()

            return "${folderNum}. ${containerName}"  // CHANGED: Removed "Container_" prefix
        }

        // Fallback
        return "Other"
    }

// Data class to track photo capture progress with checkbox functionality
data class PhotoCategoryProgress(
    val category: PhotoCategory,
    val capturedCount: Int = 0,
    val isCompleted: Boolean = false
) {
    fun needsMorePhotos(): Boolean = capturedCount < category.minCount

    fun getDisplayText(): String {
        return "${category.label} ($capturedCount/${category.minCount})"
    }
}

// Legacy support - keeping old functions for backward compatibility
fun getListNames(): List<String> = listOf("II-A", "II-B", "II-C", "II-D")

fun getList(name: String): List<String> {
    // Default to II-A paper list for backward compatibility
    val categories = PhotoLists.getPaperPhotoCategories(2)
    return categories.map { it.label }
}
}

// Example usage:
/*
// Single photo (no numbering):
val filename1 = PhotoLists.getPhotoFilename("Container List", null)
// Result: "Container List.jpg"

// First of multiple photos (no numbering):
val filename2 = PhotoLists.getPhotoFilename("Overview", 1)
// Result: "OV.jpg" (first photo has no number)

// Duplicate photos (numbering starts at 2):
val filename3 = PhotoLists.getPhotoFilename("Overview", 2)
// Result: "OV(2).jpg"

val filename4 = PhotoLists.getPhotoFilename("Overview", 3)
// Result: "OV(3).jpg"

// Container photos (always single, no numbering):
val filename5 = PhotoLists.getPhotoFilename("Container 3 - Seal", null)
// Result: "C3_SEAL.jpg"
*/