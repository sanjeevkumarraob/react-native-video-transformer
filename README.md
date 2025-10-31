# @sanjeevkumarrao/react-native-video-transformer

A lightweight React Native library to rotate and transform videos on iOS and Android. Perfect for fixing video orientation issues, especially with front-facing camera recordings.

## Features

✅ Rotate videos by 90°, 180°, or 270° (clockwise/counter-clockwise)
✅ Built with native iOS AVFoundation (no external dependencies)
✅ Preserves audio tracks
✅ High-quality export with configurable presets
✅ TypeScript support
✅ Promise-based API

## Installation

```bash
npm install @sanjeevkumarrao/react-native-video-transformer
```

or

```bash
yarn add @sanjeevkumarrao/react-native-video-transformer
```

### iOS

```bash
cd ios && pod install
```

### Android

Android implementation coming soon! Currently iOS-only.

## Usage

### Basic Example

```javascript
import { rotateVideo } from '@sanjeevkumarrao/react-native-video-transformer';

// Rotate a video 90 degrees clockwise
const rotatedPath = await rotateVideo('/path/to/video.mp4', 90);
console.log('Rotated video saved at:', rotatedPath);
```

### With React Native Camera

```javascript
import { Camera } from 'react-native-vision-camera';
import { rotateVideoClockwise } from '@sanjeevkumarrao/react-native-video-transformer';

const MyComponent = () => {
  const camera = useRef(null);

  const handleRecord = async () => {
    await camera.current.startRecording({
      onRecordingFinished: async (video) => {
        // Rotate front camera videos
        const rotatedPath = await rotateVideoClockwise(video.path);

        // Save to camera roll
        await CameraRoll.save(rotatedPath, { type: 'video' });
      },
    });
  };

  return <Camera ref={camera} />;
};
```

### Advanced Example

```javascript
import { rotateVideo } from '@sanjeevkumarrao/react-native-video-transformer';
import { Platform } from 'react-native';

async function fixVideoOrientation(videoPath, cameraPosition) {
  // Only rotate front camera videos on iOS
  if (Platform.OS === 'ios' && cameraPosition === 'front') {
    try {
      // Rotate 90 degrees clockwise for portrait mode
      const rotatedPath = await rotateVideo(videoPath, 90);
      return rotatedPath;
    } catch (error) {
      console.error('Failed to rotate video:', error);
      return videoPath; // Return original on error
    }
  }
  return videoPath;
}
```

## API Reference

### `rotateVideo(inputPath, angle)`

Rotates a video by the specified angle.

**Parameters:**
- `inputPath` (string): Path to the input video file (file:// URI or absolute path)
- `angle` (number): Rotation angle in degrees. Valid values: `90`, `-90`, `180`, `270`

**Returns:**
Promise<string> - Path to the rotated video file

**Example:**
```javascript
const outputPath = await rotateVideo('/path/to/video.mp4', 90);
```

---

### `rotateVideoClockwise(inputPath)`

Convenience method to rotate a video 90° clockwise.

**Parameters:**
- `inputPath` (string): Path to the input video file

**Returns:**
Promise<string> - Path to the rotated video file

**Example:**
```javascript
const outputPath = await rotateVideoClockwise('/path/to/video.mp4');
```

---

### `rotateVideoCounterClockwise(inputPath)`

Convenience method to rotate a video 90° counter-clockwise.

**Parameters:**
- `inputPath` (string): Path to the input video file

**Returns:**
Promise<string> - Path to the rotated video file

---

### `rotateVideo180(inputPath)`

Convenience method to rotate a video 180°.

**Parameters:**
- `inputPath` (string): Path to the input video file

**Returns:**
Promise<string> - Path to the rotated video file

## Common Use Cases

### Fix Front Camera Orientation

Front-facing camera videos often save in the wrong orientation. Fix them before saving:

```javascript
import { rotateVideo } from '@sanjeevkumarrao/react-native-video-transformer';

async function saveVideo(videoPath, cameraPosition) {
  let finalPath = videoPath;

  // Fix front camera orientation
  if (cameraPosition === 'front') {
    finalPath = await rotateVideo(videoPath, 90);
  }

  // Save to camera roll
  await CameraRoll.save(finalPath, { type: 'video' });
}
```

### Batch Process Multiple Videos

```javascript
import { rotateVideo } from '@sanjeevkumarrao/react-native-video-transformer';

async function rotateMultipleVideos(videoPaths) {
  const promises = videoPaths.map(path => rotateVideo(path, 90));
  const rotatedPaths = await Promise.all(promises);
  return rotatedPaths;
}
```

## Error Handling

```javascript
import { rotateVideo } from '@sanjeevkumarrao/react-native-video-transformer';

try {
  const rotatedPath = await rotateVideo('/path/to/video.mp4', 90);
  console.log('Success:', rotatedPath);
} catch (error) {
  if (error.code === 'INVALID_PATH') {
    console.error('Invalid video path');
  } else if (error.code === 'NO_VIDEO_TRACK') {
    console.error('No video track found in file');
  } else if (error.code === 'EXPORT_FAILED') {
    console.error('Failed to export video');
  } else {
    console.error('Unknown error:', error.message);
  }
}
```

## Error Codes

| Code | Description |
|------|-------------|
| `INVALID_PATH` | The input path is invalid or the file doesn't exist |
| `NO_VIDEO_TRACK` | No video track found in the input file |
| `COMPOSITION_ERROR` | Failed to create video composition |
| `INSERT_ERROR` | Failed to insert time range |
| `EXPORT_ERROR` | Failed to create export session |
| `EXPORT_FAILED` | Video export failed |
| `EXPORT_CANCELLED` | Video export was cancelled |
| `NOT_IMPLEMENTED` | Feature not yet implemented (Android) |

## Technical Details

### iOS Implementation

- Uses native AVFoundation framework
- Creates AVMutableComposition with proper transforms
- Exports with AVAssetExportSession
- Preserves audio tracks automatically
- Output format: MP4 (H.264)
- Quality: Highest available

### Output Files

Rotated videos are saved to the temporary directory with the naming pattern:
```
transformed_<UUID>.mp4
```

You're responsible for moving or copying the file to a permanent location if needed.

## Performance

- **iOS**: Fast, uses hardware acceleration via AVFoundation
- **File Size**: Output size depends on original video quality and duration
- **Memory**: Efficient, processes videos in chunks

## Limitations

- Android implementation coming soon
- Currently supports MP4 output format only
- Requires iOS 11.0 or later

## Troubleshooting

### Module not found error

Make sure you've installed pods:
```bash
cd ios && pod install
```

Then rebuild your app:
```bash
npx react-native run-ios
```

### Swift bridging header not found

If you encounter Swift bridging header errors, you may need to manually configure the bridging header in Xcode:

1. Open your iOS project in Xcode
2. Select your project target
3. Go to Build Settings
4. Search for "Objective-C Bridging Header"
5. Set the value to: `$(SRCROOT)/../node_modules/@sanjeevkumarrao/react-native-video-transformer/ios/VideoTransformer-Bridging-Header.h`

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT © Sanjeev Kumar Rao

## Support

- GitHub Issues: [https://github.com/sanjeevkumarraob/react-native-video-transformer/issues](https://github.com/sanjeevkumarraob/react-native-video-transformer/issues)
- Email: your.email@example.com

## Credits

Built with ❤️ for the React Native community.

Special thanks to the contributors and users of this library!
