package com.chenyeju.flutter_uvc_camera

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.utils.ToastUtils
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel


class MainActivity : FlutterFragmentActivity() {
    private val channelName = "com.chenyeju.flutter_uvc_camera/usb"
    private val vendorId = 52281
    private val productId =  52225
    private val actionUsbPermission = "com.chenyeju.flutter_uvc_camera.USB_PERMISSION"
    private lateinit var usbManager: UsbManager
    private lateinit var cameraViewFactory: UVCCameraViewFactory
    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null



    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (actionUsbPermission == intent?.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { setUpDeviceCommunication(it) }
                    } else {
                        // Permission denied for device
                        Log.d("USB", "Permission denied for device ${device?.deviceName}")
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent?.action) {
            // 有新的设备插入了，在这里一般会判断这个设备是不是我们想要的，是的话就去请求权限
                Log.d("USB", "New device attached")
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent?.action) {
                // 有设备拔出了
                connection?.close()
                connection = null
            }
        }
    }
    private fun replaceDemoFragment(fragment: Fragment) {
        val hasCameraPermission = PermissionChecker.checkSelfPermission(this,
            Manifest.permission.CAMERA
        )
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED || hasStoragePermission != PermissionChecker.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA
                )) {
                ToastUtils.show("You have already denied permission access. Go to the Settings page to turn on permissions\n")
            }
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                ),
                0
            )
            return
        }
        try {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            transaction.commitAllowingStateLoss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val filter = IntentFilter(actionUsbPermission)
        registerReceiver(usbPermissionReceiver, filter)

        flutterEngine.plugins.add(UVCCameraPlugin())

//        flutterEngine
//            .platformViewsController
//            .registry
//            .registerViewFactory("uvc_camera_view", cameraViewFactory)
        val channel =  MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "getUsbDevicesList" -> {
                    val deviceList = usbManager.deviceList
                    result.success(deviceList.toString())
                }
                "startCamera" -> {
                    replaceDemoFragment(UVCCameraFragment(channel))
                }
                "connectToUsbDevice" -> {
                    connectToUsbDevice()
                }
                "listenToDevice" -> {
                    // Implement listening logic here
                    result.success("Listening attempt started")
                }
                "writeToDevice" -> {
                    val data = call.arguments as ByteArray
                    writeToDevice(data)
                    result.success("Write attempt started")
                }
                "readFromDevice" -> {
                    // Implement reading logic here
                    result.success("Read attempt started")
                }
                "closeConnection" -> {
                    unregisterReceiver(usbPermissionReceiver)
                }
                "initialize" -> {
                    // 初始化相机
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(usbPermissionReceiver)

        } catch (_: Exception) {
            Log.d("USB", "unregisterReceiver error")
        }
        super.onDestroy()
    }

    private fun connectToUsbDevice() {
        val deviceList = usbManager.deviceList
        val device = deviceList.values.firstOrNull { it.vendorId == vendorId && it.productId == productId }
        device?.let {
            if (usbManager.hasPermission(it)) {
                setUpDeviceCommunication(it)
            } else {
                val permissionIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(actionUsbPermission).apply {
                        putExtra(UsbManager.EXTRA_DEVICE, it) // 附加 USB 设备到 Intent
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

                )
                usbManager.requestPermission(it, permissionIntent)
                Log.d("USB", "Requesting permission for device ${it.deviceName}")
            }
        } ?: Log.d("USB", "Device not found ")
    }

    override fun onPause() {
        super.onPause()
        try{
        unregisterReceiver(usbPermissionReceiver)}catch (_:Exception){
            Log.d("USB","unregisterReceiver error")
        }
    }

    private fun setUpDeviceCommunication(device: UsbDevice) {
        usbInterface = device.getInterface(0) // 确保这是正确的接口

        connection = usbManager.openDevice(device)
        if (connection == null) {
            Log.d("USB", "Could not open connection to device.")
            return
        }

        val claimResult = connection?.claimInterface(usbInterface, true)
        if (claimResult == false) {
            Log.d("USB", "Could not claim interface.")
            return
        }

        if(usbInterface == null){
            Log.d("USB", "Could not find interface.")
        }
        // 寻找正确的端点
        for (i in 0 until usbInterface!!.endpointCount) {
            val endpoint = usbInterface!!.getEndpoint(i)
            if (endpoint.direction == UsbConstants.USB_DIR_IN) {
                inEndpoint = endpoint
            } else if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
                outEndpoint = endpoint
            }
        }

        if (inEndpoint == null && outEndpoint == null) {
            Log.d("USB", "Could not find necessary endpoints.")
            return
        }

        // 此时，设备应该已经设置好并准备通信
        Log.d("USB", "Device communication setup complete.")
    }

    private fun writeToDevice(data: ByteArray) {
        try {
            val requestType = UsbConstants.USB_TYPE_VENDOR or UsbConstants.USB_DIR_OUT

            inEndpoint?.let { endpoint ->
                val bytesSent =  connection?.controlTransfer(
                    requestType,
                    1,
                    41,
                    1,
                    byteArrayOf(0, 120, 18, -1), 4, 10)
                Log.d(  "USB", "Wrote data to device.$bytesSent ")
            }

           inEndpoint?.let { endpoint ->
               val bytesSent =   connection?.bulkTransfer(endpoint, byteArrayOf(0, 120, 18, -1), 4, 10)
                Log.d(  "USB", "Wrote data to device.$bytesSent")
               ToastUtils.show("Wrote data to device.$bytesSent")

            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}

