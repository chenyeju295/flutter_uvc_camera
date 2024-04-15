# flutter_uvc_camera

一个基于 [AndroidUSBCamera](https://github.com/jiangdongguo/AndroidUSBCamera) 改写的 Flutter 插件，让 Flutter 开发的安卓软件可以使用外接摄像头。

## 开始使用

### 1. 添加依赖

在你的 Flutter 项目的 `pubspec.yaml` 文件中添加 `flutter_uvc_camera` 插件依赖：

```yaml
dependencies:
  flutter_uvc_camera: 
      path: '本地下载路径'
```

然后运行 `flutter pub get` 命令来安装插件。

### 2. 配置 Android 项目

在使用本插件之前，需要对 Android 项目进行一些配置。

#### 添加权限

在 Android 项目的 `AndroidManifest.xml` 文件中添加以下权限：

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
    <!-- 在这里添加其他所需的权限 -->
```

#### 添加 Intent Filter 和 Meta-data 用于插入设备识别，拔插监听和识别打开应用

在 `mainActivity` 的 intent-filter 中添加 USB 设备连接的 action，并在 meta-data 中引用相应的 XML 文件：

```xml
<intent-filter>
    <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
<meta-data
    android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
    android:resource="@xml/device_filter" />
```

```device_filter.xml
<?xml version="1.0" encoding="utf-8"?>
    <usb>
        <usb-device vendor-id="11111" product-id="22222" />
    </usb>
```

### 3. 使用示例

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
              params: const UVCCameraViewParamsEntity(
                  aspectRatio: 1, productIds: [521115, 77777], vendorIds: [52111, 88888]),
              width: 300,
              height: 300),
        ),
      ),
    );
  }
}
```

## 注意事项

- 请确保在使用插件前已经配置了所需的权限，否则插件可能无法正常工作。
- 插件目前支持在 Flutter 项目中使用外接摄像头，但可能存在一些限制和兼容性问题。请根据实际情况对插件进行测试和调整。

## 问题反馈

如果在使用过程中遇到任何问题或有任何建议，请在 [GitHub Issues](https://github.com/chenyeju295/flutter_uvc_camera/issues) 中反馈。



