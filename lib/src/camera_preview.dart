import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'camera_controller.dart';

class UVCCameraPreview extends StatefulWidget {
  final CameraController controller;
  final Widget? overlay;
  final bool showControls;
  final double? aspectRatio;
  final BoxFit fit;
  final Color backgroundColor;
  final Widget Function(BuildContext, String)? errorBuilder;
  final Widget Function(BuildContext)? loadingBuilder;

  const UVCCameraPreview({
    Key? key,
    required this.controller,
    this.overlay,
    this.showControls = true,
    this.aspectRatio,
    this.fit = BoxFit.contain,
    this.backgroundColor = Colors.black,
    this.errorBuilder,
    this.loadingBuilder,
  }) : super(key: key);

  @override
  State<UVCCameraPreview> createState() => _UVCCameraPreviewState();
}

class _UVCCameraPreviewState extends State<UVCCameraPreview> {
  String? _error;
  bool _isRecording = false;

  @override
  void initState() {
    super.initState();
    widget.controller.onError.listen((error) {
      setState(() => _error = error);
    });
  }

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: Container(
        color: widget.backgroundColor,
        child: Stack(
          fit: StackFit.expand,
          children: [
            // Camera Preview
            _buildCameraPreview(),
            
            // Error Display
            if (_error != null && widget.errorBuilder != null)
              widget.errorBuilder!(context, _error!),
              
            // Loading Indicator
            if (widget.controller.state == CameraState.opening && widget.loadingBuilder != null)
              widget.loadingBuilder!(context),
              
            // Controls Overlay
            if (widget.showControls) _buildControls(),
            
            // Custom Overlay
            if (widget.overlay != null) widget.overlay!,
          ],
        ),
      ),
    );
  }

  Widget _buildCameraPreview() {
    return AspectRatio(
      aspectRatio: widget.aspectRatio ?? 4/3,
      child: Stack(
        fit: StackFit.expand,
        children: [
          const AndroidView(
            viewType: 'flutter_uvc_camera',
            creationParams: null,
            creationParamsCodec: StandardMessageCodec(),
          ),
          if (_error != null && widget.errorBuilder == null)
            Center(
              child: Text(
                _error!,
                style: const TextStyle(color: Colors.white),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildControls() {
    return Positioned(
      bottom: 16,
      left: 0,
      right: 0,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          // Capture Photo Button
          IconButton(
            icon: const Icon(Icons.camera_alt, color: Colors.white, size: 32),
            onPressed: _capturePhoto,
          ),
          
          // Record Video Button
          IconButton(
            icon: Icon(
              _isRecording ? Icons.stop : Icons.videocam,
              color: _isRecording ? Colors.red : Colors.white,
              size: 32,
            ),
            onPressed: _toggleRecording,
          ),
        ],
      ),
    );
  }

  Future<void> _capturePhoto() async {
    try {
      final path = await widget.controller.takePicture();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Photo saved to: $path')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error taking photo: $e')),
        );
      }
    }
  }

  Future<void> _toggleRecording() async {
    try {
      if (_isRecording) {
        await widget.controller.stopRecording();
      } else {
        await widget.controller.startRecording();
      }
      setState(() => _isRecording = !_isRecording);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error recording video: $e')),
        );
      }
    }
  }
} 