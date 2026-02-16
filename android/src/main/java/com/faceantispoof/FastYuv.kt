package com.faceantispoof

@Suppress("unused")
object FastYuv {
    init {
        System.loadLibrary("fastyuv")
    }

    @JvmStatic
    external fun nativeNV21ToARGB(
        nv21: ByteArray,
        width: Int,
        height: Int,
        outArgb: ByteArray
    ): Boolean

    @JvmStatic
    external fun nativeNV21ToRGB(
        nv21: ByteArray,
        width: Int,
        height: Int,
        outRgb: ByteArray
    ): Boolean

    @JvmStatic
    external fun nativeNV21ToBGR(
        nv21: ByteArray,
        width: Int,
        height: Int,
        outBgr: ByteArray
    ): Boolean

    @JvmStatic
    external fun nativeNV21ToARGBResize(
        nv21: ByteArray,
        srcW: Int,
        srcH: Int,
        outArgb: ByteArray,
        dstW: Int,
        dstH: Int
    ): Boolean

    @JvmStatic
    external fun nativeNV21ToARGBRotate(
        nv21: ByteArray,
        srcW: Int,
        srcH: Int,
        outArgb: ByteArray,
        rotation: Int
    ): Boolean
}
