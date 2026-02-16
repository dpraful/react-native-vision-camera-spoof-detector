package com.faceantispoof

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import com.mrousavy.camera.frameprocessors.FrameProcessorPluginRegistry

class FaceAntiSpoofPackage : ReactPackage {

    companion object {
        init {
            try {
                FrameProcessorPluginRegistry.addFrameProcessorPlugin("faceAntiSpoof") { proxy, options ->
                    val plugin = FaceAntiSpoofFrameProcessor()
                    try {
                        plugin.initialize(proxy.context.assets)
                    } catch (e: Exception) {
                        android.util.Log.e("FaceAntiSpoof", "Failed to initialize plugin with assets", e)
                    }
                    plugin
                }
            } catch (e: Exception) {
                android.util.Log.e("FaceAntiSpoof", "Plugin already registered or failed: ${e.message}", e)
            }
        }
    }

    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        // Only return the module — DO NOT register plugin here.
        return listOf(FaceAntiSpoofModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
