part of flutter_uvc_camera;

/// Canonical [StateEvent.state] values emitted on [EventChannel]
/// `flutter_uvc_camera/video_stream`. Prefer listening to [UVCCameraController.onStateEvent]
/// for all native→Dart status instead of relying on [MethodChannel] ad‑hoc calls.
class CameraPluginStates {
  CameraPluginStates._();

  /// PlatformView created; MethodChannel calls may proceed.
  static const String viewReady = 'VIEW_READY';

  static const String opening = 'OPENING';
  static const String opened = 'OPENED';
  static const String closing = 'CLOSING';
  static const String closed = 'CLOSED';
  static const String error = 'ERROR';

  /// From [UVCCameraView] / plugin (replaces legacy `callFlutter` MethodChannel).
  static const String pluginMessage = 'PLUGIN_MESSAGE';

  /// Hardware button capture path (replaces legacy `takePictureSuccess` MethodChannel).
  static const String takePictureSuccess = 'TAKE_PICTURE_SUCCESS';

  static const String streamStarted = 'STREAM_STARTED';
  static const String streamStopped = 'STREAM_STOPPED';
  static const String streamStats = 'STREAM_STATS';
  static const String renderFps = 'RENDER_FPS';

  static const String recordingTime = 'RECORDING_TIME';
}
