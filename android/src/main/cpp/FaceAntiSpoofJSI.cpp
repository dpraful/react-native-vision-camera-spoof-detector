#include <jni.h>
#include <jsi/jsi.h>
#include <string>
#include <android/log.h>

using namespace facebook;

// Logging macro
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "FaceAntiSpoofJSI", __VA_ARGS__))

// Dummy frame processing function
jsi::Value faceAntiSpoof(jsi::Runtime &runtime, const jsi::Value &frame) {
    LOGI("FaceAntiSpoofJSI: frame received");

    jsi::Object result(runtime);
    result.setProperty(runtime, "isLive", true);
    result.setProperty(runtime, "label", "Live");
    result.setProperty(runtime, "confidence", 0.99);

    return result;
}

// JSI registration function
extern "C"
JNIEXPORT void JNICALL
Java_com_faceantispoof_FaceAntiSpoofModule_registerJSIFunction(JNIEnv *env, jobject thiz, jlong jsContextPtr) {
    LOGI("registerJSIFunction called with jsContextPtr = %ld", (long)jsContextPtr);
    
    if (jsContextPtr == 0) {
        LOGI("FaceAntiSpoofJSI: JS runtime pointer is 0 (null)!");
        return;
    }
    
    auto *runtime = reinterpret_cast<jsi::Runtime *>(jsContextPtr);
    if (!runtime) {
        LOGI("FaceAntiSpoofJSI: Runtime reinterpret_cast resulted in null pointer!");
        return;
    }

    try {
        LOGI("FaceAntiSpoofJSI: Creating JSI function...");
        
        jsi::Function func = jsi::Function::createFromHostFunction(
            *runtime,
            jsi::PropNameID::forAscii(*runtime, "__faceAntiSpoof"),
            1,
            [](jsi::Runtime &rt, const jsi::Value &thisValue, const jsi::Value *args, size_t count) -> jsi::Value {
                LOGI("FaceAntiSpoofJSI: __faceAntiSpoof function called");
                if (count < 1) {
                    jsi::Object error(rt);
                    error.setProperty(rt, "isLive", false);
                    error.setProperty(rt, "label", "No frame provided");
                    error.setProperty(rt, "confidence", 0.0);
                    return error;
                }
                return faceAntiSpoof(rt, args[0]);
            }
        );

        LOGI("FaceAntiSpoofJSI: Setting __faceAntiSpoof on global object...");
        runtime->global().setProperty(*runtime, "__faceAntiSpoof", std::move(func));
        LOGI("FaceAntiSpoofJSI: __faceAntiSpoof registered successfully!");
    } catch (const std::exception &e) {
        LOGI("FaceAntiSpoofJSI: Exception during JSI registration: %s", e.what());
    } catch (...) {
        LOGI("FaceAntiSpoofJSI: Unknown exception during JSI registration!");
    }
}
