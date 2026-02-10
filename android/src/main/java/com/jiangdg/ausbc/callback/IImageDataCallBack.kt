package com.jiangdg.ausbc.callback

/**
 * Image data callback for in-memory capture.
 */
interface IImageDataCallBack {
    fun onBegin()
    fun onComplete(data: ByteArray)
    fun onError(error: String)
}
