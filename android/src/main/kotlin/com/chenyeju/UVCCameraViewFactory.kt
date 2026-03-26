package com.chenyeju

import android.content.Context
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class UVCCameraViewFactory(
    private val plugin: FlutterUVCCameraPlugin,
    private val videoStreamHandler: VideoStreamHandler
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    var cameraView: UVCCameraView? = null
        private set
    private val recordingTimerManager = RecordingTimerManager(videoStreamHandler)
    private var pendingInit = false
    private var pendingOpen = false

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val view = UVCCameraView(context, args, videoStreamHandler, recordingTimerManager)
        cameraView = view
        plugin.setPermissionResultListener(view)
        videoStreamHandler.sendState("VIEW_READY", null)
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
}
