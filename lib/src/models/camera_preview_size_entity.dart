part of uvc_camera;

class PreviewSize {
  int? width;
  int? height;
  PreviewSize({this.width, this.height});

  Map<String, dynamic> toMap() {
    return {"width": width, "height": height};
  }

  PreviewSize.fromJson(dynamic json) {
    width = json["width"];
    height = json["height"];
  }

  @override
  String toString() {
    return 'PreviewSize{width: $width, height: $height}';
  }
}
