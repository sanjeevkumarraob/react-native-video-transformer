package com.videotransformer

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class VideoTransformerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "VideoTransformer"
    }

    @ReactMethod
    fun rotateVideo(inputPath: String, angle: Int, promise: Promise) {
        // TODO: Implement Android video rotation using MediaCodec
        promise.reject("NOT_IMPLEMENTED", "Android implementation coming soon")
    }
}
