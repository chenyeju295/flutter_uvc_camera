package com.chenyeju.flutter_uvc_camera

import android.content.Context
import android.view.LayoutInflater
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory


class UVCCameraViewFactory : PlatformViewFactory(StandardMessageCodec.INSTANCE){
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        return UVCCameraView(LayoutInflater.from(context))
    }
}