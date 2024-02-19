package com.sdk

import android.app.Activity
import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler


class FlutterUVCCameraPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private val channelName = "flutter_uvc_camera/channel"
    private val viewName = "uvc_camera_view"
    private var channel: MethodChannel?=null
    private lateinit var mUVCCameraViewFactory : UVCCameraViewFactory
    private var activity: Activity ?= null
    private var permissionResultListener: PermissionResultListener? = null
    private var mActivityPluginBinding: ActivityPluginBinding ?= null
    private var requestPermissionsResultListener:  io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, channelName )
        channel!!.setMethodCallHandler(this)
        mUVCCameraViewFactory = UVCCameraViewFactory(this,channel!!)
        flutterPluginBinding.platformViewRegistry.registerViewFactory(viewName, mUVCCameraViewFactory)
    }


    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
    }


    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        mActivityPluginBinding = binding
        requestPermissionsResultListener =io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener { requestCode, permissions, grantResults ->
            permissionResultListener?.onPermissionResult(requestCode, permissions, grantResults)
            true
        }
        binding.addRequestPermissionsResultListener (requestPermissionsResultListener!!)
    }
    fun setPermissionResultListener(listener: PermissionResultListener) {
        this.permissionResultListener = listener
    }
    override fun onDetachedFromActivityForConfigChanges() {

    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {

    }

    override fun onDetachedFromActivity() {
        activity = null
        if (requestPermissionsResultListener != null) {
            mActivityPluginBinding?.removeRequestPermissionsResultListener(requestPermissionsResultListener!!)
            requestPermissionsResultListener = null
            mActivityPluginBinding = null
        }
    }


    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "initializeCamera" -> {
                mUVCCameraViewFactory.initCamera()
            }
            "openUVCCamera" -> {
                mUVCCameraViewFactory.openUVCCamera()
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

//            "resetCamera" -> {
//                    mUVCCameraViewFactory.resetCamera()
//            }
            "closeCamera" -> {
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

