import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:flutter_uvc_camera/uvc_camera_controller.dart';

/// 自定义参数 可空  Custom parameters can be empty
class UVCCameraViewParamsEntity {
  /**
   *  if give custom minFps or maxFps or unsupported preview size
   *  set preview possible will fail
   *  **/
  /// camera preview min fps  10
  final int? minFps;

  /// camera preview max fps  60
  final int? maxFps;

  /// camera preview frame format 1 (MJPEG) or 0 (YUV)
  /// DEFAULT 1(MJPEG)  If preview fails and the screen goes black, please try switching to 0
  final int? frameFormat;

  ///  DEFAULT_BANDWIDTH = 1
  final double? bandwidthFactor;

  const UVCCameraViewParamsEntity({
    this.minFps = 10,
    this.maxFps = 60,
    this.bandwidthFactor = 1.0,
    this.frameFormat = 1,
  });

  Map<String, dynamic> toMap() {
    return {
      "minFps": minFps,
      "maxFps": maxFps,
      "frameFormat": frameFormat,
      "bandwidthFactor": bandwidthFactor
    };
  }
}

class UVCCameraView extends StatefulWidget {
  final UVCCameraController cameraController;
  final double width;
  final double height;
  final UVCCameraViewParamsEntity? params;
  const UVCCameraView(
      {super.key,
      required this.cameraController,
      required this.width,
      required this.height,
      this.params});

  @override
  State<UVCCameraView> createState() => _UVCCameraViewState();
}

class _UVCCameraViewState extends State<UVCCameraView> {
  @override
  void dispose() {
    widget.cameraController.closeCamera();
    widget.cameraController.dispose();
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
            widget.cameraController.initializeCamera();
          }),
    );
  }
}
