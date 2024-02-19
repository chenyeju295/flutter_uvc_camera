import 'dart:io';

// import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter_uvc_camera/uvc_camera_controller.dart';
import 'package:flutter_uvc_camera/uvc_camera_view.dart';

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
      appBar: AppBar(title: const Text('uvc设备测试')),
      body: Center(
        child: SingleChildScrollView(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const SizedBox(height: 40),
              TextButton(
                  onPressed: () {
                    Navigator.push(context, MaterialPageRoute(builder: (context) => const CameraTest()));
                    setState(() {});
                  },
                  child: const Text('摄像头页面')),
              const SizedBox(height: 20),
              const SizedBox(height: 20),
              if (_picturePath != null)
                Text(
                  _picturePath!,
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
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
