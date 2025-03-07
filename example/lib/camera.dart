import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/flutter_uvc_camera.dart';

class CameraTest extends StatefulWidget {
  const CameraTest({super.key});

  @override
  State<CameraTest> createState() => _CameraTestState();
}

class _CameraTestState extends State<CameraTest> {
  int selectIndex = 0;
  List<String> images = ['', '', '', '', '', '', ''];
  String errText = '';
  CameraController? cameraController;
  bool _isInitialized = false;
  
  @override
  void initState() {
    super.initState();
    _initializeCamera();
  }
  
  Future<void> _initializeCamera() async {
    cameraController = CameraController();
    
    // 监听错误和状态变化
    cameraController?.onError.listen((error) {
      setState(() => errText = error);
      showCustomToast("Error: $error");
    });
    
    cameraController?.onStateChanged.listen((state) {
      setState(() {
        _isInitialized = state == CameraState.opened;
      });
      showCustomToast("Camera state: $state");
    });
    
    try {
      await cameraController?.initialize();
    } catch (e) {
      setState(() => errText = e.toString());
    }
  }

  void showCustomToast(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 1),
      ),
    );
  }

  String videoPath = '';

  @override
  void dispose() {
    cameraController?.dispose();
    super.dispose();
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
            if (errText.isNotEmpty) 
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: Text(
                  errText,
                  style: const TextStyle(color: Colors.red),
                ),
              ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                TextButton(
                  child: const Text('Close'),
                  onPressed: () {
                    cameraController?.close();
                  },
                ),
                TextButton(
                  child: const Text('Open'),
                  onPressed: () {
                    cameraController?.open();
                  },
                ),
              ],
            ),
            if (cameraController != null)
              SizedBox(
                width: 300,
                height: 300,
                child: UVCCameraPreview(
                  controller: cameraController!,
                  errorBuilder: (context, error) => Center(
                    child: Text(
                      error,
                      style: const TextStyle(color: Colors.red),
                    ),
                  ),
                  loadingBuilder: (context) => const Center(
                    child: CircularProgressIndicator(),
                  ),
                ),
              ),
            TextButton(
              child: const Text('Update Resolution'),
              onPressed: () async {
                try {
                  final sizes = await cameraController?.getPreviewSizes();
                  if (sizes != null && sizes.isNotEmpty) {
                    final size = sizes.first;
                    final width = size['width'] as int;
                    final height = size['height'] as int;
                    await cameraController?.updateResolution(width, height);
                    showCustomToast('Resolution updated to ${width}x$height');
                  }
                } catch (e) {
                  showCustomToast('Failed to update resolution: $e');
                }
              },
            ),
            TextButton(
              child: const Text('Get Camera Info'),
              onPressed: () async {
                try {
                  final info = await cameraController?.getCameraInfo();
                  showCustomToast(info.toString());
                } catch (e) {
                  showCustomToast('Failed to get camera info: $e');
                }
              },
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                TextButton(
                  child: const Text('Start Streaming'),
                  onPressed: () {
                    cameraController?.startStreaming();
                  },
                ),
                TextButton(
                  child: const Text('Stop Streaming'),
                  onPressed: () {
                    cameraController?.stopStreaming();
                  },
                ),
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
                        child: images[0] == '' 
                          ? const Text('Take Picture') 
                          : Image.file(File(images[0])),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
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
                        child: Text(
                          cameraController?.isRecording == true
                            ? 'Stop Recording'
                            : 'Record Video'
                        ),
                      ),
                      Expanded(
                        child: Text("Video path: $videoPath"),
                      ),
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

  Future<void> takePicture(int i) async {
    try {
      String path = await cameraController!.takePicture();
      setState(() {
        images[i] = path;
      });
      showCustomToast('Picture saved to: $path');
    } catch (e) {
      showCustomToast('Failed to take picture: $e');
    }
  }

  Future<void> captureVideo(int i) async {
    try {
      if (cameraController!.isRecording) {
        // 停止录制
        String? path = await cameraController!.stopRecording();
        if (path != null) {
          setState(() {
            videoPath = path;
          });
          showCustomToast('Video saved to: $path');
        }
      } else {
        // 开始录制
        await cameraController!.startRecording();
        showCustomToast('Recording started');
      }
    } catch (e) {
      showCustomToast('Video recording error: $e');
    }
  }
}
