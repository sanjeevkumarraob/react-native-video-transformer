package com.videotransformer

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Handles OpenGL texture rendering with transformations
 */
class TextureRenderer {
    private val triangleVerticesData = floatArrayOf(
        // X, Y, U, V
        -1.0f, -1.0f, 0f, 0f,
        1.0f, -1.0f, 1f, 0f,
        -1.0f, 1.0f, 0f, 1f,
        1.0f, 1.0f, 1f, 1f
    )

    private val triangleVertices: FloatBuffer = ByteBuffer.allocateDirect(
        triangleVerticesData.size * 4
    ).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private val mvpMatrix = FloatArray(16)
    private val stMatrix = FloatArray(16)

    private var program = 0
    private var textureID = -1
    private var mvpMatrixHandle = 0
    private var stMatrixHandle = 0
    private var positionHandle = 0
    private var textureHandle = 0

    // Vertex shader for rendering video frames
    private val vertexShader = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uSTMatrix;
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 vTextureCoord;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vTextureCoord = (uSTMatrix * aTextureCoord).xy;
        }
    """.trimIndent()

    // Fragment shader for external OES texture
    private val fragmentShader = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;
        void main() {
            gl_FragColor = texture2D(sTexture, vTextureCoord);
        }
    """.trimIndent()

    init {
        triangleVertices.put(triangleVerticesData).position(0)
        Matrix.setIdentityM(stMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)
    }

    fun surfaceCreated() {
        program = createProgram(vertexShader, fragmentShader)
        if (program == 0) {
            throw RuntimeException("Failed to create program")
        }

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        checkGlError("glGetAttribLocation aPosition")
        if (positionHandle == -1) {
            throw RuntimeException("Could not get attrib location for aPosition")
        }

        textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        checkGlError("glGetAttribLocation aTextureCoord")
        if (textureHandle == -1) {
            throw RuntimeException("Could not get attrib location for aTextureCoord")
        }

        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        checkGlError("glGetUniformLocation uMVPMatrix")
        if (mvpMatrixHandle == -1) {
            throw RuntimeException("Could not get uniform location for uMVPMatrix")
        }

        stMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
        checkGlError("glGetUniformLocation uSTMatrix")
        if (stMatrixHandle == -1) {
            throw RuntimeException("Could not get uniform location for uSTMatrix")
        }

        // Create texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureID = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID)
        checkGlError("glBindTexture textureID")

        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        checkGlError("glTexParameter")
    }

    fun getTextureId(): Int = textureID

    fun drawFrame(stMatrix: FloatArray, mvpMatrix: FloatArray) {
        checkGlError("onDrawFrame start")

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)
        checkGlError("glUseProgram")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID)

        triangleVertices.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle, 2, GLES20.GL_FLOAT, false,
            16, triangleVertices
        )
        checkGlError("glVertexAttribPointer maPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        checkGlError("glEnableVertexAttribArray positionHandle")

        triangleVertices.position(2)
        GLES20.glVertexAttribPointer(
            textureHandle, 2, GLES20.GL_FLOAT, false,
            16, triangleVertices
        )
        checkGlError("glVertexAttribPointer textureHandle")
        GLES20.glEnableVertexAttribArray(textureHandle)
        checkGlError("glEnableVertexAttribArray textureHandle")

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")

        GLES20.glFinish()
    }

    fun release() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        if (textureID != -1) {
            val textures = intArrayOf(textureID)
            GLES20.glDeleteTextures(1, textures, 0)
            textureID = -1
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) {
            return 0
        }

        var program = GLES20.glCreateProgram()
        checkGlError("glCreateProgram")
        if (program == 0) {
            return 0
        }

        GLES20.glAttachShader(program, vertexShader)
        checkGlError("glAttachShader vertexShader")
        GLES20.glAttachShader(program, fragmentShader)
        checkGlError("glAttachShader fragmentShader")
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteProgram(program)
            program = 0
        }

        return program
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        checkGlError("glCreateShader type=$shaderType")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader)
            shader = 0
        }

        return shader
    }

    private fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$op: glError $error")
        }
    }
}
