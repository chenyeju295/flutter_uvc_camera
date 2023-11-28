package com.chenyeju.flutter_uvc_camera

import android.content.Context
import android.view.LayoutInflater
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory


class UVCCameraViewFactory( channel: MethodChannel) : PlatformViewFactory(StandardMessageCodec.INSTANCE){

    private val _channel: MethodChannel = channel
    private lateinit var cameraView : UVCCameraView


    init {
    }

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        cameraView = UVCCameraView(LayoutInflater.from(context),_channel)
        return cameraView
    }

    fun takePicture() {
        cameraView.takePicture()
    }

    fun startPreview() {
        cameraView.startPreview()
    }

    fun stopPreview() {
        cameraView.stopPreview()
    }

    fun isCameraOpened(): Boolean? {
        return cameraView.isCameraOpened()
    }

}