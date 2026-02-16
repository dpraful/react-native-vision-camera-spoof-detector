package com.faceantispoof

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import kotlin.math.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import android.util.Log
import kotlin.collections.ArrayList
import kotlinx.coroutines.runBlocking

class FaceAntiSpoofingAdvanced(private val assetManager: AssetManager) {

    companion object {
        const val INPUT_IMAGE_SIZE = 256
        const val BASE_THRESHOLD = 0.2f
        const val ROUTE_INDEX = 6
        const val LAPLACE_THRESHOLD = 50
        const val LAPLACIAN_THRESHOLD = 4000  // Increased from 1000 - real faces have ~3500+ laplacian score
        const val MODEL_FILE = "FaceAntiSpoofing.tflite"
        private const val TAG = "FaceAntiSpoofing"
        
        // Accelerator types
        const val ACCELERATOR_CPU = "CPU"
        const val ACCELERATOR_GPU = "GPU"
        const val ACCELERATOR_NNAPI = "NNAPI"
    }

    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private var gpuDelegate: GpuDelegate? = null
    private var currentAccelerator: String = ACCELERATOR_CPU
    
    // Reuse output arrays to avoid allocations
    private val clssPred = Array(1) { FloatArray(8) }
    private val leafNodeMask = Array(1) { FloatArray(8) }
    private val outputs = mutableMapOf<Int, Any>()
    private var outputIndex0: Int = 0
    private var outputIndex1: Int = 1

    init {
        initializeInterpreter()
    }

    private fun initializeInterpreter() {
        try {
            val modelBuffer = loadModelFile(assetManager, MODEL_FILE)
            val options = Interpreter.Options()
            
            // Use optimal settings
            val optimalThreads = 4 // Fixed as in working version
            options.setNumThreads(optimalThreads)
            
            currentAccelerator = ACCELERATOR_CPU
            Log.i(TAG, "CPU-ONLY MODE - Using $optimalThreads threads")
            
            interpreter = Interpreter(modelBuffer, options)
            
            try {
                outputIndex0 = interpreter!!.getOutputIndex("Identity")
                outputIndex1 = interpreter!!.getOutputIndex("Identity_1")
            } catch (e: Exception) {
                Log.w(TAG, "Could not resolve named output indices, using defaults: ${e.message}")
                // Fallback to default indices
                outputIndex0 = 0
                outputIndex1 = 1
            }
            
            outputs[outputIndex0] = clssPred
            outputs[outputIndex1] = leafNodeMask
            isInitialized = true
            Log.i(TAG, "Face Anti-Spoof initialized | CPU-ONLY MODE | Threads: $optimalThreads")
        } catch (e: Exception) {
            Log.e(TAG, "FAILED to load TFLite model: ${e.message}", e)
            isInitialized = false
            currentAccelerator = ACCELERATOR_CPU
        }
    }

    fun getAccelerator(): String = currentAccelerator

