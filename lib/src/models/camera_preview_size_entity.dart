part of flutter_uvc_camera;

class PreviewSize {
  final int width;
  final int height;
  const PreviewSize({this.width = 0, this.height = 0});

  Map<String, dynamic> toMap() {
    return {"width": width, "height": height};
  }

  factory PreviewSize.fromJson(dynamic json) {
    return PreviewSize(
      width: _parseIntOrNull(json?["width"]) ?? 0,
      height: _parseIntOrNull(json?["height"]) ?? 0,
    );
  }

  @override
  String toString() {
    return 'PreviewSize{width: $width, height: $height}';
  }
}

int? _parseIntOrNull(dynamic value) {
  if (value is int) return value;
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '');
}
