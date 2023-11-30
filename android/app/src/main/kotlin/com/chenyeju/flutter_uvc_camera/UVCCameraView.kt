package com.chenyeju.flutter_uvc_camera

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.chenyeju.flutter_uvc_camera.databinding.ActivityMainBinding
import io.flutter.plugin.platform.PlatformView

internal class UVCCameraView(context: Context, id: Int, creationParams: Map<*, *>?) : PlatformView {
    private var viewBinding = ActivityMainBinding.inflate(LayoutInflater.from(context))

    override fun getView(): View {
        return viewBinding.root
    }

    override fun dispose() {

    }


}