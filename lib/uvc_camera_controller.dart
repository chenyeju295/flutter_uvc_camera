import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

enum UVCCameraState { opened, closed, error }

class UVCCameraController {
  static const String _channelName = "flutter_uvc_camera/channel";
  UVCCameraState _cameraState = UVCCameraState.closed;
  Function(UVCCameraState)? cameraStateCallback;
  Function(String)? clickTakePictureButtonCallback;
  UVCCameraState get getCameraState => _cameraState;
  String _cameraErrorMsg = '';
  String get getCameraErrorMsg => _cameraErrorMsg;
  String _takePicturePath = '';
  String get getTakePicturePath => _takePicturePath;
  final List<String> _callStrings = [];
  List<String> get getCallStrings => _callStrings;
  Function(String)? msgCallback;

  MethodChannel? _cameraChannel;

  UVCCameraController() {
    _cameraChannel = const MethodChannel(_channelName);
    _cameraChannel?.setMethodCallHandler(_methodChannelHandler);
    debugPrint("------> UVCCameraController init");
  }

  void dispose() {
    _cameraChannel?.setMethodCallHandler(null);
    _cameraChannel = null;
    debugPrint("------> UVCCameraController dispose");
  }

  ///接收来自Android的消息
  Future<void> _methodChannelHandler(MethodCall call) async {
    switch (call.method) {
      case "callFlutter":
        debugPrint('------> 收到来自Android的消息：${call.arguments}');
        _callStrings.add(call.arguments.toString());
        msgCallback?.call(call.arguments['msg']);

        break;
      case "takePictureSuccess":
        _takePictureSuccess(call.arguments);
        break;
      case "CameraState":
        _setCameraState(call.arguments.toString());
        break;
    }
  }

  Future<void> initializeCamera() async {
    await _cameraChannel?.invokeMethod('initializeCamera');
  }

  Future<void> openUVCCamera() async {
    debugPrint("openUVCCamera");
    await _cameraChannel?.invokeMethod('openUVCCamera');
  }

  // Future<void> writeToDevice(int data) async {
  //   if (_cameraState == UVCCameraState.opened) {
  //     final result = await _cameraChannel?.invokeMethod('writeToDevice', data);
  //     debugPrint(result.toString());
  //   }
  // }

  void startCamera() async {
    await _cameraChannel?.invokeMethod('startCamera');
  }

  Future<String?> takePicture() async {
    String? path = await _cameraChannel?.invokeMethod('takePicture');
    debugPrint("拍照$path");
    return path;
  }

  void _setCameraState(String state) {
    debugPrint("Camera: $state");
    switch (state) {
      case "OPENED":
        _cameraState = UVCCameraState.opened;
        cameraStateCallback?.call(UVCCameraState.opened);
        break;
      case "CLOSED":
        _cameraState = UVCCameraState.closed;
        cameraStateCallback?.call(UVCCameraState.closed);
        break;
      default:
        if (state.contains("ERROR")) {
          _cameraState = UVCCameraState.error;
          _cameraErrorMsg = state;
          cameraStateCallback?.call(UVCCameraState.error);
          msgCallback?.call(state);
        }
        break;
    }
  }

  void _takePictureSuccess(String? result) {
    if (result != null) {
      _takePicturePath = result;
      clickTakePictureButtonCallback?.call(result);
    }
    debugPrint("拍照$result");
  }

  void closeCamera() {
    _cameraChannel?.invokeMethod('closeCamera');
  }
}
