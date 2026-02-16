package com.faceantispoof

import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

/**
 * Buffer pool manager for efficient reuse of large byte arrays.
 * Reduces garbage collection pressure during continuous frame processing.
 */
class ByteBufferPool(private val bufferSize: Int) {
    companion object {
        private const val TAG = "ByteBufferPool"
    }

    private val availableBuffers = mutableListOf<ByteArray>()
    private val allocatedCount = AtomicInteger(0)
    private val maxPoolSize = 3  // Keep max 3 buffers in pool

    /**
     * Acquire a buffer from the pool or allocate a new one if needed.
     */
    fun acquire(): ByteArray {
        synchronized(availableBuffers) {
            val buffer = if (availableBuffers.isNotEmpty()) {
                availableBuffers.removeAt(0)
            } else {
                allocatedCount.incrementAndGet()
                Log.d(TAG, "Allocated new buffer ($allocatedCount allocated)")
                ByteArray(bufferSize)
            }
            return buffer
        }
    }

    /**
     * Return a buffer to the pool for reuse.
     */
    fun release(buffer: ByteArray) {
        if (buffer.size != bufferSize) {
            Log.w(TAG, "Buffer size mismatch: expected $bufferSize, got ${buffer.size}")
            return
        }

        synchronized(availableBuffers) {
            if (availableBuffers.size < maxPoolSize) {
                availableBuffers.add(buffer)
                Log.d(TAG, "Buffer returned to pool (available: ${availableBuffers.size})")
            } else {
                Log.d(TAG, "Pool is full, discarding buffer")
            }
        }
    }

    /**
     * Get current pool statistics.
     */
    fun getStats(): String {
        return "ByteBufferPool(size=$bufferSize, allocated=$allocatedCount, available=${availableBuffers.size})"
    }

    fun clear() {
        synchronized(availableBuffers) {
            availableBuffers.clear()
            Log.d(TAG, "Buffer pool cleared")
        }
    }
}

