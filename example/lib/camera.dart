import 'dart:io';

import 'package:flutter/cupertino.dart';
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

  String videoPath = '';

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
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
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
              ],
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
                TextButton(
                  child: const Text('captureStreamStart'),
                  onPressed: () {
                    cameraController?.captureStreamStart();
                  },
                ),
                // TextButton(
                //   child: const Text('captureStreamStop'),
                //   onPressed: () {
                //     cameraController?.captureStreamStop();
                //   },
                // ),
              ],
            ),
            Column(
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
                        alignment: Alignment.center,
                        color: Colors.green,
                        child: images[0] == '' ? Text('takePicture') : Image.file(File(images[0])),
                      ),
                    ],
                  ),
                ),
                SizedBox(height: 20),
                GestureDetector(
                  onTap: () => captureVideo(1),
                  behavior: HitTestBehavior.opaque,
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Container(
                        width: 80,
                        height: 80,
                        color: Colors.blue,
                        alignment: Alignment.center,
                        child: Text('take video'),
                      ),
                      Expanded(child: Text("videoPath" + videoPath)),
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

  captureVideo(int i) async {
    String? path = await cameraController?.captureVideo();
    if (path != null) {
      videoPath = path;
      setState(() {});
    }
  }
}
