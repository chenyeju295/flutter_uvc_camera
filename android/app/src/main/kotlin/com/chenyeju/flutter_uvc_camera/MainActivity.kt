package com.chenyeju.flutter_uvc_camera

import android.annotation.SuppressLint
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
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel


class MainActivity : FlutterActivity() {
    private val channelName = "com.chenyeju.flutter_uvc_camera/usb"
    private val vendorId = 52281
    private val productId =  52225
    private val actionUsbPermission = "com.chenyeju.flutter_uvc_camera.USB_PERMISSION"
    private lateinit var usbManager: UsbManager
    private lateinit var permissionIntent: PendingIntent
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
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(actionUsbPermission),
            PendingIntent.FLAG_IMMUTABLE)
        val filter = IntentFilter(actionUsbPermission)
        registerReceiver(usbPermissionReceiver, filter)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName).setMethodCallHandler { call, result ->
            when (call.method) {
                "getUsbDevicesList" -> {
                    val deviceList = usbManager.deviceList
                    result.success(deviceList.toString())
                }

                "connectToUsbDevice" -> {
                    connectToUsbDevice()
                    result.success("Connection attempt started")
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
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(usbPermissionReceiver)
        super.onDestroy()
    }

    private fun connectToUsbDevice() {
        val deviceList = usbManager.deviceList
        val device = deviceList.values.firstOrNull { it.vendorId == vendorId && it.productId == productId }
        device?.let {
            if (usbManager.hasPermission(it)) {
                setUpDeviceCommunication(it)
            } else {
                usbManager.requestPermission(it, permissionIntent)
                Log.d("USB", "Requesting permission for device ${it.deviceName}")
            }
        } ?: Log.d("USB", "Device not found or already connected.")
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

        // 寻找正确的端点
        for (i in 0 until usbInterface!!.endpointCount) {
            val endpoint = usbInterface!!.getEndpoint(i)
            if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (endpoint.direction == UsbConstants.USB_DIR_IN) {
                    inEndpoint = endpoint
                } else if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
                    outEndpoint = endpoint
                }
            }
        }

        if (inEndpoint == null || outEndpoint == null) {
            Log.d("USB", "Could not find necessary endpoints.")
            return
        }

        // 此时，设备应该已经设置好并准备通信
        Log.d("USB", "Device communication setup complete.")
    }
//    private fun setUpDeviceCommunication(device: UsbDevice) {
//        usbInterface = device.getInterface(0)
//        connection = usbManager.openDevice(device)
//
//        for (i in 0 until usbInterface!!.endpointCount) {
//            val endpoint = usbInterface!!.getEndpoint(i)
//            if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
//                if (endpoint.direction == UsbConstants.USB_DIR_IN) {
//                    inEndpoint = endpoint
//                } else {
//                    outEndpoint = endpoint
//                }
//            }
//        }
//
//        connection?.claimInterface(usbInterface, true)
//    }

    private fun writeToDevice(data: ByteArray) {
        outEndpoint?.let { endpoint ->
            connection?.bulkTransfer(endpoint, data, data.size, 0)
        }
    }

}
