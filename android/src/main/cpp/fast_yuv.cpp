#include "fast_yuv.h"
#include <stdlib.h>
#include <string.h>
#include <vector>
#include <unordered_map>
#include <mutex>

// Include specific libyuv headers with SIMD optimizations.
// libyuv internally uses NEON for ARM and other SIMD extensions.
#include "libyuv/convert.h"
#include "libyuv/convert_argb.h"
#include "libyuv/convert_from.h"
#include "libyuv/scale.h"
#include "libyuv/rotate.h"

namespace fastyuv {

// Simple memory pool for reducing allocation overhead in video processing
class BufferPool {
private:
    std::unordered_map<size_t, std::vector<uint8_t*>> pool;
    std::mutex pool_mutex;
    static constexpr int MAX_BUFFERS_PER_SIZE = 4;  // Keep up to 4 buffers per size

public:
    uint8_t* allocate(size_t size) {
        std::lock_guard<std::mutex> guard(pool_mutex);
        auto& buffers = pool[size];
        if (!buffers.empty()) {
            uint8_t* buf = buffers.back();
            buffers.pop_back();
            return buf;
        }
        return (uint8_t*)malloc(size);
    }

    void deallocate(uint8_t* ptr, size_t size) {
        if (!ptr) return;
        std::lock_guard<std::mutex> guard(pool_mutex);
        auto& buffers = pool[size];
        if (buffers.size() < MAX_BUFFERS_PER_SIZE) {
            buffers.push_back(ptr);
        } else {
            free(ptr);
        }
    }

