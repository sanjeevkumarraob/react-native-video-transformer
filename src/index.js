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

/**
 * Crops a video to a specific aspect ratio
 * @param {string} inputPath - Path to the input video file
 * @param {string} aspectRatio - Desired aspect ratio (e.g., "16:9", "1:1", "9:16", "4:3")
 * @param {object} options - Optional cropping options
 * @param {string} options.position - Position of crop area ('center', 'top', 'bottom', etc.)
 * @returns {Promise<string>} - Promise that resolves with the path to the cropped video
 */
export async function cropVideo(inputPath, aspectRatio, options = {}) {
  if (!inputPath || typeof inputPath !== 'string') {
    throw new Error('inputPath must be a valid string');
  }

  if (!aspectRatio || typeof aspectRatio !== 'string') {
    throw new Error('aspectRatio must be a valid string (e.g., "16:9", "1:1")');
  }

  const position = options.position || 'center';

  try {
    const outputPath = await VideoTransformer.cropVideo(inputPath, aspectRatio, position);
    return outputPath;
  } catch (error) {
    throw new Error(`Failed to crop video: ${error.message}`);
  }
}

/**
 * Crops a video to square aspect ratio (1:1)
 * @param {string} inputPath - Path to the input video file
 * @param {object} options - Optional cropping options
 * @returns {Promise<string>} - Promise that resolves with the path to the cropped video
 */
export async function cropToSquare(inputPath, options = {}) {
  return cropVideo(inputPath, '1:1', options);
}

/**
 * Crops a video to 16:9 aspect ratio (widescreen)
 * @param {string} inputPath - Path to the input video file
 * @param {object} options - Optional cropping options
 * @returns {Promise<string>} - Promise that resolves with the path to the cropped video
 */
export async function cropToWidescreen(inputPath, options = {}) {
  return cropVideo(inputPath, '16:9', options);
}

/**
 * Crops a video to 9:16 aspect ratio (Instagram Story / TikTok)
 * @param {string} inputPath - Path to the input video file
 * @param {object} options - Optional cropping options
 * @returns {Promise<string>} - Promise that resolves with the path to the cropped video
 */
export async function cropToStory(inputPath, options = {}) {
  return cropVideo(inputPath, '9:16', options);
}

/**
 * Crops a video to 4:3 aspect ratio
 * @param {string} inputPath - Path to the input video file
 * @param {object} options - Optional cropping options
 * @returns {Promise<string>} - Promise that resolves with the path to the cropped video
 */
export async function cropTo4x3(inputPath, options = {}) {
  return cropVideo(inputPath, '4:3', options);
}

/**
 * Crops and rotates a video in a single operation (more efficient than separate crop + rotate)
 * @param {string} inputPath - Path to the input video file
 * @param {string} aspectRatio - Desired aspect ratio (e.g., "16:9", "1:1", "9:16", "4:3")
 * @param {number} angle - Rotation angle in degrees (90, 180, 270, or -90)
 * @param {object} options - Optional cropping options
 * @param {string} options.position - Position of crop area ('center', 'top', 'bottom', etc.)
 * @returns {Promise<string>} - Promise that resolves with the path to the processed video
 */
export async function cropAndRotateVideo(inputPath, aspectRatio, angle, options = {}) {
  if (!inputPath || typeof inputPath !== 'string') {
    throw new Error('inputPath must be a valid string');
  }

  if (!aspectRatio || typeof aspectRatio !== 'string') {
    throw new Error('aspectRatio must be a valid string (e.g., "16:9", "1:1")');
  }

  const validAngles = [90, -90, 180, 270];
  if (!validAngles.includes(angle)) {
    throw new Error(`angle must be one of: ${validAngles.join(', ')}`);
  }

  const position = options.position || 'center';

  try {
    const outputPath = await VideoTransformer.cropAndRotateVideo(inputPath, aspectRatio, position, angle);
    return outputPath;
  } catch (error) {
    throw new Error(`Failed to crop and rotate video: ${error.message}`);
  }
}

export default {
  rotateVideo,
  rotateVideoClockwise,
  rotateVideoCounterClockwise,
  rotateVideo180,
  cropVideo,
  cropToSquare,
  cropToWidescreen,
  cropToStory,
  cropTo4x3,
  cropAndRotateVideo,
};
