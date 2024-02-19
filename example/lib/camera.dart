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
  int _counter = 1;
  @override
  void initState() {
    super.initState();
    cameraController = UVCCameraController();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('USB Camera Debug Page'),
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Text(errText),
            TextButton(
              child: Text('关闭摄像'),
              onPressed: () {
                cameraController?.closeCamera();
              },
            ),
            TextButton(
              child: Text('打开摄像'),
              onPressed: () {
                cameraController?.openUVCCamera();
              },
            ),
            if (cameraController != null)
              SizedBox(
                  child: UVCCameraView(
                      cameraController: cameraController!,
                      params: const UVCCameraViewParamsEntity(
                          aspectRatio: 1, productIds: [52225, 77777], vendorIds: [52281]),
                      width: 300,
                      height: 300)),
            TextButton(
              child: Text('绿光'),
              onPressed: () {
                _counter++;
              },
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
              child: Text('红光'),
              onPressed: () {
                _counter++;
              },
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
              child: Text('蓝光'),
              onPressed: () {
                _counter++;
              },
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
            SizedBox(height: 100)
          ],
        ),
      ),
    );
  }

  takePicture(int i) async {
    String? path = await cameraController?.takePicture();
    print(path);
    if (path != null) {
      images[i] = path;
      setState(() {});
    }
  }
}
