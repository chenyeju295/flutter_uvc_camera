import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/uvc_camera_controller.dart';
import 'package:flutter_uvc_camera/uvc_camera_view.dart';

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
                cameraController
                    ?.updateResolution(PreviewSize(width: 352, height: 288));
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
            TextButton(
              child: const Text('点击拍照----------------'),
              onPressed: () {},
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                GestureDetector(
                  onTap: () => takePicture(0),
                  behavior: HitTestBehavior.opaque,
                  child: Container(
                    width: 120,
                    height: 120,
                    color: Colors.green,
                    child: Image.file(File(images[0])),
                  ),
                ),
                GestureDetector(
                  onTap: () => takePicture(1),
                  behavior: HitTestBehavior.opaque,
                  child: Container(
                    width: 120,
                    height: 120,
                    color: Colors.green,
                    child: Image.file(File(images[1])),
                  ),
                ),
                GestureDetector(
                  onTap: () => takePicture(2),
                  behavior: HitTestBehavior.opaque,
                  child: Container(
                    width: 120,
                    height: 120,
                    color: Colors.green,
                    child: Image.file(File(images[2])),
                  ),
                ),
              ],
            ),
            TextButton(
              child: const Text('点击拍照-------------'),
              onPressed: () {},
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                GestureDetector(
                  onTap: () => takePicture(3),
                  behavior: HitTestBehavior.opaque,
                  child: Container(
                    width: 120,
                    height: 120,
                    color: Colors.red,
                    child: Image.file(File(images[3])),
                  ),
                ),
                GestureDetector(
                  onTap: () => takePicture(4),
                  behavior: HitTestBehavior.opaque,
                  child: Container(
                    width: 120,
                    height: 120,
                    color: Colors.red,
                    child: Image.file(File(images[4])),
                  ),
                ),
                GestureDetector(
                  onTap: () => takePicture(5),
                  behavior: HitTestBehavior.opaque,
                  child: Container(
                    width: 120,
                    height: 120,
                    color: Colors.red,
                    child: Image.file(File(images[5])),
                  ),
                ),
              ],
            ),
            TextButton(
              child: const Text('点击拍照------------------'),
              onPressed: () {},
            ),
            GestureDetector(
              onTap: () => takePicture(6),
              behavior: HitTestBehavior.opaque,
              child: Container(
                width: 120,
                height: 120,
                color: Colors.blue,
                child: Image.file(File(images[6])),
              ),
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
}
