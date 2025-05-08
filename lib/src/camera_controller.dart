import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_uvc_camera/src/models/camera_config.dart';

enum CameraState { uninitialized, initialized, opening, opened, error, closed }

class CameraController {
  final MethodChannel _channel;
  final EventChannel _frameEventChannel;
  final EventChannel _streamEventChannel;

  CameraState _state = CameraState.uninitialized;
  StreamController<CameraState>? _stateStreamController;
  StreamController<String>? _errorStreamController;
  StreamController<Map<String, dynamic>>? _encodeDataStreamController;
  StreamController<Map<String, dynamic>>? _frameStreamController;
  StreamController<Map<String, dynamic>>? _logStreamController;
  StreamController<String>? _captureStateStreamController;
  StreamController<String>? _recordingStateStreamController;
  StreamController<String>? _streamStateStreamController;

  CameraConfig _config = const CameraConfig();
  bool _isRecording = false;
  bool _isStreaming = false;
  bool _frameStreamingEnabled = false;

  // 视频帧和流数据的监听器
  StreamSubscription? _frameEventSubscription;
  StreamSubscription? _streamEventSubscription;

  CameraController({CameraConfig? config})
      : _channel = const MethodChannel('flutter_uvc_camera/channel'),
        _frameEventChannel =
            const EventChannel('flutter_uvc_camera/frame_events'),
        _streamEventChannel =
            const EventChannel('flutter_uvc_camera/stream_events') {
    if (config != null) {
      _config = config;
    }
    _initializeControllers();
    _setupMethodCallHandler();
    _setupEventChannels();
  }

  // Public getters
  Stream<CameraState> get onStateChanged => _stateStreamController!.stream;
  Stream<String> get onError => _errorStreamController!.stream;
  Stream<Map<String, dynamic>> get onEncodeData =>
      _encodeDataStreamController!.stream;
  Stream<Map<String, dynamic>> get onFrameAvailable =>
      _frameStreamController!.stream;
  Stream<Map<String, dynamic>> get onLog => _logStreamController!.stream;
  Stream<String> get onCaptureStateChanged =>
      _captureStateStreamController!.stream;
  Stream<String> get onRecordingStateChanged =>
      _recordingStateStreamController!.stream;
  Stream<String> get onStreamStateChanged =>
      _streamStateStreamController!.stream;

  CameraState get state => _state;
  CameraConfig get config => _config;
  bool get isInitialized => _state != CameraState.uninitialized;
  bool get isOpened => _state == CameraState.opened;
  bool get isRecording => _isRecording;
  bool get isStreaming => _isStreaming;
  bool get isFrameStreamingEnabled => _frameStreamingEnabled;

  void _initializeControllers() {
    _stateStreamController = StreamController<CameraState>.broadcast();
    _errorStreamController = StreamController<String>.broadcast();
    _encodeDataStreamController =
        StreamController<Map<String, dynamic>>.broadcast();
    _frameStreamController = StreamController<Map<String, dynamic>>.broadcast();
    _logStreamController = StreamController<Map<String, dynamic>>.broadcast();
    _captureStateStreamController = StreamController<String>.broadcast();
    _recordingStateStreamController = StreamController<String>.broadcast();
    _streamStateStreamController = StreamController<String>.broadcast();
  }

  void _setupEventChannels() {
    // 设置视频帧事件监听
    _frameEventSubscription =
        _frameEventChannel.receiveBroadcastStream().listen((dynamic event) {
      if (event is Map) {
        final frameData = Map<String, dynamic>.from(event);
        _frameStreamController?.add(frameData);
      }
    }, onError: (dynamic error) {
      final errorDetails = error as PlatformException;
      _errorStreamController
          ?.add('Frame stream error: ${errorDetails.message}');
    });

    // 设置流数据事件监听
    _streamEventSubscription =
        _streamEventChannel.receiveBroadcastStream().listen((dynamic event) {
      if (event is Map) {
        final streamData = Map<String, dynamic>.from(event);

        // 处理不同类型的流事件
        if (streamData.containsKey('type')) {
          // 编码数据事件
          _encodeDataStreamController?.add(streamData);
        } else if (streamData.containsKey('event')) {
          // 状态变更事件
          final eventType = streamData['event'] as String;

          switch (eventType) {
            case 'recordingState':
              final state = streamData['state'] as String;
              _isRecording = state == 'started';
              _recordingStateStreamController?.add(state);
              break;

            case 'streamState':
              final state = streamData['state'] as String;
              _isStreaming = state == 'started';
              _streamStateStreamController?.add(state);
              break;

            case 'opened':
            case 'closed':
              // 这些状态已通过MethodChannel处理
              break;
          }
        }
      }
    }, onError: (dynamic error) {
      final errorDetails = error as PlatformException;
      _errorStreamController?.add('Stream error: ${errorDetails.message}');
    });
  }

  Future<void> _setupMethodCallHandler() async {
    _channel.setMethodCallHandler((call) async {
      try {
        switch (call.method) {
          case 'CameraState':
            _handleCameraState(call.arguments as String);
            break;
          case 'callFlutter':
            _handleCallFlutter(call.arguments as Map);
            break;
          case 'logMessage':
            _handleLogMessage(call.arguments as Map);
            break;
          case 'captureState':
            _handleCaptureState(call.arguments as String);
            break;
          // 这些事件现在通过EventChannel处理，不再需要MethodChannel处理
          // case 'onEncodeData':
          // case 'onFrameAvailable':
          // case 'videoRecordingState':
          // case 'streamState':
          default:
            debugPrint('未处理的方法调用: ${call.method}');
        }
      } catch (e) {
        _errorStreamController?.add('Error handling platform message: $e');
      }
    });
  }

