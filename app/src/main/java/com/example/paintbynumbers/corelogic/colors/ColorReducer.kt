package com.example.paintbynumbers.corelogic.colors

import android.graphics.Bitmap
import com.example.paintbynumbers.corelogic.clustering.KMeans
import com.example.paintbynumbers.corelogic.clustering.Vector
import com.example.paintbynumbers.corelogic.settings.ClusteringColorSpace
import com.example.paintbynumbers.corelogic.settings.Settings
import com.example.paintbynumbers.corelogic.structs.ImageColorIndicesMap
import com.example.paintbynumbers.corelogic.utils.RgbColorArray
import com.example.paintbynumbers.corelogic.utils.toRgbColorArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

data class ColorMapResult(
    val imgColorIndices: ImageColorIndicesMap,
    val colorsByIndex: List<RgbColorArray>, // Palette: list of actual RGB colors
    val width: Int,
    val height: Int
)

object ColorReducer {

    /**
     * Creates a map of unique colors in the image to an index, and a list of these unique colors.
     * This is done *after* K-Means has reduced the colors in a bitmap.
     * The input bitmap should ideally be the one directly modified by K-Means output.
     */
    fun createColorMapFromClusteredBitmap(clusteredBitmap: Bitmap): ColorMapResult {
        val width = clusteredBitmap.width
        val height = clusteredBitmap.height
        val imgColorIndices = ImageColorIndicesMap(width, height)
        val colorsMap = mutableMapOf<String, Int>() // "R,G,B" string to index
        val colorsByIndexList = mutableListOf<RgbColorArray>()
        var nextColorIndex = 0

        val pixels = IntArray(width * height)
        clusteredBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val androidColor = pixels[y * width + x]
                val r = android.graphics.Color.red(androidColor)
                val g = android.graphics.Color.green(androidColor)
                val b = android.graphics.Color.blue(androidColor)

                val colorKey = "$r,$g,$b"
                val colorIndex = colorsMap.getOrPut(colorKey) {
                    colorsByIndexList.add(intArrayOf(r, g, b))
                    nextColorIndex++
                }
                imgColorIndices.set(x, y, colorIndex)
            }
        }
        return ColorMapResult(imgColorIndices, colorsByIndexList, width, height)
    }


    /**
     * Applies K-means clustering on the imgData to reduce the colors.
     * Operates on a sourceBitmap and returns a new Bitmap with reduced colors.
     * @param sourceBitmap The input image.
     * @param settings Application settings guiding the clustering.
     * @param onProgress Optional callback for progress updates (percentage, current KMeans state).
     *                   The KMeans object in callback is for inspection, not modification.
     * @return A new Bitmap with colors reduced by K-Means.
     */
    suspend fun applyKMeansClustering(
        sourceBitmap: Bitmap,
        settings: Settings,
        onProgress: (suspend (progressPercent: Int, kMeansSnapshot: KMeans?) -> Unit)? = null
    ): Bitmap = withContext(Dispatchers.Default) {

        val width = sourceBitmap.width
        val height = sourceBitmap.height

        val sourcePixels = IntArray(width * height)
        sourceBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)

        // 1. Group unique colors and their pixel coordinates (1D index)
        // This also applies the initial bit chopping to group similar colors.
        val pointsByColor = mutableMapOf<String, MutableList<Int>>() // "R,G,B" (chopped) -> List<pixelIndex>
        val uniqueChoppedColorsForVectors = mutableMapOf<String, RgbColorArray>() // "R,G,B" (chopped) -> Original RgbColorArray for tag

        for (pixelIndex in sourcePixels.indices) {
            currentCoroutineContext().ensureActive()
            val androidColor = sourcePixels[pixelIndex]
            var r = android.graphics.Color.red(androidColor)
            var g = android.graphics.Color.green(androidColor)
            var b = android.graphics.Color.blue(androidColor)

            // Store original for tag before chopping
            val originalRgbForTag = intArrayOf(r, g, b)

            // Small performance boost: reduce bitness of colors by chopping off the last bits
            val shift = settings.bitsToChopOffForColorGrouping
            r = r shr shift shl shift
            g = g shr shift shl shift
            b = b shr shift shl shift

            val colorKey = "$r,$g,$b"
            pointsByColor.getOrPut(colorKey) { mutableListOf() }.add(pixelIndex)
            // Store the first encountered original color for this chopped key as the representative tag
            if (!uniqueChoppedColorsForVectors.containsKey(colorKey)) {
                uniqueChoppedColorsForVectors[colorKey] = originalRgbForTag
            }
        }
        onProgress?.invoke(10, null) // Progress after initial color grouping

        // 2. Create vectors for K-Means
        val vectors = mutableListOf<Vector>()
        val totalPixels = width * height.toDouble()
        uniqueChoppedColorsForVectors.forEach { (colorKey, originalRgbTag) ->
            currentCoroutineContext().ensureActive()
            val rgbChopped = colorKey.split(",").map { it.toInt() }.toIntArray() // The actual color used for vector values (if RGB space)

            val dataValues: DoubleArray = when (settings.kMeansClusteringColorSpace) {
                ClusteringColorSpace.RGB -> rgbChopped.map { it.toDouble() }.toDoubleArray()
                ClusteringColorSpace.HSL -> ColorConverter.rgbToHsl(rgbChopped).map { it }.toDoubleArray() // HSL returns DoubleArray
                ClusteringColorSpace.LAB -> ColorConverter.rgbToLab(rgbChopped) // LAB returns DoubleArray
            }
            val weight = (pointsByColor[colorKey]?.size ?: 0) / totalPixels
            vectors.add(Vector(dataValues, weight, originalRgbTag))
        }
        onProgress?.invoke(20, null) // Progress after vector creation

        if (vectors.isEmpty()) { // Should not happen with a valid image
            return@withContext sourceBitmap.copy(sourceBitmap.config, true)
        }
        val actualK = settings.kMeansNrOfClusters.coerceAtMost(vectors.size)
        if (actualK == 0 && vectors.isNotEmpty()) { // If kMeansNrOfClusters is 0, but we have colors.
             return@withContext sourceBitmap.copy(sourceBitmap.config, true) // Or handle as error
        }
         if (actualK == 0 && vectors.isEmpty()) {
             return@withContext sourceBitmap.copy(sourceBitmap.config, true)
         }


        val randomInstance = if (settings.randomSeed == 0L) Random(System.currentTimeMillis()) else Random(settings.randomSeed)
        val kmeans = KMeans(vectors, actualK, randomInstance)

        // 3. Run K-Means iterations
        val maxIterations = 200 // Safeguard against non-convergence
        var iteration = 0
        do {
            currentCoroutineContext().ensureActive()
            kmeans.step()
            iteration++
            // Update GUI with progress (e.g., every few iterations or based on time)
            if (iteration % 5 == 0) {
                 // Create a snapshot for progress reporting.
                val progressCentroids = kmeans.centroids.map {centroid ->
                    val valuesCopy = centroid.values.copyOf()
                    val tagCopy = centroid.tag?.copyOf()
                    Vector(valuesCopy, centroid.weight, tagCopy)
                }
                val kMeansSnapshot = KMeans(emptyList(), kmeans.k, randomInstance, progressCentroids)

                onProgress?.invoke(20 + (60 * iteration / maxIterations).coerceAtMost(60) , kMeansSnapshot)
            }
        } while (kmeans.currentDeltaDistanceDifference > settings.kMeansMinDeltaDifference && iteration < maxIterations)

        onProgress?.invoke(80, kmeans) // Progress after K-Means convergence

        // 4. Create the output bitmap
        // Map original chopped colors to their new K-Means centroid colors
        val outputPixels = IntArray(width * height)
        val colorMappingCache = mutableMapOf<RgbColorArray, RgbColorArray>() // Cache for original RGB tag to final RGB

        for (clusterIdx in kmeans.centroids.indices) {
            currentCoroutineContext().ensureActive()
            val centroid = kmeans.centroids[clusterIdx]
            var finalCentroidRgb: RgbColorArray = when (settings.kMeansClusteringColorSpace) {
                ClusteringColorSpace.RGB -> centroid.values.map { floor(it.coerceIn(0.0, 255.0)).toInt() }.toIntArray()
                ClusteringColorSpace.HSL -> ColorConverter.hslToRgb(centroid.values[0], centroid.values[1], centroid.values[2])
                ClusteringColorSpace.LAB -> ColorConverter.labToRgb(centroid.values)
            }.map { it.coerceIn(0, 255) }.toIntArray()


            // Apply color restrictions if any
            if (settings.kMeansColorRestrictions.isNotEmpty()) {
                var minDistance = Double.MAX_VALUE
                var closestRestrictedColor: RgbColorArray? = null
                val centroidLabForRestriction = ColorConverter.rgbToLab(finalCentroidRgb)

                for (restrictionAny in settings.kMeansColorRestrictions) {
                    val restrictionRgb = settings.getRestrictedColorValue(restrictionAny) ?: continue
                    val restrictionLab = ColorConverter.rgbToLab(restrictionRgb)

                    val distL = centroidLabForRestriction[0] - restrictionLab[0]
                    val distA = centroidLabForRestriction[1] - restrictionLab[1]
                    val distB = centroidLabForRestriction[2] - restrictionLab[2]
                    val distance = sqrt(distL*distL + distA*distA + distB*distB) // Euclidean in LAB

                    if (distance < minDistance) {
                        minDistance = distance
                        closestRestrictedColor = restrictionRgb
                    }
                }
                if (closestRestrictedColor != null) {
                    finalCentroidRgb = closestRestrictedColor
                }
            }

            // For each vector (original unique color) in this cluster
            kmeans.pointsPerCategory[clusterIdx].forEach { vectorInCluster ->
                vectorInCluster.tag?.let { originalRgbTag -> // originalRgbTag is the key for mapping
                    colorMappingCache[originalRgbTag] = finalCentroidRgb
                }
            }
        }
        onProgress?.invoke(90, kmeans) // Progress after color mapping defined

        // Apply mapped colors to output pixels
        for (pixelIndex in sourcePixels.indices) {
            currentCoroutineContext().ensureActive()
            val originalAndroidColor = sourcePixels[pixelIndex]
            var r = android.graphics.Color.red(originalAndroidColor)
            var g = android.graphics.Color.green(originalAndroidColor)
            var b = android.graphics.Color.blue(originalAndroidColor)
            val originalRgbKey = intArrayOf(r,g,b) // This was the tag for the vector

            // Find which original chopped color this pixel belonged to
            val shift = settings.bitsToChopOffForColorGrouping
            r = r shr shift shl shift
            g = g shr shift shl shift
            b = b shr shift shl shift
            // The tag used for vector creation was based on the first instance of this chopped color
            val representativeOriginalTag = uniqueChoppedColorsForVectors["$r,$g,$b"]

            val newRgb = representativeOriginalTag?.let { colorMappingCache[it] }
                         ?: originalRgbKey // Fallback to original if something went wrong (should not happen)

            outputPixels[pixelIndex] = android.graphics.Color.rgb(newRgb[0], newRgb[1], newRgb[2])
        }

        val outputBitmap = Bitmap.createBitmap(width, height, sourceBitmap.config ?: Bitmap.Config.ARGB_8888)
        outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
        onProgress?.invoke(100, kmeans) // Done
        outputBitmap
    }


    /**
     * Builds a distance matrix for each color to each other in the palette.
     * Uses Euclidean distance in RGB space.
     */
    fun buildColorDistanceMatrix(colorsByIndex: List<RgbColorArray>): Array<DoubleArray> {
        val numColors = colorsByIndex.size
        val colorDistances = Array(numColors) { DoubleArray(numColors) }

        for (j in 0 until numColors) {
            for (i in j until numColors) {
                val c1 = colorsByIndex[j]
                val c2 = colorsByIndex[i]
                val distance = sqrt(
                    (c1[0] - c2[0]).toDouble().pow(2) +
                    (c1[1] - c2[1]).toDouble().pow(2) +
                    (c1[2] - c2[2]).toDouble().pow(2)
                )
                colorDistances[i][j] = distance
                colorDistances[j][i] = distance
            }
        }
        return colorDistances
    }

    /**
     * Cleans up narrow pixel strips (1 pixel wide horizontal/vertical lines).
     * Modifies the `colormapResult.imgColorIndices` in place.
     */
    suspend fun processNarrowPixelStripCleanup(
        colorMapResult: ColorMapResult,
        settings: Settings // For narrowPixelStripCleanupRuns
    ): Int = withContext(Dispatchers.Default){
        val colorDistances = buildColorDistanceMatrix(colorMapResult.colorsByIndex)
        val imgColorIndices = colorMapResult.imgColorIndices
        var totalPixelsReplaced = 0

        repeat(settings.narrowPixelStripCleanupRuns) {
            currentCoroutineContext().ensureActive()
            var runPixelsReplaced = 0
            // Iterate only over inner pixels (1 to width-2, 1 to height-2)
            for (y in 1 until colorMapResult.height - 1) {
                for (x in 1 until colorMapResult.width - 1) {
                    val topIdx = imgColorIndices.get(x, y - 1)
                    val bottomIdx = imgColorIndices.get(x, y + 1)
                    val leftIdx = imgColorIndices.get(x - 1, y)
                    val rightIdx = imgColorIndices.get(x + 1, y)
                    val currentIdx = imgColorIndices.get(x, y)

                    // Check for vertical 1-pixel strip: current is different from top AND bottom
                    if (currentIdx != topIdx && currentIdx != bottomIdx) {
                        val distToTop = colorDistances.getOrNull(currentIdx)?.getOrNull(topIdx) ?: Double.MAX_VALUE
                        val distToBottom = colorDistances.getOrNull(currentIdx)?.getOrNull(bottomIdx) ?: Double.MAX_VALUE
                        imgColorIndices.set(x, y, if (distToTop < distToBottom) topIdx else bottomIdx)
                        runPixelsReplaced++
                    }
                    // Check for horizontal 1-pixel strip: current is different from left AND right
                    // (use else if to avoid double-processing corners that satisfy both)
                    else if (currentIdx != leftIdx && currentIdx != rightIdx) {
                        val distToLeft = colorDistances.getOrNull(currentIdx)?.getOrNull(leftIdx) ?: Double.MAX_VALUE
                        val distToRight = colorDistances.getOrNull(currentIdx)?.getOrNull(rightIdx) ?: Double.MAX_VALUE
                        imgColorIndices.set(x, y, if (distToLeft < distToRight) leftIdx else rightIdx)
                        runPixelsReplaced++
                    }
                }
            }
            totalPixelsReplaced += runPixelsReplaced
            if (runPixelsReplaced == 0) return@repeat // No changes in this run, further runs are pointless
        }
        totalPixelsReplaced
    }
}
