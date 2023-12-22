package com.chenyeju.flutter_uvc_camera

interface UVCPictureCallback {
    fun onPictureTaken(path: String)
    fun onError(error: String)
}