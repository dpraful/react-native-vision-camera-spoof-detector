package com.faceantispoof

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.pow

/**
 * Converts YUV NV21 data directly to normalized RGB float arrays without creating Bitmaps.
 * Supports high-quality bilinear interpolation for resizing to minimize blur.
 * Includes motion/stability detection and frame quality assessment.
 */
class YuvToFloatArrayConverter {
    
    // Motion detection and frame stability
    private var previousGrayscale: IntArray? = null
    private var frameMotionScore = 0f
    private var isFrameStable = false
    private var consecutiveStableFrames = 0
    private val STABILITY_THRESHOLD = 0.05f // Lower = more stable frames required
    private val STABLE_FRAMES_REQUIRED = 3
    
    companion object {
        // Interpolation quality modes
        const val INTERPOLATION_NEAREST = 0
        const val INTERPOLATION_BILINEAR = 1
        const val INTERPOLATION_BICUBIC = 2
        
        // Default to bilinear for better quality
        private val DEFAULT_INTERPOLATION = INTERPOLATION_BILINEAR
    }

    /**
     * Convert NV21 YUV data directly to a normalized RGB float array.
     * Uses high-quality bilinear interpolation to preserve image sharpness.
     * Output array is [height][width][3] where RGB channels are normalized to [0, 1].
     * @param nv21 NV21 encoded YUV data
     * @param width Original frame width
     * @param height Original frame height
     * @param targetWidth Target output width (can be different for resizing)
     * @param targetHeight Target output height (can be different for resizing)
     * @param outputRgbFloat Pre-allocated float array to store output, or null to allocate new
     * @return Float array of shape [targetHeight * targetWidth * 3] with RGB values normalized to [0, 1]
     */
    fun nv21ToRgbFloatArray(
        nv21: ByteArray,
        width: Int,
        height: Int,
        targetWidth: Int = width,
        targetHeight: Int = height,
        outputRgbFloat: FloatArray? = null,
        interpolationMode: Int = DEFAULT_INTERPOLATION
    ): FloatArray {
        if (nv21.isEmpty()) {
            throw IllegalArgumentException("NV21 array is empty")
        }
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("Invalid frame dimensions: width=$width, height=$height")
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw IllegalArgumentException("Invalid target dimensions: targetWidth=$targetWidth, targetHeight=$targetHeight")
        }
        
        val outputSize = targetWidth * targetHeight * 3
        val output = outputRgbFloat?.takeIf { it.size >= outputSize } ?: FloatArray(outputSize)

        val ySize = width * height
        val uvPixelStride = 2
        
        // Validate NV21 buffer size
        val minExpectedSize = width * height + (width * height) / 2
        if (nv21.size < minExpectedSize) {
            throw IllegalArgumentException("NV21 buffer too small: expected at least $minExpectedSize, got ${nv21.size}")
        }

        // Use bilinear interpolation for high-quality resizing
        if (interpolationMode == INTERPOLATION_BILINEAR && (width != targetWidth || height != targetHeight)) {
            return nv21ToRgbFloatArrayBilinear(nv21, width, height, targetWidth, targetHeight, output)
        }

        // Fallback to nearest neighbor
        var outIdx = 0

        for (targetY in 0 until targetHeight) {
            val srcY = (targetY * height) / targetHeight
            if (srcY >= height) {
                throw IndexOutOfBoundsException("Source Y index out of bounds: srcY=$srcY, height=$height")
            }
            val srcRowOffset = srcY * width

            for (targetX in 0 until targetWidth) {
                val srcX = (targetX * width) / targetWidth
                if (srcX >= width) {
                    throw IndexOutOfBoundsException("Source X index out of bounds: srcX=$srcX, width=$width")
                }

                // Get Y component
                val yIdx = srcRowOffset + srcX
                if (yIdx >= nv21.size) {
                    throw IndexOutOfBoundsException("Y index out of bounds: yIdx=$yIdx, nv21.size=${nv21.size}")
                }
                val yVal = nv21[yIdx].toInt() and 0xFF

                // Get U and V components (NV21 layout: UV interleaved in second half)
                val uvIndex = ySize + (srcY / 2) * width + (srcX / 2) * uvPixelStride
                if (uvIndex + 1 >= nv21.size) {
                    throw IndexOutOfBoundsException("UV index out of bounds: uvIndex=$uvIndex, nv21.size=${nv21.size}")
                }
                val vVal = nv21[uvIndex].toInt() and 0xFF
                val uVal = nv21[uvIndex + 1].toInt() and 0xFF

                // YUV to RGB conversion
                val yf = yVal.toFloat()
                val uf = uVal - 128f
                val vf = vVal - 128f

                val r = (yf + 1.402f * vf).coerceIn(0f, 255f)
                val g = (yf - 0.34414f * uf - 0.71414f * vf).coerceIn(0f, 255f)
                val b = (yf + 1.772f * uf).coerceIn(0f, 255f)

                // Normalize to [0, 1]
                if (outIdx + 2 < output.size) {
                    output[outIdx++] = r / 255f
                    output[outIdx++] = g / 255f
                    output[outIdx++] = b / 255f
                } else {
                    throw IndexOutOfBoundsException("Output array index out of bounds: outIdx=$outIdx, output.size=${output.size}")
                }
            }
        }

