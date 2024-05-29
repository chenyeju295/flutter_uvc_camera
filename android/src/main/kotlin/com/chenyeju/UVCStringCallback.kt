package com.chenyeju

interface UVCStringCallback {
    fun onSuccess(path: String)
    fun onError(error: String)
}

interface PermissionResultListener {
    fun onPermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
}
