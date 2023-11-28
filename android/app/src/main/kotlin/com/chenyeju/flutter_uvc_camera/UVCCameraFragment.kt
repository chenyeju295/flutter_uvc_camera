package com.chenyeju.flutter_uvc_camera

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chenyeju.flutter_uvc_camera.databinding.FragmentUvcCameraBinding
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import io.flutter.plugin.common.MethodChannel

class UVCCameraFragment(channel: MethodChannel) : CameraFragment() {
    private lateinit var viewBinding: FragmentUvcCameraBinding
    private val _channel: MethodChannel = channel

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {



    }

    fun takePicture() {
        _channel.invokeMethod("callFlutter", "")

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

    private fun showRecentMedia(isImage: Boolean? = null) {
    }


    fun startPreview() {
    }

    fun stopPreview() {
    }

    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return viewBinding.cameraViewContainer
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        viewBinding = FragmentUvcCameraBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun getGravity(): Int = Gravity.CENTER
}