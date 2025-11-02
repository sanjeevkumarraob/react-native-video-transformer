package com.videotransformer

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.opengl.GLES20
import android.opengl.Matrix

/**
 * Manages the video transformation pipeline with OpenGL rendering
 */
class VideoTransformationPipeline(
    private val inputSurface: InputSurface,
    private val outputSurface: OutputSurface,
    private val textureRenderer: TextureRenderer
) {
    private val stMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    init {
        Matrix.setIdentityM(stMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)
    }

    /**
     * Setup the transformation pipeline
     */
    fun setup() {
        inputSurface.makeCurrent()
        textureRenderer.surfaceCreated()
    }

    /**
     * Process a frame with rotation and/or crop transformations
     */
    fun processFrame(
        videoWidth: Int,
        videoHeight: Int,
        rotationAngle: Int,
        cropRect: VideoTransformerModule.CropRect?,
        presentationTimeUs: Long
    ) {
        // Make input surface current for rendering
        inputSurface.makeCurrent()

        // Calculate output dimensions based on transformations
        val (outputWidth, outputHeight) = calculateOutputDimensions(
            videoWidth, videoHeight, rotationAngle, cropRect
        )

        // Set viewport to output dimensions
        GLES20.glViewport(0, 0, outputWidth, outputHeight)

        // Get the texture transform from the decoder's SurfaceTexture
        outputSurface.awaitNewImage()
        val surfaceTexture = getSurfaceTextureFromOutput()
        surfaceTexture?.getTransformMatrix(stMatrix)

        // Build MVP matrix with transformations
        buildTransformationMatrix(
            videoWidth, videoHeight,
            outputWidth, outputHeight,
            rotationAngle, cropRect
        )

        // Render the frame
        textureRenderer.drawFrame(stMatrix, mvpMatrix)

        // Set presentation time and swap buffers
        inputSurface.setPresentationTime(presentationTimeUs * 1000)
        inputSurface.swapBuffers()
    }

    /**
     * Calculate output dimensions based on rotation and crop
     */
    private fun calculateOutputDimensions(
        videoWidth: Int,
        videoHeight: Int,
        rotationAngle: Int,
        cropRect: VideoTransformerModule.CropRect?
    ): Pair<Int, Int> {
        return if (cropRect != null) {
            // Crop dimensions
            Pair(cropRect.width, cropRect.height)
        } else if (rotationAngle == 90 || rotationAngle == -90 || rotationAngle == 270) {
            // Rotation swaps dimensions
            Pair(videoHeight, videoWidth)
        } else {
            // No transformation or 180 rotation
            Pair(videoWidth, videoHeight)
        }
    }

    /**
     * Build the Model-View-Projection matrix for transformations
     */
    private fun buildTransformationMatrix(
        videoWidth: Int,
        videoHeight: Int,
        outputWidth: Int,
        outputHeight: Int,
        rotationAngle: Int,
        cropRect: VideoTransformerModule.CropRect?
    ) {
        Matrix.setIdentityM(mvpMatrix, 0)

        if (cropRect != null) {
            // Apply crop transformation
            applyCropTransformation(videoWidth, videoHeight, cropRect)
        }

        if (rotationAngle != 0) {
            // Apply rotation transformation
            applyRotationTransformation(rotationAngle)
        }
    }

    /**
     * Apply crop transformation by adjusting texture coordinates
     */
    private fun applyCropTransformation(
        videoWidth: Int,
        videoHeight: Int,
        cropRect: VideoTransformerModule.CropRect
    ) {
        // Calculate normalized crop coordinates (0.0 to 1.0)
        val left = cropRect.x.toFloat() / videoWidth
        val top = cropRect.y.toFloat() / videoHeight
        val right = (cropRect.x + cropRect.width).toFloat() / videoWidth
        val bottom = (cropRect.y + cropRect.height).toFloat() / videoHeight

        // Calculate scale and translation to apply crop
        val scaleX = 1.0f / (right - left)
        val scaleY = 1.0f / (bottom - top)
        val translateX = -left * scaleX * 2.0f
        val translateY = -top * scaleY * 2.0f

        // Apply to texture matrix
        val cropMatrix = FloatArray(16)
        Matrix.setIdentityM(cropMatrix, 0)
        Matrix.translateM(cropMatrix, 0, translateX, translateY, 0f)
        Matrix.scaleM(cropMatrix, 0, scaleX, scaleY, 1f)

        // Multiply with existing stMatrix
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, cropMatrix, 0, stMatrix, 0)
        System.arraycopy(tempMatrix, 0, stMatrix, 0, 16)
    }

    /**
     * Apply rotation transformation
     */
    private fun applyRotationTransformation(angle: Int) {
        val rotationMatrix = FloatArray(16)
        Matrix.setIdentityM(rotationMatrix, 0)

        // Rotate around Z-axis
        val rotationDegrees = when (angle) {
            90 -> 90f
            -90, 270 -> -90f
            180 -> 180f
            else -> 0f
        }

        Matrix.rotateM(rotationMatrix, 0, rotationDegrees, 0f, 0f, 1f)

        // Multiply with existing mvpMatrix
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, rotationMatrix, 0, mvpMatrix, 0)
        System.arraycopy(tempMatrix, 0, mvpMatrix, 0, 16)
    }

    /**
     * Get SurfaceTexture from OutputSurface (helper method)
     */
    private fun getSurfaceTextureFromOutput(): SurfaceTexture? {
        // This is a placeholder - in practice, OutputSurface exposes its SurfaceTexture
        // The actual implementation is in OutputSurface.awaitNewImage()
        return null
    }

    fun release() {
        textureRenderer.release()
    }
}
