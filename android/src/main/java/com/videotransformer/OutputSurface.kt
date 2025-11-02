package com.videotransformer

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.view.Surface

/**
 * Holds state associated with a Surface used for MediaCodec decoder output.
 * Creates an off-screen EGL surface for receiving decoded frames via SurfaceTexture.
 */
class OutputSurface : SurfaceTexture.OnFrameAvailableListener {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private val frameSyncObject = Object()
    private var frameAvailable = false

    private var textureRenderer: TextureRenderer? = null

    init {
        setup()
    }

    private fun setup() {
        textureRenderer = TextureRenderer()
        eglSetup()
        makeCurrent()
        textureRenderer?.surfaceCreated()
        surfaceTexture = SurfaceTexture(textureRenderer?.getTextureId() ?: 0)
        surfaceTexture?.setOnFrameAvailableListener(this)
        surface = Surface(surfaceTexture)
    }

    private fun eglSetup() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("Unable to get EGL14 display")
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            eglDisplay = EGL14.EGL_NO_DISPLAY
            throw RuntimeException("Unable to initialize EGL14")
        }

        // Configure EGL for OpenGL ES 2.0
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attribList, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            throw RuntimeException("Unable to find RGB888 ES2 EGL config")
        }

        // Create an EGL rendering context
        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            contextAttribs, 0
        )
        checkEglError("eglCreateContext")
        if (eglContext === EGL14.EGL_NO_CONTEXT) {
            throw RuntimeException("Failed to create EGL context")
        }

        // Create an off-screen surface
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, 1,
            EGL14.EGL_HEIGHT, 1,
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttribs, 0)
        checkEglError("eglCreatePbufferSurface")
        if (eglSurface === EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("Failed to create EGL surface")
        }
    }

    fun getSurface(): Surface? = surface

    fun release() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
        }

        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE

        surface?.release()
        surfaceTexture?.release()

        textureRenderer?.release()
        textureRenderer = null
    }

    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    fun awaitNewImage() {
        val TIMEOUT_MS = 2500
        synchronized(frameSyncObject) {
            while (!frameAvailable) {
                try {
                    frameSyncObject.wait(TIMEOUT_MS.toLong())
                    if (!frameAvailable) {
                        throw RuntimeException("Frame wait timed out")
                    }
                } catch (ie: InterruptedException) {
                    throw RuntimeException(ie)
                }
            }
            frameAvailable = false
        }
        surfaceTexture?.updateTexImage()
    }

    fun drawImage(invert: Boolean = false) {
        val stMatrix = FloatArray(16)
        surfaceTexture?.getTransformMatrix(stMatrix)

        // Apply vertical flip if needed
        if (invert) {
            stMatrix[5] = -stMatrix[5]
            stMatrix[13] = 1.0f - stMatrix[13]
        }

        textureRenderer?.drawFrame(stMatrix, android.opengl.Matrix.FloatArray(16).apply {
            android.opengl.Matrix.setIdentityM(this, 0)
        })
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        synchronized(frameSyncObject) {
            if (frameAvailable) {
                throw RuntimeException("frameAvailable already set, frame could be dropped")
            }
            frameAvailable = true
            frameSyncObject.notifyAll()
        }
    }

    private fun checkEglError(msg: String) {
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            throw RuntimeException("$msg: EGL error: 0x${Integer.toHexString(error)}")
        }
    }
}
