# flutter_uvc_camera

一个基于 [AndroidUSBCamera](https://github.com/jiangdongguo/AndroidUSBCamera) 开发的 Flutter 插件，使 Flutter 应用能够使用外接 UVC 摄像头。

pub.dev 地址：[flutter_uvc_camera](https://pub.dev/packages/flutter_uvc_camera)

## 功能特性

- 通过USB连接并控制外部UVC摄像头
- 在Flutter应用中显示摄像头预览
- 拍照并保存到本地存储
- 录制视频并跟踪时间
- 流式传输视频帧（H264格式）和音频帧（AAC格式）用于进一步处理
- 控制摄像头特性如亮度、对比度、焦距等
- 监控摄像头连接状态
- 支持不同的预览分辨率

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

```dart
// 开始录制视频
Future<void> recordVideo() async {
  final path = await cameraController.captureVideo();
  print('视频保存在: $path');
}
```

### 视频流

```dart
// 设置帧回调
cameraController.onVideoFrameCallback = (frame) {
  // 处理H264编码的视频帧
  // frame.data包含编码数据
  // frame.timestamp包含时间戳
  // frame.size包含字节大小
  // frame.fps包含当前帧率
};

cameraController.onAudioFrameCallback = (frame) {
  // 处理AAC编码的音频帧
};

// 开始流传输
cameraController.captureStreamStart();

// 停止流传输
cameraController.captureStreamStop();
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

- `cameraStateCallback`: 相机状态变化的回调
- `msgCallback`: 来自相机的消息回调
- `clickTakePictureButtonCallback`: 摄像头物理按钮被按下时的回调
- `onVideoFrameCallback`: 视频帧数据回调
- `onAudioFrameCallback`: 音频帧数据回调
- `onRecordingTimeCallback`: 录制时间更新回调
- `onStreamStateCallback`: 流状态变化回调

#### 方法

- `initializeCamera()`: 初始化相机
- `openUVCCamera()`: 打开UVC相机
- `closeCamera()`: 关闭相机
- `captureStreamStart()`: 开始捕获视频流
- `captureStreamStop()`: 停止捕获视频流
- `takePicture()`: 拍照并保存到存储
- `captureVideo()`: 开始/停止视频录制
- `setVideoFrameRateLimit(int fps)`: 限制帧率
- `setVideoFrameSizeLimit(int maxBytes)`: 限制帧大小
- `getAllPreviewSizes()`: 获取可用预览尺寸
- `updateResolution(PreviewSize size)`: 更新相机分辨率
- `setCameraFeature(String feature, int value)`: 设置相机特性值
- `resetCameraFeature(String feature)`: 重置相机特性为默认值

### UVCCameraView

用于显示相机预览的小部件。

#### 属性

- `cameraController`: UVCCameraController实例
- `width`: 视图宽度
- `height`: 视图高度
- `params`: 相机初始化的可选参数
- `autoDispose`: 视图销毁时是否自动释放相机

## 常见问题

### 发布模式构建失败

如果在发布模式下遇到`NoSuchMethodError`，请确保已按照安装部分所述正确配置了ProGuard规则。

### USB权限问题

如果没有检测到相机，请检查：

1. 你的设备支持USB OTG
2. 你已正确配置USB设备过滤器
3. 你在AndroidManifest.xml中有适当的权限声明

## 示例

完整示例请查看[示例项目](https://github.com/chenyeju295/flutter_uvc_camera/tree/main/example)。

## 许可

本项目使用MIT许可证 - 详情请参阅LICENSE文件。

## 问题反馈

如果在使用过程中遇到任何问题或有任何建议，请在[GitHub Issues](https://github.com/chenyeju295/flutter_uvc_camera/issues)中反馈。



