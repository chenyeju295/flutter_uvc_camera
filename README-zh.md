# flutter_uvc_camera

一个基于 [AndroidUSBCamera](https://github.com/jiangdongguo/AndroidUSBCamera) 开发的 Flutter 插件，使 Flutter 应用能够使用外接 UVC 摄像头。

pub.dev 地址：[flutter_uvc_camera](https://pub.dev/packages/flutter_uvc_camera)

## 功能特性

- 通过 USB 连接并控制外部 UVC 摄像头
- 在 Flutter 应用中显示摄像头预览（相机打开后会再次应用宽高比，减轻旋转/分辨率变化导致的画面拉伸）
- 拍照保存到本地，或使用 **`takePictureBytes()`** 在内存中获取 JPEG，无需先写文件
- 录制视频并跟踪时间
- 流式传输视频帧（H264）与音频帧（AAC）；默认采用**轻量配置**，仅在需要时向 Dart 投递原始字节
- 流控能力：开关原始视频/音频投递、**仅关键帧（H264 IDR）**、按间隔抽样（`setVideoSampleEveryN`）；`VideoFrameEvent` 含 **`isKeyFrame`**
- **`getPreviewSurfaceInfo()`**：获取平台视图内实际画面区域，便于 Flutter 叠加层（overlay）与视频对齐
- 控制亮度、对比度、变焦等相机能力
- 监控摄像头连接状态
- 支持多种预览分辨率；相机打开后，**`actualPreviewSize`** 表示原生协商后的预览宽高
- 通过 **`onStateEvent`** 统一接收 **EventChannel** 状态（生命周期、插件消息、流统计等）；**`onStreamStatsCallback`** 可收到 **`StreamStatsEvent`**（编码 FPS、丢帧率，以及可选的 **`renderFps`** 原生预览渲染帧率）
- 可选 **自适应流控**（**`enableAutoAdaptiveStreaming`** / **`disableAutoAdaptiveStreaming`**）：根据流统计在负载高时自动调节视频 FPS/单帧大小上限

## 限制

- 目前仅支持Android平台
- 对于Android 10及更高版本，可能需要将targetSdkVersion降至27
- 部分设备型号可能存在兼容性问题（如Redmi Note 10）

## 安装

### 1. 添加依赖

在你的Flutter项目的`pubspec.yaml`文件中添加`flutter_uvc_camera`插件依赖：

```yaml
dependencies:
  flutter_uvc_camera: ^最新版本
```

### 2. 配置Android项目

#### 添加权限

在Android项目的`AndroidManifest.xml`文件中添加以下权限：

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

#### 添加仓库

在项目的`android/build.gradle`中添加JitPack仓库：

```gradle
allprojects {
    repositories {
        // 其他仓库
        maven { url "https://jitpack.io" }
    }
}
```

#### 配置USB设备检测

在主Activity的intent-filter中添加USB设备连接的action，并在meta-data中引用相应的XML文件：

```xml
<intent-filter>
    <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
</intent-filter>
<meta-data
    android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
    android:resource="@xml/device_filter" />
```

在`android/app/src/main/res/xml/`目录下创建`device_filter.xml`文件：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 摄像头的USB vendor-id和product-id值 -->
    <!-- 你可以使用下面的通配符配置或者指定你的摄像头ID -->
    <usb-device vendor-id="1234" product-id="5678" class="255" subclass="66" protocol="1" />
</resources>
```

### 3. 配置ProGuard（用于发布模式）

如果你在启用混淆的发布模式下构建，请在`android/app/proguard-rules.pro`中添加这些规则：

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

并更新你的`android/app/build.gradle`：

```gradle
buildTypes {
    release {
        // 你现有的配置
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        minifyEnabled true
    }
}
```

### 4. 减小 APK 体积（可选）

UVC 相关 native 库体积较大，可配合以下方式缩小用户下载：

- 优先使用 **AAB**（`flutter build appbundle`）或 **按 ABI 分包 APK**（`flutter build apk --split-per-abi`）。
- 插件 Android 侧仅包含 **arm64-v8a** 与 **armeabi-v7a**（不包含 x86 模拟器 ABI），相比打包全部 ABI 可明显减小体积。

若整包 APK 仍异常偏大，请检查是否误打包了 x86/x86_64 引擎与 native 库。

## 使用方法

### 基础用法

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
    
    // 设置相机状态回调
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
      appBar: AppBar(title: Text('UVC 摄像头')),
      body: Column(
        children: [
          // 相机预览
          Container(
            height: 300,
            child: UVCCameraView(
              cameraController: cameraController,
              width: 300,
              height: 300,
            ),
          ),
          
          // 相机控制按钮
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              ElevatedButton(
                onPressed: isCameraOpen ? null : () => cameraController.openUVCCamera(),
                child: Text('打开相机'),
              ),
              ElevatedButton(
                onPressed: isCameraOpen ? () => cameraController.closeCamera() : null,
                child: Text('关闭相机'),
              ),
              ElevatedButton(
                onPressed: isCameraOpen ? () => takePicture() : null,
                child: Text('拍照'),
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
      print('照片保存在: $path');
    }
  }
}
```

### 视频录制

**`captureVideo()`** 会**开始**录像；返回的 **`Future` 在停止录制后**以保存路径完成。请使用 **`stopVideo()`** 显式结束，或在录制中**再次调用 `captureVideo()`**（切换停止）。若在已录制时再次调用 `captureVideo()`，后一次调用会得到空字符串，**第一次** `captureVideo()` 的 `Future` 才会收到真实文件路径。

```dart
Future<void> recordFiveSeconds() async {
  final whenDone = cameraController.captureVideo();
  await Future.delayed(const Duration(seconds: 5));
  await cameraController.stopVideo();
  final path = await whenDone;
  if (path != null && path.isNotEmpty) {
    print('视频保存在: $path');
  }
}
```

### `UVCCameraView` 参数（`streamProfile`）

默认使用 **轻量（lightweight）** 配置：仍会收到状态/统计等事件，但**不会**向 Dart 投递原始 H264/AAC，除非你改用更重档位或显式开启投递。

```dart
UVCCameraView(
  cameraController: cameraController,
  width: 300,
  height: 300,
  params: const UVCCameraViewParamsEntity(
    streamProfile: UVCCameraViewParamsEntity.streamProfileLightweight, // 默认
    // streamProfileStreaming — 向 Dart 投递原始音视频字节
    // streamProfileKeyframesOnly — 仅投递 H264 关键帧（IDR）
  ),
),
```

运行中也可在不重建视图的情况下切换：

```dart
await cameraController.setStreamDataEnabled(video: true, audio: true);
await cameraController.setVideoKeyframesOnly(true);
await cameraController.setVideoSampleEveryN(2); // 每 2 帧投递一次视频
```

### 视频流

```dart
// 仅在 streamProfile / setStreamDataEnabled 允许时才会收到原始字节
cameraController.onVideoFrameCallback = (frame) {
  // H264：frame.data、timestamp、size、fps、isKeyFrame
};

cameraController.onAudioFrameCallback = (frame) {
  // AAC 音频帧
};

cameraController.captureStreamStart();
cameraController.captureStreamStop();
```

流开启后，周期性 **`StreamStatsEvent`** 包含 `videoFps`、丢帧统计、`videoDropRate` / `audioDropRate`，以及 **`renderFps`**（不可用时为 0）。单独的 **`RENDER_FPS`** 通道事件已废弃，请使用 **`StreamStatsEvent.renderFps`**。

```dart
cameraController.onStreamStatsCallback = (stats) {
  // stats.videoFps、stats.renderFps、stats.videoDropRate 等
};
```

### 内存拍照（`takePictureBytes`）

返回 JPEG 字节，不落盘。请先保证正在预览；OpenGL 预览模式下，原生侧可能会短暂开启帧回调以获取 NV21 再编码。

```dart
final bytes = await cameraController.takePictureBytes();
```

### 叠加层对齐（`getPreviewSurfaceInfo`）

当开启 `aspectRatioShow` 出现黑边时，实际画面可能小于外层 `UVCCameraView` 尺寸。请在相机 **已打开** 且布局稳定后调用（例如在 `opened` 状态回调里，必要时 `addPostFrameCallback` 延迟一帧）：

```dart
final info = await cameraController.getPreviewSurfaceInfo();
// 使用 offsetLeftRatio、offsetTopRatio、surfaceWidthRatio、surfaceHeightRatio
// 在 Stack/Positioned 中与原生 TextureView 可见区域对齐。
```

### 相机特性控制

```dart
// 设置自动对焦
await cameraController.setAutoFocus(true);

// 设置变焦级别
await cameraController.setZoom(5);

// 设置亮度
await cameraController.setBrightness(128);

// 获取所有相机特性
final features = await cameraController.getAllCameraFeatures();
```

## API参考

### UVCCameraController

与UVC摄像头交互的主控制器类。

#### 属性

- `cameraState`：当前 `UVCCameraState`
- `cameraStateCallback`：相机状态变化回调
- `onStateEvent`：统一接收原生→Dart 的 **EventChannel** 事件（新代码优先使用）
- `msgCallback`：来自相机的消息回调
- `clickTakePictureButtonCallback`：摄像头物理按钮被按下时的回调
- `onVideoFrameCallback`：视频帧数据回调
- `onAudioFrameCallback`：音频帧数据回调
- `onRecordingTimeCallback`：录制时间更新回调
- `onStreamStateCallback`：与流相关的状态事件回调
- `onStreamStatsCallback`：`StreamStatsEvent` 回调（FPS、丢帧、`renderFps`）
- `actualPreviewSize`：`opened` 后协商的预览 `PreviewSize`（来自原生 `previewWidth` / `previewHeight`）
- `isRecording`、`currentRecordingTimeMs`、`currentRecordingTimeFormatted`：录制状态与时间（亦由 `recordingTime` 事件更新）

#### 方法

- `initializeCamera()`：初始化相机
- `openUVCCamera()` / `closeCamera()`：打开/关闭 UVC 相机
- `captureStreamStart()` / `captureStreamStop()`：开始/停止 EventChannel 流（统计 + 可选原始帧）
- `setStreamDataEnabled` / `getStreamDataEnabled`：开关向 Dart 投递原始字节
- `setVideoKeyframesOnly` / `getVideoKeyframesOnly`：仅 H264 关键帧
- `setVideoSampleEveryN` / `getVideoSampleEveryN`：每 N 帧投递一次视频
- `enableAutoAdaptiveStreaming` / `disableAutoAdaptiveStreaming`：根据 `StreamStatsEvent` 可选自适应（视频 FPS/大小）
- `takePicture()`：拍照并保存到存储
- `takePictureBytes()`：拍照并返回内存中的 JPEG
- `captureVideo()` / `stopVideo()`：开始录像（Future 在停止后完成）/ 停止录像；详见上文 **视频录制**
- `setVideoFrameRateLimit` / `getVideoFrameRateLimit`：流事件帧率上限
- `setVideoFrameSizeLimit` / `setAudioFrameSizeLimit` / `getAudioFrameSizeLimit`：单帧字节上限
- `getAllPreviewSizes()`：可用预览尺寸列表
- `getCurrentCameraRequestParameters()`：当前协商参数（JSON 字符串）
- `updateResolution`：更新分辨率
- `updateCameraViewParams`：运行时更新预览参数（可能重启预览）
- `getPreviewSurfaceInfo()`：黑边内实际画面区域（用于 overlay）
- `setCameraFeature` / `getCameraFeature` / `resetCameraFeature` / `getAllCameraFeatures`
- 便捷方法：`setAutoFocus`、`setZoom`、`setBrightness` 等
- `startPlayMic` / `stopPlayMic` / `isMicPlaying()`（异步查询原生麦克风播放状态）

### UVCCameraView

用于显示相机预览的小部件。

#### 属性

- `cameraController`：`UVCCameraController` 实例
- `width` / `height`：视图尺寸
- `params`：可选 `UVCCameraViewParamsEntity`（预览分辨率/fps、`aspectRatioShow`、`rotateType`、`streamProfile` 等），默认含 `streamProfileLightweight`
- `autoDispose`：视图销毁时是否自动释放相机

## 常见问题

### 发布模式构建失败

如果在发布模式下遇到`NoSuchMethodError`，请确保已按照安装部分所述正确配置了ProGuard规则。

### USB权限问题

如果没有检测到相机，请检查：

1. 你的设备支持USB OTG
2. 你已正确配置USB设备过滤器
3. 你在AndroidManifest.xml中有适当的权限声明

### 旋转后预览被拉伸

请保持 `aspectRatioShow` 开启（参数默认值），使原生视图按协商分辨率保持宽高比；插件在相机打开时会再次应用该比例。

### Dart 端收不到原始流数据

默认 `streamProfile` 为 **lightweight**。请改用 `streamProfileStreaming`，或在控制器就绪后调用 `setStreamDataEnabled(video: true, audio: true)`。

### 叠加层与画面对不齐

请使用 `getPreviewSurfaceInfo()` 返回的比例定位 `Stack`/`Positioned`，不要仅按外层 `SizedBox` 尺寸布局。

## 示例

完整示例请查看[示例项目](https://github.com/chenyeju295/flutter_uvc_camera/tree/main/example)。

## 许可

本项目使用MIT许可证 - 详情请参阅LICENSE文件。

## 问题反馈

如果在使用过程中遇到任何问题或有任何建议，请在[GitHub Issues](https://github.com/chenyeju295/flutter_uvc_camera/issues)中反馈。



