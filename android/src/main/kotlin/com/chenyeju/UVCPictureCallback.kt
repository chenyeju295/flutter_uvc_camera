package com.chenyeju

interface UVCPictureCallback {
    fun onPictureTaken(path: String)
    fun onError(error: String)
}

interface PermissionResultListener {
    fun onPermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
}
