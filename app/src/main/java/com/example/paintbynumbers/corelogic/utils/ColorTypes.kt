package com.example.paintbynumbers.corelogic.utils

import android.graphics.Color

/**
 * Type alias for an RGB color represented as an IntArray [R, G, B].
 * Components are expected to be in the range 0-255.
 */
typealias RgbColorArray = IntArray

/**
 * Converts an RgbColorArray (IntArray of [R, G, B]) to an Android ARGB Int.
 * Alpha is set to 255 (opaque).
 */
fun RgbColorArray.toAndroidColor(): Int {
    require(this.size == 3) { "RgbColorArray must have 3 components (R, G, B)." }
    require(this[0] in 0..255 && this[1] in 0..255 && this[2] in 0..255) { "RGB components must be 0-255." }
    return Color.rgb(this[0], this[1], this[2])
}

/**
 * Converts an Android ARGB Int color to an RgbColorArray (IntArray of [R, G, B]).
 * Alpha component is ignored.
 */
fun Int.toRgbColorArray(): RgbColorArray {
    return intArrayOf(Color.red(this), Color.green(this), Color.blue(this))
}

/**
 * Type alias for LAB color represented as a DoubleArray [L, A, B].
 */
typealias LabColorArray = DoubleArray
