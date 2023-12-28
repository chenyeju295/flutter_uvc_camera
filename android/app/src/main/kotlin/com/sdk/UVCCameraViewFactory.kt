package com.sdk

import android.content.Context
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory


class UVCCameraViewFactory(private var channel: MethodChannel) : PlatformViewFactory(StandardMessageCodec.INSTANCE){
    private lateinit var cameraView : UVCCameraView
    private lateinit var context: Context


    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        this.context = context
        cameraView = UVCCameraView(context, this.channel,args)

        return cameraView
    }


    fun initCamera(arguments: Any?){
        cameraView.initCamera(arguments)
    }

    fun takePicture(callback: UVCPictureCallback){
        cameraView.takePicture(callback)
    }

    fun getAllPreviewSize() {
        cameraView.getAllPreviewSize()
    }

    fun getDevicesList() {
        cameraView.getDevicesList()
    }

    fun writeToDevice(i: Int) {
        cameraView.writeToDevice(i)

    }

    fun closeCamera() {
        cameraView.closeCamera()
    }


}