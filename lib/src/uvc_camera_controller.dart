import 'dart:convert';
import 'dart:async';
import 'dart:typed_data';

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
  Function(StreamStatsEvent)? onStreamStatsCallback;

  // 当前录制时间，单位毫秒
  int _currentRecordingTimeMs = 0;

  /// 获取当前录制时间（毫秒）
  int get currentRecordingTimeMs => _currentRecordingTimeMs;

  // 当前录制时间格式化字符串
  String _currentRecordingTimeFormatted = "00:00:00";

  /// 获取当前录制时间格式化字符串
  String get currentRecordingTimeFormatted => _currentRecordingTimeFormatted;

  /// Whether recording is active
  bool get isRecording => _isRecording;

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
  final Completer<void> _viewReadyCompleter = Completer<void>();
  int _streamErrorCount = 0;
  bool _isRecording = false;
  bool _autoAdaptEnabled = false;
  int _autoAdaptMinVideoFps = 10;
  int _autoAdaptMaxVideoFps = 30;
  int _autoAdaptCurrentVideoSizeLimit = 0;
  int _autoAdaptMinVideoSizeLimit = 256 * 1024;
  int _autoAdaptMaxVideoSizeLimit = 2 * 1024 * 1024;
  DateTime _lastAutoAdaptAt = DateTime.fromMillisecondsSinceEpoch(0);

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
    if (event == null || event is! Map) return;

    try {
      final videoEvent =
          VideoStreamEvent.fromMap(Map<dynamic, dynamic>.from(event));
      // Use microtask to avoid blocking the main thread
      Future.microtask(() => _dispatchVideoStreamEvent(videoEvent));
    } catch (e) {
      debugPrint("Error parsing video stream event: $e");
    }
  }

  void _dispatchVideoStreamEvent(VideoStreamEvent videoEvent) {
    try {
      if (videoEvent is VideoFrameEvent) {
        _handleVideoFrameEvent(videoEvent);
      } else if (videoEvent is StateEvent) {
        _handleStateEvent(videoEvent);
      }
      _streamErrorCount = 0;
    } catch (e) {
      debugPrint("Error processing video event: $e");
    }
  }

  void _handleVideoFrameEvent(VideoFrameEvent videoEvent) {
    if (videoEvent.type == 'H264') {
      onVideoFrameCallback?.call(videoEvent);
      return;
    }
    if (videoEvent.type == 'AAC') {
      onAudioFrameCallback?.call(videoEvent);
      return;
    }
  }

  void _handleStateEvent(StateEvent videoEvent) {
    if (videoEvent.state == 'RECORDING_TIME') {
      final recordingEvent = RecordingTimeEvent.fromStateEvent(videoEvent);
      _currentRecordingTimeMs = recordingEvent.elapsedMillis;
      _currentRecordingTimeFormatted = recordingEvent.formattedTime;
      _isRecording = !recordingEvent.isFinal;
      onRecordingTimeCallback?.call(recordingEvent);
      return;
    }

    if (videoEvent is StreamStatsEvent) {
      onStreamStatsCallback?.call(videoEvent);
      _handleAutoAdaptByStreamStats(videoEvent);
      onStreamStateCallback?.call(videoEvent);
      return;
    }

    if (['OPENED', 'CLOSED', 'ERROR', 'OPENING', 'CLOSING']
        .contains(videoEvent.state)) {
      final msg = videoEvent.data?['msg'] as String?;
      final stateStr =
          msg != null ? "${videoEvent.state}:$msg" : videoEvent.state;
      _setCameraState(stateStr);
      return;
    }

    onStreamStateCallback?.call(videoEvent);
  }

  /// 启用/配置自动自适应（音频优先）
  void enableAutoAdaptiveStreaming({
    int minVideoFps = 10,
    int maxVideoFps = 30,
    int minVideoSizeLimit = 256 * 1024,
    int maxVideoSizeLimit = 2 * 1024 * 1024,
  }) {
    _autoAdaptEnabled = true;
    _autoAdaptMinVideoFps = minVideoFps.clamp(1, 60);
    _autoAdaptMaxVideoFps = maxVideoFps.clamp(_autoAdaptMinVideoFps, 60);
    _autoAdaptMinVideoSizeLimit =
        minVideoSizeLimit.clamp(64 * 1024, 8 * 1024 * 1024);
    _autoAdaptMaxVideoSizeLimit =
        maxVideoSizeLimit.clamp(_autoAdaptMinVideoSizeLimit, 16 * 1024 * 1024);
    _autoAdaptCurrentVideoSizeLimit = 0;
  }

  void disableAutoAdaptiveStreaming() {
    _autoAdaptEnabled = false;
  }

  void _handleAutoAdaptByStreamStats(StreamStatsEvent stats) {
    if (!_autoAdaptEnabled) return;
    final now = DateTime.now();
    if (now.difference(_lastAutoAdaptAt).inMilliseconds < 1500) return;

    Future.microtask(() async {
      try {
        // 音频优先：只调整视频参数
        if (stats.audioDropRate > 0.02 || stats.videoDropRate > 0.20) {
          final currentFps =
              await getVideoFrameRateLimit() ?? _autoAdaptMaxVideoFps;
          if (currentFps > _autoAdaptMinVideoFps) {
            final nextFps = (currentFps * 0.85)
                .round()
                .clamp(_autoAdaptMinVideoFps, _autoAdaptMaxVideoFps);
            await setVideoFrameRateLimit(nextFps);
            _lastAutoAdaptAt = now;
            return;
          }

          // 帧率已经到下限，再限制视频帧大小，降低编码压力
          final currentLimit = _autoAdaptCurrentVideoSizeLimit <= 0
              ? _autoAdaptMaxVideoSizeLimit
              : _autoAdaptCurrentVideoSizeLimit;
          final nextLimit = (currentLimit * 0.8)
              .round()
              .clamp(_autoAdaptMinVideoSizeLimit, _autoAdaptMaxVideoSizeLimit);
          if (nextLimit != currentLimit) {
            await setVideoFrameSizeLimit(nextLimit);
            _autoAdaptCurrentVideoSizeLimit = nextLimit;
            _lastAutoAdaptAt = now;
          }
          return;
        }

        // 状态恢复：逐步回升视频帧率
        if (stats.videoDropRate < 0.03 &&
            stats.audioDropRate == 0 &&
            stats.videoFps >= 12) {
          final currentFps =
              await getVideoFrameRateLimit() ?? _autoAdaptMaxVideoFps;
          if (currentFps < _autoAdaptMaxVideoFps) {
            final nextFps = (currentFps + 2)
                .clamp(_autoAdaptMinVideoFps, _autoAdaptMaxVideoFps);
            await setVideoFrameRateLimit(nextFps);
            _lastAutoAdaptAt = now;
          }
        }
      } catch (e) {
        debugPrint("Auto-adaptive streaming update failed: $e");
      }
    });
  }

  /// 处理视频流错误
  void _handleVideoStreamError(dynamic error) {
    // Count consecutive errors to implement exponential backoff if needed
    debugPrint("Video stream error: $error");
    _streamErrorCount += 1;
    _reconnectVideoStreamChannel();

    // If error involves buffer access issues, we might need to reduce frame rate
    if (error.toString().contains("buffer is inaccessible")) {
      _reduceFrameRate();
    }
  }

  void _reconnectVideoStreamChannel() {
    _videoStreamSubscription?.cancel();
    _videoStreamSubscription = null;
    final retryDelayMs = (_streamErrorCount * 500).clamp(500, 5000);
    Future.delayed(Duration(milliseconds: retryDelayMs), () {
      _initVideoStreamChannel();
    });
  }

  /// 自动降低帧率以应对性能问题
  void _reduceFrameRate() async {
    try {
      // Get current frame rate limit - default to 30 if not yet configured
      final currentFps =
          await _methodChannel?.invokeMethod('getVideoFrameRateLimit') ?? 30;

      // Only reduce if frame rate is above minimum threshold (15 fps)
      if (currentFps > 15) {
        final newFps = (currentFps * 0.8).round(); // Reduce by 20%
        debugPrint(
            "Automatically reducing frame rate from $currentFps to $newFps due to buffer issues");
        await setVideoFrameRateLimit(newFps);
      }
    } catch (e) {
      debugPrint("Error during frame rate reduction: $e");
    }
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
        if (_callStrings.length > 200) {
          // Prevent unbounded growth during long sessions.
          _callStrings.removeAt(0);
        }
        final args = call.arguments;
        if (args is Map && args['msg'] is String) {
          msgCallback?.call(args['msg'] as String);
        }
        break;

      case "takePictureSuccess":
        if (call.arguments is String) {
          _takePictureSuccess(call.arguments as String);
        }
        break;

      case "CameraState":
        final state = call.arguments.toString();
        // Only handle VIEW_READY from MethodChannel as it's a critical initialization signal
        if (state == "VIEW_READY") {
          _setCameraState(state);
        }
        break;
    }
  }

  /// Initialize camera with better timing
  Future<void> initializeCamera() async {
    try {
      await _invokeMethodWhenReady('initializeCamera');
      debugPrint("Camera initialized successfully");
    } catch (e) {
      debugPrint("Error initializing camera: $e");
      // Retry once if failed
      await Future.delayed(const Duration(milliseconds: 300));
      try {
        await _invokeMethodWhenReady('initializeCamera');
        debugPrint("Camera initialized successfully on retry");
      } catch (e) {
        debugPrint("Error initializing camera on retry: $e");
      }
    }
  }

  /// Open UVC camera
  Future<void> openUVCCamera() async {
    debugPrint("openUVCCamera");
    try {
      await _invokeMethodWhenReady('openUVCCamera');
    } catch (e) {
      debugPrint("Error opening UVC camera: $e");
    }
  }

  /// Start capture stream
  Future<void> captureStreamStart() async {
    debugPrint("Starting camera stream");
    try {
      await _invokeMethodWhenReady('captureStreamStart');
    } catch (e) {
      debugPrint("Error starting capture stream: $e");
    }
  }

  /// Stop capture stream
  Future<void> captureStreamStop() async {
    debugPrint("Stopping camera stream");
    try {
      await _invokeMethodWhenReady('captureStreamStop');
    } catch (e) {
      debugPrint("Error stopping capture stream: $e");
    }
  }

  /// Start playing mic audio from UVC device (Android only)
  Future<bool> startPlayMic() async {
    try {
      final result = await _invokeMethodWhenReady<bool>('startPlayMic');
      return result == true;
    } catch (e) {
      debugPrint("Error starting mic playback: $e");
      return false;
    }
  }

  /// Stop playing mic audio from UVC device (Android only)
  Future<bool> stopPlayMic() async {
    try {
      final result = await _invokeMethodWhenReady<bool>('stopPlayMic');
      return result == true;
    } catch (e) {
      debugPrint("Error stopping mic playback: $e");
      return false;
    }
  }

  /// Start camera preview
  Future<void> startCamera() async {
    await openUVCCamera();
  }

  /// 设置视频帧率限制
  Future<void> setVideoFrameRateLimit(int fps) async {
    if (fps < 1 || fps > 60) {
      throw ArgumentError('帧率必须在1-60之间');
    }
    await _invokeMethodWhenReady('setVideoFrameRateLimit', {'fps': fps});
  }

  /// Get current video frame rate limit
  Future<int?> getVideoFrameRateLimit() async {
    try {
      final result = await _invokeMethodWhenReady('getVideoFrameRateLimit');
      return result is int ? result : null;
    } catch (e) {
      debugPrint("Error getting frame rate limit: $e");
      return null;
    }
  }

  /// 设置视频帧大小限制
  Future<void> setVideoFrameSizeLimit(int maxBytes) async {
    await _invokeMethodWhenReady('setVideoFrameSizeLimit', {'size': maxBytes});
  }

  /// 设置音频帧大小限制（单位字节，0表示不限制）
  Future<void> setAudioFrameSizeLimit(int maxBytes) async {
    await _invokeMethodWhenReady('setAudioFrameSizeLimit', {'size': maxBytes});
  }

  /// 获取当前音频帧大小限制
  Future<int?> getAudioFrameSizeLimit() async {
    try {
      final result = await _invokeMethodWhenReady('getAudioFrameSizeLimit');
      return result is int ? result : null;
    } catch (e) {
      debugPrint("Error getting audio frame size limit: $e");
      return null;
    }
  }

  /// Get all available preview sizes
  Future<List<PreviewSize>> getAllPreviewSizes() async {
    var result = await _invokeMethodWhenReady('getAllPreviewSizes');
    List<PreviewSize> list = [];
    if (result is String) {
      final decoded = json.decode(result);
      if (decoded is List) {
        for (final element in decoded) {
          list.add(PreviewSize.fromJson(element));
        }
        _previewSizes = list;
      }
    }
    return list;
  }

  /// Get current camera request parameters
  Future<String?> getCurrentCameraRequestParameters() async {
    return await _invokeMethodWhenReady('getCurrentCameraRequestParameters');
  }

  /// Update camera resolution
  Future<void> updateResolution(PreviewSize? previewSize) async {
    await _invokeMethodWhenReady('updateResolution', previewSize?.toMap());
  }

  /// Apply preview parameters at runtime (fps/format/bandwidth/size).
  ///
  /// On Android, if the camera is already opened and not streaming/recording,
  /// the native preview will be restarted so the new UVC setPreviewSize takes effect.
  Future<void> updateCameraViewParams(UVCCameraViewParamsEntity params) async {
    await _invokeMethodWhenReady(
      'updateCameraViewParams',
      params.toMap(),
    );
  }

  /// Take a picture
  Future<String?> takePicture() async {
    try {
      String? path = await _invokeMethodWhenReady('takePicture');
      debugPrint("path: $path");
      return path;
    } catch (e) {
      debugPrint("Error taking picture: $e");
      return null;
    }
  }

  /// Take a picture and return JPEG bytes
  Future<Uint8List?> takePictureBytes() async {
    try {
      final data = await _invokeMethodWhenReady('takePictureBytes');
      if (data is Uint8List) {
        return data;
      }
      return null;
    } catch (e) {
      debugPrint("Error taking picture bytes: $e");
      return null;
    }
  }

  /// Capture video
  Future<String?> captureVideo() async {
    // 重置录制计时
    _currentRecordingTimeMs = 0;
    _currentRecordingTimeFormatted = "00:00:00";

    try {
      String? path = await _invokeMethodWhenReady('captureVideo');
      debugPrint("path: $path");
      return path;
    } catch (e) {
      debugPrint("Error capturing video: $e");
      return null;
    }
  }

  /// Stop video recording (native toggles capture on/off)
  Future<String?> stopVideo() async {
    return captureVideo();
  }

  /// Set camera feature value
  Future<bool> setCameraFeature(String feature, int value) async {
    try {
      final result = await _invokeMethodWhenReady('setCameraFeature', {
        'feature': feature,
        'value': value,
      });
      return result == true;
    } catch (e) {
      debugPrint("Error setting camera feature $feature: $e");
      return false;
    }
  }

  /// Reset camera feature to default
  Future<bool> resetCameraFeature(String feature) async {
    try {
      final result = await _invokeMethodWhenReady('resetCameraFeature', {
        'feature': feature,
      });
      return result == true;
    } catch (e) {
      debugPrint("Error resetting camera feature $feature: $e");
      return false;
    }
  }

  /// Get camera feature value
  Future<int?> getCameraFeature(String feature) async {
    try {
      return await _invokeMethodWhenReady('getCameraFeature', {
        'feature': feature,
      });
    } catch (e) {
      debugPrint("Error getting camera feature $feature: $e");
      return null;
    }
  }

  /// Get all camera features
  Future<CameraFeatures?> getAllCameraFeatures() async {
    try {
      final result = await _invokeMethodWhenReady('getAllCameraFeatures');
      if (result is String) {
        final decoded = json.decode(result);
        if (decoded is Map<String, dynamic>) {
          final features = CameraFeatures.fromJson(decoded);
          _cameraFeatures = features;
          return features;
        }
      }
      return null;
    } catch (e) {
      debugPrint("Error getting camera features: $e");
      return null;
    }
  }

  /// Set auto focus
  Future<bool> setAutoFocus(bool enabled) async {
    return setCameraFeature('autofocus', enabled ? 1 : 0);
  }

  /// Set auto exposure
  Future<bool> setAutoExposure(bool enabled) async {
    return setCameraFeature('autoexposure', enabled ? 1 : 0);
  }

  /// Set exposure mode
  Future<bool> setExposureMode(int mode) async {
    return setCameraFeature('exposuremode', mode);
  }

  /// Set exposure priority
  Future<bool> setExposurePriority(int value) async {
    return setCameraFeature('exposurepriority', value);
  }

  /// Set exposure
  Future<bool> setExposure(int value) async {
    return setCameraFeature('exposure', value);
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
    if (state == "VIEW_READY") {
      if (!_viewReadyCompleter.isCompleted) {
        _viewReadyCompleter.complete();
      }
      return;
    }
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

  Future<void> _waitForViewReady() async {
    if (_viewReadyCompleter.isCompleted) {
      return;
    }
    try {
      await _viewReadyCompleter.future.timeout(const Duration(seconds: 2));
    } catch (_) {
      // Continue without blocking if the view-ready event is late or missing.
    }
  }

  Future<T?> _invokeMethodWhenReady<T>(String method,
      [dynamic arguments]) async {
    await _waitForViewReady();
    return _methodChannel?.invokeMethod<T>(method, arguments);
  }

  void _takePictureSuccess(String result) {
    _takePicturePath = result;
    clickTakePictureButtonCallback?.call(result);
  }

  /// Close the camera
  Future<void> closeCamera() async {
    try {
      await _invokeMethodWhenReady('closeCamera');
    } catch (e) {
      debugPrint("Error closing camera: $e");
    }
  }
}
