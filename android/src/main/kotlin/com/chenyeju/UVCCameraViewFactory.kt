package com.chenyeju

import android.content.Context
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory


class UVCCameraViewFactory(
    private val plugin: FlutterUVCCameraPlugin,
    private var channel: MethodChannel,
    private val videoStreamHandler: VideoStreamHandler
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    private var cameraView: UVCCameraView? = null
    private val recordingTimerManager = RecordingTimerManager(videoStreamHandler)
    private var pendingInit = false
    private var pendingOpen = false

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val view = UVCCameraView(context, this.channel, args, videoStreamHandler, recordingTimerManager)
        cameraView = view
        plugin.setPermissionResultListener(view)
        if (pendingInit) {
            view.initCamera()
            pendingInit = false
        }
        if (pendingOpen) {
            view.openUVCCamera()
            pendingOpen = false
        }
        return view
    }

    fun initCamera() {
        val view = cameraView
        if (view == null) {
            pendingInit = true
            return
        }
        view.initCamera()
    }

    fun openUVCCamera() {
        val view = cameraView
        if (view == null) {
            pendingOpen = true
            return
        }
        view.openUVCCamera()
    }

    fun takePicture(callback: UVCStringCallback) {
        cameraView?.takePicture(callback)
    }
    
    fun captureVideo(callback: UVCStringCallback) {
        cameraView?.captureVideo(callback)
    }

    fun captureStreamStart() {
        cameraView?.captureStreamStart()
    }
    
    fun captureStreamStop() {
        cameraView?.captureStreamStop()
    }
    
    fun startPlayMic(): Boolean {
        return cameraView?.startPlayMic() ?: false
    }
    
    fun stopPlayMic(): Boolean {
        return cameraView?.stopPlayMic() ?: false
    }

    fun getAllPreviewSizes() = cameraView?.getAllPreviewSizes()
    
    fun getCurrentCameraRequestParameters() = cameraView?.getCurrentCameraRequestParameters()

    fun closeCamera() {
        cameraView?.closeCamera()
    }

    fun updateResolution(arguments: Any?) {
        cameraView?.updateResolution(arguments)
    }
    
    // 相机特性方法
    fun setCameraFeature(feature: String, value: Int): Boolean {
        return cameraView?.setCameraFeature(feature, value) ?: false
    }
    
    fun resetCameraFeature(feature: String): Boolean {
        return cameraView?.resetCameraFeature(feature) ?: false
    }
    
    fun getCameraFeature(feature: String): Int? {
        return cameraView?.getCameraFeature(feature)
    }
    
    fun getAllCameraFeatures(): String? {
        return cameraView?.getAllCameraFeatures()
    }
}
