package com.chenyeju.flutter_uvc_camera

import android.content.Context
import android.view.LayoutInflater
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory


class UVCCameraViewFactory : PlatformViewFactory(StandardMessageCodec.INSTANCE){

    private lateinit var cameraView : UVCCameraView

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        cameraView = UVCCameraView(context,  viewId, args )
        return cameraView
    }
}