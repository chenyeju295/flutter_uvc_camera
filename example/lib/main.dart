import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/uvc_camera_controller.dart';

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
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('uvc设备测试')),
      body: Center(
        child: TextButton(
            onPressed: () {
              Navigator.push(context,
                  MaterialPageRoute(builder: (context) => const CameraTest()));
            },
            child: const Text('camera test')),
      ),
    );
  }
}
