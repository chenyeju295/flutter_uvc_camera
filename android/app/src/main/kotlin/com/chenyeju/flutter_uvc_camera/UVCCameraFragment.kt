package com.chenyeju.flutter_uvc_camera

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chenyeju.flutter_uvc_camera.databinding.ActivityMainBinding
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import io.flutter.plugin.common.MethodChannel


open class UVCCameraFragment(channel: MethodChannel, arguments: Any?) : CameraFragment() {
    private  var viewBinding: ActivityMainBinding?= null
    private val _channel = channel
    private  var previewWidth : Int?=null
    private  var previewHeight : Int?=null
    init {
        if ( arguments is Map<*, *>) {
            previewWidth =  (arguments["height"] as Number ).toInt()
            previewHeight = ( arguments["width"] as Number).toInt()
        }
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View? {
        if (viewBinding == null) {
            viewBinding = ActivityMainBinding.inflate(inflater, container, false)
        }
        return viewBinding?.root
    }

    override fun getCameraViewContainer(): ViewGroup? {
        return viewBinding?.fragmentContainer
    }
    override fun getCameraView(): IAspectRatio {

        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder().setPreviewWidth(previewWidth ?: 640)
            .setPreviewHeight(  previewHeight ?:  480)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setDefaultRotateType(RotateType.ANGLE_0)
            .setAudioSource(CameraRequest.AudioSource.SOURCE_SYS_MIC)
            .setAspectRatioShow(true)
            .setCaptureRawImage(false)
            .setRawPreviewData(false)
            .create();
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


    fun takePicture() {
        if (! isCameraOpened()) {
            callFlutter( "摄像头未打开")
            return
        }
        captureImage()
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
    fun getAllPreviewSize() {
        val list = getAllPreviewSizes()
        _channel.invokeMethod("getAllPreviewSizes",list )
    }

    fun getDevicesList() {
        val list = getDeviceList()
        _channel.invokeMethod("getDevicesList",list )
    }

    fun writeToDevice(indexData: Int) {
        try {

            var usbDevice: UsbDevice?  = null
            getCurrentCamera()?.let { strategy ->
                if (strategy is CameraUVC) {
                    usbDevice = strategy.getUsbDevice()
                }
            }
            val connection : UsbDeviceConnection? =null
            if(usbDevice != null && connection != null){
                    val intf = usbDevice!!.getInterface(0)
                    var  Data = usbDevice!!.serialNumber
                    connection?.claimInterface(intf,true)
                    if (intf.endpointCount > 0) {
                            val y0 = listOf("11", "12", "13")
                            val i =  indexData % 3
                            val b1: Byte = Integer.parseInt(y0[i], 16).toByte()
                            val b2: Byte = Integer.parseInt("ff", 16).toByte()

                            val data = byteArrayOf(0, 120,b1,b2)

                            val size = data.size

                            val request = 1

                            val value = (0x03 shl 8) or 54619

                            val index = 1024
                            val timeout = 0 // 超时时间（毫秒）


                            // 第一个控制传输

                            // 第一个控制传输
                            var result: Int? = connection?.controlTransfer(
                                64, // requestType: 对于IN传输（设备到主机），通常是0xC0（192），对于OUT传输（主机到设备），通常是0x40（64）。
                                request,//request: 根据您的设备协议，可能是1或129。
                                2560, //value: 对于2560，十进制是2560；对于2816，十进制是2816。
                                index,
                                byteArrayOf(0) , 1,
                                timeout
                            )
                            if (result != null) {
                                if (result < 0) {
                                    // 控制传输失败
                                    Log.d("USB", "Wrote data to device.  $result ")

                                    return
                                }
                                result = connection?.controlTransfer(
                                64, // requestType: 对于IN传输（设备到主机），通常是0xC0（192），对于OUT传输（主机到设备），通常是0x40（64）。
                                request,//request: 根据您的设备协议，可能是1或129。
                                2816, //value: 对于2560，十进制是2560；对于2816，十进制是2816。
                                index,
                                data, size,
                                timeout
                                )

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

    fun closeConnection() {
        val usbDevice: UsbDevice? = null
        val connection: UsbDeviceConnection? = null
        if (usbDevice != null && connection != null) {

            val intf = usbDevice!!.getInterface(0)
            connection?.releaseInterface(intf)
            connection?.close()

        }
    }


}