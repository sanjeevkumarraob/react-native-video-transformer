# Usage Examples

## Complete Example with React Native Vision Camera

```javascript
import React, { useRef, useState } from 'react';
import { View, TouchableOpacity, Text, Alert, Platform } from 'react-native';
import { Camera, useCameraDevice } from 'react-native-vision-camera';
import { CameraRoll } from '@react-native-camera-roll/camera-roll';
import { rotateVideo } from '@sanjeevkumarrao/react-native-video-transformer';

const CameraScreen = () => {
  const [cameraPosition, setCameraPosition] = useState('front');
  const [isRecording, setIsRecording] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  const camera = useRef(null);
  const device = useCameraDevice(cameraPosition);

  const startRecording = async () => {
    if (camera.current && !isRecording) {
      setIsRecording(true);

      await camera.current.startRecording({
        onRecordingFinished: async (video) => {
          setIsProcessing(true);

          try {
            let videoPath = video.path;

            // Rotate front camera videos on iOS
            if (Platform.OS === 'ios' && cameraPosition === 'front') {
              console.log('Rotating video...');
              videoPath = await rotateVideo(video.path, 90);
              console.log('Video rotated successfully');
            }

            // Save to camera roll
            await CameraRoll.save(videoPath, { type: 'video' });

            setIsProcessing(false);
            Alert.alert('Success', 'Video saved to Photos!');
          } catch (error) {
            setIsProcessing(false);
            console.error('Error:', error);
            Alert.alert('Error', 'Failed to save video');
          }

          setIsRecording(false);
        },
        onRecordingError: (error) => {
          console.error('Recording error:', error);
          setIsRecording(false);
          Alert.alert('Error', 'Failed to record video');
        },
      });
    }
  };

  const stopRecording = async () => {
    if (camera.current && isRecording) {
      await camera.current.stopRecording();
    }
  };

  const toggleCamera = () => {
    setCameraPosition(prev => prev === 'front' ? 'back' : 'front');
  };

  if (!device) {
    return <Text>Loading camera...</Text>;
  }

  return (
    <View style={{ flex: 1 }}>
      <Camera
        ref={camera}
        style={{ flex: 1 }}
        device={device}
        isActive={true}
        video={true}
        audio={true}
      />

      {isProcessing && (
        <View style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.7)',
          justifyContent: 'center',
          alignItems: 'center'
        }}>
          <Text style={{ color: 'white', fontSize: 18 }}>Processing video...</Text>
        </View>
      )}

      <View style={{ position: 'absolute', bottom: 40, alignSelf: 'center' }}>
        <TouchableOpacity
          onPress={isRecording ? stopRecording : startRecording}
          style={{
            width: 72,
            height: 72,
            borderRadius: 36,
            backgroundColor: isRecording ? 'red' : 'white',
          }}
        />
      </View>

      <TouchableOpacity
        onPress={toggleCamera}
        style={{ position: 'absolute', top: 60, right: 20 }}
      >
        <Text style={{ color: 'white' }}>Flip</Text>
      </TouchableOpacity>
    </View>
  );
};

export default CameraScreen;
```

## Example with File Picker

```javascript
import { rotateVideo } from '@sanjeevkumarrao/react-native-video-transformer';
import DocumentPicker from 'react-native-document-picker';
import RNFS from 'react-native-fs';

async function pickAndRotateVideo() {
  try {
    // Pick a video file
    const res = await DocumentPicker.pick({
      type: [DocumentPicker.types.video],
    });

    console.log('Selected video:', res.uri);

    // Rotate the video
    const rotatedPath = await rotateVideo(res.uri, 90);
    console.log('Rotated video path:', rotatedPath);

    // Optionally, move the rotated video to a permanent location
    const destPath = `${RNFS.DocumentDirectoryPath}/rotated_video.mp4`;
    await RNFS.moveFile(rotatedPath, destPath);

    console.log('Saved to:', destPath);

    return destPath;
  } catch (error) {
    if (DocumentPicker.isCancel(error)) {
      console.log('User cancelled picker');
    } else {
      console.error('Error:', error);
    }
  }
}
```

## Example with Progress Tracking

```javascript
import { rotateVideo } from '@sanjeevkumarrao/react-native-video-transformer';

async function rotateWithProgress(videoPath) {
  const [progress, setProgress] = useState(0);
  const [status, setStatus] = useState('idle');

  try {
    setStatus('rotating');
    setProgress(0);

    // Start rotation
    const rotatePromise = rotateVideo(videoPath, 90);

    // Simulate progress (since native module doesn't provide progress yet)
    const progressInterval = setInterval(() => {
      setProgress(prev => Math.min(prev + 10, 90));
    }, 300);

    // Wait for rotation to complete
    const rotatedPath = await rotatePromise;

    // Clear interval and set to 100%
    clearInterval(progressInterval);
    setProgress(100);
    setStatus('complete');

    return rotatedPath;
  } catch (error) {
    setStatus('error');
    throw error;
  }
}
```

## Example with Batch Processing

```javascript
import { rotateVideo } from '@sanjeevkumarrao/react-native-video-transformer';

async function batchRotateVideos(videoPaths, angle = 90) {
  const results = [];

  for (let i = 0; i < videoPaths.length; i++) {
    const videoPath = videoPaths[i];

    try {
      console.log(`Rotating video ${i + 1}/${videoPaths.length}...`);
      const rotatedPath = await rotateVideo(videoPath, angle);

      results.push({
        original: videoPath,
        rotated: rotatedPath,
        success: true,
      });
    } catch (error) {
      console.error(`Failed to rotate ${videoPath}:`, error);

      results.push({
        original: videoPath,
        rotated: null,
        success: false,
        error: error.message,
      });
    }
  }

  return results;
}
```

## TypeScript Example

```typescript
import { rotateVideo, rotateVideoClockwise } from '@sanjeevkumarrao/react-native-video-transformer';

interface VideoRotationResult {
  originalPath: string;
  rotatedPath: string;
  duration: number;
}

async function rotateVideoWithTiming(inputPath: string): Promise<VideoRotationResult> {
  const startTime = Date.now();

  try {
    const rotatedPath = await rotateVideoClockwise(inputPath);
    const duration = Date.now() - startTime;

    return {
      originalPath: inputPath,
      rotatedPath,
      duration,
    };
  } catch (error) {
    throw new Error(`Failed to rotate video: ${(error as Error).message}`);
  }
}
```
