# flutter_uvc_camera

A Flutter plugin based on [AndroidUSBCamera](https://github.com/jiangdongguo/AndroidUSBCamera) to enable Flutter apps to use external UVC cameras.

pub：[flutter_uvc_camera](https://pub.dev/packages/flutter_uvc_camera)

## Features

- Connect to and control external UVC cameras through USB
- Display camera preview in your Flutter app
- Take photos and save to local storage
- Record videos with time tracking
- Stream video frames (H264) and audio frames (AAC) for further processing
- Control camera features like brightness, contrast, focus, etc.
- Monitor camera connection status
- Support for different preview resolutions

## Limitations 

- Currently, only supports Android
- For Android 10+, you may need to reduce targetSdkVersion to 27
- Some device models may have compatibility issues (e.g., Redmi Note 10)

## Installation

### 1. Add Dependency

Add the `flutter_uvc_camera` plugin dependency to your Flutter project's `pubspec.yaml` file:

```yaml
dependencies:
  flutter_uvc_camera: ^latest_version
```

### 2. Configure Android Project

#### Add Permissions

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
```

#### Add Repository

In your project's `android/build.gradle`, add the JitPack repository:

```gradle
allprojects {
    repositories {
        // other repositories
        maven { url "https://jitpack.io" }
    }
}
```

#### Configure USB Device Detection

Add an action for USB device connection in the intent-filter of your main Activity, and reference the corresponding XML file in meta-data:

```xml
<intent-filter>
    <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
</intent-filter>
<meta-data
    android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
    android:resource="@xml/device_filter" />
```

Create the `device_filter.xml` file in the `android/app/src/main/res/xml/` directory:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- USB device vendor-id and product-id values for your camera -->
    <!-- You can use wildcard configuration below or specify your camera IDs -->
    <usb-device vendor-id="1234" product-id="5678" class="255" subclass="66" protocol="1" />
</resources>
```

### 3. Configure ProGuard (for release mode)

If you're building in release mode with minification enabled, add these rules to your `android/app/proguard-rules.pro`:

```pro
-keep class com.jiangdg.uvc.UVCCamera {
    native <methods>;
    long mNativePtr;
}
-keep class com.jiangdg.uvc.IStatusCallback {
    *;
}
-keep interface com.jiangdg.uvc.IButtonCallback {
    *;
}
```

And update your `android/app/build.gradle`:

```gradle
buildTypes {
    release {
        // your existing configs
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        minifyEnabled true
    }
}
```

## Usage

### Basic Usage

```dart
import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/flutter_uvc_camera.dart';

class CameraScreen extends StatefulWidget {
  @override
  State<CameraScreen> createState() => _CameraScreenState();
}

class _CameraScreenState extends State<CameraScreen> {
  late UVCCameraController cameraController;
  bool isCameraOpen = false;

  @override
  void initState() {
    super.initState();
    cameraController = UVCCameraController();
    
    // Set up camera state callback
    cameraController.cameraStateCallback = (state) {
      setState(() {
        isCameraOpen = state == UVCCameraState.opened;
      });
    };
  }

  @override
  void dispose() {
    cameraController.closeCamera();
    cameraController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('UVC Camera')),
      body: Column(
        children: [
          // Camera preview
          Container(
            height: 300,
            child: UVCCameraView(
              cameraController: cameraController,
              width: 300, 
              height: 300,
            ),
          ),
          
          // Camera control buttons
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              ElevatedButton(
                onPressed: isCameraOpen ? null : () => cameraController.openUVCCamera(),
                child: Text('Open Camera'),
              ),
              ElevatedButton(
                onPressed: isCameraOpen ? () => cameraController.closeCamera() : null,
                child: Text('Close Camera'),
              ),
              ElevatedButton(
                onPressed: isCameraOpen ? () => takePicture() : null,
                child: Text('Take Picture'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> takePicture() async {
    final path = await cameraController.takePicture();
    if (path != null) {
      print('Picture saved at: $path');
    }
  }
}
```

### Video Recording

```dart
// Start recording a video
Future<void> recordVideo() async {
  final path = await cameraController.captureVideo();
  print('Video saved at: $path');
}
```

### Video Streaming

```dart
// Set up frame callbacks
cameraController.onVideoFrameCallback = (frame) {
  // Process H264 encoded video frame
  // frame.data contains the encoded data
  // frame.timestamp contains the timestamp
  // frame.size contains the size in bytes
  // frame.fps contains the current frame rate
};

cameraController.onAudioFrameCallback = (frame) {
  // Process AAC encoded audio frame
};

// Start streaming
cameraController.captureStreamStart();

// Stop streaming
cameraController.captureStreamStop();
```

### Camera Features Control

```dart
// Set auto focus
await cameraController.setAutoFocus(true);

// Set zoom level
await cameraController.setZoom(5);

// Set brightness
await cameraController.setBrightness(128);

// Get all camera features
final features = await cameraController.getAllCameraFeatures();
```

## API Reference

### UVCCameraController

The main controller class for interacting with the UVC camera.

#### Properties

- `cameraStateCallback`: Callback for camera state changes
- `msgCallback`: Callback for messages from the camera
- `clickTakePictureButtonCallback`: Callback when the camera's physical button is pressed
- `onVideoFrameCallback`: Callback for video frame data
- `onAudioFrameCallback`: Callback for audio frame data
- `onRecordingTimeCallback`: Callback for recording time updates
- `onStreamStateCallback`: Callback for stream state changes

#### Methods

- `initializeCamera()`: Initialize the camera
- `openUVCCamera()`: Open the UVC camera
- `closeCamera()`: Close the UVC camera
- `captureStreamStart()`: Start capturing video stream
- `captureStreamStop()`: Stop capturing video stream
- `takePicture()`: Take a photo and save to storage
- `captureVideo()`: Start/stop video recording
- `setVideoFrameRateLimit(int fps)`: Limit the frame rate
- `setVideoFrameSizeLimit(int maxBytes)`: Limit the frame size
- `getAllPreviewSizes()`: Get available preview sizes
- `updateResolution(PreviewSize size)`: Update camera resolution
- `setCameraFeature(String feature, int value)`: Set camera feature value
- `resetCameraFeature(String feature)`: Reset camera feature to default

### UVCCameraView

Widget to display the camera preview.

#### Properties

- `cameraController`: The UVCCameraController instance
- `width`: The width of the view
- `height`: The height of the view
- `params`: Optional parameters for camera initialization
- `autoDispose`: Whether to automatically dispose the camera when the view is disposed

## Common Issues

### Release Mode Build Failure

If you encounter `NoSuchMethodError` when running in release mode, make sure you've properly configured ProGuard rules as described in the installation section.

### USB Permission Issues

If the camera is not being detected, check that:

1. Your device supports USB OTG
2. You've correctly configured the USB device filter
3. You have proper permissions declarations in AndroidManifest.xml

## Example

For a complete example, check the [example project](https://github.com/chenyeju295/flutter_uvc_camera/tree/main/example).

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Issue Reporting

If you encounter any problems or have any suggestions during usage, please report them on [GitHub Issues](https://github.com/chenyeju295/flutter_uvc_camera/issues).



