package com.example.paintbynumbers.corelogic.settings

import com.example.paintbynumbers.corelogic.utils.RgbColorArray

enum class ClusteringColorSpace {
    RGB, HSL, LAB
}

/**
 * Represents the application settings relevant to the core image processing logic.
 *
 * @property kMeansNrOfClusters Number of clusters (colors) for K-Means.
 * @property kMeansMinDeltaDifference Convergence threshold for K-Means.
 * @property kMeansClusteringColorSpace Color space to use for K-Means distance calculations.
 * @property kMeansColorRestrictions List of colors to restrict K-Means centroids to.
 *                                   Each element can be an RgbColorArray or a String (alias key).
 * @property colorAliases Map of color alias names to RgbColorArray values.
 * @property randomSeed Seed for the random number generator. 0 means use current time.
 * @property removeFacetsSmallerThanNrOfPoints Threshold for removing small facets.
 * @property narrowPixelStripCleanupRuns Number of passes for narrow pixel strip cleanup.
 *                                      (Added from README, may or may not be used in ColorReducer directly)
 */
data class Settings(
    // K-Means related settings
    val kMeansNrOfClusters: Int = 16,
    val kMeansMinDeltaDifference: Double = 1.0,
    val kMeansClusteringColorSpace: ClusteringColorSpace = ClusteringColorSpace.LAB,
    val kMeansColorRestrictions: List<Any> = emptyList(), // List<RgbColorArray or String>
    val colorAliases: Map<String, RgbColorArray> = emptyMap(),
    val randomSeed: Long = 0L, // 0 means use current time based on original logic

    // Facet/Cleanup related settings (from README, may be used later)
    val removeFacetsSmallerThanNrOfPoints: Int = 10,
    val narrowPixelStripCleanupRuns: Int = 3, // From README, default may vary

    // Image resizing settings (from README, for reference, handled before core logic usually)
    val resizeImageIfTooLarge: Boolean = true,
    val resizeImageWidth: Int = 800,
    val resizeImageHeight: Int = 600,

    // Other settings that might be relevant from README for ColorReducer or future steps
    val bitsToChopOffForColorGrouping: Int = 2 // From ColorReducer.ts, used to group similar initial colors
) {
    // Helper to get a restricted color, converting alias if needed
    fun getRestrictedColorValue(restriction: Any): RgbColorArray? {
        return when (restriction) {
            is String -> colorAliases[restriction]
            is RgbColorArray -> restriction
            else -> null // Or throw exception for invalid type
        }
    }
}
