part of flutter_uvc_camera;

/// 视频流事件基类
abstract class VideoStreamEvent {
  final String type;

  VideoStreamEvent(this.type);

  factory VideoStreamEvent.fromMap(Map<dynamic, dynamic> map) {
    final type = map['type']?.toString() ?? 'UNKNOWN';

    switch (type) {
      case 'H264':
      case 'AAC':
        return VideoFrameEvent.fromMap(map);
      case 'STATE':
        final state = map['state']?.toString() ?? 'UNKNOWN';
        if (state == 'STREAM_STATS') {
          return StreamStatsEvent.fromMap(map);
        }
        return StateEvent.fromMap(map);
      default:
        return UnknownEvent(type);
    }
  }
}

/// 视频帧事件
class VideoFrameEvent extends VideoStreamEvent {
  final Uint8List data;
  final int timestamp;
  final int size;
  final int fps;
  final bool isKeyFrame;

  VideoFrameEvent({
    required String type,
    required this.data,
    required this.timestamp,
    required this.size,
    required this.fps,
    required this.isKeyFrame,
  }) : super(type);

  factory VideoFrameEvent.fromMap(Map<dynamic, dynamic> map) {
    final rawData = map['data'];
    final data = rawData is Uint8List ? rawData : Uint8List(0);
    final timestamp = _parseIntOrZero(map['timestamp']);
    final size = _parseIntOrZero(map['size']);
    final fps = _parseIntOrZero(map['fps']);
    final isKeyFrame = map['isKeyFrame'] == true;
    return VideoFrameEvent(
      type: map['type']?.toString() ?? 'UNKNOWN',
      data: data,
      timestamp: timestamp,
      size: size,
      fps: fps,
      isKeyFrame: isKeyFrame,
    );
  }
}

/// 状态事件
class StateEvent extends VideoStreamEvent {
  final String state;
  final Map<String, dynamic>? data;

  StateEvent({
    required this.state,
    this.data,
  }) : super('STATE');

  factory StateEvent.fromMap(Map<dynamic, dynamic> map) {
    final state = map['state']?.toString() ?? 'UNKNOWN';

    // 提取其他数据
    final rawData = Map<String, dynamic>.from(map);
    rawData.remove('type');
    rawData.remove('state');

    return StateEvent(
      state: state,
      data: rawData.isNotEmpty ? rawData : null,
    );
  }
}

/// 实时流统计事件
class StreamStatsEvent extends StateEvent {
  final int totalVideoFrames;
  final int totalAudioFrames;
  final int droppedVideoFrames;
  final int droppedAudioFrames;
  final int videoFps;

  StreamStatsEvent({
    required this.totalVideoFrames,
    required this.totalAudioFrames,
    required this.droppedVideoFrames,
    required this.droppedAudioFrames,
    required this.videoFps,
    Map<String, dynamic>? data,
  }) : super(state: 'STREAM_STATS', data: data);

  factory StreamStatsEvent.fromMap(Map<dynamic, dynamic> map) {
    final rawData = Map<String, dynamic>.from(map);
    rawData.remove('type');
    rawData.remove('state');
    return StreamStatsEvent(
      totalVideoFrames: _parseIntOrZero(map['totalVideoFrames']),
      totalAudioFrames: _parseIntOrZero(map['totalAudioFrames']),
      droppedVideoFrames: _parseIntOrZero(map['droppedVideoFrames']),
      droppedAudioFrames: _parseIntOrZero(map['droppedAudioFrames']),
      videoFps: _parseIntOrZero(map['videoFps']),
      data: rawData.isNotEmpty ? rawData : null,
    );
  }

  double get videoDropRate =>
      totalVideoFrames <= 0 ? 0 : droppedVideoFrames / totalVideoFrames;
  double get audioDropRate =>
      totalAudioFrames <= 0 ? 0 : droppedAudioFrames / totalAudioFrames;
}

/// 录制计时状态
class RecordingTimeEvent {
  final int elapsedMillis;
  final String formattedTime;
  final bool isFinal;

  RecordingTimeEvent({
    required this.elapsedMillis,
    required this.formattedTime,
    required this.isFinal,
  });

  factory RecordingTimeEvent.fromStateEvent(StateEvent event) {
    if (event.data == null) {
      return RecordingTimeEvent(
        elapsedMillis: 0,
        formattedTime: '00:00:00',
        isFinal: false,
      );
    }

    return RecordingTimeEvent(
      elapsedMillis: _parseIntOrZero(event.data!['elapsedMillis']),
      formattedTime: event.data!['formattedTime']?.toString() ?? '00:00:00',
      isFinal: event.data!['isFinal'] == true,
    );
  }
}

/// 未知事件
class UnknownEvent extends VideoStreamEvent {
  UnknownEvent(String type) : super(type);
}

int _parseIntOrZero(dynamic value) {
  if (value is int) return value;
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '') ?? 0;
}
