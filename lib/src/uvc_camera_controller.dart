import 'dart:convert';
import 'dart:typed_data';
import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:flutter_uvc_camera/flutter_uvc_camera.dart';

/// Controller for UVC camera operations
class UVCCameraController {
  static const String _methodChannelName = "flutter_uvc_camera/channel";
  static const String _videoStreamChannelName =
      "flutter_uvc_camera/video_stream";

  UVCCameraState _cameraState = UVCCameraState.closed;

  /// Camera state callback
  Function(UVCCameraState)? cameraStateCallback;

  /// Photo capture button callback
  Function(String path)? clickTakePictureButtonCallback;

  /// Get current camera state
  UVCCameraState get getCameraState => _cameraState;

  String _cameraErrorMsg = '';

  /// Get camera error message
  String get getCameraErrorMsg => _cameraErrorMsg;

  String _takePicturePath = '';

  /// Get path of last captured picture
  String get getTakePicturePath => _takePicturePath;

  final List<String> _callStrings = [];

  /// Get call history
  List<String> get getCallStrings => _callStrings;

  /// Message callback
  Function(String)? msgCallback;

  /// Video frame callback
  Function(VideoFrameEvent)? onVideoFrameCallback;

  /// Audio frame callback
  Function(VideoFrameEvent)? onAudioFrameCallback;

  /// Recording time update callback
  Function(RecordingTimeEvent)? onRecordingTimeCallback;

  /// State change callback (stream started/stopped)
  Function(StateEvent)? onStreamStateCallback;

  // 当前录制时间，单位毫秒
  int _currentRecordingTimeMs = 0;

  /// 获取当前录制时间（毫秒）
  int get currentRecordingTimeMs => _currentRecordingTimeMs;

  // 当前录制时间格式化字符串
  String _currentRecordingTimeFormatted = "00:00:00";

  /// 获取当前录制时间格式化字符串
  String get currentRecordingTimeFormatted => _currentRecordingTimeFormatted;

  List<PreviewSize> _previewSizes = [];

  /// Get available preview sizes
  List<PreviewSize> get getPreviewSizes => _previewSizes;

  /// Camera features cache
  CameraFeatures? _cameraFeatures;

  /// Get camera features
  CameraFeatures? get cameraFeatures => _cameraFeatures;

  MethodChannel? _methodChannel;
  EventChannel? _videoStreamChannel;
  StreamSubscription? _videoStreamSubscription;

  /// Initialize controller
  UVCCameraController() {
    _methodChannel = const MethodChannel(_methodChannelName);
    _methodChannel?.setMethodCallHandler(_methodChannelHandler);

    _initVideoStreamChannel();

    debugPrint("------> UVCCameraController init");
  }

  /// 初始化视频流通道
  void _initVideoStreamChannel() {
    _videoStreamChannel = const EventChannel(_videoStreamChannelName);
    _videoStreamSubscription = _videoStreamChannel
        ?.receiveBroadcastStream()
        .listen(_handleVideoStreamEvent, onError: _handleVideoStreamError);
  }

  /// 处理视频流事件
  void _handleVideoStreamEvent(dynamic event) {
    if (event == null) return;

    try {
      final videoEvent = VideoStreamEvent.fromMap(event);

      // Use microtask to avoid blocking the main thread
      Future.microtask(() {
        if (videoEvent is VideoFrameEvent) {
          if (videoEvent.type == 'H264' && onVideoFrameCallback != null) {
            onVideoFrameCallback!(videoEvent);
          } else if (videoEvent.type == 'AAC' && onAudioFrameCallback != null) {
            onAudioFrameCallback!(videoEvent);
          }
        } else if (videoEvent is StateEvent) {
          if (videoEvent.state == 'RECORDING_TIME') {
            final recordingEvent =
                RecordingTimeEvent.fromStateEvent(videoEvent);
            _currentRecordingTimeMs = recordingEvent.elapsedMillis;
            _currentRecordingTimeFormatted = recordingEvent.formattedTime;

            if (onRecordingTimeCallback != null) {
              onRecordingTimeCallback!(recordingEvent);
            }
          } else if (onStreamStateCallback != null) {
            onStreamStateCallback!(videoEvent);
          }
        }
      });
    } catch (e) {
      debugPrint("Error handling video stream event: $e");
    }
  }

  /// 处理视频流错误
  void _handleVideoStreamError(dynamic error) {
    debugPrint("Video stream error: $error");
  }

  /// Dispose controller resources
  void dispose() {
    _videoStreamSubscription?.cancel();
    _videoStreamSubscription = null;

    _methodChannel?.setMethodCallHandler(null);
    _methodChannel = null;

    debugPrint("------> UVCCameraController dispose");
  }

