import 'package:flutter/material.dart';
import 'package:flutter_uvc_camera/flutter_uvc_camera.dart';

class FeaturesDemo extends StatefulWidget {
  const FeaturesDemo({super.key});

  @override
  State<FeaturesDemo> createState() => _FeaturesDemoState();
}

class _FeaturesDemoState extends State<FeaturesDemo> {
  UVCCameraController? cameraController;
  CameraFeatures? features;
  bool isLoading = false;
  bool isCameraOpen = false;

  // Current values for camera features
  int brightness = 0;
  int contrast = 0;
  int saturation = 0;
  int sharpness = 0;
  int zoom = 0;
  int gain = 0;
  int gamma = 0;
  int hue = 0;
  bool autoFocus = false;
  bool autoWhiteBalance = false;

  @override
  void initState() {
    super.initState();
    cameraController = UVCCameraController();
    cameraController?.msgCallback = (state) {
      showCustomToast(state);
    };
    cameraController?.cameraStateCallback = (state) {
      setState(() {
        isCameraOpen = state == UVCCameraState.opened;
        if (isCameraOpen) {
          _loadCameraFeatures();
        }
      });
    };
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

  Future<void> _loadCameraFeatures() async {
    setState(() {
      isLoading = true;
    });

    try {
      final result = await cameraController?.getAllCameraFeatures();
      if (result != null) {
        setState(() {
          features = result;

          // Initialize values from camera features
          brightness = result.brightness ?? 0;
          contrast = result.contrast ?? 0;
          saturation = result.saturation ?? 0;
          sharpness = result.sharpness ?? 0;
          zoom = result.zoom ?? 0;
          gain = result.gain ?? 0;
          gamma = result.gamma ?? 0;
          hue = result.hue ?? 0;
          autoFocus = result.autoFocus ?? false;
          autoWhiteBalance = result.autoWhiteBalance ?? false;
        });
      }
    } catch (e) {
      showCustomToast("Error loading features: $e");
    } finally {
      setState(() {
        isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Camera Features'),
        backgroundColor: Theme.of(context).colorScheme.primaryContainer,
      ),
      body: Column(
        children: [
          // Camera preview
          if (cameraController != null && isCameraOpen)
            Container(
              margin: const EdgeInsets.all(16),
              height: 200,
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
              child: ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: UVCCameraView(
                  cameraController: cameraController!,
                  params: const UVCCameraViewParamsEntity(frameFormat: 0),
                  width: double.infinity,
                  height: 200,
                ),
              ),
            )
          else
            Container(
              margin: const EdgeInsets.all(16),
              height: 200,
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
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton.icon(
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
              const SizedBox(width: 16),
              ElevatedButton.icon(
                onPressed:
                    isCameraOpen ? () => cameraController?.closeCamera() : null,
                icon: const Icon(Icons.close),
                label: const Text('Close Camera'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.red,
                  foregroundColor: Colors.white,
                  disabledBackgroundColor: Colors.grey.shade300,
                ),
              ),
            ],
          ),

          // Features controls
          Expanded(
            child: isLoading
                ? const Center(child: CircularProgressIndicator())
                : (features == null || !isCameraOpen)
                    ? Center(
                        child: Text(
                          isCameraOpen
                              ? 'No camera features available'
                              : 'Open camera to access features',
                          style: TextStyle(color: Colors.grey.shade600),
                        ),
                      )
                    : SingleChildScrollView(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Camera Adjustments',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 16),

                            // Auto controls
                            _buildSwitchControl(
                              label: 'Auto Focus',
                              value: autoFocus,
                              onChanged: features?.autoFocus != null
                                  ? (value) {
                                      setState(() => autoFocus = value);
                                      cameraController?.setAutoFocus(value);
                                    }
                                  : null,
                            ),

                            _buildSwitchControl(
                              label: 'Auto White Balance',
                              value: autoWhiteBalance,
                              onChanged: features?.autoWhiteBalance != null
                                  ? (value) {
                                      setState(() => autoWhiteBalance = value);
                                      cameraController
                                          ?.setAutoWhiteBalance(value);
                                    }
                                  : null,
                            ),

                            const Divider(),

                            // Slider controls for integer values
                            if (features?.brightness != null)
                              _buildSliderControl(
                                label: 'Brightness',
                                value: brightness.toDouble(),
                                min: 0,
                                max: 100,
                                onChanged: (value) {
                                  setState(() => brightness = value.toInt());
                                  cameraController
                                      ?.setBrightness(value.toInt());
                                },
                              ),

                            if (features?.contrast != null)
                              _buildSliderControl(
                                label: 'Contrast',
                                value: contrast.toDouble(),
                                min: 0,
                                max: 100,
                                onChanged: (value) {
                                  setState(() => contrast = value.toInt());
                                  cameraController?.setContrast(value.toInt());
                                },
                              ),

                            if (features?.saturation != null)
                              _buildSliderControl(
                                label: 'Saturation',
                                value: saturation.toDouble(),
                                min: 0,
                                max: 100,
                                onChanged: (value) {
                                  setState(() => saturation = value.toInt());
                                  cameraController
                                      ?.setSaturation(value.toInt());
                                },
                              ),

                            if (features?.sharpness != null)
                              _buildSliderControl(
                                label: 'Sharpness',
                                value: sharpness.toDouble(),
                                min: 0,
                                max: 100,
                                onChanged: (value) {
                                  setState(() => sharpness = value.toInt());
                                  cameraController?.setSharpness(value.toInt());
                                },
                              ),

                            if (features?.zoom != null)
                              _buildSliderControl(
                                label: 'Zoom',
                                value: zoom.toDouble(),
                                min: 0,
                                max: 100,
                                onChanged: (value) {
                                  setState(() => zoom = value.toInt());
                                  cameraController?.setZoom(value.toInt());
                                },
                              ),

                            if (features?.gain != null)
                              _buildSliderControl(
                                label: 'Gain',
                                value: gain.toDouble(),
                                min: 0,
                                max: 100,
                                onChanged: (value) {
                                  setState(() => gain = value.toInt());
                                  cameraController?.setGain(value.toInt());
                                },
                              ),

                            if (features?.gamma != null)
                              _buildSliderControl(
                                label: 'Gamma',
                                value: gamma.toDouble(),
                                min: 0,
                                max: 100,
                                onChanged: (value) {
                                  setState(() => gamma = value.toInt());
                                  cameraController?.setGamma(value.toInt());
                                },
                              ),

                            if (features?.hue != null)
                              _buildSliderControl(
                                label: 'Hue',
                                value: hue.toDouble(),
                                min: 0,
                                max: 100,
                                onChanged: (value) {
                                  setState(() => hue = value.toInt());
                                  cameraController?.setHue(value.toInt());
                                },
                              ),

                            // Reset all button
                            const SizedBox(height: 24),
                            SizedBox(
                              width: double.infinity,
                              child: ElevatedButton.icon(
                                onPressed:
                                    isCameraOpen ? _resetAllSettings : null,
                                icon: const Icon(Icons.restart_alt),
                                label: const Text('Reset All Settings'),
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: Colors.orange,
                                  foregroundColor: Colors.white,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildSwitchControl({
    required String label,
    required bool value,
    required ValueChanged<bool>? onChanged,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(fontSize: 16),
          ),
          Switch(
            value: value,
            onChanged: onChanged,
            activeColor: Theme.of(context).colorScheme.primary,
          ),
        ],
      ),
    );
  }

  Widget _buildSliderControl({
    required String label,
    required double value,
    required double min,
    required double max,
    required ValueChanged<double> onChanged,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                label,
                style: const TextStyle(fontSize: 16),
              ),
              Text(
                value.toInt().toString(),
                style: TextStyle(
                  fontSize: 14,
                  color: Theme.of(context).colorScheme.primary,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          Slider(
            value: value,
            min: min,
            max: max,
            divisions: (max - min).toInt(),
            onChanged: onChanged,
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                min.toInt().toString(),
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.grey.shade600,
                ),
              ),
              Text(
                max.toInt().toString(),
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.grey.shade600,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  void _resetAllSettings() async {
    if (features == null) return;

    setState(() {
      isLoading = true;
    });

    try {
      if (features?.brightness != null) {
        await cameraController?.resetCameraFeature('brightness');
      }
      if (features?.contrast != null) {
        await cameraController?.resetCameraFeature('contrast');
      }
      if (features?.saturation != null) {
        await cameraController?.resetCameraFeature('saturation');
      }
      if (features?.sharpness != null) {
        await cameraController?.resetCameraFeature('sharpness');
      }
      if (features?.zoom != null) {
        await cameraController?.resetCameraFeature('zoom');
      }
      if (features?.gain != null) {
        await cameraController?.resetCameraFeature('gain');
      }
      if (features?.gamma != null) {
        await cameraController?.resetCameraFeature('gamma');
      }
      if (features?.hue != null) {
        await cameraController?.resetCameraFeature('hue');
      }
      if (features?.autoFocus != null) {
        await cameraController?.resetCameraFeature('autofocus');
      }
      if (features?.autoWhiteBalance != null) {
        await cameraController?.resetCameraFeature('autowhitebalance');
      }

      // Refresh features after reset
      await _loadCameraFeatures();

      showCustomToast('All settings have been reset');
    } catch (e) {
      showCustomToast('Error resetting settings: $e');
    } finally {
      setState(() {
        isLoading = false;
      });
    }
  }
}
