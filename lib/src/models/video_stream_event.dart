part of flutter_uvc_camera;

/// 视频流事件基类
abstract class VideoStreamEvent {
  final String type;

  VideoStreamEvent(this.type);

  factory VideoStreamEvent.fromMap(Map<dynamic, dynamic> map) {
    final type = map['type'] as String;

    switch (type) {
      case 'H264':
      case 'AAC':
        return VideoFrameEvent.fromMap(map);
      case 'STATE':
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

  VideoFrameEvent({
    required String type,
    required this.data,
    required this.timestamp,
    required this.size,
    required this.fps,
  }) : super(type);

  factory VideoFrameEvent.fromMap(Map<dynamic, dynamic> map) {
    return VideoFrameEvent(
      type: map['type'] as String,
      data: map['data'] as Uint8List,
      timestamp: map['timestamp'] as int,
      size: map['size'] as int,
      fps: map['fps'] as int,
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
    final state = map['state'] as String;

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
      elapsedMillis: event.data!['elapsedMillis'] as int,
      formattedTime: event.data!['formattedTime'] as String,
      isFinal: event.data!['isFinal'] as bool,
    );
  }
}

/// 未知事件
class UnknownEvent extends VideoStreamEvent {
  UnknownEvent(String type) : super(type);
}
