package com.chenyeju

import android.app.Activity
import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

/**
 * Main Flutter plugin class for UVC camera
 */
class FlutterUVCCameraPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private val methodChannelName = "flutter_uvc_camera/channel"
    private val videoStreamChannelName = "flutter_uvc_camera/video_stream"
    private val viewName = "uvc_camera_view"
    
    private var methodChannel: MethodChannel? = null
    private var videoStreamChannel: EventChannel? = null
    private var videoStreamHandler = VideoStreamHandler()
    
    private lateinit var mUVCCameraViewFactory: UVCCameraViewFactory
    private var activity: Activity? = null
    private var permissionResultListener: PermissionResultListener? = null
    private var mActivityPluginBinding: ActivityPluginBinding? = null
    private var requestPermissionsResultListener: io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        // 设置Method Channel
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, methodChannelName)
        methodChannel!!.setMethodCallHandler(this)
        
        // 设置Video Stream EventChannel
        videoStreamChannel = EventChannel(flutterPluginBinding.binaryMessenger, videoStreamChannelName)
        videoStreamChannel!!.setStreamHandler(videoStreamHandler)
        
        // 初始化视图工厂
        mUVCCameraViewFactory = UVCCameraViewFactory(this, methodChannel!!, videoStreamHandler)
        flutterPluginBinding.platformViewRegistry.registerViewFactory(viewName, mUVCCameraViewFactory)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        
        videoStreamChannel?.setStreamHandler(null)
        videoStreamChannel = null
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
    }

    fun setPermissionResultListener(listener: PermissionResultListener) {
        this.permissionResultListener = listener
    }

    override fun onDetachedFromActivityForConfigChanges() {
        // Not implemented
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        // Not implemented
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
            // Basic camera operations
            "initializeCamera" -> {
                mUVCCameraViewFactory.initCamera()
                result.success(null)
            }

            "openUVCCamera" -> {
                mUVCCameraViewFactory.openUVCCamera()
                result.success(null)
            }

            "closeCamera" -> {
                mUVCCameraViewFactory.closeCamera()
                result.success(null)
            }

            // Capture operations
            "takePicture" -> {
                mUVCCameraViewFactory.takePicture(
                    object : UVCStringCallback {
                        override fun onSuccess(path: String) {
                            result.success(path)
                        }
                        override fun onError(error: String) {
                            result.error("CAPTURE_ERROR", error, null)
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
                            result.error("CAPTURE_ERROR", error, null)
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
            
            // Stream control
            "setVideoFrameRateLimit" -> {
                val fps = call.argument<Int>("fps") ?: 30
                videoStreamHandler.frameRateLimit = fps
                result.success(null)
            }
            
            "getVideoFrameRateLimit" -> {
                result.success(videoStreamHandler.frameRateLimit)
            }
            
            "setVideoFrameSizeLimit" -> {
                val size = call.argument<Int>("size") ?: 0
                videoStreamHandler.frameSizeLimit = size
                result.success(null)
            }

            // Camera settings
            "getAllPreviewSizes" -> {
               result.success(mUVCCameraViewFactory.getAllPreviewSizes())
            }

            "getCurrentCameraRequestParameters" -> {
                result.success(mUVCCameraViewFactory.getCurrentCameraRequestParameters())
            }

            "updateResolution" -> {
                mUVCCameraViewFactory.updateResolution(call.arguments)
                result.success(null)
            }
            
            // Camera feature methods
            "setCameraFeature" -> {
                val feature = call.argument<String>("feature") ?: ""
                val value = call.argument<Int>("value") ?: 0
                val success = mUVCCameraViewFactory.setCameraFeature(feature, value)
                result.success(success)
            }
            
            "resetCameraFeature" -> {
                val feature = call.argument<String>("feature") ?: ""
                val success = mUVCCameraViewFactory.resetCameraFeature(feature)
                result.success(success)
            }
            
            "getCameraFeature" -> {
                val feature = call.argument<String>("feature") ?: ""
                val value = mUVCCameraViewFactory.getCameraFeature(feature)
                result.success(value)
            }
            
            "getAllCameraFeatures" -> {
                val features = mUVCCameraViewFactory.getAllCameraFeatures()
                result.success(features)
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