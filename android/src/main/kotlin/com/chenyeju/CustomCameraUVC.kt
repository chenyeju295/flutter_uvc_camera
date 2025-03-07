/*
 * Copyright 2017-2023 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chenyeju

import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.provider.MediaStore
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.MultiCameraClient.Companion.CAPTURE_TIMES_OUT_SEC
import com.jiangdg.ausbc.MultiCameraClient.Companion.MAX_NV21_DATA
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.utils.CameraUtils
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.MediaUtils
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.uvc.IButtonCallback
import com.jiangdg.uvc.IFrameCallback
import com.jiangdg.uvc.UVCCamera
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue

/** UVC Camera
 *
 * @author Created by jiangdg on 2023/1/15
 */
class CameraUVC(ctx: Context, device: UsbDevice, private val params: Any?
    ) : MultiCameraClient.ICamera(ctx, device) {
    private var mUvcCamera: UVCCamera? = null
    private val mCameraPreviewSize by lazy {
        arrayListOf<PreviewSize>()
    }
    private val mFrameDataQueue = LinkedBlockingQueue<ByteArray>(5) // 帧数据队列，限制大小防止OOM
    private var mLastTimestamp = 0L // 用于计算帧率
    private var mFrameCount = 0
    private var mFpsStartTime = 0L
    private var mIsMJPEGMode = true // 跟踪当前是否为MJPEG模式
    
    companion object {
        private const val TAG = "CameraUVC"
        private const val FPS_CALCULATION_INTERVAL_MS = 1000 // 计算FPS的间隔
    }

    // 优化的帧回调处理
    private val frameCallBack = IFrameCallback { frame ->
        frame?.apply {
            try {
                // 计算FPS
                val currentTime = System.currentTimeMillis()
                if (mFpsStartTime == 0L) {
                    mFpsStartTime = currentTime
                } else {
                    mFrameCount++
                    if (currentTime - mFpsStartTime >= FPS_CALCULATION_INTERVAL_MS) {
                        val fps = mFrameCount * 1000 / (currentTime - mFpsStartTime)
                        mFrameCount = 0
                        mFpsStartTime = currentTime
                        if (Utils.debugCamera) {
                            Logger.d(TAG, "Current FPS: $fps")
                        }
                    }
                }
                
                frame.position(0)
                val data = ByteArray(capacity())
                get(data)
                mCameraRequest?.apply {
                    // 帧宽度检查
                    if (data.size != previewWidth * previewHeight * 3 / 2) {
                        if (Utils.debugCamera) {
                            Logger.w(TAG, "Frame size mismatch: expected ${previewWidth * previewHeight * 3 / 2}, got ${data.size}")
                        }
                        return@IFrameCallback
                    }
                    
                    // 帧处理
                    processFrameData(data, previewWidth, previewHeight)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error processing frame", e)
            }
        }
    }
    
    // 新增方法：处理帧数据
    private fun processFrameData(data: ByteArray, width: Int, height: Int) {
        mCameraRequest?.apply {
            // 预览回调
            mPreviewDataCbList.forEach { cb ->
                cb?.onPreviewData(data, width, height, IPreviewDataCallBack.DataFormat.NV21)
            }
            
            // 图像缓存
            if (mNV21DataQueue.size >= MAX_NV21_DATA) {
                mNV21DataQueue.removeLast()
            }
            mNV21DataQueue.offerFirst(data)
            
            // 视频处理
            putVideoData(data)
            
            // 帧队列更新 - 用于流处理
            if (mFrameDataQueue.remainingCapacity() == 0) {
                mFrameDataQueue.poll() // 移除最旧的帧
            }
            mFrameDataQueue.offer(data.clone()) // 使用clone避免数据被覆盖
        }
    }
    
    // 提供获取最新帧的方法，供流媒体处理使用
    fun getLatestFrame(): ByteArray? {
        return mFrameDataQueue.peek()
    }
    
    // 获取当前帧格式
    fun getCurrentFrameFormat(): Int {
        return if (mIsMJPEGMode) UVCCamera.FRAME_FORMAT_MJPEG else UVCCamera.FRAME_FORMAT_YUYV
    }

    override fun getAllPreviewSizes(aspectRatio: Double?): MutableList<PreviewSize> {
        val previewSizeList = arrayListOf<PreviewSize>()
        if (mUvcCamera?.supportedSizeList?.isNotEmpty() == true) {
            mUvcCamera?.supportedSizeList
        }  else {
            mUvcCamera?.getSupportedSizeList(UVCCamera.FRAME_FORMAT_YUYV)
        }?.let { sizeList ->
            if (mCameraPreviewSize.isEmpty()) {
                mCameraPreviewSize.clear()
                sizeList.forEach { size->
                    val width = size.width
                    val height = size.height
                    mCameraPreviewSize.add(PreviewSize(width, height))
                }
            }
            mCameraPreviewSize
        }?.onEach { size ->
            val width = size.width
            val height = size.height
            val ratio = width.toDouble() / height
            if (aspectRatio == null || aspectRatio == ratio) {
                previewSizeList.add(PreviewSize(width, height))
            }
        }
        if (Utils.debugCamera) {
            Logger.i(TAG, "aspect ratio = $aspectRatio, getAllPreviewSizes = $previewSizeList, ")
        }

        return previewSizeList
    }

    override fun <T> openCameraInternal(cameraView: T) {
        // Check permissions for Android 10+
        if (Utils.isTargetSdkOverP(ctx)) {
            if (!CameraUtils.hasCameraPermission(ctx)) {
                closeCamera()
                postStateEvent(ICameraStateCallBack.State.ERROR, "Has no CAMERA permission.")
                Logger.e(TAG,"open camera failed, need Manifest.permission.CAMERA permission when targetSdk>=28")
                return
            }
            if (!CameraUtils.hasStoragePermission(ctx)) {
                closeCamera()
                postStateEvent(ICameraStateCallBack.State.ERROR, "Has no STORAGE permission.")
                Logger.e(TAG,"open camera failed, need storage permissions when targetSdk>=28")
                return
            }
        }

        if (mCtrlBlock == null) {
            closeCamera()
            postStateEvent(ICameraStateCallBack.State.ERROR, "Usb control block can not be null ")
            return
        }

        // 1. create a UVCCamera with error handling
        val request = mCameraRequest!!
        try {
            mUvcCamera = UVCCamera().apply {
                open(mCtrlBlock)
            }
        } catch (e: Exception) {
            closeCamera()
            postStateEvent(ICameraStateCallBack.State.ERROR, "open camera failed ${e.localizedMessage}")
            Logger.e(TAG, "open camera failed.", e)
            return
        }

        // 2. Configure camera parameters with fallback options
        var minFps = 10
        var maxFps = 30 // Reduced from 60 to improve stability
        var frameFormat = UVCCamera.FRAME_FORMAT_MJPEG
        var bandwidthFactor = 1.0f // Reduced bandwidth factor for better compatibility

        if (params is Map<*, *>) {
            minFps = (params["minFps"] as? Number)?.toInt() ?: minFps
            maxFps = (params["maxFps"] as? Number)?.toInt() ?: maxFps
            frameFormat = (params["frameFormat"] as? Number)?.toInt() ?: frameFormat
            bandwidthFactor = (params["bandwidthFactor"] as? Number)?.toFloat() ?: bandwidthFactor
        }

        // 3. Set preview size with multiple fallback options
        var previewSize = getSuitableSize(request.previewWidth, request.previewHeight).apply {
            mCameraRequest!!.previewWidth = width
            mCameraRequest!!.previewHeight = height
        }

        try {
            Logger.i(TAG, "Attempting to set preview size: $previewSize")
            if (!isPreviewSizeSupported(previewSize)) {
                // Try to find the closest supported size
                val supportedSizes = mUvcCamera?.supportedSizeList ?: emptyList()
                previewSize = findClosestSize(supportedSizes, previewSize.width, previewSize.height)
                if (previewSize == null) {
                    closeCamera()
                    postStateEvent(ICameraStateCallBack.State.ERROR, "No supported preview size found")
                    return
                }
                mCameraRequest!!.previewWidth = previewSize.width
                mCameraRequest!!.previewHeight = previewSize.height
            }

            initEncodeProcessor(previewSize.width, previewSize.height)
            
            // Try MJPEG first
            try {
                mUvcCamera?.setPreviewSize(
                    previewSize.width,
                    previewSize.height,
                    minFps,
                    maxFps,
                    UVCCamera.FRAME_FORMAT_MJPEG,
                    bandwidthFactor
                )
                mIsMJPEGMode = true
                Logger.i(TAG, "Using MJPEG format for camera preview")
            } catch (e: Exception) {
                Logger.w(TAG, "MJPEG format failed, trying YUYV format")
                // Fall back to YUYV if MJPEG fails
                try {
                    mUvcCamera?.setPreviewSize(
                        previewSize.width,
                        previewSize.height,
                        minFps,
                        maxFps,
                        UVCCamera.FRAME_FORMAT_YUYV,
                        bandwidthFactor
                    )
                    mIsMJPEGMode = false
                    Logger.i(TAG, "Using YUYV format for camera preview")
                } catch (e2: Exception) {
                    closeCamera()
                    postStateEvent(ICameraStateCallBack.State.ERROR, "Failed to set preview size: ${e2.localizedMessage}")
                    Logger.e(TAG, "Failed to set preview size with both MJPEG and YUYV formats", e2)
                    return
                }
            }
        } catch (e: Exception) {
            closeCamera()
            postStateEvent(ICameraStateCallBack.State.ERROR, "Failed to configure camera: ${e.localizedMessage}")
            Logger.e(TAG, "Failed to configure camera", e)
            return
        }

        // 4. Set frame callback if needed
        if (!isNeedGLESRender || mCameraRequest!!.isRawPreviewData || mCameraRequest!!.isCaptureRawImage) {
            try {
                mUvcCamera?.setFrameCallback(frameCallBack, UVCCamera.PIXEL_FORMAT_YUV420SP)
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to set frame callback, continuing without it", e)
            }
        }

        // 5. Start preview with surface handling
        try {
            when(cameraView) {
                is Surface -> {
                    mUvcCamera?.setPreviewDisplay(cameraView)
                }
                is SurfaceTexture -> {
                    mUvcCamera?.setPreviewTexture(cameraView)
                }
                is SurfaceView -> {
                    mUvcCamera?.setPreviewDisplay(cameraView.holder)
                }
                is TextureView -> {
                    mUvcCamera?.setPreviewTexture(cameraView.surfaceTexture)
                }
                else -> {
                    throw IllegalStateException("Unsupported view type: $cameraView")
                }
            }

            // 6. Configure camera parameters
            try {
                mUvcCamera?.autoFocus = true
                mUvcCamera?.autoWhiteBlance = true
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to set auto focus or white balance", e)
            }

            // 7. Start preview
            mUvcCamera?.startPreview()
            mUvcCamera?.updateCameraParams()
            isPreviewed = true
            postStateEvent(ICameraStateCallBack.State.OPENED)
            
            if (Utils.debugCamera) {
                Logger.i(TAG, "Preview started successfully: name=${device.deviceName}, size=$previewSize")
            }
        } catch (e: Exception) {
            closeCamera()
            postStateEvent(ICameraStateCallBack.State.ERROR, "Failed to start preview: ${e.localizedMessage}")
            Logger.e(TAG, "Failed to start preview", e)
        }
    }

    private fun findClosestSize(supportedSizes: List<com.jiangdg.uvc.Size>, targetWidth: Int, targetHeight: Int): PreviewSize? {
        if (supportedSizes.isEmpty()) return null
        
        var minDiff = Int.MAX_VALUE
        var closestSize: PreviewSize? = null
        
        for (size in supportedSizes) {
            val diff = Math.abs(size.width * size.height - targetWidth * targetHeight)
            if (diff < minDiff) {
                minDiff = diff
                closestSize = PreviewSize(size.width, size.height)
            }
        }
        
        return closestSize
    }

    override fun closeCameraInternal() {
        postStateEvent(ICameraStateCallBack.State.CLOSED)
        isPreviewed = false
        releaseEncodeProcessor()
        // 清理队列
        mFrameDataQueue.clear()
        mFrameCount = 0
        mFpsStartTime = 0L
        mUvcCamera?.destroy()
        mUvcCamera = null
        if (Utils.debugCamera) {
            Logger.i(TAG, " stop preview, name = ${device.deviceName}")
        }
    }

    override fun captureImageInternal(savePath: String?, callback: ICaptureCallBack) {
        mSaveImageExecutor.submit {
            if (! CameraUtils.hasStoragePermission(ctx)) {
                mMainHandler.post {
                    callback.onError("have no storage permission")
                }
                Logger.e(TAG,"open camera failed, have no storage permission")
                return@submit
            }
            if (! isPreviewed) {
                mMainHandler.post {
                    callback.onError("camera not previewing")
                }
                Logger.i(TAG, "captureImageInternal failed, camera not previewing")
                return@submit
            }
            val data = mNV21DataQueue.pollFirst(CAPTURE_TIMES_OUT_SEC, TimeUnit.SECONDS)
            if (data == null) {
                mMainHandler.post {
                    callback.onError("Times out")
                }
                Logger.i(TAG, "captureImageInternal failed, times out.")
                return@submit
            }
            mMainHandler.post {
                callback.onBegin()
            }
            val date = mDateFormat.format(System.currentTimeMillis())
            val title = savePath ?: "IMG_UVC_$date"
            val displayName = savePath ?: "$title.jpg"
            val path = savePath ?: "$mCameraDir/$displayName"
            val location = Utils.getGpsLocation(ctx)
            val width = mCameraRequest!!.previewWidth
            val height = mCameraRequest!!.previewHeight
            val ret = MediaUtils.saveYuv2Jpeg(path, data, width, height)
            if (! ret) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                mMainHandler.post {
                    callback.onError("save yuv to jpeg failed.")
                }
                Logger.w(TAG, "save yuv to jpeg failed.")
                return@submit
            }
            val values = ContentValues()
            values.put(MediaStore.Images.ImageColumns.TITLE, title)
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
            values.put(MediaStore.Images.ImageColumns.DATA, path)
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
            ctx.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            mMainHandler.post {
                callback.onComplete(path)
            }
            if (Utils.debugCamera) { Logger.i(TAG, "captureImageInternal save path = $path") }
        }
    }

    /**
     * Is mic supported
     *
     * @return true camera support mic
     */
    fun isMicSupported() = CameraUtils.isCameraContainsMic(this.device)

    /**
     * Send camera command
     *
     * This method cannot be verified, please use it with caution
     */
    fun sendCameraCommand(command: Int) {
        mCameraHandler?.post {
            mUvcCamera?.sendCommand(command)
        }
    }

    /**
     * Set auto focus
     *
     * @param enable true enable auto focus
     */
    fun setAutoFocus(enable: Boolean) {
        mUvcCamera?.autoFocus = enable
    }

    /**
     * Get auto focus
     *
     * @return true enable auto focus
     */
    fun getAutoFocus() = mUvcCamera?.autoFocus

    /**
     * Reset auto focus
     */
    fun resetAutoFocus() {
        mUvcCamera?.resetFocus()
    }

    /**
     * Set auto white balance
     *
     * @param autoWhiteBalance true enable auto white balance
     */
    fun setAutoWhiteBalance(autoWhiteBalance: Boolean) {
        mUvcCamera?.autoWhiteBlance = autoWhiteBalance
    }

    /**
     * Get auto white balance
     *
     * @return true enable auto white balance
     */
    fun getAutoWhiteBalance() = mUvcCamera?.autoWhiteBlance

    /**
     * Set zoom
     *
     * @param zoom zoom value, 0 means reset
     */
    fun setZoom(zoom: Int) {
        mUvcCamera?.zoom = zoom
    }

    /**
     * Get zoom
     */
    fun getZoom() = mUvcCamera?.zoom

    /**
     * Reset zoom
     */
    fun resetZoom() {
        mUvcCamera?.resetZoom()
    }

    /**
     * Set gain
     *
     * @param gain gain value, 0 means reset
     */
    fun setGain(gain: Int) {
        mUvcCamera?.gain = gain
    }

    /**
     * Get gain
     */
    fun getGain() = mUvcCamera?.gain

    /**
     * Reset gain
     */
    fun resetGain() {
        mUvcCamera?.resetGain()
    }

    /**
     * Set gamma
     *
     * @param gamma gamma value, 0 means reset
     */
    fun setGamma(gamma: Int) {
        mUvcCamera?.gamma = gamma
    }

    /**
     * Get gamma
     */
    fun getGamma() = mUvcCamera?.gamma

    /**
     * Reset gamma
     */
    fun resetGamma() {
        mUvcCamera?.resetGamma()
    }

    /**
     * Set brightness
     *
     * @param brightness brightness value, 0 means reset
     */
    fun setBrightness(brightness: Int) {
        mUvcCamera?.brightness = brightness
    }

    /**
     * Get brightness
     */
    fun getBrightness() = mUvcCamera?.brightness

    /**
     * Reset brightnes
     */
    fun resetBrightness() {
        mUvcCamera?.resetBrightness()
    }

    /**
     * Set contrast
     *
     * @param contrast contrast value, 0 means reset
     */
    fun setContrast(contrast: Int) {
        mUvcCamera?.contrast = contrast
    }

    /**
     * Get contrast
     */
    fun getContrast() = mUvcCamera?.contrast

    /**
     * Reset contrast
     */
    fun resetContrast() {
        mUvcCamera?.resetContrast()
    }

    /**
     * Set sharpness
     *
     * @param sharpness sharpness value, 0 means reset
     */
    fun setSharpness(sharpness: Int) {
        mUvcCamera?.sharpness = sharpness
    }

    /**
     * Get sharpness
     */
    fun getSharpness() = mUvcCamera?.sharpness

    /**
     * Reset sharpness
     */
    fun resetSharpness() {
        mUvcCamera?.resetSharpness()
    }

    ///设置硬件按钮回调
    fun setButtonCallback(callback: IButtonCallback?) {
        mUvcCamera?.setButtonCallback(callback)
    }

    /**
     * Set saturation
     *
     * @param saturation saturation value, 0 means reset
     */
    fun setSaturation(saturation: Int) {
        mUvcCamera?.saturation = saturation
    }

    /**
     * Get saturation
     */
    fun getSaturation() = mUvcCamera?.saturation

    /**
     * Reset saturation
     */
    fun resetSaturation() {
        mUvcCamera?.resetSaturation()
    }

    /**
     * Set hue
     *
     * @param hue hue value, 0 means reset
     */
    fun setHue(hue: Int) {
        mUvcCamera?.hue = hue
    }

    /**
     * Get hue
     */
    fun getHue() = mUvcCamera?.hue

    /**
     * Reset saturation
     */
    fun resetHue() {
        mUvcCamera?.resetHue()
    }

    // 新增方法：提供直接访问相机参数的接口
    fun getPreviewSize(): PreviewSize? {
        val width = mCameraRequest?.previewWidth ?: 0
        val height = mCameraRequest?.previewHeight ?: 0
        if (width == 0 || height == 0) return null
        return PreviewSize(width, height)
    }
    
    // 新增方法：获取当前的FPS范围
    fun getFpsRange(): Pair<Int, Int> {
        return Pair(
            mCameraRequest?.minFps ?: 10,
            mCameraRequest?.maxFps ?: 30
        )
    }
}