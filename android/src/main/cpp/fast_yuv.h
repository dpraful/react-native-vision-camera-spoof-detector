#pragma once

#include <stdint.h>

// Lightweight helpers using libyuv. Implemented in fast_yuv.cpp.
namespace fastyuv {

// Convert NV21 (YUV420sp) to 3-channel RGB (RGB24). Returns true on success.
bool NV21ToRGB24(const uint8_t* nv21, int width, int height, uint8_t* dst_rgb);

// Convert NV21 to ARGB (4 bytes per pixel, order A,R,G,B)
bool NV21ToARGB(const uint8_t* nv21, int width, int height, uint8_t* dst_argb);

// Convert NV21 to BGR24
bool NV21ToBGR24(const uint8_t* nv21, int width, int height, uint8_t* dst_bgr);

// Resize NV21 -> ARGB with scaling to dst_w x dst_h
bool NV21ToARGBResize(const uint8_t* nv21, int src_w, int src_h, uint8_t* dst_argb, int dst_w, int dst_h);

// Rotate NV21 to ARGB with rotation 0/90/180/270
bool NV21ToARGBRotate(const uint8_t* nv21, int src_w, int src_h, uint8_t* dst_argb, int rotation);

} // namespace fastyuv
