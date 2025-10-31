import { NativeModules, Platform } from 'react-native';

const { VideoTransformer } = NativeModules;

if (!VideoTransformer) {
  throw new Error(
    'VideoTransformer native module is not linked. ' +
    'Make sure you have run `pod install` (iOS) or rebuilt the app (Android).'
  );
}

/**
 * Rotates a video by the specified angle
 * @param {string} inputPath - Path to the input video file
 * @param {number} angle - Rotation angle in degrees (90, 180, 270, or -90)
 * @returns {Promise<string>} - Promise that resolves with the path to the rotated video
 */
export async function rotateVideo(inputPath, angle) {
  if (!inputPath || typeof inputPath !== 'string') {
    throw new Error('inputPath must be a valid string');
  }

  const validAngles = [90, -90, 180, 270];
  if (!validAngles.includes(angle)) {
    throw new Error(`angle must be one of: ${validAngles.join(', ')}`);
  }

  try {
    const outputPath = await VideoTransformer.rotateVideo(inputPath, angle);
    return outputPath;
  } catch (error) {
    throw new Error(`Failed to rotate video: ${error.message}`);
  }
}

/**
 * Rotates a video 90 degrees clockwise
 * @param {string} inputPath - Path to the input video file
 * @returns {Promise<string>} - Promise that resolves with the path to the rotated video
 */
export async function rotateVideoClockwise(inputPath) {
  return rotateVideo(inputPath, 90);
}

/**
 * Rotates a video 90 degrees counter-clockwise
 * @param {string} inputPath - Path to the input video file
 * @returns {Promise<string>} - Promise that resolves with the path to the rotated video
 */
export async function rotateVideoCounterClockwise(inputPath) {
  return rotateVideo(inputPath, -90);
}

/**
 * Rotates a video 180 degrees
 * @param {string} inputPath - Path to the input video file
 * @returns {Promise<string>} - Promise that resolves with the path to the rotated video
 */
export async function rotateVideo180(inputPath) {
  return rotateVideo(inputPath, 180);
}

export default {
  rotateVideo,
  rotateVideoClockwise,
  rotateVideoCounterClockwise,
  rotateVideo180,
};
