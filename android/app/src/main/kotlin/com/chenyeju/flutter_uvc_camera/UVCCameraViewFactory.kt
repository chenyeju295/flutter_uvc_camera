package com.chenyeju.flutter_uvc_camera

import UVCCameraView
import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ContextWrapper
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.chenyeju.flutter_uvc_camera.uvc_camera.PictureSaver
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
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