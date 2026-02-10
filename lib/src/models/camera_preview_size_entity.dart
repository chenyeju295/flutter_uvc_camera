part of flutter_uvc_camera;

class PreviewSize {
  int? width;
  int? height;
  PreviewSize({this.width, this.height});

  Map<String, dynamic> toMap() {
    return {"width": width, "height": height};
  }

  PreviewSize.fromJson(dynamic json) {
    width = _parseIntOrNull(json?["width"]);
    height = _parseIntOrNull(json?["height"]);
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
