package com.videotransformer

import android.media.*
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

class VideoTransformerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "VideoTransformer"
    }

    data class CropRect(val x: Int, val y: Int, val width: Int, val height: Int)

    @ReactMethod
    fun rotateVideo(inputPath: String, angle: Int, promise: Promise) {
        try {
            val normalizedPath = if (inputPath.startsWith("file://")) {
                inputPath.substring(7)
            } else {
                inputPath
            }

            val inputFile = File(normalizedPath)
            if (!inputFile.exists()) {
                promise.reject("INVALID_PATH", "Input file does not exist: $normalizedPath")
                return
            }

            val outputFile = File(reactApplicationContext.cacheDir, "transformed_${UUID.randomUUID()}.mp4")

            processVideoWithGL(
                inputPath = normalizedPath,
                outputPath = outputFile.absolutePath,
                rotationAngle = angle,
                cropRect = null,
                promise = promise
            )

        } catch (e: Exception) {
            promise.reject("ROTATION_ERROR", "Failed to rotate video: ${e.message}", e)
        }
    }

    @ReactMethod
    fun cropVideo(inputPath: String, aspectRatio: String, position: String, promise: Promise) {
        try {
            val normalizedPath = if (inputPath.startsWith("file://")) {
                inputPath.substring(7)
            } else {
                inputPath
            }

            val inputFile = File(normalizedPath)
            if (!inputFile.exists()) {
                promise.reject("INVALID_PATH", "Input file does not exist: $normalizedPath")
                return
            }

            val parts = aspectRatio.split(":")
            if (parts.size != 2) {
                promise.reject("INVALID_ASPECT_RATIO", "Invalid aspect ratio format. Use format like '16:9'")
                return
            }

            val widthRatio = parts[0].toDoubleOrNull()
            val heightRatio = parts[1].toDoubleOrNull()

            if (widthRatio == null || heightRatio == null) {
                promise.reject("INVALID_ASPECT_RATIO", "Invalid aspect ratio values")
                return
            }

            val targetAspectRatio = widthRatio / heightRatio

            val extractor = MediaExtractor()
            extractor.setDataSource(normalizedPath)

            var videoFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    videoFormat = format
                    break
                }
            }

            if (videoFormat == null) {
                promise.reject("NO_VIDEO_TRACK", "No video track found")
                extractor.release()
                return
            }

            val videoWidth = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val videoHeight = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)

            val cropRect = calculateCropRect(videoWidth, videoHeight, targetAspectRatio, position)
            extractor.release()

            val outputFile = File(reactApplicationContext.cacheDir, "transformed_${UUID.randomUUID()}.mp4")

            processVideoWithGL(
                inputPath = normalizedPath,
                outputPath = outputFile.absolutePath,
                rotationAngle = 0,
                cropRect = cropRect,
                promise = promise
            )

        } catch (e: Exception) {
            promise.reject("CROP_ERROR", "Failed to crop video: ${e.message}", e)
        }
    }

    private fun calculateCropRect(
        videoWidth: Int,
        videoHeight: Int,
        targetAspectRatio: Double,
        position: String
    ): CropRect {
        val videoAspectRatio = videoWidth.toDouble() / videoHeight.toDouble()

        val cropWidth: Int
        val cropHeight: Int

        if (targetAspectRatio > videoAspectRatio) {
            cropWidth = videoWidth
            cropHeight = (videoWidth / targetAspectRatio).toInt()
        } else {
            cropHeight = videoHeight
            cropWidth = (videoHeight * targetAspectRatio).toInt()
        }

        val cropX: Int
        val cropY: Int

        when (position.lowercase()) {
            "center" -> {
                cropX = (videoWidth - cropWidth) / 2
                cropY = (videoHeight - cropHeight) / 2
            }
            "top" -> {
                cropX = (videoWidth - cropWidth) / 2
                cropY = 0
            }
            "bottom" -> {
                cropX = (videoWidth - cropWidth) / 2
                cropY = videoHeight - cropHeight
            }
            "left" -> {
                cropX = 0
                cropY = (videoHeight - cropHeight) / 2
            }
            "right" -> {
                cropX = videoWidth - cropWidth
                cropY = (videoHeight - cropHeight) / 2
            }
            "top-left" -> {
                cropX = 0
                cropY = 0
            }
            "top-right" -> {
                cropX = videoWidth - cropWidth
                cropY = 0
            }
            "bottom-left" -> {
                cropX = 0
                cropY = videoHeight - cropHeight
            }
            "bottom-right" -> {
                cropX = videoWidth - cropWidth
                cropY = videoHeight - cropHeight
            }
            else -> {
                cropX = (videoWidth - cropWidth) / 2
                cropY = (videoHeight - cropHeight) / 2
            }
        }

        return CropRect(cropX, cropY, cropWidth, cropHeight)
    }

    private fun processVideoWithGL(
        inputPath: String,
        outputPath: String,
        rotationAngle: Int,
        cropRect: CropRect?,
        promise: Promise
    ) {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: InputSurface? = null
        var outputSurface: OutputSurface? = null
        var textureRenderer: TextureRenderer? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            // Find video and audio tracks
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                when {
                    mime.startsWith("video/") -> {
                        videoTrackIndex = i
                        videoFormat = format
                    }
                    mime.startsWith("audio/") -> {
                        audioTrackIndex = i
                        audioFormat = format
                    }
                }
            }

            if (videoFormat == null) {
                promise.reject("NO_VIDEO_TRACK", "No video track found")
                return
            }

            // Setup Output Surface for decoder
            outputSurface = OutputSurface()

            // Setup decoder
            val videoMime = videoFormat.getString(MediaFormat.KEY_MIME)!!
            decoder = MediaCodec.createDecoderByType(videoMime)
            decoder.configure(videoFormat, outputSurface.getSurface(), null, 0)
            decoder.start()

            // Get video dimensions
            val videoWidth = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val videoHeight = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)

            // Calculate output dimensions
            val (outputWidth, outputHeight) = if (cropRect != null) {
                Pair(cropRect.width, cropRect.height)
            } else if (rotationAngle == 90 || rotationAngle == -90 || rotationAngle == 270) {
                Pair(videoHeight, videoWidth)
            } else {
                Pair(videoWidth, videoHeight)
            }

            // Setup encoder
            val encoderFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                outputWidth,
                outputHeight
            )
            encoderFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, 5000000)
            encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = InputSurface(encoder.createInputSurface())
            encoder.start()

            // Setup texture renderer
            textureRenderer = TextureRenderer()
            inputSurface.makeCurrent()
            textureRenderer.surfaceCreated()

            // Setup muxer
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoMuxerTrack = -1
            var audioMuxerTrack = -1
            var muxerStarted = false

            // Process video frames
            extractor.selectTrack(videoTrackIndex)

            val decoderBufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()
            var decoderDone = false
            var encoderDone = false
            var decoderOutputAvailable = false

            while (!encoderDone) {
                // Feed decoder
                if (!decoderDone) {
                    val inputBufferId = decoder.dequeueInputBuffer(10000)
                    if (inputBufferId >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferId)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputBufferId, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            decoderDone = true
                        } else {
                            val presentationTime = extractor.sampleTime
                            decoder.queueInputBuffer(
                                inputBufferId, 0, sampleSize,
                                presentationTime, 0
                            )
                            extractor.advance()
                        }
                    }
                }

                // Get decoded frame
                val decoderStatus = decoder.dequeueOutputBuffer(decoderBufferInfo, 10000)
                if (decoderStatus >= 0) {
                    val doRender = decoderBufferInfo.size > 0

                    decoder.releaseOutputBuffer(decoderStatus, doRender)

                    if (doRender) {
                        outputSurface.awaitNewImage()

                        // Render transformed frame to encoder
                        inputSurface.makeCurrent()
                        android.opengl.GLES20.glViewport(0, 0, outputWidth, outputHeight)

                        val stMatrix = FloatArray(16)
                        val mvpMatrix = FloatArray(16)
                        android.opengl.Matrix.setIdentityM(stMatrix, 0)
                        android.opengl.Matrix.setIdentityM(mvpMatrix, 0)

                        // Apply crop transformation via texture coordinates
                        if (cropRect != null) {
                            val left = cropRect.x.toFloat() / videoWidth
                            val top = cropRect.y.toFloat() / videoHeight
                            val right = (cropRect.x + cropRect.width).toFloat() / videoWidth
                            val bottom = (cropRect.y + cropRect.height).toFloat() / videoHeight

                            val scaleX = 1.0f / (right - left)
                            val scaleY = 1.0f / (bottom - top)
                            val translateX = -left * scaleX * 2.0f
                            val translateY = -top * scaleY * 2.0f

                            val cropMatrix = FloatArray(16)
                            android.opengl.Matrix.setIdentityM(cropMatrix, 0)
                            android.opengl.Matrix.translateM(cropMatrix, 0, translateX, translateY, 0f)
                            android.opengl.Matrix.scaleM(cropMatrix, 0, scaleX, scaleY, 1f)

                            val tempMatrix = FloatArray(16)
                            android.opengl.Matrix.multiplyMM(tempMatrix, 0, cropMatrix, 0, stMatrix, 0)
                            System.arraycopy(tempMatrix, 0, stMatrix, 0, 16)
                        }

                        // Apply rotation transformation
                        if (rotationAngle != 0) {
                            val angle = when (rotationAngle) {
                                90 -> 90f
                                -90, 270 -> -90f
                                180 -> 180f
                                else -> 0f
                            }
                            android.opengl.Matrix.rotateM(mvpMatrix, 0, angle, 0f, 0f, 1f)
                        }

                        textureRenderer.drawFrame(stMatrix, mvpMatrix)

                        inputSurface.setPresentationTime(decoderBufferInfo.presentationTimeUs * 1000)
                        inputSurface.swapBuffers()
                    }

                    if ((decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        encoder.signalEndOfInputStream()
                    }
                }

                // Get encoded data
                val encoderStatus = encoder.dequeueOutputBuffer(encoderBufferInfo, 10000)
                when {
                    encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = encoder.outputFormat
                        videoMuxerTrack = muxer.addTrack(newFormat)
                        if (audioFormat == null || audioMuxerTrack != -1) {
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    encoderStatus >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(encoderStatus)!!
                        if (encoderBufferInfo.size > 0 && muxerStarted) {
                            muxer.writeSampleData(videoMuxerTrack, outputBuffer, encoderBufferInfo)
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false)

                        if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encoderDone = true
                        }
                    }
                }
            }

            // Process audio track (passthrough)
            if (audioTrackIndex >= 0 && audioFormat != null) {
                processAudioTrack(extractor, muxer, audioTrackIndex, audioFormat)
            }

            promise.resolve(outputPath)

        } catch (e: Exception) {
            promise.reject("PROCESSING_ERROR", "Failed to process video: ${e.message}", e)
        } finally {
            textureRenderer?.release()
            inputSurface?.release()
            outputSurface?.release()
            decoder?.stop()
            decoder?.release()
            encoder?.stop()
            encoder?.release()
            extractor?.release()
            try {
                muxer?.stop()
            } catch (e: Exception) {
                // Ignore stop errors
            }
            muxer?.release()
        }
    }

    private fun processAudioTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        audioTrackIndex: Int,
        audioFormat: MediaFormat
    ) {
        extractor.selectTrack(audioTrackIndex)
        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val audioMuxerTrack = muxer.addTrack(audioFormat)
        val maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val buffer = ByteBuffer.allocateDirect(maxBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) {
                break
            }

            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = extractor.sampleTime
            bufferInfo.flags = extractor.sampleFlags
            bufferInfo.offset = 0

            muxer.writeSampleData(audioMuxerTrack, buffer, bufferInfo)
            extractor.advance()
        }
    }
}
