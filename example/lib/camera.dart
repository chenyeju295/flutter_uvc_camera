import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/flutter_uvc_camera.dart';

class CameraTest extends StatefulWidget {
  const CameraTest({super.key});

  @override
  State<CameraTest> createState() => _CameraTestState();
}

class _CameraTestState extends State<CameraTest> {
  List<String> images = ['', '', '', '', '', '', ''];
  String errText = '';
  late UVCCameraController cameraController;
  String videoPath = '';
  bool isRecording = false;
  String recordingTime = "00:00:00";

  @override
  void initState() {
    super.initState();
    cameraController = UVCCameraController();
    cameraController.msgCallback = (state) {
      showCustomToast(state);
    };

    // 添加录制时间回调
    cameraController.onRecordingTimeCallback = (timeEvent) {
      setState(() {
        recordingTime = timeEvent.formattedTime;

        // 如果收到了最终的录制时间更新，那么录制已结束
        if (timeEvent.isFinal && isRecording) {
          isRecording = false;
        }
      });
    };
  }

  void showCustomToast(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(16),
        duration: const Duration(seconds: 2),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(10),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('USB Camera Test'),
        backgroundColor: Theme.of(context).colorScheme.primaryContainer,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            if (errText.isNotEmpty)
              Container(
                margin: const EdgeInsets.only(bottom: 16),
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.red.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.red.shade200),
                ),
                child: Text(
                  errText,
                  style: const TextStyle(color: Colors.red),
                ),
              ),

            // Camera controls
            _buildSectionTitle('Camera Controls'),
            _buildControlSection(
              children: [
                _buildControlButton(
                  'Open Camera',
                  Icons.camera,
                  Colors.green,
                  () => cameraController.openUVCCamera(),
                ),
                SizedBox(height: 16),
                _buildControlButton(
                  'Close Camera',
                  Icons.camera_outlined,
                  Colors.red,
                  () => cameraController.closeCamera(),
                ),
              ],
            ),

            // Camera preview
            if (cameraController != null) _buildCameraPreview(),

            // Camera settings
            _buildSectionTitle('Camera Settings'),
            _buildControlSection(
              children: [
                _buildControlButton(
                  'Update Resolution',
                  Icons.settings_overscan,
                  Colors.blue,
                  () async {
                    await cameraController.getAllPreviewSizes();
                    cameraController
                        .updateResolution(PreviewSize(width: 352, height: 288));
                  },
                ),
                SizedBox(height: 16),
                _buildControlButton(
                  'Get Parameters',
                  Icons.settings,
                  Colors.purple,
                  () {
                    cameraController
                        ?.getCurrentCameraRequestParameters()
                        .then((value) => showCustomToast(value.toString()));
                  },
                ),
              ],
            ),

            // Stream control
            _buildSectionTitle('Stream Control'),
            _buildControlButton(
              'Start Capture Stream',
              Icons.play_circle_outline,
              Colors.amber,
              () => cameraController.captureStreamStart(),
              fullWidth: true,
            ),

            // Media capture
            _buildSectionTitle('Media Capture'),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                _buildMediaCaptureCard(
                  'Take Photo',
                  Icons.photo_camera,
                  Colors.green.shade100,
                  Colors.green,
                  () => takePicture(0),
                  previewPath: images[0],
                ),
                const SizedBox(width: 16),
                _buildMediaCaptureCard(
                  isRecording ? 'Stop Recording' : 'Record Video',
                  isRecording ? Icons.stop : Icons.videocam,
                  isRecording ? Colors.red.shade100 : Colors.blue.shade100,
                  isRecording ? Colors.red : Colors.blue,
                  () => captureVideo(1),
                  showRecording: isRecording,
                  recordingTime: recordingTime,
                ),
              ],
            ),

            if (videoPath.isNotEmpty)
              Container(
                margin: const EdgeInsets.only(top: 12),
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.blue.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.blue.shade200),
                ),
                child: Text(
                  'Video saved at: $videoPath',
                  style: const TextStyle(fontSize: 14),
                ),
              ),

            const SizedBox(height: 100),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.symmetric(vertical: 16),
      child: Text(
        title,
        style: const TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildControlSection({required List<Widget> children}) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            Column(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: children,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildControlButton(
    String label,
    IconData icon,
    Color color,
    VoidCallback onPressed, {
    bool fullWidth = false,
  }) {
    return SizedBox(
      width: fullWidth ? double.infinity : null,
      child: ElevatedButton.icon(
        onPressed: onPressed,
        icon: Icon(icon, color: color),
        label: Text(label),
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.white,
          foregroundColor: Colors.black87,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        ),
      ),
    );
  }

  Widget _buildCameraPreview() {
    return Stack(
      children: [
        Container(
          margin: const EdgeInsets.symmetric(vertical: 16),
          decoration: BoxDecoration(
            color: Colors.black,
            borderRadius: BorderRadius.circular(12),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.2),
                spreadRadius: 1,
                blurRadius: 5,
                offset: const Offset(0, 3),
              ),
            ],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: UVCCameraView(
              cameraController: cameraController!,
              params: const UVCCameraViewParamsEntity(frameFormat: 0),
              width: 300,
              height: 300,
            ),
          ),
        ),

        // 录制状态指示器
        if (isRecording)
          Positioned(
            bottom: 30,
            left: 0,
            right: 0,
            child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 30),
              padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
              decoration: BoxDecoration(
                color: Colors.red.withOpacity(0.8),
                borderRadius: BorderRadius.circular(24),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    width: 12,
                    height: 12,
                    decoration: BoxDecoration(
                      color: Colors.white,
                      shape: BoxShape.circle,
                      border: Border.all(color: Colors.red.shade700, width: 2),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Text(
                    'REC $recordingTime',
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildMediaCaptureCard(
    String label,
    IconData icon,
    Color bgColor,
    Color iconColor,
    VoidCallback onTap, {
    String? previewPath,
    bool showRecording = false,
    String recordingTime = '',
  }) {
    return Expanded(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          height: 120,
          decoration: BoxDecoration(
            color: bgColor,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.grey.shade300),
          ),
          child: previewPath != null && previewPath.isNotEmpty
              ? ClipRRect(
                  borderRadius: BorderRadius.circular(11),
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      Image.file(
                        File(previewPath),
                        fit: BoxFit.cover,
                      ),
                      Positioned(
                        bottom: 0,
                        left: 0,
                        right: 0,
                        child: Container(
                          color: Colors.black54,
                          padding: const EdgeInsets.symmetric(vertical: 4),
                          child: Text(
                            label,
                            textAlign: TextAlign.center,
                            style: const TextStyle(color: Colors.white),
                          ),
                        ),
                      ),
                    ],
                  ),
                )
              : Stack(
                  children: [
                    Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(icon, size: 48, color: iconColor),
                        const SizedBox(height: 8),
                        Text(
                          label,
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: iconColor,
                          ),
                        ),
                        if (showRecording)
                          Padding(
                            padding: const EdgeInsets.only(top: 8.0),
                            child: Text(
                              recordingTime,
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                color: Colors.red,
                              ),
                            ),
                          ),
                      ],
                    ),
                    if (showRecording)
                      Positioned(
                        top: 8,
                        right: 8,
                        child: Container(
                          width: 12,
                          height: 12,
                          decoration: BoxDecoration(
                            color: Colors.red,
                            shape: BoxShape.circle,
                            border: Border.all(color: Colors.white, width: 1),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withOpacity(0.2),
                                blurRadius: 2,
                              ),
                            ],
                          ),
                        ),
                      ),
                  ],
                ),
        ),
      ),
    );
  }

  takePicture(int i) async {
    String? path = await cameraController.takePicture();
    if (path != null) {
      images[i] = path;
      setState(() {});
      showCustomToast('Photo saved successfully');
    }
  }

  captureVideo(int i) async {
    if (isRecording) {
      // 如果正在录制，则停止录制
      String? path = await cameraController.captureVideo();
      if (path != null) {
        videoPath = path;
        setState(() {
          isRecording = false;
        });
        showCustomToast('Video saved successfully');
      }
    } else {
      // 开始录制
      setState(() {
        isRecording = true;
        recordingTime = "00:00:00"; // 重置录制时间
      });
      await cameraController.captureVideo();
    }
  }
}
