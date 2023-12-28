package com.sdk

interface UVCPictureCallback {
    fun onPictureTaken(path: String)
    fun onError(error: String)
}