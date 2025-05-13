package com.chenyeju

import android.os.Handler
import android.os.Looper
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.uvc.UVCCamera

/**
 * Manages camera configuration and parameters
 */
class CameraConfigManager {
    // Default values
    companion object {
        const val DEFAULT_PREVIEW_WIDTH = 640
        const val DEFAULT_PREVIEW_HEIGHT = 480
        const val DEFAULT_MIN_FPS = 10
        const val DEFAULT_MAX_FPS = 60
        const val DEFAULT_FRAME_FORMAT = UVCCamera.FRAME_FORMAT_MJPEG
        const val DEFAULT_BANDWIDTH_FACTOR = 1.0f
    }
    
    // Camera parameters with defaults
    private var previewWidth = DEFAULT_PREVIEW_WIDTH
    private var previewHeight = DEFAULT_PREVIEW_HEIGHT
    private var minFps = DEFAULT_MIN_FPS
    private var maxFps = DEFAULT_MAX_FPS
    private var frameFormat = DEFAULT_FRAME_FORMAT
    private var bandwidthFactor = DEFAULT_BANDWIDTH_FACTOR
    private var captureRawImage = false
    private var rawPreviewData = false
    private var aspectRatioShow = true
    
    // Handler for camera operations
    val cameraHandler = Handler(Looper.getMainLooper())
    
    /**
     * Update configuration from Flutter parameters
     */
    fun updateFromParams(params: Any?) {
        if (params is Map<*, *>) {
            previewWidth = (params["previewWidth"] as? Number)?.toInt() ?: previewWidth
            previewHeight = (params["previewHeight"] as? Number)?.toInt() ?: previewHeight
            minFps = (params["minFps"] as? Number)?.toInt() ?: minFps
            maxFps = (params["maxFps"] as? Number)?.toInt() ?: maxFps
            frameFormat = (params["frameFormat"] as? Number)?.toInt() ?: frameFormat
            bandwidthFactor = (params["bandwidthFactor"] as? Number)?.toFloat() ?: bandwidthFactor
            captureRawImage = (params["captureRawImage"] as? Boolean) ?: captureRawImage
            rawPreviewData = (params["rawPreviewData"] as? Boolean) ?: rawPreviewData
            aspectRatioShow = (params["aspectRatioShow"] as? Boolean) ?: aspectRatioShow
        }
    }
    
    /**
     * Update resolution
     */
    fun updateResolution(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
    }
    
    /**
     * Get camera parameters for CustomCameraUVC
     */
    fun getCameraParams(): Map<String, Any> {
        return mapOf(
            "minFps" to minFps,
            "maxFps" to maxFps,
            "frameFormat" to frameFormat,
            "bandwidthFactor" to bandwidthFactor
        )
    }
    
    /**
     * Get current preview size
     */
    fun getPreviewSize(): PreviewSize {
        return PreviewSize(previewWidth, previewHeight)
    }
    
    /**
     * Build camera request
     */
    fun buildCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(previewWidth)
            .setPreviewHeight(previewHeight)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setDefaultRotateType(RotateType.ANGLE_0)
            .setAudioSource(CameraRequest.AudioSource.SOURCE_SYS_MIC)
            .setAspectRatioShow(aspectRatioShow)
            .setCaptureRawImage(captureRawImage)
            .setRawPreviewData(rawPreviewData)
            .create()
    }
} 