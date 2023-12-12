package com.chenyeju.flutter_uvc_camera

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.view.KeyEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel


class MainActivity : FlutterFragmentActivity() {
    private val channelName = "com.chenyeju.flutter_uvc_camera/channel"
    private lateinit var channel: MethodChannel
    private lateinit var mUVCCameraFragment : UVCCameraFragment

    private val vendorId = 52281
    private val productId =  52225


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        channel =  MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
        flutterEngine.
            platformViewsController
            .registry
            .registerViewFactory("uvc_camera_view", UVCCameraViewFactory())

        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "initializeCamera" -> {
                    mUVCCameraFragment = UVCCameraFragment(channel,call.arguments)
                    replaceCameraFragment(mUVCCameraFragment)
                }
                "takePicture" -> {
                    mUVCCameraFragment.takePicture()
                }
                "getAllPreviewSize" -> {
                    mUVCCameraFragment.getAllPreviewSize()
                }
                "getDevicesList" -> {
                    mUVCCameraFragment.getDevicesList()
                }
                "listenToDevice" -> {
                    // Implement listening logic here
                }
                "writeToDevice" -> {
                    if(call.arguments is Int){
                    mUVCCameraFragment.writeToDevice(call.arguments as Int)}
                    result.success("Write attempt started")
                }
                "readFromDevice" -> {
                    // Implement reading logic here
                    result.success("Read attempt started")
                }
                "closeConnection" -> {
                    mUVCCameraFragment.closeConnection()
                }

                "getPlatformVersion" -> {
                    result.success("Android " + Build.VERSION.RELEASE)
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun replaceCameraFragment(fragment: UVCCameraFragment) {
        val hasCameraPermission = PermissionChecker.checkSelfPermission(this,
            Manifest.permission.CAMERA
        )
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED || hasStoragePermission != PermissionChecker.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA
                )) {
                channel.invokeMethod("callFlutter","You have already denied permission access. Go to the Settings page to turn on permissions\n")
            }
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
                0
            )
            return
        }



        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commitAllowingStateLoss()
        if(!fragment.checkCamera()){
            fragment.callFlutter("请插入UVC摄像头")
        }
    }


}

