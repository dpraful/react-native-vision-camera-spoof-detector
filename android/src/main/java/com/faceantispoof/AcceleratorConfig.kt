package com.faceantispoof

/**
 * Configuration class for TensorFlow Lite accelerators
 * Allows developers to control GPU/NNAPI delegate behavior
 * and optimize for different device tiers (low-end, mid-range, high-end)
 */
object AcceleratorConfig {
    
    /**
     * Enable GPU acceleration (recommended for modern devices)
     * Accelerates model 3-10x compared to CPU
     * GPU is automatically skipped on low-end devices even if enabled
     */
    var enableGpu: Boolean = true
    
    /**
     * Enable NNAPI acceleration (recommended for Pixel/Samsung devices)
     * Accelerates model 3-10x compared to CPU
     * Fallback if GPU is not available
     * Works better on mid-range and high-end devices
     */
    var enableNnapi: Boolean = true
    
    /**
     * Enable CPU fallback if GPU/NNAPI are not available
     * Default: true (always fallback to CPU)
     */
    var enableCpuFallback: Boolean = true
    
    /**
     * Number of threads for CPU inference
     * Default: 4 threads (optimal for most devices)
     * Low-end devices will use 1-2 threads regardless
     */
    var cpuThreadCount: Int = 4
    
    /**
     * Force CPU-only mode for all devices
     * Useful for debugging or if GPU/NNAPI causes issues
     * Default: false
     */
    var forceCpuOnly: Boolean = false
    
    /**
     * Disable GPU delegate specifically
     * Keeps NNAPI enabled if available
     * Default: false
     */
    var disableGpuDelegate: Boolean = false
    
    /**
     * Disable NNAPI delegate specifically
     * GPU will still be attempted if enabled
     * Default: false
     */
    var disableNnapiDelegate: Boolean = false
    
    /**
     * Print debug logs for accelerator selection and performance
     * Default: true
     */
    var debugLogging: Boolean = true
    
    /**
     * Enable aggressive optimization for low-end devices
     * Reduces memory usage and processes fewer frames
     * Default: true
     */
    var optimizeForLowEndDevices: Boolean = true
    
    /**
     * Maximum memory (in MB) to allocate for the interpreter
     * Low-end devices will use less aggressive settings
     * Default: 0 (use system defaults)
     */
    var maxMemoryAllocation: Int = 0
    
    override fun toString(): String {
        return """
            ╔════════════════════════════════════════════════╗
            ║        TFLite Accelerator Configuration        ║
            ╠════════════════════════════════════════════════╣
            ║ GPU Acceleration:        $enableGpu
            ║ NNAPI Acceleration:      $enableNnapi
            ║ CPU Fallback:            $enableCpuFallback
            ║ CPU Threads:             $cpuThreadCount
            ║ Force CPU Only:          $forceCpuOnly
            ║ Disable GPU Delegate:    $disableGpuDelegate
            ║ Disable NNAPI Delegate:  $disableNnapiDelegate
            ║ Debug Logging:           $debugLogging
            ║ Optimize Low-End:        $optimizeForLowEndDevices
            ║ Max Memory (MB):         $maxMemoryAllocation
            ╚════════════════════════════════════════════════╝
        """.trimIndent()
    }
}
