package com.chenyeju.flutter_uvc_camera

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbRequest
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chenyeju.flutter_uvc_camera.databinding.ActivityMainBinding
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import io.flutter.plugin.common.MethodChannel
import java.nio.ByteBuffer


open class UVCCameraFragment(channel: MethodChannel, arguments: Any?) : CameraFragment() {
    private var viewBinding: ActivityMainBinding? = null
    private val _channel = channel
    private var previewWidth: Int? = null
    private var previewHeight: Int? = null
    private var connection: UsbDeviceConnection? = null

    init {
        if (arguments is Map<*, *>) {
            previewWidth = (arguments["height"] as Number).toInt()
            previewHeight = (arguments["width"] as Number).toInt()
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
            .setPreviewHeight(previewHeight ?: 480)
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
        if (!isCameraOpened()) {
            callFlutter("摄像头未打开")
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
        _channel.invokeMethod("getAllPreviewSizes", list)
    }

    fun getDevicesList() {
        val list = getDeviceList()
        _channel.invokeMethod("getDevicesList", list)
    }

    fun readDevice(){
        try {

        var  usbDevice = getCtrlBlock()?.device
        val usbRequest = UsbRequest()
       var   endpoint = usbDevice?.getInterface(0)?.getEndpoint(0)

        if (endpoint!= null ){
            usbRequest.initialize(connection,endpoint)
            var buffer = ByteArray(endpoint.maxPacketSize)
            var byteBuffer  = ByteBuffer.allocate(endpoint.maxPacketSize)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                usbRequest.queue(byteBuffer)
            }
            buffer = byteBuffer.array()
            // 执行阻塞读取操作
                val received: Int? =
                connection?.bulkTransfer(endpoint, buffer, buffer.size, 0)
                if (received != null) {
                    if (received > 0) {
                        // 处理接收到的数据
                        Log.d("TAG", "readDevice: " + String(buffer, 0, received))
                    }

            }
        }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun writeToDevice(indexData: Int) {
        try {
            connection = getCtrlBlock()?.connection
            val intf =getCtrlBlock()?.device?.getInterface(0)
            if (connection != null) {
              var isOpen =  connection?.claimInterface(intf, true)
                val y0 = listOf("11", "12", "13")
                val i = indexData % 3
                val b1: Byte = Integer.parseInt(y0[i], 16).toByte()
                val b2: Byte = Integer.parseInt("ff", 16).toByte()
                val data = byteArrayOf(0, 120, b1, b2,0x00, 0x00, 0x00, 0x00)
                val buffer1 = byteArrayOf(0x00, 0x82.toByte(), 0xD5.toByte(), 0x4B.toByte(), 0x00, 0x00, 0x00, 0x00)
                val bufferSize = buffer1.size
                val requestType =  UsbConstants.USB_TYPE_VENDOR  or  UsbConstants.USB_DIR_OUT
                val readType =  161
                val dataSize = data.size
                val request = 1
                val request2 = 129
                val value = 2560
                val value2 = 2816
                val index = 1024
                val timeout = 0 // 超时时间（毫秒）


                // 第一个控制传输
                val result: Int? = connection?.controlTransfer(
                    64,
                    1,
                    value,
                    0,
                    byteArrayOf(0),
                    1,
                    timeout
                )

                if (result != null) {
                    if (result < 0) {
                        // 控制传输失败
                        Log.d("USB", "Wrote data to device------------${buffer1.toString()}. $result")
                        return
                    }
//                    val result2: Int? = connection?.controlTransfer(
//                        requestType ,
//                        request,
//                        value2,
//                        index,
//                        data,
//                        size,
//                        timeout
//                    )
                    val result2: Int? = connection?.controlTransfer(
                        readType ,
                        request2,
                        value2,
                        index,
                        data,
                        dataSize,
                        timeout
                    )

                    Log.d("USB", "Wrote data to device------------$buffer1. $result")
                    Log.d("USB", "Wrote data to device------------$data. $result2")
//
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("USB", "$e")

        }

    }

    fun closeConnection() {

//            connection?.releaseInterface(intf)
            connection?.close()


    }


}