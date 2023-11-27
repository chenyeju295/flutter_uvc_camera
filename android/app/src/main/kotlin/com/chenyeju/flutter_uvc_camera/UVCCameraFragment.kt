package com.chenyeju.flutter_uvc_camera

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chenyeju.flutter_uvc_camera.databinding.FragmentUvcCameraBinding
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
class UVCCameraFragment : CameraFragment() {
    private lateinit var viewBinding: FragmentUvcCameraBinding

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {



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