import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:flutter_uvc_camera/uvc_camera_controller.dart';

/// 自定义参数 可空  Custom parameters can be empty
class UVCCameraViewParamsEntity {
  final double? aspectRatio;

  /// usb产品ID 用于过滤识别设备 USB product ID used to filter device recognition
  final List<int>? productIds;

  /// usb厂商ID 用于过滤识别设备 USB vendor ID used to filter device recognition
  final List<int>? vendorIds;
  const UVCCameraViewParamsEntity(
      {this.aspectRatio, this.productIds, this.vendorIds});

  Map<String, dynamic> toMap() {
    return {
      "aspectRatio": aspectRatio,
      "productIds": productIds,
      "vendorIds": vendorIds
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
