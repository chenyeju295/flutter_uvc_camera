package com.chenyeju.flutter_uvc_camera

import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.StandardMessageCodec

class UVCCameraPlugin : FlutterPlugin,
    MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var mUVCCameraViewFactory: UVCCameraViewFactory

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "uvc_camera")
        channel.setMethodCallHandler(this)
        mUVCCameraViewFactory = UVCCameraViewFactory(channel)
        flutterPluginBinding.platformViewRegistry
            .registerViewFactory("uvc_camera_view", mUVCCameraViewFactory)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android " + Build.VERSION.RELEASE)
            }
            "isCameraOpened" -> {
                val cameraOpened: Boolean = mUVCCameraViewFactory.isCameraOpened() == true
                result.success(cameraOpened.toString())
            }
            "takePicture" -> {
                mUVCCameraViewFactory.takePicture()
                result.success("takePicture")
            }
            "startPreview" -> {
                mUVCCameraViewFactory.startPreview()
                result.success("startPreview")
            }
            "stopPreview" -> {
                mUVCCameraViewFactory.stopPreview()
                result.success("stopPreview")
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}