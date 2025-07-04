package com.example.paintbynumbers.corelogic.clustering

import com.example.paintbynumbers.corelogic.utils.RgbColorArray
import java.util.Arrays

/**
 * Represents a data point (typically a color in a specific color space) for clustering.
 *
 * @property values The components of the vector (e.g., [R, G, B] or [L, A, B]).
 * @property weight The weight of this vector, often representing frequency of the color.
 * @property tag Optional storage for original RgbColorArray if values are transformed (e.g. to LAB).
 */
data class Vector(
    val values: DoubleArray,
    val weight: Double = 1.0,
    var tag: RgbColorArray? = null // To store original RGB color if 'values' are in a different space like LAB
) {
    /**
     * Calculates the Euclidean distance to another vector.
     */
    fun distanceTo(p: Vector): Double {
        if (values.size != p.values.size) {
            throw IllegalArgumentException("Vectors must have the same dimensions.")
        }
        var sumSquares = 0.0
        for (i in values.indices) {
            val diff = p.values[i] - values[i]
            sumSquares += diff * diff
        }
        return kotlin.math.sqrt(sumSquares)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vector

        if (!values.contentEquals(other.values)) return false
        if (weight != other.weight) return false
        // Tag is not part of primary equality, but can be checked if needed
        // if (tag != null) {
        //     if (other.tag == null) return false
        //     if (!tag.contentEquals(other.tag)) return false
        // } else if (other.tag != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = values.contentHashCode()
        result = 31 * result + weight.hashCode()
        // result = 31 * result + (tag?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        /**
         * Calculates the weighted average of the given points.
         * @throws IllegalArgumentException if the list of points is empty or dimensions mismatch.
         */
        fun average(pts: List<Vector>): Vector {
            if (pts.isEmpty()) {
                throw IllegalArgumentException("Can't average 0 elements")
            }

            val dims = pts[0].values.size
            val avgValues = DoubleArray(dims)
            var weightSum = 0.0

            for (p in pts) {
                if (p.values.size != dims) {
                    throw IllegalArgumentException("All vectors must have the same dimensions for averaging.")
                }
                weightSum += p.weight
                for (i in 0 until dims) {
                    avgValues[i] += p.weight * p.values[i]
                }
            }

            if (weightSum == 0.0) {
                // Avoid division by zero if all weights are zero (unlikely but possible)
                // Return a zero vector or the first point, depending on desired behavior.
                // Here, returning the first point's structure with zero values.
                return Vector(DoubleArray(dims), 0.0, pts[0].tag)
            }

            for (i in avgValues.indices) {
                avgValues[i] /= weightSum
            }
            // The tag of the average vector is somewhat ambiguous.
            // It could be null, or the tag of the most weighted vector, etc.
            // For now, setting to null or could take the first point's tag.
            return Vector(avgValues, 1.0, null)
        }
    }
}
