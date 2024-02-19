import 'dart:io';

// import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter_uvc_camera/uvc_camera_controller.dart';
import 'package:flutter_uvc_camera/uvc_camera_view.dart';
import 'package:permission_handler/permission_handler.dart';

import 'camera.dart';

void main() {
  runApp(const MaterialApp(home: MyApp()));
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _counter = 0;
  final cameraController = UVCCameraController();
  String? _picturePath;

  /// 获取当前Android版本
  // Future<double> getAndroidVersion() async {
  //   var androidInfo = await DeviceInfoPlugin().androidInfo;
  //   var release = double.parse(androidInfo.version.release);
  //   return release;
  // }
  //
  // Future<bool> requestImageStoragePermission() async {
  //   if (Platform.isIOS) {
  //     var status = await Permission.photos.status;
  //     if (!status.isGranted) {
  //       status = await Permission.photos.request();
  //     }
  //
  //     if (status.isGranted) {
  //       // permission granted, save the image
  //       return true;
  //     } else if (status.isPermanentlyDenied) {
  //       return false;
  //     } else {
  //       // permission denied, but can ask for permission again
  //       return false;
  //     }
  //   } else if (Platform.isAndroid) {
  //     var version = await getAndroidVersion();
  //     Permission permission;
  //
  //     if (version >= 13) {
  //       permission = Permission.photos;
  //     } else {
  //       permission = Permission.storage;
  //     }
  //
  //     var status = await permission.status;
  //     if (!status.isGranted) {
  //       status = await permission.request();
  //     }
  //
  //     if (status.isGranted) {
  //       // permission granted, save the image
  //       return true;
  //     } else if (status.isPermanentlyDenied) {
  //       return false;
  //     } else {
  //       // permission denied, but can ask for permission again
  //       return false;
  //     }
  //   }
  //   return false;
  // }

  @override
  void initState() {
    super.initState();
    _incrementCounter();
  }

  void _incrementCounter() {
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text('uvc设备测试'),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: '刷新',
        child: const Icon(Icons.refresh),
      ),
      body: Center(
        child: SingleChildScrollView(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const SizedBox(height: 40),
              Text(cameraController.getCameraState.toString()),
              Text("按钮拍照路径：${cameraController.getTakePicturePath}"),
              const SizedBox(height: 40),
              TextButton(
                  onPressed: () {
                    Navigator.push(context, MaterialPageRoute(builder: (context) => const CameraTest()));
                    setState(() {});
                  },
                  child: const Text('二级页面')),
              TextButton(
                  onPressed: () {
                    cameraController.openUVCCamera();
                    setState(() {});
                  },
                  child: const Text('打开摄像')),
              const SizedBox(height: 20),
              // SizedBox(child: UVCCameraView(cameraController: cameraController, width: 300, height: 300)),
              const SizedBox(height: 20),
              TextButton(
                  onPressed: () async {
                    _picturePath = await cameraController.takePicture();

                    // requestImageStoragePermission().then((value) async {
                    //   if (value) {
                    //     await Future.delayed(const Duration(seconds: 1));
                    //     _picturePath = await cameraController.takePicture();
                    //     setState(() {});
                    //   }
                    // });
                  },
                  child: const Text('takePicture')),
              if (_picturePath != null)
                Text(
                  _picturePath!,
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              const SizedBox(height: 20),
              TextButton(
                  onPressed: () {
                    // cameraController.writeToDevice(_counter);
                    _counter++;
                    setState(() {});
                  },
                  child: const Text('writeToDevice')),
              const SizedBox(height: 20),

              const SizedBox(height: 20),
              ...cameraController.getCallStrings.map((e) => Text(e)),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }
}
