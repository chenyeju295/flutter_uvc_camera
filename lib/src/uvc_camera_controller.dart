import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:flutter_uvc_camera/uvc_camera.dart';

class UVCCameraController {
  static const String _channelName = "flutter_uvc_camera/channel";

  UVCCameraState _cameraState = UVCCameraState.closed;

  /// 摄像头状态回调
  Function(UVCCameraState)? cameraStateCallback;

  /// 拍照按钮回调
  Function(String path)? clickTakePictureButtonCallback;
  UVCCameraState get getCameraState => _cameraState;
  String _cameraErrorMsg = '';
  String get getCameraErrorMsg => _cameraErrorMsg;
  String _takePicturePath = '';
  String get getTakePicturePath => _takePicturePath;
  final List<String> _callStrings = [];
  List<String> get getCallStrings => _callStrings;
  Function(String)? msgCallback;
  List<PreviewSize> _previewSizes = [];
  List<PreviewSize> get getPreviewSizes => _previewSizes;

  MethodChannel? _cameraChannel;

  ///初始化
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
        debugPrint('------> Received from Android：${call.arguments}');
        _callStrings.add(call.arguments.toString());
        msgCallback?.call(call.arguments['msg']);

        break;
      case "takePictureSuccess":
        _takePictureSuccess(call.arguments);
        break;
      case "CameraState":
        _setCameraState(call.arguments.toString());
        break;
      case "onEncodeData":
        final Map<dynamic, dynamic> args = call.arguments;
        // capture H264 & AAC only
        debugPrint(args.toString());
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

  void captureStreamStart() {
    _cameraChannel?.invokeMethod('captureStreamStart');
  }

  void captureStreamStop() {
    _cameraChannel?.invokeMethod('captureStreamStop');
  }

  void startCamera() async {
    await _cameraChannel?.invokeMethod('startCamera');
  }

  /// 获取全部预览大小
  Future getAllPreviewSizes() async {
    var result = await _cameraChannel?.invokeMethod('getAllPreviewSizes');
    List<PreviewSize> list = [];
    json.decode(result)?.forEach((element) {
      list.add(PreviewSize.fromJson(element));
    });
    _previewSizes = list;
  }

  /// 获取当前摄像头请求参数
  Future<String?> getCurrentCameraRequestParameters() async {
    return await _cameraChannel
        ?.invokeMethod('getCurrentCameraRequestParameters');
  }

  /// 更新预览大小
  void updateResolution(PreviewSize? previewSize) {
    _cameraChannel?.invokeMethod('updateResolution', previewSize?.toMap());
  }

  ///拍照
  Future<String?> takePicture() async {
    String? path = await _cameraChannel?.invokeMethod('takePicture');
    debugPrint("path: $path");
    return path;
  }

  ///录像
  Future<String?> captureVideo() async {
    String? path = await _cameraChannel?.invokeMethod('captureVideo');
    debugPrint("path: $path");
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
  }

  void closeCamera() {
    _cameraChannel?.invokeMethod('closeCamera');
  }
}
