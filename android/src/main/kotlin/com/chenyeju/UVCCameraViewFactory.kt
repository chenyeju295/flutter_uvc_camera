package com.chenyeju

import android.content.Context
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import com.jiangdg.ausbc.utils.Logger


class UVCCameraViewFactory(private val plugin: FlutterUVCCameraPlugin, private var channel: MethodChannel) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    private lateinit var cameraView: UVCCameraView
    private var frameEventSink: EventChannel.EventSink? = null
    private var streamEventSink: EventChannel.EventSink? = null
    
    companion object {
        private const val TAG = "UVCCameraViewFactory"
    }

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        cameraView = UVCCameraView(context, this.channel, args)
        plugin.setPermissionResultListener(cameraView)
        return cameraView
    }

    fun setFrameEventSink(eventSink: EventChannel.EventSink?) {
        frameEventSink = eventSink
        cameraView.setFrameEventSink(eventSink)
        Logger.i(TAG, "Frame event sink ${if (eventSink == null) "cleared" else "set"}")
    }
    
    fun setStreamEventSink(eventSink: EventChannel.EventSink?) {
        streamEventSink = eventSink
        cameraView.setStreamEventSink(eventSink)
        Logger.i(TAG, "Stream event sink ${if (eventSink == null) "cleared" else "set"}")
    }

    fun initCamera() {
        cameraView.initCamera()
    }

    fun openUVCCamera() {
        cameraView.openUVCCamera()
    }

    fun takePicture(callback: UVCStringCallback) {
        cameraView.takePicture(callback)
    }
    
    fun captureVideo(callback: UVCStringCallback) {
        cameraView.captureVideo(callback)
    }
    
    fun captureVideoStop(callback: UVCStringCallback) {
        cameraView.captureVideoStop(callback)
    }

    fun captureStreamStart() {
        cameraView.captureStreamStart()
    }
    
    fun captureStreamStop() {
        cameraView.captureStreamStop()
    }

    fun startFrameStreaming() {
        cameraView.startFrameStreaming()
    }

    fun stopFrameStreaming() {
        cameraView.stopFrameStreaming()
    }

    fun getAllPreviewSizes() = cameraView.getAllPreviewSizes()
    
    fun getCurrentCameraRequestParameters() = cameraView.getCurrentCameraRequestParameters()

    fun closeCamera() {
        cameraView.closeCamera()
    }

    fun updateResolution(arguments: Any?) {
        cameraView.updateResolution(arguments)
    }

    fun getCameraInfo(): Map<String, Any> {
        return cameraView.getCameraInfo()
    }

    fun isCameraOpened(): Boolean {
        return try {
            cameraView.isCameraOpened()
        } catch (e: Exception) {
            false
        }
    }
}