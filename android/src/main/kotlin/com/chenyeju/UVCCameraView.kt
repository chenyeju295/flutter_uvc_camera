package com.chenyeju

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.media.MediaScannerConnection
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.chenyeju.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.callback.IEncodeDataCallBack
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.SettableFuture
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.CaptureMediaView
import com.jiangdg.ausbc.widget.IAspectRatio
import com.jiangdg.usb.USBMonitor
import com.jiangdg.uvc.IButtonCallback
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class UVCCameraView(
    private val mContext: Context,
    private val mChannel: MethodChannel,
    private val params: Any?
) : PlatformView , PermissionResultListener, ICameraStateCallBack {
    private var mViewBinding = ActivityMainBinding.inflate(LayoutInflater.from(mContext))
    private var mActivity: Activity? = getActivityFromContext(mContext)
    private var mCameraView: IAspectRatio? = null
    private var mCameraClient: MultiCameraClient? = null
    private val mCameraMap = hashMapOf<Int, MultiCameraClient.ICamera>()
    private var mCurrentCamera: SettableFuture<MultiCameraClient.ICamera>? = null
    private var isCapturingVideoOrAudio: Boolean = false
    private val mRequestPermission: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }
    private val mMainHandler = Handler(Looper.getMainLooper())
    private var mStreamingActive = false
    private var mStreamingThread: Thread? = null
    private val mStreamingLock = Object()

    companion object {
        private const val TAG = "CameraView"
        private const val REQUEST_PERMISSION = 1
        private const val STREAM_INTERVAL_MS = 33 // ~30fps
        private const val MAX_STREAMING_RETRIES = 3
    }

