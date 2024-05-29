import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/uvc_camera.dart';

class CameraTest extends StatefulWidget {
  const CameraTest({super.key});

  @override
  State<CameraTest> createState() => _CameraTestState();
}

class _CameraTestState extends State<CameraTest> {
  int selectIndex = 0;
  List<String> images = ['', '', '', '', '', '', ''];
  String errText = '';
  UVCCameraController? cameraController;
  @override
  void initState() {
    super.initState();
    cameraController = UVCCameraController();
    cameraController?.msgCallback = (state) {
      showCustomToast(state);
    };
  }

  void showCustomToast(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 1), // 设置持续时间
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('USB Camera Debug Page'),
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Text(errText),
            TextButton(
              child: const Text('close'),
              onPressed: () {
                cameraController?.closeCamera();
              },
            ),
            TextButton(
              child: const Text('open'),
              onPressed: () {
                cameraController?.openUVCCamera();
              },
            ),
            if (cameraController != null)
              SizedBox(
                  child: UVCCameraView(
                      cameraController: cameraController!,
                      params: const UVCCameraViewParamsEntity(frameFormat: 0),
                      width: 300,
                      height: 300)),
            TextButton(
              child: const Text('updateResolution'),
              onPressed: () async {
                await cameraController?.getAllPreviewSizes();
                cameraController?.updateResolution(PreviewSize(width: 352, height: 288));
              },
            ),
            TextButton(
              child: const Text('getCurrentCameraRequestParameters'),
              onPressed: () {
                cameraController
                    ?.getCurrentCameraRequestParameters()
                    .then((value) => showCustomToast(value.toString()));
              },
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                GestureDetector(
                  onTap: () => takePicture(0),
                  behavior: HitTestBehavior.opaque,
                  child: Column(
                    children: [
                      Container(
                        width: 80,
                        height: 80,
                        color: Colors.green,
                        child: Image.file(File(images[0])),
                      ),
                      const Text('takePicture'),
                    ],
                  ),
                ),
                GestureDetector(
                  onTap: () => takeVideo(),
                  behavior: HitTestBehavior.opaque,
                  child: Column(
                    children: [
                      Container(
                        width: 80,
                        height: 80,
                        color: Colors.red,
                        child: Image.file(File(images[1])),
                      ),
                      const Text('takeVideo'),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 100)
          ],
        ),
      ),
    );
  }

  takePicture(int i) async {
    String? path = await cameraController?.takePicture();
    if (path != null) {
      images[i] = path;
      setState(() {});
    }
  }

  takeVideo() {}
}
