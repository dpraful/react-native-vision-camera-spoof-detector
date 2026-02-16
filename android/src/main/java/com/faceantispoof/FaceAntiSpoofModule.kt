package com.faceantispoof

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = FaceAntiSpoofModule.NAME)
class FaceAntiSpoofModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "FaceAntiSpoof"
    }

    private var faceAntiSpoofing: FaceAntiSpoofingAdvanced? = null
    private var isModuleInitialized = false

    init {
        try {
            System.loadLibrary("faceantispoof")
        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "Error loading native library", e)
        }

        try {
            // Initialize face anti-spoofing with app assets
            faceAntiSpoofing = FaceAntiSpoofingAdvanced(reactContext.assets)
            isModuleInitialized = true
            android.util.Log.i("FaceAntiSpoof", "Face anti-spoofing initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "Failed to initialize face anti-spoofing: ${e.message}", e)
            isModuleInitialized = false
        }

        try {
            tryRegisterJSI()
        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "Error starting JSI registration", e)
        }
    }

    private fun tryRegisterJSI() {
        try {
            Thread {
                try {
                    Thread.sleep(500)
                    val holder = reactApplicationContext.javaScriptContextHolder ?: return@Thread

                    // Try nativePointer
                    try {
                        val field = holder.javaClass.getDeclaredField("nativePointer")
                        field.isAccessible = true
                        val jsContextPtr = field.getLong(holder)
                        if (jsContextPtr != 0L) {
                            try {
                                registerJSIFunction(jsContextPtr)
                            } catch (e: Exception) {
                                android.util.Log.d("FaceAntiSpoof", "JSI registration not available: ${e.message}")
                            }
                            return@Thread
                        }
                    } catch (_: Exception) { }

                    // Try mContext
                    try {
                        val field = holder.javaClass.getDeclaredField("mContext")
                        field.isAccessible = true
                        val jsContextPtr = field.getLong(holder)
                        if (jsContextPtr != 0L) {
                            try {
                                registerJSIFunction(jsContextPtr)
                            } catch (e: Exception) {
                                android.util.Log.d("FaceAntiSpoof", "JSI registration not available: ${e.message}")
                            }
                            return@Thread
                        }
                    } catch (_: Exception) { }

                } catch (e: Exception) {
                    android.util.Log.d("FaceAntiSpoof", "Error in JSI registration thread: ${e.message}")
                }
            }.start()
        } catch (e: Exception) {
            android.util.Log.d("FaceAntiSpoof", "Failed to start thread for JSI registration: ${e.message}")
        }
    }

    override fun getName(): String = NAME

    @ReactMethod
    fun initialize(promise: Promise) {
        try {
            if (!isModuleInitialized) {
                // Try to initialize now if not done in init()
                try {
                    faceAntiSpoofing = FaceAntiSpoofingAdvanced(reactApplicationContext.assets)
                    isModuleInitialized = true
                    android.util.Log.i("FaceAntiSpoof", "Face anti-spoofing initialized in initialize()")
                } catch (e: Exception) {
                    android.util.Log.e("FaceAntiSpoof", "Failed to initialize face anti-spoofing: ${e.message}", e)
                    promise.reject("INIT_ERROR", "Failed to initialize face anti-spoof: ${e.message}", e)
                    return
                }
            }
            promise.resolve(isModuleInitialized)
        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "initialize() error", e)
            promise.reject("INIT_ERROR", "Failed to initialize face anti-spoof", e)
        }
    }

    @ReactMethod
    fun checkModelStatus(promise: Promise) {
        try {
            val isInitialized = isModuleInitialized && faceAntiSpoofing?.isInitialized() == true
            val result = mapOf(
                "pluginAvailable" to isInitialized,
                "modelLoaded" to isInitialized,
                "moduleInitialized" to isModuleInitialized
            )
            promise.resolve(com.facebook.react.bridge.Arguments.makeNativeMap(result))
        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "checkModelStatus() error", e)
            promise.reject("STATUS_ERROR", "Failed to check model status", e)
        }
    }

    @ReactMethod
    fun isAvailable(promise: Promise) {
        try {
            val available = isModuleInitialized && faceAntiSpoofing?.isInitialized() == true
            promise.resolve(available)
        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "isAvailable() error", e)
            promise.reject("AVAIL_ERROR", "Failed to check availability", e)
        }
    }

    @ReactMethod
    fun testMethod(promise: Promise) {
        try {
            promise.resolve("Native module is working!")
        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "testMethod() error", e)
            promise.reject("TEST_ERROR", "Failed executing testMethod", e)
        }
    }

    @ReactMethod
    fun getModuleInfo(promise: Promise) {
        try {
            val info = mapOf(
                "name" to NAME,
                "methods" to listOf("initialize", "checkModelStatus", "isAvailable", "testMethod", "getModuleInfo")
            )
            promise.resolve(com.facebook.react.bridge.Arguments.makeNativeMap(info))
        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "getModuleInfo() error", e)
            promise.reject("INFO_ERROR", "Failed to get module info", e)
        }
    }

    @ReactMethod
    fun install(promise: Promise) {
        try {
            val holder = reactApplicationContext.javaScriptContextHolder

            if (holder != null) {
                // Try nativePointer
                try {
                    val f = holder.javaClass.getDeclaredField("nativePointer")
                    f.isAccessible = true
                    val ptr = f.getLong(holder)
                    if (ptr != 0L) {
                        try {
                            registerJSIFunction(ptr)
                            promise.resolve(true)
                            return
                        } catch (e: Exception) {
                            android.util.Log.d("FaceAntiSpoof", "JSI not available: ${e.message}")
                        }
                    }
                } catch (_: Exception) {}

                // Try mContext
                try {
                    val f = holder.javaClass.getDeclaredField("mContext")
                    f.isAccessible = true
                    val ptr = f.getLong(holder)
                    if (ptr != 0L) {
                        try {
                            registerJSIFunction(ptr)
                            promise.resolve(true)
                            return
                        } catch (e: Exception) {
                            android.util.Log.d("FaceAntiSpoof", "JSI not available: ${e.message}")
                        }
                    }
                } catch (_: Exception) {}

                promise.resolve(false)
            } else {
                promise.resolve(false)
            }

        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "install() error", e)
            promise.reject("INSTALL_ERROR", "Failed to install JSI function", e)
        }
    }

    @ReactMethod
    fun cleanup(promise: Promise) {
        try {
            faceAntiSpoofing?.close()
            faceAntiSpoofing = null
            isModuleInitialized = false
            android.util.Log.i("FaceAntiSpoof", "Cleanup completed")
            promise.resolve(true)
        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "cleanup() error", e)
            promise.reject("CLEANUP_ERROR", "Failed to cleanup face anti-spoof", e)
        }
    }

    override fun onCatalystInstanceDestroy() {
        // Cleanup when React Native is shutting down
        try {
            faceAntiSpoofing?.close()
            faceAntiSpoofing = null
            isModuleInitialized = false
            android.util.Log.i("FaceAntiSpoof", "onCatalystInstanceDestroy cleanup completed")
        } catch (e: Exception) {
            android.util.Log.e("FaceAntiSpoof", "Error in onCatalystInstanceDestroy", e)
        }
        super.onCatalystInstanceDestroy()
    }

    // Native JSI registration - optional, may not be available
    private external fun registerJSIFunction(jsContext: Long)
}