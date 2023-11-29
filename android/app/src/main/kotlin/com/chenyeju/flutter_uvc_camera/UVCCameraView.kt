package com.chenyeju.flutter_uvc_camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.chenyeju.flutter_uvc_camera.databinding.ActivityMainBinding
import com.chenyeju.flutter_uvc_camera.databinding.FragmentUvcCameraBinding
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.utils.ToastUtils
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

internal class UVCCameraView(context: Context,channel: MethodChannel, id: Int, creationParams: Map<*, *>?) : PlatformView {
    private val isCameraOpened = false
    private val cameraFragment = UVCCameraFragment (channel)
    private var viewBinding: ActivityMainBinding

    init {
        viewBinding = ActivityMainBinding.inflate(LayoutInflater.from(context))
    }


    fun isCameraOpened(): Boolean {
        return isCameraOpened
    }

    fun takePicture() {
        cameraFragment.takePicture()
    }

    fun startPreview() {
        cameraFragment.startPreview()
    }

    fun stopPreview() {
        cameraFragment.stopPreview()
    }

    private fun replaceDemoFragment(fragment: Fragment) {

    }


    override fun getView(): View {
        return viewBinding.root
    }

    override fun dispose() {

    }


}