    // NEW: Add this method to accept Bitmap as input (matching working version)
    suspend fun advancedAntiSpoofing(bitmap: Bitmap): Result<AntiSpoofingResult> {
        return try {
            if (!isInitialized) {
                Result.failure(IllegalStateException("Classifier not initialized"))
            } else {
                val nnScore = neuralNetworkAntiSpoofing(bitmap).getOrThrow()
                val laplacianScore = laplacian(bitmap).getOrThrow()
                val isLive = nnScore < BASE_THRESHOLD && laplacianScore > LAPLACIAN_THRESHOLD
                val combinedScore = calculateCombinedScore(nnScore, laplacianScore)
                val confidence = calculateConfidence(nnScore, laplacianScore)

                Result.success(
                    AntiSpoofingResult(
                        neuralNetworkScore = nnScore,
                        laplacianScore = laplacianScore,
                        textureComplexity = 0, // Not calculated in working version
                        temporalConsistency = 0f, // Not calculated in working version
                        colorSaturation = 0f, // Not calculated in working version
                        combinedScore = combinedScore,
                        confidence = confidence
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in advancedAntiSpoofing", e)
            Result.failure(e)
        }
    }

    // NEW: Legacy method for NV21 compatibility
    fun advancedAntiSpoofing(nv21: ByteArray, width: Int, height: Int): AntiSpoofingResult {
        if (!isInitialized) {
            return AntiSpoofingResult(
                neuralNetworkScore = 0f,
                laplacianScore = 0,
                textureComplexity = 0,
                temporalConsistency = 0f,
                colorSaturation = 0f,
                combinedScore = 0f,
                confidence = 0f
            )
        }

        return try {
            // Convert NV21 to Bitmap
            val bitmap = nv21ToBitmap(nv21, width, height)
            val result = runBlocking { advancedAntiSpoofing(bitmap) }
            
            val spoofResult = result.getOrNull()
            if (spoofResult != null) {
                spoofResult
            } else {
                Log.e(TAG, "Anti-spoofing analysis failed", result.exceptionOrNull())
                AntiSpoofingResult(
                    neuralNetworkScore = 0f,
                    laplacianScore = 0,
                    textureComplexity = 0,
                    temporalConsistency = 0f,
                    colorSaturation = 0f,
                    combinedScore = 0f,
                    confidence = 0f
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in advancedAntiSpoofing", e)
            AntiSpoofingResult(
                neuralNetworkScore = 0f,
                laplacianScore = 0,
                textureComplexity = 0,
                temporalConsistency = 0f,
                colorSaturation = 0f,
                combinedScore = 0f,
                confidence = 0f
            )
        }
    }

    // FROM WORKING VERSION: neuralNetworkAntiSpoofing with Bitmap
    private fun neuralNetworkAntiSpoofing(bitmap: Bitmap): Result<Float> {
        return try {
            val bitmapScale = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true)
            val img = normalizeImage(bitmapScale)
            val input = arrayOf(img)

            // Clear and prepare outputs
            clssPred[0].fill(0f)
            leafNodeMask[0].fill(0f)
            
            outputs[outputIndex0] = clssPred
            outputs[outputIndex1] = leafNodeMask

            interpreter!!.runForMultipleInputsOutputs(arrayOf(input), outputs)

            Log.i(TAG, "clss_pred: [${clssPred[0].joinToString()}]")
            Log.i(TAG, "leaf_node_mask: [${leafNodeMask[0].joinToString()}]")

            val score = leafScore1(clssPred, leafNodeMask)

            if (bitmapScale != bitmap) {
                bitmapScale.recycle()
            }

            Result.success(score)
        } catch (e: Exception) {
            Log.e(TAG, "Error in neuralNetworkAntiSpoofing", e)
            Result.failure(e)
        }
    }

    // FROM WORKING VERSION: leafScore1
    private fun leafScore1(clssPred: Array<FloatArray>, leafNodeMask: Array<FloatArray>): Float {
        var score = 0f
        for (i in 0 until 8) {
            score += abs(clssPred[0][i]) * leafNodeMask[0][i]
        }
        return score
    }

    // FROM WORKING VERSION: normalizeImage
    private fun normalizeImage(bitmap: Bitmap): Array<Array<FloatArray>> {
        val h = bitmap.height
        val w = bitmap.width
        val floatValues = Array(h) { Array(w) { FloatArray(3) } }

        val imageStd = 255f
        val pixels = IntArray(h * w)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in 0 until h) {
            for (j in 0 until w) {
                val pixel = pixels[i * w + j]
                val r = ((pixel shr 16) and 0xFF) / imageStd
                val g = ((pixel shr 8) and 0xFF) / imageStd
                val b = (pixel and 0xFF) / imageStd
                floatValues[i][j] = floatArrayOf(r, g, b)
            }
        }
        return floatValues
    }

    // FROM WORKING VERSION: laplacian with Bitmap
    private fun laplacian(bitmap: Bitmap): Result<Int> {
        return try {
            val bitmapScale = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true)
            val score = calculateLaplacianScore(bitmapScale)

            if (bitmapScale != bitmap) {
                bitmapScale.recycle()
            }

            Result.success(score)
        } catch (e: Exception) {
            Log.e(TAG, "Error in laplacian calculation", e)
            Result.failure(e)
        }
    }

    // FROM WORKING VERSION: calculateLaplacianScore
    private fun calculateLaplacianScore(bitmap: Bitmap): Int {
        val laplace = arrayOf(
            intArrayOf(0, 1, 0),
            intArrayOf(1, -4, 1),
            intArrayOf(0, 1, 0)
        )

        val size = laplace.size
        val img = convertGreyImg(bitmap)
        val height = img.size
        val width = img[0].size

        var score = 0
        for (x in 0 until height - size + 1) {
            for (y in 0 until width - size + 1) {
                var result = 0
                for (i in 0 until size) {
                    for (j in 0 until size) {
                        result += img[x + i][y + j] * laplace[i][j]
                    }
                }
                if (result > LAPLACE_THRESHOLD) {
                    score++
                }
            }
        }
        return score
    }

    // FROM WORKING VERSION: convertGreyImg
    private fun convertGreyImg(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height
        val greyImg = Array(height) { IntArray(width) }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in 0 until height) {
            for (j in 0 until width) {
                val pixel = pixels[i * width + j]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                greyImg[i][j] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }
        return greyImg
    }

    // FROM WORKING VERSION: calculateCombinedScore
    private fun calculateCombinedScore(neuralScore: Float, laplacianScore: Int): Float {
        // Normalize both scores to 0-1 range
        val normalizedNeural = neuralScore.coerceIn(0f, 1f)
        val normalizedLaplacian = (laplacianScore.toFloat() / LAPLACIAN_THRESHOLD).coerceIn(0f, 1f)
        
        // For real face: neuralScore should be LOW (< 0.2), laplacianScore should be HIGH (> 3000)
        // Combined score: lower neural + higher laplacian = higher (more live)
        val neuralWeight = 0.6f
        val laplacianWeight = 0.4f
        
        // Invert neural score since low neural = live, high neural = spoof
        val invertedNeural = 1f - normalizedNeural
        
        return (invertedNeural * neuralWeight) + (normalizedLaplacian * laplacianWeight)
    }

    // NEW: Helper function to calculate confidence
    private fun calculateConfidence(neuralScore: Float, laplacianScore: Int): Float {
        // Confidence based on how well both metrics indicate a real face
        val neuralConfidence = neuralScore.coerceIn(0f, 1f)  // High neural score = spoof (lower confidence)
        val laplacianConfidence = (laplacianScore.toFloat() / LAPLACIAN_THRESHOLD).coerceIn(0f, 1f)
        
        // For real face: neural should be high (bad) and laplacian should be high (good)
        // So: (1 - neural) gives us confidence from neural, and laplacian gives direct confidence
        val invertedNeural = 1f - neuralConfidence
        
        return (invertedNeural * 0.6f + laplacianConfidence * 0.4f)
    }

    // NEW: Convert NV21 to Bitmap
    private fun nv21ToBitmap(nv21: ByteArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        
        val ySize = width * height
        val uvSize = (width * height) / 4
        val uvOffset = ySize
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val yIndex = y * width + x
                val uvIndex = uvOffset + ((y / 2) * (width / 2) + (x / 2)) * 2
                
                val yValue = (nv21[yIndex].toInt() and 0xFF)
                val uValue = (nv21[uvIndex].toInt() and 0xFF) - 128
                val vValue = (nv21[uvIndex + 1].toInt() and 0xFF) - 128
                
                // YUV to RGB conversion
                var r = yValue + (1.402f * vValue)
                var g = yValue - (0.344f * uValue) - (0.714f * vValue)
                var b = yValue + (1.772f * uValue)
                
                // Clamp values
                r = r.coerceIn(0f, 255f)
                g = g.coerceIn(0f, 255f)
                b = b.coerceIn(0f, 255f)
                
                pixels[yIndex] = (0xFF shl 24) or 
                               ((r.toInt() and 0xFF) shl 16) or 
                               ((g.toInt() and 0xFF) shl 8) or 
                               (b.toInt() and 0xFF)
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, modelName: String): MappedByteBuffer {
        return try {
            val fileDescriptor = assetManager.openFd(modelName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model file", e)
            throw e
        }
    }

    fun isInitialized(): Boolean = isInitialized

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            gpuDelegate?.close()
            gpuDelegate = null
            currentAccelerator = ACCELERATOR_CPU
            Log.i(TAG, "Interpreter and delegates closed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter", e)
        }
    }
}

data class AntiSpoofingResult(
    val neuralNetworkScore: Float,
    val laplacianScore: Int,
    val textureComplexity: Int,
    val temporalConsistency: Float,
    val colorSaturation: Float,
    val combinedScore: Float,
    val confidence: Float
)