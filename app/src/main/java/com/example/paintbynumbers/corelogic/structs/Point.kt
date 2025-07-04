package com.example.paintbynumbers.corelogic.structs

/**
 * Represents a 2D point with integer coordinates, typically for pixel positions.
 */
data class Point(val x: Int, val y: Int) {

    /**
     * Calculates the Manhattan distance to another Point.
     * This is |x1 - x2| + |y1 - y2|.
     * It's used because it reflects pixel grid adjacency (horizontal/vertical)
     * more directly than Euclidean distance for some algorithms.
     */
    fun distanceTo(pt: Point): Int {
        return kotlin.math.abs(pt.x - x) + kotlin.math.abs(pt.y - y)
    }

    /**
     * Calculates the Manhattan distance to specified coordinates.
     */
    fun distanceToCoord(px: Int, py: Int): Int {
        return kotlin.math.abs(px - x) + kotlin.math.abs(py - y)
    }
}
