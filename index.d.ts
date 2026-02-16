/**
 * React Native Vision Camera Face Anti-Spoofing Detector
 * TypeScript type definitions
 */

import type { Frame } from 'react-native-vision-camera';

/**
 * Result from face anti-spoofing detection
 */
export interface FaceAntiSpoofingResult {
  /** Whether the face is live (not a spoof/fake) */
  isLive: boolean;
  
  /** Human-readable label: "Live Face" or "Spoof Face" */
  label: string;
  
  /** Neural network confidence score (0.0-1.0), lower is more likely to be live */
  neuralNetworkScore: number;
  
  /** Laplacian-based image quality score, higher is better */
  laplacianScore: number;
  
  /** Combined score (weighted average, 0.0-1.0) */
  combinedScore: number;
  
  /** Error message if detection failed */
  error?: string;
}

/**
 * Accelerator type for TensorFlow Lite inference
 */
export type AcceleratorType = 'CPU' | 'GPU' | 'NNAPI';

/**
 * Frame processor function for real-time face anti-spoofing detection
 */
export function faceAntiSpoofFrameProcessor(
  frame: Frame
): FaceAntiSpoofingResult | null;

/**
 * Check if face anti-spoofing module is available
 */
export function isFaceAntiSpoofAvailable(): boolean;

/**
 * Initialize the face anti-spoofing module
 * Must be called before using the frame processor
 */
export function initializeFaceAntiSpoof(): Promise<boolean>;

/**
 * Native module interface
 */
export interface FaceAntiSpoofModule {
  initialize(): Promise<boolean>;
  
  checkModelStatus(): Promise<{
    pluginAvailable: boolean;
    modelLoaded: boolean;
    accelerator?: AcceleratorType;
  }>;
  
  isAvailable(): Promise<boolean>;
  
  testMethod(): Promise<string>;
  
  getModuleInfo(): Promise<{
    name: string;
    methods: string[];
    version: string;
    accelerator?: AcceleratorType;
  }>;
  
  install(): Promise<boolean>;
}

/**
 * Default export - the native module
 */
declare const FaceAntiSpoof: FaceAntiSpoofModule;
export default FaceAntiSpoof;
