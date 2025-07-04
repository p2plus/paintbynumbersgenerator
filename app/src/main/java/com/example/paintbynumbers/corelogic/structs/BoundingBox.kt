package com.example.paintbynumbers.corelogic.structs

import kotlin.math.max
import kotlin.math.min

/**
 * Represents a bounding box defined by minimum and maximum x and y coordinates.
 */
class BoundingBox {
    var minX: Int = Int.MAX_VALUE
        private set // Allow external read, but modify only via addPoint
    var minY: Int = Int.MAX_VALUE
        private set
    var maxX: Int = Int.MIN_VALUE
        private set
    var maxY: Int = Int.MIN_VALUE
        private set

    val width: Int
        get() = if (isEmpty()) 0 else maxX - minX + 1

    val height: Int
        get() = if (isEmpty()) 0 else maxY - minY + 1

    /**
     * Checks if the bounding box is empty (e.g., no points added yet or reset).
     */
    fun isEmpty(): Boolean {
        return minX > maxX || minY > maxY
    }

    /**
     * Adds a point to the bounding box, expanding the box if necessary.
     */
    fun addPoint(x: Int, y: Int) {
        minX = min(minX, x)
        minY = min(minY, y)
        maxX = max(maxX, x)
        maxY = max(maxY, y)
    }

    /**
     * Adds a Point object to the bounding box.
     */
    fun addPoint(point: Point) {
        addPoint(point.x, point.y)
    }

    /**
     * Resets the bounding box to an empty state.
     */
    fun reset() {
        minX = Int.MAX_VALUE
        minY = Int.MAX_VALUE
        maxX = Int.MIN_VALUE
        maxY = Int.MIN_VALUE
    }

    /**
     * Checks if a given point is contained within this bounding box (inclusive).
     */
    fun contains(x: Int, y: Int): Boolean {
        return x >= minX && x <= maxX && y >= minY && y <= maxY
    }

    fun contains(point: Point): Boolean {
        return contains(point.x, point.y)
    }
}
