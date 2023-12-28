package com.chenyeju.flutter_uvc_camera

import android.os.Build
import com.chenyeju.flutter_uvc_camera.callback.UVCPictureCallback
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler


class MainPlugin : FlutterPlugin, MethodCallHandler {
    private val channelName = "com.chenyeju.flutter_uvc_camera/channel"
    private lateinit var channel: MethodChannel
    private lateinit var mUVCCameraViewFactory :UVCCameraViewFactory

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, channelName )
        channel.setMethodCallHandler(this)
        mUVCCameraViewFactory = UVCCameraViewFactory(channel)
        flutterPluginBinding.platformViewRegistry.registerViewFactory("uvc_camera_view", mUVCCameraViewFactory)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "initializeCamera" -> {
                mUVCCameraViewFactory.initCamera(call.arguments)
            }
            "takePicture" -> {
                mUVCCameraViewFactory.takePicture(
                    object : UVCPictureCallback {
                        override fun onPictureTaken(path: String) {
                            result.success(path)
                        }

                        override fun onError(error: String) {
                            result.error("error", error, error)
                        }
                    }
                )
            }
            "getAllPreviewSize" -> {
                mUVCCameraViewFactory.getAllPreviewSize()
            }
            "getDevicesList" -> {
                mUVCCameraViewFactory.getDevicesList()
            }
            "listenToDevice" -> {
                // Implement listening logic here
            }
            "writeToDevice" -> {
                if(call.arguments is Int){
                    mUVCCameraViewFactory.writeToDevice(call.arguments as Int)}
            }
            "closeConnection" -> {
                mUVCCameraViewFactory.closeCamera()
            }

            "getPlatformVersion" -> {
                result.success("Android " + Build.VERSION.RELEASE)
            }

            else -> {
                result.notImplemented()
            }
        }

    }



}

