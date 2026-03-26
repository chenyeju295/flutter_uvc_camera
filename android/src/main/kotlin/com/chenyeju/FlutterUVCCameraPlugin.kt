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

class FlutterUVCCameraPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private val methodChannelName = "flutter_uvc_camera/channel"
    private val videoStreamChannelName = "flutter_uvc_camera/video_stream"
    private val viewName = "uvc_camera_view"

    private var methodChannel: MethodChannel? = null
    private var videoStreamChannel: EventChannel? = null
    private var videoStreamHandler = VideoStreamHandler()

    private var mUVCCameraViewFactory: UVCCameraViewFactory? = null
    private var activity: Activity? = null
    private var permissionResultListener: PermissionResultListener? = null
    private var mActivityPluginBinding: ActivityPluginBinding? = null
    private var requestPermissionsResultListener: io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, methodChannelName)
        methodChannel!!.setMethodCallHandler(this)

        videoStreamChannel = EventChannel(flutterPluginBinding.binaryMessenger, videoStreamChannelName)
        videoStreamChannel!!.setStreamHandler(videoStreamHandler)

        val factory = UVCCameraViewFactory(this, videoStreamHandler)
        mUVCCameraViewFactory = factory
        flutterPluginBinding.platformViewRegistry.registerViewFactory(viewName, factory)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        mUVCCameraViewFactory = null

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

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}

    override fun onDetachedFromActivity() {
        activity = null
        if (requestPermissionsResultListener != null) {
            mActivityPluginBinding?.removeRequestPermissionsResultListener(requestPermissionsResultListener!!)
            requestPermissionsResultListener = null
            mActivityPluginBinding = null
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val factory = mUVCCameraViewFactory
        when (call.method) {
            "initializeCamera" -> {
                if (factory == null) {
                    result.error("NOT_READY", "Platform view not created yet", null)
                    return@onMethodCall
                }
                factory.initCamera()
                result.success(null)
            }
            "openUVCCamera" -> {
                if (factory == null) {
                    result.error("NOT_READY", "Platform view not created yet", null)
                    return@onMethodCall
                }
                factory.openUVCCamera()
                result.success(null)
            }
            "closeCamera" -> {
                factory?.cameraView?.closeCamera()
                result.success(null)
            }
            "takePicture" -> {
                val view = factory?.cameraView
                if (view == null) {
                    result.error("NOT_READY", "Camera view not created yet", null)
                    return@onMethodCall
                }
                view.takePicture(
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
            "takePictureBytes" -> {
                val view = factory?.cameraView
                if (view == null) {
                    result.error("NOT_READY", "Camera view not created yet", null)
                    return@onMethodCall
                }
                view.takePictureBytes(object : com.jiangdg.ausbc.callback.IImageDataCallBack {
                    override fun onBegin() {}
                    override fun onComplete(data: ByteArray) {
                        result.success(data)
                    }
                    override fun onError(error: String) {
                        result.error("CAPTURE_ERROR", error, null)
                    }
                })
            }
            "captureVideo" -> {
                val view = factory?.cameraView
                if (view == null) {
                    result.error("NOT_READY", "Camera view not created yet", null)
                    return@onMethodCall
                }
                view.captureVideo(
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
                factory?.cameraView?.captureStreamStart()
                result.success(null)
            }
            "captureStreamStop" -> {
                factory?.cameraView?.captureStreamStop()
                result.success(null)
            }
            "startPlayMic" -> {
                result.success(factory?.cameraView?.startPlayMic() ?: false)
            }
            "stopPlayMic" -> {
                result.success(factory?.cameraView?.stopPlayMic() ?: false)
            }
            "isMicPlaying" -> {
                result.success(factory?.cameraView?.isMicPlaying() ?: false)
            }
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
            "setAudioFrameSizeLimit" -> {
                val size = call.argument<Int>("size") ?: 0
                videoStreamHandler.audioFrameSizeLimit = size
                result.success(null)
            }
            "getAudioFrameSizeLimit" -> {
                result.success(videoStreamHandler.audioFrameSizeLimit)
            }
            "setStreamDataEnabled" -> {
                val enableVideo = call.argument<Boolean>("video") ?: true
                val enableAudio = call.argument<Boolean>("audio") ?: true
                videoStreamHandler.enableVideoFrames = enableVideo
                videoStreamHandler.enableAudioFrames = enableAudio
                result.success(null)
            }
            "getStreamDataEnabled" -> {
                result.success(
                    mapOf(
                        "video" to videoStreamHandler.enableVideoFrames,
                        "audio" to videoStreamHandler.enableAudioFrames,
                    )
                )
            }
            "setVideoKeyframesOnly" -> {
                val enabled = call.argument<Boolean>("enabled") ?: false
                videoStreamHandler.videoKeyframesOnly = enabled
                result.success(null)
            }
            "getVideoKeyframesOnly" -> {
                result.success(videoStreamHandler.videoKeyframesOnly)
            }
            "setVideoSampleEveryN" -> {
                val n = call.argument<Int>("n") ?: 1
                videoStreamHandler.setVideoSampleEveryN(n)
                result.success(null)
            }
            "getVideoSampleEveryN" -> {
                result.success(videoStreamHandler.getVideoSampleEveryN())
            }
            "getAllPreviewSizes" -> {
                result.success(factory?.cameraView?.getAllPreviewSizes())
            }
            "getCurrentCameraRequestParameters" -> {
                result.success(factory?.cameraView?.getCurrentCameraRequestParameters())
            }
            "getPreviewSurfaceInfo" -> {
                result.success(factory?.cameraView?.getPreviewSurfaceInfo())
            }
            "updateResolution" -> {
                factory?.cameraView?.updateResolution(call.arguments)
                result.success(null)
            }
            "updateCameraViewParams" -> {
                val view = factory?.cameraView
                if (view == null) {
                    result.error("NOT_READY", "Camera view not created yet", null)
                    return@onMethodCall
                }
                try {
                    view.updateCameraViewParams(call.arguments)
                    result.success(null)
                } catch (e: Exception) {
                    result.error("UPDATE_PARAMS_ERROR", e.localizedMessage, null)
                }
            }
            "setCameraFeature" -> {
                val feature = call.argument<String>("feature") ?: ""
                val value = call.argument<Int>("value") ?: 0
                result.success(factory?.cameraView?.setCameraFeature(feature, value) ?: false)
            }
            "resetCameraFeature" -> {
                val feature = call.argument<String>("feature") ?: ""
                result.success(factory?.cameraView?.resetCameraFeature(feature) ?: false)
            }
            "getCameraFeature" -> {
                val feature = call.argument<String>("feature") ?: ""
                result.success(factory?.cameraView?.getCameraFeature(feature))
            }
            "getAllCameraFeatures" -> {
                result.success(factory?.cameraView?.getAllCameraFeatures())
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
