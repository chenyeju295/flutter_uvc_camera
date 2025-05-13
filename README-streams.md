# UVC Camera 视频流和录制计时功能指南

本文档详细说明如何使用 Flutter UVC Camera 插件的视频流和录制计时新功能。

## 视频流功能

UVC Camera 插件现在使用 EventChannel 处理视频流数据，相比之前的方法，具有以下优点：

1. 更高效的数据传输
2. 实时帧率信息
3. 更好的流控制
4. 更低的内存占用

### 基本使用方法

```dart
// 创建控制器
final controller = UVCCameraController();

// 设置视频帧回调
controller.onVideoFrameCallback = (frame) {
  // 处理视频帧数据
  print('收到视频帧: ${frame.size} 字节, FPS: ${frame.fps}');
  
  // 这里可以对视频数据进行处理，例如保存到文件或进行视频分析
  processVideoData(frame.data);
};

// 设置音频帧回调（如果需要）
controller.onAudioFrameCallback = (frame) {
  // 处理音频帧数据
  processAudioData(frame.data);
};

// 设置流状态回调
controller.onStreamStateCallback = (state) {
  print('流状态变化: ${state.state}');
};

// 设置帧率限制（可选，默认30fps）
await controller.setVideoFrameRateLimit(15); // 限制每秒15帧

// 设置帧大小限制（可选，单位字节，默认不限制）
await controller.setVideoFrameSizeLimit(1024 * 1024); // 限制最大1MB

// 开始流
controller.captureStreamStart();

// 停止流
controller.captureStreamStop();
```

## 录制计时功能

新增的录制计时功能可以实时获取视频录制的时长信息。

### 基本使用方法

```dart
// 设置录制时间更新回调
controller.onRecordingTimeCallback = (timeEvent) {
  print('录制时间: ${timeEvent.formattedTime}');
  print('已录制毫秒数: ${timeEvent.elapsedMillis}');
  
  // 如果是录制结束的最终时间
  if (timeEvent.isFinal) {
    print('录制结束，总时长: ${timeEvent.formattedTime}');
  }
};

// 开始录制视频
final videoPath = await controller.captureVideo();

// 当前录制时间也可以直接从控制器获取
print('当前录制时间: ${controller.currentRecordingTimeFormatted}');
print('当前录制毫秒数: ${controller.currentRecordingTimeMs}');
```

## 示例：创建带计时器的视频录制页面

下面是一个实现带计时器的视频录制界面的示例：

```dart
class VideoRecordingPage extends StatefulWidget {
  final UVCCameraController controller;
  
  const VideoRecordingPage({Key? key, required this.controller}) : super(key: key);
  
  @override
  State<VideoRecordingPage> createState() => _VideoRecordingPageState();
}

class _VideoRecordingPageState extends State<VideoRecordingPage> {
  String recordingTime = "00:00:00";
  bool isRecording = false;
  String? recordedVideoPath;
  
  @override
  void initState() {
    super.initState();
    
    // 设置录制时间回调
    widget.controller.onRecordingTimeCallback = (timeEvent) {
      setState(() {
        recordingTime = timeEvent.formattedTime;
        
        if (timeEvent.isFinal) {
          isRecording = false;
        }
      });
    };
  }
  
  void toggleRecording() async {
    if (isRecording) {
      // 停止录制
      recordedVideoPath = await widget.controller.captureVideo();
      setState(() {
        isRecording = false;
      });
    } else {
      // 开始录制
      setState(() {
        isRecording = true;
        recordingTime = "00:00:00";
        recordedVideoPath = null;
      });
      
      recordedVideoPath = await widget.controller.captureVideo();
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('视频录制'),
        actions: [
          if (isRecording)
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 16),
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.red,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  const Icon(Icons.circle, color: Colors.white, size: 12),
                  const SizedBox(width: 4),
                  Text(
                    recordingTime,
                    style: const TextStyle(color: Colors.white),
                  ),
                ],
              ),
            ),
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // 相机预览
            UVCCameraView(
              cameraController: widget.controller,
              width: MediaQuery.of(context).size.width,
              height: MediaQuery.of(context).size.width * 4 / 3,
            ),
            
            const SizedBox(height: 20),
            
            // 录制按钮
            ElevatedButton.icon(
              onPressed: toggleRecording,
              icon: Icon(isRecording ? Icons.stop : Icons.videocam),
              label: Text(isRecording ? '停止录制' : '开始录制'),
              style: ElevatedButton.styleFrom(
                backgroundColor: isRecording ? Colors.red : Colors.blue,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              ),
            ),
            
            if (recordedVideoPath != null) ...[
              const SizedBox(height: 16),
              Text('视频已保存: $recordedVideoPath'),
            ],
          ],
        ),
      ),
    );
  }
}
```

## 注意事项

1. **内存管理**：视频帧数据可能较大，请确保及时处理或释放，避免内存泄漏。

2. **帧率限制**：在性能受限的设备上，建议限制帧率以避免卡顿。

3. **录制时间**：当前实现每100毫秒更新一次录制时间，足够大多数UI显示需求。

4. **事件订阅**：确保在组件销毁时正确释放控制器资源（dispose方法）。

5. **调试**：如果需要查看视频流的详细信息，可以临时将帧数据写入日志：
   ```dart
   controller.onVideoFrameCallback = (frame) {
     debugPrint('视频帧: 类型=${frame.type}, 大小=${frame.size}字节, FPS=${frame.fps}');
   };
   ``` 