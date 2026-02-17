# react-native-vision-camera-spoof-detector

High-performance face anti-spoofing and liveness detection module for React Native Vision Camera. Uses TensorFlow Lite with GPU acceleration and optimized YUV processing.

## Features

- **High Accuracy**: Utilizes advanced TensorFlow Lite models for reliable spoof detection.
- **Real-time Performance**: GPU-accelerated processing ensures smooth frame rates.
- **Easy Integration**: Seamlessly integrates with `react-native-vision-camera`.
- **YUV Processing**: Optimized for efficient image data handling.

## Installation

```bash
npm install react-native-vision-camera-spoof-detector
# or
yarn add react-native-vision-camera-spoof-detector
```

Make sure you have `react-native-vision-camera` installed:

```bash
npm install react-native-vision-camera
# or
yarn add react-native-vision-camera
```

## Usage

Here's a basic example of how to use the spoof detector with Vision Camera:

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
    // Initialize the module
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
        frameProcessorFps={5} // Adjust FPS as needed
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
  container: {
    flex: 1,
  },
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

## API

### `initializeFaceAntiSpoof()`

Initializes the face anti-spoofing module. Must be called before using the frame processor.

- **Returns**: `Promise<boolean>` - `true` if initialization was successful.

### `faceAntiSpoofFrameProcessor(frame)`

Processes the camera frame and returns the spoof detection result.

- **frame**: The frame object from `react-native-vision-camera`.
- **Returns**: `FaceAntiSpoofingResult | null`

#### `FaceAntiSpoofingResult`

- `isLive` (boolean): `true` if the face is real (live), `false` otherwise.
- `label` (string): "Live Face" or "Spoof Face".
- `neuralNetworkScore` (number): Confidence score (0.0 to 1.0).
- `laplacianScore` (number): Image quality score based on Laplacian variance.
- `combinedScore` (number): Weighted average score.
- `error` (string, optional): Error message if detection failed.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

JESCON TECHNOLOGIES PVT LTD
