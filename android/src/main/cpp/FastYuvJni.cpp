#include <jni.h>
#include <android/log.h>
#include <string.h>
#include "fast_yuv.h"

#define LOG_TAG "FastYuvJNI"
#define ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_visioncamera_fastyuv_FastYuv_nativeNV21ToARGB(JNIEnv* env, jclass clazz,
                                                                                jbyteArray nv21Array,
                                                                                jint width, jint height,
                                                                                jbyteArray outArgbArray) {
    if (!nv21Array || !outArgbArray) return JNI_FALSE;
    jsize in_len = env->GetArrayLength(nv21Array);
    jsize out_len = env->GetArrayLength(outArgbArray);
    int expected_in = width * height + (width * height) / 2;
    int expected_out = width * height * 4;
    if (in_len < expected_in || out_len < expected_out) return JNI_FALSE;

    jbyte* in_bytes = env->GetByteArrayElements(nv21Array, nullptr);
    jbyte* out_bytes = env->GetByteArrayElements(outArgbArray, nullptr);
    bool ok = fastyuv::NV21ToARGB((const uint8_t*)in_bytes, width, height, (uint8_t*)out_bytes);
    env->ReleaseByteArrayElements(nv21Array, in_bytes, JNI_ABORT);
    env->ReleaseByteArrayElements(outArgbArray, out_bytes, 0);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_visioncamera_fastyuv_FastYuv_nativeNV21ToRGB(JNIEnv* env, jclass clazz,
                                                                               jbyteArray nv21Array,
                                                                               jint width, jint height,
                                                                               jbyteArray outRgbArray) {
    if (!nv21Array || !outRgbArray) return JNI_FALSE;
    jsize in_len = env->GetArrayLength(nv21Array);
    jsize out_len = env->GetArrayLength(outRgbArray);
    int expected_in = width * height + (width * height) / 2;
    int expected_out = width * height * 3;
    if (in_len < expected_in || out_len < expected_out) return JNI_FALSE;

    jbyte* in_bytes = env->GetByteArrayElements(nv21Array, nullptr);
    jbyte* out_bytes = env->GetByteArrayElements(outRgbArray, nullptr);
    bool ok = fastyuv::NV21ToRGB24((const uint8_t*)in_bytes, width, height, (uint8_t*)out_bytes);
    env->ReleaseByteArrayElements(nv21Array, in_bytes, JNI_ABORT);
    env->ReleaseByteArrayElements(outRgbArray, out_bytes, 0);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_visioncamera_fastyuv_FastYuv_nativeNV21ToBGR(JNIEnv* env, jclass clazz,
                                                                               jbyteArray nv21Array,
                                                                               jint width, jint height,
                                                                               jbyteArray outBgrArray) {
    if (!nv21Array || !outBgrArray) return JNI_FALSE;
    jsize in_len = env->GetArrayLength(nv21Array);
    jsize out_len = env->GetArrayLength(outBgrArray);
    int expected_in = width * height + (width * height) / 2;
    int expected_out = width * height * 3;
    if (in_len < expected_in || out_len < expected_out) return JNI_FALSE;

    jbyte* in_bytes = env->GetByteArrayElements(nv21Array, nullptr);
    jbyte* out_bytes = env->GetByteArrayElements(outBgrArray, nullptr);
    bool ok = fastyuv::NV21ToBGR24((const uint8_t*)in_bytes, width, height, (uint8_t*)out_bytes);
    env->ReleaseByteArrayElements(nv21Array, in_bytes, JNI_ABORT);
    env->ReleaseByteArrayElements(outBgrArray, out_bytes, 0);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_visioncamera_fastyuv_FastYuv_nativeNV21ToARGBResize(JNIEnv* env, jclass clazz,
                                                                                       jbyteArray nv21Array,
                                                                                       jint src_w, jint src_h,
                                                                                       jbyteArray outArgbArray,
                                                                                       jint dst_w, jint dst_h) {
    if (!nv21Array || !outArgbArray) return JNI_FALSE;
    jsize in_len = env->GetArrayLength(nv21Array);
    jsize out_len = env->GetArrayLength(outArgbArray);
    int expected_in = src_w * src_h + (src_w * src_h) / 2;
    int expected_out = dst_w * dst_h * 4;
    if (in_len < expected_in || out_len < expected_out) return JNI_FALSE;

    jbyte* in_bytes = env->GetByteArrayElements(nv21Array, nullptr);
    jbyte* out_bytes = env->GetByteArrayElements(outArgbArray, nullptr);
    bool ok = fastyuv::NV21ToARGBResize((const uint8_t*)in_bytes, src_w, src_h, (uint8_t*)out_bytes, dst_w, dst_h);
    env->ReleaseByteArrayElements(nv21Array, in_bytes, JNI_ABORT);
    env->ReleaseByteArrayElements(outArgbArray, out_bytes, 0);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_visioncamera_fastyuv_FastYuv_nativeNV21ToARGBRotate(JNIEnv* env, jclass clazz,
                                                                                       jbyteArray nv21Array,
                                                                                       jint src_w, jint src_h,
                                                                                       jbyteArray outArgbArray,
                                                                                       jint rotation) {
    if (!nv21Array || !outArgbArray) return JNI_FALSE;
    jsize in_len = env->GetArrayLength(nv21Array);
    jsize out_len = env->GetArrayLength(outArgbArray);
    int expected_in = src_w * src_h + (src_w * src_h) / 2;
    // rotation may swap dims; allocate expected_out conservatively using max dims
    int dst_w = (rotation % 180 == 0) ? src_w : src_h;
    int dst_h = (rotation % 180 == 0) ? src_h : src_w;
    int expected_out = dst_w * dst_h * 4;
    if (in_len < expected_in || out_len < expected_out) return JNI_FALSE;

    jbyte* in_bytes = env->GetByteArrayElements(nv21Array, nullptr);
    jbyte* out_bytes = env->GetByteArrayElements(outArgbArray, nullptr);
    bool ok = fastyuv::NV21ToARGBRotate((const uint8_t*)in_bytes, src_w, src_h, (uint8_t*)out_bytes, rotation);
    env->ReleaseByteArrayElements(nv21Array, in_bytes, JNI_ABORT);
    env->ReleaseByteArrayElements(outArgbArray, out_bytes, 0);
    return ok ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
