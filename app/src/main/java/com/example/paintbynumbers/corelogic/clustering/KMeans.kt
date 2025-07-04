package com.example.paintbynumbers.corelogic.clustering

import kotlin.random.Random

/**
 * K-Means clustering algorithm implementation.
 *
 * @property points The list of [Vector]s to cluster.
 * @property k The number of clusters to find.
 * @property random Kotlin's Random number generator for centroid initialization.
 * @property initialCentroids Optional predefined initial centroids. If null, centroids are chosen randomly from points.
 */
class KMeans(
    private val points: List<Vector>,
    val k: Int,
    private val random: Random,
    initialCentroids: List<Vector>? = null
) {
    val centroids: MutableList<Vector> = mutableListOf()
    val pointsPerCategory: MutableList<MutableList<Vector>>
    var currentDeltaDistanceDifference: Double = 0.0
        private set
    var currentIteration: Int = 0
        private set

    init {
        if (k <= 0) throw IllegalArgumentException("Number of clusters (k) must be positive.")
        if (points.isEmpty()) throw IllegalArgumentException("Point list cannot be empty for KMeans.")
        if (k > points.size) throw IllegalArgumentException("Number of clusters (k) cannot exceed the number of points.")


        pointsPerCategory = MutableList(k) { mutableListOf() }

        if (initialCentroids != null) {
            if (initialCentroids.size != k) {
                throw IllegalArgumentException("Initial centroids list size must match k.")
            }
            this.centroids.addAll(initialCentroids)
        } else {
            initCentroids()
        }
    }

    private fun initCentroids() {
        // Simple random initialization: pick k distinct points from the input dataset
        // More sophisticated methods like k-means++ could be used for better initialization.
        if (points.size < k) {
             // Should not happen due to init check, but defensive.
            centroids.addAll(points) // Use all points if k is larger
            // Add duplicates if k is still larger - though this scenario is problematic for k-means
            var i = 0
            while(centroids.size < k) {
                centroids.add(points[i % points.size])
                i++
            }
            return
        }

        val distinctPoints = points.distinct() // Ensure we pick from unique points if possible
        val availablePoints = distinctPoints.toMutableList()

        for (i in 0 until k) {
            if (availablePoints.isEmpty()) {
                // Fallback if not enough distinct points (e.g. k > distinctPoints.size)
                // Pick from original points with replacement
                centroids.add(points[random.nextInt(points.size)])
            } else {
                val randomIndex = random.nextInt(availablePoints.size)
                centroids.add(availablePoints.removeAt(randomIndex))
            }
        }
    }

    /**
     * Performs one iteration of the K-Means algorithm.
     * 1. Clears current cluster assignments.
     * 2. Assigns each point to the nearest centroid.
     * 3. Recalculates centroids based on the mean of assigned points.
     * Updates `currentDeltaDistanceDifference` with the sum of distances moved by centroids.
     */
    fun step() {
        // 1. Clear category assignments
        for (i in 0 until k) {
            pointsPerCategory[i].clear()
        }

        // 2. Assign points to the nearest centroid
        for (p in points) {
            var minDist = Double.MAX_VALUE
            var centroidIndex = -1
            for (centroidIdx in 0 until k) {
                val dist = centroids[centroidIdx].distanceTo(p)
                if (dist < minDist) {
                    minDist = dist
                    centroidIndex = centroidIdx
                }
            }
            if (centroidIndex != -1) {
                pointsPerCategory[centroidIndex].add(p)
            } else {
                // Should not happen if k > 0 and centroids are initialized
                // Assign to first category as a fallback if somehow no centroid was closest
                // (e.g. if all distances were Double.MAX_VALUE, though Vector.distanceTo should prevent this)
                if (pointsPerCategory.isNotEmpty()) {
                    pointsPerCategory[0].add(p)
                }
            }
        }

        var totalDistanceDiff = 0.0

        // 3. Adjust centroids
        for (centroidIdx in 0 until k) {
            val categoryPoints = pointsPerCategory[centroidIdx]
            if (categoryPoints.isNotEmpty()) {
                val oldCentroid = centroids[centroidIdx]
                val newCentroid = Vector.average(categoryPoints)
                // Preserve the original tag of the centroid if it was an initial seed point that has not moved yet.
                // Once averaged, the tag might not be relevant in the same way.
                // The original JS code does not seem to propagate tags for centroids after averaging.
                // newCentroid.tag = oldCentroid.tag // Optional: consider tag propagation strategy

                totalDistanceDiff += oldCentroid.distanceTo(newCentroid)
                centroids[centroidIdx] = newCentroid
            } else {
                // If a centroid has no points, it could be re-initialized.
                // For now, it remains unchanged. Some k-means variants might re-assign it
                // to a random point or the point furthest from its centroid.
                // The original code doesn't explicitly handle this, average() would throw.
                // Here, if categoryPoints is empty, old centroid is kept.
            }
        }
        currentDeltaDistanceDifference = totalDistanceDiff
        currentIteration++
    }
}
