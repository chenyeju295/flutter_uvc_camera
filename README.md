# flutter_uvc_camera

A new Flutter plugin project.

## Getting Started

1、MainActivity调整
FlutterActivity=>FlutterFragmentActivity

2、原生工程manifest中添加权限：
<uses-permission android:name="android.permission.USB_PERMISSION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.INTERNET"/>
<uses-feature android:name="android.hardware.usb.host" />
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                 android:maxSdkVersion="34"
                 tools:ignore="ScopedStorage"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="34"/>
<uses-feature android:name="android.hardware.camera"/>
<uses-feature android:name="android.hardware.camera.autofocus"/>
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.WRITE_MEDIA_IMAGES"/>


3、manifest中的.mainActivity中添加如下内容
<intent-filter>
<action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
<category android:name="android.intent.category.DEFAULT" />
</intent-filter>
<meta-data
android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
android:resource="@xml/device_filter" />

