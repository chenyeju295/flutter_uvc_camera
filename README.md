# flutter_uvc_camera

A Flutter plugin based on [AndroidUSBCamera](https://github.com/jiangdongguo/AndroidUSBCamera) to enable Flutter apps to use external cameras.

pub：[flutter_uvc_camera](https://pub.dev/packages/flutter_uvc_camera)

## Preface 
- Only supports Android
- Android 10 need to reduce targetSdkVersion to 27
- Discovering problematic models：Redmi Note 10 

## Getting Started

### 1. Add Dependency

Add the `flutter_uvc_camera` plugin dependency to your Flutter project's `pubspec.yaml` file:

```yaml
dependencies:
  flutter_uvc_camera: last_version
```


### 2. Configure Android Project
Before using this plugin, some configurations are needed for the Android project.

### Add Permissions
Add the following permissions to the AndroidManifest.xml file of your Android project:
```xml
   <uses-permission android:name="android.permission.USB_PERMISSION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-feature android:name="android.hardware.usb.host" />
    <uses-feature android:name="android.hardware.camera"/>
    <uses-feature android:name="android.hardware.camera.autofocus"/>
    <!-- Add other necessary permissions here -->
```

#### Add `maven { url "https://jitpack.io" }` in android/build.gradle:

```gradle
allprojects {
    repositories {
        /// other repositories
        maven { url "https://jitpack.io" }
    }
}
```

#### Add Intent Filter and Meta-data for Device Insertion Detection, Plug-in Monitoring, and Recognition of Opening Applications

Android documents [USB host overview](https://developer.android.google.cn/develop/connectivity/usb/host?hl=en)

Add an action for USB device connection in the intent-filter of mainActivity, and reference the corresponding XML file in meta-data:

```xml
<intent-filter>
    <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
</intent-filter>
<meta-data
    android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
    android:resource="@xml/device_filter" />
```

```device_filter.xml
<?xml version="1.0" encoding="utf-8"?>

<resources>
    <usb-device vendor-id="1234" product-id="5678" class="255" subclass="66" protocol="1" />
</resources>
```

### 3. Usage Example

```dart
import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/uvc_camera_controller.dart';
import 'package:flutter_uvc_camera/uvc_camera_view.dart';

class CameraTest extends StatefulWidget {
  const CameraTest({super.key});

  @override
  State<CameraTest> createState() => _CameraTestState();
}

class _CameraTestState extends State<CameraTest> {
  UVCCameraController? cameraController;
  
  @override
  void initState() {
    super.initState();
    cameraController = UVCCameraController();
    cameraController?.msgCallback = (state) {
      showCustomToast(state);
    };
  }
  
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('UVC Camera Example'),
        ),
        body: Center(
          child: UVCCameraView(
              cameraController: cameraController!,
              width: 300,
              height: 300),
        ),
      ),
    );
  }
}
```

## Notes
- Ensure that the required permissions are configured before using the plugin, otherwise the plugin may not function 
  properly.
- The plugin currently supports using external cameras in Flutter projects but may have some limitations and 
  compatibility issues. Please test and adjust the plugin according to your needs.

## Issue Reporting
If you encounter any problems or have any suggestions during usage, please report them on
[GitHub Issues](https://github.com/chenyeju295/flutter_uvc_camera/issues) .



