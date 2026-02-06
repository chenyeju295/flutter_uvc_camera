part of flutter_uvc_camera;

/// 自定义参数 可空  Custom parameters can be empty
class UVCCameraViewParamsEntity {
  /**
   *  if give custom minFps or maxFps or unsupported preview size
   *  set preview possible will fail
   *  **/
  /// camera preview min fps  10
  final int? minFps;

  /// camera preview max fps  60
  final int? maxFps;

  /// camera preview frame format 1 (MJPEG) or 0 (YUV)
  /// DEFAULT 1(MJPEG)  If preview fails and the screen goes black, please try switching to 0
  final int? frameFormat;

  ///  DEFAULT_BANDWIDTH = 1
  final double? bandwidthFactor;

  /// preview width
  final int? previewWidth;

  /// preview height
  final int? previewHeight;

  /// whether to keep aspect ratio when rendering
  final bool? aspectRatioShow;

  /// capture raw image data
  final bool? captureRawImage;

  /// output raw preview data
  final bool? rawPreviewData;

  /// default rotation angle: 0, 90, 180, 270
  final int? rotateType;

  const UVCCameraViewParamsEntity({
    this.minFps = 10,
    this.maxFps = 60,
    this.bandwidthFactor = 1.0,
    this.frameFormat = 1,
    this.previewWidth,
    this.previewHeight,
    this.aspectRatioShow,
    this.captureRawImage,
    this.rawPreviewData,
    this.rotateType,
  });

  Map<String, dynamic> toMap() {
    return {
      "minFps": minFps,
      "maxFps": maxFps,
      "frameFormat": frameFormat,
      "bandwidthFactor": bandwidthFactor,
      "previewWidth": previewWidth,
      "previewHeight": previewHeight,
      "aspectRatioShow": aspectRatioShow,
      "captureRawImage": captureRawImage,
      "rawPreviewData": rawPreviewData,
      "rotateType": rotateType,
    };
  }
}