        return output
    }

    /**
     * High-quality bilinear interpolation for resizing YUV to RGB.
     * Significantly better quality than nearest-neighbor, reduces blur artifacts.
     */
    private fun nv21ToRgbFloatArrayBilinear(
        nv21: ByteArray,
        width: Int,
        height: Int,
        targetWidth: Int,
        targetHeight: Int,
        output: FloatArray
    ): FloatArray {
        val ySize = width * height
        val uvPixelStride = 2
        val scaleX = width.toFloat() / targetWidth
        val scaleY = height.toFloat() / targetHeight
        
        var outIdx = 0

        for (targetY in 0 until targetHeight) {
            val srcY = targetY * scaleY
            val y1 = srcY.toInt()
            val y2 = (y1 + 1).coerceAtMost(height - 1)
            val fracY = srcY - y1

            for (targetX in 0 until targetWidth) {
                val srcX = targetX * scaleX
                val x1 = srcX.toInt()
                val x2 = (x1 + 1).coerceAtMost(width - 1)
                val fracX = srcX - x1

                // Sample 4 neighboring pixels in YUV space and interpolate
                val (r, g, b) = bilinearInterpolateYuv(
                    nv21, width, height, ySize, uvPixelStride,
                    x1, y1, x2, y2, fracX, fracY
                )

                if (outIdx + 2 < output.size) {
                    output[outIdx++] = r / 255f
                    output[outIdx++] = g / 255f
                    output[outIdx++] = b / 255f
                }
            }
        }

        return output
    }

    /**
     * Bilinear interpolation for YUV pixel sampling.
     * Returns interpolated RGB values.
     */
    private fun bilinearInterpolateYuv(
        nv21: ByteArray,
        width: Int,
        height: Int,
        ySize: Int,
        uvPixelStride: Int,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        fracX: Float,
        fracY: Float
    ): Triple<Float, Float, Float> {
        // Sample all 4 corners
        val samples = arrayOf(
            yuvToRgb(nv21, width, ySize, uvPixelStride, x1, y1),
            yuvToRgb(nv21, width, ySize, uvPixelStride, x2, y1),
            yuvToRgb(nv21, width, ySize, uvPixelStride, x1, y2),
            yuvToRgb(nv21, width, ySize, uvPixelStride, x2, y2)
        )

        // Bilinear interpolation
        val top = lerp(samples[0], samples[1], fracX)
        val bottom = lerp(samples[2], samples[3], fracX)
        return lerp(top, bottom, fracY)
    }

    /**
     * Linear interpolation helper.
     */
    private fun lerp(a: Triple<Float, Float, Float>, b: Triple<Float, Float, Float>, t: Float): Triple<Float, Float, Float> {
        return Triple(
            a.first * (1 - t) + b.first * t,
            a.second * (1 - t) + b.second * t,
            a.third * (1 - t) + b.third * t
        )
    }

    /**
     * Convert a single YUV pixel to RGB with optional gamma correction.
     * Applies sRGB gamma curve to match hardware ISP output.
     */
    private fun yuvToRgb(
        nv21: ByteArray,
        width: Int,
        ySize: Int,
        uvPixelStride: Int,
        x: Int,
        y: Int
    ): Triple<Float, Float, Float> {
        val yIdx = y * width + x
        val yVal = nv21[yIdx].toInt() and 0xFF

        val uvIndex = ySize + (y / 2) * width + (x / 2) * uvPixelStride
        val vVal = nv21[uvIndex].toInt() and 0xFF
        val uVal = nv21[uvIndex + 1].toInt() and 0xFF

        val yf = yVal.toFloat()
        val uf = uVal - 128f
        val vf = vVal - 128f

        val r = (yf + 1.402f * vf).coerceIn(0f, 255f)
        val g = (yf - 0.34414f * uf - 0.71414f * vf).coerceIn(0f, 255f)
        val b = (yf + 1.772f * uf).coerceIn(0f, 255f)

        // FIXED: Apply sRGB gamma correction to match hardware ISP
        // This ensures that colors match hardware-captured JPEGs
        val rLinear = r / 255f
        val gLinear = g / 255f
        val bLinear = b / 255f
        
        val rCorrected = applyGammaCorrection(rLinear)
        val gCorrected = applyGammaCorrection(gLinear)
        val bCorrected = applyGammaCorrection(bLinear)

        return Triple(rCorrected * 255f, gCorrected * 255f, bCorrected * 255f)
    }

    /**
     * Convert NV21 directly to a flat RGB float buffer (single channel interleaving).
     * @param nv21 NV21 encoded YUV data
     * @param width Original frame width
     * @param height Original frame height
     * @param targetWidth Target output width
     * @param targetHeight Target output height
     * @return ByteBuffer containing RGB floats in native byte order
     */
    fun nv21ToRgbByteBuffer(
        nv21: ByteArray,
        width: Int,
        height: Int,
        targetWidth: Int = width,
        targetHeight: Int = height
    ): ByteBuffer {
        val outputSize = targetWidth * targetHeight * 3 * 4 // 3 floats per pixel, 4 bytes per float
        val output = ByteBuffer.allocateDirect(outputSize).apply {
            order(java.nio.ByteOrder.nativeOrder())
        }

        val ySize = width * height
        val uvPixelStride = 2

        for (targetY in 0 until targetHeight) {
            val srcY = (targetY * height) / targetHeight
            val srcRowOffset = srcY * width

            for (targetX in 0 until targetWidth) {
                val srcX = (targetX * width) / targetWidth

                val yVal = nv21[srcRowOffset + srcX].toInt() and 0xFF

                val uvIndex = ySize + (srcY / 2) * width + (srcX / 2) * uvPixelStride
                val vVal = nv21[uvIndex].toInt() and 0xFF
                val uVal = nv21[uvIndex + 1].toInt() and 0xFF

                val yf = yVal.toFloat()
                val uf = uVal - 128f
                val vf = vVal - 128f

                val r = (yf + 1.402f * vf).coerceIn(0f, 255f) / 255f
                val g = (yf - 0.34414f * uf - 0.71414f * vf).coerceIn(0f, 255f) / 255f
                val b = (yf + 1.772f * uf).coerceIn(0f, 255f) / 255f

                output.putFloat(r)
                output.putFloat(g)
                output.putFloat(b)
            }
        }

        output.rewind()
        return output
    }

    /**
     * Convert NV21 to grayscale intensity values stored as floats [0, 1].
     * Useful for Laplacian edge detection.
     * @param nv21 NV21 encoded YUV data
     * @param width Original frame width
     * @param height Original frame height
     * @param targetWidth Target output width
     * @param targetHeight Target output height
     * @return Float array of size [targetHeight * targetWidth] with normalized grayscale values
     */
    fun nv21ToGrayscaleFloatArray(
        nv21: ByteArray,
        width: Int,
        height: Int,
        targetWidth: Int = width,
        targetHeight: Int = height,
        outputGrayscale: FloatArray? = null
    ): FloatArray {
        val outputSize = targetWidth * targetHeight
        val output = outputGrayscale?.takeIf { it.size >= outputSize } ?: FloatArray(outputSize)

        val ySize = width * height

        var outIdx = 0
        for (targetY in 0 until targetHeight) {
            val srcY = (targetY * height) / targetHeight
            val srcRowOffset = srcY * width

            for (targetX in 0 until targetWidth) {
                val srcX = (targetX * width) / targetWidth
                val yVal = nv21[srcRowOffset + srcX].toInt() and 0xFF
                output[outIdx++] = yVal / 255f
            }
        }

        return output
    }

    /**
     * Convert NV21 to integer grayscale values [0, 255].
     * More efficient for operations that don't need float precision.
     */
    fun nv21ToGrayscaleIntArray(
        nv21: ByteArray,
        width: Int,
        height: Int,
        targetWidth: Int = width,
        targetHeight: Int = height,
        outputGrayscale: IntArray? = null
    ): IntArray {
        if (nv21.isEmpty()) {
            throw IllegalArgumentException("NV21 array is empty")
        }
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("Invalid frame dimensions: width=$width, height=$height")
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw IllegalArgumentException("Invalid target dimensions: targetWidth=$targetWidth, targetHeight=$targetHeight")
        }
        
        val outputSize = targetWidth * targetHeight
        val output = outputGrayscale?.takeIf { it.size >= outputSize } ?: IntArray(outputSize)

        val ySize = width * height
        val minExpectedSize = width * height + (width * height) / 2
        if (nv21.size < minExpectedSize) {
            throw IllegalArgumentException("NV21 buffer too small: expected at least $minExpectedSize, got ${nv21.size}")
        }

        var outIdx = 0
        for (targetY in 0 until targetHeight) {
            val srcY = (targetY * height) / targetHeight
            if (srcY >= height) {
                throw IndexOutOfBoundsException("Source Y out of bounds: srcY=$srcY, height=$height")
            }
            val srcRowOffset = srcY * width

            for (targetX in 0 until targetWidth) {
                val srcX = (targetX * width) / targetWidth
                if (srcX >= width) {
                    throw IndexOutOfBoundsException("Source X out of bounds: srcX=$srcX, width=$width")
                }
                val yIdx = srcRowOffset + srcX
                if (yIdx >= nv21.size) {
                    throw IndexOutOfBoundsException("Y index out of bounds: yIdx=$yIdx, nv21.size=${nv21.size}")
                }
                val yVal = nv21[yIdx].toInt() and 0xFF
                if (outIdx < output.size) {
                    output[outIdx++] = yVal
                } else {
                    throw IndexOutOfBoundsException("Output index out of bounds: outIdx=$outIdx, output.size=${output.size}")
                }
            }
        }

        return output
    }

    /**
     * Convert NV21 directly to RGB byte array without float conversion.
     * Uses high-quality bilinear interpolation for resizing.
     * Optimized for JPEG encoding (where RGB bytes are needed directly).
     * Output format: RGBRGBRGB... (no alpha channel)
     *
     * @param nv21 NV21 encoded YUV data
     * @param width Original frame width
     * @param height Original frame height
     * @param targetWidth Target output width (for resizing)
     * @param targetHeight Target output height (for resizing)
     * @param outputRgbBytes Pre-allocated byte array, or null to allocate new
     * @return Byte array of size [targetHeight * targetWidth * 3] with RGB bytes
     */
    fun nv21ToRgbByteArray(
        nv21: ByteArray,
        width: Int,
        height: Int,
        targetWidth: Int = width,
        targetHeight: Int = height,
        outputRgbBytes: ByteArray? = null
    ): ByteArray {
        if (nv21.isEmpty()) {
            throw IllegalArgumentException("NV21 array is empty")
        }
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("Invalid frame dimensions: width=$width, height=$height")
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw IllegalArgumentException("Invalid target dimensions: targetWidth=$targetWidth, targetHeight=$targetHeight")
        }
        
        val outputSize = targetWidth * targetHeight * 3
        val output = outputRgbBytes?.takeIf { it.size >= outputSize } ?: ByteArray(outputSize)

        val ySize = width * height
        val uvPixelStride = 2
        
        val minExpectedSize = width * height + (width * height) / 2
        if (nv21.size < minExpectedSize) {
            throw IllegalArgumentException("NV21 buffer too small: expected at least $minExpectedSize, got ${nv21.size}")
        }

        // Use bilinear interpolation for better quality
        if (width != targetWidth || height != targetHeight) {
            return nv21ToRgbByteArrayBilinear(nv21, width, height, targetWidth, targetHeight, output)
        }

        // Direct copy with nearest neighbor
        var outIdx = 0

        for (targetY in 0 until targetHeight) {
            val srcY = (targetY * height) / targetHeight
            if (srcY >= height) {
                throw IndexOutOfBoundsException("Source Y out of bounds: srcY=$srcY, height=$height")
            }
            val srcRowOffset = srcY * width

            for (targetX in 0 until targetWidth) {
                val srcX = (targetX * width) / targetWidth
                if (srcX >= width) {
                    throw IndexOutOfBoundsException("Source X out of bounds: srcX=$srcX, width=$width")
                }

                // Get Y component
                val yIdx = srcRowOffset + srcX
                if (yIdx >= nv21.size) {
                    throw IndexOutOfBoundsException("Y index out of bounds: yIdx=$yIdx, nv21.size=${nv21.size}")
                }
                val yVal = nv21[yIdx].toInt() and 0xFF

                // Get U and V components (NV21 layout: UV interleaved in second half)
                val uvIndex = ySize + (srcY / 2) * width + (srcX / 2) * uvPixelStride
                if (uvIndex + 1 >= nv21.size) {
                    throw IndexOutOfBoundsException("UV index out of bounds: uvIndex=$uvIndex, nv21.size=${nv21.size}")
                }
                val vVal = nv21[uvIndex].toInt() and 0xFF
                val uVal = nv21[uvIndex + 1].toInt() and 0xFF

                // YUV to RGB conversion
                val yf = yVal.toFloat()
                val uf = uVal - 128f
                val vf = vVal - 128f

                val r = (yf + 1.402f * vf).coerceIn(0f, 255f).toInt().toByte()
                val g = (yf - 0.34414f * uf - 0.71414f * vf).coerceIn(0f, 255f).toInt().toByte()
                val b = (yf + 1.772f * uf).coerceIn(0f, 255f).toInt().toByte()

                if (outIdx + 2 < output.size) {
                    output[outIdx++] = r
                    output[outIdx++] = g
                    output[outIdx++] = b
                } else {
                    throw IndexOutOfBoundsException("Output index out of bounds: outIdx=$outIdx, output.size=${output.size}")
                }
            }
        }

        return output
    }

    /**
     * High-quality bilinear interpolation for RGB byte array resizing.
     */
    private fun nv21ToRgbByteArrayBilinear(
        nv21: ByteArray,
        width: Int,
        height: Int,
        targetWidth: Int,
        targetHeight: Int,
        output: ByteArray
    ): ByteArray {
        val ySize = width * height
        val uvPixelStride = 2
        val scaleX = width.toFloat() / targetWidth
        val scaleY = height.toFloat() / targetHeight
        
        var outIdx = 0

        for (targetY in 0 until targetHeight) {
            val srcY = targetY * scaleY
            val y1 = srcY.toInt()
            val y2 = (y1 + 1).coerceAtMost(height - 1)
            val fracY = srcY - y1

            for (targetX in 0 until targetWidth) {
                val srcX = targetX * scaleX
                val x1 = srcX.toInt()
                val x2 = (x1 + 1).coerceAtMost(width - 1)
                val fracX = srcX - x1


                val (r, g, b) = bilinearInterpolateYuv(
                    nv21, width, height, ySize, uvPixelStride,
                    x1, y1, x2, y2, fracX, fracY
                )

                if (outIdx + 2 < output.size) {
                    output[outIdx++] = r.toInt().toByte()
                    output[outIdx++] = g.toInt().toByte()
                    output[outIdx++] = b.toInt().toByte()
                }
            }
        }

        return output
    }

    /**
     * Apply sRGB gamma correction to match hardware camera ISP output.
     * This is CRITICAL for face recognition: hardware cameras apply gamma correction,
     * but raw YUV conversion doesn't. This function brings the frame processor output
     * in line with native camera JPEGs.
     * 
     * @param linear Linear RGB value in range [0, 1]
     * @return Gamma-corrected RGB value in range [0, 1]
     */
    private fun applyGammaCorrection(linear: Float): Float {
        return if (linear <= 0.0031308f) {
            // Linear segment for very dark values
            linear * 12.92f
        } else {
            // Power law segment for normal values
            // sRGB formula: value^(1/2.4)
            1.055f * Math.pow(linear.toDouble(), 1.0 / 2.4).toFloat() - 0.055f
        }
    }
}
