/**
 * React Native Vision Camera Face Anti-Spoofing Detector
 * High-performance face anti-spoofing and liveness detection module
 * TypeScript type definitions
 */

import type { Frame, FrameProcessor } from 'react-native-vision-camera';
import type { SharedValue } from 'react-native-reanimated';

// ============================================================================
// Core Detection Results
// ============================================================================

/**
 * Face anti-spoofing detection result
 * Contains comprehensive information about face liveness detection
 */
export interface FaceAntiSpoofingResult {
  /**
   * Whether the face is live (real) or a spoof attempt
   * @true - Face is real/live
   * @false - Face is likely a spoof/fake
   */
  isLive: boolean;

  /**
   * Human-readable label
   * @values "Live Face" | "Spoof Face"
   */
  label: string;

  /**
   * Neural network confidence score
   * Range: 0.0 to 1.0
   * Lower values indicate more likely to be live
   */
  neuralNetworkScore: number;

  /**
   * Laplacian variance-based image quality score
   * Range: 0.0 to 5000+
   * Higher values = better image quality
   * Used to distinguish blur from spoofing
   */
  laplacianScore: number;

  /**
   * Combined weighted score from multiple models
   * Range: 0.0 to 1.0
   * Provides robust detection by ensemble voting
   */
  combinedScore: number;

  /**
   * Error message if detection failed
   * @optional Only present if an error occurred
   */
  error?: string;
}

/**
 * Face rectangle/bounds information
 */
export interface FaceRect {
  /** X coordinate in pixels */
  x: number;
  /** Y coordinate in pixels */
  y: number;
  /** Face width in pixels */
  width: number;
  /** Face height in pixels */
  height: number;
}

// ============================================================================
// Anti-spoofing State & Configuration
// ============================================================================

/**
 * Anti-spoofing detection state
 */
export interface AntiSpoofState {
  /** Whether face is detected as live */
  isLive: boolean;
  /** Confidence score (0.0-1.0) */
  confidence: number;
  /** Consecutive frames detected as live */
  consecutiveLiveFrames: number;
  /** Last detection result */
  lastResult: FaceAntiSpoofingResult | null;
  /** Whether face is single and detected */
  hasSingleFace: boolean;
  /** Whether face is centered in frame */
  isFaceCentered: boolean;
}

/**
 * Face tracking state
 */
export interface FaceTrackingState {
  lastX: number;
  lastY: number;
  lastW: number;
  lastH: number;
  stableCount: number;
}

/**
 * Liveness detection state
 */
export interface LivenessState {
  level: number;
  step: number;
  blinkCount: number;
}

/**
 * Complete shared state for frame processing
 */
export interface SharedFrameProcessorState {
  lastProcessedTime: number;
  faceTracking: FaceTrackingState;
  flags: {
    captured: boolean;
    showCodeScanner: boolean;
    isActive: boolean;
    hasSingleFace: boolean;
    isFaceCentered: boolean;
    eyeClosed: boolean;
  };
  liveness: LivenessState;
  antiSpoof: AntiSpoofState;
  centering: {
    centeredFrames: number;
    frameWidth: number;
    frameHeight: number;
  };
  performance: {
    batchCounter: number;
    lastBatchUpdate: number;
  };
}

/**
 * Face detection update callback data
 */
export interface FacesUpdateData {
  count: number;
  progress: number;
  step: number;
  isCentered: boolean;
  antiSpoofState: AntiSpoofState;
}

/**
 * Anti-spoof update callback data
 */
export interface OnAntiSpoofUpdateData {
  isLive: boolean;
  confidence: number;
  rawResult?: FaceAntiSpoofingResult;
  consecutiveLiveFrames: number;
  isFaceCentered: boolean;
}

// ============================================================================
// Hook Configuration
// ============================================================================

/**
 * Configuration options for useFaceDetectionFrameProcessor hook
 */
export interface UseFaceDetectionFrameProcessorOptions {
  /**
   * Callback when a stable, live face is detected
   * @param faceRect - Face position and size
   * @param antiSpoofResult - Anti-spoofing detection result
   */
  onStableFaceDetected?: (
    faceRect: FaceRect,
    antiSpoofResult?: FaceAntiSpoofingResult
  ) => void;

  /**
   * Callback for face updates during detection
   * Called periodically with detection progress
   */
  onFacesUpdate?: (data: FacesUpdateData) => void;

  /**
   * Callback for liveness verification updates
   * @param step - Liveness step (0-2)
   * @param extra - Additional data like blink count
   */
  onLivenessUpdate?: (step: number, extra?: Record<string, any>) => void;

  /**
   * Callback for anti-spoof detection updates
   * Called when anti-spoofing detection result changes
   */
  onAntiSpoofUpdate?: (result: OnAntiSpoofUpdateData) => void;

  /**
   * Show code scanner overlay
   * @default false
   */
  showCodeScanner?: boolean;

  /**
   * Loading state - pauses frame processing
   * @default false
   */
  isLoading?: boolean;

  /**
   * Enable/disable frame processing
   * @default true
   */
  isActive?: boolean;

  /**
   * Liveness verification level
   * @values 0 - Anti-spoofing only (fast)
   * @values 1 - Anti-spoofing + blink detection (more thorough)
   * @default 0
   */
  livenessLevel?: number;

  /**
   * Anti-spoofing confidence threshold
   * Range: 0.0 - 1.0
   * Lower values = more lenient, higher values = stricter
   * @default 0.35
   */
  antispooflevel?: number;
}