//    init{
//        processingParams()
//    }
//
//    private fun processingParams() {
//        if (params is Map<*, *>) {
//
//        }
//    }

    override fun getView(): View {
        return mViewBinding.root
    }

    private fun setCameraERRORState(msg: String? = null) {
        mMainHandler.post {
            mChannel.invokeMethod("CameraState", "ERROR:$msg")
        }
    }

    fun initCamera(){
        checkCameraPermission()
        val cameraView = AspectRatioTextureView(mContext)
        handleTextureView(cameraView)
        mCameraView = cameraView
        cameraView.also { view->
            mViewBinding.fragmentContainer
                .apply {
                    removeAllViews()
                    addView(view, getViewLayoutParams(this))
                }
        }
    }

    fun openUVCCamera() {
        try {
            checkCameraPermission()
            openCamera()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to open UVC camera", e)
            setCameraERRORState("Failed to open camera: ${e.localizedMessage}")
        }
    }

    override fun dispose() {
        unRegisterMultiCamera()
        mViewBinding.fragmentContainer.removeAllViews()
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> handleCameraOpened()
            ICameraStateCallBack.State.CLOSED -> handleCameraClosed()
            ICameraStateCallBack.State.ERROR -> handleCameraError(msg)
        }
        logInfo("CameraState: $code ${msg ?: ""}") 
    }

    private fun handleCameraError(msg: String?) {
        mMainHandler.post {
            mChannel.invokeMethod("CameraState", "ERROR:$msg")
        }
    }

    private fun handleCameraClosed() {
        stopFrameStreaming()
        mMainHandler.post {
            mChannel.invokeMethod("CameraState", "CLOSED")
        }
    }

    private fun handleCameraOpened() {
        mMainHandler.post {
            mChannel.invokeMethod("CameraState", "OPENED")
        }
        setButtonCallback()
    }

    fun registerMultiCamera() {
        mCameraClient = MultiCameraClient(view.context, object : IDeviceConnectCallBack {
            override fun onAttachDev(device: UsbDevice?) {
                device ?: return
                view.context.let {
                    if (mCameraMap.containsKey(device.deviceId)) {
                        return
                    }
                    generateCamera(it, device).apply {
                        mCameraMap[device.deviceId] = this
                    }
                    if (mRequestPermission.get()) {
                        return@let
                    }
                    getDefaultCamera()?.apply {
                        if (vendorId == device.vendorId && productId == device.productId) {
                            Logger.i(TAG, "default camera pid: $productId, vid: $vendorId")
                            requestPermission(device)
                        }
                        return@let
                    }
                    requestPermission(device)
                }
            }

            override fun onDetachDec(device: UsbDevice?) {
                mCameraMap.remove(device?.deviceId)?.apply {
                    setUsbControlBlock(null)
                }
                mRequestPermission.set(false)
                try {
                    mCurrentCamera?.cancel(true)
                    mCurrentCamera = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onConnectDev(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                device ?: return
                ctrlBlock ?: return
                view.context ?: return
                mCameraMap[device.deviceId]?.apply {
                    setUsbControlBlock(ctrlBlock)
                }?.also { camera ->
                    try {
                        mCurrentCamera?.cancel(true)
                        mCurrentCamera = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    mCurrentCamera = SettableFuture()
                    mCurrentCamera?.set(camera)
                    openCamera(mCameraView)
                    Logger.i(TAG, "camera connection. pid: ${device.productId}, vid: ${device.vendorId}")
                }
            }

            override fun onDisConnectDec(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                closeCamera()
                mRequestPermission.set(false)
            }

            override fun onCancelDev(device: UsbDevice?) {
                mRequestPermission.set(false)
                try {
                    mCurrentCamera?.cancel(true)
                    mCurrentCamera = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        mCameraClient?.register()

    }

    fun unRegisterMultiCamera() {
        mCameraMap.values.forEach {
            it.closeCamera()
        }
        mCameraMap.clear()
        mCameraClient?.unRegister()
        mCameraClient?.destroy()
        mCameraClient = null
    }
    private fun handleTextureView(textureView: TextureView) {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                try {
                    registerMultiCamera()
                    checkCamera()
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in surface texture available", e)
                    setCameraERRORState("Surface texture error: ${e.localizedMessage}")
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                try {
                    surfaceSizeChanged(width, height)
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in surface texture size changed", e)
                }
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                try {
                    unRegisterMultiCamera()
                } catch (e: Exception) {
                    Logger.e(TAG, "Error in surface texture destroyed", e)
                }
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // No implementation needed
            }
        }
    }

    private fun checkCamera() {
        try {
            val device = getDefaultCamera()
            if (device == null) {
                setCameraERRORState("No camera detected")
                return
            }
            
            if (!mRequestPermission.get()) {
                requestPermission(device)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error checking camera", e)
            setCameraERRORState("Error checking camera: ${e.localizedMessage}")
        }
    }

    override fun onPermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        // 处理权限结果
        if (requestCode == 1230) {
            val index = permissions.indexOf(Manifest.permission.CAMERA)
            if (index >= 0 && grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                registerMultiCamera()
            } else {
                callFlutter("设备权限被拒绝" )
                setCameraERRORState(msg = "设备权限被拒绝")
            }


        }
    }
    private fun checkCameraPermission() {
        if (mActivity == null) {
            mActivity = getActivityFromContext(mContext)
            if (mActivity == null) {
                setCameraERRORState("Activity not found")
                return
            }
        }

        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val missingPermissions = requiredPermissions.filter {
            PermissionChecker.checkSelfPermission(mContext, it) != PermissionChecker.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                mActivity!!,
                missingPermissions.toTypedArray(),
                REQUEST_PERMISSION
            )
            return
        }

        // All permissions granted, proceed with initialization
        initializeCamera()
    }

    private fun initializeCamera() {
        try {
            val cameraView = AspectRatioTextureView(mContext)
            handleTextureView(cameraView)
            mCameraView = cameraView
            
            // Configure layout parameters
            val layoutParams = when (mViewBinding.fragmentContainer) {
                is FrameLayout -> FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
                is RelativeLayout -> RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                }
                is LinearLayout -> LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                else -> ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            mViewBinding.fragmentContainer.apply {
                removeAllViews()
                addView(cameraView, layoutParams)
            }

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize camera view", e)
            setCameraERRORState("Failed to initialize camera view: ${e.localizedMessage}")
        }
    }

    private fun callFlutter(msg: String, type: String? = null) {
        val data = HashMap<String, String>()
        data["type"] = type ?: "msg"
        data["msg"] = msg
        mChannel.invokeMethod("callFlutter", data)
    }


     fun getAllPreviewSizes() : String? {
         val previewSizes = getCurrentCamera()?.getAllPreviewSizes()
         if (previewSizes.isNullOrEmpty()) {
             callFlutter("Get camera preview size failed")
             return null
         }
         return Gson().toJson(previewSizes)
     }

    fun updateResolution(arguments: Any?) {
        val map = arguments as HashMap<*, *>
        val width = map["width"] as Int
        val height = map["height"] as Int
        getCurrentCamera()?.updateResolution(width, height)
    }

   fun getCurrentCameraRequestParameters(): String? {
      val size = getCurrentCamera()?.getCameraRequest()
       if (size == null) {
           callFlutter("Get camera info failed")
           return null
       }
       return Gson().toJson(size)
    }


    private fun getActivityFromContext(context: Context?): Activity? {
        if (context == null) {
            return null
        }
        if (context is Activity) {
            return context
        }
        if (context is Application || context is Service) {
            return null
        }
        var c = context
        while (c != null) {
            if (c is ContextWrapper) {
                c = c.baseContext
                if (c is Activity) {
                    return c
                }
            } else {
                return null
            }
        }
        return null
    }


    private fun getCurrentCamera(): MultiCameraClient.ICamera? {
        return try {
            mCurrentCamera?.get(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun requestPermission(device: UsbDevice?) {
        mRequestPermission.set(true)
        mCameraClient?.requestPermission(device)
    }


    fun generateCamera(ctx: Context, device: UsbDevice): MultiCameraClient.ICamera {
        return CameraUVC(ctx, device,params)
    }

    fun getDefaultCamera(): UsbDevice? = null
    fun getDefaultEffect() = getCurrentCamera()?.getDefaultEffect()

    private fun captureImage(callBack: ICaptureCallBack, savePath: String? = null) {
        getCurrentCamera()?.captureImage(callBack, savePath)
    }

     fun captureVideoStop() {
        getCurrentCamera()?.captureVideoStop()
    }
    private fun captureVideoStart(callBack: ICaptureCallBack, path: String ?= null, durationInSec: Long = 0L) {
        getCurrentCamera()?.captureVideoStart(callBack, path, durationInSec)
    }

    fun switchCamera(usbDevice: UsbDevice) {
        getCurrentCamera()?.closeCamera()
        try {
            Thread.sleep(500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        requestPermission(usbDevice)
    }

    fun openCamera(st: IAspectRatio? = null) {
        when (st) {
            is TextureView, is SurfaceView -> {
                st
            }
            else -> {
                null
            }
        }.apply {
            getCurrentCamera()?.openCamera(this, getCameraRequest())
            getCurrentCamera()?.setCameraStateCallBack(this@UVCCameraView)
        }
    }

    fun closeCamera() {
        getCurrentCamera()?.closeCamera()
    }

    private fun surfaceSizeChanged(surfaceWidth: Int, surfaceHeight: Int) {
        getCurrentCamera()?.setRenderSize(surfaceWidth, surfaceHeight)
    }

    private fun getViewLayoutParams(viewGroup: ViewGroup): ViewGroup.LayoutParams {
        return when(viewGroup) {
            is FrameLayout -> {
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    getGravity()
                )
            }
            is LinearLayout -> {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = getGravity()
                }
            }
            is RelativeLayout -> {
                RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                ).apply{
                    when(getGravity()) {
                        Gravity.TOP -> {
                            addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                        }
                        Gravity.BOTTOM -> {
                            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                        }
                        else -> {
                            addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
                            addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
                        }
                    }
                }
            }
            else -> throw IllegalArgumentException("Unsupported container view, " +
                    "you can use FrameLayout or LinearLayout or RelativeLayout")
        }
    }


    private fun getGravity() = Gravity.CENTER


    private fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(640)
            .setPreviewHeight(480)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setDefaultRotateType(RotateType.ANGLE_0)
            .setAudioSource(CameraRequest.AudioSource.SOURCE_SYS_MIC)
            .setAspectRatioShow(true)
            .setCaptureRawImage(false)
            .setRawPreviewData(false)
            .create()
    }



    private fun setButtonCallback(){
        getCurrentCamera()?.let {camera->
            if (camera !is CameraUVC) {
                return@let null
            }
            camera.setButtonCallback(IButtonCallback { button, state -> // 拍照按钮被按下
                if (button == 1 && state == 1) {
                    takePicture(
                        object : UVCStringCallback {
                            override fun onSuccess(path: String) {
                                mChannel.invokeMethod("takePictureSuccess", path)
                            }

                            override fun onError(error: String) {
                                callFlutter("拍照失败：$error","onError")
                            }
                        }
                    )
                }
                Logger.i(TAG,"点击了设备按钮：button=$button state=$state")
            }
            )
        }



    }
    /**
     * Start capture H264 & AAC only
     */
     fun captureStreamStart() {
        setEncodeDataCallBack()
        getCurrentCamera()?.captureStreamStart()
    }

     fun captureStreamStop() {
        getCurrentCamera()?.captureStreamStop()
    }

    private fun setEncodeDataCallBack() {
        getCurrentCamera()?.setEncodeDataCallBack(object : IEncodeDataCallBack {
            override fun onEncodeData(
                type: IEncodeDataCallBack.DataType,
                buffer: ByteBuffer,
                offset: Int,
                size: Int,
                timestamp: Long
            ) {
                try {
                    val data = ByteArray(size)
                    val bufferPosition = buffer.position()
                    buffer.get(data, 0, size)
                    buffer.position(bufferPosition) // 重置buffer位置，不影响原始数据
                    
                    val args = hashMapOf<String, Any>(
                        "type" to type.name,
                        "data" to data,
                        "timestamp" to timestamp
                    )
                    
                    mMainHandler.post {
                        mChannel.invokeMethod("onEncodeData", args)
                    }
                } catch (e: Exception) {
                    logError("Error processing encoded data", e)
                }
            }
        })
    }


    private fun isCameraOpened() = getCurrentCamera()?.isCameraOpened()  ?: false

    fun takePicture(callback: UVCStringCallback) {
        if (!isCameraOpened()) {
            callback.onError("Camera not open")
            return
        }
        
        try {
            captureImage(object : ICaptureCallBack {
                override fun onBegin() {
                    logInfo("Starting to capture image")
                    mMainHandler.post {
                        mChannel.invokeMethod("captureState", "started")
                    }
                }

                override fun onComplete(path: String?) {
                    if (path != null) {
                        logInfo("Image saved to: $path")
                        callback.onSuccess(path)
                        notifyMediaScanner(path)
                        mMainHandler.post {
                            mChannel.invokeMethod("captureState", "completed")
                        }
                    } else {
                        logError("Failed to save image")
                        callback.onError("Failed to save image file")
                        mMainHandler.post {
                            mChannel.invokeMethod("captureState", "error")
                        }
                    }
                }
                
                override fun onError(error: String?) {
                    logError("Error capturing image: $error")
                    callback.onError(error ?: "Unknown error in image capture")
                    mMainHandler.post {
                        mChannel.invokeMethod("captureState", "error")
                    }
                }
            })
        } catch (e: Exception) {
            logError("Failed to take picture", e)
            callback.onError("Failed to take picture: ${e.localizedMessage}")
        }
    }

    fun  captureVideo(callback: UVCStringCallback) {
        if (!isCameraOpened()) {
            callback.onError("Camera not open")
            return
        }
        
        if (isCapturingVideoOrAudio) {
            try {
                getCurrentCamera()?.captureVideoStop()
                isCapturingVideoOrAudio = false
                logInfo("Video recording stopped")
            } catch (e: Exception) {
                logError("Error stopping video capture", e)
                callback.onError("Failed to stop video: ${e.localizedMessage}")
            }
            return
        }
        
        try {
            captureVideoStart(object : ICaptureCallBack {
                override fun onBegin() {
                    isCapturingVideoOrAudio = true
                    logInfo("Video recording started")
                    mMainHandler.post {
                        mChannel.invokeMethod("videoRecordingState", "started")
                    }
                }

                override fun onError(error: String?) {
                    isCapturingVideoOrAudio = false
                    logError("Video recording error: $error")
                    callback.onError(error ?: "Unknown error in video recording")
                    mMainHandler.post {
                        mChannel.invokeMethod("videoRecordingState", "error")
                    }
                }

                override fun onComplete(path: String?) {
                    isCapturingVideoOrAudio = false
                    if (path != null) {
                        logInfo("Video saved to: $path")
                        callback.onSuccess(path)
                        notifyMediaScanner(path)
                        mMainHandler.post {
                            mChannel.invokeMethod("videoRecordingState", "completed")
                        }
                    } else {
                        logError("Failed to save video")
                        callback.onError("Failed to save video file")
                        mMainHandler.post {
                            mChannel.invokeMethod("videoRecordingState", "error")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            logError("Failed to start video recording", e)
            callback.onError("Failed to start video: ${e.localizedMessage}")
        }
    }

    // 通知媒体扫描器
    private fun notifyMediaScanner(path: String) {
        try {
            MediaScannerConnection.scanFile(
                mContext,
                arrayOf(path),
                null
            ) { _, uri ->
                logInfo("Media scanner completed: $uri")
            }
        } catch (e: Exception) {
            logError("Error notifying media scanner", e)
        }
    }

    // 新增：优化的流媒体处理方法
    fun startFrameStreaming() {
        if (mStreamingActive) return
        
        val camera = getCurrentCamera() as? CameraUVC ?: return
        
        synchronized(mStreamingLock) {
            mStreamingActive = true
            mStreamingThread = Thread {
                var consecutiveErrors = 0
                
                while (mStreamingActive) {
                    try {
                        val frame = camera.getLatestFrame()
                        if (frame != null) {
                            val previewSize = camera.getPreviewSize()
                            if (previewSize != null) {
                                val frameFormat = camera.getCurrentFrameFormat()
                                sendFrameToFlutter(frame, previewSize.width, previewSize.height, frameFormat)
                                consecutiveErrors = 0
                            }
                        }
                        Thread.sleep(STREAM_INTERVAL_MS.toLong())
                    } catch (e: Exception) {
                        if (e is InterruptedException) break
                        
                        consecutiveErrors++
                        logError("Error in frame streaming", e)
                        
                        if (consecutiveErrors >= MAX_STREAMING_RETRIES) {
                            logError("Too many streaming errors, stopping stream")
                            stopFrameStreaming()
                            break
                        }
                    }
                }
            }.apply { 
                name = "FrameStreamingThread"
                start() 
            }
        }
    }
    
    fun stopFrameStreaming() {
        synchronized(mStreamingLock) {
            mStreamingActive = false
            mStreamingThread?.interrupt()
            mStreamingThread = null
        }
    }
    
    private fun sendFrameToFlutter(frameData: ByteArray, width: Int, height: Int, format: Int) {
        val data = HashMap<String, Any>()
        data["frameData"] = frameData
        data["width"] = width
        data["height"] = height
        data["format"] = format
        
        mMainHandler.post {
            mChannel.invokeMethod("onFrameAvailable", data)
        }
    }

    // 优化视频捕获方法
    fun captureVideo(callback: UVCStringCallback) {
        if (!isCameraOpened()) {
            callback.onError("Camera not open")
            return
        }
        
        if (isCapturingVideoOrAudio) {
            try {
                getCurrentCamera()?.captureVideoStop()
                isCapturingVideoOrAudio = false
                logInfo("Video recording stopped")
            } catch (e: Exception) {
                logError("Error stopping video capture", e)
                callback.onError("Failed to stop video: ${e.localizedMessage}")
            }
            return
        }
        
        try {
            captureVideoStart(object : ICaptureCallBack {
                override fun onBegin() {
                    isCapturingVideoOrAudio = true
                    logInfo("Video recording started")
                    mMainHandler.post {
                        mChannel.invokeMethod("videoRecordingState", "started")
                    }
                }

                override fun onError(error: String?) {
                    isCapturingVideoOrAudio = false
                    logError("Video recording error: $error")
                    callback.onError(error ?: "Unknown error in video recording")
                    mMainHandler.post {
                        mChannel.invokeMethod("videoRecordingState", "error")
                    }
                }

                override fun onComplete(path: String?) {
                    isCapturingVideoOrAudio = false
                    if (path != null) {
                        logInfo("Video saved to: $path")
                        callback.onSuccess(path)
                        notifyMediaScanner(path)
                        mMainHandler.post {
                            mChannel.invokeMethod("videoRecordingState", "completed")
                        }
                    } else {
                        logError("Failed to save video")
                        callback.onError("Failed to save video file")
                        mMainHandler.post {
                            mChannel.invokeMethod("videoRecordingState", "error")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            logError("Failed to start video recording", e)
            callback.onError("Failed to start video: ${e.localizedMessage}")
        }
    }

    // 优化图片捕获方法
    fun takePicture(callback: UVCStringCallback) {
        if (!isCameraOpened()) {
            callback.onError("Camera not open")
            return
        }
        
        try {
            captureImage(object : ICaptureCallBack {
                override fun onBegin() {
                    logInfo("Starting to capture image")
                    mMainHandler.post {
                        mChannel.invokeMethod("captureState", "started")
                    }
                }

                override fun onComplete(path: String?) {
                    if (path != null) {
                        logInfo("Image saved to: $path")
                        callback.onSuccess(path)
                        notifyMediaScanner(path)
                        mMainHandler.post {
                            mChannel.invokeMethod("captureState", "completed")
                        }
                    } else {
                        logError("Failed to save image")
                        callback.onError("Failed to save image file")
                        mMainHandler.post {
                            mChannel.invokeMethod("captureState", "error")
                        }
                    }
                }
                
                override fun onError(error: String?) {
                    logError("Error capturing image: $error")
                    callback.onError(error ?: "Unknown error in image capture")
                    mMainHandler.post {
                        mChannel.invokeMethod("captureState", "error")
                    }
                }
            })
        } catch (e: Exception) {
            logError("Failed to take picture", e)
            callback.onError("Failed to take picture: ${e.localizedMessage}")
        }
    }
    
    // 通知媒体扫描器
    private fun notifyMediaScanner(path: String) {
        try {
            MediaScannerConnection.scanFile(
                mContext,
                arrayOf(path),
                null
            ) { _, uri ->
                logInfo("Media scanner completed: $uri")
            }
        } catch (e: Exception) {
            logError("Error notifying media scanner", e)
        }
    }

    // 改进流捕获方法
    fun captureStreamStart() {
        if (!isCameraOpened()) {
            setCameraERRORState("Cannot start streaming: Camera not open")
            return
        }
        
        try {
            setEncodeDataCallBack()
            getCurrentCamera()?.captureStreamStart()
            
            logInfo("Stream capture started")
            mMainHandler.post {
                mChannel.invokeMethod("streamState", "started")
            }
        } catch (e: Exception) {
            logError("Failed to start stream capture", e)
            mMainHandler.post {
                mChannel.invokeMethod("streamState", "error")
            }
        }
    }

    fun captureStreamStop() {
        try {
            getCurrentCamera()?.captureStreamStop()
            logInfo("Stream capture stopped")
            mMainHandler.post {
                mChannel.invokeMethod("streamState", "stopped")
            }
        } catch (e: Exception) {
            logError("Failed to stop stream capture", e)
        }
    }

    // 获取相机信息的优化方法
    fun getCameraInfo(): Map<String, Any> {
        val camera = getCurrentCamera() as? CameraUVC
        val infoMap = mutableMapOf<String, Any>()
        
        if (camera != null) {
            infoMap["isOpened"] = camera.isCameraOpened
            
            camera.getPreviewSize()?.let {
                infoMap["previewWidth"] = it.width
                infoMap["previewHeight"] = it.height
            }
            
            val fpsRange = camera.getFpsRange()
            infoMap["minFps"] = fpsRange.first
            infoMap["maxFps"] = fpsRange.second
            infoMap["frameFormat"] = camera.getCurrentFrameFormat()
            infoMap["deviceName"] = camera.device.deviceName ?: "Unknown"
            infoMap["usbInfo"] = "${camera.device.vendorId}:${camera.device.productId}"
        }
        
        return infoMap
    }

    // 优化的错误处理和日志方法
    private fun logError(message: String, exception: Exception? = null) {
        Logger.e(TAG, message, exception)
        mMainHandler.post {
            mChannel.invokeMethod("logMessage", mapOf(
                "level" to "error",
                "message" to message,
                "details" to (exception?.localizedMessage ?: "")
            ))
        }
    }

    private fun logInfo(message: String) {
        Logger.i(TAG, message)
        if (Utils.debugCamera) {
            mMainHandler.post {
                mChannel.invokeMethod("logMessage", mapOf(
                    "level" to "info",
                    "message" to message
                ))
            }
        }
    }
}