package com.chenyeju.flutter_uvc_camera

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chenyeju.flutter_uvc_camera.databinding.ActivityMainBinding
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import io.flutter.plugin.common.MethodChannel

class UVCCameraFragment(channel: MethodChannel) : CameraFragment() {
    private lateinit var viewBinding: ActivityMainBinding
    private val _channel = channel

    fun takePicture() {
        if (! isCameraOpened()) {
            _channel.invokeMethod("callFlutter", "摄像头未打开")
            return
        }
        captureImage()
    }

    fun checkCamera(): Boolean {
        try {
            getCurrentCamera()?.let { strategy ->
                if (strategy is CameraUVC) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun captureImage() {
        captureImage(
            object : ICaptureCallBack {
                override fun onBegin() {
                    ToastUtils.show("开始拍照")
                }

                override fun onComplete(path: String?) {
                    _channel.invokeMethod("takePictureSuccess", path)
                }

                override fun onError(error: String?) {
                    ToastUtils.show(error ?: "未知异常")
                    _channel.invokeMethod("callFlutter", error)
                }

            }
        )
    }



    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> handleCameraOpened()
            ICameraStateCallBack.State.CLOSED -> handleCameraClosed()
            ICameraStateCallBack.State.ERROR -> handleCameraError(msg)
        }
    }

    private fun handleCameraError(msg: String?) {
        _channel.invokeMethod("CameraState", "ERROR：$msg")
    }

    private fun handleCameraClosed() {
        _channel.invokeMethod("CameraState", "OPENED")
    }

    private fun handleCameraOpened() {
        _channel.invokeMethod("CameraState", "OPENED")
    }
    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return viewBinding.fragmentContainer
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        viewBinding = ActivityMainBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun getGravity(): Int = Gravity.CENTER
}