/**
 * Return value from useFaceDetectionFrameProcessor hook
 */
export interface UseFaceDetectionFrameProcessorReturn {
  /**
   * Frame processor function for Vision Camera
   */
  frameProcessor: FrameProcessor;

  /**
   * Reset capture state to initial values
   */
  resetCaptureState: () => void;

  /**
   * Force reset all states including shared values
   */
  forceResetCaptureState: () => void;

  /**
   * Update code scanner visibility
   */
  updateShowCodeScanner: (value: boolean) => void;

  /**
   * Update processing active state
   */
  updateIsActive: (active: boolean) => void;

  /**
   * Initialize anti-spoofing module
   */
  initializeAntiSpoof: () => Promise<boolean>;

  /**
   * Shared value for capture state
   * Use .value to access: capturedSV.value.value
   */
  capturedSV: SharedValue<boolean>;

  /**
   * Current anti-spoofing detection state
   */
  antiSpoofState: AntiSpoofState;
}

// ============================================================================
// Module Functions
// ============================================================================

/**
 * Frame processor function for real-time face anti-spoofing detection
 * Call within a Vision Camera frame processor
 * 
 * @example
 * ```typescript
 * const frameProcessor = useFrameProcessor((frame) => {
 *   'worklet';
 *   const result = faceAntiSpoofFrameProcessor(frame);
 *   if (result?.isLive) {
 *     // Face is live
 *   }
 * }, []);
 * ```
 * 
 * @param frame - Vision Camera frame object
 * @returns Detection result or null if no face detected
 */
export function faceAntiSpoofFrameProcessor(
  frame: Frame
): FaceAntiSpoofingResult | null;

/**
 * Check if face anti-spoofing module is available on device
 * Call before attempting to use the module
 * 
 * @returns true if module is available, false otherwise
 */
export function isFaceAntiSpoofAvailable(): boolean;

/**
 * Initialize the face anti-spoofing module
 * Must be called before using the frame processor
 * 
 * @returns Promise<boolean> - true if initialization successful
 * 
 * @example
 * ```typescript
 * useEffect(() => {
 *   initializeFaceAntiSpoof()
 *     .then(success => {
 *       if (success) {
 *         console.log('Anti-spoof ready');
 *       }
 *     });
 * }, []);
 * ```
 */
export function initializeFaceAntiSpoof(): Promise<boolean>;

/**
 * Custom React Native hook for comprehensive face detection with anti-spoofing
 * Provides advanced features like blink detection and face centering
 * 
 * @param options - Configuration options
 * @returns Hook return value with frame processor and callbacks
 * 
 * @example
 * ```typescript
 * const { frameProcessor, resetCaptureState, antiSpoofState } = 
 *   useFaceDetectionFrameProcessor({
 *     livenessLevel: 1,
 *     antispooflevel: 0.35,
 *     onStableFaceDetected: (rect, result) => {
 *       console.log('Capture complete!', rect);
 *     },
 *   });
 * ```
 */
export function useFaceDetectionFrameProcessor(
  options: UseFaceDetectionFrameProcessorOptions
): UseFaceDetectionFrameProcessorReturn;

// ============================================================================
// Accelerator Types
// ============================================================================

/**
 * TensorFlow Lite accelerator type
 * Determines which hardware is used for inference
 */
export type AcceleratorType = 'CPU' | 'GPU' | 'NNAPI';

/**
 * Module information
 */
export interface ModuleInfo {
  /** Module name */
  name: string;
  /** Available API methods */
  methods: string[];
  /** Module version */
  version: string;
  /** Active accelerator (if available) */
  accelerator?: AcceleratorType;
}

/**
 * Model status information
 */
export interface ModelStatus {
  /** Whether plugin is available on this device */
  pluginAvailable: boolean;
  /** Whether TensorFlow Lite model is loaded */
  modelLoaded: boolean;
  /** Active accelerator type */
  accelerator?: AcceleratorType;
}

// ============================================================================
// Native Module Interface
// ============================================================================

/**
 * Native module interface for advanced use cases
 * Generally not needed - use the exported functions instead
 */
export interface FaceAntiSpoofModule {
  /**
   * Initialize native module
   */
  initialize(): Promise<boolean>;

  /**
   * Check model loading status
   */
  checkModelStatus(): Promise<ModelStatus>;

  /**
   * Check module availability
   */
  isAvailable(): Promise<boolean>;

  /**
   * Test connection to native module
   */
  testMethod(): Promise<string>;

  /**
   * Get module information
   */
  getModuleInfo(): Promise<ModuleInfo>;

  /**
   * Install/setup native dependencies
   */
  install(): Promise<boolean>;
}

/**
 * Native module instance
 */
declare const FaceAntiSpoof: FaceAntiSpoofModule;

export default FaceAntiSpoof;

// ============================================================================
// Constants
// ============================================================================

/**
 * Default anti-spoofing confidence threshold
 */
export const DEFAULT_ANTI_SPOOF_LEVEL = 0.35;

/**
 * Default face stability threshold (frames)
 */
export const DEFAULT_FACE_STABILITY_THRESHOLD = 3;

/**
 * Default face movement threshold (pixels)
 */
export const DEFAULT_FACE_MOVEMENT_THRESHOLD = 15;

/**
 * Minimum required blinks for liveness verification
 */
export const DEFAULT_REQUIRED_BLINKS = 3;

/**
 * Blink detection threshold (eye closure probability)
 */
export const DEFAULT_BLINK_THRESHOLD = 0.3;

