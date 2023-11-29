package com.chenyeju.flutter_uvc_camera

import android.content.Context
import android.view.LayoutInflater
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory


class UVCCameraViewFactory(channel: MethodChannel ) : PlatformViewFactory(StandardMessageCodec.INSTANCE){

    private lateinit var cameraView : UVCCameraView
     private  var mChannel : MethodChannel  = channel



    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        cameraView = UVCCameraView(context, mChannel, viewId, args as Map<*, *>?)
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

    fun isCameraOpened(): Boolean {
        return cameraView.isCameraOpened()
    }

}