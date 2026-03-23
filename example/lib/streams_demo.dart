import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/flutter_uvc_camera.dart';

class StreamsDemo extends StatefulWidget {
  const StreamsDemo({super.key});

  @override
  State<StreamsDemo> createState() => _StreamsDemoState();
}

class _StreamsDemoState extends State<StreamsDemo> {
  late UVCCameraController cameraController;
  bool isCameraOpen = false;
  bool isStreaming = false;
  bool isRecording = false;
  int _previewFrameFormat = 0; // 0: YUYV, 1: MJPEG
  bool _isApplyingPreviewParams = false;

  // Streaming stats
  int videoFramesReceived = 0;
  int audioFramesReceived = 0;
  int lastVideoFrameSize = 0;
  String recordingTime = "00:00:00";
  String streamState = "Not started";

  // FPS values
  int _currentFps = 0;
  int _renderFps = 0; // GL render FPS
  StreamStatsEvent? _streamStats;
  bool _autoAdaptEnabled = true;

  @override
  void initState() {
    super.initState();
    cameraController = UVCCameraController();

    // Set up callbacks BEFORE attempting to open the camera
    cameraController.msgCallback = (state) {
      debugPrint("Camera message: $state");
      showCustomToast(state);
    };

    cameraController.cameraStateCallback = (state) {
      debugPrint("Camera state changed: $state");
      if (mounted) {
        setState(() {
          isCameraOpen = state == UVCCameraState.opened;
        });
      }
    };

    cameraController.onVideoFrameCallback = (frame) {
      debugPrint("Video frame received: size=${frame.size}, fps=${frame.fps}");
      setState(() {
        videoFramesReceived++;
        lastVideoFrameSize = frame.size;

        // Use the FPS from the frame directly
        if (frame.fps > 0) {
          _currentFps = frame.fps;
        }
      });
    };

    cameraController.onAudioFrameCallback = (frame) {
      setState(() {
        audioFramesReceived++;
      });
    };

    cameraController.onRecordingTimeCallback = (timeEvent) {
      setState(() {
        recordingTime = timeEvent.formattedTime;

        // 如果收到了最终的录制时间更新，那么录制已结束
        if (timeEvent.isFinal && isRecording) {
          isRecording = false;
        }
      });
    };

    cameraController.onStreamStateCallback = (stateEvent) {
      debugPrint("Stream state changed: ${stateEvent.state}");
      // STREAM_STATS is also a StateEvent; keep it out of "Stream State"
      // so UI semantics stay consistent.
      if (stateEvent.state == 'STREAM_STATS') return;
      setState(() {
        streamState = stateEvent.state;

        if (stateEvent.state == 'STARTED' ||
            stateEvent.state == 'STREAM_STARTED') {
          isStreaming = true;
        } else if (stateEvent.state == 'STOPPED' ||
            stateEvent.state == 'STREAM_STOPPED') {
          isStreaming = false;
          // Reset render FPS when streaming stops
          _renderFps = 0;
        } else if (stateEvent.state == 'RENDER_FPS' &&
            stateEvent.data != null) {
          // Update render FPS from native side
          final renderFps = stateEvent.data?['renderFps'];
          if (renderFps is int && renderFps > 0) {
            debugPrint("Received GL render FPS: $renderFps");
            _renderFps = renderFps;
          }
        }
      });
    };

    cameraController.onStreamStatsCallback = (statsEvent) {
      setState(() {
        _streamStats = statsEvent;
      });
    };

    // After initializing camera
    cameraController.setVideoFrameRateLimit(20); // Lower than default 30
    cameraController.setVideoFrameSizeLimit(1024 * 1024); // Limit frame size
    cameraController.setAudioFrameSizeLimit(0); // Keep audio unrestricted
    cameraController.enableAutoAdaptiveStreaming(
      minVideoFps: 10,
      maxVideoFps: 24,
      minVideoSizeLimit: 256 * 1024,
      maxVideoSizeLimit: 1024 * 1024,
    );
  }

  @override
  void dispose() {
    cameraController.closeCamera();
    cameraController.dispose();
    super.dispose();
  }

  Future<void> _togglePreviewFormat() async {
    if (!isCameraOpen ||
        isStreaming ||
        isRecording ||
        _isApplyingPreviewParams) {
      return;
    }

    final nextFormat = _previewFrameFormat == 0 ? 1 : 0;
    setState(() {
      _previewFrameFormat = nextFormat;
      _isApplyingPreviewParams = true;
    });

    // Preview fps/format: MJPEG often tolerates higher fps than YUYV.
    final nextMaxFps = nextFormat == 0 ? 30 : 60;
    await cameraController.updateCameraViewParams(
      UVCCameraViewParamsEntity(
        frameFormat: nextFormat,
        minFps: 10,
        maxFps: nextMaxFps,
        // Keep bandwidthFactor default (1.0) and preview size unchanged.
      ),
    );
    if (mounted) {
      setState(() {
        _isApplyingPreviewParams = false;
      });
    }
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
    debugPrint(
        "Toggle streaming called, current state: ${isStreaming ? 'streaming' : 'not streaming'}");
    if (isStreaming) {
      debugPrint("Stopping stream");
      cameraController.captureStreamStop();
    } else {
      debugPrint("Starting stream");
      cameraController.captureStreamStart();
    }
  }

