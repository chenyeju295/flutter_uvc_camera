part of flutter_uvc_camera;

/// Camera features for UVC cameras
class CameraFeatures {
  final bool? autoFocus;
  final bool? autoWhiteBalance;
  final int? zoom;
  final int? brightness;
  final int? contrast;
  final int? saturation;
  final int? sharpness;
  final int? gain;
  final int? gamma;
  final int? hue;

  CameraFeatures({
    this.autoFocus,
    this.autoWhiteBalance,
    this.zoom,
    this.brightness,
    this.contrast,
    this.saturation,
    this.sharpness,
    this.gain,
    this.gamma,
    this.hue,
  });

  factory CameraFeatures.fromJson(Map<String, dynamic> json) {
    return CameraFeatures(
      autoFocus: json['autoFocus'],
      autoWhiteBalance: json['autoWhiteBalance'],
      zoom: json['zoom'],
      brightness: json['brightness'],
      contrast: json['contrast'],
      saturation: json['saturation'],
      sharpness: json['sharpness'],
      gain: json['gain'],
      gamma: json['gamma'],
      hue: json['hue'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'autoFocus': autoFocus,
      'autoWhiteBalance': autoWhiteBalance,
      'zoom': zoom,
      'brightness': brightness,
      'contrast': contrast,
      'saturation': saturation,
      'sharpness': sharpness,
      'gain': gain,
      'gamma': gamma,
      'hue': hue,
    };
  }

  @override
  String toString() {
    return 'CameraFeatures(autoFocus: $autoFocus, autoWhiteBalance: $autoWhiteBalance, '
        'zoom: $zoom, brightness: $brightness, contrast: $contrast, '
        'saturation: $saturation, sharpness: $sharpness, gain: $gain, '
        'gamma: $gamma, hue: $hue)';
  }
}
