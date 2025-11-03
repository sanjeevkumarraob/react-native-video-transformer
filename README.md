# @sanjeevkumarrao/react-native-video-transformer

A lightweight React Native library to rotate and transform videos on iOS and Android. Perfect for fixing video orientation issues, especially with front-facing camera recordings.

## Features

✅ Rotate videos by 90°, 180°, or 270° (clockwise/counter-clockwise)
✅ Crop videos to different aspect ratios (16:9, 1:1, 9:16, 4:3, custom)
✅ Flexible crop positioning (center, top, bottom, corners, etc.)
✅ Built with native iOS AVFoundation and Android MediaCodec
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

No additional setup required. Video transformation is now supported on both iOS and Android.

## Usage

### Basic Example - Rotation

```javascript
import { rotateVideo } from '@sanjeevkumarrao/react-native-video-transformer';

// Rotate a video 90 degrees clockwise
const rotatedPath = await rotateVideo('/path/to/video.mp4', 90);
console.log('Rotated video saved at:', rotatedPath);
```

### Basic Example - Cropping

```javascript
import { cropToSquare, cropVideo } from '@sanjeevkumarrao/react-native-video-transformer';

// Crop to square (1:1) for Instagram
const squarePath = await cropToSquare('/path/to/video.mp4');
console.log('Cropped video saved at:', squarePath);

// Crop to custom aspect ratio with specific position
const customPath = await cropVideo('/path/to/video.mp4', '16:9', { position: 'top' });
console.log('Custom cropped video saved at:', customPath);
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

---

### `cropVideo(inputPath, aspectRatio, options)`

Crops a video to a specific aspect ratio.

**Parameters:**
- `inputPath` (string): Path to the input video file (file:// URI or absolute path)
- `aspectRatio` (string): Desired aspect ratio (e.g., "16:9", "1:1", "9:16", "4:3")
- `options` (object, optional): Cropping options
  - `position` (string): Crop position - one of: 'center', 'top', 'bottom', 'left', 'right', 'top-left', 'top-right', 'bottom-left', 'bottom-right' (default: 'center')

**Returns:**
Promise<string> - Path to the cropped video file

**Example:**
```javascript
// Crop to 16:9 from center
const outputPath = await cropVideo('/path/to/video.mp4', '16:9');

// Crop to square from top
const squarePath = await cropVideo('/path/to/video.mp4', '1:1', { position: 'top' });
```

---

### `cropToSquare(inputPath, options)`

Convenience method to crop a video to 1:1 aspect ratio (square).

**Parameters:**
- `inputPath` (string): Path to the input video file
- `options` (object, optional): Cropping options (same as cropVideo)

**Returns:**
Promise<string> - Path to the cropped video file

**Example:**
```javascript
const squarePath = await cropToSquare('/path/to/video.mp4');
```

---

### `cropToWidescreen(inputPath, options)`

Convenience method to crop a video to 16:9 aspect ratio (widescreen).

**Parameters:**
- `inputPath` (string): Path to the input video file
- `options` (object, optional): Cropping options

**Returns:**
Promise<string> - Path to the cropped video file

---

### `cropToStory(inputPath, options)`

Convenience method to crop a video to 9:16 aspect ratio (Instagram Story/TikTok format).

**Parameters:**
- `inputPath` (string): Path to the input video file
- `options` (object, optional): Cropping options

**Returns:**
Promise<string> - Path to the cropped video file

**Example:**
```javascript
const storyPath = await cropToStory('/path/to/video.mp4');
```

---

### `cropTo4x3(inputPath, options)`

Convenience method to crop a video to 4:3 aspect ratio.

**Parameters:**
- `inputPath` (string): Path to the input video file
- `options` (object, optional): Cropping options

**Returns:**
Promise<string> - Path to the cropped video file

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

### Prepare Videos for Social Media

```javascript
import { cropToSquare, cropToStory, cropToWidescreen } from '@sanjeevkumarrao/react-native-video-transformer';

// Crop for Instagram post (square)
const instagramPath = await cropToSquare('/path/to/video.mp4');

// Crop for Instagram Story or TikTok
const storyPath = await cropToStory('/path/to/video.mp4', { position: 'center' });

// Crop for YouTube (widescreen)
const youtubePath = await cropToWidescreen('/path/to/video.mp4');

// Custom aspect ratio with specific positioning
const customPath = await cropVideo('/path/to/video.mp4', '21:9', { position: 'bottom' });
```

### Combine Rotation and Cropping

```javascript
import { rotateVideo, cropToSquare } from '@sanjeevkumarrao/react-native-video-transformer';

async function prepareForInstagram(videoPath) {
  // First rotate if needed
  const rotatedPath = await rotateVideo(videoPath, 90);

  // Then crop to square
  const finalPath = await cropToSquare(rotatedPath);

  return finalPath;
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
| `INVALID_ASPECT_RATIO` | Invalid aspect ratio format provided |
| `ROTATION_ERROR` | Failed to rotate video |
| `CROP_ERROR` | Failed to crop video |
| `PROCESSING_ERROR` | General video processing error |

## Technical Details

### iOS Implementation

- Uses native AVFoundation framework
- Creates AVMutableComposition with proper transforms
- Supports both rotation and cropping via AVMutableVideoCompositionLayerInstruction
- Exports with AVAssetExportSession
- Preserves audio tracks automatically
- Output format: MP4 (H.264)
- Quality: Highest available
- Handles video orientation automatically

### Android Implementation

- Uses MediaCodec for video encoding/decoding
- Uses MediaMuxer for combining tracks
- Supports rotation and aspect ratio cropping
- Preserves audio tracks
- Output format: MP4 (H.264)
- Hardware-accelerated encoding when available

### Output Files

Transformed videos (rotated or cropped) are saved to the temporary/cache directory with the naming pattern:
```
transformed_<UUID>.mp4
```

You're responsible for moving or copying the file to a permanent location if needed.

## Performance

- **iOS**: Fast, uses hardware acceleration via AVFoundation
- **Android**: Hardware-accelerated encoding when available via MediaCodec
- **File Size**: Output size depends on original video quality and duration
- **Memory**: Efficient, processes videos in chunks
- **Processing Time**: Varies based on video length, resolution, and device capabilities

## Limitations

- Currently supports MP4 output format only
- Requires iOS 11.0 or later
- Requires Android API level 21 (Lollipop) or later
- Android implementation uses surface-based encoding (may have limitations on some devices)
- Maximum video resolution depends on device capabilities

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
- Email: sanjeevkumarrao@gmail.com

## Credits

Built with ❤️ for the React Native community.

Special thanks to the contributors and users of this library!
