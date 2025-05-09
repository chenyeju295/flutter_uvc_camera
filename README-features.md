# UVC Camera Features Guide

This document explains how to use the advanced camera features now exposed by the Flutter UVC Camera plugin.

## Available Features

The plugin now exposes the following camera features:

- **Auto Focus**: Enable/disable automatic focus
- **Auto White Balance**: Enable/disable automatic white balance
- **Zoom**: Set camera zoom level
- **Brightness**: Adjust camera brightness
- **Contrast**: Adjust camera contrast
- **Saturation**: Adjust color saturation
- **Sharpness**: Adjust image sharpness
- **Gain**: Adjust camera gain
- **Gamma**: Adjust gamma correction
- **Hue**: Adjust color hue

## Basic Usage

First, make sure to initialize your camera controller:

```dart
final cameraController = UVCCameraController();
await cameraController.initializeCamera();
await cameraController.openUVCCamera();
```

### Getting All Camera Features

You can retrieve all features at once:

```dart
final features = await cameraController.getAllCameraFeatures();
print('Auto focus: ${features?.autoFocus}');
print('Brightness: ${features?.brightness}');
// ... and so on
```

### Setting Individual Features

Set features using the appropriate methods:

```dart
// Set auto focus
await cameraController.setAutoFocus(true);

// Set brightness (values typically 0-255, camera dependent)
await cameraController.setBrightness(128);

// Set contrast
await cameraController.setContrast(128);

// Set zoom level
await cameraController.setZoom(1);
```

### Resetting Features

You can reset individual features to their default values:

```dart
await cameraController.resetCameraFeature('brightness');
await cameraController.resetCameraFeature('contrast');
// ... and so on
```

## Advanced Usage

For more control, you can use the generic methods:

```dart
// Set any camera feature by name and value
await cameraController.setCameraFeature('sharpness', 100);

// Get any camera feature value
final value = await cameraController.getCameraFeature('gain');
```

## Example: Creating a Camera Settings Page

Here's an example of a settings page that lets users adjust camera parameters:

```dart
class CameraSettingsPage extends StatefulWidget {
  final UVCCameraController controller;
  
  const CameraSettingsPage({Key? key, required this.controller}) : super(key: key);
  
  @override
  State<CameraSettingsPage> createState() => _CameraSettingsPageState();
}

class _CameraSettingsPageState extends State<CameraSettingsPage> {
  CameraFeatures? features;
  bool isLoading = true;
  
  @override
  void initState() {
    super.initState();
    _loadFeatures();
  }
  
  Future<void> _loadFeatures() async {
    features = await widget.controller.getAllCameraFeatures();
    setState(() {
      isLoading = false;
    });
  }
  
  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return Center(child: CircularProgressIndicator());
    }
    
    return ListView(
      children: [
        SwitchListTile(
          title: Text('Auto Focus'),
          value: features?.autoFocus ?? false,
          onChanged: (value) async {
            await widget.controller.setAutoFocus(value);
            _loadFeatures();
          },
        ),
        
        _buildSlider(
          title: 'Brightness',
          value: features?.brightness?.toDouble() ?? 128,
          onChanged: (value) async {
            await widget.controller.setBrightness(value.round());
          },
        ),
        
        // ... other settings
      ],
    );
  }
  
  Widget _buildSlider({
    required String title,
    required double value,
    required Function(double) onChanged,
    double min = 0,
    double max = 255,
  }) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: TextStyle(fontWeight: FontWeight.bold)),
          Row(
            children: [
              Expanded(
                child: Slider(
                  value: value.clamp(min, max),
                  min: min,
                  max: max,
                  onChanged: onChanged,
                ),
              ),
              Text(value.round().toString()),
            ],
          ),
        ],
      ),
    );
  }
}
```

## Notes

- Not all cameras support all features. Check if a feature is null before trying to use it.
- The valid range for each parameter depends on the specific camera model.
- Setting a feature to 0 typically resets it to the default value.
- Changes to camera features are not persistent across camera sessions - you'll need to reapply them when the camera is reopened. 