import 'package:flutter/cupertino.dart';
import 'package:flutter_uvc_camera/uvc_camera_controller.dart';

class UVCCameraView extends StatefulWidget {
  const UVCCameraView({super.key});

  @override
  State<UVCCameraView> createState() => _UVCCameraViewState();
}

class _UVCCameraViewState extends State<UVCCameraView> {
  UVCCameraController cameraController = UVCCameraController();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: AndroidView(
          viewType: 'uvc_camera_view',
          onPlatformViewCreated: (id) {
            cameraController.initializeCamera();
          }),
    );
  }
}
