import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_uvc_camera/flutter_uvc_camera.dart';

class UVCCameraView extends StatefulWidget {
  final UVCCameraController cameraController;
  final double width;
  final double height;
  final UVCCameraViewParamsEntity? params;
  final bool autoDispose;

  const UVCCameraView({
    super.key,
    required this.cameraController,
    required this.width,
    required this.height,
    this.params,
    this.autoDispose = false,
  });

  @override
  State<UVCCameraView> createState() => _UVCCameraViewState();
}

class _UVCCameraViewState extends State<UVCCameraView> {
  @override
  void dispose() {
    if (widget.autoDispose) {
      widget.cameraController.closeCamera();
      widget.cameraController.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: widget.width,
      height: widget.height,
      child: AndroidView(
        viewType: 'uvc_camera_view',
        creationParams: widget.params?.toMap(),
        creationParamsCodec: const StandardMessageCodec(),
        onPlatformViewCreated: (id) {
          Future.microtask(() {
            widget.cameraController.initializeCamera();
          });
        },
      ),
    );
  }
}
