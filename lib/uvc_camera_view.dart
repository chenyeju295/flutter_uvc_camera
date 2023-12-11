import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:flutter_uvc_camera/uvc_camera_controller.dart';

class UVCCameraView extends StatefulWidget {
  final double width;
  final double height;

  const UVCCameraView({super.key, required this.width, required this.height});

  @override
  State<UVCCameraView> createState() => _UVCCameraViewState();
}

class _UVCCameraViewState extends State<UVCCameraView> {
  UVCCameraController cameraController = UVCCameraController();

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: widget.width,
      height: widget.height,
      child: AndroidView(
          viewType: 'uvc_camera_view',
          onPlatformViewCreated: (id) {
            cameraController.initializeCamera(width: widget.width, height: widget.height);
          }),
    );
  }
}