  void _handleCameraState(String state) {
    if (state.startsWith('ERROR:')) {
      _state = CameraState.error;
      _errorStreamController?.add(state.substring(6));
    } else {
      switch (state) {
        case 'OPENED':
          _state = CameraState.opened;
          break;
        case 'CLOSED':
          _state = CameraState.closed;
          _isRecording = false;
          _isStreaming = false;
          _frameStreamingEnabled = false;
          break;
        default:
          _state = CameraState.error;
          _errorStreamController?.add('Unknown state: $state');
      }
    }
    _stateStreamController?.add(_state);
  }

  void _handleCallFlutter(Map arguments) {
    final type = arguments['type'] as String?;
    final msg = arguments['msg'] as String?;

    if (type == 'onError') {
      _errorStreamController?.add(msg ?? 'Unknown error');
    }
  }

  void _handleLogMessage(Map arguments) {
    _logStreamController?.add(Map<String, dynamic>.from(arguments));
  }

  void _handleCaptureState(String state) {
    _captureStateStreamController?.add(state);
  }

  // Camera Control Methods
  Future<void> initialize() async {
    try {
      await _channel.invokeMethod('initCamera');
      _state = CameraState.initialized;
      _stateStreamController?.add(_state);
    } catch (e) {
      _state = CameraState.error;
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<void> open() async {
    if (!isInitialized) {
      throw CameraException('Camera not initialized');
    }
    try {
      _state = CameraState.opening;
      _stateStreamController?.add(_state);
      await _channel.invokeMethod('openUVCCamera');
    } catch (e) {
      _state = CameraState.error;
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<void> close() async {
    try {
      await _channel.invokeMethod('closeCamera');
    } catch (e) {
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<String> takePicture([String? path]) async {
    if (!isOpened) {
      throw CameraException('Camera not opened');
    }
    try {
      final result = await _channel.invokeMethod('takePicture', {
        'path': path,
      });
      return result as String;
    } catch (e) {
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<void> startRecording([String? path]) async {
    if (!isOpened) {
      throw CameraException('Camera not opened');
    }
    if (_isRecording) {
      throw CameraException('Recording already in progress');
    }
    try {
      await _channel.invokeMethod('captureVideo', {
        'path': path,
      });
      // 状态更新将由事件通道通知
    } catch (e) {
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<String?> stopRecording() async {
    if (!_isRecording) {
      return null;
    }
    try {
      final result = await _channel.invokeMethod('captureVideoStop');
      // 状态更新将由事件通道通知
      return result as String?;
    } catch (e) {
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<void> startStreaming() async {
    if (!isOpened) {
      throw CameraException('Camera not opened');
    }
    try {
      await _channel.invokeMethod('captureStreamStart');
      // 状态更新将由事件通道通知
    } catch (e) {
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<void> stopStreaming() async {
    if (!_isStreaming) {
      return;
    }
    try {
      await _channel.invokeMethod('captureStreamStop');
      // 状态更新将由事件通道通知
    } catch (e) {
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<void> startFrameStreaming() async {
    if (!isOpened) {
      throw CameraException('Camera not opened');
    }
    try {
      await _channel.invokeMethod('startFrameStreaming');
      _frameStreamingEnabled = true;
    } catch (e) {
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<void> stopFrameStreaming() async {
    if (!_frameStreamingEnabled) {
      return;
    }
    try {
      await _channel.invokeMethod('stopFrameStreaming');
      _frameStreamingEnabled = false;
    } catch (e) {
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<List<Map<String, dynamic>>> getPreviewSizes() async {
    try {
      final String? sizesJson =
          await _channel.invokeMethod('getAllPreviewSizes');
      if (sizesJson == null) return [];
      final List<dynamic> sizes = json.decode(sizesJson);
      return sizes.cast<Map<String, dynamic>>();
    } catch (e) {
      _errorStreamController?.add(e.toString());
      return [];
    }
  }

  Future<void> updateResolution(int width, int height) async {
    try {
      await _channel.invokeMethod('updateResolution', {
        'width': width,
        'height': height,
      });
      _config = _config.copyWith(
        previewWidth: width,
        previewHeight: height,
      );
    } catch (e) {
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  Future<Map<String, dynamic>> getCameraInfo() async {
    try {
      final result = await _channel.invokeMethod('getCameraInfo');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      _errorStreamController?.add(e.toString());
      return {};
    }
  }

  Future<bool> isCameraOpened() async {
    try {
      final result = await _channel.invokeMethod('isCameraOpened');
      return result as bool;
    } catch (e) {
      _errorStreamController?.add(e.toString());
      return false;
    }
  }

  Future<void> setDebugMode(bool enabled) async {
    try {
      await _channel.invokeMethod('setDebugMode', {
        'enabled': enabled,
      });
    } catch (e) {
      _errorStreamController?.add(e.toString());
      rethrow;
    }
  }

  void dispose() {
    // 取消事件通道订阅
    _frameEventSubscription?.cancel();
    _streamEventSubscription?.cancel();

    // 关闭所有流控制器
    _stateStreamController?.close();
    _errorStreamController?.close();
    _encodeDataStreamController?.close();
    _frameStreamController?.close();
    _logStreamController?.close();
    _captureStateStreamController?.close();
    _recordingStateStreamController?.close();
    _streamStateStreamController?.close();
  }
}

class CameraException implements Exception {
  final String message;
  CameraException(this.message);

  @override
  String toString() => 'CameraException: $message';
}
