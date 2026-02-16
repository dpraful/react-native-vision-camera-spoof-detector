package com.faceantispoof

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.util.Log

/**
 * DEPRECATED: Use YuvToFloatArrayConverter instead for Bitmap-free processing.
 * 
 * Lightweight YUV -> RGB converter using RenderScript intrinsics.
 * Based on CameraX sample implementation; faster than compressing to JPEG.
 * This class creates Bitmaps which should be avoided in performance-critical code.
 */
@Deprecated("Use YuvToFloatArrayConverter for direct ByteBuffer/FloatArray processing without Bitmaps", ReplaceWith("YuvToFloatArrayConverter"))
class YuvToRgbConverter(context: Context) {
    private val rs: RenderScript = RenderScript.create(context.applicationContext)
    private val scriptYuvToRgb: ScriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    private var yuvBuffer: ByteArray = ByteArray(0)
    private var yuvAlloc: Allocation? = null
    private var rgbaAlloc: Allocation? = null

    init {
        Log.w("YuvToRgbConverter", "YuvToRgbConverter is deprecated. Use YuvToFloatArrayConverter instead.")
    }

    fun yuvToRgb(image: Image, output: Bitmap) {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val total = ySize + uSize + vSize

        if (yuvBuffer.size != total) {
            yuvBuffer = ByteArray(total)
        }

        yBuffer.get(yuvBuffer, 0, ySize)
        vBuffer.get(yuvBuffer, ySize, vSize)
        uBuffer.get(yuvBuffer, ySize + vSize, uSize)

        if (yuvAlloc == null || yuvAlloc!!.bytesSize < yuvBuffer.size) {
            try { yuvAlloc?.destroy() } catch (ignored: Throwable) {}
            yuvAlloc = Allocation.createSized(rs, Element.U8(rs), yuvBuffer.size)
        }

        yuvAlloc!!.copyFrom(yuvBuffer)
        scriptYuvToRgb.setInput(yuvAlloc)

        // Create or reuse rgba allocation tied to the output bitmap
        if (rgbaAlloc == null || rgbaAlloc!!.type.x != output.width || rgbaAlloc!!.type.y != output.height) {
            try { rgbaAlloc?.destroy() } catch (ignored: Throwable) {}
            rgbaAlloc = Allocation.createFromBitmap(rs, output)
        }

        scriptYuvToRgb.forEach(rgbaAlloc)
        rgbaAlloc!!.copyTo(output)
    }

    fun destroy() {
        try { yuvAlloc?.destroy() } catch (ignored: Throwable) {}
        try { rgbaAlloc?.destroy() } catch (ignored: Throwable) {}
        try { rs.destroy() } catch (ignored: Throwable) {}
    }
}
