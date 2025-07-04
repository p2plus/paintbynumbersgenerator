package com.example.paintbynumbers.corelogic.structs

/**
 * A 2D map storing color indices for each pixel of an image.
 * This is used to map each pixel to an entry in a color palette.
 *
 * @property width The width of the image.
 * @property height The height of the image.
 */
class ImageColorIndicesMap(val width: Int, val height: Int) {
    private val data: IntArray = IntArray(width * height)

    init {
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("Width and height must be positive.")
        }
    }

    /**
     * Gets the color index at the specified (x, y) coordinate.
     * @throws IndexOutOfBoundsException if x or y are out of bounds.
     */
    fun get(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw IndexOutOfBoundsException("Coordinates ($x, $y) are out of bounds for image size ($width, $height).")
        }
        return data[y * width + x]
    }

    /**
     * Sets the color index at the specified (x, y) coordinate.
     * @throws IndexOutOfBoundsException if x or y are out of bounds.
     */
    fun set(x: Int, y: Int, value: Int) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw IndexOutOfBoundsException("Coordinates ($x, $y) are out of bounds for image size ($width, $height).")
        }
        data[y * width + x] = value
    }

    /**
     * Fills the entire map with a specific color index.
     */
    fun fill(value: Int) {
        data.fill(value)
    }

    /**
     * Provides direct access to the underlying flat data array.
     * Use with caution, respecting the width/height for indexing.
     */
    fun getRawData(): IntArray {
        return data
    }
}
