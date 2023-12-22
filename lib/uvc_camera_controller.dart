import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

enum CameraState { opened, closed, error }

class UVCCameraController {
  static const String _channelName = "com.chenyeju.flutter_uvc_camera/channel";
  static CameraState _cameraState = CameraState.closed;
  CameraState get getCameraState => _cameraState;
  static String _cameraErrorMsg = '';
  String get getCameraErrorMsg => _cameraErrorMsg;
  static String _takePicturePath = '';
  String get getTakePicturePath => _takePicturePath;
  static final List<String> _callStrings = [];
  List<String> get getCallStrings => _callStrings;
  static final List<dynamic> _devicesList = [];
  List<dynamic> get devicesList => _devicesList;
  static final List<dynamic> _allPreviewSize = [];
  List<dynamic> get allPreviewSize => _allPreviewSize;

  final MethodChannel _cameraChannel = const MethodChannel(_channelName)..setMethodCallHandler(_methodChannelHandler);

  ///接收来自Android的消息
  static Future<void> _methodChannelHandler(MethodCall call) async {
    switch (call.method) {
      case "callFlutter":
        debugPrint('------> 收到来自Android的消息：${call.arguments}');
        _callStrings.add(call.arguments.toString());
        break;
      case "takePictureSuccess":
        _takePictureSuccess(call.arguments.toString());
        break;
      case "CameraState":
        _setCameraState(call.arguments.toString());
        break;
      case "getDevicesList":
        _devicesList.addAll(call.arguments);
        break;
      case "getAllPreviewSize":
        _allPreviewSize.addAll(call.arguments);
    }
  }

  Future<void> initializeCamera({required double width, required double height}) async {
    await _cameraChannel
        .invokeMethod('initializeCamera', {"width": width, "height": height, "aspectRatio": width / height});
  }

  Future<void> setCameraPreviewSize({required double width, required double height}) async {
    await _cameraChannel.invokeMethod('initializeCamera', {"width": width, "height": height});
  }

  Future<void> connectToUsbDevice() async {
    try {
      final result = await _cameraChannel.invokeMethod('connectToUsbDevice');
      print(result);
    } on PlatformException catch (e) {
      print("Failed to connect to USB device: '${e.message}'.");
    }
  }

  Future<void> writeToDevice(int data) async {
    try {
      final result = await _cameraChannel.invokeMethod('writeToDevice', data);
      print(result);
    } on PlatformException catch (e) {
      print("Failed to write to USB device: '${e.message}'.");
    }
  }

  void startCamera() async {
    try {
      await _cameraChannel.invokeMethod('startCamera');
    } on PlatformException catch (e) {
      // 处理异常
      print(e);
    }
  }

  void getAllPreviewSize() async {
    try {
      await _cameraChannel.invokeMethod('getAllPreviewSize');
    } on PlatformException catch (e) {
      // 处理异常
      print(e);
    }
  }

  void getDevicesList() async {
    try {
      await _cameraChannel.invokeMethod('getDevicesList');
    } on PlatformException catch (e) {
      // 处理异常
      print(e);
    }
  }

  void takePicture() async {
    try {
      await _cameraChannel.invokeMethod('takePicture');
    } on PlatformException catch (e) {
      // 处理异常
      print(e);
    }
  }

  static void _setCameraState(String state) {
    debugPrint("Camera: $state");
    switch (state) {
      case "OPENED":
        _cameraState = CameraState.opened;
        break;
      case "CLOSED":
        _cameraState = CameraState.closed;
        break;
      default:
        if (state.contains("ERROR")) {
          _cameraState = CameraState.error;
          _cameraErrorMsg = state;
          debugPrint("Camera$state");
        }
        break;
    }
  }

  static void _takePictureSuccess(String result) {
    _takePicturePath = result;
    debugPrint("拍照$result");
  }

  void readFromDevice(int counter) {
    _cameraChannel.invokeMethod('readFromDevice', counter);
  }
}
