import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/flutter_uvc_camera.dart';

class StreamsDemo extends StatefulWidget {
  const StreamsDemo({super.key});

  @override
  State<StreamsDemo> createState() => _StreamsDemoState();
}

class _StreamsDemoState extends State<StreamsDemo> {
  UVCCameraController? cameraController;
  bool isCameraOpen = false;
  bool isStreaming = false;

  // Streaming stats
  int videoFramesReceived = 0;
  int audioFramesReceived = 0;
  int lastVideoFrameSize = 0;
  String recordingTime = "00:00:00";
  String streamState = "Not started";

  // FPS calculation
  int _framesInLastSecond = 0;
  int _currentFps = 0;
  DateTime? _lastFpsUpdateTime;

  @override
  void initState() {
    super.initState();
    cameraController = UVCCameraController();
    setupListeners();
  }

  void setupListeners() {
    cameraController?.msgCallback = (state) {
      showCustomToast(state);
    };

    cameraController?.cameraStateCallback = (state) {
      setState(() {
        isCameraOpen = state == UVCCameraState.opened;
      });
    };

    cameraController?.onVideoFrameCallback = (frame) {
      setState(() {
        videoFramesReceived++;
        lastVideoFrameSize = frame.size;
        _updateFps();
      });
    };

    cameraController?.onAudioFrameCallback = (frame) {
      setState(() {
        audioFramesReceived++;
      });
    };

    cameraController?.onRecordingTimeCallback = (timeEvent) {
      setState(() {
        recordingTime = timeEvent.formattedTime;
      });
    };

    cameraController?.onStreamStateCallback = (stateEvent) {
      setState(() {
        streamState = stateEvent.state;

        if (stateEvent.state == 'STARTED') {
          isStreaming = true;
        } else if (stateEvent.state == 'STOPPED') {
          isStreaming = false;
        }
      });
    };
  }

  void _updateFps() {
    if (_lastFpsUpdateTime == null) {
      _lastFpsUpdateTime = DateTime.now();
      _framesInLastSecond = 1;
      return;
    }

    final now = DateTime.now();
    final difference = now.difference(_lastFpsUpdateTime!);

    if (difference.inMilliseconds >= 1000) {
      setState(() {
        _currentFps = _framesInLastSecond;
        _framesInLastSecond = 1;
        _lastFpsUpdateTime = now;
      });
    } else {
      _framesInLastSecond++;
    }
  }

  @override
  void dispose() {
    cameraController?.closeCamera();
    cameraController?.dispose();
    super.dispose();
  }

  void showCustomToast(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(16),
        duration: const Duration(seconds: 2),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(10),
        ),
      ),
    );
  }

  void toggleStreaming() {
    if (isStreaming) {
      cameraController?.captureStreamStop();
    } else {
      cameraController?.captureStreamStart();
    }
  }

  Future<void> captureVideo() async {
    final path = await cameraController?.captureVideo();
    if (path != null) {
      showCustomToast('Video saved to: $path');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Camera Streaming'),
        backgroundColor: Theme.of(context).colorScheme.primaryContainer,
      ),
      body: Column(
        children: [
          // Camera preview
          if (cameraController != null && isCameraOpen)
            Container(
              margin: const EdgeInsets.all(16),
              height: 250,
              decoration: BoxDecoration(
                color: Colors.black,
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.2),
                    spreadRadius: 1,
                    blurRadius: 5,
                    offset: const Offset(0, 3),
                  ),
                ],
              ),
              child: Stack(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(16),
                    child: UVCCameraView(
                      cameraController: cameraController!,
                      params: const UVCCameraViewParamsEntity(frameFormat: 0),
                      width: MediaQuery.of(context).size.width - 32,
                      height: 250,
                    ),
                  ),
                  Positioned(
                    top: 10,
                    right: 10,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.black54,
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        '$_currentFps FPS',
                        style: const TextStyle(color: Colors.white),
                      ),
                    ),
                  ),
                  if (isStreaming)
                    Positioned(
                      top: 10,
                      left: 10,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.red,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Container(
                              width: 8,
                              height: 8,
                              decoration: const BoxDecoration(
                                color: Colors.white,
                                shape: BoxShape.circle,
                              ),
                            ),
                            const SizedBox(width: 4),
                            const Text(
                              'LIVE',
                              style: TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 12,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                ],
              ),
            )
          else
            Container(
              margin: const EdgeInsets.all(16),
              height: 250,
              decoration: BoxDecoration(
                color: Colors.grey.shade900,
                borderRadius: BorderRadius.circular(16),
              ),
              child: const Center(
                child: Text(
                  'Camera not open',
                  style: TextStyle(color: Colors.white),
                ),
              ),
            ),

          // Camera open/close buttons
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: isCameraOpen
                        ? null
                        : () => cameraController?.openUVCCamera(),
                    icon: const Icon(Icons.camera),
                    label: const Text('Open Camera'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.green,
                      foregroundColor: Colors.white,
                      disabledBackgroundColor: Colors.grey.shade300,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: isCameraOpen
                        ? () => cameraController?.closeCamera()
                        : null,
                    icon: const Icon(Icons.close),
                    label: const Text('Close Camera'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white,
                      disabledBackgroundColor: Colors.grey.shade300,
                    ),
                  ),
                ),
              ],
            ),
          ),

          // Streaming controls
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                ElevatedButton.icon(
                  onPressed: isCameraOpen ? toggleStreaming : null,
                  icon: Icon(isStreaming ? Icons.stop : Icons.play_arrow),
                  label:
                      Text(isStreaming ? 'Stop Streaming' : 'Start Streaming'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: isStreaming ? Colors.red : Colors.blue,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 12),
                  ),
                ),
                const SizedBox(height: 8),
                ElevatedButton.icon(
                  onPressed:
                      (isCameraOpen && isStreaming) ? captureVideo : null,
                  icon: const Icon(Icons.videocam),
                  label: const Text('Record Video'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.orange,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 12),
                  ),
                ),
              ],
            ),
          ),

          // Streaming stats
          Expanded(
            child: Container(
              margin: const EdgeInsets.all(16),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.grey.shade100,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: Colors.grey.shade300),
              ),
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Streaming Statistics',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    _buildStatRow('Stream State', streamState),
                    _buildStatRow('Current FPS', _currentFps.toString()),
                    _buildStatRow(
                        'Video Frames', videoFramesReceived.toString()),
                    _buildStatRow(
                        'Audio Frames', audioFramesReceived.toString()),
                    _buildStatRow(
                        'Last Frame Size', '$lastVideoFrameSize bytes'),
                    _buildStatRow('Recording Time', recordingTime),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: TextStyle(
              fontSize: 16,
              color: Colors.grey.shade800,
            ),
          ),
          Text(
            value,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }
}
