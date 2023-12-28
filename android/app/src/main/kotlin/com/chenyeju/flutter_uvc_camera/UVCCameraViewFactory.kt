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


class UVCCameraViewFactory(private var channel: MethodChannel) : PlatformViewFactory(StandardMessageCodec.INSTANCE),ActivityAware{
    private lateinit var cameraView : UVCCameraView
    private lateinit var context: Context
    private var activity: Activity? = null


    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        this.context = context
        cameraView = UVCCameraView(context, this.channel,args)

        return cameraView
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
//        checkCameraPermission()
    }
    private fun checkCameraPermission() : Boolean {
        val hasCameraPermission = PermissionChecker.checkSelfPermission(context,
            Manifest.permission.CAMERA
        )
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED) {
            if (activity == null) {
                return false
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity!!,
                    Manifest.permission.CAMERA
                )
            ) {
                channel.invokeMethod(
                    "callFlutter",
                    "You have already denied permission access. Go to the Settings page to turn on permissions\n"
                )
            }
            ActivityCompat.requestPermissions(
                activity!!,
                arrayOf(
                    Manifest.permission.CAMERA,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
                0
            )
            return false
        }
        return true
    }


    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    fun initCamera(arguments: Any?){
        checkCameraPermission()
        cameraView.initCamera(arguments)
    }

    ///
    /// 拍照
    ///


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