package com.example.gqtphotoapp

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

    // Generate paper photo categories based on num_sample_containers
    fun getPaperPhotoCategories(numSampleContainers: Int): List<PhotoCategory> {
        val numSampleBales = numSampleContainers * 2

        return listOf(
            PhotoCategory("Overview", minCount = 3),
            PhotoCategory("Close View", minCount = 2),
            PhotoCategory("Radiation background", minCount = 1),
            PhotoCategory("Radiation", minCount = numSampleContainers),
            PhotoCategory("Moisture", minCount = numSampleBales),
            PhotoCategory("Sample Bale Weight", minCount = numSampleBales),
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
            PhotoCategory("Selfie with Loading Container", minCount = 1),
            PhotoCategory("Full-Loaded Container", minCount = numSampleContainers),
            PhotoCategory("Selfie with Full Loaded Container", minCount = numSampleContainers),
            PhotoCategory("Closed Container", minCount = numSampleContainers),
            PhotoCategory("Selfie with Closed Container", minCount = numSampleContainers),
            PhotoCategory("Seal", minCount = numSampleContainers)
        )
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