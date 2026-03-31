# flutter_uvc_camera

A Flutter plugin based on [AndroidUSBCamera](https://github.com/jiangdongguo/AndroidUSBCamera) to enable Flutter apps to use external UVC cameras.

pub：[flutter_uvc_camera](https://pub.dev/packages/flutter_uvc_camera)

## Features

- Connect to and control external UVC cameras through USB
- Display camera preview in your Flutter app (aspect ratio is re-applied after the camera opens so rotation/resolution changes do not stretch the image)
- Take photos and save to local storage, or capture **JPEG bytes in memory** (`takePictureBytes`) without writing a file first
- Record videos with time tracking
- Stream video frames (H264) and audio frames (AAC) for further processing, with **lightweight defaults** so raw bytes are not pushed to Dart unless you opt in
- Fine-grained stream controls: enable/disable raw video & audio delivery, **keyframes-only** H264, and **frame sampling** (`setVideoSampleEveryN`); `VideoFrameEvent` includes `isKeyFrame`
- **Preview surface info** (`getPreviewSurfaceInfo`) for aligning Flutter overlays with the actual letterboxed image area inside the platform view
- Control camera features like brightness, contrast, focus, etc.
- Monitor camera connection status
- Support for different preview resolutions; after open, **`actualPreviewSize`** reflects the negotiated preview width/height from native
- Unified **EventChannel** status via **`onStateEvent`** (lifecycle, plugin messages, stream stats); **`onStreamStatsCallback`** receives **`StreamStatsEvent`** (encode FPS, drop rates, and optional **`renderFps`** for native preview render rate)
- Optional **auto-adaptive streaming** (`enableAutoAdaptiveStreaming` / `disableAutoAdaptiveStreaming`) adjusts video FPS/size limits from stream stats when under load

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

### 4. APK size (optional)

The native UVC stack is large. To keep downloads smaller:

- Prefer **Android App Bundle** (`flutter build appbundle`) or **per-ABI APKs** (`flutter build apk --split-per-abi`).
- The plugin’s Android build targets **arm64-v8a** and **armeabi-v7a** only (no x86 emulator ABIs in the plugin AAR), which reduces size versus packaging every ABI.

If your APK still looks very large, check that you are not bundling x86/x86_64 engine and native libs unnecessarily.

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

`captureVideo()` **starts** recording; the returned `Future` **completes with the saved file path when recording stops**. Use **`stopVideo()`** to stop explicitly, or call **`captureVideo()` again** while recording (toggle stop). If you call `captureVideo()` while already recording, that second call finishes with an empty string and the **original** `captureVideo()` `Future` receives the real path.

```dart
Future<void> recordFiveSeconds() async {
  final whenDone = cameraController.captureVideo();
  await Future.delayed(const Duration(seconds: 5));
  await cameraController.stopVideo();
  final path = await whenDone;
  if (path != null && path.isNotEmpty) {
    print('Video saved at: $path');
  }
}
```

### `UVCCameraView` parameters (`streamProfile`)

By default the plugin uses a **lightweight** profile: stream events still carry state/stats, but **raw H264/AAC bytes are not sent to Dart** until you choose a heavier profile or enable delivery explicitly.

```dart
UVCCameraView(
  cameraController: cameraController,
  width: 300,
  height: 300,
  params: const UVCCameraViewParamsEntity(
    streamProfile: UVCCameraViewParamsEntity.streamProfileLightweight, // default
    // streamProfileStreaming — push raw video/audio bytes to Dart
    // streamProfileKeyframesOnly — push H264 keyframes only (IDR)
  ),
),
```

At runtime you can still toggle delivery without recreating the view:

```dart
await cameraController.setStreamDataEnabled(video: true, audio: true);
await cameraController.setVideoKeyframesOnly(true);
await cameraController.setVideoSampleEveryN(2); // deliver every 2nd video frame
```

### Video streaming

```dart
// Set up frame callbacks (only receive raw bytes when streamProfile / setStreamDataEnabled allows it)
cameraController.onVideoFrameCallback = (frame) {
  // H264: frame.data, frame.timestamp, frame.size, frame.fps, frame.isKeyFrame
};

cameraController.onAudioFrameCallback = (frame) {
  // AAC encoded audio
};

cameraController.captureStreamStart();
// ...
cameraController.captureStreamStop();
```

Periodic **`StreamStatsEvent`** (when the stream is active) includes `videoFps`, drop counts, `videoDropRate` / `audioDropRate`, and **`renderFps`** (0 if unavailable). The legacy separate `RENDER_FPS` channel event is deprecated; use **`StreamStatsEvent.renderFps`**.

```dart
cameraController.onStreamStatsCallback = (stats) {
  // stats.videoFps, stats.renderFps, stats.videoDropRate, ...
};
```

### In-memory photo (`takePictureBytes`)

Returns JPEG bytes without saving to storage (useful for uploads or fast preview). Ensure the camera is previewing; in OpenGL preview mode the native side may briefly enable a frame callback to obtain NV21 before encoding.

```dart
final bytes = await cameraController.takePictureBytes();
```

### Overlay alignment (`getPreviewSurfaceInfo`)

When `aspectRatioShow` keeps letterboxing, the visible video rect may be smaller than the `UVCCameraView` box. Call after the camera is **opened** and layout has settled (e.g. in your `opened` state callback, optionally after a short `post-frame` delay):

```dart
final info = await cameraController.getPreviewSurfaceInfo();
// Use info.offsetLeftRatio, offsetTopRatio, surfaceWidthRatio, surfaceHeightRatio
// to position Stack/Positioned overlays in the same bounds as the native TextureView.
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

- `cameraState`: Current `UVCCameraState`
- `cameraStateCallback`: Callback for camera state changes
- `onStateEvent`: Unified callback for native→Dart `EventChannel` events (prefer for new code)
- `msgCallback`: Callback for messages from the camera
- `clickTakePictureButtonCallback`: Callback when the camera's physical button is pressed
- `onVideoFrameCallback`: Callback for video frame data
- `onAudioFrameCallback`: Callback for audio frame data
- `onRecordingTimeCallback`: Callback for recording time updates
- `onStreamStateCallback`: Callback for stream-related state events
- `onStreamStatsCallback`: Callback for `StreamStatsEvent` (FPS, drops, `renderFps`)
- `actualPreviewSize`: Negotiated preview `PreviewSize` after `opened` (from native `previewWidth` / `previewHeight`)
- `isRecording`, `currentRecordingTimeMs`, `currentRecordingTimeFormatted`: Recording state and time (also driven by `recordingTime` events)

#### Methods

- `initializeCamera()`: Initialize the camera
- `openUVCCamera()`: Open the UVC camera
- `closeCamera()`: Close the UVC camera
- `captureStreamStart()` / `captureStreamStop()`: Start/stop the EventChannel stream (stats + optional raw frames)
- `setStreamDataEnabled({bool video, bool audio})` / `getStreamDataEnabled()`: Toggle raw byte delivery to Dart
- `setVideoKeyframesOnly(bool)` / `getVideoKeyframesOnly()`: H264 keyframes only
- `setVideoSampleEveryN(int)` / `getVideoSampleEveryN()`: Deliver every Nth video frame
- `enableAutoAdaptiveStreaming` / `disableAutoAdaptiveStreaming`: Optional client-side adaptation from `StreamStatsEvent` (video FPS/size)
- `takePicture()`: Take a photo and save to storage
- `takePictureBytes()`: Take a photo and return JPEG bytes in memory
- `captureVideo()` / `stopVideo()`: Start recording (Future completes when stopped) / stop recording; see **Video Recording** above
- `setVideoFrameRateLimit` / `getVideoFrameRateLimit`: Frame rate cap for stream events
- `setVideoFrameSizeLimit` / `setAudioFrameSizeLimit` / `getAudioFrameSizeLimit`: Byte size limits
- `getAllPreviewSizes()`: Get available preview sizes
- `getCurrentCameraRequestParameters()`: Current negotiated request as JSON string
- `updateResolution(PreviewSize size)`: Update camera resolution
- `updateCameraViewParams(UVCCameraViewParamsEntity params)`: Update preview parameters at runtime (may restart preview)
- `getPreviewSurfaceInfo()`: Letterboxed preview rect inside the platform view (for overlays)
- `setCameraFeature` / `getCameraFeature` / `resetCameraFeature` / `getAllCameraFeatures`
- Convenience setters: `setAutoFocus`, `setZoom`, `setBrightness`, etc.
- `startPlayMic` / `stopPlayMic` / `isMicPlaying()` (async query of native mic playback state)

### UVCCameraView

Widget to display the camera preview.

#### Properties

- `cameraController`: The UVCCameraController instance
- `width`: The width of the view
- `height`: The height of the view
- `params`: Optional `UVCCameraViewParamsEntity` (preview size/fps, `aspectRatioShow`, `rotateType`, `streamProfile`, etc.). Defaults include `streamProfileLightweight`.
- `autoDispose`: Whether to automatically dispose the camera when the view is disposed

## Common Issues

### Release Mode Build Failure

If you encounter `NoSuchMethodError` when running in release mode, make sure you've properly configured ProGuard rules as described in the installation section.

### USB Permission Issues

If the camera is not being detected, check that:

1. Your device supports USB OTG
2. You've correctly configured the USB device filter
3. You have proper permissions declarations in AndroidManifest.xml

### Preview looks stretched after rotation

Ensure `aspectRatioShow` is enabled (default in params) so the native view keeps the negotiated preview aspect ratio; the plugin reapplies it when the camera opens.

### No raw stream bytes in Dart

The default `streamProfile` is **lightweight**. Switch to `streamProfileStreaming` or call `setStreamDataEnabled(video: true, audio: true)` after the controller is ready.

### Overlay misaligned with video

Use `getPreviewSurfaceInfo()` and position overlays using the returned ratios, not only the outer `SizedBox` size.

## Example

For a complete example, check the [example project](https://github.com/chenyeju295/flutter_uvc_camera/tree/main/example).

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Issue Reporting

If you encounter any problems or have any suggestions during usage, please report them on [GitHub Issues](https://github.com/chenyeju295/flutter_uvc_camera/issues).



