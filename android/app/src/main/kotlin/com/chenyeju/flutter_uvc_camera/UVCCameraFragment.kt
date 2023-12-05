package com.chenyeju.flutter_uvc_camera

import android.R.attr
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chenyeju.flutter_uvc_camera.databinding.ActivityMainBinding
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import io.flutter.plugin.common.MethodChannel


class UVCCameraFragment(channel: MethodChannel) : CameraFragment() {
    private lateinit var viewBinding: ActivityMainBinding
    private val _channel = channel
    private var connection: UsbDeviceConnection? = null

    fun takePicture() {
        if (! isCameraOpened()) {
            callFlutter( "摄像头未打开")
            return
        }
        captureImage()
    }

 fun callFlutter(error: String?) {
        _channel.invokeMethod("callFlutter", error)
    }

    fun checkCamera(): Boolean {
        try {
            getCurrentCamera()?.let { strategy ->
                if (strategy is CameraUVC) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun captureImage() {
        captureImage(
            object : ICaptureCallBack {
                override fun onBegin() {
                    callFlutter("开始拍照")
                }

                override fun onComplete(path: String?) {
                    _channel.invokeMethod("takePictureSuccess", path)
                }

                override fun onError(error: String?) {
                    callFlutter(error)
                }

            }
        )
    }

    fun writeToDevice(indexData: Int) {
        var array = listOf("11", "12", "13")
        try {
            var usbDevice: UsbDevice?  = null
            val mUsbManager = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager

            getCurrentCamera()?.let { strategy ->
                if (strategy is CameraUVC) {
                    usbDevice = strategy.getUsbDevice()
                    connection = mUsbManager.openDevice(usbDevice)
                }

            }

            if(usbDevice != null) {
                    val intf = usbDevice!!.getInterface(0)
                    connection?.claimInterface(intf,true)
                    if (intf.endpointCount > 0) {
                            val y0 = listOf("11", "12", "13")
                            val i =  indexData % 3
                            val b1: Byte = Integer.parseInt(y0[i], 16).toByte()
                            val b2: Byte = Integer.parseInt("ff", 16).toByte()

                            val data = byteArrayOf(0, 120,b1,b2)

                            val timeout = 1000 // 超时时间（毫秒）
                            val size = data.size

                            // 发送控制传输
                            val requestType = UsbConstants.USB_TYPE_VENDOR // 控制传输的请求类型

                            val request = 1 // 控制传输的请求码

                            val value = 2560 // 控制传输的值

                            val index = 0 // 控制传输的索引


                            // 第一个控制传输

                            // 第一个控制传输
                            var result: Int? = connection?.controlTransfer(
                                requestType,
                                request,
                                value,
                                index,
                             data,
                               size,
                                timeout
                            )
                            if (result != null) {
                                if (result < 0) {
                                    // 控制传输失败
                                    Log.d("USB", "Wrote data to device.  $result ")

                                    return
                                }
                            }
                            Log.d("USB", "Wrote data to device------------. $result")
//                        result = connection?.controlTransfer(requestType, request, value, index, data, size, timeout);
//                        if (result != null) {
//                            if (result < 0) {
//                                // 控制传输失败
//                                return;
//                            }
//                        }

//                             bytesSent = connection?.bulkTransfer(
//                                endpoint,
//                                data,
//                                size,
//                                timeout
//                            )

//                            callFlutter(    bytesSent.toString())
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(  "USB", "$e")

        }

    }
    private fun showUsbDevicesDialog(usbDeviceList: MutableList<UsbDevice>?, curDevice: UsbDevice?) {
        if (usbDeviceList.isNullOrEmpty()) {
            ToastUtils.show("Get usb device failed")
            return
        }
        val list = arrayListOf<String>()
        var selectedIndex: Int = -1
        for (index in (0 until usbDeviceList.size)) {
            val dev = usbDeviceList[index]
            val devName =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !dev.productName.isNullOrEmpty()) {
                    "${dev.productName}(${curDevice?.deviceId})"
                } else {
                    dev.deviceName
                }
            val curDevName =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !curDevice?.productName.isNullOrEmpty()) {
                    "${curDevice!!.productName}(${curDevice.deviceId})"
                } else {
                    curDevice?.deviceName
                }
            if (devName == curDevName) {
                selectedIndex = index
            }
            list.add(devName)
        }
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
    }

    private fun handleCameraError(msg: String?) {
        _channel.invokeMethod("CameraState", "ERROR：$msg")
    }

    private fun handleCameraClosed() {
        _channel.invokeMethod("CameraState", "OPENED")
    }

    private fun handleCameraOpened() {
        _channel.invokeMethod("CameraState", "OPENED")
    }
    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return viewBinding.fragmentContainer
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        viewBinding = ActivityMainBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun getGravity(): Int = Gravity.CENTER
}