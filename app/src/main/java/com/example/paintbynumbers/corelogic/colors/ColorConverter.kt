package com.example.paintbynumbers.corelogic.colors

import com.example.paintbynumbers.corelogic.utils.LabColorArray
import com.example.paintbynumbers.corelogic.utils.RgbColorArray
import kotlin.math.pow

/**
 * Object containing color conversion utilities (RGB, HSL, LAB).
 * Based on formulas from the original TypeScript code.
 */
object ColorConverter {

    /**
     * Converts an RGB color value to HSL.
     * Assumes r, g, and b are contained in the set [0, 255].
     * Returns h, s, and l in the set [0, 1].
     * @param rIn The red color value (0-255).
     * @param gIn The green color value (0-255).
     * @param bIn The blue color value (0-255).
     * @return DoubleArray The HSL representation [h, s, l].
     */
    fun rgbToHsl(rIn: Int, gIn: Int, bIn: Int): DoubleArray {
        val r = rIn / 255.0
        val g = gIn / 255.0
        val b = bIn / 255.0
        val max = kotlin.math.max(r, kotlin.math.max(g, b))
        val min = kotlin.math.min(r, kotlin.math.min(g, b))
        var h: Double
        val s: Double
        val l = (max + min) / 2.0

        if (max == min) {
            h = 0.0
            s = 0.0 // achromatic
        } else {
            val d = max - min
            s = if (l > 0.5) d / (2.0 - max - min) else d / (max + min)
            h = when (max) {
                r -> (g - b) / d + (if (g < b) 6.0 else 0.0)
                g -> (b - r) / d + 2.0
                b -> (r - g) / d + 4.0
                else -> 0.0 // Should not happen
            }
            h /= 6.0
        }
        return doubleArrayOf(h, s, l)
    }

    fun rgbToHsl(rgb: RgbColorArray): DoubleArray {
        require(rgb.size == 3) { "RGB array must have 3 components." }
        return rgbToHsl(rgb[0], rgb[1], rgb[2])
    }

    /**
     * Converts an HSL color value to RGB.
     * Assumes h, s, and l are contained in the set [0, 1].
     * Returns r, g, and b in the set [0, 255].
     * @param h The hue (0-1).
     * @param s The saturation (0-1).
     * @param l The lightness (0-1).
     * @return RgbColorArray The RGB representation [r, g, b].
     */
    fun hslToRgb(h: Double, s: Double, l: Double): RgbColorArray {
        var r: Double
        var g: Double
        var b: Double

        if (s == 0.0) {
            r = l; g = l; b = l // achromatic
        } else {
            val hue2rgb = { p: Double, q: Double, tIn: Double ->
                var t = tIn
                if (t < 0) t += 1.0
                if (t > 1) t -= 1.0
                when {
                    t < 1.0 / 6.0 -> p + (q - p) * 6.0 * t
                    t < 1.0 / 2.0 -> q
                    t < 2.0 / 3.0 -> p + (q - p) * (2.0 / 3.0 - t) * 6.0
                    else -> p
                }
            }
            val q = if (l < 0.5) l * (1.0 + s) else l + s - l * s
            val p = 2.0 * l - q
            r = hue2rgb(p, q, h + 1.0 / 3.0)
            g = hue2rgb(p, q, h)
            b = hue2rgb(p, q, h - 1.0 / 3.0)
        }
        return intArrayOf((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    /**
     * Converts a CIELAB color value to RGB.
     * @param lab LabColorArray [L, a, b]
     * @return RgbColorArray The RGB representation [r, g, b].
     */
    fun labToRgb(lab: LabColorArray): RgbColorArray {
        require(lab.size == 3) { "LAB array must have 3 components." }
        var y = (lab[0] + 16.0) / 116.0
        var x = lab[1] / 500.0 + y
        var z = y - lab[2] / 200.0

        val x3 = x * x * x
        val y3 = y * y * y
        val z3 = z * z * z

        x = 0.95047 * if (x3 > 0.008856) x3 else (x - 16.0 / 116.0) / 7.787
        y = 1.00000 * if (y3 > 0.008856) y3 else (y - 16.0 / 116.0) / 7.787
        z = 1.08883 * if (z3 > 0.008856) z3 else (z - 16.0 / 116.0) / 7.787

        var r = x * 3.2406 + y * -1.5372 + z * -0.4986
        var g = x * -0.9689 + y * 1.8758 + z * 0.0415
        var b = x * 0.0557 + y * -0.2040 + z * 1.0570

        r = if (r > 0.0031308) 1.055 * r.pow(1.0 / 2.4) - 0.055 else 12.92 * r
        g = if (g > 0.0031308) 1.055 * g.pow(1.0 / 2.4) - 0.055 else 12.92 * g
        b = if (b > 0.0031308) 1.055 * b.pow(1.0 / 2.4) - 0.055 else 12.92 * b

        return intArrayOf(
            (kotlin.math.max(0.0, kotlin.math.min(1.0, r)) * 255).toInt(),
            (kotlin.math.max(0.0, kotlin.math.min(1.0, g)) * 255).toInt(),
            (kotlin.math.max(0.0, kotlin.math.min(1.0, b)) * 255).toInt()
        )
    }

    /**
     * Converts an RGB color value to CIELAB.
     * @param rgb RgbColorArray [r, g, b]
     * @return LabColorArray The CIELAB representation [L, a, b].
     */
    fun rgbToLab(rgb: RgbColorArray): LabColorArray {
        require(rgb.size == 3) { "RGB array must have 3 components." }
        var r = rgb[0] / 255.0
        var g = rgb[1] / 255.0
        var b = rgb[2] / 255..0

        r = if (r > 0.04045) ((r + 0.055) / 1.055).pow(2.4) else r / 12.92
        g = if (g > 0.04045) ((g + 0.055) / 1.055).pow(2.4) else g / 12.92
        b = if (b > 0.04045) ((b + 0.055) / 1.055).pow(2.4) else b / 12.92

        var x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
        var y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.00000
        var z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883

        x = if (x > 0.008856) x.pow(1.0 / 3.0) else (7.787 * x) + 16.0 / 116.0
        y = if (y > 0.008856) y.pow(1.0 / 3.0) else (7.787 * y) + 16.0 / 116.0
        z = if (z > 0.008856) z.pow(1.0 / 3.0) else (7.787 * z) + 16.0 / 116.0

        return doubleArrayOf((116.0 * y) - 16.0, 500.0 * (x - y), 200.0 * (y - z))
    }
}
