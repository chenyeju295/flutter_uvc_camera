package com.chenyeju

import android.app.Activity
import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import com.jiangdg.ausbc.utils.Logger

class FlutterUVCCameraPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private val channelName = "flutter_uvc_camera/channel"
    private val viewName = "flutter_uvc_camera"
    private var channel: MethodChannel? = null
    private lateinit var mUVCCameraViewFactory: UVCCameraViewFactory
    private var activity: Activity? = null
    private var permissionResultListener: PermissionResultListener? = null
    private var mActivityPluginBinding: ActivityPluginBinding? = null
    private var requestPermissionsResultListener: io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener? =
        null
    
    companion object {
        private const val TAG = "FlutterUVCCameraPlugin"
        private var debug = false
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, channelName)
        channel!!.setMethodCallHandler(this)
        mUVCCameraViewFactory = UVCCameraViewFactory(this, channel!!)
        flutterPluginBinding.platformViewRegistry.registerViewFactory(viewName, mUVCCameraViewFactory)
        Logger.i(TAG, "Plugin attached to engine")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
        Logger.i(TAG, "Plugin detached from engine")
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        mActivityPluginBinding = binding
        requestPermissionsResultListener =
            io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener { requestCode, permissions, grantResults ->
                permissionResultListener?.onPermissionResult(requestCode, permissions, grantResults)
                true
            }
        binding.addRequestPermissionsResultListener(requestPermissionsResultListener!!)
        Logger.i(TAG, "Plugin attached to activity")
    }

    fun setPermissionResultListener(listener: PermissionResultListener) {
        this.permissionResultListener = listener
        Logger.i(TAG, "Permission result listener set")
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Logger.i(TAG, "Plugin detached from activity for config changes")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Logger.i(TAG, "Plugin reattached to activity for config changes")
    }

    override fun onDetachedFromActivity() {
        activity = null
        if (requestPermissionsResultListener != null) {
            mActivityPluginBinding?.removeRequestPermissionsResultListener(requestPermissionsResultListener!!)
            requestPermissionsResultListener = null
            mActivityPluginBinding = null
        }
        Logger.i(TAG, "Plugin detached from activity")
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                "initCamera" -> {
                    mUVCCameraViewFactory.initCamera()
                    result.success(null)
                }
                
                "openUVCCamera" -> {
                    mUVCCameraViewFactory.openUVCCamera()
                    result.success(null)
                }
                
                "takePicture" -> {
                    val savePath = call.argument<String>("path")
                    mUVCCameraViewFactory.takePicture(
                        object : UVCStringCallback {
                            override fun onSuccess(path: String) {
                                result.success(path)
                            }
                            override fun onError(error: String) {
                                result.error("CAMERA_ERROR", error, null)
                            }
                        }
                    )
                }
                
                "captureVideo" -> {
                    mUVCCameraViewFactory.captureVideo(
                        object : UVCStringCallback {
                            override fun onSuccess(path: String) {
                                result.success(path)
                            }
                            override fun onError(error: String) {
                                result.error("CAMERA_ERROR", error, null)
                            }
                        }
                    )
                }
                
                "captureVideoStop" -> {
                    mUVCCameraViewFactory.captureVideo(
                        object : UVCStringCallback {
                            override fun onSuccess(path: String) {
                                result.success(path)
                            }
                            override fun onError(error: String) {
                                result.error("CAMERA_ERROR", error, null)
                            }
                        }
                    )
                }
                
                "captureStreamStart" -> {
                    mUVCCameraViewFactory.captureStreamStart()
                    result.success(null)
                }
                
                "captureStreamStop" -> {
                    mUVCCameraViewFactory.captureStreamStop()
                    result.success(null)
                }
                
                "startFrameStreaming" -> {
                    mUVCCameraViewFactory.startFrameStreaming()
                    result.success(null)
                }
                
                "stopFrameStreaming" -> {
                    mUVCCameraViewFactory.stopFrameStreaming()
                    result.success(null)
                }
                
                "closeCamera" -> {
                    mUVCCameraViewFactory.closeCamera()
                    result.success(null)
                }
                
                "getAllPreviewSizes" -> {
                    result.success(mUVCCameraViewFactory.getAllPreviewSizes())
                }
                
                "getCurrentCameraRequestParameters" -> {
                    result.success(mUVCCameraViewFactory.getCurrentCameraRequestParameters())
                }
                
                "getCameraInfo" -> {
                    result.success(mUVCCameraViewFactory.getCameraInfo())
                }
                
                "updateResolution" -> {
                    mUVCCameraViewFactory.updateResolution(call.arguments)
                    result.success(null)
                }
                
                "isCameraOpened" -> {
                    result.success(mUVCCameraViewFactory.isCameraOpened())
                }
                
                "setDebugMode" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    debug = enabled
                    Logger.i(TAG, "Debug mode set to $enabled")
                    result.success(null)
                }
                
                "getPlatformVersion" -> {
                    result.success("Android " + Build.VERSION.RELEASE)
                }
                
                else -> {
                    result.notImplemented()
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error handling method call: ${call.method}", e)
            result.error("EXCEPTION", e.message, e.stackTraceToString())
        }
    }
}