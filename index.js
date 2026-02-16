import { NativeModules } from 'react-native';
import { VisionCameraProxy } from 'react-native-vision-camera';

const { FaceAntiSpoof } = NativeModules;

const _faceAntiSpoofPlugin = VisionCameraProxy.initFrameProcessorPlugin('faceAntiSpoof', {});

// Worklet called by VisionCamera
export const faceAntiSpoofFrameProcessor = function (frame) {
  'worklet';
  try {
    if (!_faceAntiSpoofPlugin) {
      console.warn('[FaceAntiSpoof] Plugin not initialized');
      return null;
    }
    
    if (typeof _faceAntiSpoofPlugin.call !== 'function') {
      console.warn('[FaceAntiSpoof] Plugin call method not available');
      return null;
    }
    
    if (!frame) {
      console.warn('[FaceAntiSpoof] Frame is null or undefined');
      return null;
    }
    
    const result = _faceAntiSpoofPlugin.call(frame);
    
    // Validate result structure
    if (result && typeof result === 'object') {
      return result;
    }
    
    return null;
  } catch (err) {
    console.error('[FaceAntiSpoof] Error in frame processor:', err);
    return null;
  }
};

export default FaceAntiSpoof;

// Utility functions
export const isFaceAntiSpoofAvailable = () => {
  return !!FaceAntiSpoof;
};

export const initializeFaceAntiSpoof = async () => {
  try {
    if (!FaceAntiSpoof) {
      throw new Error('FaceAntiSpoof module not available on this platform');
    }
    
    const result = await FaceAntiSpoof.initialize();
    const status = await FaceAntiSpoof.checkModelStatus();

    if (!result || !status) {
      throw new Error('Initialization returned empty result');
    }

    try {
      if (typeof FaceAntiSpoof.install === 'function') {
        await FaceAntiSpoof.install();
      }
    } catch (e) {
      console.warn('[FaceAntiSpoof] install() failed:', e);
    }

    const isSuccess = result && status.pluginAvailable;
    console.log('[FaceAntiSpoof] Initialization result:', isSuccess);
    return isSuccess;
  } catch (error) {
    console.error('[FaceAntiSpoof] Initialization failed:', error);
    throw error;
  }
};
