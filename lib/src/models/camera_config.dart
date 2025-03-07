class CameraConfig {
  final int minFps;
  final int maxFps;
  final int frameFormat;
  final double bandwidthFactor;
  final int previewWidth;
  final int previewHeight;

  const CameraConfig({
    this.minFps = 10,
    this.maxFps = 30,
    this.frameFormat = 6, // MJPEG by default
    this.bandwidthFactor = 1.0,
    this.previewWidth = 640,
    this.previewHeight = 480,
  });

  Map<String, dynamic> toMap() {
    return {
      'minFps': minFps,
      'maxFps': maxFps,
      'frameFormat': frameFormat,
      'bandwidthFactor': bandwidthFactor,
      'previewWidth': previewWidth,
      'previewHeight': previewHeight,
    };
  }

  CameraConfig copyWith({
    int? minFps,
    int? maxFps,
    int? frameFormat,
    double? bandwidthFactor,
    int? previewWidth,
    int? previewHeight,
  }) {
    return CameraConfig(
      minFps: minFps ?? this.minFps,
      maxFps: maxFps ?? this.maxFps,
      frameFormat: frameFormat ?? this.frameFormat,
      bandwidthFactor: bandwidthFactor ?? this.bandwidthFactor,
      previewWidth: previewWidth ?? this.previewWidth,
      previewHeight: previewHeight ?? this.previewHeight,
    );
  }
}

class PreviewSize {
  final int width;
  final int height;

  const PreviewSize(this.width, this.height);

  double get aspectRatio => width / height;

  @override
  String toString() => '$width x $height';

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is PreviewSize && 
           other.width == width && 
           other.height == height;
  }

  @override
  int get hashCode => width.hashCode ^ height.hashCode;
}

enum FrameFormat {
  yuyv(4),
  mjpeg(6);

  final int value;
  const FrameFormat(this.value);
} 