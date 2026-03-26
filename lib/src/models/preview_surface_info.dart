part of flutter_uvc_camera;

/// Preview surface rectangle info inside the AndroidView.
///
/// This is useful for drawing overlays aligned with the actual image area,
/// because the preview view may be measured smaller than the parent due to aspect ratio.
class PreviewSurfaceInfo {
  final int? containerWidthPx;
  final int? containerHeightPx;
  final int? surfaceWidthPx;
  final int? surfaceHeightPx;

  /// offset and size ratios relative to the container (0..1).
  final double offsetLeftRatio;
  final double offsetTopRatio;
  final double surfaceWidthRatio;
  final double surfaceHeightRatio;

  const PreviewSurfaceInfo({
    this.containerWidthPx,
    this.containerHeightPx,
    this.surfaceWidthPx,
    this.surfaceHeightPx,
    required this.offsetLeftRatio,
    required this.offsetTopRatio,
    required this.surfaceWidthRatio,
    required this.surfaceHeightRatio,
  });

  factory PreviewSurfaceInfo.fromMap(Map<dynamic, dynamic> map) {
    double asDouble(dynamic v) {
      if (v is double) return v;
      if (v is num) return v.toDouble();
      return double.tryParse(v?.toString() ?? '') ?? 0.0;
    }

    int? asInt(dynamic v) {
      if (v is int) return v;
      if (v is num) return v.toInt();
      return int.tryParse(v?.toString() ?? '');
    }

    return PreviewSurfaceInfo(
      containerWidthPx: asInt(map['containerWidthPx']),
      containerHeightPx: asInt(map['containerHeightPx']),
      surfaceWidthPx: asInt(map['surfaceWidthPx']),
      surfaceHeightPx: asInt(map['surfaceHeightPx']),
      offsetLeftRatio: asDouble(map['offsetLeftRatio']),
      offsetTopRatio: asDouble(map['offsetTopRatio']),
      surfaceWidthRatio: asDouble(map['surfaceWidthRatio']),
      surfaceHeightRatio: asDouble(map['surfaceHeightRatio']),
    );
  }
}
