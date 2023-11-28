package com.chenyeju.flutter_uvc_camera

import android.Manifest
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.chenyeju.flutter_uvc_camera.databinding.ActivityMainBinding
import com.jiangdg.ausbc.utils.ToastUtils
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class UVCCameraView(inflater: LayoutInflater,channel: MethodChannel) : PlatformView {
    private val _channel: MethodChannel = channel
    private val isCameraOpened = false
    private var viewBinding: ActivityMainBinding = ActivityMainBinding.inflate(inflater)
    private val cameraFragment = UVCCameraFragment (channel)

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

    private fun replaceDemoFragment(activity: Activity, fragment: Fragment) {
        val hasCameraPermission = PermissionChecker.checkSelfPermission(activity,
            Manifest.permission.CAMERA
        )
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED || hasStoragePermission != PermissionChecker.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.CAMERA
                )) {
                ToastUtils.show("You have already denied permission access. Go to the Settings page to turn on permissions\n")
            }
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
                0
            )
            return
        }
        try {
            viewBinding.fragmentContainer.addView(
                fragment.view,
            )
//            transaction.replace(R.id.fragment_container, fragment)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun getView(): View {
        return  viewBinding.root
    }

    override fun dispose() {

    }


}