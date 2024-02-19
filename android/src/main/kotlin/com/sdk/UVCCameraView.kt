package com.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.media.MediaScannerConnection
import android.os.Environment
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
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.SettableFuture
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import com.jiangdg.usb.USBMonitor
import com.jiangdg.uvc.IButtonCallback
import com.sdk.databinding.ActivityMainBinding
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.text.SimpleDateFormat
import java.util.Locale
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
    private var mAspectRatio: Double? = null
    private val mRequestPermission: AtomicBoolean by lazy {
        AtomicBoolean(false)
    }
    private var validProductIds = intArrayOf(52225)
    private var validVendorIds = intArrayOf(52281)

    companion object {
        private const val TAG = "CameraView"
    }

    private val mCameraDir by lazy {
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}"
    }
    private val mDateFormat by lazy {
        SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault())
    }
    init{
        processingParams()
    }

    private fun processingParams() {
        if (params is Map<*, *>) {
            mAspectRatio = (params["aspectRatio"] as? Number)?.toDouble()
            val productIds = params["productIds"] as? List<*>
            val vendorIds = params["vendorIds"] as? List<*>
            productIds?.forEach { id ->
                if(id is Int){
                    validProductIds = validProductIds.plus(id)
                }
            }
            vendorIds?.forEach { id ->
                if(id is Int){
                    validVendorIds = validVendorIds.plus(id)
                }
            }
        }}

    override fun getView(): View {
        return mViewBinding.root
    }


    private fun setCameraERRORState(msg:String?=null){
       mChannel.invokeMethod("CameraState","ERROR:$msg")
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
        checkCameraPermission()
         if(!isCameraOpened()){
             setCameraERRORState(msg = "未检测到设备")
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
        Logger.i(TAG, "------>CameraState: $code") ;
    }

    private fun handleCameraError(msg: String?) {
        mChannel.invokeMethod("CameraState", "ERROR:$msg")
    }

    private fun handleCameraClosed() {
        mChannel.invokeMethod("CameraState", "CLOSED")
    }

    private fun handleCameraOpened() {
        mChannel.invokeMethod("CameraState", "OPENED")
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
        if(mCameraClient?.getDeviceList()?.isEmpty() == true)
        {
            setCameraERRORState("未检测到设备")
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
    private fun checkCameraPermission() : Boolean {
        if (mActivity == null) {
            return false
        }
        val hasCameraPermission = PermissionChecker.checkSelfPermission(
            mActivity!!,
            Manifest.permission.CAMERA
        )
        val hasStoragePermission = PermissionChecker.checkSelfPermission(
            mActivity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )


        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED
            || hasStoragePermission != PermissionChecker.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                mActivity!!,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
                1230
            )
            return false
        }
        return true
    }

    private fun callFlutter(msg: String, type: String? = null) {
        val data = HashMap<String, String>()
        data["type"] = type ?: "msg"
        data["msg"] = msg
        mChannel.invokeMethod("callFlutter", data)
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
        return CameraUVC(ctx, device)
    }

    fun getDefaultCamera(): UsbDevice? = null
    fun getDefaultEffect() = getCurrentCamera()?.getDefaultEffect()

    private fun captureImage(callBack: ICaptureCallBack, savePath: String? = null) {
        getCurrentCamera()?.captureImage(callBack, savePath)
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
        val height  = 640.0
        var width = 480.0
        if (mAspectRatio != null) { width = height * mAspectRatio!! }
        return CameraRequest.Builder()
            .setPreviewWidth(height.toInt())
            .setPreviewHeight(width.toInt())
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
                        object : UVCPictureCallback {
                            override fun onPictureTaken(path: String) {
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

    private fun isCameraOpened() = getCurrentCamera()?.isCameraOpened()  ?: false

    fun takePicture(callback: UVCPictureCallback) {
        val date = mDateFormat.format(System.currentTimeMillis())
        val title = "IMG_AUSBC_$date"
        val displayName =  "$title.jpg"
        if (!isCameraOpened()) {
            callFlutter("摄像头未打开")
            setCameraERRORState("设备未打开")
            return
        }
        captureImage( object : ICaptureCallBack {
            override fun onBegin() {
                callFlutter("开始拍照")
            }

            override fun onComplete(path: String?) {
                if (path != null) {
                    callback.onPictureTaken(path)
                    MediaScannerConnection.scanFile(view.context, arrayOf(path), null) {
                            mPath, uri ->
                        // 文件已经被扫描到媒体数据库
                        println("Media scan completed for file: $mPath with uri: $uri")
                    }
                } else {
                    callback.onError("拍照失败，未能保存图片")
                }
            }
            override fun onError(error: String?) {
                callback.onError(error ?: "未知错误")
            }

        }, savePath = "$mCameraDir/$displayName")
    }

}