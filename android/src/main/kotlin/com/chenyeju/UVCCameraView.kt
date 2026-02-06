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
    private val params: Any?,
    private val videoStreamHandler: VideoStreamHandler,
    private val recordingTimerManager: RecordingTimerManager
) : PlatformView, PermissionResultListener, ICameraStateCallBack {
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
    
    // New managers for better organization
    private val stateManager = CameraStateManager(mChannel)
    private val configManager = CameraConfigManager()
    private val featuresManager = CameraFeaturesManager(mChannel)
    
    // Track streaming state
    private var isStreaming = false
    // Handler for periodic FPS updates
    private val fpsReportHandler = Handler(Looper.getMainLooper())
    private val fpsReportRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isStreaming) {
                // Get FPS from camera if available
                val camera = getCurrentCamera()
                if (camera is CameraUVC) {
                    try {
                        // Get render FPS from camera implementation
                        val renderFps = camera.getRenderFps()
                        if (renderFps > 0) {
                            Logger.i(TAG, "camera render frame rate is $renderFps fps-->gl_render")
                            // Send as state update
                            val data = HashMap<String, Any>()
                            data["renderFps"] = renderFps
                            videoStreamHandler.sendState("RENDER_FPS", data)
                        }
                    } catch (e: Exception) {
                        Logger.e(TAG, "Error getting render FPS", e)
                    }
                }
                
                // Schedule next update
                fpsReportHandler.postDelayed(this, 1000)
            }
        }
    }
    
    init {
        configManager.updateFromParams(params)
    }

    companion object {
        private const val TAG = "CameraView"
    }

    override fun getView(): View {
        return mViewBinding.root
    }

    private fun setCameraERRORState(msg: String? = null) {
        mChannel.invokeMethod("CameraState", "ERROR:$msg")
    }

    fun initCamera() {
        stateManager.updateState(CameraStateManager.CameraState.CLOSED)
        val cameraView = AspectRatioTextureView(mContext)
        handleTextureView(cameraView)
        mCameraView = cameraView
        applyDisplayAspectRatio(cameraView)
        cameraView.also { view ->
            mViewBinding.fragmentContainer
                .apply {
                    removeAllViews()
                    addView(view, getViewLayoutParams(this))
                }
        }
    }

    /**
     * Set aspect ratio on the view when aspectRatioShow is true to avoid distortion when rotated.
     */
    private fun applyDisplayAspectRatio(textureView: TextureView) {
        if (!configManager.getAspectRatioShow()) return
        val (w, h) = configManager.getDisplayAspectSize()
        if (w <= 0 || h <= 0) return
        try {
            val method = textureView.javaClass.getMethod("setAspectRatio", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            method.invoke(textureView, w, h)
        } catch (e: Exception) {
            try {
                val ratio = w.toDouble() / h
                val method = textureView.javaClass.getMethod("setAspectRatio", Double::class.javaPrimitiveType)
                method.invoke(textureView, ratio)
            } catch (e2: Exception) {
                // Library may not expose setAspectRatio; render uses setAspectRatioShow from request
            }
        }
    }

    fun openUVCCamera() {
        if (!PermissionManager.hasRequiredPermissions(mContext)) {
            PermissionManager.requestPermissionsIfNeeded(mActivity)
            return
        }
        
        stateManager.updateState(CameraStateManager.CameraState.OPENING)
        openCamera()
    }

    override fun dispose() {
        unRegisterMultiCamera()
        mViewBinding.fragmentContainer.removeAllViews()
        stateManager.updateState(CameraStateManager.CameraState.CLOSED)
        recordingTimerManager.release()
        
        // Clean up FPS monitoring
        isStreaming = false
        fpsReportHandler.removeCallbacks(fpsReportRunnable)
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
                stateManager.updateState(CameraStateManager.CameraState.OPENED)
                setButtonCallback()
            }
            ICameraStateCallBack.State.CLOSED -> {
                stateManager.updateState(CameraStateManager.CameraState.CLOSED)
            }
            ICameraStateCallBack.State.ERROR -> {
                stateManager.updateState(CameraStateManager.CameraState.ERROR, msg)
            }
        }
        Logger.i(TAG, "------>CameraState: $code")
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
                if (!PermissionManager.hasRequiredPermissions(mContext)) {
                    PermissionManager.requestPermissionsIfNeeded(mActivity)
                    return
                }
                registerMultiCamera()
                checkCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                surfaceSizeChanged(width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                unRegisterMultiCamera()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }
        }
    }

    private fun checkCamera() {
        if (mCameraClient?.getDeviceList()?.isEmpty() == true) {
            stateManager.updateState(CameraStateManager.CameraState.ERROR, "No device detected")
        }
    }

    override fun onPermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (PermissionManager.isPermissionGranted(requestCode, permissions, grantResults)) {
            registerMultiCamera()
            checkCamera()
        } else {
            callFlutter("Permission denied")
            stateManager.updateState(CameraStateManager.CameraState.ERROR, "Permission denied")
        }
    }

    private fun callFlutter(msg: String, type: String? = null) {
        val data = HashMap<String, String>()
        data["type"] = type ?: "msg"
        data["msg"] = msg
        mChannel.invokeMethod("callFlutter", data)
    }

    fun getAllPreviewSizes(): String? {
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
        configManager.updateResolution(width, height)
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
    
    // New methods for camera features
    fun setCameraFeature(feature: String, value: Int): Boolean {
        val camera = getCurrentCamera()
        if (camera !is CameraUVC) {
            callFlutter("Camera not available or not UVC type")
            return false
        }
        
        return featuresManager.applyCameraFeature(camera, feature, value)
    }
    
    fun resetCameraFeature(feature: String): Boolean {
        val camera = getCurrentCamera()
        if (camera !is CameraUVC) {
            callFlutter("Camera not available or not UVC type")
            return false
        }
        
        return featuresManager.resetCameraFeature(camera, feature)
    }
    
    fun getCameraFeature(feature: String): Int? {
        val camera = getCurrentCamera()
        if (camera !is CameraUVC) {
            callFlutter("Camera not available or not UVC type")
            return null
        }
        
        return featuresManager.getCameraFeature(camera, feature)
    }
    
    fun getAllCameraFeatures(): String? {
        val camera = getCurrentCamera()
        if (camera !is CameraUVC) {
            callFlutter("Camera not available or not UVC type")
            return null
        }
        
        return featuresManager.getAllCameraFeatures(camera)
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
        return CameraUVC(ctx, device, configManager.getCameraParams())
    }

    fun getDefaultCamera(): UsbDevice? = null
    
    fun getDefaultEffect() = getCurrentCamera()?.getDefaultEffect()

    private fun captureImage(callBack: ICaptureCallBack, savePath: String? = null) {
        getCurrentCamera()?.captureImage(callBack, savePath)
    }

    fun captureVideoStop() {
        getCurrentCamera()?.captureVideoStop()
    }
    
    private fun captureVideoStart(callBack: ICaptureCallBack, path: String? = null, durationInSec: Long = 0L) {
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
            getCurrentCamera()?.openCamera(this, configManager.buildCameraRequest())
            getCurrentCamera()?.setCameraStateCallBack(this@UVCCameraView)
        }
    }

    fun closeCamera() {
        stateManager.updateState(CameraStateManager.CameraState.CLOSING)
        getCurrentCamera()?.closeCamera()
    }

    private fun surfaceSizeChanged(surfaceWidth: Int, surfaceHeight: Int) {
        getCurrentCamera()?.setRenderSize(surfaceWidth, surfaceHeight)
    }

    private fun getViewLayoutParams(viewGroup: ViewGroup): ViewGroup.LayoutParams {
        return when (viewGroup) {
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
                ).apply {
                    when (getGravity()) {
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
            else -> throw IllegalArgumentException(
                "Unsupported container view, " +
                        "you can use FrameLayout or LinearLayout or RelativeLayout"
            )
        }
    }

    private fun getGravity() = Gravity.CENTER

    private fun setButtonCallback() {
        getCurrentCamera()?.let { camera ->
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
                                callFlutter("Failed to take picture: $error", "onError")
                            }
                        }
                    )
                }
                Logger.i(TAG, "Device button clicked: button=$button state=$state")
            }
            )
        }
    }
    
    /**
     * Start capture H264 & AAC only
     */
    fun captureStreamStart() {
        if (!stateManager.checkStateForOperation("stream")) {
            Logger.e(TAG, "Camera not ready for streaming. Current state: ${stateManager.getCurrentState()}")
            callFlutter("Camera not ready for streaming")
            return
        }
        
        Logger.i(TAG, "Starting camera stream")
        setEncodeDataCallBack()
        val camera = getCurrentCamera()
        if (camera == null) {
            Logger.e(TAG, "Cannot start stream - camera is null")
            callFlutter("Camera not available")
            return
        }
        camera.captureStreamStart()
        Logger.i(TAG, "Camera stream started")
        isStreaming = true
        videoStreamHandler.sendState("STREAM_STARTED")
        
        // Start FPS monitoring
        fpsReportHandler.post(fpsReportRunnable)
    }

    fun captureStreamStop() {
        Logger.i(TAG, "Stopping camera stream")
        getCurrentCamera()?.captureStreamStop()
        Logger.i(TAG, "Camera stream stopped")
        isStreaming = false
        videoStreamHandler.sendState("STREAM_STOPPED")
        
        // Stop FPS monitoring
        fpsReportHandler.removeCallbacks(fpsReportRunnable)
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
                videoStreamHandler.onVideoFrame(
                    type.name, 
                    buffer,
                    offset,
                    size,
                    timestamp
                )
            }
        })
    }

    private fun isCameraOpened() = stateManager.isOpened()

    fun takePicture(callback: UVCStringCallback) {
        if (!isCameraOpened()) {
            callback.onError("Camera not open")
            return
        }
        
        captureImage(object : ICaptureCallBack {
            override fun onBegin() {
                callFlutter("Starting capture")
            }

            override fun onComplete(path: String?) {
                if (path != null) {
                    callback.onSuccess(path)
                    MediaScannerConnection.scanFile(view.context, arrayOf(path), null) { mPath, uri ->
                        // File scanned into media database
                        println("Media scan completed for file: $mPath with uri: $uri")
                    }
                } else {
                    callback.onError("Failed to save image")
                }
            }
            
            override fun onError(error: String?) {
                callback.onError(error ?: "Unknown error")
            }
        })
    }

    fun captureVideo(callback: UVCStringCallback) {
        if (isCapturingVideoOrAudio) {
            recordingTimerManager.stopRecording()
            captureVideoStop()
            return
        }
        
        if (!isCameraOpened()) {
            callback.onError("Camera not open")
            return
        }

        captureVideoStart(object : ICaptureCallBack {
            override fun onBegin() {
                isCapturingVideoOrAudio = true
                callFlutter("Started video recording")
                recordingTimerManager.startRecording()
            }

            override fun onError(error: String?) {
                isCapturingVideoOrAudio = false
                recordingTimerManager.stopRecording()
                callback.onError(error ?: "Video capture error")
            }

            override fun onComplete(path: String?) {
                if (path != null) {
                    callback.onSuccess(path)
                    MediaScannerConnection.scanFile(view.context, arrayOf(path), null) { mPath, uri ->
                        println("Media scan completed for file: $mPath with uri: $uri")
                    }
                    isCapturingVideoOrAudio = false
                    recordingTimerManager.stopRecording()
                } else {
                    isCapturingVideoOrAudio = false
                    recordingTimerManager.stopRecording()
                    callback.onError("Failed to save video")
                }
            }
        })
    }

    fun startPlayMic(): Boolean {
        if (!isCameraOpened()) {
            callFlutter("Camera not open")
            return false
        }
        val camera = getCurrentCamera() ?: run {
            callFlutter("Camera not available")
            return false
        }
        return invokeCameraMethod(camera, "startPlayMic")
    }

    fun stopPlayMic(): Boolean {
        val camera = getCurrentCamera() ?: run {
            callFlutter("Camera not available")
            return false
        }
        return invokeCameraMethod(camera, "stopPlayMic")
    }

    private fun invokeCameraMethod(camera: Any, methodName: String): Boolean {
        return try {
            val method = camera.javaClass.methods.firstOrNull { it.name == methodName }
            if (method == null) {
                callFlutter("Method $methodName not supported by current camera implementation")
                return false
            }
            val result = method.invoke(camera)
            when (result) {
                is Boolean -> result
                else -> true
            }
        } catch (e: Exception) {
            callFlutter("Failed to call $methodName: ${e.message}")
            false
        }
    }
}
