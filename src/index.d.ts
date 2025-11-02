/**
 * Position options for cropping
 */
export type CropPosition = 'center' | 'top' | 'bottom' | 'left' | 'right' | 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';

/**
 * Options for video cropping
 */
export interface CropOptions {
  /** Position of the crop area relative to the original video */
  position?: CropPosition;
}

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

/**
 * Crops a video to a specific aspect ratio
 * @param inputPath - Path to the input video file (file:// URI or absolute path)
 * @param aspectRatio - Desired aspect ratio as string (e.g., "16:9", "1:1", "9:16", "4:3")
 * @param options - Optional cropping options
 * @returns Promise that resolves with the path to the cropped video
 */
export function cropVideo(inputPath: string, aspectRatio: string, options?: CropOptions): Promise<string>;

/**
 * Crops a video to square aspect ratio (1:1)
 * @param inputPath - Path to the input video file
 * @param options - Optional cropping options
 * @returns Promise that resolves with the path to the cropped video
 */
export function cropToSquare(inputPath: string, options?: CropOptions): Promise<string>;

/**
 * Crops a video to 16:9 aspect ratio (widescreen)
 * @param inputPath - Path to the input video file
 * @param options - Optional cropping options
 * @returns Promise that resolves with the path to the cropped video
 */
export function cropToWidescreen(inputPath: string, options?: CropOptions): Promise<string>;

/**
 * Crops a video to 9:16 aspect ratio (Instagram Story / TikTok)
 * @param inputPath - Path to the input video file
 * @param options - Optional cropping options
 * @returns Promise that resolves with the path to the cropped video
 */
export function cropToStory(inputPath: string, options?: CropOptions): Promise<string>;

/**
 * Crops a video to 4:3 aspect ratio
 * @param inputPath - Path to the input video file
 * @param options - Optional cropping options
 * @returns Promise that resolves with the path to the cropped video
 */
export function cropTo4x3(inputPath: string, options?: CropOptions): Promise<string>;

declare const _default: {
  rotateVideo: typeof rotateVideo;
  rotateVideoClockwise: typeof rotateVideoClockwise;
  rotateVideoCounterClockwise: typeof rotateVideoCounterClockwise;
  rotateVideo180: typeof rotateVideo180;
  cropVideo: typeof cropVideo;
  cropToSquare: typeof cropToSquare;
  cropToWidescreen: typeof cropToWidescreen;
  cropToStory: typeof cropToStory;
  cropTo4x3: typeof cropTo4x3;
};

export default _default;
