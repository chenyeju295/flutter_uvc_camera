part of flutter_uvc_camera;

class PreviewSize {
  final int width;
  final int height;
  
  const PreviewSize(this.width, this.height);

  Map<String, dynamic> toMap() {
    return {"width": width, "height": height};
  }

  factory PreviewSize.fromJson(Map<String, dynamic> json) {
    return PreviewSize(
      json["width"] as int,
      json["height"] as int,
    );
  }

  double get aspectRatio => width / height;

  @override
  String toString() {
    return 'PreviewSize{width: $width, height: $height}';
  }
  
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
