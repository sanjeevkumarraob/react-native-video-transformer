/**
 * Rotates a video by the specified angle
 * @param inputPath - Path to the input video file (file:// URI or absolute path)
 * @param angle - Rotation angle in degrees (90, 180, 270, or -90)
 * @returns Promise that resolves with the path to the rotated video
 */
export function rotateVideo(inputPath: string, angle: number): Promise<string>;

/**
 * Rotates a video 90 degrees clockwise
 * @param inputPath - Path to the input video file
 * @returns Promise that resolves with the path to the rotated video
 */
export function rotateVideoClockwise(inputPath: string): Promise<string>;

/**
 * Rotates a video 90 degrees counter-clockwise
 * @param inputPath - Path to the input video file
 * @returns Promise that resolves with the path to the rotated video
 */
export function rotateVideoCounterClockwise(inputPath: string): Promise<string>;

/**
 * Rotates a video 180 degrees
 * @param inputPath - Path to the input video file
 * @returns Promise that resolves with the path to the rotated video
 */
export function rotateVideo180(inputPath: string): Promise<string>;

declare const _default: {
  rotateVideo: typeof rotateVideo;
  rotateVideoClockwise: typeof rotateVideoClockwise;
  rotateVideoCounterClockwise: typeof rotateVideoCounterClockwise;
  rotateVideo180: typeof rotateVideo180;
};

export default _default;