  /// Handle method calls from platform
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
    }
  }

  /// Initialize camera with better timing
  Future<void> initializeCamera() async {
    // Give some time for the platform view to initialize
    await Future.delayed(const Duration(milliseconds: 100));

    try {
      await _methodChannel?.invokeMethod('initializeCamera');
      debugPrint("Camera initialized successfully");
    } catch (e) {
      debugPrint("Error initializing camera: $e");
      // Retry once if failed
      await Future.delayed(const Duration(milliseconds: 300));
      try {
        await _methodChannel?.invokeMethod('initializeCamera');
        debugPrint("Camera initialized successfully on retry");
      } catch (e) {
        debugPrint("Error initializing camera on retry: $e");
      }
    }
  }

  /// Open UVC camera
  Future<void> openUVCCamera() async {
    debugPrint("openUVCCamera");
    await _methodChannel?.invokeMethod('openUVCCamera');
  }

  /// Start capture stream
  void captureStreamStart() {
    _methodChannel?.invokeMethod('captureStreamStart');
  }

  /// Stop capture stream
  void captureStreamStop() {
    _methodChannel?.invokeMethod('captureStreamStop');
  }

  /// Start camera preview
  Future<void> startCamera() async {
    await _methodChannel?.invokeMethod('startCamera');
  }

  /// 设置视频帧率限制
  Future<void> setVideoFrameRateLimit(int fps) async {
    if (fps < 1 || fps > 60) {
      throw ArgumentError('帧率必须在1-60之间');
    }
    await _methodChannel?.invokeMethod('setVideoFrameRateLimit', {'fps': fps});
  }

  /// 设置视频帧大小限制
  Future<void> setVideoFrameSizeLimit(int maxBytes) async {
    await _methodChannel
        ?.invokeMethod('setVideoFrameSizeLimit', {'size': maxBytes});
  }

  /// Get all available preview sizes
  Future<List<PreviewSize>> getAllPreviewSizes() async {
    var result = await _methodChannel?.invokeMethod('getAllPreviewSizes');
    List<PreviewSize> list = [];
    if (result != null) {
      json.decode(result).forEach((element) {
        list.add(PreviewSize.fromJson(element));
      });
      _previewSizes = list;
    }
    return list;
  }

  /// Get current camera request parameters
  Future<String?> getCurrentCameraRequestParameters() async {
    return await _methodChannel
        ?.invokeMethod('getCurrentCameraRequestParameters');
  }

  /// Update camera resolution
  void updateResolution(PreviewSize? previewSize) {
    _methodChannel?.invokeMethod('updateResolution', previewSize?.toMap());
  }

  /// Take a picture
  Future<String?> takePicture() async {
    String? path = await _methodChannel?.invokeMethod('takePicture');
    debugPrint("path: $path");
    return path;
  }

  /// Capture video
  Future<String?> captureVideo() async {
    // 重置录制计时
    _currentRecordingTimeMs = 0;
    _currentRecordingTimeFormatted = "00:00:00";

    String? path = await _methodChannel?.invokeMethod('captureVideo');
    debugPrint("path: $path");
    return path;
  }

  /// Set camera feature value
  Future<bool> setCameraFeature(String feature, int value) async {
    final result = await _methodChannel?.invokeMethod('setCameraFeature', {
      'feature': feature,
      'value': value,
    });
    return result == true;
  }

  /// Reset camera feature to default
  Future<bool> resetCameraFeature(String feature) async {
    final result = await _methodChannel?.invokeMethod('resetCameraFeature', {
      'feature': feature,
    });
    return result == true;
  }

  /// Get camera feature value
  Future<int?> getCameraFeature(String feature) async {
    return await _methodChannel?.invokeMethod('getCameraFeature', {
      'feature': feature,
    });
  }

  /// Get all camera features
  Future<CameraFeatures?> getAllCameraFeatures() async {
    final result = await _methodChannel?.invokeMethod('getAllCameraFeatures');
    if (result != null) {
      final features = CameraFeatures.fromJson(json.decode(result));
      _cameraFeatures = features;
      return features;
    }
    return null;
  }

  /// Set auto focus
  Future<bool> setAutoFocus(bool enabled) async {
    return setCameraFeature('autofocus', enabled ? 1 : 0);
  }

  /// Set auto white balance
  Future<bool> setAutoWhiteBalance(bool enabled) async {
    return setCameraFeature('autowhitebalance', enabled ? 1 : 0);
  }

  /// Set camera zoom
  Future<bool> setZoom(int value) async {
    return setCameraFeature('zoom', value);
  }

  /// Set camera brightness
  Future<bool> setBrightness(int value) async {
    return setCameraFeature('brightness', value);
  }

  /// Set camera contrast
  Future<bool> setContrast(int value) async {
    return setCameraFeature('contrast', value);
  }

  /// Set camera saturation
  Future<bool> setSaturation(int value) async {
    return setCameraFeature('saturation', value);
  }

  /// Set camera sharpness
  Future<bool> setSharpness(int value) async {
    return setCameraFeature('sharpness', value);
  }

  /// Set camera gain
  Future<bool> setGain(int value) async {
    return setCameraFeature('gain', value);
  }

  /// Set camera gamma
  Future<bool> setGamma(int value) async {
    return setCameraFeature('gamma', value);
  }

  /// Set camera hue
  Future<bool> setHue(int value) async {
    return setCameraFeature('hue', value);
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

  /// Close the camera
  void closeCamera() {
    _methodChannel?.invokeMethod('closeCamera');
  }
}
