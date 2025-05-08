import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/flutter_uvc_camera.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'UVC Camera Example',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        useMaterial3: true,
      ),
      home: const CameraPage(),
    );
  }
}

class CameraPage extends StatefulWidget {
  const CameraPage({Key? key}) : super(key: key);

  @override
  State<CameraPage> createState() => _CameraPageState();
}

class _CameraPageState extends State<CameraPage> {
  late CameraController _controller;
  String? _error;
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    _initializeCamera();
  }

  Future<void> _initializeCamera() async {
    _controller = CameraController();

    // Listen for camera state changes
    _controller.onStateChanged.listen((state) {
      if (mounted) {
        setState(() {
          _isInitialized = state == CameraState.opened;
        });
      }
    });

    // Listen for errors
    _controller.onError.listen((error) {
      if (mounted) {
        setState(() => _error = error);
      }
    });

    try {
      await _controller.initialize();
      await _controller.open();
    } catch (e) {
      setState(() => _error = e.toString());
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
        title: const Text('UVC Camera Example'),
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
            child: UVCCameraPreview(
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
          ),
          if (!_isInitialized)
            const Padding(
              padding: EdgeInsets.all(16.0),
              child: Text(
                'Waiting for camera...',
                style: TextStyle(fontSize: 16),
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
}

class SettingsSheet extends StatelessWidget {
  final CameraController controller;
  final List<PreviewSize> previewSizes;

  const SettingsSheet({
    Key? key,
    required this.controller,
    required this.previewSizes,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Camera Settings',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 16),
          const Text('Preview Size'),
          const SizedBox(height: 8),
          SizedBox(
            height: 50,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              itemCount: previewSizes.length,
              itemBuilder: (context, index) {
                final size = previewSizes[index];
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ElevatedButton(
                    onPressed: () {
                      controller.updateResolution(size.width, size.height);
                      Navigator.pop(context);
                    },
                    child: Text('${size.width}x${size.height}'),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
