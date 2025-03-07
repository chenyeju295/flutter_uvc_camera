import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/flutter_uvc_camera.dart';
import 'package:path_provider/path_provider.dart';

import 'main.dart';

class CameraTest extends StatefulWidget {
  const CameraTest({Key? key}) : super(key: key);

  @override
  State<CameraTest> createState() => _CameraTestState();
}

class _CameraTestState extends State<CameraTest> {
  late CameraController _controller;
  bool _isRecording = false;
  String? _lastPhotoPath;
  String? _lastVideoPath;
  String? _error;

  @override
  void initState() {
    super.initState();
    _initializeCamera();
  }

  Future<void> _initializeCamera() async {
    _controller = CameraController();

    // Listen for errors
    _controller.onError.listen((error) {
      if (mounted) {
        setState(() => _error = error);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: $error')),
        );
      }
    });

    try {
      await _controller.initialize();
      await _controller.open();
    } catch (e) {
      setState(() => _error = e.toString());
    }
  }

  Future<void> _takePicture() async {
    try {
      final directory = await getTemporaryDirectory();
      final String filePath =
          '${directory.path}/${DateTime.now().millisecondsSinceEpoch}.jpg';

      await _controller.takePicture(filePath);
      setState(() => _lastPhotoPath = filePath);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Photo captured successfully')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to take picture: ${e.toString()}')),
        );
      }
    }
  }

  Future<void> _toggleRecording() async {
    if (_isRecording) {
      try {
        final videoPath = await _controller.stopRecording();
        setState(() {
          _isRecording = false;
          _lastVideoPath = videoPath;
        });

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Video recording stopped')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
                content: Text('Failed to stop recording: ${e.toString()}')),
          );
        }
      }
    } else {
      try {
        final directory = await getTemporaryDirectory();
        final String filePath =
            '${directory.path}/${DateTime.now().millisecondsSinceEpoch}.mp4';

        await _controller.startRecording(filePath);
        setState(() => _isRecording = true);

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Video recording started')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
                content: Text('Failed to start recording: ${e.toString()}')),
          );
        }
      }
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Camera Test'),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: _showSettings,
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: Stack(
              children: [
                UVCCameraPreview(
                  controller: _controller,
                  errorBuilder: (context, error) => Center(
                    child: Text(
                      error,
                      style: const TextStyle(color: Colors.red),
                    ),
                  ),
                  loadingBuilder: (context) => const Center(
                    child: CircularProgressIndicator(),
                  ),
                ),
                if (_isRecording)
                  const Positioned(
                    top: 16,
                    right: 16,
                    child: RecordingIndicator(),
                  ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                IconButton(
                  icon: const Icon(Icons.camera_alt),
                  onPressed: _takePicture,
                  tooltip: 'Take Picture',
                ),
                IconButton(
                  icon: Icon(_isRecording ? Icons.stop : Icons.videocam),
                  onPressed: _toggleRecording,
                  tooltip: _isRecording ? 'Stop Recording' : 'Start Recording',
                ),
                if (_lastPhotoPath != null)
                  IconButton(
                    icon: const Icon(Icons.photo),
                    onPressed: () => _showMediaPreview(_lastPhotoPath!, true),
                    tooltip: 'View Last Photo',
                  ),
                if (_lastVideoPath != null)
                  IconButton(
                    icon: const Icon(Icons.video_library),
                    onPressed: () => _showMediaPreview(_lastVideoPath!, false),
                    tooltip: 'View Last Video',
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _showSettings() async {
    final sizes = await _controller.getPreviewSizes();
    if (!mounted) return;

    showModalBottomSheet(
      context: context,
      builder: (context) => SettingsSheet(
        controller: _controller,
        previewSizes: sizes
            .map((size) =>
                PreviewSize(size['width'] as int, size['height'] as int))
            .toList(),
      ),
    );
  }

  void _showMediaPreview(String path, bool isPhoto) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => MediaPreviewPage(
          filePath: path,
          isPhoto: isPhoto,
        ),
      ),
    );
  }
}

class RecordingIndicator extends StatefulWidget {
  const RecordingIndicator({Key? key}) : super(key: key);

  @override
  State<RecordingIndicator> createState() => _RecordingIndicatorState();
}

class _RecordingIndicatorState extends State<RecordingIndicator>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(seconds: 1),
      vsync: this,
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        return Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: Colors.red.withOpacity(_controller.value),
          ),
        );
      },
    );
  }
}

class MediaPreviewPage extends StatelessWidget {
  final String filePath;
  final bool isPhoto;

  const MediaPreviewPage({
    Key? key,
    required this.filePath,
    required this.isPhoto,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(isPhoto ? 'Photo Preview' : 'Video Preview'),
      ),
      body: Center(
        child: isPhoto
            ? Image.file(File(filePath))
            : const Text(
                'Video preview not implemented'), // TODO: Implement video preview
      ),
    );
  }
}