  Future<void> captureVideo() async {
    if (isRecording) {
      // 如果正在录制，则停止录制
      setState(() {
        isRecording = false;
      });
      final path = await cameraController.captureVideo();
      if (path != null) {
        showCustomToast('Video saved to: $path');
      }
    } else {
      // 开始录制
      setState(() {
        isRecording = true;
        recordingTime = "00:00:00"; // 重置录制时间
      });
      await cameraController.captureVideo();
    }
  }

  void _toggleAutoAdapt(bool enabled) {
    setState(() {
      _autoAdaptEnabled = enabled;
    });
    if (enabled) {
      cameraController.enableAutoAdaptiveStreaming(
        minVideoFps: 10,
        maxVideoFps: 24,
        minVideoSizeLimit: 256 * 1024,
        maxVideoSizeLimit: 1024 * 1024,
      );
      showCustomToast('Auto adaptive streaming enabled');
    } else {
      cameraController.disableAutoAdaptiveStreaming();
      showCustomToast('Auto adaptive streaming disabled');
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
                    cameraController: cameraController,
                    params: const UVCCameraViewParamsEntity(frameFormat: 0),
                    width: 300,
                    height: 300,
                    autoDispose: false,
                  ),
                ),
                Positioned(
                  top: 10,
                  right: 10,
                  child: Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: isStreaming
                          ? Colors.green.withOpacity(0.7)
                          : Colors.black54,
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      '$_currentFps FPS',
                      style: TextStyle(
                        color: Colors.white,
                        fontWeight:
                            isStreaming ? FontWeight.bold : FontWeight.normal,
                        fontSize: isStreaming ? 14 : 12,
                      ),
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
                if (isRecording)
                  Positioned(
                    bottom: 10,
                    left: 0,
                    right: 0,
                    child: Container(
                      margin: const EdgeInsets.symmetric(horizontal: 20),
                      padding: const EdgeInsets.symmetric(
                          vertical: 6, horizontal: 12),
                      decoration: BoxDecoration(
                        color: Colors.red.withOpacity(0.8),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                            width: 12,
                            height: 12,
                            decoration: BoxDecoration(
                              color: Colors.white,
                              shape: BoxShape.circle,
                              border: Border.all(
                                  color: Colors.red.shade700, width: 2),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            'RECORDING $recordingTime',
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 14,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
              ],
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
                        : () => cameraController.openUVCCamera(),
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
                        ? () => cameraController.closeCamera()
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
                  onPressed: (isCameraOpen &&
                          !isStreaming &&
                          !isRecording &&
                          !_isApplyingPreviewParams)
                      ? _togglePreviewFormat
                      : null,
                  icon: const Icon(Icons.swap_horiz),
                  label: Text(
                    _previewFrameFormat == 0
                        ? 'Switch to MJPEG preview'
                        : 'Switch to YUYV preview',
                  ),
                ),
                const SizedBox(height: 8),
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
                  icon: Icon(isRecording ? Icons.stop : Icons.videocam),
                  label: Text(isRecording ? 'Stop Recording' : 'Record Video'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: isRecording ? Colors.red : Colors.orange,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 12),
                  ),
                ),
                const SizedBox(height: 8),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Audio-first auto adapt'),
                  subtitle: const Text(
                      'Auto reduce video fps/size when drops increase'),
                  value: _autoAdaptEnabled,
                  onChanged: isCameraOpen ? _toggleAutoAdapt : null,
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
                    if (_renderFps > 0)
                      _buildStatRow('GL Render FPS', _renderFps.toString(),
                          highlight: true),
                    if (_streamStats != null)
                      _buildStatRow(
                          'Native Video FPS', _streamStats!.videoFps.toString(),
                          highlight: true),
                    _buildStatRow('Recording Status',
                        isRecording ? 'Recording' : 'Not Recording',
                        highlight: isRecording),
                    _buildStatRow('Recording Time', recordingTime,
                        highlight: isRecording),
                    _buildStatRow(
                        'Video Frames', videoFramesReceived.toString()),
                    _buildStatRow(
                        'Audio Frames', audioFramesReceived.toString()),
                    _buildStatRow(
                        'Last Frame Size', '$lastVideoFrameSize bytes'),
                    if (_streamStats != null) ...[
                      const Divider(),
                      _buildStatRow('Native Video Frames',
                          _streamStats!.totalVideoFrames.toString()),
                      _buildStatRow('Native Audio Frames',
                          _streamStats!.totalAudioFrames.toString()),
                      _buildStatRow('Dropped Video Frames',
                          _streamStats!.droppedVideoFrames.toString()),
                      _buildStatRow('Dropped Audio Frames',
                          _streamStats!.droppedAudioFrames.toString()),
                      _buildStatRow(
                        'Video Drop Rate',
                        '${(_streamStats!.videoDropRate * 100).toStringAsFixed(2)}%',
                        highlight: _streamStats!.videoDropRate > 0.1,
                      ),
                      _buildStatRow(
                        'Audio Drop Rate',
                        '${(_streamStats!.audioDropRate * 100).toStringAsFixed(2)}%',
                        highlight: _streamStats!.audioDropRate > 0.02,
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatRow(String label, String value, {bool highlight = false}) {
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
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: highlight ? Colors.blue : null,
            ),
          ),
        ],
      ),
    );
  }
}
