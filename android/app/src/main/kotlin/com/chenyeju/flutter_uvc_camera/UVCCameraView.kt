import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.chenyeju.flutter_uvc_camera.UVCPictureCallback
import com.chenyeju.flutter_uvc_camera.databinding.ActivityMainBinding
import com.chenyeju.flutter_uvc_camera.usbvideo.IButtonCallback
import com.chenyeju.flutter_uvc_camera.usbvideo.USBCameraSDK
import com.chenyeju.flutter_uvc_camera.uvc_camera.CustomTextureView
import com.chenyeju.flutter_uvc_camera.uvc_camera.PictureSaver
import com.chenyeju.flutter_uvc_camera.uvc_camera.PictureSaverCallback
import com.chenyeju.flutter_uvc_camera.uvc_camera.PictureSaverInfo
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView


internal class UVCCameraView(
    private val mContext: Context,
    private val mChannel: MethodChannel, params: Any?) : PlatformView , ActivityAware {
    private var mViewBinding = ActivityMainBinding.inflate(LayoutInflater.from(mContext))
    private var mActivity: Activity? = null
    private var  previewW = 640
    private var   previewH = 480
    var   m_dsp_adr = 0xd55b
    var   m_min_val = 0x10
    var   m_max_val = 0x13
    private var mUsbManager: UsbManager? = null
    private var mUsbDevice: UsbDevice? = null
    private var mUsbConnection: UsbDeviceConnection? = null
    private var mAspectRatio: Double? = null

    private val ACTION_USB_PERMISSION = "REQUEST_USB_PERMISSION"
    private var mTextureView: CustomTextureView? = null

    private var mContentResolver: ContentResolver? = null
    private var mPictureSaver: PictureSaver? = null

    override fun onFlutterViewDetached() {
        super.onFlutterViewDetached()
        mActivity = null
    }

    override fun getView(): View {
        return mViewBinding.root
    }
    override fun dispose() {

    }



    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun initCamera(arguments: Any?) {
        if (arguments is Map<*, *>) {
            mAspectRatio = (arguments["aspectRatio"] as? Number)?.toDouble()
        }
        if (mAspectRatio != null) { previewW= (previewH * mAspectRatio!!).toInt() }
        mTextureView = mViewBinding.textureView
        mTextureView?.surfaceTextureListener = mSurfaceTextureListener
        mTextureView?.setAspectRatio(previewW, previewH)
//        checkCameraPermission()
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        mContext.registerReceiver(mUsbReceiver, filter)
        mPictureSaver = PictureSaver(mPictureSaverCallback)
        mContentResolver = getContentResolver()

        openUVCCamera()

    }

    private fun openUVCCamera() {

        mUsbManager = mContext.getSystemService(Context.USB_SERVICE ) as UsbManager
        val devMap: HashMap<String, UsbDevice>? = mUsbManager?.deviceList
        val devList: MutableList<UsbDevice> = java.util.ArrayList()
        devMap?.values?.let { devList.addAll(it) }

        mUsbDevice = null
        for (i in devList.indices) {
            if (devList[i].deviceClass == 239) {   // 239表示图像设备，这里获取第一个图像设备，也可以通过vendorId之类获取指定设备
                mUsbDevice = devList[i]
                break
            }
        }

        if (mUsbDevice == null) {
            Toast.makeText(mContext, "没有摄像头设备", Toast.LENGTH_SHORT).show()
            return
        }


        // 请求权限
        if (mUsbManager?.hasPermission(mUsbDevice)==true) {
            openCameraContinue()

        } else {
            val permissionIntent =
                PendingIntent.getBroadcast(mContext, 0, Intent(ACTION_USB_PERMISSION), 0)
            mUsbManager?.requestPermission(mUsbDevice, permissionIntent)
        }
    }
    private fun openCameraContinue() {
        mUsbConnection = mUsbManager!!.openDevice(mUsbDevice)
        if (mUsbConnection == null) {
            Toast.makeText(mContext, "打开USB设备错误", Toast.LENGTH_SHORT).show()
            return
        }
        val fd = mUsbConnection?.fileDescriptor
        USBCameraSDK.setPreviewSurface(null, previewW, previewH, 0)
        if (
            USBCameraSDK.openCamera(
                fd!!,
                0,
                previewW,
                previewH,
                0,
                0
            )
            <0
        ) {
            mUsbConnection?.close()
            mUsbConnection = null
            Toast.makeText(mContext, "打开摄像头错误", Toast.LENGTH_SHORT).show()
            return
        }

        // (方式一，通过UVC返回的button数据方式)
        USBCameraSDK.setButtonCallback { button, state ->
            if (button == 1 && state == 1) {
                takePicture(
                    object : UVCPictureCallback {
                        override fun onPictureTaken(path: String) {
                            mChannel.invokeMethod("takePictureSuccess", path)
                        }

                        override fun onError(error: String) {
                            mChannel.invokeMethod("callFlutter", error)
                        }
                    }
                )
            }
        }
    }


    private fun getContentResolver(): ContentResolver? {
        if (mActivity != null) {
            return mActivity!!.contentResolver
        }
        return null
    }

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                Toast.makeText(mContext, "Usb device attached", Toast.LENGTH_SHORT).show()
                //mHandler.sendEmptyMessage(MSG_OPENDEVICE);
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                Toast.makeText(mContext, "Usb device detached", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ///
    /// 拍照
    ///
    private var mPictureSaverCallback: PictureSaverCallback = object : PictureSaverCallback {
        override fun onMessage(msg: Int, info: PictureSaverInfo?) {
            when (msg) {
                PictureSaverCallback.MESSAGE_STOREIMAGE_ERR -> {}

                PictureSaverCallback.MESSAGE_STOREIMAGE_SUCCESS -> {

                }

                else -> {}
            }
        }
    }


    private fun checkCameraPermission() : Boolean {
        val hasCameraPermission = PermissionChecker.checkSelfPermission(mContext,
            Manifest.permission.CAMERA
        )
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED|| hasStoragePermission != PermissionChecker.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity!!,
                    Manifest.permission.CAMERA
                )
            ) {
                mChannel.invokeMethod(
                    "callFlutter",
                    "You have already denied permission access. Go to the Settings page to turn on permissions\n"
                )
            }
            ActivityCompat.requestPermissions(
                mActivity!!,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
                0
            )
            return false
        }
        return true
    }

    fun takePicture(callback: UVCPictureCallback) {

    }

    fun getAllPreviewSize() {
    }

    fun getDevicesList() {
    }

    fun writeToDevice(i: Int) {

    }

    fun closeCamera() {
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        mActivity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        mActivity = binding.activity
    }

    override fun onDetachedFromActivity() {
        mActivity = null
    }

    var mSurface: Surface? = null
    private var mSurfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            mSurface = Surface(surface)
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            mSurface = null
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }
}