    static BufferPool& getInstance() {
        static BufferPool instance;
        return instance;
    }
};

// RAII helper for automatic buffer management
class BufferGuard {
private:
    uint8_t* ptr;
    size_t size;

public:
    BufferGuard(size_t size) : size(size) {
        ptr = BufferPool::getInstance().allocate(size);
    }
    ~BufferGuard() {
        BufferPool::getInstance().deallocate(ptr, size);
    }
    uint8_t* get() { return ptr; }
    operator uint8_t*() { return ptr; }
};

bool NV21ToRGB24(const uint8_t* nv21, int width, int height, uint8_t* dst_rgb) {
    if (!nv21 || !dst_rgb || width <= 0 || height <= 0) return false;

    // FIXED: libyuv NV21->I420->ARGB pipeline
    // libyuv provides the most reliable conversion with NEON/SIMD optimization
    int y_size = width * height;
    int i420_u_size = (width/2) * (height/2);

    BufferGuard i420_y_guard(y_size);
    BufferGuard i420_u_guard(i420_u_size);
    BufferGuard i420_v_guard(i420_u_size);
    
    uint8_t* i420_y = i420_y_guard.get();
    uint8_t* i420_u = i420_u_guard.get();
    uint8_t* i420_v = i420_v_guard.get();

    if (!i420_y || !i420_u || !i420_v) {
        return false;
    }

    // NV21: Y plane, then interleaved VU (V first)
    // CRITICAL: src_vu stride is 2 (interleaved UV pairs), not just width
    const uint8_t* src_y = nv21;
    const uint8_t* src_vu = nv21 + y_size;

    // Convert NV21 to I420 (planar YUV)
    // NV21ToI420 correctly handles the interleaved VU data
    int res = libyuv::NV21ToI420(src_y, width,
                                 src_vu, width,  // stride = width (interleaved)
                                 i420_y, width,
                                 i420_u, width/2,
                                 i420_v, width/2,
                                 width, height);

    if (res != 0) {
        return false;
    }

    // Convert I420 -> ARGB with proper channel ordering
    // ARGB layout: A(unused) R G B per pixel
    int pixel_count = width * height;
    
    BufferGuard tmp_argb_guard(pixel_count * 4);
    uint8_t* tmp_argb = tmp_argb_guard.get();
    if (!tmp_argb) { return false; }

    res = libyuv::I420ToARGB(i420_y, width,
                             i420_u, width/2,
                             i420_v, width/2,
                             tmp_argb, width * 4,
                             width, height);
    
    if (res == 0) {
        // Extract RGB from ARGB: ARGB format is [A][R][G][B] per pixel
        // tmp_argb[i*4 + 0] = A (unused alpha)
        // tmp_argb[i*4 + 1] = R
        // tmp_argb[i*4 + 2] = G
        // tmp_argb[i*4 + 3] = B
        for (int i = 0; i < pixel_count; ++i) {
            dst_rgb[i*3 + 0] = tmp_argb[i*4 + 1];  // R
            dst_rgb[i*3 + 1] = tmp_argb[i*4 + 2];  // G
            dst_rgb[i*3 + 2] = tmp_argb[i*4 + 3];  // B
        }
    }

    return res == 0;
}

bool NV21ToARGB(const uint8_t* nv21, int width, int height, uint8_t* dst_argb) {
    if (!nv21 || !dst_argb || width <= 0 || height <= 0) return false;

    int y_size = width * height;
    int uv_size = (width/2) * (height/2);
    
    // Use buffer pool for I420 intermediate buffers
    BufferGuard i420_y_guard(y_size);
    BufferGuard i420_u_guard(uv_size);
    BufferGuard i420_v_guard(uv_size);
    
    uint8_t* i420_y = i420_y_guard.get();
    uint8_t* i420_u = i420_u_guard.get();
    uint8_t* i420_v = i420_v_guard.get();
    
    if (!i420_y || !i420_u || !i420_v) { return false; }

    const uint8_t* src_y = nv21;
    const uint8_t* src_vu = nv21 + y_size;

    // libyuv handles NEON/SIMD optimizations internally on ARM
    int res = libyuv::NV21ToI420(src_y, width,
                                 src_vu, width,
                                 i420_y, width,
                                 i420_u, width/2,
                                 i420_v, width/2,
                                 width, height);

    if (res != 0) { return false; }

    // Fast NEON-optimized I420 to ARGB conversion
    res = libyuv::I420ToARGB(i420_y, width,
                             i420_u, width/2,
                             i420_v, width/2,
                             dst_argb, width * 4,
                             width, height);

    return res == 0;
}

bool NV21ToBGR24(const uint8_t* nv21, int width, int height, uint8_t* dst_bgr) {
    // Convert to RGB24 then swap channels with SIMD-friendly loop
    if (!nv21 || !dst_bgr) return false;
    int pixel_count = width * height;
    
    BufferGuard tmp_guard(pixel_count * 3);
    uint8_t* tmp = tmp_guard.get();
    if (!tmp) return false;
    
    bool ok = NV21ToRGB24(nv21, width, height, tmp);
    if (!ok) { return false; }
    
    // Swap R<->B with vectorizable loop
    for (int i = 0; i < pixel_count; ++i) {
        dst_bgr[i*3 + 0] = tmp[i*3 + 2];
        dst_bgr[i*3 + 1] = tmp[i*3 + 1];
        dst_bgr[i*3 + 2] = tmp[i*3 + 0];
    }
    return true;
}

bool NV21ToARGBResize(const uint8_t* nv21, int src_w, int src_h, uint8_t* dst_argb, int dst_w, int dst_h) {
    if (!nv21 || !dst_argb || src_w <= 0 || src_h <= 0 || dst_w <= 0 || dst_h <= 0) return false;

    int src_y_size = src_w * src_h;
    int src_uv_size = (src_w/2) * (src_h/2);
    
    BufferGuard i420_y_guard(src_y_size);
    BufferGuard i420_u_guard(src_uv_size);
    BufferGuard i420_v_guard(src_uv_size);
    
    uint8_t* i420_y = i420_y_guard.get();
    uint8_t* i420_u = i420_u_guard.get();
    uint8_t* i420_v = i420_v_guard.get();
    
    if (!i420_y || !i420_u || !i420_v) { return false; }

    const uint8_t* src_y = nv21;
    const uint8_t* src_vu = nv21 + src_y_size;

    int res = libyuv::NV21ToI420(src_y, src_w,
                                 src_vu, src_w,
                                 i420_y, src_w,
                                 i420_u, src_w/2,
                                 i420_v, src_w/2,
                                 src_w, src_h);
    if (res != 0) { return false; }

    // Allocate scaled I420
    int dst_y_size = dst_w * dst_h;
    int dst_uv_size = (dst_w/2)*(dst_h/2);
    
    BufferGuard dst_y_guard(dst_y_size);
    BufferGuard dst_u_guard(dst_uv_size);
    BufferGuard dst_v_guard(dst_uv_size);
    
    uint8_t* dst_y = dst_y_guard.get();
    uint8_t* dst_u = dst_u_guard.get();
    uint8_t* dst_v = dst_v_guard.get();
    
    if (!dst_y || !dst_u || !dst_v) { return false; }

    // libyuv scale uses NEON/SIMD internally
    res = libyuv::I420Scale(i420_y, src_w,
                           i420_u, src_w/2,
                           i420_v, src_w/2,
                           src_w, src_h,
                           dst_y, dst_w,
                           dst_u, dst_w/2,
                           dst_v, dst_w/2,
                           dst_w, dst_h,
                           libyuv::kFilterBox);

    if (res != 0) { return false; }

    // Convert scaled I420 to ARGB with NEON acceleration
    res = libyuv::I420ToARGB(dst_y, dst_w,
                             dst_u, dst_w/2,
                             dst_v, dst_w/2,
                             dst_argb, dst_w * 4,
                             dst_w, dst_h);

    return res == 0;
}

bool NV21ToARGBRotate(const uint8_t* nv21, int src_w, int src_h, uint8_t* dst_argb, int rotation) {
    if (!nv21 || !dst_argb) return false;
    // rotation must be 0/90/180/270
    int rot = rotation % 360;
    if (rot < 0) rot += 360;
    if (rot % 90 != 0) return false;

    int y_size = src_w * src_h;
    int uv_size = (src_w/2)*(src_h/2);
    
    BufferGuard i420_y_guard(y_size);
    BufferGuard i420_u_guard(uv_size);
    BufferGuard i420_v_guard(uv_size);
    
    uint8_t* i420_y = i420_y_guard.get();
    uint8_t* i420_u = i420_u_guard.get();
    uint8_t* i420_v = i420_v_guard.get();
    
    if (!i420_y || !i420_u || !i420_v) { return false; }

    const uint8_t* src_y = nv21;
    const uint8_t* src_vu = nv21 + y_size;

    int res = libyuv::NV21ToI420(src_y, src_w,
                                 src_vu, src_w,
                                 i420_y, src_w,
                                 i420_u, src_w/2,
                                 i420_v, src_w/2,
                                 src_w, src_h);
    if (res != 0) { return false; }

    // Rotate I420
    int dst_w = (rot == 90 || rot == 270) ? src_h : src_w;
    int dst_h = (rot == 90 || rot == 270) ? src_w : src_h;

    int dst_y_size = dst_w * dst_h;
    int dst_uv_size = (dst_w/2)*(dst_h/2);
    
    BufferGuard ry_guard(dst_y_size);
    BufferGuard ru_guard(dst_uv_size);
    BufferGuard rv_guard(dst_uv_size);
    
    uint8_t* ry = ry_guard.get();
    uint8_t* ru = ru_guard.get();
    uint8_t* rv = rv_guard.get();
    
    if (!ry || !ru || !rv) { return false; }

    libyuv::RotationMode mode = libyuv::kRotate0;
    switch (rot) {
        case 0: mode = libyuv::kRotate0; break;
        case 90: mode = libyuv::kRotate90; break;
        case 180: mode = libyuv::kRotate180; break;
        case 270: mode = libyuv::kRotate270; break;
    }

    // I420Rotate uses NEON on ARM
    res = libyuv::I420Rotate(i420_y, src_w,
                             i420_u, src_w/2,
                             i420_v, src_w/2,
                             ry, dst_w,
                             ru, dst_w/2,
                             rv, dst_w/2,
                             src_w, src_h,
                             mode);

    if (res != 0) { return false; }

    // Convert rotated I420 to ARGB with NEON acceleration
    res = libyuv::I420ToARGB(ry, dst_w,
                             ru, dst_w/2,
                             rv, dst_w/2,
                             dst_argb, dst_w * 4,
                             dst_w, dst_h);

    return res == 0;
}

} // namespace fastyuv
