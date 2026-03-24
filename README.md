# react-native-vision-camera-spoof-detector

[![npm version](https://badge.fury.io/js/react-native-vision-camera-spoof-detector.svg)](https://badge.fury.io/js/react-native-vision-camera-spoof-detector)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue)](https://github.com/jescon-tech/react-native-vision-camera-spoof-detector)

High-performance face anti-spoofing and liveness detection module for React Native Vision Camera. Features TensorFlow Lite with GPU acceleration, optimized YUV processing, and real-time blink detection for robust liveness verification.

## 🎯 Features

- **🚀 Real-time Performance**: GPU-accelerated TensorFlow Lite processing for smooth 60fps detection
- **🎯 High Accuracy**: Advanced ML models for distinguishing live faces from spoofing attempts
- **👁️ Blink Detection**: Native blink detection for enhanced liveness verification
- **📱 Optimized YUV Processing**: Efficient image data handling for React Native
- **🔧 Easy Integration**: Seamlessly integrates with `react-native-vision-camera`
- **⚡ Face Stability Tracking**: Automatic stable face detection with customizable thresholds
- **🛡️ Face Centering**: Intelligent face positioning validation in frame
- **📊 Anti-spoofing Confidence**: Detailed confidence scores with multiple detection models
- **🔄 Batched Updates**: Optimized state management with minimal re-renders

## 📋 Requirements

- React Native >= 0.60.0
- react-native-vision-camera >= 4.6.4
- react-native-reanimated >= 3.0.0
- react-native-worklets-core >= 1.0.0
- react-native-vision-camera-face-detector (optional, for enhanced features)

## 📦 Installation

### Step 1: Install the package

```bash
npm install react-native-vision-camera-spoof-detector
# or
yarn add react-native-vision-camera-spoof-detector
```

### Step 2: Install peer dependencies

```bash
npm install react-native-vision-camera react-native-reanimated react-native-worklets-core
# or
yarn add react-native-vision-camera react-native-reanimated react-native-worklets-core
```

### Step 3: Configure Android (if not auto-linked)

Add to `android/app/build.gradle`:

```gradle
dependencies {
    implementation project(':react-native-vision-camera-spoof-detector')
}
```

### Step 4: Link native module (for React Native < 0.60)

```bash
react-native link react-native-vision-camera-spoof-detector
```

## 🚀 Quick Start

### Simple Usage

```javascript
import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Camera, useCameraDevices, useFrameProcessor } from 'react-native-vision-camera';
import { faceAntiSpoofFrameProcessor, initializeFaceAntiSpoof } from 'react-native-vision-camera-spoof-detector';
import { runOnJS } from 'react-native-reanimated';

export default function App() {
  const devices = useCameraDevices();
  const device = devices.front;
  const [spoofResult, setSpoofResult] = useState(null);

  useEffect(() => {
    initializeFaceAntiSpoof().then((success) => {
      console.log('FaceAntiSpoof initialized:', success);
    });
  }, []);

  const frameProcessor = useFrameProcessor((frame) => {
    'worklet';
    const result = faceAntiSpoofFrameProcessor(frame);
    if (result) {
      runOnJS(setSpoofResult)(result);
    }
  }, []);

  if (device == null) return <Text>Loading...</Text>;

  return (
    <View style={styles.container}>
      <Camera
        style={StyleSheet.absoluteFill}
        device={device}
        isActive={true}
        frameProcessor={frameProcessor}
        frameProcessorFps={5}
      />
      {spoofResult && (
        <View style={styles.resultContainer}>
          <Text style={styles.resultText}>
            Is Live: {spoofResult.isLive ? 'Yes' : 'No'}
          </Text>
          <Text style={styles.resultText}>
            Score: {spoofResult.neuralNetworkScore?.toFixed(2)}
          </Text>
          <Text style={styles.resultText}>
            Label: {spoofResult.label}
          </Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  resultContainer: {
    position: 'absolute',
    bottom: 50,
    left: 0,
    right: 0,
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.5)',
    padding: 10,
  },
  resultText: {
    color: 'white',
    fontSize: 20,
    fontWeight: 'bold',
  },
});
```

### Advanced Usage with Full Feature Set

```javascript
import { useCallback, useMemo, useEffect, useRef } from 'react';
import { Worklets } from 'react-native-worklets-core';
import { useFrameProcessor } from 'react-native-vision-camera';
import { useFaceDetector } from 'react-native-vision-camera-face-detector';
import {
  faceAntiSpoofFrameProcessor,
  initializeFaceAntiSpoof,
  isFaceAntiSpoofAvailable,
} from 'react-native-vision-camera-spoof-detector';

const useFaceDetectionFrameProcessor = ({
  onStableFaceDetected = () => { },
  onFacesUpdate = () => { },
  onLivenessUpdate = () => { },
  onAntiSpoofUpdate = () => { },
  showCodeScanner = false,
  isLoading = false,
  isActive = true,
  livenessLevel = 0,
  antispooflevel = 0.35,
}) => {
  const { detectFaces } = useFaceDetector({
    performanceMode: 'fast',
    landmarkMode: 'none',
    contourMode: 'none',
    classificationMode: livenessLevel === 1 ? 'all' : 'none',
    minFaceSize: 0.2,
  });

  const isMounted = useRef(true);
  const antiSpoofInitialized = useRef(false);

  const initializeAntiSpoof = useCallback(async () => {
    if (antiSpoofInitialized.current) return true;
    try {
      const available = isFaceAntiSpoofAvailable?.();
      if (!available) return false;
      await initializeFaceAntiSpoof();
      antiSpoofInitialized.current = true;
      return true;
    } catch (err) {
      console.error('Anti-spoof initialization error:', err);
      return false;
    }
  }, []);

  useEffect(() => {
    initializeAntiSpoof();
  }, [initializeAntiSpoof]);

  // Shared state for face tracking
  const sharedState = useMemo(
    () =>
      Worklets.createSharedValue({
        flags: {
          captured: false,
          showCodeScanner: showCodeScanner,
          isActive: isActive,
          hasSingleFace: false,
          isFaceCentered: false,
        },
        antiSpoof: {
          isLive: false,
          confidence: 0,
          consecutiveLiveFrames: 0,
        },
      }),
    []
  );

  const frameProcessor = useFrameProcessor(
    (frame) => {
      'worklet';
      
      try {
        const detected = detectFaces?.(frame);
        
        if (!detected || detected.length === 0) {
          onFacesUpdate?.({ count: 0, progress: 0 });
          return;
        }

        if (detected.length === 1 && !sharedState.value.flags.captured) {
          const antiSpoofResult = faceAntiSpoofFrameProcessor?.(frame);
          
          if (antiSpoofResult?.isLive) {
            sharedState.value.antiSpoof.isLive = true;
            sharedState.value.antiSpoof.confidence = antiSpoofResult.combinedScore;
            onAntiSpoofUpdate?.({
              isLive: true,
              confidence: antiSpoofResult.combinedScore,
            });
          }
          
          onFacesUpdate?.({ count: 1, progress: 50 });
        } else {
          onFacesUpdate?.({ count: detected.length, progress: 0 });
        }
      } catch (err) {
        console.error('Frame processing error:', err);
      } finally {
        frame.release?.();
      }
    },
    [detectFaces, isLoading]
  );

  return {
    frameProcessor,
    sharedState,
    initializeAntiSpoof,
  };
};

export default useFaceDetectionFrameProcessor;
```

## 📚 API Reference

### `initializeFaceAntiSpoof()`

Initializes the face anti-spoofing module. Must be called before using the frame processor.

```javascript
const success = await initializeFaceAntiSpoof();
```

**Returns**: `Promise<boolean>` - True if successful

---

### `isFaceAntiSpoofAvailable()`

Checks if the module is available on the device.

```javascript
const available = isFaceAntiSpoofAvailable();
```

**Returns**: `boolean`

---

### `faceAntiSpoofFrameProcessor(frame)`

Process frame and get anti-spoofing result.

```javascript
const result = faceAntiSpoofFrameProcessor(frame);
```

**Parameters**: `frame` (Vision Camera Frame)

**Returns**: `FaceAntiSpoofingResult | null`

### `FaceAntiSpoofingResult`

```typescript
interface FaceAntiSpoofingResult {
  isLive: boolean;              // Real face (true) or spoof (false)
  label: string;                // "Live Face" or "Spoof Face"
  neuralNetworkScore: number;   // 0.0-1.0 confidence
  laplacianScore: number;       // Image quality score
  combinedScore: number;        // Weighted average
  error?: string;               // Error message if any
}
```

## 🔧 Configuration

```javascript
// Anti-spoofing sensitivity (0.0-1.0, lower = more lenient)
const antispooflevel = 0.35;

// Liveness verification mode
// 0: Anti-spoofing only
// 1: Anti-spoofing + blink detection
const livenessLevel = 1;

// Customizable thresholds
const FACE_STABILITY_THRESHOLD = 3;           // Frames for stable face
const FACE_MOVEMENT_THRESHOLD = 15;           // Max pixel movement
const BLINK_THRESHOLD = 0.3;                  // Eye closure probability
const REQUIRED_BLINKS = 3;                    // Blinks for liveness
const REQUIRED_CONSECUTIVE_LIVE_FRAMES = 3;   // Consecutive live frames
const REAL_LAPLACIAN_THRESHOLD = 3500;        // Image quality threshold
const FACE_CENTER_THRESHOLD_X = 0.2;          // X-axis tolerance
const FACE_CENTER_THRESHOLD_Y = 0.15;         // Y-axis tolerance
```

## 🎮 Complete Examples

Check the [examples](./examples) folder for:
- Basic anti-spoofing detection
- Face detection with liveness
- Complete capture flow
- UI components and feedback

## 🔍 Attack Detection Capabilities

The module detects and prevents:
- ✅ Print attacks (photos)
- ✅ Display attacks (screens/tablets)
- ✅ Mask attacks (with blink detection)
- ✅ Replay attacks (videos)

Performance depends on:
- Image quality
- Lighting conditions
- Face angle and positioning
- Device camera specs

## ⚙️ Performance Tips

1. Use `performanceMode: 'fast'` in Face Detector
2. Module automatically batches state updates
3. Adjust `FRAME_PROCESSOR_MIN_INTERVAL_MS` as needed
4. GPU acceleration is used automatically when available
5. Proper frame release prevents memory leaks

## 📱 Platform Support

| Platform | Status | GPU | Notes |
|----------|--------|-----|-------|
| Android  | ✅ Supported | Yes | Fully optimized |
| iOS      | ⏳ In Progress | Yes | Coming soon |
| Web      | ❌ No | N/A | Not applicable |

## 🐛 Troubleshooting

**Module won't initialize**
```javascript
const available = isFaceAntiSpoofAvailable();
if (!available) {
  console.log('Not available on this device');
}
```

**Low accuracy**
- Check lighting conditions
- Ensure face is centered
- Adjust `antispooflevel` parameter
- Verify TensorFlow Lite models are bundled

**Performance issues**
- Reduce frame processing frequency
- Use lower camera resolution
- Enable fast performance mode
- Check device temperature

**Face detection fails**
- Ensure clear face visibility
- Check camera permissions
- Verify sufficient lighting
- Check minimum face size threshold

## 📖 Documentation

- [Complete API Documentation](./.docs/API.md)
- [Migration Guide](./.docs/MIGRATION.md)
- [Best Practices](./.docs/BEST_PRACTICES.md)
- [Comprehensive Example](./examples/CompleteExampleApp.tsx)

## 🤝 Contributing

Contributions welcome! See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

## 📄 License

MIT License - see [LICENSE](./LICENSE) file for details.

## 👨‍💼 Author

**PRAFULDAS M M**
- Company: JESCON TECHNOLOGIES PVT LTD
- Location: Thrissur, Kerala, India
- Email: jescontechnologies@gmail.com

## 🔗 Quick Links

- [NPM Package](https://www.npmjs.com/package/react-native-vision-camera-spoof-detector)
- [GitHub Repository](https://github.com/jescon-tech/react-native-vision-camera-spoof-detector)
- [React Native Vision Camera](https://react-native-vision-camera.com)
- [TensorFlow Lite](https://www.tensorflow.org/lite)

## 📞 Support & Community

- 🐛 [Report Issues](https://github.com/jescon-tech/react-native-vision-camera-spoof-detector/issues)
- 💬 [GitHub Discussions](https://github.com/jescon-tech/react-native-vision-camera-spoof-detector/discussions)
- 📧 Email: jescontechnologies@gmail.com

## 🙏 Acknowledgments

Built with:
- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [React Native Vision Camera](https://react-native-vision-camera.com)
- [React Native Worklets](https://docs.swmansion.com/react-native-worklets/)

---

Made with ❤️ by JESCON TECHNOLOGIES PVT LTD
