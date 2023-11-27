package com.chenyeju.flutter_uvc_camera

import android.view.LayoutInflater
import android.view.View
import com.chenyeju.flutter_uvc_camera.databinding.ActivityMainBinding
import io.flutter.plugin.platform.PlatformView

class UVCCameraView(inflater: LayoutInflater) : PlatformView {

    private var viewBinding: ActivityMainBinding = ActivityMainBinding.inflate(inflater)


    override fun getView(): View {
        return  viewBinding.root
    }

    override fun dispose() {

    }


}