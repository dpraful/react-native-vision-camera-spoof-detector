package com.faceantispoof

import com.mrousavy.camera.frameprocessors.Frame
import com.mrousavy.camera.frameprocessors.FrameProcessorPlugin
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Frame processor that performs face anti-spoofing detection directly on YUV frames
 * without creating intermediate Bitmaps. Uses ByteBuffer and FloatArray for efficient processing.
 * 
 * IMPORTANT: All heavy processing is offloaded to a dedicated background thread (not the VisionCamera callback thread)
 * to prevent frame drop issues and ensure smooth camera preview.
 */
class FaceAntiSpoofFrameProcessor : FrameProcessorPlugin() {

    private var faceAntiSpoofing: FaceAntiSpoofingAdvanced? = null
    private var isInitialized = false
    
    // Single-threaded executor for offloading processing tasks
    private val processingExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FaceAntiSpoofProcessor").apply { isDaemon = true }
    }
    
    // Store the latest result to avoid race conditions
    private val latestResult: AtomicReference<Map<String, Any?>> = AtomicReference(
        mapOf(
            "error" to "Not initialized",
            "isLive" to false,
            "label" to "Not Initialized"
        )
    )
    
    // Track if processing is in flight to prevent queue overflow
    private val isProcessing = AtomicReference(false)

    fun initialize(assets: android.content.res.AssetManager) {
        try {
            faceAntiSpoofing = FaceAntiSpoofingAdvanced(assets)
            isInitialized = true
            Log.i("FaceAntiSpoof", "Initialized successfully")
        } catch (e: Exception) {
            Log.e("FaceAntiSpoof", "Initialization failed: ${e.message}", e)
            isInitialized = false
        }
    }

    override fun callback(frame: Frame, params: Map<String, Any?>?): Any? {
        // Return immediately without blocking the VisionCamera thread
        // The callback should return quickly to avoid frame drops
        
        if (!isInitialized) {
            return latestResult.get()
        }

        // Only process if the previous frame has finished processing
        // This prevents accumulating a queue of frames to process
        if (!isProcessing.compareAndSet(false, true)) {
            // Processing already in flight, return the latest result
            return latestResult.get()
        }

        // CRITICAL: Copy frame data BEFORE submitting to background thread
        // The frame reference becomes invalid after the callback returns
        val nv21: ByteArray
        val width: Int
        val height: Int
        
        try {
            // Extract YUV data immediately while the frame is still valid
            val image = frame.getImage() ?: throw Exception("Frame image is null")
            val planes = image.planes
            
            if (planes == null) {
                throw Exception("Frame planes are null")
            }

            if (planes.size < 3) {
                throw Exception("Invalid frame planes: expected 3, got ${planes.size}")
            }

            val yBuffer = planes[0]?.buffer ?: throw Exception("Y buffer is null")
            val uBuffer = planes[1]?.buffer ?: throw Exception("U buffer is null")
            val vBuffer = planes[2]?.buffer ?: throw Exception("V buffer is null")

            // Ensure buffers are accessible
            if (!yBuffer.isDirect && !yBuffer.hasArray()) {
                throw Exception("Y buffer is not accessible")
            }
            if (!uBuffer.isDirect && !uBuffer.hasArray()) {
                throw Exception("U buffer is not accessible")
            }
            if (!vBuffer.isDirect && !vBuffer.hasArray()) {
                throw Exception("V buffer is not accessible")
            }

            yBuffer.rewind()
            uBuffer.rewind()
            vBuffer.rewind()

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            if (ySize <= 0 || uSize <= 0 || vSize <= 0) {
                throw Exception("Invalid buffer sizes: Y=$ySize, U=$uSize, V=$vSize")
            }

            nv21 = ByteArray(ySize + uSize + vSize)

            try {
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)
            } catch (e: Exception) {
                throw Exception("Error copying YUV buffers: ${e.message}", e)
            }

            width = frame.getWidth()
            height = frame.getHeight()

            if (width <= 0 || height <= 0) {
                throw Exception("Invalid frame dimensions: width=$width, height=$height")
            }
        } catch (e: Exception) {
            Log.e("FaceAntiSpoof", "Error extracting frame data: ${e.message}", e)
            val errorMap = mapOf(
                "error" to e.message,
                "isLive" to false,
                "label" to "Error: ${e.message}"
            )
            latestResult.set(errorMap)
            isProcessing.set(false)
            return latestResult.get()
        }

        // Submit the heavy processing to the background thread executor
        // Now using the copied nv21 data, not the frame reference
        processingExecutor.submit {
            try {
                val faceAntispoof = faceAntiSpoofing ?: throw Exception("FaceAntiSpoofing not initialized")

                // Process copied NV21 data directly (no Bitmap conversion)
                val result = faceAntispoof.advancedAntiSpoofing(nv21, width, height)

                val resultMap = mapOf(
                    "neuralNetworkScore" to result.neuralNetworkScore.toDouble(),
                    "laplacianScore" to result.laplacianScore,
                    "combinedScore" to result.combinedScore.toDouble(),
                    "confidence" to result.confidence.toDouble()
                )
                
                // Update the latest result for the next callback
                latestResult.set(resultMap)
            } catch (e: Exception) {
                Log.e("FaceAntiSpoof", "Processing error: ${e.message}", e)
                val errorMap = mapOf(
                    "error" to e.message,
                    "isLive" to false,
                    "label" to "Error: ${e.message}"
                )
                latestResult.set(errorMap)
            } finally {
                // Mark processing as complete for the next frame
                isProcessing.set(false)
            }
        }
        
        // Return the latest result immediately without waiting
        return latestResult.get()
    }
    
    /**
     * Cleanup resources when the frame processor is no longer needed
     * This should be called when the camera is closed or the app is destroyed
     */
    fun cleanup() {
        try {
            // Shutdown the executor and wait for any running tasks to complete
            processingExecutor.shutdown()
            if (!processingExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                Log.w("FaceAntiSpoof", "Executor did not terminate in time, forcing shutdown")
                processingExecutor.shutdownNow()
            }
            
            // Clean up the face anti-spoofing model
            faceAntiSpoofing?.close()
            faceAntiSpoofing = null
            isInitialized = false
            
            Log.i("FaceAntiSpoof", "Cleanup completed successfully")
        } catch (e: Exception) {
            Log.e("FaceAntiSpoof", "Error during cleanup: ${e.message}", e)
        }
    }
}
